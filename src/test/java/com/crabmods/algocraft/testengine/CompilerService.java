package com.crabmods.algocraft.testengine;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Compiles Java source code in-memory and loads the resulting class.
 * Supports injecting helper classes (TreeNode, ListNode, Node variants).
 */
public class CompilerService implements AutoCloseable {

    private Path tempDir;
    private URLClassLoader classLoader;

    /**
     * Compile source code and return the loaded class.
     * Automatically injects TreeNode/ListNode/Node if referenced in the code.
     *
     * @param className  the expected class name (e.g. "Solution" or "MinStack")
     * @param sourceCode the full Java source code
     * @return the compiled Class object
     */
    public Class<?> compile(String className, String sourceCode) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java Compiler not found. Run with JDK, not JRE.");
        }

        tempDir = Files.createTempDirectory("algocraft_test_");

        // Determine which helper classes to inject
        List<File> sourceFiles = new ArrayList<>();

        if (sourceCode.contains("TreeNode")) {
            File f = writeSource("TreeNode", TREE_NODE_SOURCE);
            sourceFiles.add(f);
        }
        if (sourceCode.contains("ListNode")) {
            File f = writeSource("ListNode", LIST_NODE_SOURCE);
            sourceFiles.add(f);
        }
        // N-ary tree Node / Graph Node / Random pointer Node
        if (sourceCode.contains("class Node") || sourceCode.contains("Node {")
                || sourceCode.contains("Node(") || sourceCode.contains("Node ")) {
            // Only inject if it's not a TreeNode or ListNode reference
            if (!sourceCode.contains("class TreeNode") && !sourceCode.contains("class ListNode")) {
                // Detect which Node variant is needed from the code
                if (sourceCode.contains("List<Node> children") || sourceCode.contains("children")) {
                    File f = writeSource("Node", NARY_NODE_SOURCE);
                    sourceFiles.add(f);
                } else if (sourceCode.contains("Node random") || sourceCode.contains(".random")) {
                    File f = writeSource("Node", RANDOM_POINTER_NODE_SOURCE);
                    sourceFiles.add(f);
                } else if (sourceCode.contains("List<Node> neighbors") || sourceCode.contains("neighbors")) {
                    File f = writeSource("Node", GRAPH_NODE_SOURCE);
                    sourceFiles.add(f);
                }
            }
        }

        // VersionControl parent class (for "First Bad Version" problem)
        if (sourceCode.contains("extends VersionControl")) {
            File f = writeSource("VersionControl", VERSION_CONTROL_SOURCE);
            sourceFiles.add(f);
        }

        // Prepend import java.util.* if not already present
        if (!sourceCode.contains("import java.util.*")) {
            sourceCode = "import java.util.*;\nimport java.util.stream.*;\n" + sourceCode;
        }

        // Detect actual class name in the source code (strip comments first)
        String actualClassName = className;
        String codeNoComments = sourceCode
                .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                .replaceAll("//[^\n]*", "");
        java.util.regex.Matcher cm = java.util.regex.Pattern
                .compile("(?:public\\s+)?class\\s+(\\w+)").matcher(codeNoComments);
        while (cm.find()) {
            String name = cm.group(1);
            // Skip helper classes we already handle
            if (!name.equals("TreeNode") && !name.equals("ListNode") && !name.equals("Node")) {
                actualClassName = name;
                break;
            }
        }

        // Write the main source file (filename must match the class name)
        File mainFile = writeSource(actualClassName, sourceCode);
        sourceFiles.add(mainFile);

        // Compile all files together
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            StringWriter output = new StringWriter();

            JavaCompiler.CompilationTask task = compiler.getTask(
                    output, fileManager, diagnostics,
                    List.of("-d", tempDir.toString(), "-Xlint:none"),
                    null, compilationUnits);

            boolean success = task.call();
            if (!success) {
                StringBuilder errorMsg = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        errorMsg.append("  Line ").append(d.getLineNumber())
                                .append(": ").append(d.getMessage(null)).append("\n");
                    }
                }
                throw new RuntimeException(errorMsg.toString());
            }
        }

        classLoader = new URLClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                getClass().getClassLoader());

        return Class.forName(actualClassName, true, classLoader);
    }

    private File writeSource(String className, String source) throws IOException {
        File file = new File(tempDir.toFile(), className + ".java");
        Files.writeString(file.toPath(), source);
        return file;
    }

    @Override
    public void close() {
        if (classLoader != null) {
            try { classLoader.close(); } catch (Exception ignored) {}
        }
        if (tempDir != null) {
            try {
                File dir = tempDir.toFile();
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) f.delete();
                }
                dir.delete();
            } catch (Exception ignored) {}
        }
    }

    // ─── Helper class source code ──────────────────────────────────────────

    static final String TREE_NODE_SOURCE = """
            public class TreeNode {
                public int val;
                public TreeNode left;
                public TreeNode right;
                public TreeNode() {}
                public TreeNode(int val) { this.val = val; }
                public TreeNode(int val, TreeNode left, TreeNode right) {
                    this.val = val;
                    this.left = left;
                    this.right = right;
                }
            }
            """;

    static final String LIST_NODE_SOURCE = """
            public class ListNode {
                public int val;
                public ListNode next;
                public ListNode() {}
                public ListNode(int val) { this.val = val; }
                public ListNode(int val, ListNode next) {
                    this.val = val;
                    this.next = next;
                }
            }
            """;

    static final String GRAPH_NODE_SOURCE = """
            import java.util.*;
            public class Node {
                public int val;
                public List<Node> neighbors;
                public Node() { val = 0; neighbors = new ArrayList<>(); }
                public Node(int val) { this.val = val; neighbors = new ArrayList<>(); }
                public Node(int val, List<Node> neighbors) {
                    this.val = val;
                    this.neighbors = neighbors;
                }
            }
            """;

    static final String NARY_NODE_SOURCE = """
            import java.util.*;
            public class Node {
                public int val;
                public List<Node> children;
                public Node() { children = new ArrayList<>(); }
                public Node(int val) { this.val = val; children = new ArrayList<>(); }
                public Node(int val, List<Node> children) {
                    this.val = val;
                    this.children = children;
                }
            }
            """;

    static final String RANDOM_POINTER_NODE_SOURCE = """
            public class Node {
                public int val;
                public Node next;
                public Node random;
                public Node(int val) { this.val = val; this.next = null; this.random = null; }
            }
            """;

    static final String VERSION_CONTROL_SOURCE = """
            public class VersionControl {
                private int badVersion = 1;
                public void setBadVersion(int bad) { this.badVersion = bad; }
                public boolean isBadVersion(int version) { return version >= badVersion; }
            }
            """;
}
