package com.crabmods.algocraft.testengine;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

/**
 * Compares actual output against expected output with type-aware comparison.
 * Handles:
 * - Exact string matching (after normalization)
 * - Floating point epsilon comparison
 * - TreeNode level-order comparison (with trailing null stripping)
 * - ListNode comparison
 * - Unordered array/list comparison
 * - Void method output (modified input argument)
 */
public class OutputComparator {

    private static final double EPSILON = 1e-5;

    /**
     * Format a Java object to its string representation matching test case output format.
     */
    public static String format(Object result) {
        if (result == null) return "null";

        if (result instanceof int[]) return Arrays.toString((int[]) result);
        if (result instanceof long[]) return Arrays.toString((long[]) result);
        if (result instanceof double[]) return Arrays.toString((double[]) result);
        if (result instanceof boolean[]) return Arrays.toString((boolean[]) result);
        if (result instanceof char[]) {
            char[] chars = (char[]) result;
            StringJoiner sj = new StringJoiner(",", "[", "]");
            for (char c : chars) sj.add("\"" + c + "\"");
            return sj.toString();
        }
        if (result instanceof String[]) {
            String[] arr = (String[]) result;
            StringJoiner sj = new StringJoiner(",", "[", "]");
            for (String s : arr) sj.add("\"" + s + "\"");
            return sj.toString();
        }
        if (result instanceof int[][]) {
            int[][] arr = (int[][]) result;
            StringJoiner sj = new StringJoiner(",", "[", "]");
            for (int[] row : arr) sj.add(Arrays.toString(row));
            return sj.toString();
        }
        if (result instanceof char[][]) {
            char[][] arr = (char[][]) result;
            StringJoiner sj = new StringJoiner(",", "[", "]");
            for (char[] row : arr) {
                StringJoiner rsj = new StringJoiner(",", "[", "]");
                for (char c : row) rsj.add("\"" + c + "\"");
                sj.add(rsj.toString());
            }
            return sj.toString();
        }
        if (result instanceof List<?> list) {
            return formatList(list);
        }

        // TreeNode/ListNode/Node: format appropriately
        String className = result.getClass().getSimpleName();
        if (className.equals("TreeNode")) return formatTreeNode(result);
        if (className.equals("ListNode")) return formatListNode(result);
        if (className.equals("Node")) return formatNode(result);

        return result.toString();
    }

    private static String formatList(List<?> list) {
        if (list.isEmpty()) return "[]";
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (Object item : list) {
            if (item instanceof String) {
                sj.add("\"" + item + "\"");
            } else if (item instanceof List) {
                sj.add(formatList((List<?>) item));
            } else {
                sj.add(format(item));
            }
        }
        return sj.toString();
    }

    private static String formatTreeNode(Object node) {
        if (node == null) return "[]";
        try {
            Field valField = node.getClass().getField("val");
            Field leftField = node.getClass().getField("left");
            Field rightField = node.getClass().getField("right");

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
            // Strip trailing nulls
            while (!result.isEmpty() && result.get(result.size() - 1).equals("null")) {
                result.remove(result.size() - 1);
            }
            return "[" + String.join(",", result) + "]";
        } catch (Exception e) {
            return node.toString();
        }
    }

    private static String formatListNode(Object node) {
        if (node == null) return "[]";
        try {
            Field valField = node.getClass().getField("val");
            Field nextField = node.getClass().getField("next");
            List<String> values = new ArrayList<>();
            Set<Object> visited = new HashSet<>();
            Object current = node;
            while (current != null && !visited.contains(current)) {
                visited.add(current);
                values.add(String.valueOf(valField.get(current)));
                current = nextField.get(current);
            }
            return "[" + String.join(",", values) + "]";
        } catch (Exception e) {
            return node.toString();
        }
    }

