package com.crabmods.algocraft.testengine.adapters;

import com.crabmods.algocraft.testengine.*;
import com.google.gson.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Test adapter for design class problems (MinStack, LRUCache, Trie, etc.).
 * <p>
 * Test format:
 * Input (two lines):
 *   Line 1: ["ClassName","method1","method2",...]
 *   Line 2: [[constructorArgs],[args1],[args2],...]
 * Output:
 *   [null,result1,result2,...]
 */
public class DesignClassAdapter {

    private static final Gson GSON = new Gson();

    /**
     * Run a design class test case.
     *
     * @param compiledClass the compiled design class
     * @param input         the two-line input (methods array + args array)
     * @param expected      the expected output array
     * @return TestResult with pass/fail
     */
    public static TestResult run(Class<?> compiledClass, String input, String expected) {
        try {
            // Parse the two-line input
            String[] lines = input.split("\n");
            if (lines.length < 2) {
                return TestResult.error("Design class test requires 2-line input format");
            }

            JsonArray methodNames = JsonParser.parseString(lines[0].trim()).getAsJsonArray();
            JsonArray allArgs = JsonParser.parseString(lines[1].trim()).getAsJsonArray();
            JsonArray expectedResults = JsonParser.parseString(expected.trim()).getAsJsonArray();

            if (methodNames.size() != allArgs.size()) {
                return TestResult.error("Method names and args arrays have different lengths");
            }

            // Create instance with constructor args
            JsonArray ctorArgs = allArgs.get(0).getAsJsonArray();
            Object instance = createInstance(compiledClass, ctorArgs);
            if (instance == null) {
                return TestResult.error("Failed to create instance of " + compiledClass.getSimpleName());
            }

            // Execute each method call
            List<String> actualResults = new ArrayList<>();
            actualResults.add("null"); // Constructor returns null

            for (int i = 1; i < methodNames.size(); i++) {
                String methodName = methodNames.get(i).getAsString();
                JsonArray methodArgs = allArgs.get(i).getAsJsonArray();

                try {
                    Object result = invokeMethod(instance, methodName, methodArgs);
                    if (result == null) {
                        actualResults.add("null");
                    } else if (result instanceof Integer || result instanceof Long
                            || result instanceof Double || result instanceof Float
                            || result instanceof Boolean) {
                        actualResults.add(result.toString());
                    } else if (result instanceof String) {
                        actualResults.add("\"" + result + "\"");
                    } else {
                        // Use OutputComparator for all other types (arrays, lists, etc.)
                        actualResults.add(OutputComparator.format(result));
                    }
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    return TestResult.error("Method " + methodName + " threw: "
                            + (cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                            : e.getMessage()));
                }
            }

            // Compare results
            String actualStr = "[" + String.join(",", actualResults) + "]";
            String normalizedActual = normalizeDesignOutput(actualStr);
            String normalizedExpected = normalizeDesignOutput(expected);

            if (normalizedActual.equals(normalizedExpected)) {
                return TestResult.pass();
            }

            // Try element-by-element comparison with problem-specific randomized design semantics.
            if (compareElementWise(actualResults, expectedResults, methodNames, allArgs)) {
                return TestResult.pass();
            }

            return TestResult.fail(input, expected, actualStr);
        } catch (Exception e) {
            return TestResult.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Create an instance of the design class using the constructor args.
     */
    private static Object createInstance(Class<?> clazz, JsonArray ctorArgs) throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        // Try to match constructor by argument count
        for (Constructor<?> ctor : constructors) {
            if (ctor.getParameterCount() == ctorArgs.size()) {
                ctor.setAccessible(true);
                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    args[i] = jsonToJava(ctorArgs.get(i), paramTypes[i]);
                }
                return ctor.newInstance(args);
            }
        }

        // Fallback: no-arg constructor
        if (ctorArgs.isEmpty()) {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        }

        throw new RuntimeException("No matching constructor found for " + clazz.getSimpleName()
                + " with " + ctorArgs.size() + " args");
    }

