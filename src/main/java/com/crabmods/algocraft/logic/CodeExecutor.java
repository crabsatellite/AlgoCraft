package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.Config;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes user-submitted code in a sandboxed environment with timeout protection.
 * <p>
 * Security measures:
 * <ul>
 *   <li>Pattern-based code scanning to block dangerous operations</li>
 *   <li>Execution timeout to prevent infinite loops</li>
 *   <li>Isolated classloader for each execution</li>
 *   <li>Temporary file cleanup after execution</li>
 * </ul>
 */
public class CodeExecutor {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Use a bounded thread pool to prevent resource exhaustion
    private static final int MAX_CONCURRENT_EXECUTIONS = 4;
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
        1, MAX_CONCURRENT_EXECUTIONS,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(10),
        r -> {
            Thread t = new Thread(r, "AlgoCraft-CodeExecutor");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY); // Lower priority for user code
            return t;
        },
        new ThreadPoolExecutor.CallerRunsPolicy() // Fallback to caller thread if pool is full
    );
    
    // Default timeout in milliseconds (used if config not available)
    private static final int DEFAULT_TIMEOUT_MS = 2000;
    
    // Maximum code length to prevent DoS attacks
    private static final int MAX_CODE_LENGTH = 50_000;
    
    // Maximum compilation output size
    private static final int MAX_COMPILE_OUTPUT_SIZE = 10_000;
    
    // Dangerous patterns that should be blocked in user code
    // Using case-insensitive matching and handling whitespace variations
    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
        // Process execution
        Pattern.compile("Runtime\\s*\\.\\s*getRuntime\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Runtime\\s*\\.\\s*exec", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ProcessBuilder", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Process\\s+", Pattern.CASE_INSENSITIVE),
        
        // System operations
        Pattern.compile("System\\s*\\.\\s*exit\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setProperty", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setSecurityManager", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*load\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*loadLibrary", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*console", Pattern.CASE_INSENSITIVE),
        
        // Network operations
        Pattern.compile("java\\s*\\.\\s*net\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javax\\s*\\.\\s*net\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Socket", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ServerSocket", Pattern.CASE_INSENSITIVE),
        Pattern.compile("URL\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("HttpClient", Pattern.CASE_INSENSITIVE),
        Pattern.compile("HttpURLConnection", Pattern.CASE_INSENSITIVE),
        
        // File operations
        Pattern.compile("java\\s*\\.\\s*nio\\s*\\.\\s*file\\s*\\.\\s*Files", Pattern.CASE_INSENSITIVE),
        Pattern.compile("java\\s*\\.\\s*io\\s*\\.\\s*File(?:Input|Output|Reader|Writer)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("RandomAccessFile", Pattern.CASE_INSENSITIVE),
        Pattern.compile("FileChannel", Pattern.CASE_INSENSITIVE),
        
        // Reflection and classloading
        Pattern.compile("Class\\s*\\.\\s*forName\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ClassLoader", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*getClass\\s*\\(\\s*\\)\\s*\\.\\s*getResource", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*getClass\\s*\\(\\s*\\)\\s*\\.\\s*getClassLoader", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Method\\s*\\.\\s*invoke", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Field\\s*\\.\\s*set", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Constructor\\s*\\.\\s*newInstance", Pattern.CASE_INSENSITIVE),
        
        // Unsafe operations
        Pattern.compile("Unsafe", Pattern.CASE_INSENSITIVE),
        Pattern.compile("SecurityManager", Pattern.CASE_INSENSITIVE),
        Pattern.compile("AccessController", Pattern.CASE_INSENSITIVE),
        Pattern.compile("PrivilegedAction", Pattern.CASE_INSENSITIVE),
        
        // Threading (except basic usage)
        Pattern.compile("new\\s+Thread\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ExecutorService", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ThreadPoolExecutor", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ForkJoinPool", Pattern.CASE_INSENSITIVE),
        Pattern.compile("CompletableFuture\\s*\\.\\s*runAsync", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*start\\s*\\(\\s*\\)"), // Thread.start()
        
        // JNI/Native
        Pattern.compile("\\bnative\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("JNI", Pattern.CASE_INSENSITIVE),
        
        // Scripting engines
        Pattern.compile("ScriptEngine", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javax\\s*\\.\\s*script", Pattern.CASE_INSENSITIVE),
        
        // Serialization attacks
        Pattern.compile("ObjectInputStream", Pattern.CASE_INSENSITIVE),
        Pattern.compile("XMLDecoder", Pattern.CASE_INSENSITIVE),
        
        // JNDI
        Pattern.compile("InitialContext", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javax\\s*\\.\\s*naming", Pattern.CASE_INSENSITIVE)
    );
    
    // Allowed imports whitelist for additional validation
    private static final Set<String> ALLOWED_IMPORTS = Set.of(
        "java.util.*",
        "java.util.Arrays",
        "java.util.ArrayList",
        "java.util.LinkedList",
        "java.util.HashMap",
        "java.util.HashSet",
        "java.util.TreeMap",
        "java.util.TreeSet",
        "java.util.PriorityQueue",
        "java.util.Queue",
        "java.util.Deque",
        "java.util.ArrayDeque",
        "java.util.Stack",
        "java.util.List",
        "java.util.Map",
        "java.util.Set",
        "java.util.Collections",
        "java.util.Comparator",
        "java.util.stream.*",
        "java.util.stream.Stream",
        "java.util.stream.Collectors",
        "java.util.function.*",
        "java.lang.Math",
        "java.lang.String",
        "java.lang.StringBuilder",
        "java.lang.StringBuffer",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Character",
        "java.lang.Boolean"
    );

    public static String execute(String code, String input, String expectedOutput) {
        // Validate code length to prevent DoS
        if (code == null || code.isEmpty()) {
            return "ERROR: No code provided";
        }
        if (code.length() > MAX_CODE_LENGTH) {
            return "ERROR: Code exceeds maximum length of " + MAX_CODE_LENGTH + " characters";
        }
        
        // Security check - scan for dangerous patterns
        String securityError = checkCodeSecurity(code);
        if (securityError != null) {
            return securityError;
        }
        
        // Validate imports
        String importError = validateImports(code);
        if (importError != null) {
            return importError;
        }
        
        Path tempDir = null;
        URLClassLoader classLoader = null;
        
        try {
            // Get timeout from config, with fallback
            int timeoutMs = getTimeout();
            
            // Use standard Java Compiler API
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return "ERROR: Java Compiler not found. Please run with a JDK, not a JRE.";
            }

            tempDir = Files.createTempDirectory("algocraft_exec_");
            
            // Write source code to file
            File sourceFile = new File(tempDir.toFile(), "Solution.java");
            Files.writeString(sourceFile.toPath(), code);

            // Compile with diagnostic collector for better error messages
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
                StringWriter compileOutput = new StringWriter();
                
                JavaCompiler.CompilationTask task = compiler.getTask(
                    compileOutput,
                    fileManager,
                    diagnostics,
                    List.of("-d", tempDir.toString(), "-Xlint:none"),
                    null,
                    compilationUnits
                );
                
                boolean success = task.call();
                
                if (!success) {
                    StringBuilder errorMsg = new StringBuilder("ERROR: Compilation failed\n");
                    int errorCount = 0;
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            errorMsg.append("Line ").append(diagnostic.getLineNumber())
                                   .append(": ").append(diagnostic.getMessage(null)).append("\n");
                            errorCount++;
                            if (errorCount >= 3) {
                                errorMsg.append("... and more errors");
                                break;
                            }
                        }
                    }
                    String result = errorMsg.toString();
                    return result.length() > MAX_COMPILE_OUTPUT_SIZE 
                        ? result.substring(0, MAX_COMPILE_OUTPUT_SIZE) + "..." 
                        : result;
                }
            }

            // Load class with sandboxed classloader for security
            classLoader = new SandboxClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                CodeExecutor.class.getClassLoader()
            );
            
            Class<?> clazz = Class.forName("Solution", true, classLoader);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            
            // Find the first public method that isn't Object's methods
            Method method = findSolutionMethod(clazz);
            
            if (method == null) {
                return "ERROR: No public method found in Solution class";
            }

            final Object[] args = parseArgs(input, method.getParameterTypes());
            final Method finalMethod = method;
            final Object finalInstance = instance;
            
            // Execute with timeout
            Future<Object> future = EXECUTOR.submit(() -> {
                finalMethod.setAccessible(true);
                return finalMethod.invoke(finalInstance, args);
            });
            
            Object invokeResult;
            try {
                invokeResult = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return "ERROR: Time Limit Exceeded (>" + timeoutMs + "ms)";
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    // Unwrap InvocationTargetException
                    if (cause instanceof java.lang.reflect.InvocationTargetException) {
                        cause = cause.getCause();
                    }
                    String message = cause.getMessage();
                    if (message != null && message.length() > 200) {
                        message = message.substring(0, 200) + "...";
                    }
                    return "ERROR: " + cause.getClass().getSimpleName() + ": " + message;
                }
                return "ERROR: Execution failed";
            } catch (RejectedExecutionException e) {
                return "ERROR: Server busy, please try again later";
            }
            
            String resultStr = formatResult(invokeResult);
            
            // Normalize for comparison (remove spaces)
            String normResult = normalizeOutput(resultStr);
            String normExpected = normalizeOutput(expectedOutput);
            
            if (normResult.equals(normExpected)) {
                return "PASS";
            } else {
                return "FAIL: Expected " + expectedOutput + ", got " + resultStr;
            }
        } catch (Throwable e) {
            LOGGER.error("Code execution error", e);
            String message = e.getMessage();
            if (message != null && message.length() > 200) {
                message = message.substring(0, 200) + "...";
            }
            return "ERROR: " + e.getClass().getSimpleName() + ": " + message;
        } finally {
            // Close classloader first
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (Exception ignored) {}
            }
            
            // Cleanup temp dir with retry
            cleanupTempDir(tempDir);
        }
    }
    
    /**
     * Result of a single test case execution.
     */
    public static class TestResult {
        public final boolean passed;
        public final String message;
        public final String actualOutput;
        
        public TestResult(boolean passed, String message, String actualOutput) {
            this.passed = passed;
            this.message = message;
            this.actualOutput = actualOutput;
        }
        
        public boolean isCompileError() {
            return message != null && message.contains("Compilation failed");
        }
        
        public boolean isTimeLimit() {
            return message != null && message.contains("Time Limit Exceeded");
        }
        
        public boolean isRuntimeError() {
            return message != null && message.startsWith("ERROR:") && !isCompileError() && !isTimeLimit();
        }
    }
    
    /**
     * Test case input/output pair.
     */
    public static class TestCase {
        public final String input;
        public final String expectedOutput;
        
        public TestCase(String input, String expectedOutput) {
            this.input = input;
            this.expectedOutput = expectedOutput;
        }
    }
    
    /**
     * Execute code against multiple test cases efficiently.
     * Compiles once and runs all tests, avoiding repeated compilation overhead.
     * This is the recommended method for Judge to use.
     * 
     * @param code The user's source code
     * @param testCases List of test cases to run
     * @return List of results, one per test case
     */
    public static List<TestResult> executeBatch(String code, List<TestCase> testCases) {
        List<TestResult> results = new ArrayList<>();
        
        // Validate code length to prevent DoS
        if (code == null || code.isEmpty()) {
            TestResult error = new TestResult(false, "ERROR: No code provided", null);
            for (int i = 0; i < testCases.size(); i++) results.add(error);
            return results;
        }
        if (code.length() > MAX_CODE_LENGTH) {
            TestResult error = new TestResult(false, "ERROR: Code exceeds maximum length of " + MAX_CODE_LENGTH + " characters", null);
            for (int i = 0; i < testCases.size(); i++) results.add(error);
            return results;
        }
        
        // Security check - scan for dangerous patterns
        String securityError = checkCodeSecurity(code);
        if (securityError != null) {
            TestResult error = new TestResult(false, securityError, null);
            for (int i = 0; i < testCases.size(); i++) results.add(error);
            return results;
        }
        
        // Validate imports
        String importError = validateImports(code);
        if (importError != null) {
            TestResult error = new TestResult(false, importError, null);
            for (int i = 0; i < testCases.size(); i++) results.add(error);
            return results;
        }
        
        Path tempDir = null;
        URLClassLoader classLoader = null;
        
        try {
            int timeoutMs = getTimeout();
            
            // Use standard Java Compiler API
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                TestResult error = new TestResult(false, "ERROR: Java Compiler not found. Please run with a JDK, not a JRE.", null);
                for (int i = 0; i < testCases.size(); i++) results.add(error);
                return results;
            }

            tempDir = Files.createTempDirectory("algocraft_exec_");
            
            // Write source code to file
            File sourceFile = new File(tempDir.toFile(), "Solution.java");
            Files.writeString(sourceFile.toPath(), code);

            // Compile ONCE
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
                StringWriter compileOutput = new StringWriter();
                
                JavaCompiler.CompilationTask task = compiler.getTask(
                    compileOutput,
                    fileManager,
                    diagnostics,
                    List.of("-d", tempDir.toString(), "-Xlint:none"),
                    null,
                    compilationUnits
                );
                
                boolean success = task.call();
                
                if (!success) {
                    StringBuilder errorMsg = new StringBuilder("ERROR: Compilation failed\n");
                    int errorCount = 0;
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            errorMsg.append("Line ").append(diagnostic.getLineNumber())
                                   .append(": ").append(diagnostic.getMessage(null)).append("\n");
                            errorCount++;
                            if (errorCount >= 3) {
                                errorMsg.append("... and more errors");
                                break;
                            }
                        }
                    }
                    String result = errorMsg.toString();
                    if (result.length() > MAX_COMPILE_OUTPUT_SIZE) {
                        result = result.substring(0, MAX_COMPILE_OUTPUT_SIZE) + "...";
                    }
                    TestResult error = new TestResult(false, result, null);
                    for (int i = 0; i < testCases.size(); i++) results.add(error);
                    return results;
                }
            }

            // Load class ONCE with sandboxed classloader
            classLoader = new SandboxClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                CodeExecutor.class.getClassLoader()
            );
            
            Class<?> clazz = Class.forName("Solution", true, classLoader);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            
            Method method = findSolutionMethod(clazz);
            if (method == null) {
                TestResult error = new TestResult(false, "ERROR: No public method found in Solution class", null);
                for (int i = 0; i < testCases.size(); i++) results.add(error);
                return results;
            }
            method.setAccessible(true);
            
            // Run all test cases with the compiled class
            for (TestCase testCase : testCases) {
                results.add(runSingleTestCase(constructor, method, testCase, timeoutMs));
            }
            
            return results;
            
        } catch (Throwable e) {
            LOGGER.error("Batch code execution error", e);
            String message = e.getMessage();
            if (message != null && message.length() > 200) {
                message = message.substring(0, 200) + "...";
            }
            TestResult error = new TestResult(false, "ERROR: " + e.getClass().getSimpleName() + ": " + message, null);
            // Fill remaining results with error
            while (results.size() < testCases.size()) {
                results.add(error);
            }
            return results;
        } finally {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (Exception ignored) {}
            }
            cleanupTempDir(tempDir);
        }
    }
    
    /**
     * Run a single test case with the compiled class.
     */
    private static TestResult runSingleTestCase(Constructor<?> constructor, Method method, 
                                                 TestCase testCase, int timeoutMs) {
        try {
            Object instance = constructor.newInstance();
            final Object[] args = parseArgs(testCase.input, method.getParameterTypes());
            final Object finalInstance = instance;
            final Method finalMethod = method;
            
            Future<Object> future = EXECUTOR.submit(() -> finalMethod.invoke(finalInstance, args));
            
            Object invokeResult;
            try {
                invokeResult = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return new TestResult(false, "ERROR: Time Limit Exceeded (>" + timeoutMs + "ms)", null);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof java.lang.reflect.InvocationTargetException) {
                        cause = cause.getCause();
                    }
                    String message = cause.getMessage();
                    if (message != null && message.length() > 200) {
                        message = message.substring(0, 200) + "...";
                    }
                    return new TestResult(false, "ERROR: " + cause.getClass().getSimpleName() + ": " + message, null);
                }
                return new TestResult(false, "ERROR: Execution failed", null);
            } catch (RejectedExecutionException e) {
                return new TestResult(false, "ERROR: Server busy, please try again later", null);
            }
            
            String resultStr = formatResult(invokeResult);
            String normResult = normalizeOutput(resultStr);
            String normExpected = normalizeOutput(testCase.expectedOutput);
            
            if (normResult.equals(normExpected)) {
                return new TestResult(true, "PASS", resultStr);
            } else {
                return new TestResult(false, "FAIL: Expected " + testCase.expectedOutput + ", got " + resultStr, resultStr);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.length() > 200) {
                message = message.substring(0, 200) + "...";
            }
            return new TestResult(false, "ERROR: " + e.getClass().getSimpleName() + ": " + message, null);
        }
    }
    
    /**
     * Find the solution method in the class, excluding Object methods.
     */
    private static Method findSolutionMethod(Class<?> clazz) {
        Set<String> objectMethods = Set.of("equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait");
        for (Method m : clazz.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers()) 
                && !objectMethods.contains(m.getName())) {
                return m;
            }
        }
        return null;
    }
    
    /**
     * Check code for dangerous patterns.
     * Uses case-insensitive matching and handles whitespace variations.
     * @return error message if dangerous, null if safe
     */
    private static String checkCodeSecurity(String code) {
        // Remove comments before checking (to prevent hiding malicious code in comments)
        String codeWithoutComments = removeComments(code);
        
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            Matcher matcher = pattern.matcher(codeWithoutComments);
            if (matcher.find()) {
                LOGGER.warn("Blocked dangerous code pattern: {}", pattern.pattern());
                return "ERROR: Forbidden code pattern detected. System operations are not allowed.";
            }
        }
        return null;
    }
    
    // Pre-compiled patterns for comment removal (avoid recompilation on each call)
    private static final Pattern MULTI_LINE_COMMENT_PATTERN = 
        Pattern.compile("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/");
    private static final Pattern SINGLE_LINE_COMMENT_PATTERN = 
        Pattern.compile("//.*");
    
    /**
     * Remove single-line and multi-line comments from code.
     * Uses pre-compiled patterns for better performance.
     */
    private static String removeComments(String code) {
        // Remove multi-line comments
        code = MULTI_LINE_COMMENT_PATTERN.matcher(code).replaceAll(" ");
        // Remove single-line comments
        code = SINGLE_LINE_COMMENT_PATTERN.matcher(code).replaceAll(" ");
        return code;
    }
    
    // Pre-compiled pattern for import validation
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([^;]+);");
    
    // Pre-computed allowed base packages for faster lookup
    private static final Set<String> ALLOWED_BASE_PACKAGES;
    static {
        Set<String> packages = new HashSet<>();
        for (String ai : ALLOWED_IMPORTS) {
            if (ai.endsWith(".*")) {
                packages.add(ai.replace(".*", ""));
            }
        }
        ALLOWED_BASE_PACKAGES = Set.copyOf(packages);
    }
    
    /**
     * Validate that all imports are from the allowed list.
     * Uses pre-compiled pattern and optimized package lookup.
     * @return error message if invalid import found, null if all valid
     */
    private static String validateImports(String code) {
        Matcher matcher = IMPORT_PATTERN.matcher(code);
        
        while (matcher.find()) {
            String importStatement = matcher.group(1).trim();
            // Skip static imports for allowed packages
            if (importStatement.startsWith("static ")) {
                importStatement = importStatement.substring(7).trim();
                // Check if the static import is from an allowed package
                if (!isImportAllowed(importStatement)) {
                    LOGGER.warn("Blocked forbidden import: {}", importStatement);
                    return "ERROR: Forbidden import: " + importStatement;
                }
            } else if (!ALLOWED_IMPORTS.contains(importStatement)) {
                // Check if matches a wildcard pattern
                if (!isImportAllowed(importStatement)) {
                    LOGGER.warn("Blocked forbidden import: {}", importStatement);
                    return "ERROR: Forbidden import: " + importStatement;
                }
            }
        }
        return null;
    }
    
    /**
     * Check if an import is allowed using optimized package lookup.
     * Uses pre-computed base packages for O(n) instead of O(n*m) complexity.
     */
    private static boolean isImportAllowed(String importStatement) {
        // Check exact match first (O(1) for HashSet)
        if (ALLOWED_IMPORTS.contains(importStatement)) {
            return true;
        }
        // Check if matches any wildcard package
        for (String basePackage : ALLOWED_BASE_PACKAGES) {
            if (importStatement.startsWith(basePackage)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get timeout from config with fallback.
     */
    private static int getTimeout() {
        try {
            return Config.MAX_EXECUTION_TIME.get();
        } catch (Exception e) {
            return DEFAULT_TIMEOUT_MS;
        }
    }
    
    // Pre-compiled pattern for whitespace normalization
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    /**
     * Normalize output for comparison.
     * Uses pre-compiled pattern for better performance.
     */
    private static String normalizeOutput(String output) {
        if (output == null) return "";
        return WHITESPACE_PATTERN.matcher(output).replaceAll("").toLowerCase();
    }
    
    /**
     * Clean up temporary directory with retry logic.
     */
    private static void cleanupTempDir(Path tempDir) {
        if (tempDir != null) {
            // Schedule cleanup with slight delay to ensure classloader released the files
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100); // Brief delay for file handles to release
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                
                int maxRetries = 3;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        File dir = tempDir.toFile();
                        File[] files = dir.listFiles();
                        boolean allDeleted = true;
                        
                        if (files != null) {
                            for (File f : files) {
                                if (!f.delete()) {
                                    allDeleted = false;
                                    f.deleteOnExit();
                                }
                            }
                        }
                        
                        if (allDeleted && dir.delete()) {
                            return; // Success
                        }
                        
                        if (attempt < maxRetries - 1) {
                            Thread.sleep(50 * (attempt + 1)); // Exponential backoff
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        LOGGER.debug("Failed to cleanup temp dir (attempt {}): {}", attempt + 1, tempDir, e);
                    }
                }
                
                // Final fallback
                tempDir.toFile().deleteOnExit();
            });
        }
    }
    
    /**
     * Shutdown the executor service gracefully.
     * Should be called when the mod is unloading.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Parse input string into method arguments.
     * Supports: int, int[], int[][], long, double, boolean, String, String[], char, char[]
     * Input format: "nums = [2,7,11,15], target = 9"
     */
    private static Object[] parseArgs(String input, Class<?>[] types) {
        // Split by comma followed by variable name
        String[] parts = input.split(",\\s*(?=[a-zA-Z_][a-zA-Z0-9_]*\\s*=)");
        Object[] args = new Object[types.length];
        
        for (int i = 0; i < types.length && i < parts.length; i++) {
            String part = parts[i].trim();
            int eqIndex = part.indexOf('=');
            if (eqIndex == -1) continue;
            
            String valPart = part.substring(eqIndex + 1).trim();
            args[i] = parseValue(valPart, types[i]);
        }
        return args;
    }

    /**
     * Parse a single value into the target type.
     */
    private static Object parseValue(String val, Class<?> type) {
        val = val.trim();
        
        // Primitives and wrappers
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(val);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(val.replace("L", "").replace("l", ""));
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(val);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(val.replace("f", "").replace("F", ""));
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(val);
        }
        if (type == char.class || type == Character.class) {
            val = val.replace("'", "").replace("\"", "");
            return val.isEmpty() ? '\0' : val.charAt(0);
        }
        
        // String
        if (type == String.class) {
            if ((val.startsWith("\"") && val.endsWith("\"")) || 
                (val.startsWith("'") && val.endsWith("'"))) {
                return val.substring(1, val.length() - 1);
            }
            return val;
        }
        
        // 1D Arrays
        if (type == int[].class) {
            return parseIntArray(val);
        }
        if (type == long[].class) {
            return parseLongArray(val);
        }
        if (type == double[].class) {
            return parseDoubleArray(val);
        }
        if (type == char[].class) {
            return parseCharArray(val);
        }
        if (type == String[].class) {
            return parseStringArray(val);
        }
        if (type == boolean[].class) {
            return parseBooleanArray(val);
        }
        
        // 2D Arrays
        if (type == int[][].class) {
            return parse2DIntArray(val);
        }
        if (type == char[][].class) {
            return parse2DCharArray(val);
        }
        if (type == String[][].class) {
            return parse2DStringArray(val);
        }
        
        // List types
        if (type == List.class) {
            return Arrays.asList(toObjectArray(parseIntArray(val)));
        }
        
        LOGGER.warn("Unsupported parameter type: {}", type.getName());
        return null;
    }
    
    private static int[] parseIntArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new int[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new int[0];
        
        String[] nums = val.split(",");
        int[] arr = new int[nums.length];
        for (int i = 0; i < nums.length; i++) {
            arr[i] = Integer.parseInt(nums[i].trim());
        }
        return arr;
    }
    
    private static long[] parseLongArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new long[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new long[0];
        
        String[] nums = val.split(",");
        long[] arr = new long[nums.length];
        for (int i = 0; i < nums.length; i++) {
            arr[i] = Long.parseLong(nums[i].trim().replace("L", "").replace("l", ""));
        }
        return arr;
    }
    
    private static double[] parseDoubleArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new double[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new double[0];
        
        String[] nums = val.split(",");
        double[] arr = new double[nums.length];
        for (int i = 0; i < nums.length; i++) {
            arr[i] = Double.parseDouble(nums[i].trim());
        }
        return arr;
    }
    
    private static char[] parseCharArray(String val) {
        val = val.trim();
        if (val.equals("[]") || val.equals("\"\"")) return new char[0];
        
        if (val.startsWith("\"") && val.endsWith("\"")) {
            return val.substring(1, val.length() - 1).toCharArray();
        }
        
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new char[0];
        
        String[] chars = val.split(",");
        char[] arr = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            String c = chars[i].trim().replace("'", "").replace("\"", "");
            arr[i] = c.isEmpty() ? '\0' : c.charAt(0);
        }
        return arr;
    }
    
    private static String[] parseStringArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new String[0];
        
        if (val.startsWith("[") && val.endsWith("]")) {
            val = val.substring(1, val.length() - 1);
        }
        if (val.isEmpty()) return new String[0];
        
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';
        
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || val.charAt(i-1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        
        return result.toArray(new String[0]);
    }
    
    private static boolean[] parseBooleanArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new boolean[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new boolean[0];
        
        String[] bools = val.split(",");
        boolean[] arr = new boolean[bools.length];
        for (int i = 0; i < bools.length; i++) {
            arr[i] = Boolean.parseBoolean(bools[i].trim());
        }
        return arr;
    }
    
    private static int[][] parse2DIntArray(String val) {
        val = val.trim();
        if (val.equals("[]") || val.equals("[[]]")) return new int[0][];
        
        List<int[]> rows = new ArrayList<>();
        int depth = 0;
        int start = -1;
        
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c == '[') {
                depth++;
                if (depth == 2) start = i;
            } else if (c == ']') {
                if (depth == 2 && start != -1) {
                    rows.add(parseIntArray(val.substring(start, i + 1)));
                }
                depth--;
            }
        }
        
        return rows.toArray(new int[0][]);
    }
    
    private static char[][] parse2DCharArray(String val) {
        val = val.trim();
        if (val.equals("[]") || val.equals("[[]]")) return new char[0][];
        
        List<char[]> rows = new ArrayList<>();
        int depth = 0;
        int start = -1;
        
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c == '[') {
                depth++;
                if (depth == 2) start = i;
            } else if (c == ']') {
                if (depth == 2 && start != -1) {
                    rows.add(parseCharArray(val.substring(start, i + 1)));
                }
                depth--;
            }
        }
        
        return rows.toArray(new char[0][]);
    }
    
    private static String[][] parse2DStringArray(String val) {
        val = val.trim();
        if (val.equals("[]") || val.equals("[[]]")) return new String[0][];
        
        List<String[]> rows = new ArrayList<>();
        int depth = 0;
        int start = -1;
        
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c == '[') {
                depth++;
                if (depth == 2) start = i;
            } else if (c == ']') {
                if (depth == 2 && start != -1) {
                    rows.add(parseStringArray(val.substring(start, i + 1)));
                }
                depth--;
            }
        }
        
        return rows.toArray(new String[0][]);
    }
    
    private static Integer[] toObjectArray(int[] arr) {
        Integer[] result = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i];
        }
        return result;
    }

    /**
     * Format result object to string for comparison.
     */
    private static String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        if (result instanceof int[]) {
            return Arrays.toString((int[]) result);
        }
        if (result instanceof long[]) {
            return Arrays.toString((long[]) result);
        }
        if (result instanceof double[]) {
            return Arrays.toString((double[]) result);
        }
        if (result instanceof boolean[]) {
            return Arrays.toString((boolean[]) result);
        }
        if (result instanceof char[]) {
            return Arrays.toString((char[]) result);
        }
        
        if (result instanceof Object[]) {
            return Arrays.deepToString((Object[]) result);
        }
        
        if (result instanceof int[][]) {
            return Arrays.deepToString((int[][]) result);
        }
        if (result instanceof char[][]) {
            return Arrays.deepToString((char[][]) result);
        }
        
        if (result instanceof List) {
            return result.toString();
        }
        
        return String.valueOf(result);
    }
}