    private static String formatNode(Object node) {
        if (node == null) return "null";
        try {
            // Detect which type of Node
            boolean hasRandom = false, hasNeighbors = false, hasChildren = false;
            for (Field f : node.getClass().getFields()) {
                if (f.getName().equals("random")) hasRandom = true;
                if (f.getName().equals("neighbors")) hasNeighbors = true;
                if (f.getName().equals("children")) hasChildren = true;
            }

            if (hasRandom) {
                // Random pointer node: [[val,randomIdx],...]
                Field valField = node.getClass().getField("val");
                Field nextField = node.getClass().getField("next");
                Field randomField = node.getClass().getField("random");
                // Collect all nodes first
                List<Object> nodes = new ArrayList<>();
                Map<Object, Integer> indexMap = new HashMap<>();
                Object cur = node;
                while (cur != null) {
                    indexMap.put(cur, nodes.size());
                    nodes.add(cur);
                    cur = nextField.get(cur);
                }
                StringJoiner sj = new StringJoiner(",", "[", "]");
                for (Object n : nodes) {
                    int val = (int) valField.get(n);
                    Object rand = randomField.get(n);
                    String randStr = rand == null ? "null" : String.valueOf(indexMap.get(rand));
                    sj.add("[" + val + "," + randStr + "]");
                }
                return sj.toString();
            }

            if (hasNeighbors) {
                // Graph node: [[neighbor_vals],...]
                Field valField = node.getClass().getField("val");
                Field neighborsField = node.getClass().getField("neighbors");
                // BFS to collect all nodes
                Map<Object, Integer> indexMap = new LinkedHashMap<>();
                Queue<Object> queue = new LinkedList<>();
                queue.offer(node);
                indexMap.put(node, 0);
                while (!queue.isEmpty()) {
                    Object cur = queue.poll();
                    @SuppressWarnings("unchecked")
                    List<Object> neighbors = (List<Object>) neighborsField.get(cur);
                    for (Object nb : neighbors) {
                        if (!indexMap.containsKey(nb)) {
                            indexMap.put(nb, indexMap.size());
                            queue.offer(nb);
                        }
                    }
                }
                // Sort by val (1-indexed)
                StringJoiner sj = new StringJoiner(",", "[", "]");
                List<Object> ordered = new ArrayList<>(indexMap.keySet());
                ordered.sort((a, b) -> {
                    try { return Integer.compare((int) valField.get(a), (int) valField.get(b)); }
                    catch (Exception e2) { return 0; }
                });
                for (Object n : ordered) {
                    @SuppressWarnings("unchecked")
                    List<Object> neighbors = (List<Object>) neighborsField.get(n);
                    StringJoiner nsj = new StringJoiner(",", "[", "]");
                    for (Object nb : neighbors) {
                        nsj.add(String.valueOf((int) valField.get(nb)));
                    }
                    sj.add(nsj.toString());
                }
                return sj.toString();
            }

            return node.toString();
        } catch (Exception e) {
            return node.toString();
        }
    }

    /**
     * Compare actual result against expected output, with type-aware logic.
     *
     * @param actual     the formatted actual result string
     * @param expected   the expected output string
     * @param returnType the method return type (for context-aware comparison)
     * @return true if they match
     */
    public static boolean compare(String actual, String expected, Class<?> returnType) {
        String aNorm = normalize(actual);
        String eNorm = normalize(expected);

        // Exact match
        if (aNorm.equals(eNorm)) return true;

        // Float/double epsilon comparison
        if (returnType == double.class || returnType == float.class
                || returnType == Double.class || returnType == Float.class) {
            return compareDouble(actual, expected);
        }
        if (returnType == double[].class) {
            return compareDoubleArrays(actual, expected);
        }

        // TreeNode comparison
        if (returnType != null && returnType.getSimpleName().equals("TreeNode")) {
            return compareTreeNode(aNorm, eNorm);
        }

        // ListNode comparison
        if (returnType != null && returnType.getSimpleName().equals("ListNode")) {
            return compareListNode(aNorm, eNorm);
        }

        // Node comparison (with null/[] equivalence)
        if (returnType != null && returnType.getSimpleName().equals("Node")) {
            if ((aNorm.equals("null") || aNorm.equals("[]")) &&
                (eNorm.equals("null") || eNorm.equals("[]"))) {
                return true;
            }
            return aNorm.equals(eNorm);
        }

        // null/[] equivalence (for void, Node, ListNode, and any nullable return)
        if ((aNorm.equals("null") && eNorm.equals("[]")) || (aNorm.equals("[]") && eNorm.equals("null"))) {
            return true;
        }

        // Unordered array comparison (for problems where order doesn't matter)
        if (returnType == int[].class) {
            if (compareUnorderedIntArray(actual, expected)) return true;
        }
        if (returnType == int[][].class) {
            if (compareUnorderedNested(actual, expected)) return true;
        }

        // List comparison (may be unordered)
        if (returnType != null && List.class.isAssignableFrom(returnType)) {
            if (compareUnorderedNested(actual, expected)) return true;
        }

        // Fallback: try double array comparison (for List<Double> etc.)
        if (expected.contains(".") && actual.contains(".")) {
            if (compareDoubleArrays(actual, expected)) return true;
        }

        // Fallback: try single double comparison
        if (expected.matches("-?\\d+\\.\\d+") && actual.matches("-?\\d+\\.\\d+")) {
            if (compareDouble(actual, expected)) return true;
        }

        return false;
    }