    /**
     * Invoke a named method on the instance with the given JSON args.
     */
    private static Object invokeMethod(Object instance, String methodName, JsonArray methodArgs) throws Exception {
        Class<?> clazz = instance.getClass();

        // Find method by name and parameter count
        Method bestMatch = null;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == methodArgs.size()) {
                bestMatch = m;
                break;
            }
        }

        if (bestMatch == null) {
            // Try parent class
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == methodArgs.size()) {
                    bestMatch = m;
                    break;
                }
            }
        }

        if (bestMatch == null) {
            throw new RuntimeException("Method not found: " + methodName + " with " + methodArgs.size() + " args");
        }

        bestMatch.setAccessible(true);
        Class<?>[] paramTypes = bestMatch.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = jsonToJava(methodArgs.get(i), paramTypes[i]);
        }

        Object result = bestMatch.invoke(instance, args);

        // void methods return null
        if (bestMatch.getReturnType() == void.class) return null;
        return result;
    }

    /**
     * Convert a JSON element to a Java object of the given type.
     */
    private static Object jsonToJava(JsonElement element, Class<?> targetType) {
        if (element == null || element.isJsonNull()) return null;

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
            String s = element.getAsString();
            return s.isEmpty() ? '\0' : s.charAt(0);
        }

        // Arrays
        if (targetType == int[].class && element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            int[] result = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) result[i] = arr.get(i).getAsInt();
            return result;
        }
        if (targetType == String[].class && element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            String[] result = new String[arr.size()];
            for (int i = 0; i < arr.size(); i++) result[i] = arr.get(i).getAsString();
            return result;
        }
        if (targetType == long[].class && element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            long[] result = new long[arr.size()];
            for (int i = 0; i < arr.size(); i++) result[i] = arr.get(i).getAsLong();
            return result;
        }
        if (targetType == int[][].class && element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            int[][] result = new int[arr.size()][];
            for (int i = 0; i < arr.size(); i++) {
                JsonArray row = arr.get(i).getAsJsonArray();
                result[i] = new int[row.size()];
                for (int j = 0; j < row.size(); j++) result[i][j] = row.get(j).getAsInt();
            }
            return result;
        }

        // List types
        if (targetType == List.class && element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            List<Object> list = new ArrayList<>();
            for (JsonElement e : arr) {
                if (e.isJsonPrimitive()) {
                    JsonPrimitive p = e.getAsJsonPrimitive();
                    if (p.isNumber()) list.add(p.getAsInt());
                    else if (p.isString()) list.add(p.getAsString());
                    else if (p.isBoolean()) list.add(p.getAsBoolean());
                } else if (e.isJsonArray()) {
                    // Nested list
                    List<Object> inner = new ArrayList<>();
                    for (JsonElement ie : e.getAsJsonArray()) {
                        if (ie.isJsonPrimitive()) {
                            JsonPrimitive p = ie.getAsJsonPrimitive();
                            if (p.isNumber()) inner.add(p.getAsInt());
                            else if (p.isString()) inner.add(p.getAsString());
                        }
                    }
                    list.add(inner);
                }
            }
            return list;
        }

        // Fallback: try as string
        return element.getAsString();
    }

    private static String normalizeDesignOutput(String s) {
        return s.replaceAll("\\s+", "").replace("\"", "").toLowerCase();
    }

    private static boolean compareElementWise(List<String> actual, JsonArray expected,
                                              JsonArray methodNames, JsonArray allArgs) {
        if (actual.size() != expected.size()) return false;
        RandomizedDesignState randomizedState = RandomizedDesignState.fromConstructor(methodNames);
        if (randomizedState != null && containsMethod(methodNames, "getRandom")) {
            return compareRandomizedDesignOutput(actual, expected, methodNames, allArgs, randomizedState);
        }

        for (int i = 0; i < actual.size(); i++) {
            String a = actual.get(i).trim();
            JsonElement e = expected.get(i);

            if (a.equals("null") && e.isJsonNull()) continue;
            if (e.isJsonNull() && a.equals("null")) continue;

            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                try {
                    double aVal = Double.parseDouble(a);
                    double eVal = e.getAsDouble();
                    if (Math.abs(aVal - eVal) >= 1e-5) return false;
                    continue;
                } catch (NumberFormatException ignored) {}
            }

            String normalizedA = a.replaceAll("\\s+", "").replace("\"", "").toLowerCase();
            String normalizedE = e.toString().replaceAll("\\s+", "").replace("\"", "").toLowerCase();
            if (!normalizedA.equals(normalizedE)) return false;
        }
        return true;
    }

    private static boolean compareRandomizedDesignOutput(List<String> actual, JsonArray expected,
                                                         JsonArray methodNames, JsonArray allArgs,
                                                         RandomizedDesignState state) {
        if (methodNames.size() != allArgs.size()) return false;
        for (int i = 0; i < actual.size(); i++) {
            String methodName = methodNames.get(i).getAsString();
            if ("getRandom".equals(methodName)) {
                if (!state.containsActualRandomValue(actual.get(i).trim())) {
                    return false;
                }
                continue;
            }

            if (!singleOutputMatches(actual.get(i), expected.get(i))) {
                return false;
            }
            state.applyDeterministicOperation(methodName, allArgs.get(i).getAsJsonArray());
        }
        return true;
    }

    private static boolean singleOutputMatches(String actual, JsonElement expected) {
        String a = actual.trim();
        if (a.equals("null") && expected.isJsonNull()) return true;
        if (expected.isJsonNull() && a.equals("null")) return true;

        if (expected.isJsonPrimitive() && expected.getAsJsonPrimitive().isNumber()) {
            try {
                double aVal = Double.parseDouble(a);
                double eVal = expected.getAsDouble();
                return Math.abs(aVal - eVal) < 1e-5;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        String normalizedA = a.replaceAll("\\s+", "").replace("\"", "").toLowerCase();
        String normalizedE = expected.toString().replaceAll("\\s+", "").replace("\"", "").toLowerCase();
        return normalizedA.equals(normalizedE);
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
}
