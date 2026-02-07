package com.crabmods.algocraft.logic;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * A sandboxed ClassLoader that restricts which classes can be loaded.
 * This provides an additional layer of security beyond pattern matching.
 * 
 * <p>Security model:
 * <ul>
 *   <li>Only explicitly allowed packages can be loaded</li>
 *   <li>Dangerous classes are blocked even if in allowed packages</li>
 *   <li>User code is loaded from the provided URL (temp directory)</li>
 * </ul>
 * 
 * <p>Performance: Minimal overhead - just a Set lookup per class load.
 */
public class SandboxClassLoader extends URLClassLoader {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Packages that are allowed to be loaded.
     * These are safe standard library packages needed for algorithm solutions.
     */
    private static final Set<String> ALLOWED_PACKAGES = Set.of(
        // Core Java
        "java.lang",
        "java.util",
        "java.util.function",
        "java.util.stream",
        "java.util.regex",
        "java.math",
        
        // Primitives and basic types (needed for arrays, etc.)
        "java.io.Serializable",  // Interface only, for Collections
        
        // The user's Solution class (no package)
        ""
    );
    
    /**
     * Specific classes that are blocked even if in allowed packages.
     * These are dangerous classes that could be used for attacks.
     */
    private static final Set<String> BLOCKED_CLASSES = Set.of(
        // Process execution
        "java.lang.ProcessBuilder",
        "java.lang.Process",
        "java.lang.ProcessHandle",
        "java.lang.Runtime",
        
        // Classloading and reflection attacks
        "java.lang.ClassLoader",
        "java.lang.Class",  // Block direct Class usage (we provide it)
        "java.lang.reflect.Proxy",
        "java.lang.invoke.MethodHandles",
        "java.lang.invoke.MethodHandle",
        "java.lang.invoke.VarHandle",
        
        // Threading (could be used for DoS)
        "java.lang.Thread",
        "java.lang.ThreadGroup",
        "java.lang.ThreadLocal",
        "java.lang.InheritableThreadLocal",
        
        // System access
        "java.lang.System",  // We don't need System in solutions
        "java.lang.SecurityManager",
        "java.lang.Compiler",
        
        // Unsafe operations
        "sun.misc.Unsafe",
        "jdk.internal.misc.Unsafe"
    );
    
    /**
     * Classes that are explicitly allowed even though they might seem dangerous.
     * These are needed for normal algorithm operations.
     */
    private static final Set<String> ALLOWED_CLASSES = Set.of(
        // Math operations
        "java.lang.Math",
        "java.lang.StrictMath",
        
        // Primitive wrappers
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Boolean",
        "java.lang.Character",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Number",
        
        // String operations
        "java.lang.String",
        "java.lang.StringBuilder",
        "java.lang.StringBuffer",
        "java.lang.CharSequence",
        
        // Basic types
        "java.lang.Object",
        "java.lang.Comparable",
        "java.lang.Cloneable",
        "java.lang.Iterable",
        "java.lang.AutoCloseable",
        
        // Exceptions (needed for error handling)
        "java.lang.Exception",
        "java.lang.RuntimeException",
        "java.lang.Error",
        "java.lang.Throwable",
        "java.lang.IllegalArgumentException",
        "java.lang.IllegalStateException",
        "java.lang.NullPointerException",
        "java.lang.IndexOutOfBoundsException",
        "java.lang.ArrayIndexOutOfBoundsException",
        "java.lang.StringIndexOutOfBoundsException",
        "java.lang.ArithmeticException",
        "java.lang.NumberFormatException",
        "java.lang.UnsupportedOperationException",
        
        // Functional interfaces
        "java.lang.Runnable",
        "java.lang.FunctionalInterface",
        
        // Enum support
        "java.lang.Enum",
        
        // Annotation
        "java.lang.Override",
        "java.lang.Deprecated",
        "java.lang.SuppressWarnings"
    );
    
    /**
     * No-package classes loaded from URLs (user code + helper classes like TreeNode, ListNode).
     * Any class without a package is considered user code compiled in the temp directory.
     */

    private final boolean strictMode;
    
    /**
     * Create a sandboxed ClassLoader.
     * 
     * @param urls URLs to load user classes from (typically temp directory)
     * @param parent Parent classloader
     * @param strictMode If true, only explicitly allowed classes can be loaded
     */
    public SandboxClassLoader(URL[] urls, ClassLoader parent, boolean strictMode) {
        super(urls, parent);
        this.strictMode = strictMode;
    }
    
    /**
     * Create a sandboxed ClassLoader with strict mode enabled.
     */
    public SandboxClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, true);
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // User code classes (no package) - load from URLs in temp directory.
        // Covers Solution, TreeNode, ListNode, Node, design classes (MinStack, etc.)
        if (!name.contains(".")) {
            return super.loadClass(name, resolve);
        }
        
        // Check if the class is allowed
        if (!isClassAllowed(name)) {
            LOGGER.warn("Blocked attempt to load restricted class: {}", name);
            throw new SecurityException("Access to class '" + name + "' is not allowed in sandbox");
        }
        
        // Delegate to parent for allowed classes
        return super.loadClass(name, resolve);
    }
    
    /**
     * Check if a class is allowed to be loaded in the sandbox.
     * 
     * @param className Fully qualified class name
     * @return true if the class can be loaded
     */
    private boolean isClassAllowed(String className) {
        // Explicitly blocked classes
        if (BLOCKED_CLASSES.contains(className)) {
            return false;
        }
        
        // Explicitly allowed classes (whitelist)
        if (ALLOWED_CLASSES.contains(className)) {
            return true;
        }
        
        // Check if in allowed package
        String packageName = getPackageName(className);
        
        // java.util.* is fully allowed (except specific blocked classes)
        if (packageName.equals("java.util") || 
            packageName.startsWith("java.util.") ||
            packageName.equals("java.util.function") ||
            packageName.equals("java.util.stream") ||
            packageName.equals("java.util.regex") ||
            packageName.equals("java.math")) {
            return true;
        }
        
        // java.lang package needs more careful filtering
        if (packageName.equals("java.lang")) {
            // In strict mode, only explicitly allowed java.lang classes
            if (strictMode) {
                return ALLOWED_CLASSES.contains(className);
            }
            // In non-strict mode, allow unless blocked
            return !BLOCKED_CLASSES.contains(className);
        }
        
        // Block everything else
        return false;
    }
    
    /**
     * Extract package name from fully qualified class name.
     */
    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
    
    /**
     * Check if a class name is safe before even attempting to load.
     * Can be used for early validation.
     * 
     * @param className The class name to check
     * @return true if the class would be allowed
     */
    public static boolean isClassNameSafe(String className) {
        if (BLOCKED_CLASSES.contains(className)) {
            return false;
        }
        
        String packageName = className.contains(".") 
            ? className.substring(0, className.lastIndexOf('.')) 
            : "";
        
        // Quick check for obviously dangerous packages
        if (packageName.startsWith("java.io") ||
            packageName.startsWith("java.net") ||
            packageName.startsWith("java.nio") ||
            packageName.startsWith("java.security") ||
            packageName.startsWith("javax.") ||
            packageName.startsWith("sun.") ||
            packageName.startsWith("jdk.") ||
            packageName.startsWith("com.sun.")) {
            return false;
        }
        
        return true;
    }
}