    // ─── Normalization ──────────────────────────────────────────────

    static String normalize(String s) {
        if (s == null) return "";
        s = s.trim();
        // Strip surrounding quotes
        if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        return s.replaceAll("\\s+", "").replace("\"", "").toLowerCase();
    }

    // ─── Numeric comparison ─────────────────────────────────────────

    private static boolean compareDouble(String actual, String expected) {
        try {
            double a = Double.parseDouble(actual.trim());
            double e = Double.parseDouble(expected.trim());
            return Math.abs(a - e) < EPSILON;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean compareDoubleArrays(String actual, String expected) {
        try {
            double[] a = parseDoubles(actual);
            double[] e = parseDoubles(expected);
            if (a.length != e.length) return false;
            for (int i = 0; i < a.length; i++) {
                if (Math.abs(a[i] - e[i]) >= EPSILON) return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static double[] parseDoubles(String s) {
        s = s.trim().replaceAll("[\\[\\]]", "");
        if (s.isEmpty()) return new double[0];
        String[] parts = s.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Double.parseDouble(parts[i].trim());
        return result;
    }

    // ─── TreeNode/ListNode comparison ───────────────────────────────

    private static boolean compareTreeNode(String aNorm, String eNorm) {
        // null/[] equivalence
        if ((aNorm.equals("null") || aNorm.equals("[]")) && (eNorm.equals("null") || eNorm.equals("[]")))
            return true;
        // Trailing null stripping
        String a = aNorm.replaceAll("(,null)+]$", "]");
        String e = eNorm.replaceAll("(,null)+]$", "]");
        if (a.equals(e)) return true;
        // Val-only comparison (for LCA-type problems returning a node)
        if (eNorm.matches("-?\\d+")) {
            String aVal = aNorm.replaceAll("[\\[\\]]", "").split(",")[0];
            return aVal.equals(eNorm);
        }
        return false;
    }

    private static boolean compareListNode(String aNorm, String eNorm) {
        if ((aNorm.equals("null") || aNorm.equals("[]")) && (eNorm.equals("null") || eNorm.equals("[]")))
            return true;
        // Handle "Intersected at 'X'" format (P77-style)
        if (eNorm.startsWith("intersectedat")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(eNorm);
            if (m.find()) {
                String expectedVal = m.group();
                String firstVal = aNorm.replaceAll("[\\[\\]]", "").split(",")[0].trim();
                return firstVal.equals(expectedVal);
            }
        }
        if (eNorm.equals("nointersection")) {
            return aNorm.equals("null") || aNorm.equals("[]");
        }
        return aNorm.equals(eNorm);
    }

    // ─── Unordered comparison ───────────────────────────────────────

    private static boolean compareUnorderedIntArray(String actual, String expected) {
        try {
            int[] a = InputParser.parseIntArray(actual);
            int[] e = InputParser.parseIntArray(expected);
            Arrays.sort(a);
            Arrays.sort(e);
            return Arrays.equals(a, e);
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean compareUnorderedNested(String actual, String expected) {
        String aNorm = actual.replaceAll("\\s+", "");
        String eNorm = expected.replaceAll("\\s+", "");

        // Flat list unordered comparison
        if (aNorm.startsWith("[") && !aNorm.startsWith("[[")
                && eNorm.startsWith("[") && !eNorm.startsWith("[[")) {
            Set<String> aSet = parseAsSet(aNorm.substring(1, aNorm.length() - 1));
            Set<String> eSet = parseAsSet(eNorm.substring(1, eNorm.length() - 1));
            if (aSet.equals(eSet)) return true;
        }

        // Nested list unordered comparison
        Set<Set<String>> aSets = parseAsSetOfSets(actual);
        Set<Set<String>> eSets = parseAsSetOfSets(expected);
        return aSets.equals(eSets);
    }

    private static Set<String> parseAsSet(String inner) {
        if (inner.isEmpty()) return new HashSet<>();
        return Arrays.stream(inner.split(","))
                .map(s -> s.trim().replace("\"", "").toLowerCase())
                .collect(Collectors.toSet());
    }

    private static Set<Set<String>> parseAsSetOfSets(String s) {
        s = s.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        Set<Set<String>> result = new HashSet<>();
        List<String> items = InputParser.splitTopLevel(s, ',');
        for (String item : items) {
            item = item.trim();
            if (item.startsWith("[") && item.endsWith("]")) {
                result.add(parseAsSet(item.substring(1, item.length() - 1)));
            } else {
                result.add(Set.of(item.replace("\"", "").toLowerCase()));
            }
        }
        return result;
    }
}
