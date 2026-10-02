package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeExecutorSecurityTest {
    private static final Duration CALLER_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration RECOVERY_STRESS_TIMEOUT = Duration.ofSeconds(20);
    private static final String TIMEOUT_PROPERTY = "algocraft.codeExecutorTimeoutMs";
    private static final String WORKER_MAX_TIMEOUT_PROPERTY = "algocraft.codeExecutorWorkerMaxTimeoutMs";
    private static final String WORKER_MODE_PROPERTY = "algocraft.codeExecutor.worker";
    private static final String IN_PROCESS_PROPERTY = "algocraft.codeExecutor.inProcess";
    private static final String FORCE_ECJ_COMPILER_PROPERTY = "algocraft.codeExecutor.forceEcjCompiler";
    private static final String DISPLAY_BOUNDING_TIMEOUT_MS = "1000";
    private String previousTimeoutProperty;
    private String previousWorkerMaxTimeoutProperty;
    private String previousWorkerModeProperty;
    private String previousInProcessProperty;
    private String previousForceEcjCompilerProperty;

    @BeforeEach
    void setShortExecutorTimeout() {
        previousTimeoutProperty = System.getProperty(TIMEOUT_PROPERTY);
        previousWorkerMaxTimeoutProperty = System.getProperty(WORKER_MAX_TIMEOUT_PROPERTY);
        previousWorkerModeProperty = System.getProperty(WORKER_MODE_PROPERTY);
        previousInProcessProperty = System.getProperty(IN_PROCESS_PROPERTY);
        previousForceEcjCompilerProperty = System.getProperty(FORCE_ECJ_COMPILER_PROPERTY);
        System.setProperty(TIMEOUT_PROPERTY, "100");
        System.clearProperty(WORKER_MAX_TIMEOUT_PROPERTY);
        System.clearProperty(WORKER_MODE_PROPERTY);
        System.clearProperty(IN_PROCESS_PROPERTY);
        System.clearProperty(FORCE_ECJ_COMPILER_PROPERTY);
    }

    @AfterEach
    void clearShortExecutorTimeout() {
        restoreProperty(TIMEOUT_PROPERTY, previousTimeoutProperty);
        restoreProperty(WORKER_MAX_TIMEOUT_PROPERTY, previousWorkerMaxTimeoutProperty);
        restoreProperty(WORKER_MODE_PROPERTY, previousWorkerModeProperty);
        restoreProperty(IN_PROCESS_PROPERTY, previousInProcessProperty);
        restoreProperty(FORCE_ECJ_COMPILER_PROPERTY, previousForceEcjCompilerProperty);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private void useDisplayBoundingTimeout() {
        System.setProperty(TIMEOUT_PROPERTY, DISPLAY_BOUNDING_TIMEOUT_MS);
    }

    @Test
    void workerProcessWallClockBudgetStaysBoundedForCiAndGameplay() throws Exception {
        Field startupGrace = CodeExecutor.class.getDeclaredField("WORKER_STARTUP_GRACE_MS");
        startupGrace.setAccessible(true);
        Field maxTimeout = CodeExecutor.class.getDeclaredField("WORKER_MAX_TIMEOUT_MS");
        maxTimeout.setAccessible(true);
        Method workerTimeout = CodeExecutor.class.getDeclaredMethod("workerTimeoutMs", int.class);
        workerTimeout.setAccessible(true);

        assertTrue(startupGrace.getInt(null) <= 15_000,
                "worker startup grace should not hide stuck compiler or process startup paths");
        assertTrue(maxTimeout.getInt(null) <= 45_000,
                "one judge worker must not be allowed to hold CI or gameplay for minutes by default");
        assertTrue((int) workerTimeout.invoke(null, 500) <= 45_000,
                "large official-bank batches must still respect the hard worker wall-clock cap");
        System.setProperty(TIMEOUT_PROPERTY, "8000");
        System.setProperty(WORKER_MAX_TIMEOUT_PROPERTY, "120000");
        assertTrue((int) workerTimeout.invoke(null, 15) >= 100_000,
                "reviewed official problems can explicitly request enough worker budget for compile and per-case checks");
        assertTrue((int) workerTimeout.invoke(null, 500) <= 120_000,
                "the explicit worker cap override must remain bounded");
    }

    @Test
    void configuredTimeoutHasStableLowerBoundForWorkerRuntimeOverhead() throws Exception {
        Method getTimeout = CodeExecutor.class.getDeclaredMethod("getTimeout");
        getTimeout.setAccessible(true);

        assertEquals(2_000, (int) getTimeout.invoke(null),
                "tiny timeout values should not make class loading and reflection overhead fail normal submissions");
    }

    @Test
    void serverJvmCannotForceUserCodeBackIntoProcessWithSystemProperty() throws Exception {
        Method runsInsideWorkerProcess = CodeExecutor.class.getDeclaredMethod("runsInsideWorkerProcess");
        runsInsideWorkerProcess.setAccessible(true);

        System.clearProperty(WORKER_MODE_PROPERTY);
        System.setProperty(IN_PROCESS_PROPERTY, "true");
        assertFalse((boolean) runsInsideWorkerProcess.invoke(null),
                "server JVM must ignore external in-process execution switches and keep user code isolated in workers");

        System.setProperty(WORKER_MODE_PROPERTY, "true");
        assertTrue((boolean) runsInsideWorkerProcess.invoke(null),
                "the isolated worker process must still identify itself with the internal worker marker");
    }

    @Test
    void workerProcessStreamReaderBoundsCapturedOutput() throws Exception {
        byte[] largeOutput = "x".repeat(16_384).getBytes(StandardCharsets.UTF_8);

        CodeExecutor.WorkerStreamOutput output = CodeExecutor.readProcessStream(
                new ByteArrayInputStream(largeOutput),
                1024
        ).get(2, TimeUnit.SECONDS);

        assertTrue(output.truncated(), "worker stream reader should report truncated oversized output");
        assertEquals(1024, output.text().length(),
                "worker stream reader should retain only the configured byte budget for ASCII output");
    }

    @Test
    void workerProcessStreamReaderPreservesSmallOutputWithoutTruncating() throws Exception {
        byte[] smallOutput = "worker-json".getBytes(StandardCharsets.UTF_8);

        CodeExecutor.WorkerStreamOutput output = CodeExecutor.readProcessStream(
                new ByteArrayInputStream(smallOutput),
                1024
        ).get(2, TimeUnit.SECONDS);

        assertFalse(output.truncated(), "small worker output should not be marked as truncated");
        assertEquals("worker-json", output.text());
    }

    @Test
    void executeRejectsDangerousSystemOperationsBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        System.exit(0);
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsDangerousSystemMethodReferenceBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.function.IntConsumer killer = System::exit;
                        killer.accept(0);
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsUnicodeEscapedDangerousSystemMethodReferenceBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.function.IntConsumer killer = SystemCOLONCOLONexit;
                        killer.accept(0);
                        return 1;
                    }
                }
                """.replace("COLONCOLON", unicodeEscape("003a") + unicodeEscape("003a")), "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeBatchRejectsDangerousSystemMethodReferenceBeforeWorkerCanExit() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int solve(int value) {
                        java.util.function.IntConsumer killer = System::exit;
                        killer.accept(value);
                        return value;
                    }
                }
                """, List.of(
                        new CodeExecutor.TestCase("value = 0", "0"),
                        new CodeExecutor.TestCase("value = 1", "1")
                ));

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(result ->
                result.message.startsWith("ERROR: Forbidden code pattern")), results.toString());
    }

    @Test
    void executeAllowsDangerousSystemMethodReferenceWordsInsideStringLiterals() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return "System::exit";
                    }
                }
                """, "", "\"System::exit\"");

        assertEquals("PASS", result);
    }

    @Test
    void executeRejectsDangerousRuntimeMethodReferenceBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.function.Supplier<Runtime> runtime = Runtime::getRuntime;
                        runtime.get().exit(0);
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsUnicodeEscapedDangerousRuntimeMethodReferenceBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.function.Supplier<Runtime> runtime = RuntimeCOLONCOLONgetRuntime;
                        runtime.get().exit(0);
                        return 1;
                    }
                }
                """.replace("COLONCOLON", unicodeEscape("003a") + unicodeEscape("003a")), "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeBatchRejectsDangerousRuntimeMethodReferenceBeforeWorkerCanExit() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int solve(int value) {
                        java.util.function.Supplier<Runtime> runtime = Runtime::getRuntime;
                        runtime.get().exit(value);
                        return value;
                    }
                }
                """, List.of(
                        new CodeExecutor.TestCase("value = 0", "0"),
                        new CodeExecutor.TestCase("value = 1", "1")
                ));

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(result ->
                result.message.startsWith("ERROR: Forbidden code pattern")), results.toString());
    }

    @Test
    void executeAllowsDangerousRuntimeMethodReferenceWordsInsideStringLiterals() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return "Runtime::getRuntime";
                    }
                }
                """, "", "\"Runtime::getRuntime\"");

        assertEquals("PASS", result);
    }

    @Test
    void executeRejectsForbiddenImportsBeforeCompilation() {
        String result = CodeExecutor.execute("""
                import java.nio.file.Path;
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden import"), result);
    }

    @Test
    void executeRejectsForbiddenSubpackageImportsEvenWhenParentWildcardIsAllowed() {
        String result = CodeExecutor.execute("""
                import java.util.concurrent.Future;
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden import: java.util.concurrent.Future"), result);
    }

    @Test
    void executeRejectsFullyQualifiedConcurrentUtilitiesBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.util.concurrent.CompletableFuture.supplyAsync(() -> 1).join();
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsParallelStreamBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.util.Arrays.asList(1, 2, 3)
                                .parallelStream()
                                .mapToInt(Integer::intValue)
                                .sum();
                    }
                }
                """, "", "6");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsStreamParallelBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.util.Arrays.asList(1, 2, 3)
                                .stream()
                                .parallel()
                                .mapToInt(Integer::intValue)
                                .sum();
                    }
                }
                """, "", "6");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsJavaUtilTimerBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        new java.util.Timer().schedule(new java.util.TimerTask() {
                            public void run() {
                            }
                        }, 1L);
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsUnicodeEscapedSystemCallsBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        SystemDOTexit(0);
                        return 1;
                    }
                }
                """.replace("DOT", unicodeEscape("002e")), "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsUnicodeEscapedForbiddenImportsBeforeCompilation() {
        String result = CodeExecutor.execute(("""
                import javaDOTnioDOTfileDOTPath;
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """).replace("DOT", unicodeEscape("002e")), "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden import: java.nio.file.Path"), result);
    }

    @Test
    void executeRejectsUnicodeEscapedCommentNewlineBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        // hidden after unicode newline NEWLINE                        System.exit(0);
                        return 1;
                    }
                }
                """.replace("NEWLINE", unicodeEscape("000a")), "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsUnicodeEscapedFullyQualifiedConcurrentBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return javaDOTutilDOTconcurrentDOTCompletableFuture.supplyAsync(() -> 1).join();
                    }
                }
                """.replace("DOT", unicodeEscape("002e")), "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executePreservesEscapedUnicodeTextInsideStringLiterals() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return "SystemESCAPED_DOTexit(0)";
                    }
                }
                """.replace("ESCAPED_DOT", "\\\\" + "u002e"), "",
                "\"System" + unicodeEscape("002e") + "exit(0)\"");

        assertEquals("PASS", result);
    }

    @Test
    void sandboxClassLoaderRejectsConcurrentUtilitiesEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.util.concurrent.CompletableFuture"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void executeAllowsBigIntegerImportForExactArithmetic() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                import java.math.BigInteger;
                class Solution {
                    public String solve() {
                        return BigInteger.valueOf(40).add(BigInteger.valueOf(2)).toString();
                    }
                }
                """, "", "\"42\"");

        assertEquals("PASS", result);
    }

    @Test
    void sandboxClassLoaderAllowsBigIntegerForExactArithmetic() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertEquals("java.math.BigInteger", loader.loadClass("java.math.BigInteger").getName());
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsTimerEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.util.Timer"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.util.TimerTask"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsThreadingApisEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.Thread"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.ThreadGroup"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.ThreadLocal"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.InheritableThreadLocal"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsSystemLoggerApisEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.System$Logger"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.System$Logger$Level"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void executeRejectsServiceLoaderBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.util.ServiceLoader.load(Runnable.class).iterator().hasNext() ? 1 : 0;
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsResourceBundleBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return java.util.ResourceBundle.getBundle("algocraft").getBaseBundleName();
                    }
                }
                """, "", "\"algocraft\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsThrowableStackTraceDiscoveryBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return new RuntimeException("probe").getStacESCAPED_KTrace().length;
                    }
                }
                """.replace("ESCAPED_K", unicodeEscape("006b")), "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsThrowablePrintStackTraceBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        new IllegalStateException("probe").printStackTrace();
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void sandboxClassLoaderRejectsClasspathDiscoveryUtilitiesEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.util.ServiceLoader"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.util.ResourceBundle"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsStackTraceElementEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.StackTraceElement"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsReflectionCoreClassesEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.reflect.Method"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.reflect.Field"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.reflect.Constructor"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.reflect.AccessibleObject"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsMethodHandleReflectionApisEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.invoke.MethodHandles"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.invoke.MethodHandles$Lookup"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.invoke.MethodHandle"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.invoke.MethodType"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.invoke.VarHandle"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsFormatterAndReferenceApisEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.util.Formatter"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.ref.Cleaner"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.ref.WeakReference"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.ref.ReferenceQueue"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsNetworkFileProcessManagementAndUiApisEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.net.Socket"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.io.File"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.nio.file.Path"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.ProcessHandle"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.lang.management.ManagementFactory"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("javax.management.MBeanServerFactory"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.awt.Desktop"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("javax.swing.JOptionPane"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.beans.Expression"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void sandboxClassLoaderRejectsSecurityAndInternalJdkApisEvenWithoutImports() {
        try (SandboxClassLoader loader = new SandboxClassLoader(new URL[0],
                CodeExecutorSecurityTest.class.getClassLoader())) {
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("java.security.MessageDigest"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("sun.misc.Unsafe"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("jdk.internal.misc.Unsafe"));
            assertThrows(SecurityException.class,
                    () -> loader.loadClass("com.sun.crypto.provider.AESCipher"));
        } catch (Exception exception) {
            throw new AssertionError("sandbox classloader setup should not fail", exception);
        }
    }

    @Test
    void executeRejectsPackagePrefixSpoofingImports() {
        String result = CodeExecutor.execute("""
                import java.utilx.List;
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden import: java.utilx.List"), result);
    }

    @Test
    void executeIgnoresForbiddenImportsInsideComments() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                // import java.nio.file.Path;
                /*
                 * import java.util.concurrent.Future;
                 */
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");

        assertEquals("PASS", result);
    }

    @Test
    void executeIgnoresForbiddenImportsInsideStringLiterals() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return "import java.nio.file.Path;";
                    }
                }
                """, "", "\"import java.nio.file.Path;\"");

        assertEquals("PASS", result);
    }

    @Test
    void executeAllowsDangerousWordsInsideStringLiterals() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return "System.exit(0)";
                    }
                }
                """, "", "\"System.exit(0)\"");

        assertEquals("PASS", result);
    }

    @Test
    void executeAllowsClasspathDiscoveryWordsInsideStringLiterals() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return "ServiceLoader ResourceBundle";
                    }
                }
                """, "", "\"ServiceLoader ResourceBundle\"");

        assertEquals("PASS", result);
    }

    @Test
    void executeAllowsRejectedNamespaceWordsInsideStringLiterals() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return "java.security.MessageDigest javax.crypto.Cipher sun.misc.Unsafe";
                    }
                }
                """, "", "\"java.security.MessageDigest javax.crypto.Cipher sun.misc.Unsafe\"");

        assertEquals("PASS", result);
    }

    @Test
    void executeRejectsDangerousCallAfterStringContainingLineCommentMarker() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        String marker = "https://example.test";
                        System.exit(0);
                        return marker.length();
                    }
                }
                """, "", "20");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSystemEnvironmentAccessBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return System.getenv().size();
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSystemPropertiesAccessBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return System.getProperties().getProperty("user.home", "");
                    }
                }
                """, "", "\"\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSystemPropertyMutationBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        System.clearProperty("user.home");
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSystemGarbageCollectionBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        System.gc();
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSystemFinalizationBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        System.runFinalization();
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSystemLoggerBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        SystemDOTgetLogger("algocraft").log(SystemDOTLogger.Level.INFO, "probe");
                        return 1;
                    }
                }
                """.replace("DOT", unicodeEscape("002e")), "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsLocaleDefaultMutationBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.Locale.setDefault(java.util.Locale.ROOT);
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsTimeZoneDefaultMutationBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsFullyQualifiedJavaIoFileBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public boolean solve() {
                        return new java.io.File("algocraft-should-not-touch").delete();
                    }
                }
                """, "", "false");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsFullyQualifiedJavaNioFileApiBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return java.nio.file.Path.of("algocraft-should-not-touch").toString();
                    }
                }
                """, "", "\"algocraft-should-not-touch\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsFormatterFileConstructorMadeAvailableByDefaultImportsBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        Formatter formatter = new Formatter("algocraft-should-not-create.txt");
                        formatter.format("%d", 1);
                        formatter.close();
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsFullyQualifiedFormatterFileConstructorBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.Formatter formatter = new java.util.Formatter("algocraft-should-not-create.txt");
                        formatter.format("%d", 1);
                        formatter.close();
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeAllowsStringFormatForNormalAlgorithmOutput() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return String.format("%02d", 7);
                    }
                }
                """, "", "\"07\"");

        assertEquals("PASS", result);
    }

    @Test
    void executeRejectsProcessHandleBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public long solve() {
                        return java.lang.ProcessHandle.current().pid();
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsThreadSleepBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() throws Exception {
                        Thread.sleep(1L);
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsFullyQualifiedThreadConstructorBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.lang.Thread thread = new java.lang.Thread(() -> {});
                        return thread.getName().length();
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsThreadLocalConstructorBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        ThreadLocal<Integer> local = new ThreadLocal<>();
                        local.set(1);
                        return local.get();
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsManagementFactoryBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.lang.management.ManagementFactory.getRuntimeMXBean()
                                .getInputArguments()
                                .size();
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsFullyQualifiedReflectionBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.lang.reflect.Array.getLength(new int[] {1, 2, 3});
                    }
                }
                """, "", "3");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsFullyQualifiedJavaLangInvokeBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.lang.invoke.MethodType.methodType(void.class).parameterCount();
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsUnicodeEscapedJavaLangInvokeBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return javaDOTlangDOTinvokeDOTMethodType.methodType(void.class).parameterCount();
                    }
                }
                """.replace("DOT", unicodeEscape("002e")), "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsReflectionInvokeThroughClassLiteralBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() throws Exception {
                        var method = System.class.getDeclaredMethod("getenv", String.class);
                        method.setAccessible(true);
                        return (String) method.invoke(null, "PATH");
                    }
                }
                """, "", "\"\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsReflectionMetadataEnumerationBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return System.class.getDeclaredMethods().length;
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsClassLiteralResourceDiscoveryBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return Solution.class.getResource("Solution.class") == null ? 0 : 1;
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsGetClassProtectionDomainBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return getClass().getProtectionDomain().getCodeSource().getLocation().toString();
                    }
                }
                """, "", "\"\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsClassLiteralModuleDiscoveryBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return Solution.class.getModule().getName();
                    }
                }
                """, "", "\"\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsIndirectClassResourceDiscoveryBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        var type = Solution.class;
                        return type.getResource("Solution.class") == null ? 0 : 1;
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsIndirectProtectionDomainDiscoveryBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        var type = getClass();
                        return type.getProtectionDomain().getCodeSource().getLocation().toString();
                    }
                }
                """, "", "\"\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsIndirectModuleDiscoveryBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        var type = Solution.class;
                        return type.getModule().getName();
                    }
                }
                """, "", "\"\"");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsStackWalkerBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public long solve() {
                        return StackWalker.getInstance().walk(stream -> stream.count());
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsRuntimeVersionBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return Runtime.version().feature();
                    }
                }
                """, "", "21");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsJavaLangRefCleanerBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.lang.ref.Cleaner.create() == null ? 0 : 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsJavaLangRefWeakReferenceBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return new java.lang.ref.WeakReference<Object>(this).get() == null ? 0 : 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsDesktopApiBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public boolean solve() {
                        return java.awt.Desktop.isDesktopSupported();
                    }
                }
                """, "", "false");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSwingApiBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        javax.swing.JOptionPane.showMessageDialog(null, "blocked");
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsJavaxManagementBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return javax.management.MBeanServerFactory.findMBeanServer(null).size();
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsJavaBeansExpressionBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public Object solve() throws Exception {
                        return new java.beans.Expression(System.class, "getenv", new Object[0]).getValue();
                    }
                }
                """, "", "null");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsLoggingManagerBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.logging.LogManager.getLogManager().reset();
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsPreferencesBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        java.util.prefs.Preferences.userRoot().node("algocraft").put("k", "v");
                        return 1;
                    }
                }
                """, "", "1");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSqlDriverManagerBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public boolean solve() {
                        return java.sql.DriverManager.getDrivers().hasMoreElements();
                    }
                }
                """, "", "false");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsRmiNamingBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() throws Exception {
                        return java.rmi.Naming.list("//localhost/").length;
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsSecurityPackageBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() throws Exception {
                        return java.security.MessageDigest.getInstance("SHA-256")
                                .digest(new byte[] {1, 2, 3})
                                .length;
                    }
                }
                """, "", "32");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsBroadJavaxPackageBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() throws Exception {
                        return javax.crypto.Cipher.getInstance("AES").getBlockSize();
                    }
                }
                """, "", "16");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsInternalJdkAndSunPackagesBeforeCompilation() {
        String sunResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return sun.misc.Unsafe.getUnsafe().addressSize();
                    }
                }
                """, "", "8");

        assertTrue(sunResult.startsWith("ERROR: Forbidden code pattern"), sunResult);

        String jdkResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return jdk.internal.misc.Unsafe.getUnsafe().addressSize();
                    }
                }
                """, "", "8");

        assertTrue(jdkResult.startsWith("ERROR: Forbidden code pattern"), jdkResult);

        String comSunResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return com.sun.crypto.provider.AESCipher.class.getName().length();
                    }
                }
                """, "", "0");

        assertTrue(comSunResult.startsWith("ERROR: Forbidden code pattern"), comSunResult);
    }

    @Test
    void executeRejectsArchiveAndCalendarPackagesBeforeCompilation() {
        String zipResult = CodeExecutor.execute("""
                class Solution {
                    public long solve() {
                        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                        crc.update(new byte[] {1, 2, 3});
                        return crc.getValue();
                    }
                }
                """, "", "0");

        assertTrue(zipResult.startsWith("ERROR: Forbidden code pattern"), zipResult);

        String jarResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return new java.util.jar.Manifest().getMainAttributes().size();
                    }
                }
                """, "", "0");

        assertTrue(jarResult.startsWith("ERROR: Forbidden code pattern"), jarResult);

        String timeResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return java.time.LocalDate.now().getYear();
                    }
                }
                """, "", "2026");

        assertTrue(timeResult.startsWith("ERROR: Forbidden code pattern"), timeResult);

        String textResult = CodeExecutor.execute("""
                class Solution {
                    public String solve() {
                        return new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(0L));
                    }
                }
                """, "", "\"1970\"");

        assertTrue(textResult.startsWith("ERROR: Forbidden code pattern"), textResult);
    }

    @Test
    void executeRejectsJavaLangForeignBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public long solve() {
                        return java.lang.foreign.MemorySegment.NULL.byteSize();
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeRejectsModuleLayerBeforeCompilation() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return ModuleLayer.boot().modules().size();
                    }
                }
                """, "", "0");

        assertTrue(result.startsWith("ERROR: Forbidden code pattern"), result);
    }

    @Test
    void executeTruncatesOversizedWrongAnswerOutputBeforeReturningToCaller() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute(oversizedStringReturnCode(), "", "\"expected\"");

        assertTrue(result.startsWith("FAIL: Expected \"expected\", got \""), result);
        assertTrue(result.contains("[truncated]"), result);
        assertTrue(result.length() < 2_200, "Oversized actual output should not be returned in full");
    }

    @Test
    void executeBatchTruncatesOversizedActualOutputBeforeReturningToCaller() {
        useDisplayBoundingTimeout();

        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch(
                oversizedStringReturnCode(),
                List.of(new CodeExecutor.TestCase("", "\"expected\""))
        );

        assertEquals(1, results.size());
        CodeExecutor.TestResult result = results.get(0);
        assertFalse(result.passed);
        assertTrue(result.message.contains("[truncated]"), result.message);
        assertTrue(result.message.length() < 2_200, "Oversized failure message should be display-bounded");
        assertTrue(result.actualOutput.contains("[truncated]"), result.actualOutput);
        assertTrue(result.actualOutput.length() < 2_100, "Oversized actual output should be display-bounded");
    }

    @Test
    void judgeTruncatesOversizedWrongAnswerDetailsBeforeUiDisplay() {
        useDisplayBoundingTimeout();

        Problem problem = new Problem();
        problem.setId("hostile-output");
        Problem.TestCase visibleCase = new Problem.TestCase();
        visibleCase.setInput("");
        visibleCase.setOutput("\"expected\"");
        problem.setExamples(List.of(visibleCase));
        problem.setTests(List.of());

        SubmissionResult result = Judge.grade(problem, oversizedStringReturnCode());

        assertFalse(result.isSuccess());
        assertEquals("Wrong Answer", result.getMessage());
        assertEquals(1, result.getDetails().size());
        String actual = result.getDetails().get(0).getActual();
        assertTrue(actual.contains("[truncated]"), actual);
        assertTrue(actual.length() < 2_100, "Judge details should be safe for the in-game result panel");
    }

    @Test
    void executeTruncatesOversizedRuntimeExceptionMessageAndRecovers() {
        useDisplayBoundingTimeout();

        String result = CodeExecutor.execute(oversizedExceptionCode(), "", "1");

        assertTrue(result.startsWith("ERROR: IllegalStateException: "), result);
        assertTrue(result.contains("..."), result);
        assertTrue(result.length() < 280, "Oversized exception messages should be display-bounded");

        String normalResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");
        assertEquals("PASS", normalResult);
    }

    @Test
    void executeBatchTruncatesOversizedRuntimeExceptionMessageAndRecovers() {
        useDisplayBoundingTimeout();

        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch(
                oversizedExceptionCode(),
                List.of(new CodeExecutor.TestCase("", "1"))
        );

        assertEquals(1, results.size());
        CodeExecutor.TestResult result = results.get(0);
        assertFalse(result.passed);
        assertTrue(result.isRuntimeError(), result.message);
        assertTrue(result.message.contains("IllegalStateException"), result.message);
        assertTrue(result.message.contains("..."), result.message);
        assertTrue(result.message.length() < 280, "Batch exception messages should be display-bounded");

        List<CodeExecutor.TestResult> normalResults = CodeExecutor.executeBatch("""
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, List.of(new CodeExecutor.TestCase("", "1")));
        assertEquals(1, normalResults.size());
        assertTrue(normalResults.get(0).passed, normalResults.get(0).message);
    }

    @Test
    void judgeTruncatesOversizedRuntimeExceptionDetailsBeforeUiDisplay() {
        useDisplayBoundingTimeout();

        Problem problem = singleVisibleCaseProblem("hostile-runtime", "", "1");

        SubmissionResult result = Judge.grade(problem, oversizedExceptionCode());

        assertFalse(result.isSuccess());
        assertEquals("Runtime Error", result.getMessage());
        assertEquals(1, result.getDetails().size());
        String error = result.getDetails().get(0).getError();
        assertTrue(error.contains("IllegalStateException"), error);
        assertTrue(error.contains("..."), error);
        assertTrue(error.length() < 280, "Judge runtime error details should be safe for the in-game result panel");
    }

    @Test
    void executeContainsStackOverflowErrorAndRecovers() {
        useDisplayBoundingTimeout();

        String result = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.execute(stackOverflowCode(), "", "1"));

        assertTrue(result.startsWith("ERROR: StackOverflowError"), result);
        assertTrue(result.length() < 280, "Errors should stay display-bounded");

        String normalResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");
        assertEquals("PASS", normalResult);
    }

    @Test
    void executeBatchContainsStackOverflowErrorAndRecovers() {
        useDisplayBoundingTimeout();

        List<CodeExecutor.TestResult> results = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.executeBatch(
                stackOverflowCode(),
                List.of(new CodeExecutor.TestCase("", "1"))
        ));

        assertEquals(1, results.size());
        CodeExecutor.TestResult result = results.get(0);
        assertFalse(result.passed);
        assertTrue(result.isRuntimeError(), result.message);
        assertTrue(result.message.contains("StackOverflowError"), result.message);
        assertTrue(result.message.length() < 280, "Batch errors should stay display-bounded");

        List<CodeExecutor.TestResult> normalResults = CodeExecutor.executeBatch("""
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, List.of(new CodeExecutor.TestCase("", "1")));
        assertEquals(1, normalResults.size());
        assertTrue(normalResults.get(0).passed, normalResults.get(0).message);
    }

    @Test
    void judgeContainsStackOverflowErrorDetailsBeforeUiDisplay() {
        useDisplayBoundingTimeout();

        Problem problem = singleVisibleCaseProblem("hostile-stack-overflow", "", "1");

        SubmissionResult result = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> Judge.grade(problem, stackOverflowCode()));

        assertFalse(result.isSuccess());
        assertEquals("Runtime Error", result.getMessage());
        assertEquals(1, result.getDetails().size());
        String error = result.getDetails().get(0).getError();
        assertTrue(error.contains("StackOverflowError"), error);
        assertTrue(error.length() < 280, "Judge runtime error details should stay display-bounded");
    }

    @Test
    void executeContainsOutOfMemoryErrorAndRecovers() {
        useDisplayBoundingTimeout();

        String result = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.execute(outOfMemoryCode(), "", "1"));

        assertTrue(result.startsWith("ERROR: OutOfMemoryError"), result);
        assertTrue(result.length() < 280, "Memory errors should stay display-bounded");

        String normalResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");
        assertEquals("PASS", normalResult);
    }

    @Test
    void executeBatchContainsOutOfMemoryErrorAndRecovers() {
        useDisplayBoundingTimeout();

        List<CodeExecutor.TestResult> results = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.executeBatch(
                outOfMemoryCode(),
                List.of(new CodeExecutor.TestCase("", "1"))
        ));

        assertEquals(1, results.size());
        CodeExecutor.TestResult result = results.get(0);
        assertFalse(result.passed);
        assertTrue(result.isRuntimeError(), result.message);
        assertTrue(result.message.contains("OutOfMemoryError"), result.message);
        assertTrue(result.message.length() < 280, "Batch memory errors should stay display-bounded");

        List<CodeExecutor.TestResult> normalResults = CodeExecutor.executeBatch("""
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, List.of(new CodeExecutor.TestCase("", "1")));
        assertEquals(1, normalResults.size());
        assertTrue(normalResults.get(0).passed, normalResults.get(0).message);
    }

    @Test
    void judgeContainsOutOfMemoryErrorDetailsBeforeUiDisplay() {
        useDisplayBoundingTimeout();

        Problem problem = singleVisibleCaseProblem("hostile-memory", "", "1");

        SubmissionResult result = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> Judge.grade(problem, outOfMemoryCode()));

        assertFalse(result.isSuccess());
        assertEquals("Runtime Error", result.getMessage());
        assertEquals(1, result.getDetails().size());
        String error = result.getDetails().get(0).getError();
        assertTrue(error.contains("OutOfMemoryError"), error);
        assertTrue(error.length() < 280, "Judge memory error details should stay display-bounded");
    }

    @Test
    void ecjFallbackCompilesSingleAndBatchSubmissionsWhenSystemJavacIsBypassed() {
        useDisplayBoundingTimeout();
        System.setProperty(FORCE_ECJ_COMPILER_PROPERTY, "true");

        String singleResult = CodeExecutor.execute("""
                class Solution {
                    public int solve(int value) {
                        return value * 2;
                    }
                }
                """, "value = 21", "42");
        assertEquals("PASS", singleResult);

        List<CodeExecutor.TestResult> batchResults = CodeExecutor.executeBatch("""
                class Solution {
                    public int solve(int value) {
                        return value + 1;
                    }
                }
                """, List.of(
                        new CodeExecutor.TestCase("value = 1", "2"),
                        new CodeExecutor.TestCase("value = 41", "42")
                ));
        assertEquals(2, batchResults.size());
        assertTrue(batchResults.stream().allMatch(result -> result.passed),
                () -> "ECJ fallback batch results should pass: " + batchResults.stream()
                        .map(result -> result.message)
                        .toList());
    }

    @Test
    void executeTimesOutStaticInitializerWithoutBlockingCaller() {
        String result = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.execute("""
                class Solution {
                    private static volatile long sink;

                    static {
                        while (keepRunning()) {
                            sink++;
                        }
                    }

                    private static boolean keepRunning() {
                        return true;
                    }

                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1"));

        assertTrue(result.contains("Time Limit Exceeded"), result);
    }

    @Test
    void executeTimesOutConstructorWithoutBlockingCaller() {
        String result = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.execute("""
                class Solution {
                    private volatile long sink;

                    public Solution() {
                        while (true) {
                            sink++;
                        }
                    }

                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1"));

        assertTrue(result.contains("Time Limit Exceeded"), result);
    }

    @Test
    void executeBatchTimesOutClassInitializationBeforeRunningTests() {
        List<CodeExecutor.TestResult> results = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.executeBatch("""
                class Solution {
                    private static volatile long sink;

                    static {
                        while (keepRunning()) {
                            sink++;
                        }
                    }

                    private static boolean keepRunning() {
                        return true;
                    }

                    public int solve() {
                        return 1;
                    }
                }
                """, List.of(new CodeExecutor.TestCase("", "1"))));

        assertTrue(results.get(0).isTimeLimit(), results.get(0).message);
    }

    @Test
    void executeBatchTimesOutConstructorPerTestCase() {
        List<CodeExecutor.TestResult> results = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.executeBatch("""
                class Solution {
                    private volatile long sink;

                    public Solution() {
                        while (true) {
                            sink++;
                        }
                    }

                    public int solve() {
                        return 1;
                    }
                }
                """, List.of(new CodeExecutor.TestCase("", "1"))));

        assertTrue(results.get(0).isTimeLimit(), results.get(0).message);
    }

    @Test
    void timedOutExecutionsDoNotBlockLaterNormalSubmissions() {
        String timedOutCode = """
                class Solution {
                    private volatile long sink;

                    public int solve() {
                        while (true) {
                            sink++;
                        }
                    }
                }
                """;

        assertTimeoutPreemptively(RECOVERY_STRESS_TIMEOUT, () -> {
            for (int i = 0; i < 4; i++) {
                String result = CodeExecutor.execute(timedOutCode, "", "1");
                assertTrue(result.contains("Time Limit Exceeded"), result);
            }

            String normalResult = CodeExecutor.execute("""
                    class Solution {
                        public int solve() {
                            return 1;
                        }
                    }
                    """, "", "1");

            assertEquals("PASS", normalResult);
        });
    }

    @Test
    void nonInterruptibleInfiniteLoopIsContainedOutsideTheCallerJvm() {
        long executorThreadsBefore = liveExecutorThreadCount();

        String result = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        while (true) {
                        }
                    }
                }
                """, "", "1"));

        assertTrue(result.contains("Time Limit Exceeded"), result);
        assertEquals(executorThreadsBefore, liveExecutorThreadCount(),
                "A non-interruptible user loop must not leave execution threads in the caller JVM");

        String normalResult = CodeExecutor.execute("""
                class Solution {
                    public int solve() {
                        return 1;
                    }
                }
                """, "", "1");
        assertEquals("PASS", normalResult);
    }

    @Test
    void batchNonInterruptibleLoopStopsBatchAndDoesNotPoisonCallerJvm() {
        long executorThreadsBefore = liveExecutorThreadCount();

        List<CodeExecutor.TestResult> results = assertTimeoutPreemptively(CALLER_TIMEOUT, () -> CodeExecutor.executeBatch("""
                class Solution {
                    public int solve(int value) {
                        if (value == 1) {
                            while (true) {
                            }
                        }
                        return value;
                    }
                }
                """, List.of(
                        new CodeExecutor.TestCase("value = 1", "1"),
                        new CodeExecutor.TestCase("value = 2", "2")
                )));

        assertEquals(2, results.size());
        assertTrue(results.get(0).isTimeLimit(), results.get(0).message);
        assertTrue(results.get(1).message.startsWith("ERROR: Skipped after previous Time Limit Exceeded"),
                results.get(1).message);
        assertEquals(executorThreadsBefore, liveExecutorThreadCount(),
                "A timed-out batch test must not leave execution threads in the caller JVM");
    }

    private static long liveExecutorThreadCount() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(thread -> thread.getName().startsWith("AlgoCraft-CodeExecutor"))
                .count();
    }

    private static String oversizedStringReturnCode() {
        return """
                class Solution {
                    public String solve() {
                        return "x".repeat(8_000);
                    }
                }
                """;
    }

    private static String oversizedExceptionCode() {
        return """
                class Solution {
                    public int solve() {
                        throw new IllegalStateException("x".repeat(8_000));
                    }
                }
                """;
    }

    private static String stackOverflowCode() {
        return """
                class Solution {
                    public int solve() {
                        return recurse();
                    }

                    private int recurse() {
                        return recurse();
                    }
                }
                """;
    }

    private static String outOfMemoryCode() {
        return """
                class Solution {
                    public int solve() {
                        int[] values = new int[Integer.MAX_VALUE];
                        return values.length;
                    }
                }
                """;
    }

    private static Problem singleVisibleCaseProblem(String id, String input, String output) {
        Problem problem = new Problem();
        problem.setId(id);
        Problem.TestCase visibleCase = new Problem.TestCase();
        visibleCase.setInput(input);
        visibleCase.setOutput(output);
        problem.setExamples(List.of(visibleCase));
        problem.setTests(List.of());
        return problem;
    }

    private static String unicodeEscape(String hex) {
        return "\\" + "u" + hex;
    }
}
