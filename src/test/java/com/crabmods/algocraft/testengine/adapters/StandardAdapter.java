package com.crabmods.algocraft.testengine.adapters;

import com.crabmods.algocraft.testengine.*;

import java.lang.reflect.*;

/**
 * Test adapter for standard algorithm problems.
 * Handles: single method with input → output.
 * Also handles void methods (return modified first argument).
 */
public class StandardAdapter {

    /**
     * Run a single test case for a standard problem.
     *
     * @param solutionClass the compiled Solution class
     * @param input         raw test input string
     * @param expected      expected output string
     * @return TestResult with pass/fail and details
     */
    public static TestResult run(Class<?> solutionClass, String input, String expected) {
        try {
            Method method = findSolutionMethod(solutionClass);
            if (method == null) {
                return TestResult.error("No solution method found in " + solutionClass.getSimpleName());
            }
            method.setAccessible(true);

            java.lang.reflect.Constructor<?> ctor = solutionClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object instance = ctor.newInstance();

            // Handle special parent class setup (e.g., VersionControl.setBadVersion)
            setupParentClass(instance, input);

            Object[] args = InputParser.parseInput(input, method);
            Object result = method.invoke(instance, args);

            // Handle void methods: return the modified first argument
            String actualStr;
            Class<?> compareType;
            if (method.getReturnType() == void.class && args.length > 0) {
                actualStr = OutputComparator.format(args[0]);
                compareType = args[0] != null ? args[0].getClass() : void.class;
            } else {
                actualStr = OutputComparator.format(result);
                compareType = method.getReturnType();
            }

            boolean passed = OutputComparator.compare(actualStr, expected, compareType);
            if (passed) {
                return TestResult.pass();
            } else {
                return TestResult.fail(input, expected, actualStr);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            String msg = cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                    : e.getMessage();
            return TestResult.error(msg);
        } catch (Exception e) {
            return TestResult.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Find the solution method. Prefers public methods, falls back to any non-private non-Object method.
     */
    public static Method findSolutionMethod(Class<?> clazz) {
        // First pass: prefer public methods
        for (Method m : clazz.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !isObjectMethod(m.getName())) {
                return m;
            }
        }
        // Second pass: any non-synthetic, non-Object method
        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.isSynthetic() && !isObjectMethod(m.getName())) {
                return m;
            }
        }
        return null;
    }

    /**
     * Set up parent class state if needed (e.g., VersionControl.setBadVersion).
     */
    private static void setupParentClass(Object instance, String input) {
        try {
            // Check for "bad = X" parameter (First Bad Version)
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("bad\\s*=\\s*(\\d+)").matcher(input);
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

    private static boolean isObjectMethod(String name) {
        return switch (name) {
            case "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait" -> true;
            default -> false;
        };
    }
}
