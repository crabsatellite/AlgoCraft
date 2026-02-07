package com.crabmods.algocraft.testengine;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/**
 * Universal input parser that converts test case input strings into Java method arguments.
 * Supports all AlgoCraft problem types including:
 * - Primitives: int, long, double, float, boolean, char
 * - Strings
 * - 1D/2D arrays: int[], String[], char[], int[][], char[][], String[][]
 * - TreeNode (level-order array notation)
 * - ListNode (with optional cycle via pos parameter)
 * - List types: List<Integer>, List<String>, List<List<Integer>>, List<List<String>>
 * - Multiple parameters of the same type
 */
public class InputParser {

    /**
     * Parse a full test input string like "nums = [1,2,3], target = 9" into method arguments.
     */
    public static Object[] parseInput(String input, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericTypes = method.getGenericParameterTypes();

        // Split input by parameter assignments
        List<String> valueParts = splitInputParts(input);

        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length && i < valueParts.size(); i++) {
            args[i] = parseValue(valueParts.get(i).trim(), paramTypes[i], genericTypes[i]);
        }
        return args;
    }

    /**
     * Split "nums = [1,2,3], target = 9" into ["[1,2,3]", "9"].
     * Handles nested brackets and quoted strings correctly.
     */
    static List<String> splitInputParts(String input) {
        List<String> parts = new ArrayList<>();
        // Match "varName = value" patterns, being careful with nested brackets
        Pattern pattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*");
        Matcher matcher = pattern.matcher(input);

        List<int[]> assignments = new ArrayList<>();
        while (matcher.find()) {
            assignments.add(new int[]{matcher.start(), matcher.end()});
        }

        for (int i = 0; i < assignments.size(); i++) {
            int valueStart = assignments.get(i)[1];
            int valueEnd;
            if (i + 1 < assignments.size()) {
                // Find the comma separator before the next assignment
                valueEnd = findValueEnd(input, valueStart, assignments.get(i + 1)[0]);
            } else {
                valueEnd = input.length();
            }
            String value = input.substring(valueStart, valueEnd).trim();
            // Remove trailing comma
            if (value.endsWith(",")) {
                value = value.substring(0, value.length() - 1).trim();
            }
            parts.add(value);
        }
        return parts;
    }

    private static int findValueEnd(String input, int valueStart, int nextAssignStart) {
        // Walk backwards from nextAssignStart to find the separating comma
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = valueStart; i < nextAssignStart; i++) {
            char c = input.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (!inString) {
                if (c == '[' || c == '(' || c == '{') depth++;
                else if (c == ']' || c == ')' || c == '}') depth--;
                else if (c == ',' && depth == 0) {
                    // This comma separates two parameters
                    return i;
                }
            }
        }
        return nextAssignStart;
    }

    /**
     * Parse a single value string into the target type.
     */
    public static Object parseValue(String val, Class<?> type, Type genericType) {
        val = val.trim();

        // Handle "null"
        if (val.equals("null") && !type.isPrimitive()) return null;

        // Primitives
        if (type == int.class || type == Integer.class) return Integer.parseInt(val);
        if (type == long.class || type == Long.class) return Long.parseLong(val.replace("L", "").replace("l", ""));
        if (type == double.class || type == Double.class) return Double.parseDouble(val);
        if (type == float.class || type == Float.class) return Float.parseFloat(val.replace("f", "").replace("F", ""));
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(val);
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
        if (type == int[].class) return parseIntArray(val);
        if (type == long[].class) return parseLongArray(val);
        if (type == double[].class) return parseDoubleArray(val);
        if (type == char[].class) return parseCharArray(val);
        if (type == String[].class) return parseStringArray(val);
        if (type == boolean[].class) return parseBooleanArray(val);

        // 2D Arrays
        if (type == int[][].class) return parse2DIntArray(val);
        if (type == char[][].class) return parse2DCharArray(val);
        if (type == String[][].class) return parse2DStringArray(val);

        // List types (requires generic type info)
        if (type == List.class && genericType instanceof ParameterizedType pt) {
            return parseList(val, pt);
        }

        // TreeNode (will be handled by the adapter if the class is loaded)
        if (type.getSimpleName().equals("TreeNode")) {
            return buildTreeNode(val, type);
        }

        // ListNode
        if (type.getSimpleName().equals("ListNode")) {
            return buildListNode(val, type);
        }

        // Node types
        if (type.getSimpleName().equals("Node")) {
            return buildNode(val, type);
        }

        // Array of ListNode (e.g., ListNode[] for mergeKLists)
        if (type.isArray() && type.getComponentType().getSimpleName().equals("ListNode")) {
            return buildListNodeArray(val, type.getComponentType());
        }

        return null;
    }

    // ─── Array parsing ──────────────────────────────────────────────

    public static int[] parseIntArray(String val) {
        val = val.trim();
        if (val.equals("[]") || val.equals("null")) return new int[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new int[0];
        String[] nums = val.split(",");
        int[] arr = new int[nums.length];
        for (int i = 0; i < nums.length; i++) arr[i] = Integer.parseInt(nums[i].trim());
        return arr;
    }

    public static long[] parseLongArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new long[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new long[0];
        String[] nums = val.split(",");
        long[] arr = new long[nums.length];
        for (int i = 0; i < nums.length; i++) arr[i] = Long.parseLong(nums[i].trim());
        return arr;
    }

    public static double[] parseDoubleArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new double[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new double[0];
        String[] nums = val.split(",");
        double[] arr = new double[nums.length];
        for (int i = 0; i < nums.length; i++) arr[i] = Double.parseDouble(nums[i].trim());
        return arr;
    }

    public static char[] parseCharArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new char[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new char[0];
        String[] parts = val.split(",");
        char[] arr = new char[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim().replace("\"", "").replace("'", "");
            arr[i] = s.isEmpty() ? '\0' : s.charAt(0);
        }
        return arr;
    }

    public static String[] parseStringArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new String[0];
        // Remove outer brackets
        if (val.startsWith("[")) val = val.substring(1);
        if (val.endsWith("]")) val = val.substring(0, val.length() - 1);
        if (val.isEmpty()) return new String[0];
        List<String> items = splitTopLevel(val, ',');
        String[] arr = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            String s = items.get(i).trim();
            if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                s = s.substring(1, s.length() - 1);
            }
            arr[i] = s;
        }
        return arr;
    }

    public static boolean[] parseBooleanArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new boolean[0];
        val = val.replaceAll("[\\[\\]]", "");
        if (val.isEmpty()) return new boolean[0];
        String[] parts = val.split(",");
        boolean[] arr = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Boolean.parseBoolean(parts[i].trim());
        return arr;
    }

    public static int[][] parse2DIntArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new int[0][];
        if (val.equals("[[]]")) return new int[][]{ new int[0] }; // single empty row
        // Remove outermost brackets
        if (val.startsWith("[")) val = val.substring(1);
        if (val.endsWith("]")) val = val.substring(0, val.length() - 1);
        List<String> rows = splitTopLevel(val, ',');
        // Handle case where we split a flat list vs nested
        if (!rows.isEmpty() && rows.get(0).trim().startsWith("[")) {
            int[][] result = new int[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                result[i] = parseIntArray(rows.get(i).trim());
            }
            return result;
        }
        // Single row
        return new int[][]{parseIntArray("[" + val + "]")};
    }

    public static char[][] parse2DCharArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new char[0][];
        if (val.startsWith("[")) val = val.substring(1);
        if (val.endsWith("]")) val = val.substring(0, val.length() - 1);
        List<String> rows = splitTopLevel(val, ',');
        if (!rows.isEmpty() && rows.get(0).trim().startsWith("[")) {
            char[][] result = new char[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                result[i] = parseCharArray(rows.get(i).trim());
            }
            return result;
        }
        return new char[][]{parseCharArray("[" + val + "]")};
    }

    public static String[][] parse2DStringArray(String val) {
        val = val.trim();
        if (val.equals("[]")) return new String[0][];
        if (val.startsWith("[")) val = val.substring(1);
        if (val.endsWith("]")) val = val.substring(0, val.length() - 1);
        List<String> rows = splitTopLevel(val, ',');
        if (!rows.isEmpty() && rows.get(0).trim().startsWith("[")) {
            String[][] result = new String[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                result[i] = parseStringArray(rows.get(i).trim());
            }
            return result;
        }
        return new String[][]{parseStringArray("[" + val + "]")};
    }

    // ─── List parsing ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Object parseList(String val, ParameterizedType pt) {
        Type elementType = pt.getActualTypeArguments()[0];

        // List<Integer>
        if (elementType == Integer.class) {
            int[] arr = parseIntArray(val);
            List<Integer> list = new ArrayList<>();
            for (int v : arr) list.add(v);
            return list;
        }

        // List<String>
        if (elementType == String.class) {
            String[] arr = parseStringArray(val);
            return new ArrayList<>(Arrays.asList(arr));
        }

        // List<List<Integer>>
        if (elementType instanceof ParameterizedType inner) {
            Type innerElement = inner.getActualTypeArguments()[0];
            if (innerElement == Integer.class) {
                return parseListOfListInteger(val);
            }
            if (innerElement == String.class) {
                return parseListOfListString(val);
            }
        }

        // Fallback: treat as List<Integer>
        int[] arr = parseIntArray(val);
        List<Integer> list = new ArrayList<>();
        for (int v : arr) list.add(v);
        return list;
    }

    public static List<List<Integer>> parseListOfListInteger(String val) {
        val = val.trim();
        List<List<Integer>> result = new ArrayList<>();
        if (val.equals("[]")) return result;
        // Remove outer brackets
        if (val.startsWith("[")) val = val.substring(1);
        if (val.endsWith("]")) val = val.substring(0, val.length() - 1);
        List<String> rows = splitTopLevel(val, ',');
        for (String row : rows) {
            row = row.trim();
            if (row.startsWith("[")) {
                int[] arr = parseIntArray(row);
                List<Integer> list = new ArrayList<>();
                for (int v : arr) list.add(v);
                result.add(list);
            }
        }
        return result;
    }

    public static List<List<String>> parseListOfListString(String val) {
        val = val.trim();
        List<List<String>> result = new ArrayList<>();
        if (val.equals("[]")) return result;
        if (val.startsWith("[")) val = val.substring(1);
        if (val.endsWith("]")) val = val.substring(0, val.length() - 1);
        List<String> rows = splitTopLevel(val, ',');
        for (String row : rows) {
            row = row.trim();
            if (row.startsWith("[")) {
                String[] arr = parseStringArray(row);
                result.add(new ArrayList<>(Arrays.asList(arr)));
            }
        }
        return result;
    }

    // ─── Tree/List/Node building ────────────────────────────────────

    /**
     * Build a TreeNode from level-order array notation: [3,1,4,null,5]
     */
    public static Object buildTreeNode(String val, Class<?> treeNodeClass) {
        val = val.trim();
        if (val.equals("[]") || val.equals("null")) return null;

        String[] elements = val.replaceAll("[\\[\\]]", "").split(",");
        if (elements.length == 0 || elements[0].trim().equals("null")) return null;

        try {
            Constructor<?> ctor = treeNodeClass.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            Field leftField = treeNodeClass.getField("left");
            Field rightField = treeNodeClass.getField("right");

            Object root = ctor.newInstance(Integer.parseInt(elements[0].trim()));
            Queue<Object> queue = new LinkedList<>();
            queue.offer(root);
            int i = 1;

            while (!queue.isEmpty() && i < elements.length) {
                Object node = queue.poll();
                // Left child
                if (i < elements.length) {
                    String s = elements[i].trim();
                    if (!s.equals("null")) {
                        Object left = ctor.newInstance(Integer.parseInt(s));
                        leftField.set(node, left);
                        queue.offer(left);
                    }
                    i++;
                }
                // Right child
                if (i < elements.length) {
                    String s = elements[i].trim();
                    if (!s.equals("null")) {
                        Object right = ctor.newInstance(Integer.parseInt(s));
                        rightField.set(node, right);
                        queue.offer(right);
                    }
                    i++;
                }
            }
            return root;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build TreeNode: " + e.getMessage(), e);
        }
    }

    /**
     * Build a ListNode from array notation: [1,2,3,4]
     */
    public static Object buildListNode(String val, Class<?> listNodeClass) {
        val = val.trim();
        if (val.equals("[]") || val.equals("null")) return null;

        int[] values = parseIntArray(val);
        if (values.length == 0) return null;

        try {
            Constructor<?> ctor = listNodeClass.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            Field nextField = listNodeClass.getField("next");

            Object head = ctor.newInstance(values[0]);
            Object current = head;
            for (int i = 1; i < values.length; i++) {
                Object next = ctor.newInstance(values[i]);
                nextField.set(current, next);
                current = next;
            }
            return head;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build ListNode: " + e.getMessage(), e);
        }
    }

    /**
     * Build a Node from the appropriate format (graph adjacency list, N-ary tree, etc.)
     */
    public static Object buildNode(String val, Class<?> nodeClass) {
        // Determine node type by its fields
        try {
            boolean hasChildren = false;
            boolean hasNeighbors = false;
            boolean hasRandom = false;
            for (Field f : nodeClass.getFields()) {
                if (f.getName().equals("children")) hasChildren = true;
                if (f.getName().equals("neighbors")) hasNeighbors = true;
                if (f.getName().equals("random")) hasRandom = true;
            }
            if (hasNeighbors) return buildGraphNode(val, nodeClass);
            if (hasChildren) return buildNaryTreeNode(val, nodeClass);
            if (hasRandom) return buildRandomPointerNode(val, nodeClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Node: " + e.getMessage(), e);
        }
        return null;
    }

    private static Object buildGraphNode(String val, Class<?> nodeClass) throws Exception {
        // Input: [[2,4],[1,3],[2,4],[1,3]] (adjacency list, 1-indexed)
        val = val.trim();
        if (val.equals("[]")) return null;

        int[][] adjList = parse2DIntArray(val);
        if (adjList.length == 0) return null;

        Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        Field neighborsField = nodeClass.getField("neighbors");

        Object[] nodes = new Object[adjList.length];
        for (int i = 0; i < adjList.length; i++) {
            nodes[i] = ctor.newInstance(i + 1); // 1-indexed
        }
        for (int i = 0; i < adjList.length; i++) {
            @SuppressWarnings("unchecked")
            List<Object> neighbors = (List<Object>) neighborsField.get(nodes[i]);
            for (int neighbor : adjList[i]) {
                neighbors.add(nodes[neighbor - 1]); // 1-indexed
            }
        }
        return nodes[0];
    }

    private static Object buildNaryTreeNode(String val, Class<?> nodeClass) throws Exception {
        // Input: [1,null,3,2,4,null,5,6] (level-order with null separating children groups)
        val = val.trim();
        if (val.equals("[]") || val.equals("null")) return null;

        String inner = val.replaceAll("[\\[\\]]", "");
        String[] elements = inner.split(",");
        if (elements.length == 0 || elements[0].trim().equals("null")) return null;

        Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        Field childrenField = nodeClass.getField("children");

        Object root = ctor.newInstance(Integer.parseInt(elements[0].trim()));
        Queue<Object> queue = new LinkedList<>();
        queue.offer(root);
        int i = 2; // Skip root and first null separator

        while (!queue.isEmpty() && i < elements.length) {
            Object parent = queue.poll();
            @SuppressWarnings("unchecked")
            List<Object> children = (List<Object>) childrenField.get(parent);

            while (i < elements.length) {
                String s = elements[i].trim();
                i++;
                if (s.equals("null")) break; // Null separates children groups
                Object child = ctor.newInstance(Integer.parseInt(s));
                children.add(child);
                queue.offer(child);
            }
        }
        return root;
    }

    private static Object buildRandomPointerNode(String val, Class<?> nodeClass) throws Exception {
        // Input: [[7,null],[13,0],[11,4],[10,2],[1,0]]
        val = val.trim();
        if (val.equals("[]")) return null;

        // Parse as 2D array of strings to handle "null"
        String inner = val.substring(1, val.length() - 1); // Remove outer []
        List<String> pairs = splitTopLevel(inner, ',');

        Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        Field nextField = nodeClass.getField("next");
        Field randomField = nodeClass.getField("random");

        List<Object> nodes = new ArrayList<>();
        List<String> randomIndices = new ArrayList<>();

        for (String pair : pairs) {
            pair = pair.trim();
            if (!pair.startsWith("[")) continue;
            String[] parts = pair.replaceAll("[\\[\\]]", "").split(",");
            int nodeVal = Integer.parseInt(parts[0].trim());
            Object node = ctor.newInstance(nodeVal);
            nodes.add(node);
            randomIndices.add(parts.length > 1 ? parts[1].trim() : "null");
        }

        // Link next pointers
        for (int j = 0; j < nodes.size() - 1; j++) {
            nextField.set(nodes.get(j), nodes.get(j + 1));
        }
        // Link random pointers
        for (int j = 0; j < nodes.size(); j++) {
            String idx = randomIndices.get(j);
            if (!idx.equals("null")) {
                int ri = Integer.parseInt(idx);
                randomField.set(nodes.get(j), nodes.get(ri));
            }
        }
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    // ─── ListNode[] building ──────────────────────────────────────────

    /**
     * Build an array of ListNode from nested array notation: [[1,4,5],[1,3,4],[2,6]]
     */
    public static Object buildListNodeArray(String val, Class<?> listNodeClass) {
        val = val.trim();
        if (val.equals("[]")) return java.lang.reflect.Array.newInstance(listNodeClass, 0);

        // Remove outer brackets
        String inner = val.substring(1, val.length() - 1);
        List<String> parts = splitTopLevel(inner, ',');

        // Filter for actual sub-arrays (skip non-bracket parts)
        List<String> arrays = new ArrayList<>();
        for (String p : parts) {
            p = p.trim();
            if (p.startsWith("[")) arrays.add(p);
        }

        Object arr = java.lang.reflect.Array.newInstance(listNodeClass, arrays.size());
        for (int i = 0; i < arrays.size(); i++) {
            java.lang.reflect.Array.set(arr, i, buildListNode(arrays.get(i), listNodeClass));
        }
        return arr;
    }

    // ─── Named parameter parsing ────────────────────────────────────

    /**
     * Parse all named parameters from input: "a = 1, b = [2,3]" → {a: "1", b: "[2,3]"}
     */
    public static Map<String, String> parseAllNamedParams(String input) {
        Map<String, String> result = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*");
        Matcher matcher = pattern.matcher(input);

        List<String> names = new ArrayList<>();
        List<int[]> positions = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
            positions.add(new int[]{matcher.start(), matcher.end()});
        }

        for (int i = 0; i < names.size(); i++) {
            int valueStart = positions.get(i)[1];
            int valueEnd;
            if (i + 1 < positions.size()) {
                valueEnd = findValueEnd(input, valueStart, positions.get(i + 1)[0]);
            } else {
                valueEnd = input.length();
            }
            String value = input.substring(valueStart, valueEnd).trim();
            if (value.endsWith(",")) value = value.substring(0, value.length() - 1).trim();
            result.put(names.get(i), value);
        }
        return result;
    }

    // ─── Utility: split at top-level delimiter ──────────────────────

    /**
     * Split a string by delimiter, respecting brackets and quotes.
     */
    public static List<String> splitTopLevel(String s, char delimiter) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                current.append(c);
                continue;
            }
            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }
            if (!inString) {
                if (c == '[' || c == '(' || c == '{') depth++;
                else if (c == ']' || c == ')' || c == '}') depth--;
                else if (c == delimiter && depth == 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(c);
        }
        if (!current.isEmpty()) result.add(current.toString());
        return result;
    }
}
