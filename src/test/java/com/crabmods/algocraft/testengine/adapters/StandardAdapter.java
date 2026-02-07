package com.crabmods.algocraft.testengine.adapters;

import com.crabmods.algocraft.testengine.*;

import java.lang.reflect.*;
import java.util.*;

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

            Object[] args;
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);

            // Special case: intersecting linked lists (P77 etc.)
            if (namedParams.containsKey("intersectVal") && namedParams.containsKey("skipA")) {
                args = buildIntersectingListArgs(namedParams, method);
            } else {
                args = InputParser.parseInput(input, method);
                // Post-process: inject cycle for "pos" parameter (P71 etc.)
                if (namedParams.containsKey("pos")) {
                    injectCycleIfNeeded(args, namedParams);
                }
            }

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

    // ─── Special data structure construction ───────────────────────

    /**
     * Inject a cycle into a ListNode argument if "pos" parameter is present and >= 0.
     */
    private static void injectCycleIfNeeded(Object[] args, Map<String, String> namedParams) {
        try {
            int pos = Integer.parseInt(namedParams.get("pos").trim());
            if (pos < 0 || args.length == 0 || args[0] == null) return;

            // Walk to find tail and target node
            Field nextField = args[0].getClass().getField("next");
            Object targetNode = args[0];
            for (int i = 0; i < pos; i++) {
                targetNode = nextField.get(targetNode);
                if (targetNode == null) return;
            }
            Object tail = args[0];
            while (nextField.get(tail) != null) {
                tail = nextField.get(tail);
            }
            nextField.set(tail, targetNode); // create cycle
        } catch (Exception ignored) {}
    }

    /**
     * Build intersecting linked list arguments from named params (intersectVal, listA, listB, skipA, skipB).
     */
    private static Object[] buildIntersectingListArgs(Map<String, String> namedParams, Method method)
            throws Exception {
        int skipA = Integer.parseInt(namedParams.get("skipA").trim());
        int skipB = Integer.parseInt(namedParams.get("skipB").trim());
        int[] valsA = InputParser.parseIntArray(namedParams.getOrDefault("listA", "[]"));
        int[] valsB = InputParser.parseIntArray(namedParams.getOrDefault("listB", "[]"));

        // Determine the ListNode class from the method parameter type
        Class<?> listNodeClass = method.getParameterTypes()[0];
        Constructor<?> ctor = listNodeClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        Field nextField = listNodeClass.getField("next");

        // Build the shared suffix (from valsA[skipA:])
        Object sharedHead = null;
        Object sharedTail = null;
        for (int i = skipA; i < valsA.length; i++) {
            Object node = ctor.newInstance(valsA[i]);
            if (sharedHead == null) { sharedHead = node; sharedTail = node; }
            else { nextField.set(sharedTail, node); sharedTail = node; }
        }

        // Build headA prefix and link to shared
        Object headA = sharedHead;
        for (int i = skipA - 1; i >= 0; i--) {
            Object node = ctor.newInstance(valsA[i]);
            nextField.set(node, headA);
            headA = node;
        }

        // Build headB prefix and link to shared
        Object headB = sharedHead;
        for (int i = skipB - 1; i >= 0; i--) {
            Object node = ctor.newInstance(valsB[i]);
            nextField.set(node, headB);
            headB = node;
        }

        // Handle no-intersection case (intersectVal == 0 or skipA == valsA.length)
        if (sharedHead == null) {
            headA = InputParser.buildListNode(namedParams.getOrDefault("listA", "[]"), listNodeClass);
            headB = InputParser.buildListNode(namedParams.getOrDefault("listB", "[]"), listNodeClass);
        }

        return new Object[]{ headA, headB };
    }

    private static boolean isObjectMethod(String name) {
        return switch (name) {
            case "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait" -> true;
            default -> false;
        };
    }
}
