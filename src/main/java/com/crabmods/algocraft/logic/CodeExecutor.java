package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.Config;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.MessageDigest;
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
    private static final System.Logger LOGGER = System.getLogger(CodeExecutor.class.getName());
    static final String TIMEOUT_PROPERTY = "algocraft.codeExecutorTimeoutMs";
    static final String WORKER_MAX_TIMEOUT_PROPERTY = "algocraft.codeExecutorWorkerMaxTimeoutMs";
    static final String WORKER_MODE_PROPERTY = "algocraft.codeExecutor.worker";
    static final String FORCE_ECJ_COMPILER_PROPERTY = "algocraft.codeExecutor.forceEcjCompiler";
    private static final int WORKER_STARTUP_GRACE_MS = 15_000;
    private static final int WORKER_MAX_TIMEOUT_MS = 45_000;
    private static final int WORKER_MAX_TIMEOUT_OVERRIDE_LIMIT_MS = 120_000;
    private static final String COMPILER_RELEASE = "17";
    private static final String ECJ_COMPILER_CLASS = "org.eclipse.jdt.internal.compiler.tool.EclipseCompiler";
    private static final String BUNDLED_ECJ_RESOURCE = "assets/algocraft/compiler/ecj.jar";
    private static final Pattern METHOD_DECLARATION_PATTERN = Pattern.compile(
            "\\b(?:(public|protected|private)\\s+)?(?:static\\s+)?[\\w<>\\[\\], ?]+\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
    private static final Gson GSON = new Gson();

    private static final java.util.concurrent.atomic.AtomicLong executionThreadIds =
            new java.util.concurrent.atomic.AtomicLong();

    // ─── Execution metrics (thread-safe counters) ─────────────────────────
    private static final java.util.concurrent.atomic.AtomicLong totalCompilations = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong totalExecutions = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong totalTimeouts = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong totalErrors = new java.util.concurrent.atomic.AtomicLong();

    /**
     * Return a snapshot of execution metrics.
     */
    public static Map<String, Long> getMetrics() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("compilations", totalCompilations.get());
        m.put("executions", totalExecutions.get());
        m.put("timeouts", totalTimeouts.get());
        m.put("errors", totalErrors.get());
        return m;
    }

    // ─── Periodic stale temp-dir cleanup ────────────────────────────────
    private static final ScheduledExecutorService CLEANUP_SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AlgoCraft-Cleanup");
            t.setDaemon(true);
            return t;
        });

    static {
        // Every 30 minutes, delete algocraft_exec_ dirs older than 1 hour
        CLEANUP_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                Path tmpRoot = Path.of(System.getProperty("java.io.tmpdir"));
                File[] dirs = tmpRoot.toFile().listFiles(
                    f -> f.isDirectory() && f.getName().startsWith("algocraft_exec_"));
                if (dirs == null) return;
                long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
                for (File dir : dirs) {
                    if (dir.lastModified() < cutoff) {
                        try {
                            deleteDirectoryTree(dir.toPath());
                        } catch (java.io.IOException e) {
                            LOGGER.log(System.Logger.Level.DEBUG,
                                    "Periodic cleanup: could not delete stale dir " + dir, e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.DEBUG, "Periodic temp cleanup error", e);
            }
        }, 30, 30, TimeUnit.MINUTES);
    }

    // Default timeout in milliseconds (used if config not available)
    private static final int DEFAULT_TIMEOUT_MS = 2000;
    private static final int MIN_TIMEOUT_MS = 2_000;

    // Maximum code length to prevent DoS attacks
    public static final int MAX_CODE_LENGTH = 50_000;

    // Maximum compilation output size
    private static final int MAX_COMPILE_OUTPUT_SIZE = 10_000;

    // Maximum user result text returned to the IDE/Judge UI.
    private static final int MAX_RESULT_OUTPUT_SIZE = 2_000;

    // Maximum stdout/stderr retained from the isolated worker JVM.
    static final int MAX_WORKER_STREAM_BYTES = 4 * 1024 * 1024;

    // Dangerous patterns that should be blocked in user code
    // Using case-insensitive matching and handling whitespace variations
    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
        // Process execution
        Pattern.compile("Runtime\\s*\\.\\s*getRuntime\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?Runtime\\s*::\\s*(?:getRuntime|version)\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("Runtime\\s*\\.\\s*exec", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ProcessBuilder", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Process\\s+", Pattern.CASE_INSENSITIVE),

        // System operations
        Pattern.compile("System\\s*\\.\\s*exit\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?System\\s*::\\s*"
                + "(?:exit|getenv|getProperties|getProperty|clearProperty|setProperties|setProperty|"
                + "setIn|setOut|setErr|setSecurityManager|load|loadLibrary|console|gc|runFinalization|"
                + "getLogger)\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*getenv\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*getProperties\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*getProperty\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*clearProperty\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setProperties\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setProperty", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setIn\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setOut\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setErr\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*setSecurityManager", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*load\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*loadLibrary", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*console", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*gc\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*runFinalization\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*getLogger\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*Logger\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\s*\\.\\s*(?:in|out|err)\\b", Pattern.CASE_INSENSITIVE),

        // JVM-global locale/timezone mutation
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*Locale\\s*\\.\\s*setDefault\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*util\\s*\\.\\s*)?Locale\\s*::\\s*setDefault\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bLocale\\s*\\.\\s*setDefault\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*TimeZone\\s*\\.\\s*setDefault\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*util\\s*\\.\\s*)?TimeZone\\s*::\\s*setDefault\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bTimeZone\\s*\\.\\s*setDefault\\s*\\(", Pattern.CASE_INSENSITIVE),

        // Network operations
        Pattern.compile("java\\s*\\.\\s*net\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javax\\s*\\.\\s*net\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*rmi\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Socket", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ServerSocket", Pattern.CASE_INSENSITIVE),
        Pattern.compile("URL\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("HttpClient", Pattern.CASE_INSENSITIVE),
        Pattern.compile("HttpURLConnection", Pattern.CASE_INSENSITIVE),

        // File operations
        Pattern.compile("java\\s*\\.\\s*nio\\s*\\.\\s*file\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("java\\s*\\.\\s*io\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*(?:jar|zip)\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("RandomAccessFile", Pattern.CASE_INSENSITIVE),
        Pattern.compile("FileChannel", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*Formatter\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bnew\\s+Formatter\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*util\\s*\\.\\s*)?Formatter\\s*::\\s*new\\b",
                Pattern.CASE_INSENSITIVE),

        // Reflection and classloading
        Pattern.compile("Class\\s*\\.\\s*forName\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?Class\\s*::\\s*forName\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("ClassLoader", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*lang\\s*\\.\\s*reflect\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*beans\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:\\.|\\b)\\s*getClass\\s*\\(\\s*\\)\\s*\\.\\s*getResource", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:\\.|\\b)\\s*getClass\\s*\\(\\s*\\)\\s*\\.\\s*getClassLoader", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:\\.|\\b)\\s*getClass\\s*\\(\\s*\\)\\s*\\.\\s*getProtectionDomain", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:\\.|\\b)\\s*getClass\\s*\\(\\s*\\)\\s*\\.\\s*getModule", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*getResource(?:AsStream)?\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*getProtectionDomain\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*getModule\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*class\\s*\\.\\s*getResource", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*class\\s*\\.\\s*getClassLoader", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*class\\s*\\.\\s*getProtectionDomain", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*class\\s*\\.\\s*getModule", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*getStackTrace\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*printStackTrace\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bStackTraceElement\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*get(?:Declared)?(?:Method|Methods|Field|Fields|Constructor|Constructors)\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*setAccessible\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*trySetAccessible\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*invoke\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Method\\s*\\.\\s*invoke", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Field\\s*\\.\\s*set", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Constructor\\s*\\.\\s*newInstance", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bStackWalker\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bModuleLayer\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bPackage\\s*\\.\\s*getPackages\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?Runtime\\s*\\.\\s*version\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*lang\\s*\\.\\s*ref\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*lang\\s*\\.\\s*foreign\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*time\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*text\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*security\\s*\\.", Pattern.CASE_INSENSITIVE),

        // Desktop/UI escape hatches
        Pattern.compile("\\bjava\\s*\\.\\s*awt\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjavax\\s*\\.\\s*swing\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjavafx\\s*\\.", Pattern.CASE_INSENSITIVE),

        // Classpath/resource discovery
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*ServiceLoader\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bServiceLoader\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*util\\s*\\.\\s*)?ServiceLoader\\s*::\\s*load\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*ResourceBundle\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bResourceBundle\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*util\\s*\\.\\s*)?ResourceBundle\\s*::\\s*getBundle\\b",
                Pattern.CASE_INSENSITIVE),

        // Unsafe operations
        Pattern.compile("Unsafe", Pattern.CASE_INSENSITIVE),
        Pattern.compile("SecurityManager", Pattern.CASE_INSENSITIVE),
        Pattern.compile("AccessController", Pattern.CASE_INSENSITIVE),
        Pattern.compile("PrivilegedAction", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*lang\\s*\\.\\s*invoke\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*lang\\s*\\.\\s*ProcessHandle\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bProcessHandle\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?ProcessHandle\\s*::\\s*current\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*lang\\s*\\.\\s*management\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bManagementFactory\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*lang\\s*\\.\\s*management\\s*\\.\\s*)?ManagementFactory\\s*::\\s*"
                + "(?:getRuntimeMXBean|getThreadMXBean|getOperatingSystemMXBean|getMemoryMXBean)\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjavax\\s*\\.\\s*management\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*logging\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*prefs\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*sql\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjavax\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsun\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjdk\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcom\\s*\\.\\s*sun\\s*\\.", Pattern.CASE_INSENSITIVE),

        // Threading (except basic usage)
        Pattern.compile("\\bnew\\s+(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?"
                + "(?:Thread|ThreadGroup|ThreadLocal|InheritableThreadLocal)\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?Thread\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?"
                + "(?:ThreadGroup|ThreadLocal|InheritableThreadLocal)\\s*\\.",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*lang\\s*\\.\\s*)?Thread\\s*::\\s*(?:new|sleep|yield|currentThread|onSpinWait)\\b",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("ExecutorService", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ThreadPoolExecutor", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ForkJoinPool", Pattern.CASE_INSENSITIVE),
        Pattern.compile("CompletableFuture\\s*\\.\\s*runAsync", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*concurrent\\s*\\.", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*parallelStream\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\s*parallel\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*Timer\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjava\\s*\\.\\s*util\\s*\\.\\s*TimerTask\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bnew\\s+Timer\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bnew\\s+TimerTask\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:java\\s*\\.\\s*util\\s*\\.\\s*)?Timer(?:Task)?\\s*::\\s*new\\b",
                Pattern.CASE_INSENSITIVE),
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
        Pattern.compile("javax\\s*\\.\\s*naming", Pattern.CASE_INSENSITIVE),

        // MethodHandle-based reflection (bypasses Class.forName block)
        Pattern.compile("MethodHandles", Pattern.CASE_INSENSITIVE),
        Pattern.compile("MethodHandle", Pattern.CASE_INSENSITIVE),
        Pattern.compile("VarHandle", Pattern.CASE_INSENSITIVE),

        // Instrumentation
        Pattern.compile("java\\.lang\\.instrument", Pattern.CASE_INSENSITIVE),

        // Compiler abuse (prevent user code from spawning its own compiler)
        Pattern.compile("javax\\.tools\\.ToolProvider", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javax\\.tools\\.JavaCompiler", Pattern.CASE_INSENSITIVE)
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
        "java.math.BigInteger",
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

    // ─── Helper class source code for injection ────────────────────────────

    private static final String TREE_NODE_SOURCE =
            "public class TreeNode {\n" +
            "    public int val;\n" +
            "    public TreeNode left;\n" +
            "    public TreeNode right;\n" +
            "    public TreeNode() {}\n" +
            "    public TreeNode(int val) { this.val = val; }\n" +
            "    public TreeNode(int val, TreeNode left, TreeNode right) {\n" +
            "        this.val = val; this.left = left; this.right = right;\n" +
            "    }\n" +
            "}\n";

    private static final String LIST_NODE_SOURCE =
            "public class ListNode {\n" +
            "    public int val;\n" +
            "    public ListNode next;\n" +
            "    public ListNode() {}\n" +
            "    public ListNode(int val) { this.val = val; }\n" +
            "    public ListNode(int val, ListNode next) {\n" +
            "        this.val = val; this.next = next;\n" +
            "    }\n" +
            "}\n";

    private static final String GRAPH_NODE_SOURCE =
            "import java.util.*;\n" +
            "public class Node {\n" +
            "    public int val;\n" +
            "    public List<Node> neighbors;\n" +
            "    public Node() { val = 0; neighbors = new ArrayList<>(); }\n" +
            "    public Node(int val) { this.val = val; neighbors = new ArrayList<>(); }\n" +
            "    public Node(int val, List<Node> neighbors) { this.val = val; this.neighbors = neighbors; }\n" +
            "}\n";

    private static final String NARY_NODE_SOURCE =
            "import java.util.*;\n" +
            "public class Node {\n" +
            "    public int val;\n" +
            "    public List<Node> children;\n" +
            "    public Node() { children = new ArrayList<>(); }\n" +
            "    public Node(int val) { this.val = val; children = new ArrayList<>(); }\n" +
            "    public Node(int val, List<Node> children) { this.val = val; this.children = children; }\n" +
            "}\n";

    private static final String RANDOM_POINTER_NODE_SOURCE =
            "public class Node {\n" +
            "    public int val;\n" +
            "    public Node next;\n" +
            "    public Node random;\n" +
            "    public Node(int val) { this.val = val; this.next = null; this.random = null; }\n" +
            "}\n";

    private static final String VERSION_CONTROL_SOURCE =
            "public class VersionControl {\n" +
            "    private int badVersion = 1;\n" +
            "    public void setBadVersion(int bad) { this.badVersion = bad; }\n" +
            "    public boolean isBadVersion(int version) { return version >= badVersion; }\n" +
            "}\n";

    // Pre-compiled pattern for class name detection
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
    private static final Set<String> HELPER_CLASS_NAMES = Set.of("TreeNode", "ListNode", "Node", "VersionControl");

    /**
     * Inject import statements if not already present.
     */
    private static String injectImports(String code) {
        if (!code.contains("import java.util.*")) {
            code = "import java.util.*;\nimport java.util.stream.*;\n" + code;
        }
        return code;
    }

    /**
     * Detect the actual class name from source code (handles design classes like MinStack, LRUCache).
     */
    private static String detectClassName(String code) {
        String codeNoComments = removeComments(code);
        Matcher cm = CLASS_NAME_PATTERN.matcher(codeNoComments);
        while (cm.find()) {
            String name = cm.group(1);
            if (!HELPER_CLASS_NAMES.contains(name)) {
                return name;
            }
        }
        return "Solution";
    }

    /**
     * Check if user code already defines a class (not just references it in comments).
     */
    private static boolean codeDefinesClass(String code, String className) {
        String codeNoComments = removeComments(code);
        return codeNoComments.contains("class " + className);
    }

    /**
     * Write a helper source file to the temp directory.
     */
    private static File writeHelperSource(Path tempDir, String className, String source) throws java.io.IOException {
        File file = new File(tempDir.toFile(), className + ".java");
        Files.writeString(file.toPath(), source);
        return file;
    }

    /**
     * Prepare all source files for compilation: inject imports, write helper classes, write main source.
     *
     * @param code the user's source code (will have imports injected)
     * @param tempDir the temporary directory for compilation
     * @return array where [0] = modified code, [1] = class name; source files are written to tempDir
     */
    private static List<File> prepareSourceFiles(String code, String className, Path tempDir) throws java.io.IOException {
        List<File> sourceFiles = new ArrayList<>();

        // Write helper classes if referenced but not defined by user
        if (code.contains("TreeNode") && !codeDefinesClass(code, "TreeNode")) {
            sourceFiles.add(writeHelperSource(tempDir, "TreeNode", TREE_NODE_SOURCE));
        }
        if (code.contains("ListNode") && !codeDefinesClass(code, "ListNode")) {
            sourceFiles.add(writeHelperSource(tempDir, "ListNode", LIST_NODE_SOURCE));
        }
        if (code.contains("Node") && !codeDefinesClass(code, "Node")
                && !codeDefinesClass(code, "TreeNode") && !codeDefinesClass(code, "ListNode")) {
            if (code.contains("children")) {
                sourceFiles.add(writeHelperSource(tempDir, "Node", NARY_NODE_SOURCE));
            } else if (code.contains("random")) {
                sourceFiles.add(writeHelperSource(tempDir, "Node", RANDOM_POINTER_NODE_SOURCE));
            } else if (code.contains("neighbors")) {
                sourceFiles.add(writeHelperSource(tempDir, "Node", GRAPH_NODE_SOURCE));
            }
        }
        if (code.contains("extends VersionControl")) {
            sourceFiles.add(writeHelperSource(tempDir, "VersionControl", VERSION_CONTROL_SOURCE));
        }

        // Write main source file
        File sourceFile = new File(tempDir.toFile(), className + ".java");
        Files.writeString(sourceFile.toPath(), code);
        sourceFiles.add(sourceFile);

        return sourceFiles;
    }

    /**
     * Set up parent class state if needed (e.g., VersionControl.setBadVersion for First Bad Version).
     */
    private static void setupParentClass(Object instance, String input) {
        try {
            Matcher m = Pattern.compile("bad\\s*=\\s*(\\d+)").matcher(input);
            if (m.find()) {
                int badVersion = Integer.parseInt(m.group(1));
                Method setBad = instance.getClass().getMethod("setBadVersion", int.class);
                setBad.setAccessible(true);
                setBad.invoke(instance, badVersion);
            }
        } catch (Exception ignored) {
            // Not all classes have these methods
        }
    }

    public static String execute(String code, String input, String expectedOutput) {
        if (runsInsideWorkerProcess()) {
            return executeInProcess(code, input, expectedOutput);
        }
        String envelopeError = validateCodeEnvelope(code);
        if (envelopeError != null) {
            return envelopeError;
        }
        WorkerRequest request = WorkerRequest.single(code, input, expectedOutput);
        WorkerResponse response = runWorker(request, workerTimeoutMs(1));
        if (response.error != null) {
            return response.error;
        }
        return response.singleResult == null ? "ERROR: Worker returned no result" : response.singleResult;
    }

    static String executeInProcess(String code, String input, String expectedOutput) {
        String envelopeError = validateCodeEnvelope(code);
        if (envelopeError != null) {
            return envelopeError;
        }

        code = translateJavaUnicodeEscapes(code);

        // Validate imports
        String importError = validateImports(code);
        if (importError != null) {
            return importError;
        }

        // Security check - scan for dangerous patterns
        String securityError = checkCodeSecurity(code);
        if (securityError != null) {
            return securityError;
        }

        Path tempDir = null;
        URLClassLoader classLoader = null;

        try {
            // Get timeout from config, with fallback
            int timeoutMs = getTimeout();

            // Use standard Java Compiler API
            JavaCompiler compiler = createCompiler();
            if (compiler == null) {
                return compilerUnavailableMessage();
            }

            tempDir = Files.createTempDirectory("algocraft_exec_");
            totalCompilations.incrementAndGet();

            // Prepare source: inject imports, write helper classes, detect class name
            code = injectImports(code);
            String className = detectClassName(code);
            List<File> sourceFiles = prepareSourceFiles(code, className, tempDir);

            // Compile with diagnostic collector for better error messages
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
                StringWriter compileOutput = new StringWriter();

                JavaCompiler.CompilationTask task = compiler.getTask(
                    compileOutput,
                    fileManager,
                    diagnostics,
                    compilerOptions(compiler, tempDir),
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

            // Load and execute through the bounded executor so class initializers,
            // constructors, and method bodies share the same timeout boundary.
            classLoader = new SandboxClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                CodeExecutor.class.getClassLoader()
            );
            URLClassLoader executionClassLoader = classLoader;
            String finalClassName = className;

            totalExecutions.incrementAndGet();
            return runStringWithTimeout(() -> {
                Class<?> clazz = Class.forName(finalClassName, true, executionClassLoader);

                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object instance = constructor.newInstance();

                setupParentClass(instance, input);

                Method method = findSolutionMethod(clazz, input);
                if (method == null) {
                    return "ERROR: No public method found in " + finalClassName + " class";
                }

                method.setAccessible(true);
                Object[] args = parseArgs(input, method.getParameterTypes());
                Object invokeResult = method.invoke(instance, args);
                String resultStr = formatMethodResult(method, invokeResult, args, expectedOutput);
                boolean specialRequired = requiresSpecialOutputMatch(method, null);
                boolean specialPassed = specialOutputMatches(method, invokeResult, args, input, null, expectedOutput);

                return (specialRequired && specialPassed)
                        || (!specialRequired && (outputsMatch(resultStr, expectedOutput, method.getReturnType())
                        || specialPassed))
                        ? "PASS"
                        : wrongAnswerMessage(expectedOutput, resultStr);
            }, timeoutMs);
        } catch (Throwable e) {
            LOGGER.log(System.Logger.Level.ERROR, "Code execution error", e);
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
        public final String problemId;
        public final String preferredMethodName;

        public TestCase(String input, String expectedOutput) {
            this(input, expectedOutput, null);
        }

        public TestCase(String input, String expectedOutput, String problemId) {
            this(input, expectedOutput, problemId, null);
        }

        public TestCase(String input, String expectedOutput, String problemId, String preferredMethodName) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.problemId = problemId;
            this.preferredMethodName = preferredMethodName;
        }
    }

    static class WorkerRequest {
        String mode;
        String code;
        String input;
        String expectedOutput;
        List<WorkerTestCase> testCases;
        int timeoutMs;

        static WorkerRequest single(String code, String input, String expectedOutput) {
            WorkerRequest request = new WorkerRequest();
            request.mode = "single";
            request.code = code;
            request.input = input;
            request.expectedOutput = expectedOutput;
            request.timeoutMs = getTimeout();
            return request;
        }

        static WorkerRequest batch(String code, List<TestCase> testCases) {
            WorkerRequest request = new WorkerRequest();
            request.mode = "batch";
            request.code = code;
            request.testCases = new ArrayList<>();
            if (testCases != null) {
                for (TestCase testCase : testCases) {
                    request.testCases.add(new WorkerTestCase(
                            testCase.input,
                            testCase.expectedOutput,
                            testCase.problemId,
                            testCase.preferredMethodName
                    ));
                }
            }
            request.timeoutMs = getTimeout();
            return request;
        }

        List<TestCase> toTestCases() {
            List<TestCase> converted = new ArrayList<>();
            if (testCases != null) {
                for (WorkerTestCase testCase : testCases) {
                    converted.add(new TestCase(
                            testCase.input,
                            testCase.expectedOutput,
                            testCase.problemId,
                            testCase.preferredMethodName
                    ));
                }
            }
            return converted;
        }
    }

    static class WorkerResponse {
        String singleResult;
        List<WorkerTestResult> batchResults;
        String error;

        static WorkerResponse single(String result) {
            WorkerResponse response = new WorkerResponse();
            response.singleResult = result;
            return response;
        }

        static WorkerResponse batch(List<TestResult> results) {
            WorkerResponse response = new WorkerResponse();
            response.batchResults = new ArrayList<>();
            for (TestResult result : results) {
                response.batchResults.add(new WorkerTestResult(result.passed, result.message, result.actualOutput));
            }
            return response;
        }

        static WorkerResponse error(String message) {
            WorkerResponse response = new WorkerResponse();
            response.error = message;
            return response;
        }

        List<TestResult> toTestResults() {
            List<TestResult> converted = new ArrayList<>();
            if (batchResults != null) {
                for (WorkerTestResult result : batchResults) {
                    converted.add(new TestResult(result.passed, result.message, result.actualOutput));
                }
            }
            return converted;
        }
    }

    static class WorkerTestCase {
        String input;
        String expectedOutput;
        String problemId;
        String preferredMethodName;

        WorkerTestCase() {
        }

        WorkerTestCase(String input, String expectedOutput) {
            this(input, expectedOutput, null);
        }

        WorkerTestCase(String input, String expectedOutput, String problemId) {
            this(input, expectedOutput, problemId, null);
        }

        WorkerTestCase(String input, String expectedOutput, String problemId, String preferredMethodName) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.problemId = problemId;
            this.preferredMethodName = preferredMethodName;
        }
    }

    static class WorkerTestResult {
        boolean passed;
        String message;
        String actualOutput;

        WorkerTestResult() {
        }

        WorkerTestResult(boolean passed, String message, String actualOutput) {
            this.passed = passed;
            this.message = message;
            this.actualOutput = actualOutput;
        }
    }

    /**
     * Execute code against multiple test cases efficiently.
     * Compiles once and runs tests until completion or the first timeout.
     * This is the recommended method for Judge to use.
     *
     * @param code The user's source code
     * @param testCases List of test cases to run
     * @return List of results, one per test case
     */
    public static List<TestResult> executeBatch(String code, List<TestCase> testCases) {
        if (runsInsideWorkerProcess()) {
            return executeBatchInProcess(code, testCases);
        }
        List<TestResult> envelopeErrors = validateCodeEnvelopeForBatch(code, testCases);
        if (envelopeErrors != null) {
            return envelopeErrors;
        }
        List<TestResult> testCaseErrors = validateBatchTestCases(testCases);
        if (testCaseErrors != null) {
            return testCaseErrors;
        }
        WorkerRequest request = WorkerRequest.batch(code, testCases);
        WorkerResponse response = runWorker(request, workerTimeoutMs(testCases == null ? 1 : testCases.size()));
        if (response.error != null) {
            return repeatedError(testCases, response.error);
        }
        return normalizeWorkerBatchResults(testCases, response);
    }

    static List<TestResult> normalizeWorkerBatchResultsForTest(List<TestCase> testCases, WorkerResponse response) {
        return normalizeWorkerBatchResults(testCases, response);
    }

    private static List<TestResult> normalizeWorkerBatchResults(List<TestCase> testCases, WorkerResponse response) {
        if (response == null || response.batchResults == null) {
            return repeatedError(testCases, "ERROR: Worker returned no batch result");
        }
        for (int i = 0; i < response.batchResults.size(); i++) {
            if (response.batchResults.get(i) == null) {
                return repeatedError(testCases, "ERROR: Worker returned null result at index " + i);
            }
        }
        List<TestResult> results = response.toTestResults();
        int expectedCount = testCases == null ? 0 : testCases.size();
        if (results.size() != expectedCount) {
            return repeatedError(testCases, "ERROR: Worker returned " + results.size()
                    + " results for " + expectedCount + " test cases");
        }
        return results;
    }

    static List<TestResult> executeBatchInProcess(String code, List<TestCase> testCases) {
        List<TestResult> envelopeErrors = validateCodeEnvelopeForBatch(code, testCases);
        if (envelopeErrors != null) {
            return envelopeErrors;
        }
        List<TestResult> testCaseErrors = validateBatchTestCases(testCases);
        if (testCaseErrors != null) {
            return testCaseErrors;
        }

        List<TestResult> results = new ArrayList<>();

        code = translateJavaUnicodeEscapes(code);

        // Validate imports
        String importError = validateImports(code);
        if (importError != null) {
            TestResult error = new TestResult(false, importError, null);
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

        Path tempDir = null;
        URLClassLoader classLoader = null;

        try {
            int timeoutMs = getTimeout();

            // Use standard Java Compiler API
            JavaCompiler compiler = createCompiler();
            if (compiler == null) {
                TestResult error = new TestResult(false, compilerUnavailableMessage(), null);
                for (int i = 0; i < testCases.size(); i++) results.add(error);
                return results;
            }

            tempDir = Files.createTempDirectory("algocraft_exec_");
            totalCompilations.incrementAndGet();

            // Prepare source: inject imports, write helper classes, detect class name
            code = injectImports(code);
            String className = detectClassName(code);
            List<File> sourceFiles = prepareSourceFiles(code, className, tempDir);

            // Compile ONCE
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
                StringWriter compileOutput = new StringWriter();

                JavaCompiler.CompilationTask task = compiler.getTask(
                    compileOutput,
                    fileManager,
                    diagnostics,
                    compilerOptions(compiler, tempDir),
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

            // Load class ONCE with sandboxed classloader and timeout protection.
            classLoader = new SandboxClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                CodeExecutor.class.getClassLoader()
            );
            ClassLoadResult classLoad = loadClassWithTimeout(className, classLoader, timeoutMs);
            if (classLoad.error() != null) {
                for (int i = 0; i < testCases.size(); i++) {
                    results.add(classLoad.error());
                }
                return results;
            }
            Class<?> clazz = classLoad.clazz();

            if (shouldUseDesignClassRunner(className, testCases)) {
                for (TestCase testCase : testCases) {
                    TestResult testResult = runDesignTestCase(clazz, testCase, timeoutMs);
                    if (appendAndStopAfterTimeout(results, testResult, testCases.size(), timeoutMs)) {
                        break;
                    }
                }
                return results;
            }

            if (isCodecClass(clazz)) {
                for (TestCase testCase : testCases) {
                    TestResult testResult = runCodecTestCase(clazz, testCase, timeoutMs);
                    if (appendAndStopAfterTimeout(results, testResult, testCases.size(), timeoutMs)) {
                        break;
                    }
                }
                return results;
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);

            String firstTestInput = testCases.isEmpty() ? "" : testCases.get(0).input;
            Method method = findSolutionMethod(clazz, firstTestInput, preferredMethodName(testCases));
            if (method == null) {
                TestResult error = new TestResult(false, "ERROR: No public method found in " + className + " class", null);
                for (int i = 0; i < testCases.size(); i++) results.add(error);
                return results;
            }
            method.setAccessible(true);

            // Run all test cases with the compiled class
            for (TestCase testCase : testCases) {
                TestResult testResult = runSingleTestCase(constructor, method, testCase, timeoutMs);
                if (appendAndStopAfterTimeout(results, testResult, testCases.size(), timeoutMs)) {
                    break;
                }
            }

            return results;

        } catch (Throwable e) {
            LOGGER.log(System.Logger.Level.ERROR, "Batch code execution error", e);
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

    private static boolean runsInsideWorkerProcess() {
        return Boolean.getBoolean(WORKER_MODE_PROPERTY);
    }

    private static boolean appendAndStopAfterTimeout(List<TestResult> results, TestResult testResult,
                                                     int totalCount, int timeoutMs) {
        results.add(testResult);
        if (!testResult.isTimeLimit()) {
            return false;
        }
        TestResult skipped = new TestResult(
                false,
                "ERROR: Skipped after previous Time Limit Exceeded (>" + timeoutMs + "ms)",
                null
        );
        while (results.size() < totalCount) {
            results.add(skipped);
        }
        return true;
    }

    private static JavaCompiler createCompiler() {
        if (!Boolean.getBoolean(FORCE_ECJ_COMPILER_PROPERTY)) {
            JavaCompiler systemCompiler = ToolProvider.getSystemJavaCompiler();
            if (systemCompiler != null) {
                return systemCompiler;
            }
        }
        try {
            Object compiler = Class.forName(ECJ_COMPILER_CLASS).getDeclaredConstructor().newInstance();
            if (compiler instanceof JavaCompiler javaCompiler) {
                return javaCompiler;
            }
            LOGGER.log(System.Logger.Level.ERROR, "ECJ compiler fallback does not implement JavaCompiler: "
                    + compiler.getClass().getName());
            return null;
        } catch (Throwable throwable) {
            LOGGER.log(System.Logger.Level.ERROR, "ECJ compiler fallback is unavailable", throwable);
            return null;
        }
    }

    private static String compilerUnavailableMessage() {
        return "ERROR: Java Compiler not found. AlgoCraft needs its bundled ECJ compiler or a JDK with javac.";
    }

    private static List<String> compilerOptions(JavaCompiler compiler, Path tempDir) {
        List<String> options = new ArrayList<>();
        options.add("-d");
        options.add(tempDir.toString());
        options.add("--release");
        options.add(COMPILER_RELEASE);
        if (ECJ_COMPILER_CLASS.equals(compiler.getClass().getName())) {
            options.add("-warn:none");
        } else {
            options.add("-Xlint:none");
        }
        return options;
    }

    private static int workerTimeoutMs(int testCount) {
        long timeoutMs = getTimeout();
        long budget = WORKER_STARTUP_GRACE_MS + timeoutMs * (Math.max(1, testCount) + 2L);
        return (int) Math.min(Math.max(budget, timeoutMs + WORKER_STARTUP_GRACE_MS), workerMaxTimeout());
    }

    private static int workerMaxTimeout() {
        String override = System.getProperty(WORKER_MAX_TIMEOUT_PROPERTY);
        if (override != null && !override.isBlank()) {
            try {
                int value = Integer.parseInt(override.trim());
                if (value >= WORKER_MAX_TIMEOUT_MS) {
                    return Math.min(value, WORKER_MAX_TIMEOUT_OVERRIDE_LIMIT_MS);
                }
            } catch (NumberFormatException ignored) {
                // Fall through to the default hard cap.
            }
        }
        return WORKER_MAX_TIMEOUT_MS;
    }

    private static WorkerResponse runWorker(WorkerRequest request, int timeoutMs) {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(workerCommand(request.timeoutMs));
            processBuilder.directory(Path.of(System.getProperty("user.dir", ".")).toFile());
            process = processBuilder.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                GSON.toJson(request, writer);
            }

            CompletableFuture<WorkerStreamOutput> stdout = readProcessStream(process.getInputStream());
            CompletableFuture<WorkerStreamOutput> stderr = readProcessStream(process.getErrorStream());
            boolean exited = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
                totalTimeouts.incrementAndGet();
                return WorkerResponse.error("ERROR: Time Limit Exceeded (>" + request.timeoutMs + "ms)");
            }

            WorkerStreamOutput workerOutput = stdout.get(2, TimeUnit.SECONDS);
            WorkerStreamOutput workerErrors = stderr.get(2, TimeUnit.SECONDS);
            String output = workerOutput.text().trim();
            String errors = workerErrors.text().trim();
            if (workerOutput.truncated()) {
                return WorkerResponse.error("ERROR: Worker output exceeded " + MAX_WORKER_STREAM_BYTES + " bytes");
            }
            if (process.exitValue() != 0) {
                return WorkerResponse.error("ERROR: Worker exited with code " + process.exitValue() + ": "
                        + abbreviate(errors) + (workerErrors.truncated() ? "... [truncated]" : ""));
            }
            if (output.isBlank()) {
                return WorkerResponse.error("ERROR: Worker returned no output" + (errors.isBlank() ? "" : ": "
                        + abbreviate(errors) + (workerErrors.truncated() ? "... [truncated]" : "")));
            }
            WorkerResponse response = GSON.fromJson(output, WorkerResponse.class);
            return response == null ? WorkerResponse.error("ERROR: Worker returned invalid output") : response;
        } catch (Throwable e) {
            totalErrors.incrementAndGet();
            return WorkerResponse.error("ERROR: Worker failed: " + e.getClass().getSimpleName() + ": " + safeMessage(e));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static List<String> workerCommand(int timeoutMs) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-Xmx256m");
        // Java 17 switches hot reflection calls to generated accessor classes.
        // Keep the trusted judge on native reflection instead of exposing JDK
        // internal accessor classes through the sandbox's restricted loader.
        command.add("-Dsun.reflect.noInflation=false");
        command.add("-Dsun.reflect.inflationThreshold=2147483647");
        command.add("-D" + TIMEOUT_PROPERTY + "=" + timeoutMs);
        command.add("-D" + WORKER_MODE_PROPERTY + "=true");
        if (Boolean.getBoolean(FORCE_ECJ_COMPILER_PROPERTY)) {
            command.add("-D" + FORCE_ECJ_COMPILER_PROPERTY + "=true");
        }
        command.add("-cp");
        command.add(workerClasspath());
        command.add(CodeExecutorWorker.class.getName());
        return command;
    }

    static CompletableFuture<WorkerStreamOutput> readProcessStream(InputStream inputStream) {
        return readProcessStream(inputStream, MAX_WORKER_STREAM_BYTES);
    }

    static CompletableFuture<WorkerStreamOutput> readProcessStream(InputStream inputStream, int maxBytes) {
        return CompletableFuture.supplyAsync(() -> {
            int limit = Math.max(0, maxBytes);
            try (InputStream stream = inputStream;
                 ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 8192))) {
                byte[] buffer = new byte[8192];
                int retained = 0;
                boolean truncated = false;
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    int allowed = Math.max(0, limit - retained);
                    int copied = Math.min(read, allowed);
                    if (copied > 0) {
                        output.write(buffer, 0, copied);
                        retained += copied;
                    }
                    if (copied < read) {
                        truncated = true;
                    }
                }
                return new WorkerStreamOutput(new String(output.toByteArray(), StandardCharsets.UTF_8), truncated);
            } catch (Exception e) {
                return new WorkerStreamOutput("", false);
            }
        });
    }

    record WorkerStreamOutput(String text, boolean truncated) {
    }

    private static String javaExecutable() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        Path java = javaHome.resolve("bin").resolve(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe"
                : "java");
        return java.toString();
    }

    private static String workerClasspath() {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        String currentClasspath = System.getProperty("java.class.path", "");
        if (!currentClasspath.isBlank()) {
            entries.addAll(Arrays.asList(currentClasspath.split(Pattern.quote(File.pathSeparator))));
        }
        addCodeSource(entries, CodeExecutor.class);
        addCodeSource(entries, CodeExecutorWorker.class);
        addCodeSource(entries, Gson.class);
        addCodeSource(entries, JsonParser.class);
        addCodeSource(entries, ECJ_COMPILER_CLASS);
        addBundledEcjCompilerJar(entries);
        return String.join(File.pathSeparator, entries);
    }

    private static void addCodeSource(Set<String> entries, String className) {
        try {
            addCodeSource(entries, Class.forName(className));
        } catch (Throwable ignored) {
            // The bundled compiler jar resource below is the fallback for plain worker JVMs.
        }
    }

    private static void addCodeSource(Set<String> entries, Class<?> clazz) {
        try {
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                addCodeSourceLocation(entries, codeSource.getLocation());
            }
        } catch (RuntimeException | URISyntaxException ignored) {
            // Existing classpath entries remain the source of truth if a code source is not file-backed.
        }
    }

    private static void addCodeSourceLocation(Set<String> entries, URL location) throws URISyntaxException {
        if ("file".equalsIgnoreCase(location.getProtocol())) {
            entries.add(Path.of(location.toURI()).toString());
            return;
        }

        String external = location.toExternalForm();
        String fileBacked = extractEmbeddedJarPath(external);
        if (fileBacked != null) {
            entries.add(fileBacked);
        }
    }

    private static String extractEmbeddedJarPath(String external) {
        String value = external;
        if (value.startsWith("union:")) {
            value = value.substring("union:".length());
        } else if (value.startsWith("jar:file:")) {
            value = value.substring("jar:file:".length());
        } else if (value.startsWith("file:")) {
            value = value.substring("file:".length());
        }

        int jarEnd = value.toLowerCase(Locale.ROOT).indexOf(".jar");
        if (jarEnd < 0) {
            return null;
        }
        value = value.substring(0, jarEnd + ".jar".length());
        value = URLDecoder.decode(value, StandardCharsets.UTF_8);
        if (value.matches("^/[A-Za-z]:/.*")) {
            value = value.substring(1);
        }
        try {
            Path path = Path.of(value);
            return Files.exists(path) ? path.toString() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void addBundledEcjCompilerJar(Set<String> entries) {
        Path ecjJar = extractBundledEcjCompilerJar();
        if (ecjJar != null) {
            entries.add(ecjJar.toString());
        }
    }

    private static Path extractBundledEcjCompilerJar() {
        try (InputStream input = CodeExecutor.class.getClassLoader().getResourceAsStream(BUNDLED_ECJ_RESOURCE)) {
            if (input == null) {
                return null;
            }
            byte[] jarBytes = input.readAllBytes();
            String hash = sha256Hex(jarBytes);
            Path dir = Path.of(System.getProperty("java.io.tmpdir"), "algocraft_compiler");
            Files.createDirectories(dir);
            Path target = dir.resolve("ecj-" + hash.substring(0, 16) + ".jar");
            if (Files.isRegularFile(target) && sha256Hex(Files.readAllBytes(target)).equals(hash)) {
                return target;
            }

            Path temp = Files.createTempFile(dir, "ecj-", ".tmp");
            try {
                Files.write(temp, jarBytes);
                moveCompilerJarIntoPlace(temp, target);
            } finally {
                Files.deleteIfExists(temp);
            }
            return target;
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not extract bundled ECJ compiler for worker process", e);
            return null;
        }
    }

    private static void moveCompilerJarIntoPlace(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.FileAlreadyExistsException ignored) {
            // Another worker prepared the same content-addressed compiler jar.
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            try {
                Files.move(source, target);
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // Another worker prepared the same content-addressed compiler jar.
            }
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private static List<TestResult> repeatedError(List<TestCase> testCases, String message) {
        int count = Math.max(1, testCases == null ? 1 : testCases.size());
        List<TestResult> results = new ArrayList<>();
        TestResult error = new TestResult(false, message, null);
        for (int i = 0; i < count; i++) {
            results.add(error);
        }
        return results;
    }

    static String validateCodeEnvelope(String code) {
        if (code == null || code.isEmpty()) {
            return "ERROR: No code provided";
        }
        if (code.length() > MAX_CODE_LENGTH) {
            return "ERROR: Code exceeds maximum length of " + MAX_CODE_LENGTH + " characters";
        }
        return null;
    }

    private static List<TestResult> validateCodeEnvelopeForBatch(String code, List<TestCase> testCases) {
        String envelopeError = validateCodeEnvelope(code);
        if (envelopeError == null) {
            return null;
        }
        List<TestResult> results = new ArrayList<>();
        int count = Math.max(1, testCases == null ? 0 : testCases.size());
        TestResult error = new TestResult(false, envelopeError, null);
        for (int i = 0; i < count; i++) {
            results.add(error);
        }
        return results;
    }

    private static List<TestResult> validateBatchTestCases(List<TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of(new TestResult(false, "ERROR: No test cases provided", null));
        }
        for (int i = 0; i < testCases.size(); i++) {
            if (testCases.get(i) == null) {
                return repeatedError(testCases, "ERROR: Null test case at index " + i);
            }
        }
        return null;
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 500 ? text.substring(0, 500) + "..." : text;
    }

    private static boolean shouldUseDesignClassRunner(String className, List<TestCase> testCases) {
        return !"Solution".equals(className)
                && !testCases.isEmpty()
                && testCases.stream().allMatch(testCase -> isSerializedDesignInput(testCase.input));
    }

    private static boolean isSerializedDesignInput(String input) {
        if (input == null) {
            return false;
        }
        String[] lines = input.strip().split("\\R", 2);
        if (lines.length < 2) {
            return false;
        }
        try {
            JsonElement methods = JsonParser.parseString(lines[0].trim());
            JsonElement args = JsonParser.parseString(lines[1].trim());
            return methods.isJsonArray() && args.isJsonArray();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String translateJavaUnicodeEscapes(String code) {
        StringBuilder translated = new StringBuilder(code.length());
        int index = 0;
        while (index < code.length()) {
            char current = code.charAt(index);
            if (current == '\\'
                    && isUnicodeEscapeEligible(translated)
                    && index + 1 < code.length()
                    && code.charAt(index + 1) == 'u') {
                int hexStart = index + 2;
                while (hexStart < code.length() && code.charAt(hexStart) == 'u') {
                    hexStart++;
                }
                int hexEnd = hexStart + 4;
                if (hexEnd <= code.length() && isHexQuad(code, hexStart)) {
                    translated.append((char) Integer.parseInt(code.substring(hexStart, hexEnd), 16));
                    index = hexEnd;
                    continue;
                }
            }
            translated.append(current);
            index++;
        }
        return translated.toString();
    }

    private static boolean isUnicodeEscapeEligible(StringBuilder translated) {
        int backslashes = 0;
        for (int i = translated.length() - 1; i >= 0 && translated.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 0;
    }

    private static boolean isHexQuad(String value, int start) {
        for (int i = start; i < start + 4; i++) {
            char ch = value.charAt(i);
            boolean isHex = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!isHex) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCodecClass(Class<?> clazz) {
        return findNamedMethod(clazz, "serialize", "encode") != null
                && findNamedMethod(clazz, "deserialize", "decode") != null;
    }

    private static TestResult runDesignTestCase(Class<?> clazz, TestCase testCase, int timeoutMs) {
        return runTestResultWithTimeout(() -> runDesignSequence(clazz, testCase), timeoutMs);
    }

    private static TestResult runDesignSequence(Class<?> clazz, TestCase testCase) {
        try {
            String[] lines = testCase.input.strip().split("\\R", 2);
            JsonArray methodNames = JsonParser.parseString(lines[0].trim()).getAsJsonArray();
            JsonArray allArgs = JsonParser.parseString(lines[1].trim()).getAsJsonArray();
            if (methodNames.size() != allArgs.size()) {
                return new TestResult(false, "ERROR: Method names and args arrays have different lengths", null);
            }

            Object instance = createDesignInstance(clazz, allArgs.get(0).getAsJsonArray());
            List<String> actualResults = new ArrayList<>();
            actualResults.add("null");
            for (int i = 1; i < methodNames.size(); i++) {
                String methodName = methodNames.get(i).getAsString();
                Object result = invokeDesignMethod(instance, methodName, allArgs.get(i).getAsJsonArray());
                actualResults.add(formatDesignValue(result));
            }

            String actual = "[" + String.join(",", actualResults) + "]";
            if (outputsMatch(actual, testCase.expectedOutput, clazz)
                    || randomizedDesignOutputsMatch(methodNames, allArgs, actualResults, testCase.expectedOutput)) {
                return new TestResult(true, "PASS", displayOutput(actual));
            }
            return wrongAnswer(testCase.expectedOutput, actual);
        } catch (Exception e) {
            return new TestResult(false, "ERROR: " + e.getClass().getSimpleName() + ": " + safeMessage(e), null);
        }
    }

    private static boolean randomizedDesignOutputsMatch(JsonArray methodNames, JsonArray allArgs,
                                                        List<String> actualResults, String expectedOutput) {
        RandomizedDesignState state = RandomizedDesignState.fromConstructor(methodNames);
        if (state == null || !containsMethod(methodNames, "getRandom")) {
            return false;
        }
        try {
            JsonArray expectedResults = JsonParser.parseString(expectedOutput).getAsJsonArray();
            if (methodNames.size() != allArgs.size() || expectedResults.size() != actualResults.size()) {
                return false;
            }
            for (int i = 0; i < expectedResults.size(); i++) {
                String methodName = methodNames.get(i).getAsString();
                String actual = actualResults.get(i);
                if ("getRandom".equals(methodName)) {
                    if (!state.containsActualRandomValue(actual)) {
                        return false;
                    }
                    continue;
                }
                if (!outputsMatch(actual, expectedResults.get(i).toString(), null)) {
                    return false;
                }
                state.applyDeterministicOperation(methodName, allArgs.get(i).getAsJsonArray());
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean containsMethod(JsonArray methodNames, String expectedMethodName) {
        for (JsonElement methodName : methodNames) {
            if (expectedMethodName.equals(methodName.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static final class RandomizedDesignState {
        private final boolean allowsDuplicates;
        private final Map<Integer, Integer> counts = new HashMap<>();

        private RandomizedDesignState(boolean allowsDuplicates) {
            this.allowsDuplicates = allowsDuplicates;
        }

        static RandomizedDesignState fromConstructor(JsonArray methodNames) {
            if (methodNames.isEmpty()) {
                return null;
            }
            String constructorName = methodNames.get(0).getAsString();
            if ("RandomizedSet".equals(constructorName)) {
                return new RandomizedDesignState(false);
            }
            if ("RandomizedCollection".equals(constructorName)) {
                return new RandomizedDesignState(true);
            }
            return null;
        }

        void applyDeterministicOperation(String methodName, JsonArray args) {
            if (args.isEmpty()) {
                return;
            }
            int value = args.get(0).getAsInt();
            if ("insert".equals(methodName)) {
                if (allowsDuplicates) {
                    counts.merge(value, 1, Integer::sum);
                } else {
                    counts.putIfAbsent(value, 1);
                }
            } else if ("remove".equals(methodName)) {
                int count = counts.getOrDefault(value, 0);
                if (count <= 1) {
                    counts.remove(value);
                } else {
                    counts.put(value, count - 1);
                }
            }
        }

        boolean containsActualRandomValue(String actual) {
            if (actual == null || actual.equals("null") || actual.startsWith("ERROR:")) {
                return false;
            }
            try {
                int value = Integer.parseInt(actual);
                return counts.getOrDefault(value, 0) > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private static Object createDesignInstance(Class<?> clazz, JsonArray args) throws Exception {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == args.size()) {
                constructor.setAccessible(true);
                return constructor.newInstance(jsonArgsToJava(args, constructor.getParameterTypes()));
            }
        }
        throw new NoSuchMethodException("No constructor in " + clazz.getSimpleName() + " accepts " + args.size() + " args");
    }

    private static Object invokeDesignMethod(Object instance, String methodName, JsonArray args) throws Exception {
        Method method = null;
        for (Method candidate : instance.getClass().getDeclaredMethods()) {
            if (candidate.getName().equals(methodName) && candidate.getParameterCount() == args.size()) {
                method = candidate;
                break;
            }
        }
        if (method == null) {
            for (Method candidate : instance.getClass().getMethods()) {
                if (candidate.getName().equals(methodName) && candidate.getParameterCount() == args.size()) {
                    method = candidate;
                    break;
                }
            }
        }
        if (method == null) {
            throw new NoSuchMethodException(methodName + " with " + args.size() + " args");
        }
        method.setAccessible(true);
        Object result = method.invoke(instance, jsonArgsToJava(args, method.getParameterTypes()));
        return method.getReturnType() == void.class ? null : result;
    }

    private static Object[] jsonArgsToJava(JsonArray args, Class<?>[] parameterTypes) {
        Object[] values = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            values[i] = jsonToJava(args.get(i), parameterTypes[i]);
        }
        return values;
    }

    private static Object jsonToJava(JsonElement element, Class<?> targetType) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return element.getAsInt();
        }
        if (targetType == long.class || targetType == Long.class) {
            return element.getAsLong();
        }
        if (targetType == double.class || targetType == Double.class) {
            return element.getAsDouble();
        }
        if (targetType == float.class || targetType == Float.class) {
            return element.getAsFloat();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return element.getAsBoolean();
        }
        if (targetType == String.class) {
            return element.getAsString();
        }
        if (targetType == char.class || targetType == Character.class) {
            String text = element.getAsString();
            return text.isEmpty() ? '\0' : text.charAt(0);
        }
        if (targetType.isArray() && element.isJsonArray()) {
            return jsonArrayToJavaArray(element.getAsJsonArray(), targetType.getComponentType());
        }
        if (List.class.isAssignableFrom(targetType) && element.isJsonArray()) {
            return jsonArrayToList(element.getAsJsonArray());
        }
        return null;
    }

    private static Object jsonArrayToJavaArray(JsonArray array, Class<?> componentType) {
        Object result = java.lang.reflect.Array.newInstance(componentType, array.size());
        for (int i = 0; i < array.size(); i++) {
            java.lang.reflect.Array.set(result, i, jsonToJava(array.get(i), componentType));
        }
        return result;
    }

    private static List<Object> jsonArrayToList(JsonArray array) {
        List<Object> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) {
                values.add(null);
            } else if (element.isJsonArray()) {
                values.add(jsonArrayToList(element.getAsJsonArray()));
            } else if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    values.add(primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    values.add(primitive.getAsInt());
                } else {
                    values.add(primitive.getAsString());
                }
            }
        }
        return values;
    }

    private static String formatDesignValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + text + "\"";
        }
        return formatResult(value).replace(" ", "");
    }

    private static TestResult runCodecTestCase(Class<?> clazz, TestCase testCase, int timeoutMs) {
        return runTestResultWithTimeout(() -> runCodecRoundTrip(clazz, testCase), timeoutMs);
    }

    private static TestResult runCodecRoundTrip(Class<?> clazz, TestCase testCase) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object codec = constructor.newInstance();

            Method serializeMethod = findNamedMethod(clazz, "serialize", "encode");
            Method deserializeMethod = findNamedMethod(clazz, "deserialize", "decode");
            if (serializeMethod == null || deserializeMethod == null) {
                return new TestResult(false, "ERROR: Codec class missing serialize/deserialize methods", null);
            }

            serializeMethod.setAccessible(true);
            deserializeMethod.setAccessible(true);
            Object encoded = serializeMethod.invoke(codec, parseArgs(testCase.input, serializeMethod.getParameterTypes()));
            Object decoded = deserializeMethod.invoke(codec, encoded);
            String actual = formatResult(decoded);
            if (outputsMatch(actual, testCase.expectedOutput, clazz)) {
                return new TestResult(true, "PASS", displayOutput(actual));
            }
            return wrongAnswer(testCase.expectedOutput, actual);
        } catch (Exception e) {
            return new TestResult(false, "ERROR: " + e.getClass().getSimpleName() + ": " + safeMessage(e), null);
        }
    }

    private static Method findNamedMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static <T> ExecutionResult<T> runCallableWithTimeout(Callable<T> task, int timeoutMs) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Thread worker = new Thread(() -> {
            try {
                result.complete(task.call());
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        }, "AlgoCraft-CodeExecutor-" + executionThreadIds.incrementAndGet());
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.start();

        try {
            return new ExecutionResult<>(result.get(timeoutMs, TimeUnit.MILLISECONDS), null, false, false);
        } catch (TimeoutException e) {
            terminateTimedOutWorker(worker);
            return new ExecutionResult<>(null, null, true, false);
        } catch (ExecutionException e) {
            return new ExecutionResult<>(null, unwrapExecutionCause(e.getCause()), false, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            terminateTimedOutWorker(worker);
            return new ExecutionResult<>(null, e, false, true);
        }
    }

    private static void terminateTimedOutWorker(Thread worker) {
        worker.interrupt();
    }

    private static String runStringWithTimeout(Callable<String> task, int timeoutMs) {
        ExecutionResult<String> result = runCallableWithTimeout(task, timeoutMs);
        if (result.timeout()) {
            totalTimeouts.incrementAndGet();
            return "ERROR: Time Limit Exceeded (>" + timeoutMs + "ms)";
        }
        if (result.interrupted()) {
            return "ERROR: Interrupted";
        }
        if (result.error() != null) {
            totalErrors.incrementAndGet();
            Throwable cause = unwrapExecutionCause(result.error());
            return "ERROR: " + cause.getClass().getSimpleName() + ": " + safeMessage(cause);
        }
        return result.value();
    }

    private static TestResult runTestResultWithTimeout(Callable<TestResult> task, int timeoutMs) {
        ExecutionResult<TestResult> result = runCallableWithTimeout(task, timeoutMs);
        if (result.timeout()) {
            totalTimeouts.incrementAndGet();
            return new TestResult(false, "ERROR: Time Limit Exceeded (>" + timeoutMs + "ms)", null);
        }
        if (result.interrupted()) {
            return new TestResult(false, "ERROR: Interrupted", null);
        }
        if (result.error() != null) {
            totalErrors.incrementAndGet();
            Throwable cause = unwrapExecutionCause(result.error());
            return new TestResult(false, "ERROR: " + cause.getClass().getSimpleName() + ": " + safeMessage(cause), null);
        }
        return result.value();
    }

    private static ClassLoadResult loadClassWithTimeout(String className, ClassLoader classLoader, int timeoutMs) {
        ExecutionResult<Class<?>> result = runCallableWithTimeout(() -> Class.forName(className, true, classLoader), timeoutMs);
        if (result.timeout()) {
            totalTimeouts.incrementAndGet();
            return new ClassLoadResult(null, new TestResult(false, "ERROR: Time Limit Exceeded (>" + timeoutMs + "ms)", null));
        }
        if (result.interrupted()) {
            return new ClassLoadResult(null, new TestResult(false, "ERROR: Interrupted", null));
        }
        if (result.error() != null) {
            totalErrors.incrementAndGet();
            Throwable cause = unwrapExecutionCause(result.error());
            return new ClassLoadResult(null, new TestResult(false, "ERROR: " + cause.getClass().getSimpleName() + ": " + safeMessage(cause), null));
        }
        return new ClassLoadResult(result.value(), null);
    }

    private static Throwable unwrapExecutionCause(Throwable cause) {
        if (cause instanceof java.lang.reflect.InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return cause == null ? new RuntimeException("Unknown execution error") : cause;
    }

    private static Throwable unwrapExecutionCause(ExecutionException e) {
        return unwrapExecutionCause(e.getCause());
    }

    private record ClassLoadResult(Class<?> clazz, TestResult error) {
    }

    private record ExecutionResult<T>(T value, Throwable error, boolean timeout, boolean interrupted) {
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return "";
        }
        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }
        return message;
    }

    /**
     * Run a single test case with the compiled class.
     */
    private static TestResult runSingleTestCase(Constructor<?> constructor, Method method,
                                                 TestCase testCase, int timeoutMs) {
        return runTestResultWithTimeout(() -> {
            Object instance = constructor.newInstance();

            // Set up parent class state if needed (e.g., VersionControl)
            setupParentClass(instance, testCase.input);

            Object[] args = parseArgs(testCase.input, method.getParameterTypes());
            totalExecutions.incrementAndGet();
            Object invokeResult = method.invoke(instance, args);

            String resultStr = formatMethodResult(method, invokeResult, args, testCase.expectedOutput);
            boolean specialRequired = requiresSpecialOutputMatch(method, testCase.problemId);
            boolean specialPassed = specialOutputMatches(method, invokeResult, args,
                    testCase.input, testCase.problemId, testCase.expectedOutput);

            if ((specialRequired && specialPassed)
                    || (!specialRequired && (outputsMatch(resultStr, testCase.expectedOutput, method.getReturnType())
                    || specialPassed))) {
                return new TestResult(true, "PASS", displayOutput(resultStr));
            } else {
                return wrongAnswer(testCase.expectedOutput, resultStr);
            }
        }, timeoutMs);
    }

    private static TestResult wrongAnswer(String expectedOutput, String actualOutput) {
        return new TestResult(false, wrongAnswerMessage(expectedOutput, actualOutput), displayOutput(actualOutput));
    }

    private static String wrongAnswerMessage(String expectedOutput, String actualOutput) {
        return "FAIL: Expected " + displayOutput(expectedOutput) + ", got " + displayOutput(actualOutput);
    }

    private static String displayOutput(String output) {
        if (output == null) {
            return null;
        }
        if (output.length() <= MAX_RESULT_OUTPUT_SIZE) {
            return output;
        }
        return output.substring(0, MAX_RESULT_OUTPUT_SIZE) + "... [truncated]";
    }

    /**
     * Find the solution method in the class, excluding Object methods.
     */
    private static Method findSolutionMethod(Class<?> clazz) {
        return findSolutionMethod(clazz, "");
    }

    private static Method findSolutionMethod(Class<?> clazz, String input) {
        return findSolutionMethod(clazz, input, null);
    }

    private static Method findSolutionMethod(Class<?> clazz, String input, String preferredMethodName) {
        Set<String> preferredNames = preferredMethodName == null || preferredMethodName.isBlank()
                ? Set.of()
                : Set.of(preferredMethodName);
        return SolutionMethodSelector.select(clazz, input, preferredNames, method -> canParseMethodInput(input, method));
    }

    private static boolean canParseMethodInput(String input, Method method) {
        try {
            Object[] args = parseArgs(input == null ? "" : input, method.getParameterTypes());
            if (args.length != method.getParameterCount()) {
                return false;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] == null && parameterTypes[i].isPrimitive()) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String preferredMethodName(List<TestCase> testCases) {
        if (testCases == null) {
            return null;
        }
        for (TestCase testCase : testCases) {
            if (testCase != null && testCase.preferredMethodName != null && !testCase.preferredMethodName.isBlank()) {
                return testCase.preferredMethodName;
            }
        }
        return null;
    }

    public static String preferredMethodNameFromInitialCode(String initialCode) {
        if (initialCode == null || initialCode.isBlank()) {
            return null;
        }
        Matcher matcher = METHOD_DECLARATION_PATTERN.matcher(removeComments(initialCode));
        String fallback = null;
        while (matcher.find()) {
            String visibility = matcher.group(1);
            String name = matcher.group(2);
            if (!"if".equals(name) && !"for".equals(name) && !"while".equals(name) && !"switch".equals(name)) {
                if ("public".equals(visibility)) {
                    return name;
                }
                if (fallback == null && !"private".equals(visibility)) {
                    fallback = name;
                }
            }
        }
        return fallback;
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
                LOGGER.log(System.Logger.Level.WARNING,
                        "Blocked dangerous code pattern: " + pattern.pattern());
                return "ERROR: Forbidden code pattern detected. System operations are not allowed.";
            }
        }
        return null;
    }

    /**
     * Remove comments and mask literal contents before policy scans.
     * This keeps real code visible while preventing strings/comments from
     * triggering or hiding security/import patterns.
     */
    private static String removeComments(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(code.length());
        int i = 0;
        while (i < code.length()) {
            char current = code.charAt(i);
            char next = i + 1 < code.length() ? code.charAt(i + 1) : '\0';

            if (current == '/' && next == '/') {
                result.append("  ");
                i += 2;
                while (i < code.length()) {
                    char ch = code.charAt(i);
                    if (ch == '\n' || ch == '\r') {
                        result.append(ch);
                        i++;
                        break;
                    }
                    result.append(' ');
                    i++;
                }
                continue;
            }

            if (current == '/' && next == '*') {
                result.append("  ");
                i += 2;
                while (i < code.length()) {
                    char ch = code.charAt(i);
                    char after = i + 1 < code.length() ? code.charAt(i + 1) : '\0';
                    if (ch == '*' && after == '/') {
                        result.append("  ");
                        i += 2;
                        break;
                    }
                    appendMasked(result, ch);
                    i++;
                }
                continue;
            }

            if (current == '"' && i + 2 < code.length()
                    && code.charAt(i + 1) == '"' && code.charAt(i + 2) == '"') {
                result.append("   ");
                i += 3;
                while (i < code.length()) {
                    char ch = code.charAt(i);
                    if (ch == '"' && i + 2 < code.length()
                            && code.charAt(i + 1) == '"' && code.charAt(i + 2) == '"') {
                        result.append("   ");
                        i += 3;
                        break;
                    }
                    appendMasked(result, ch);
                    i++;
                }
                continue;
            }

            if (current == '"') {
                result.append(' ');
                i++;
                while (i < code.length()) {
                    char ch = code.charAt(i);
                    if (ch == '\\' && i + 1 < code.length()) {
                        result.append("  ");
                        i += 2;
                        continue;
                    }
                    result.append(' ');
                    i++;
                    if (ch == '"') {
                        break;
                    }
                }
                continue;
            }

            if (current == '\'') {
                result.append(' ');
                i++;
                while (i < code.length()) {
                    char ch = code.charAt(i);
                    if (ch == '\\' && i + 1 < code.length()) {
                        result.append("  ");
                        i += 2;
                        continue;
                    }
                    result.append(' ');
                    i++;
                    if (ch == '\'') {
                        break;
                    }
                }
                continue;
            }

            result.append(current);
            i++;
        }
        return result.toString();
    }

    private static void appendMasked(StringBuilder result, char ch) {
        result.append(ch == '\n' || ch == '\r' ? ch : ' ');
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
        Matcher matcher = IMPORT_PATTERN.matcher(removeComments(code));

        while (matcher.find()) {
            String importStatement = matcher.group(1).trim();
            // Skip static imports for allowed packages
            if (importStatement.startsWith("static ")) {
                importStatement = importStatement.substring(7).trim();
                // Check if the static import is from an allowed package
                if (!isImportAllowed(importStatement)) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Blocked forbidden import: " + importStatement);
                    return "ERROR: Forbidden import: " + importStatement;
                }
            } else if (!ALLOWED_IMPORTS.contains(importStatement)) {
                // Check if matches a wildcard pattern
                if (!isImportAllowed(importStatement)) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Blocked forbidden import: " + importStatement);
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
            if (isDirectImportFromWildcardPackage(importStatement, basePackage)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDirectImportFromWildcardPackage(String importStatement, String basePackage) {
        String prefix = basePackage + ".";
        if (!importStatement.startsWith(prefix)) {
            return false;
        }
        String remainder = importStatement.substring(prefix.length());
        return !remainder.isEmpty() && remainder.indexOf('.') < 0;
    }

    /**
     * Get timeout from config with fallback.
     */
    private static int getTimeout() {
        String override = System.getProperty(TIMEOUT_PROPERTY);
        if (override != null && !override.isBlank()) {
            try {
                int value = Integer.parseInt(override.trim());
                if (value > 0) {
                    return Math.max(value, MIN_TIMEOUT_MS);
                }
            } catch (NumberFormatException ignored) {
                // Fall through to the configured value.
            }
        }
        try {
            return Math.max(Config.MAX_EXECUTION_TIME.get(), MIN_TIMEOUT_MS);
        } catch (Throwable e) {
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
        output = output.trim();
        if (output.startsWith("\"") && output.endsWith("\"") && output.length() >= 2) {
            output = output.substring(1, output.length() - 1);
        }
        String normalized = WHITESPACE_PATTERN.matcher(output).replaceAll("").replace("\"", "").toLowerCase();
        return normalized.replaceAll("(-?\\d+)\\.0+(?=,|\\]|$)", "$1");
    }

    /**
     * Clean up temporary directory with retry logic.
     */
    private static void cleanupTempDir(Path tempDir) {
        if (tempDir == null) {
            return;
        }

        java.io.IOException lastFailure = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                deleteDirectoryTree(tempDir);
                return;
            } catch (java.io.IOException e) {
                lastFailure = e;
                LOGGER.log(System.Logger.Level.DEBUG,
                        "Failed to cleanup temp dir attempt " + attempt + ": " + tempDir, e);
            }

            if (attempt < 5) {
                try {
                    Thread.sleep(50L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        markDeleteOnExit(tempDir);
        if (lastFailure != null) {
            LOGGER.log(System.Logger.Level.DEBUG,
                    "Scheduled temp dir for deleteOnExit after cleanup retries failed: " + tempDir, lastFailure);
        }
    }

    private static void deleteDirectoryTree(Path root) throws java.io.IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static void markDeleteOnExit(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                entry.toFile().deleteOnExit();
            }
        } catch (java.io.IOException ignored) {
            root.toFile().deleteOnExit();
        }
    }

    /**
     * Shutdown background maintenance tasks.
     * Should be called when the mod is unloading.
     */
    public static void shutdown() {
        CLEANUP_SCHEDULER.shutdown();
        try {
            CLEANUP_SCHEDULER.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Parse input string into method arguments.
     * Supports: int, int[], int[][], long, double, boolean, String, String[], char, char[]
     * Input format: "nums = [2,7,11,15], target = 9"
     */
    private static Object[] parseArgs(String input, Class<?>[] types) {
        Map<String, String> named = parseNamedArguments(input);
        Object[] specialArgs = parseSpecialNamedArgs(named, types);
        if (specialArgs != null) {
            return specialArgs;
        }

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

    private static Object[] parseSpecialNamedArgs(Map<String, String> named, Class<?>[] types) {
        if (types.length == 0 || named.isEmpty()) {
            return null;
        }

        if (types.length == 1 && types[0].getSimpleName().equals("ListNode") && named.containsKey("head")) {
            Object[] args = new Object[1];
            int pos = parseOptionalInt(named.get("pos"), -1);
            args[0] = parseListNode(named.get("head"), types[0], pos);
            return args;
        }

        if (types.length == 2
                && types[0].getSimpleName().equals("ListNode")
                && types[1].getSimpleName().equals("ListNode")
                && named.containsKey("listA")
                && named.containsKey("listB")) {
            return parseIntersectingListNodeArgs(named, types);
        }

        if (types.length == 1 && types[0].isArray()
                && types[0].getComponentType().getSimpleName().equals("ListNode")
                && named.containsKey("lists")) {
            Object[] args = new Object[1];
            args[0] = parseListNodeArray(named.get("lists"), types[0].getComponentType());
            return args;
        }

        if (types.length == 1 && types[0].getSimpleName().equals("Node")) {
            String value = named.getOrDefault("head", named.getOrDefault("adjList", named.get("root")));
            if (value != null) {
                Object[] args = new Object[1];
                args[0] = parseNode(value, types[0]);
                return args;
            }
        }

        return null;
    }

    private static Map<String, String> parseNamedArguments(String input) {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*").matcher(input);
        List<String> names = new ArrayList<>();
        List<int[]> positions = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
            positions.add(new int[]{matcher.start(), matcher.end()});
        }
        for (int i = 0; i < names.size(); i++) {
            int valueStart = positions.get(i)[1];
            int valueEnd = i + 1 < positions.size() ? positions.get(i + 1)[0] : input.length();
            String value = input.substring(valueStart, valueEnd).trim();
            if (value.endsWith(",")) {
                value = value.substring(0, value.length() - 1).trim();
            }
            values.put(names.get(i), value);
        }
        return values;
    }

    private static int parseOptionalInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
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
        if (type.isArray() && type.getComponentType().getSimpleName().equals("ListNode")) {
            return parseListNodeArray(val, type.getComponentType());
        }

        // List types
        if (type == List.class) {
            return parseListValue(val);
        }

        // TreeNode and ListNode (dynamically compiled helper classes)
        String typeName = type.getSimpleName();
        if (typeName.equals("TreeNode")) {
            return parseTreeNode(val, type);
        }
        if (typeName.equals("ListNode")) {
            return parseListNode(val, type);
        }
        if (typeName.equals("Node")) {
            return parseNode(val, type);
        }

        LOGGER.log(System.Logger.Level.WARNING, "Unsupported parameter type: " + type.getName());
        return null;
    }

    /**
     * Parse a level-order array into a TreeNode tree using reflection.
     * Input format: [1,2,3,null,4,5]
     */
    private static Object parseTreeNode(String val, Class<?> treeNodeClass) {
        try {
            val = val.trim();
            if (val.equals("[]") || val.equals("null")) return null;

            val = val.replaceAll("[\\[\\]]", "");
            String[] parts = val.split(",");
            if (parts.length == 0 || parts[0].trim().isEmpty()) return null;

            Constructor<?> ctor = treeNodeClass.getConstructor(int.class);
            java.lang.reflect.Field leftField = treeNodeClass.getField("left");
            java.lang.reflect.Field rightField = treeNodeClass.getField("right");

            Object root = ctor.newInstance(Integer.parseInt(parts[0].trim()));
            Queue<Object> queue = new LinkedList<>();
            queue.offer(root);
            int i = 1;

            while (!queue.isEmpty() && i < parts.length) {
                Object node = queue.poll();

                // Left child
                if (i < parts.length) {
                    String leftVal = parts[i].trim();
                    i++;
                    if (!leftVal.equals("null")) {
                        Object leftNode = ctor.newInstance(Integer.parseInt(leftVal));
                        leftField.set(node, leftNode);
                        queue.offer(leftNode);
                    }
                }

                // Right child
                if (i < parts.length) {
                    String rightVal = parts[i].trim();
                    i++;
                    if (!rightVal.equals("null")) {
                        Object rightNode = ctor.newInstance(Integer.parseInt(rightVal));
                        rightField.set(node, rightNode);
                        queue.offer(rightNode);
                    }
                }
            }

            return root;
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to parse TreeNode: " + val, e);
            return null;
        }
    }

    /**
     * Parse an array into a ListNode linked list using reflection.
     * Input format: [1,2,3,4,5]
     */
    private static Object parseListNode(String val, Class<?> listNodeClass) {
        return parseListNode(val, listNodeClass, -1);
    }

    private static Object parseListNode(String val, Class<?> listNodeClass, int cyclePos) {
        try {
            val = val.trim();
            if (val.equals("[]") || val.equals("null")) return null;

            val = val.replaceAll("[\\[\\]]", "");
            String[] parts = val.split(",");
            if (parts.length == 0 || parts[0].trim().isEmpty()) return null;

            Constructor<?> ctor = listNodeClass.getConstructor(int.class);
            java.lang.reflect.Field nextField = listNodeClass.getField("next");

            Object head = ctor.newInstance(Integer.parseInt(parts[0].trim()));
            Object current = head;
            List<Object> nodes = new ArrayList<>();
            nodes.add(head);

            for (int i = 1; i < parts.length; i++) {
                Object nextNode = ctor.newInstance(Integer.parseInt(parts[i].trim()));
                nextField.set(current, nextNode);
                current = nextNode;
                nodes.add(nextNode);
            }

            if (cyclePos >= 0 && cyclePos < nodes.size()) {
                nextField.set(current, nodes.get(cyclePos));
            }

            return head;
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to parse ListNode: " + val, e);
            return null;
        }
    }

    private static Object[] parseIntersectingListNodeArgs(Map<String, String> named, Class<?>[] types) {
        try {
            int intersectVal = parseOptionalInt(named.get("intersectVal"), 0);
            int skipA = parseOptionalInt(named.get("skipA"), -1);
            int skipB = parseOptionalInt(named.get("skipB"), -1);
            int[] valuesA = parseIntArray(named.get("listA"));
            int[] valuesB = parseIntArray(named.get("listB"));
            if (intersectVal == 0 || skipA < 0 || skipA >= valuesA.length || skipB < 0) {
                return new Object[]{buildListNode(valuesA, 0, valuesA.length, types[0]), buildListNode(valuesB, 0, valuesB.length, types[1])};
            }

            Constructor<?> ctor = types[0].getConstructor(int.class);
            java.lang.reflect.Field nextField = types[0].getField("next");
            List<Object> nodesA = new ArrayList<>();
            for (int value : valuesA) {
                nodesA.add(ctor.newInstance(value));
            }
            for (int i = 0; i + 1 < nodesA.size(); i++) {
                nextField.set(nodesA.get(i), nodesA.get(i + 1));
            }
            Object headA = nodesA.isEmpty() ? null : nodesA.get(0);
            Object shared = nodesA.get(skipA);

            Object headB = null;
            Object tailB = null;
            for (int i = 0; i < Math.min(skipB, valuesB.length); i++) {
                Object node = ctor.newInstance(valuesB[i]);
                if (headB == null) {
                    headB = node;
                } else {
                    nextField.set(tailB, node);
                }
                tailB = node;
            }
            if (headB == null) {
                headB = shared;
            } else {
                nextField.set(tailB, shared);
            }
            return new Object[]{headA, headB};
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to parse intersecting ListNode args", e);
            return new Object[]{null, null};
        }
    }

    private static Object buildListNode(int[] values, int startInclusive, int endExclusive, Class<?> listNodeClass) throws Exception {
        if (startInclusive >= endExclusive) {
            return null;
        }
        Constructor<?> ctor = listNodeClass.getConstructor(int.class);
        java.lang.reflect.Field nextField = listNodeClass.getField("next");
        Object head = ctor.newInstance(values[startInclusive]);
        Object current = head;
        for (int i = startInclusive + 1; i < endExclusive; i++) {
            Object next = ctor.newInstance(values[i]);
            nextField.set(current, next);
            current = next;
        }
        return head;
    }

    private static Object parseListNodeArray(String val, Class<?> listNodeClass) {
        val = val.trim();
        if (val.equals("[]")) {
            return java.lang.reflect.Array.newInstance(listNodeClass, 0);
        }
        String inner = val.substring(1, val.length() - 1);
        List<String> parts = splitTopLevel(inner, ',');
        List<String> arrays = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("[")) {
                arrays.add(trimmed);
            }
        }
        Object result = java.lang.reflect.Array.newInstance(listNodeClass, arrays.size());
        for (int i = 0; i < arrays.size(); i++) {
            java.lang.reflect.Array.set(result, i, parseListNode(arrays.get(i), listNodeClass));
        }
        return result;
    }

    private static Object parseNode(String val, Class<?> nodeClass) {
        try {
            boolean hasChildren = false;
            boolean hasNeighbors = false;
            boolean hasRandom = false;
            for (java.lang.reflect.Field field : nodeClass.getFields()) {
                if (field.getName().equals("children")) hasChildren = true;
                if (field.getName().equals("neighbors")) hasNeighbors = true;
                if (field.getName().equals("random")) hasRandom = true;
            }
            if (hasNeighbors) return parseGraphNode(val, nodeClass);
            if (hasChildren) return parseNaryNode(val, nodeClass);
            if (hasRandom) return parseRandomPointerNode(val, nodeClass);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to parse Node: " + val, e);
        }
        return null;
    }

    private static Object parseGraphNode(String val, Class<?> nodeClass) throws Exception {
        int[][] adjacency = val.trim().equals("[[]]") ? new int[][]{new int[0]} : parse2DIntArray(val);
        if (adjacency.length == 0) {
            return null;
        }
        Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        java.lang.reflect.Field neighborsField = nodeClass.getField("neighbors");
        Object[] nodes = new Object[adjacency.length];
        for (int i = 0; i < adjacency.length; i++) {
            nodes[i] = ctor.newInstance(i + 1);
        }
        for (int i = 0; i < adjacency.length; i++) {
            @SuppressWarnings("unchecked")
            List<Object> neighbors = (List<Object>) neighborsField.get(nodes[i]);
            for (int neighbor : adjacency[i]) {
                neighbors.add(nodes[neighbor - 1]);
            }
        }
        return nodes[0];
    }

    private static Object parseNaryNode(String val, Class<?> nodeClass) throws Exception {
        val = val.trim();
        if (val.equals("[]") || val.equals("null")) {
            return null;
        }
        String[] elements = val.replaceAll("[\\[\\]]", "").split(",");
        if (elements.length == 0 || elements[0].trim().equals("null")) {
            return null;
        }
        Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        java.lang.reflect.Field childrenField = nodeClass.getField("children");
        Object root = ctor.newInstance(Integer.parseInt(elements[0].trim()));
        Queue<Object> queue = new LinkedList<>();
        queue.offer(root);
        int i = 2;
        while (!queue.isEmpty() && i < elements.length) {
            Object parent = queue.poll();
            @SuppressWarnings("unchecked")
            List<Object> children = (List<Object>) childrenField.get(parent);
            while (i < elements.length) {
                String item = elements[i++].trim();
                if (item.equals("null")) {
                    break;
                }
                Object child = ctor.newInstance(Integer.parseInt(item));
                children.add(child);
                queue.offer(child);
            }
        }
        return root;
    }

    private static Object parseRandomPointerNode(String val, Class<?> nodeClass) throws Exception {
        val = val.trim();
        if (val.equals("[]") || val.equals("null")) {
            return null;
        }
        String inner = val.substring(1, val.length() - 1);
        List<String> pairs = splitTopLevel(inner, ',');
        Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        java.lang.reflect.Field nextField = nodeClass.getField("next");
        java.lang.reflect.Field randomField = nodeClass.getField("random");
        List<Object> nodes = new ArrayList<>();
        List<String> randomIndices = new ArrayList<>();
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (!trimmed.startsWith("[")) {
                continue;
            }
            String[] parts = trimmed.replaceAll("[\\[\\]]", "").split(",");
            nodes.add(ctor.newInstance(Integer.parseInt(parts[0].trim())));
            randomIndices.add(parts.length > 1 ? parts[1].trim() : "null");
        }
        for (int i = 0; i + 1 < nodes.size(); i++) {
            nextField.set(nodes.get(i), nodes.get(i + 1));
        }
        for (int i = 0; i < nodes.size(); i++) {
            String random = randomIndices.get(i);
            if (!random.equals("null")) {
                randomField.set(nodes.get(i), nodes.get(Integer.parseInt(random)));
            }
        }
        return nodes.isEmpty() ? null : nodes.get(0);
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
        for (String item : splitTopLevel(val, ',')) {
            result.add(unquoteStringToken(item));
        }
        return result.toArray(new String[0]);
    }

    private static String unquoteStringToken(String token) {
        String result = token.trim();
        if (result.length() >= 2) {
            char first = result.charAt(0);
            char last = result.charAt(result.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                result = result.substring(1, result.length() - 1);
            }
        }
        return result
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\");
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

    private static List<Object> parseListValue(String val) {
        try {
            JsonElement element = JsonParser.parseString(val.trim());
            if (element.isJsonArray()) {
                return jsonArrayToList(element.getAsJsonArray());
            }
        } catch (RuntimeException ignored) {
        }
        int[] numbers = parseIntArray(val);
        List<Object> values = new ArrayList<>();
        for (int number : numbers) {
            values.add(number);
        }
        return values;
    }

    /**
     * Format result object to string for comparison.
     */
    private static String formatMethodResult(Method method, Object invokeResult, Object[] args, String expectedOutput) {
        if ((method.getReturnType() == int.class || method.getReturnType() == Integer.class)
                && args.length > 0
                && args[0] instanceof char[] chars
                && expectedOutput != null
                && expectedOutput.contains("length")
                && expectedOutput.contains("chars")) {
            int length = ((Number) invokeResult).intValue();
            StringJoiner prefix = new StringJoiner(",", "[", "]");
            for (int i = 0; i < Math.min(length, chars.length); i++) {
                prefix.add("\"" + chars[i] + "\"");
            }
            return "length = " + length + ", chars = " + prefix;
        }
        if (method.getReturnType() == void.class && args.length > 0) {
            return formatResult(args[0]);
        }
        return formatResult(invokeResult);
    }

    private static String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        if (result instanceof String text) {
            return "\"" + text + "\"";
        }
        if (result instanceof Character character) {
            return "\"" + character + "\"";
        }

        // Check for TreeNode/ListNode by class name (loaded from sandbox classloader)
        String className = result.getClass().getSimpleName();
        if (className.equals("TreeNode")) {
            return formatTreeNode(result);
        }
        if (className.equals("ListNode")) {
            return formatListNode(result);
        }
        if (className.equals("Node")) {
            return formatNode(result);
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
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            for (char item : (char[]) result) {
                joiner.add("\"" + item + "\"");
            }
            return joiner.toString();
        }
        if (result instanceof String[]) {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            for (String item : (String[]) result) {
                joiner.add("\"" + item + "\"");
            }
            return joiner.toString();
        }

        if (result instanceof int[][]) {
            return Arrays.deepToString((int[][]) result);
        }
        if (result instanceof char[][]) {
            StringJoiner outer = new StringJoiner(",", "[", "]");
            for (char[] row : (char[][]) result) {
                StringJoiner inner = new StringJoiner(",", "[", "]");
                for (char item : row) {
                    inner.add("\"" + item + "\"");
                }
                outer.add(inner.toString());
            }
            return outer.toString();
        }

        if (result instanceof Object[]) {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            for (Object item : (Object[]) result) {
                joiner.add(formatResult(item));
            }
            return joiner.toString();
        }

        if (result instanceof List<?> list) {
            return formatList(list);
        }

        return String.valueOf(result);
    }

    private static String formatList(List<?> list) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Object item : list) {
            joiner.add(formatResult(item));
        }
        return joiner.toString();
    }

    /**
     * Format a TreeNode object as a level-order array string using reflection.
     */
    private static String formatTreeNode(Object node) {
        if (node == null) return "[]";
        try {
            Class<?> clazz = node.getClass();
            java.lang.reflect.Field valField = clazz.getField("val");
            java.lang.reflect.Field leftField = clazz.getField("left");
            java.lang.reflect.Field rightField = clazz.getField("right");

            List<String> result = new ArrayList<>();
            Queue<Object> queue = new LinkedList<>();
            queue.offer(node);

            while (!queue.isEmpty()) {
                Object current = queue.poll();
                if (current == null) {
                    result.add("null");
                } else {
                    result.add(String.valueOf(valField.get(current)));
                    queue.offer(leftField.get(current));
                    queue.offer(rightField.get(current));
                }
            }

            // Remove trailing nulls
            while (!result.isEmpty() && result.get(result.size() - 1).equals("null")) {
                result.remove(result.size() - 1);
            }

            return "[" + String.join(",", result) + "]";
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    /**
     * Format a ListNode object as an array string using reflection.
     */
    private static String formatListNode(Object node) {
        if (node == null) return "[]";
        try {
            Class<?> clazz = node.getClass();
            java.lang.reflect.Field valField = clazz.getField("val");
            java.lang.reflect.Field nextField = clazz.getField("next");

            List<String> result = new ArrayList<>();
            Object current = node;
            int maxLen = 10000; // Prevent infinite loop for cyclic lists

            while (current != null && result.size() < maxLen) {
                result.add(String.valueOf(valField.get(current)));
                current = nextField.get(current);
            }

            return "[" + String.join(",", result) + "]";
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    private static String formatNode(Object node) {
        if (node == null) return "null";
        try {
            boolean hasRandom = false;
            boolean hasNeighbors = false;
            for (java.lang.reflect.Field field : node.getClass().getFields()) {
                if (field.getName().equals("random")) hasRandom = true;
                if (field.getName().equals("neighbors")) hasNeighbors = true;
            }
            if (hasRandom) {
                java.lang.reflect.Field valField = node.getClass().getField("val");
                java.lang.reflect.Field nextField = node.getClass().getField("next");
                java.lang.reflect.Field randomField = node.getClass().getField("random");
                List<Object> nodes = new ArrayList<>();
                Map<Object, Integer> indexByNode = new HashMap<>();
                Object current = node;
                while (current != null) {
                    indexByNode.put(current, nodes.size());
                    nodes.add(current);
                    current = nextField.get(current);
                }
                StringJoiner joiner = new StringJoiner(",", "[", "]");
                for (Object item : nodes) {
                    Object random = randomField.get(item);
                    String randomIndex = random == null ? "null" : String.valueOf(indexByNode.get(random));
                    joiner.add("[" + valField.get(item) + "," + randomIndex + "]");
                }
                return joiner.toString();
            }
            if (hasNeighbors) {
                java.lang.reflect.Field valField = node.getClass().getField("val");
                java.lang.reflect.Field neighborsField = node.getClass().getField("neighbors");
                Map<Object, Integer> seen = new LinkedHashMap<>();
                Queue<Object> queue = new LinkedList<>();
                seen.put(node, 0);
                queue.offer(node);
                while (!queue.isEmpty()) {
                    Object current = queue.poll();
                    @SuppressWarnings("unchecked")
                    List<Object> neighbors = (List<Object>) neighborsField.get(current);
                    for (Object neighbor : neighbors) {
                        if (!seen.containsKey(neighbor)) {
                            seen.put(neighbor, seen.size());
                            queue.offer(neighbor);
                        }
                    }
                }
                List<Object> ordered = new ArrayList<>(seen.keySet());
                ordered.sort((left, right) -> {
                    try {
                        return Integer.compare((int) valField.get(left), (int) valField.get(right));
                    } catch (Exception ignored) {
                        return 0;
                    }
                });
                StringJoiner joiner = new StringJoiner(",", "[", "]");
                for (Object item : ordered) {
                    @SuppressWarnings("unchecked")
                    List<Object> neighbors = (List<Object>) neighborsField.get(item);
                    StringJoiner neighborJoiner = new StringJoiner(",", "[", "]");
                    for (Object neighbor : neighbors) {
                        neighborJoiner.add(String.valueOf((int) valField.get(neighbor)));
                    }
                    joiner.add(neighborJoiner.toString());
                }
                return joiner.toString();
            }
            return String.valueOf(node);
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    /**
     * Compare actual output against expected, with normalization and special cases.
     * Handles null/[] equivalence, double precision, and whitespace differences.
     */
    private static boolean outputsMatch(String actual, String expected, Class<?> returnType) {
        if (actual == null) actual = "null";
        if (expected == null) expected = "";

        // Exact match after normalization
        String normActual = normalizeOutput(actual);
        String normExpected = normalizeOutput(expected);
        if (normActual.equals(normExpected)) return true;

        // null / [] equivalence
        Set<String> emptyEquivalents = Set.of("null", "[]", "");
        if (emptyEquivalents.contains(normActual) && emptyEquivalents.contains(normExpected)) {
            return true;
        }

        // Double/float precision: try parsing as doubles
        try {
            double a = Double.parseDouble(actual.trim());
            double b = Double.parseDouble(expected.trim());
            if (Math.abs(a - b) < 1e-5) return true;
        } catch (NumberFormatException ignored) {}

        if (returnType != null) {
            String typeName = returnType.getSimpleName();
            if (typeName.equals("TreeNode")) {
                return compareTreeNode(normActual, normExpected);
            }
            if (typeName.equals("ListNode")) {
                return compareListNode(normActual, normExpected);
            }
            if (typeName.equals("Node")) {
                if (Set.of("null", "[]", "").contains(normActual) && Set.of("null", "[]", "").contains(normExpected)) {
                    return true;
                }
                return normActual.equals(normExpected);
            }
        }

        if (expected.contains(".") && actual.contains(".") && compareDoubleArrays(actual, expected)) {
            return true;
        }

        if (compareJsonArraysWithNumericTolerance(actual, expected)) {
            return true;
        }

        return false;
    }

    private static boolean compareJsonArraysWithNumericTolerance(String actual, String expected) {
        try {
            JsonElement actualElement = JsonParser.parseString(actual.trim());
            JsonElement expectedElement = JsonParser.parseString(expected.trim());
            if (!actualElement.isJsonArray() || !expectedElement.isJsonArray()) {
                return false;
            }
            return jsonElementsMatchWithNumericTolerance(actualElement, expectedElement);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean jsonElementsMatchWithNumericTolerance(JsonElement actual, JsonElement expected) {
        if (actual == null || actual.isJsonNull()) {
            return expected == null || expected.isJsonNull();
        }
        if (expected == null || expected.isJsonNull()) {
            return false;
        }
        if (actual.isJsonArray() && expected.isJsonArray()) {
            JsonArray actualArray = actual.getAsJsonArray();
            JsonArray expectedArray = expected.getAsJsonArray();
            if (actualArray.size() != expectedArray.size()) {
                return false;
            }
            for (int i = 0; i < actualArray.size(); i++) {
                if (!jsonElementsMatchWithNumericTolerance(actualArray.get(i), expectedArray.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (actual.isJsonPrimitive() && expected.isJsonPrimitive()) {
            JsonPrimitive actualPrimitive = actual.getAsJsonPrimitive();
            JsonPrimitive expectedPrimitive = expected.getAsJsonPrimitive();
            if (actualPrimitive.isNumber() && expectedPrimitive.isNumber()) {
                return Math.abs(actualPrimitive.getAsDouble() - expectedPrimitive.getAsDouble()) < 1e-5;
            }
            return normalizeOutput(actualPrimitive.toString()).equals(normalizeOutput(expectedPrimitive.toString()));
        }
        return false;
    }

    private static boolean compareTreeNode(String actual, String expected) {
        if (Set.of("null", "[]", "").contains(actual) && Set.of("null", "[]", "").contains(expected)) {
            return true;
        }
        String trimmedActual = actual.replaceAll("(,null)+]$", "]");
        String trimmedExpected = expected.replaceAll("(,null)+]$", "]");
        if (trimmedActual.equals(trimmedExpected)) {
            return true;
        }
        if (expected.matches("-?\\d+")) {
            String firstValue = actual.replaceAll("[\\[\\]]", "").split(",")[0];
            return firstValue.equals(expected);
        }
        return false;
    }

    private static boolean compareListNode(String actual, String expected) {
        if (Set.of("null", "[]", "").contains(actual) && Set.of("null", "[]", "").contains(expected)) {
            return true;
        }
        if (expected.startsWith("intersectedat")) {
            Matcher matcher = Pattern.compile("\\d+").matcher(expected);
            if (matcher.find()) {
                String firstValue = actual.replaceAll("[\\[\\]]", "").split(",")[0];
                return firstValue.equals(matcher.group());
            }
        }
        if (expected.equals("nointersection")) {
            return actual.equals("null") || actual.equals("[]");
        }
        return actual.equals(expected);
    }

    private static boolean requiresSpecialOutputMatch(Method method, String problemId) {
        String id = normalizeProblemId(problemId);
        return (id.equals("26") && "removeDuplicates".equals(method.getName()))
                || (id.equals("27") && "removeElement".equals(method.getName()));
    }

    private static boolean specialOutputMatches(Method method, Object invokeResult, Object[] args,
                                                String input, String problemId, String expectedOutput) {
        String id = normalizeProblemId(problemId);
        if (id.equals("1") && "twoSum".equals(method.getName())) {
            return twoSumZeroBasedOutputMatches(invokeResult, input);
        }
        if (id.equals("4") && "groupAnagrams".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, true);
        }
        if (id.equals("5") && "topKFrequent".equals(method.getName())) {
            return topKFrequentOutputMatches(invokeResult, input);
        }
        if (id.equals("15") && "intersect".equals(method.getName())) {
            return intArrayMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("16") && "threeSum".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, true);
        }
        if (id.equals("17") && "fourSum".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, true);
        }
        if (id.equals("22") && "twoSum".equals(method.getName())) {
            return twoSumOneBasedOutputMatches(invokeResult, input);
        }
        if (id.equals("26") && "removeDuplicates".equals(method.getName())) {
            return removeDuplicatesOutputMatches(invokeResult, args, input, expectedOutput);
        }
        if (id.equals("27") && "removeElement".equals(method.getName())) {
            return removeElementOutputMatches(invokeResult, args, input, expectedOutput);
        }
        if (id.equals("44") && "generateParenthesis".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("108") && "kClosest".equals(method.getName())) {
            return kClosestOutputMatches(invokeResult, input);
        }
        if (id.equals("103") && "findWords".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("115") && "kSmallestPairs".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("116") && "subsets".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, true);
        }
        if (id.equals("117") && "combinationSum".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, true);
        }
        if (id.equals("118") && "permute".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("119") && "subsetsWithDup".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, true);
        }
        if (id.equals("120") && "combinationSum2".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, true);
        }
        if (id.equals("122") && "partition".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("123") && "letterCombinations".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("124") && "solveNQueens".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("126") && "restoreIpAddresses".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("133") && "pacificAtlantic".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("141") && "findLadders".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("136") && "findOrder".equals(method.getName())) {
            return courseScheduleOrderMatches(invokeResult, input, expectedOutput);
        }
        if (id.equals("146") && "alienOrder".equals(method.getName())) {
            return alienDictionaryOrderMatches(invokeResult, input, expectedOutput);
        }
        if (id.equals("221") && "intersection".equals(method.getName())) {
            return intArraySetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("228") && "findDuplicates".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("236") && "sortArrayByParity".equals(method.getName())) {
            return sortArrayByParityOutputMatches(invokeResult, input);
        }
        if (id.equals("242") && "allCellsDistOrder".equals(method.getName())) {
            return allCellsDistOrderOutputMatches(invokeResult, input);
        }
        if (id.equals("305") && "findSubstring".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("341") && "removeInvalidParentheses".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("348") && "findAllConcatenatedWordsInADict".equals(method.getName())) {
            return jsonFlatMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("363") && "palindromePairs".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("376") && "crackSafe".equals(method.getName())) {
            return crackingSafeOutputMatches(invokeResult, input);
        }
        if (id.equals("399") && "smallestSufficientTeam".equals(method.getName())) {
            return smallestSufficientTeamOutputMatches(invokeResult, input, expectedOutput);
        }
        if (id.equals("432") && "findOriginalArray".equals(method.getName())) {
            return intArrayMultisetMatches(invokeResult, expectedOutput);
        }
        if (id.equals("455") && "missingRolls".equals(method.getName())) {
            return missingRollsOutputMatches(invokeResult, input);
        }
        if (id.equals("487") && "findDuplicateSubtrees".equals(method.getName())) {
            return jsonNestedOuterMultisetMatches(invokeResult, expectedOutput, false);
        }
        if (id.equals("497") && "constructArray".equals(method.getName())) {
            return beautifulArrangementOutputMatches(invokeResult, input);
        }
        return false;
    }

    private static String normalizeProblemId(String problemId) {
        if (problemId == null) {
            return "";
        }
        String trimmed = problemId.trim();
        int colon = trimmed.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < trimmed.length()) {
            trimmed = trimmed.substring(colon + 1);
        }
        return trimmed.startsWith("p") ? trimmed.substring(1) : trimmed;
    }

    private static boolean twoSumZeroBasedOutputMatches(Object result, String input) {
        if (!(result instanceof int[] actual) || actual.length != 2 || actual[0] == actual[1]) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int[] nums = parseIntArray(named.getOrDefault("nums", "[]"));
            int target = Integer.parseInt(named.get("target").trim());
            return actual[0] >= 0 && actual[0] < nums.length
                    && actual[1] >= 0 && actual[1] < nums.length
                    && nums[actual[0]] + nums[actual[1]] == target;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean twoSumOneBasedOutputMatches(Object result, String input) {
        if (!(result instanceof int[] actual) || actual.length != 2 || actual[0] == actual[1]) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int[] numbers = parseIntArray(named.getOrDefault("numbers", named.getOrDefault("nums", "[]")));
            int target = Integer.parseInt(named.get("target").trim());
            int left = actual[0] - 1;
            int right = actual[1] - 1;
            return left >= 0 && left < numbers.length
                    && right >= 0 && right < numbers.length
                    && actual[0] < actual[1]
                    && numbers[left] + numbers[right] == target;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean topKFrequentOutputMatches(Object result, String input) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int[] nums = parseIntArray(named.getOrDefault("nums", "[]"));
            int k = Integer.parseInt(named.get("k").trim());
            if (actual.length != k) {
                return false;
            }
            Map<Integer, Integer> frequency = new HashMap<>();
            for (int num : nums) {
                frequency.merge(num, 1, Integer::sum);
            }
            List<Integer> counts = new ArrayList<>(frequency.values());
            counts.sort(Comparator.reverseOrder());
            if (k <= 0 || k > counts.size()) {
                return false;
            }
            int threshold = counts.get(k - 1);
            Set<Integer> seen = new HashSet<>();
            for (int value : actual) {
                Integer count = frequency.get(value);
                if (count == null || count < threshold || !seen.add(value)) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean removeDuplicatesOutputMatches(Object result, Object[] args,
                                                         String input, String expectedOutput) {
        if (!(result instanceof Number number)
                || args == null
                || args.length == 0
                || !(args[0] instanceof int[] actualNums)) {
            return false;
        }
        try {
            int actualLength = number.intValue();
            int expectedLength = parseLeadingInt(expectedOutput);
            int[] original = parseIntArray(parseNamedArguments(input).getOrDefault("nums", "[]"));
            int[] expectedPrefix = uniqueSortedPrefix(original);
            if (expectedLength != expectedPrefix.length
                    || actualLength != expectedLength
                    || actualLength < 0
                    || actualLength > actualNums.length) {
                return false;
            }
            for (int i = 0; i < expectedPrefix.length; i++) {
                if (actualNums[i] != expectedPrefix[i]) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean removeElementOutputMatches(Object result, Object[] args,
                                                      String input, String expectedOutput) {
        if (!(result instanceof Number number)
                || args == null
                || args.length == 0
                || !(args[0] instanceof int[] actualNums)) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int actualLength = number.intValue();
            int expectedLength = parseLeadingInt(expectedOutput);
            int[] original = parseIntArray(named.getOrDefault("nums", "[]"));
            int removedValue = Integer.parseInt(named.get("val").trim());
            int[] expectedPrefix = valuesExcept(original, removedValue);
            return expectedLength == expectedPrefix.length
                    && actualLength == expectedLength
                    && actualLength >= 0
                    && actualLength <= actualNums.length
                    && prefixMultisetMatches(actualNums, actualLength, expectedPrefix);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static int parseLeadingInt(String text) {
        Matcher matcher = Pattern.compile("-?\\d+").matcher(text == null ? "" : text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Expected output does not contain a length");
        }
        return Integer.parseInt(matcher.group());
    }

    private static int[] uniqueSortedPrefix(int[] values) {
        if (values.length == 0) {
            return new int[0];
        }
        int[] prefix = new int[values.length];
        int size = 0;
        for (int value : values) {
            if (size == 0 || prefix[size - 1] != value) {
                prefix[size] = value;
                size++;
            }
        }
        return Arrays.copyOf(prefix, size);
    }

    private static int[] valuesExcept(int[] values, int removedValue) {
        int[] kept = new int[values.length];
        int size = 0;
        for (int value : values) {
            if (value != removedValue) {
                kept[size] = value;
                size++;
            }
        }
        return Arrays.copyOf(kept, size);
    }

    private static boolean prefixMultisetMatches(int[] actual, int length, int[] expected) {
        if (length != expected.length || length < 0 || length > actual.length) {
            return false;
        }
        int[] actualPrefix = Arrays.copyOf(actual, length);
        int[] expectedSorted = Arrays.copyOf(expected, expected.length);
        Arrays.sort(actualPrefix);
        Arrays.sort(expectedSorted);
        return Arrays.equals(actualPrefix, expectedSorted);
    }

    private static boolean intArrayMultisetMatches(Object result, String expectedOutput) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] expected = parseIntArray(expectedOutput);
            Arrays.sort(actual);
            Arrays.sort(expected);
            return Arrays.equals(actual, expected);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean intArraySetMatches(Object result, String expectedOutput) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] expected = parseIntArray(expectedOutput);
            Set<Integer> actualSet = new HashSet<>();
            for (int value : actual) {
                if (!actualSet.add(value)) {
                    return false;
                }
            }
            Set<Integer> expectedSet = new HashSet<>();
            for (int value : expected) {
                expectedSet.add(value);
            }
            return actualSet.equals(expectedSet);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean jsonFlatMultisetMatches(Object result, String expectedOutput) {
        try {
            JsonArray actual = parseOutputArray(formatResult(result));
            JsonArray expected = parseOutputArray(expectedOutput);
            return jsonMultiset(actual, false).equals(jsonMultiset(expected, false));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean jsonNestedOuterMultisetMatches(Object result, String expectedOutput, boolean sortInnerArrays) {
        try {
            JsonArray actual = parseOutputArray(formatResult(result));
            JsonArray expected = parseOutputArray(expectedOutput);
            return jsonMultiset(actual, sortInnerArrays).equals(jsonMultiset(expected, sortInnerArrays));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static JsonArray parseOutputArray(String output) {
        JsonElement parsed = JsonParser.parseString(output);
        if (!parsed.isJsonArray()) {
            throw new IllegalArgumentException("Output is not an array");
        }
        return parsed.getAsJsonArray();
    }

    private static Map<String, Integer> jsonMultiset(JsonArray array, boolean sortInnerArrays) {
        Map<String, Integer> multiset = new HashMap<>();
        for (JsonElement element : array) {
            String key = canonicalJsonValue(element, sortInnerArrays);
            multiset.merge(key, 1, Integer::sum);
        }
        return multiset;
    }

    private static String canonicalJsonValue(JsonElement element, boolean sortArrays) {
        if (!element.isJsonArray()) {
            return element.toString();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            values.add(canonicalJsonValue(child, sortArrays));
        }
        if (sortArrays) {
            Collections.sort(values);
        }
        return "[" + String.join(",", values) + "]";
    }

    private static boolean kClosestOutputMatches(Object result, String input) {
        if (!(result instanceof int[][] actual)) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int[][] points = parse2DIntArray(named.getOrDefault("points", "[]"));
            int k = Integer.parseInt(named.get("k").trim());
            if (actual.length != k) {
                return false;
            }
            List<Long> distances = new ArrayList<>();
            Map<String, Integer> pointCounts = new HashMap<>();
            for (int[] point : points) {
                if (point.length != 2) {
                    return false;
                }
                distances.add(squaredDistance(point));
                pointCounts.merge(point[0] + "," + point[1], 1, Integer::sum);
            }
            distances.sort(Long::compareTo);
            if (k < 0 || k > distances.size()) {
                return false;
            }
            long threshold = k == 0 ? Long.MIN_VALUE : distances.get(k - 1);
            for (int[] point : actual) {
                if (point == null || point.length != 2 || squaredDistance(point) > threshold) {
                    return false;
                }
                String key = point[0] + "," + point[1];
                Integer count = pointCounts.get(key);
                if (count == null || count == 0) {
                    return false;
                }
                pointCounts.put(key, count - 1);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static long squaredDistance(int[] point) {
        return (long) point[0] * point[0] + (long) point[1] * point[1];
    }

    private static boolean courseScheduleOrderMatches(Object result, String input, String expectedOutput) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] expected = parseIntArray(expectedOutput);
            if (expected.length == 0) {
                return actual.length == 0;
            }
            Map<String, String> named = parseNamedArguments(input);
            int numCourses = Integer.parseInt(named.get("numCourses").trim());
            int[][] prerequisites = parse2DIntArray(named.getOrDefault("prerequisites", "[]"));
            if (actual.length != numCourses) {
                return false;
            }
            int[] position = new int[numCourses];
            Arrays.fill(position, -1);
            for (int i = 0; i < actual.length; i++) {
                int course = actual[i];
                if (course < 0 || course >= numCourses || position[course] != -1) {
                    return false;
                }
                position[course] = i;
            }
            for (int[] edge : prerequisites) {
                if (edge.length != 2) {
                    return false;
                }
                int course = edge[0];
                int prerequisite = edge[1];
                if (position[prerequisite] > position[course]) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean alienDictionaryOrderMatches(Object result, String input, String expectedOutput) {
        if (!(result instanceof String actual)) {
            return false;
        }
        try {
            String expected = unquoteStringToken(expectedOutput);
            if (expected.isEmpty()) {
                return actual.isEmpty();
            }
            if (actual.isEmpty()) {
                return false;
            }

            Map<String, String> named = parseNamedArguments(input);
            String[] words = parseStringArray(named.getOrDefault("words", "[]"));
            Set<Character> present = new HashSet<>();
            for (String word : words) {
                for (int i = 0; i < word.length(); i++) {
                    present.add(word.charAt(i));
                }
            }

            if (actual.length() != present.size()) {
                return false;
            }
            Map<Character, Integer> position = new HashMap<>();
            for (int i = 0; i < actual.length(); i++) {
                char ch = actual.charAt(i);
                if (!present.contains(ch) || position.put(ch, i) != null) {
                    return false;
                }
            }

            for (int i = 0; i + 1 < words.length; i++) {
                String first = words[i];
                String second = words[i + 1];
                if (first.length() > second.length() && first.startsWith(second)) {
                    return false;
                }
                int limit = Math.min(first.length(), second.length());
                for (int j = 0; j < limit; j++) {
                    char from = first.charAt(j);
                    char to = second.charAt(j);
                    if (from != to) {
                        if (position.get(from) > position.get(to)) {
                            return false;
                        }
                        break;
                    }
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean allCellsDistOrderOutputMatches(Object result, String input) {
        if (!(result instanceof int[][] actual)) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int rows = Integer.parseInt(named.get("rows").trim());
            int cols = Integer.parseInt(named.get("cols").trim());
            int rCenter = Integer.parseInt(named.get("rCenter").trim());
            int cCenter = Integer.parseInt(named.get("cCenter").trim());
            if (actual.length != rows * cols) {
                return false;
            }
            boolean[][] seen = new boolean[rows][cols];
            int previousDistance = -1;
            for (int[] cell : actual) {
                if (cell == null || cell.length != 2) {
                    return false;
                }
                int row = cell[0];
                int col = cell[1];
                if (row < 0 || row >= rows || col < 0 || col >= cols || seen[row][col]) {
                    return false;
                }
                int distance = Math.abs(row - rCenter) + Math.abs(col - cCenter);
                if (distance < previousDistance) {
                    return false;
                }
                previousDistance = distance;
                seen[row][col] = true;
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean sortArrayByParityOutputMatches(Object result, String input) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int[] nums = parseIntArray(named.getOrDefault("nums", "[]"));
            if (actual.length != nums.length) {
                return false;
            }

            int[] expectedValues = Arrays.copyOf(nums, nums.length);
            int[] actualValues = Arrays.copyOf(actual, actual.length);
            Arrays.sort(expectedValues);
            Arrays.sort(actualValues);
            if (!Arrays.equals(expectedValues, actualValues)) {
                return false;
            }

            boolean seenOdd = false;
            for (int value : actual) {
                if (value % 2 == 0) {
                    if (seenOdd) {
                        return false;
                    }
                } else {
                    seenOdd = true;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean smallestSufficientTeamOutputMatches(Object result, String input, String expectedOutput) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] expected = parseIntArray(expectedOutput);
            if (actual.length != expected.length) {
                return false;
            }

            Map<String, String> named = parseNamedArguments(input);
            String[] reqSkills = parseStringArray(named.getOrDefault("req_skills", "[]"));
            String[][] people = parse2DStringArray(named.getOrDefault("people", "[]"));
            Map<String, Integer> skillIndex = new HashMap<>();
            for (int i = 0; i < reqSkills.length; i++) {
                skillIndex.put(reqSkills[i], i);
            }

            boolean[] selected = new boolean[people.length];
            boolean[] covered = new boolean[reqSkills.length];
            for (int person : actual) {
                if (person < 0 || person >= people.length || selected[person]) {
                    return false;
                }
                selected[person] = true;
                for (String skill : people[person]) {
                    Integer index = skillIndex.get(skill);
                    if (index != null) {
                        covered[index] = true;
                    }
                }
            }
            for (boolean skillCovered : covered) {
                if (!skillCovered) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean crackingSafeOutputMatches(Object result, String input) {
        if (!(result instanceof String actual)) {
            return false;
        }
        try {
            Map<String, String> named = parseNamedArguments(input);
            int n = Integer.parseInt(named.get("n").trim());
            int k = Integer.parseInt(named.get("k").trim());
            if (n <= 0 || k <= 0 || k > 10) {
                return false;
            }

            int totalPasswords = 1;
            for (int i = 0; i < n; i++) {
                totalPasswords *= k;
            }
            if (actual.length() != totalPasswords + n - 1) {
                return false;
            }

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < actual.length(); i++) {
                char digit = actual.charAt(i);
                if (digit < '0' || digit >= '0' + k) {
                    return false;
                }
                if (i + 1 >= n) {
                    seen.add(actual.substring(i + 1 - n, i + 1));
                }
            }
            return seen.size() == totalPasswords;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean missingRollsOutputMatches(Object result, String input) {
        if (!(result instanceof int[] actual)) {
            return false;
        }

        try {
            Map<String, String> named = parseNamedArguments(input);
            int[] rolls = parseIntArray(named.getOrDefault("rolls", "[]"));
            int mean = Integer.parseInt(named.get("mean").trim());
            int n = Integer.parseInt(named.get("n").trim());

            long known = 0;
            for (int roll : rolls) {
                known += roll;
            }

            long missing = (long) mean * (rolls.length + n) - known;
            if (missing < n || missing > 6L * n) {
                return actual.length == 0;
            }
            if (actual.length != n) {
                return false;
            }

            long actualSum = 0;
            for (int roll : actual) {
                if (roll < 1 || roll > 6) {
                    return false;
                }
                actualSum += roll;
            }
            return actualSum == missing;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean beautifulArrangementOutputMatches(Object result, String input) {
        if (!(result instanceof int[] actual)) {
            return false;
        }

        try {
            Map<String, String> named = parseNamedArguments(input);
            int n = Integer.parseInt(named.get("n").trim());
            int k = Integer.parseInt(named.get("k").trim());
            if (actual.length != n) {
                return false;
            }
            boolean[] seen = new boolean[n + 1];
            for (int value : actual) {
                if (value < 1 || value > n || seen[value]) {
                    return false;
                }
                seen[value] = true;
            }
            Set<Integer> differences = new HashSet<>();
            for (int i = 1; i < actual.length; i++) {
                differences.add(Math.abs(actual[i] - actual[i - 1]));
            }
            return differences.size() == k;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static List<String> splitTopLevel(String text, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (ch == '[' || ch == '(' || ch == '{') {
                    depth++;
                } else if (ch == ']' || ch == ')' || ch == '}') {
                    depth--;
                } else if (ch == delimiter && depth == 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }

    private static boolean compareDoubleArrays(String actual, String expected) {
        try {
            double[] actualValues = parseDoubles(actual);
            double[] expectedValues = parseDoubles(expected);
            if (actualValues.length != expectedValues.length) {
                return false;
            }
            for (int i = 0; i < actualValues.length; i++) {
                if (Math.abs(actualValues[i] - expectedValues[i]) >= 1e-5) {
                    return false;
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static double[] parseDoubles(String text) {
        String inner = text.trim().replaceAll("[\\[\\]]", "");
        if (inner.isBlank()) {
            return new double[0];
        }
        String[] parts = inner.split(",");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i].trim());
        }
        return values;
    }
}
