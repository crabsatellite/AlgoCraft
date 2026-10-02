package com.crabmods.algocraft.testengine.adapters;

import com.crabmods.algocraft.testengine.*;
import com.crabmods.algocraft.logic.SolutionMethodSelector;
import com.google.gson.*;

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
        return run(solutionClass, input, expected, "");
    }

    public static TestResult run(Class<?> solutionClass, String input, String expected, String problemId) {
        try {
            Method method = findSolutionMethod(solutionClass, input);
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

            boolean passed;
            if ("compress".equals(method.getName())
                    && result instanceof Number
                    && args.length > 0
                    && args[0] instanceof char[]) {
                passed = compareCompressedCharPrefix(((Number) result).intValue(), (char[]) args[0], expected);
            } else if (isProblem(problemId, "242") && "allCellsDistOrder".equals(method.getName()) && result instanceof int[][]) {
                passed = compareAllCellsDistOrder((int[][]) result, input);
            } else if (isProblem(problemId, "455") && "missingRolls".equals(method.getName())) {
                passed = compareMissingRollsResult(result, input);
            } else if (isProblem(problemId, "497") && "constructArray".equals(method.getName()) && result instanceof int[]) {
                passed = compareBeautifulArrangement((int[]) result, input);
            } else {
                boolean semanticRequired = requiresSemanticAlternative(problemId, method.getName());
                boolean semanticPassed = compareSemanticAlternative(problemId, method.getName(), result, args,
                        input, expected);
                passed = semanticRequired
                        ? semanticPassed
                        : semanticPassed || OutputComparator.compare(actualStr, expected, compareType);
            }
            if (passed) {
                return TestResult.pass();
            } else {
                return TestResult.fail(input, expected, actualStr);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            return TestResult.error(describeException(cause != null ? cause : e));
        } catch (Exception e) {
            return TestResult.error(describeException(e));
        }
    }

    private static String describeException(Throwable throwable) {
        java.io.StringWriter buffer = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(buffer));
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage()
                + System.lineSeparator() + buffer;
    }

    private static boolean compareAllCellsDistOrder(int[][] actual, String input) {
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int rows = Integer.parseInt(namedParams.get("rows").trim());
            int cols = Integer.parseInt(namedParams.get("cols").trim());
            int rCenter = Integer.parseInt(namedParams.get("rCenter").trim());
            int cCenter = Integer.parseInt(namedParams.get("cCenter").trim());

            if (actual.length != rows * cols) return false;
            boolean[][] seen = new boolean[rows][cols];
            int previousDistance = -1;
            int count = 0;

            for (int[] cell : actual) {
                if (cell == null || cell.length != 2) return false;
                int r = cell[0];
                int c = cell[1];
                if (r < 0 || r >= rows || c < 0 || c >= cols || seen[r][c]) return false;

                int distance = Math.abs(r - rCenter) + Math.abs(c - cCenter);
                if (distance < previousDistance) return false;
                previousDistance = distance;
                seen[r][c] = true;
                count++;
            }

            return count == rows * cols;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean compareBeautifulArrangement(int[] actual, String input) {
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int n = Integer.parseInt(namedParams.get("n").trim());
            int k = Integer.parseInt(namedParams.get("k").trim());

            if (actual.length != n) return false;
            boolean[] seen = new boolean[n + 1];
            for (int value : actual) {
                if (value < 1 || value > n || seen[value]) return false;
                seen[value] = true;
            }

            Set<Integer> differences = new HashSet<>();
            for (int i = 1; i < actual.length; i++) {
                differences.add(Math.abs(actual[i] - actual[i - 1]));
            }
            return differences.size() == k;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean compareCompressedCharPrefix(int actualLength, char[] actualChars, String expected) {
        String trimmed = expected.trim();
        if (!trimmed.contains("[")) {
            return OutputComparator.compare(String.valueOf(actualLength), trimmed, int.class);
        }

        java.util.regex.Matcher lengthMatcher = java.util.regex.Pattern
                .compile("(?:length\\s*=\\s*)?(\\d+)")
                .matcher(trimmed);
        if (!lengthMatcher.find()) return false;

        int expectedLength = Integer.parseInt(lengthMatcher.group(1));
        int prefixStart = trimmed.indexOf('[');
        int prefixEnd = trimmed.lastIndexOf(']');
        if (prefixStart < 0 || prefixEnd < prefixStart) return false;

        char[] expectedPrefix = InputParser.parseCharArray(trimmed.substring(prefixStart, prefixEnd + 1));
        if (actualLength != expectedLength || expectedPrefix.length != expectedLength) return false;
        if (actualChars.length < expectedPrefix.length) return false;
        for (int i = 0; i < expectedPrefix.length; i++) {
            if (actualChars[i] != expectedPrefix[i]) return false;
        }
        return true;
    }

    /**
     * Find the solution method. Prefers public methods, falls back to any non-private non-Object method.
     */
    public static Method findSolutionMethod(Class<?> clazz) {
        return findSolutionMethod(clazz, "");
    }

    public static Method findSolutionMethod(Class<?> clazz, String input) {
        return SolutionMethodSelector.select(clazz, input, method -> canParseMethodInput(input, method));
    }

    private static boolean canParseMethodInput(String input, Method method) {
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input == null ? "" : input);
            Object[] args;
            if (namedParams.containsKey("intersectVal") && namedParams.containsKey("skipA")) {
                args = buildIntersectingListArgs(namedParams, method);
            } else {
                args = InputParser.parseInput(input == null ? "" : input, method);
            }
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
        } catch (Exception exception) {
            return false;
        }
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

    private static boolean requiresSemanticAlternative(String problemId, String methodName) {
        String id = normalizeProblemId(problemId);
        return switch (id) {
            case "26" -> "removeDuplicates".equals(methodName);
            case "27" -> "removeElement".equals(methodName);
            default -> false;
        };
    }

    private static boolean compareSemanticAlternative(String problemId, String methodName, Object result,
                                                      Object[] args, String input, String expected) {
        String id = normalizeProblemId(problemId);
        return switch (id) {
            case "1" -> "twoSum".equals(methodName) && compareZeroBasedTwoSum(result, input);
            case "4" -> "groupAnagrams".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, true);
            case "5" -> "topKFrequent".equals(methodName) && compareTopKFrequent(result, input);
            case "15" -> "intersect".equals(methodName) && compareIntArrayMultiset(result, expected);
            case "16" -> "threeSum".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, true);
            case "17" -> "fourSum".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, true);
            case "22" -> "twoSum".equals(methodName) && compareOneBasedTwoSum(result, input);
            case "26" -> "removeDuplicates".equals(methodName)
                    && compareRemoveDuplicatesPrefix(result, args, input, expected);
            case "27" -> "removeElement".equals(methodName)
                    && compareRemoveElementPrefix(result, args, input, expected);
            case "44" -> "generateParenthesis".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "103" -> "findWords".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "108" -> "kClosest".equals(methodName) && compareKClosest(result, input);
            case "115" -> "kSmallestPairs".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, false);
            case "116" -> "subsets".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, true);
            case "117" -> "combinationSum".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, true);
            case "118" -> "permute".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, false);
            case "119" -> "subsetsWithDup".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, true);
            case "120" -> "combinationSum2".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, true);
            case "122" -> "partition".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, false);
            case "123" -> "letterCombinations".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "124" -> "solveNQueens".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, false);
            case "126" -> "restoreIpAddresses".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "133" -> "pacificAtlantic".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, false);
            case "136" -> "findOrder".equals(methodName) && compareCourseScheduleOrder(result, input, expected);
            case "141" -> "findLadders".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, false);
            case "146" -> "alienOrder".equals(methodName) && compareAlienDictionaryOrder(result, input, expected);
            case "221" -> "intersection".equals(methodName) && compareIntArraySet(result, expected);
            case "228" -> "findDuplicates".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "236" -> "sortArrayByParity".equals(methodName) && compareSortArrayByParity(result, input);
            case "305" -> "findSubstring".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "341" -> "removeInvalidParentheses".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "348" -> "findAllConcatenatedWordsInADict".equals(methodName) && compareJsonFlatMultiset(result, expected);
            case "363" -> "palindromePairs".equals(methodName) && compareJsonNestedOuterMultiset(result, expected, false);
            case "376" -> "crackSafe".equals(methodName) && compareCrackingSafe(result, input);
            case "399" -> "smallestSufficientTeam".equals(methodName) && compareSmallestSufficientTeam(result, input, expected);
            case "432" -> "findOriginalArray".equals(methodName) && compareIntArrayMultiset(result, expected);
            case "487" -> "findDuplicateSubtrees".equals(methodName) && compareFormattedJsonNestedOuterMultiset(result, expected, false);
            default -> false;
        };
    }

    private static boolean compareRemoveDuplicatesPrefix(Object result, Object[] args, String input, String expected) {
        if (!(result instanceof Number number)
                || args == null
                || args.length == 0
                || !(args[0] instanceof int[] actualNums)) {
            return false;
        }
        try {
            int actualLength = number.intValue();
            int expectedLength = parseLeadingInt(expected);
            int[] original = InputParser.parseIntArray(
                    InputParser.parseAllNamedParams(input).getOrDefault("nums", "[]"));
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
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean compareRemoveElementPrefix(Object result, Object[] args, String input, String expected) {
        if (!(result instanceof Number number)
                || args == null
                || args.length == 0
                || !(args[0] instanceof int[] actualNums)) {
            return false;
        }
        try {
            Map<String, String> named = InputParser.parseAllNamedParams(input);
            int actualLength = number.intValue();
            int expectedLength = parseLeadingInt(expected);
            int[] original = InputParser.parseIntArray(named.getOrDefault("nums", "[]"));
            int removedValue = Integer.parseInt(named.get("val").trim());
            int[] expectedPrefix = valuesExcept(original, removedValue);
            return expectedLength == expectedPrefix.length
                    && actualLength == expectedLength
                    && actualLength >= 0
                    && actualLength <= actualNums.length
                    && prefixMultisetMatches(actualNums, actualLength, expectedPrefix);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static int parseLeadingInt(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+")
                .matcher(text == null ? "" : text);
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

    private static boolean isProblem(String problemId, String expectedId) {
        return expectedId.equals(normalizeProblemId(problemId));
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

    private static boolean compareZeroBasedTwoSum(Object result, String input) {
        if (!(result instanceof int[] actual) || actual.length != 2 || actual[0] == actual[1]) {
            return false;
        }
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            if (!namedParams.containsKey("nums") || !namedParams.containsKey("target")) {
                return false;
            }
            int[] nums = InputParser.parseIntArray(namedParams.get("nums"));
            int target = Integer.parseInt(namedParams.get("target").trim());
            return actual[0] >= 0 && actual[0] < nums.length
                    && actual[1] >= 0 && actual[1] < nums.length
                    && nums[actual[0]] + nums[actual[1]] == target;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareOneBasedTwoSum(Object result, String input) {
        if (!(result instanceof int[] actual) || actual.length != 2 || actual[0] == actual[1]) {
            return false;
        }
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            if (!namedParams.containsKey("target")) {
                return false;
            }
            int[] numbers = InputParser.parseIntArray(
                    namedParams.getOrDefault("numbers", namedParams.getOrDefault("nums", "[]")));
            int target = Integer.parseInt(namedParams.get("target").trim());
            int left = actual[0] - 1;
            int right = actual[1] - 1;
            return left >= 0 && left < numbers.length
                    && right >= 0 && right < numbers.length
                    && actual[0] < actual[1]
                    && numbers[left] + numbers[right] == target;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareTopKFrequent(Object result, String input) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int[] nums = InputParser.parseIntArray(namedParams.getOrDefault("nums", "[]"));
            int k = Integer.parseInt(namedParams.get("k").trim());
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
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareIntArrayMultiset(Object result, String expected) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] actualCopy = Arrays.copyOf(actual, actual.length);
            int[] expectedValues = InputParser.parseIntArray(expected);
            Arrays.sort(actualCopy);
            Arrays.sort(expectedValues);
            return Arrays.equals(actualCopy, expectedValues);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareIntArraySet(Object result, String expected) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] expectedValues = InputParser.parseIntArray(expected);
            Set<Integer> actualSet = new HashSet<>();
            for (int value : actual) {
                if (!actualSet.add(value)) {
                    return false;
                }
            }
            Set<Integer> expectedSet = new HashSet<>();
            for (int value : expectedValues) {
                expectedSet.add(value);
            }
            return actualSet.equals(expectedSet);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareKClosest(Object result, String input) {
        if (!(result instanceof int[][] actual)) {
            return false;
        }
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int[][] points = InputParser.parse2DIntArray(namedParams.getOrDefault("points", "[]"));
            int k = Integer.parseInt(namedParams.get("k").trim());
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
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static long squaredDistance(int[] point) {
        return (long) point[0] * point[0] + (long) point[1] * point[1];
    }

    private static boolean compareCourseScheduleOrder(Object result, String input, String expected) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] expectedValues = InputParser.parseIntArray(expected);
            if (expectedValues.length == 0) {
                return actual.length == 0;
            }

            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int numCourses = Integer.parseInt(namedParams.get("numCourses").trim());
            int[][] prerequisites = InputParser.parse2DIntArray(namedParams.getOrDefault("prerequisites", "[]"));
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
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareAlienDictionaryOrder(Object result, String input, String expected) {
        if (!(result instanceof String actual)) {
            return false;
        }
        if (expectedStringValue(expected).isEmpty()) {
            return actual.isEmpty();
        }
        if (actual.isEmpty()) {
            return false;
        }

        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            String[] words = InputParser.parseStringArray(namedParams.getOrDefault("words", "[]"));
            Set<Character> present = new HashSet<>();
            for (String word : words) {
                for (char ch : word.toCharArray()) {
                    present.add(ch);
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
                int limit = Math.min(first.length(), second.length());
                int j = 0;
                while (j < limit && first.charAt(j) == second.charAt(j)) {
                    j++;
                }
                if (j == limit) {
                    if (first.length() > second.length()) {
                        return false;
                    }
                    continue;
                }
                char before = first.charAt(j);
                char after = second.charAt(j);
                if (position.get(before) > position.get(after)) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String expectedStringValue(String expected) {
        String value = expected == null ? "" : expected.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean compareSortArrayByParity(Object result, String input) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int[] nums = InputParser.parseIntArray(namedParams.getOrDefault("nums", "[]"));
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
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareSmallestSufficientTeam(Object result, String input, String expected) {
        if (!(result instanceof int[] actual)) {
            return false;
        }
        try {
            int[] expectedValues = InputParser.parseIntArray(expected);
            if (actual.length != expectedValues.length) {
                return false;
            }

            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            String[] reqSkills = InputParser.parseStringArray(namedParams.getOrDefault("req_skills", "[]"));
            String[][] people = InputParser.parse2DStringArray(namedParams.getOrDefault("people", "[]"));
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
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareCrackingSafe(Object result, String input) {
        if (!(result instanceof String actual)) {
            return false;
        }
        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int n = Integer.parseInt(namedParams.get("n").trim());
            int k = Integer.parseInt(namedParams.get("k").trim());
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
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareJsonFlatMultiset(Object result, String expected) {
        try {
            JsonArray actual = parseJsonArray(result);
            JsonArray expectedArray = parseExpectedJsonArray(expected);
            return jsonMultiset(actual, false).equals(jsonMultiset(expectedArray, false));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareJsonNestedOuterMultiset(Object result, String expected, boolean sortInnerArrays) {
        try {
            JsonArray actual = parseJsonArray(result);
            JsonArray expectedArray = parseExpectedJsonArray(expected);
            return jsonMultiset(actual, sortInnerArrays).equals(jsonMultiset(expectedArray, sortInnerArrays));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean compareFormattedJsonNestedOuterMultiset(Object result, String expected,
                                                                   boolean sortInnerArrays) {
        try {
            JsonArray actual = parseExpectedJsonArray(OutputComparator.format(result));
            JsonArray expectedArray = parseExpectedJsonArray(expected);
            return jsonMultiset(actual, sortInnerArrays).equals(jsonMultiset(expectedArray, sortInnerArrays));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static JsonArray parseJsonArray(Object result) {
        JsonElement element = new Gson().toJsonTree(result);
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Result is not a JSON array");
        }
        return element.getAsJsonArray();
    }

    private static JsonArray parseExpectedJsonArray(String expected) {
        JsonElement element = JsonParser.parseString(expected);
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Expected output is not a JSON array");
        }
        return element.getAsJsonArray();
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

    private static boolean isObjectMethod(String name) {
        return switch (name) {
            case "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait" -> true;
            default -> false;
        };
    }

    private static boolean compareMissingRollsResult(Object result, String input) {
        if (!(result instanceof int[] actual)) {
            return false;
        }

        try {
            Map<String, String> namedParams = InputParser.parseAllNamedParams(input);
            int[] rolls = InputParser.parseIntArray(namedParams.getOrDefault("rolls", "[]"));
            int mean = Integer.parseInt(namedParams.get("mean").trim());
            int n = Integer.parseInt(namedParams.get("n").trim());

            int existingSum = 0;
            for (int roll : rolls) {
                existingSum += roll;
            }

            int missingSum = mean * (rolls.length + n) - existingSum;
            if (missingSum < n || missingSum > 6 * n) {
                return actual.length == 0;
            }
            if (actual.length != n) {
                return false;
            }

            int actualSum = 0;
            for (int roll : actual) {
                if (roll < 1 || roll > 6) {
                    return false;
                }
                actualSum += roll;
            }
            return actualSum == missingSum;
        } catch (Exception ignored) {
            return false;
        }
    }
}
