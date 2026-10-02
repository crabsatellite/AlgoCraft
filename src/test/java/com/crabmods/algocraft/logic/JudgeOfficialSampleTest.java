package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeOfficialSampleTest {
    private static final Gson GSON = new Gson();
    private static final Path QUESTION_BANK = Paths.get(System.getProperty("user.dir"), "question_bank", "official");
    private static final String FORCE_ECJ_COMPILER_PROPERTY = "algocraft.codeExecutor.forceEcjCompiler";

    static Stream<Arguments> representativeOfficialProblems() {
        return Stream.of(
                Arguments.of("1", "first problem without prompt diagram"),
                Arguments.of("7", "matrix problem with prompt diagram"),
                Arguments.of("73", "serialized design-class problem"),
                Arguments.of("95", "codec round-trip problem"),
                Arguments.of("496", "tree width BFS regression near the end of the bank"),
                Arguments.of("500", "last official problem with tree helpers")
        );
    }

    @ParameterizedTest(name = "p{0} judge accepts official reference solution ({1})")
    @MethodSource("representativeOfficialProblems")
    void judgeAcceptsRepresentativeOfficialReferenceSolutions(String problemId, String reason) throws IOException {
        assertRepresentativeProblemAccepted(problemId);
    }

    @ParameterizedTest(name = "p{0} judge accepts official reference solution through ECJ fallback ({1})")
    @MethodSource("representativeOfficialProblems")
    void judgeAcceptsRepresentativeOfficialReferenceSolutionsThroughEcjFallback(String problemId, String reason) throws IOException {
        System.setProperty(FORCE_ECJ_COMPILER_PROPERTY, "true");
        try {
            assertRepresentativeProblemAccepted(problemId);
        } finally {
            System.clearProperty(FORCE_ECJ_COMPILER_PROPERTY);
        }
    }

    @Test
    void judgeAcceptsMissingRollsAlternativeValidArrays() throws IOException {
        JsonObject json = readProblemJson("455");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String greedyCode = json.getAsJsonArray("solutions").get(1).getAsJsonObject().get("code").getAsString();

        SubmissionResult result = Judge.grade(problem, greedyCode);

        assertTrue(result.isSuccess(), () -> "p455 allows any dice array with the right length, range, and sum: "
                + result.getMessage() + " details=" + summarizeDetails(result));
        assertEquals(result.getTotalCount(), result.getPassedCount(),
                "p455 alternate valid arrays should pass every official test through Judge.grade");
    }

    @Test
    void judgeAcceptsAlienDictionaryAlternativeValidOrders() throws IOException {
        JsonObject json = readProblemJson("146");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String reverseReadyKahnCode = """
                import java.util.*;
                class Solution {
                    public String alienOrder(String[] words) {
                        Map<Character, Set<Character>> graph = new HashMap<>();
                        Map<Character, Integer> indegree = new HashMap<>();
                        for (String word : words) {
                            for (char ch : word.toCharArray()) {
                                graph.putIfAbsent(ch, new HashSet<>());
                                indegree.putIfAbsent(ch, 0);
                            }
                        }
                        for (int i = 0; i + 1 < words.length; i++) {
                            String first = words[i];
                            String second = words[i + 1];
                            if (first.length() > second.length() && first.startsWith(second)) {
                                return "";
                            }
                            int limit = Math.min(first.length(), second.length());
                            for (int j = 0; j < limit; j++) {
                                char from = first.charAt(j);
                                char to = second.charAt(j);
                                if (from != to) {
                                    if (graph.get(from).add(to)) {
                                        indegree.merge(to, 1, Integer::sum);
                                    }
                                    break;
                                }
                            }
                        }
                        PriorityQueue<Character> ready = new PriorityQueue<>(Comparator.reverseOrder());
                        for (char ch : indegree.keySet()) {
                            if (indegree.get(ch) == 0) {
                                ready.offer(ch);
                            }
                        }
                        StringBuilder order = new StringBuilder();
                        while (!ready.isEmpty()) {
                            char ch = ready.poll();
                            order.append(ch);
                            for (char next : graph.get(ch)) {
                                indegree.put(next, indegree.get(next) - 1);
                                if (indegree.get(next) == 0) {
                                    ready.offer(next);
                                }
                            }
                        }
                        return order.length() == indegree.size() ? order.toString() : "";
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, reverseReadyKahnCode);

        assertTrue(result.isSuccess(), () -> "p146 accepts any valid alien alphabet order: "
                + result.getMessage() + " details=" + summarizeDetails(result));
        assertEquals(result.getTotalCount(), result.getPassedCount(),
                "p146 alternative valid orders should pass every official test through Judge.grade");
    }

    @Test
    void judgeRejectsAlienDictionaryOrdersWithMissingCharacters() throws IOException {
        JsonObject json = readProblemJson("146");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String missingCharactersCode = """
                class Solution {
                    public String alienOrder(String[] words) {
                        return "a";
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, missingCharactersCode);

        assertFalse(result.isSuccess(),
                "p146 validator must reject outputs that do not contain every distinct character exactly once");
    }

    @Test
    void judgeRejectsMissingRollsArraysWithInvalidSumOrLength() throws IOException {
        JsonObject json = readProblemJson("455");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String invalidCode = """
                class Solution {
                    public int[] missingRolls(int[] rolls, int mean, int n) {
                        return new int[]{6, 6, 6, 6};
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, invalidCode);

        assertFalse(result.isSuccess(), "p455 validator must still reject arrays with wrong length or sum");
    }

    @Test
    void judgeDoesNotAcceptReorderedArrayWhenProblemRequiresIndexOrder() throws IOException {
        JsonObject json = readProblemJson("6");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String reversedButSameMultiset = """
                class Solution {
                    public int[] productExceptSelf(int[] nums) {
                        int n = nums.length;
                        int[] correct = new int[n];
                        int prefix = 1;
                        for (int i = 0; i < n; i++) {
                            correct[i] = prefix;
                            prefix *= nums[i];
                        }
                        int suffix = 1;
                        for (int i = n - 1; i >= 0; i--) {
                            correct[i] *= suffix;
                            suffix *= nums[i];
                        }
                        for (int left = 0, right = n - 1; left < right; left++, right--) {
                            int tmp = correct[left];
                            correct[left] = correct[right];
                            correct[right] = tmp;
                        }
                        return correct;
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, reversedButSameMultiset);

        assertFalse(result.isSuccess(),
                "p6 productExceptSelf is order-sensitive; a reversed multiset must not be accepted");
    }

    @Test
    void judgeAcceptsSortArrayByParityAlternativeValidPartitions() throws IOException {
        JsonObject json = readProblemJson("236");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String parityPartitionCode = """
                class Solution {
                    public int[] sortArrayByParity(int[] nums) {
                        int[] answer = new int[nums.length];
                        int write = 0;
                        for (int i = nums.length - 1; i >= 0; i--) {
                            if (nums[i] % 2 == 0) answer[write++] = nums[i];
                        }
                        for (int value : nums) {
                            if (value % 2 != 0) answer[write++] = value;
                        }
                        return answer;
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, parityPartitionCode);

        assertTrue(result.isSuccess(), () -> "p236 accepts any partition with all evens before odds: "
                + result.getMessage() + " details=" + summarizeDetails(result));
    }

    @Test
    void judgeRejectsSortArrayByParityOutputsWithOddBeforeEven() throws IOException {
        JsonObject json = readProblemJson("236");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String unchangedCode = """
                class Solution {
                    public int[] sortArrayByParity(int[] nums) {
                        return nums;
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, unchangedCode);

        assertFalse(result.isSuccess(), "p236 validator must reject arrays that leave an odd before an even");
    }

    @Test
    void judgeAcceptsSmallestSufficientTeamInAnyOrder() throws IOException {
        JsonObject json = readProblemJson("399");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String pathReconstructionCode = json.getAsJsonArray("solutions").get(1).getAsJsonObject().get("code").getAsString();

        SubmissionResult result = Judge.grade(problem, pathReconstructionCode);

        assertTrue(result.isSuccess(), () -> "p399 team member order should not matter: "
                + result.getMessage() + " details=" + summarizeDetails(result));
    }

    @Test
    void judgeRejectsSmallestSufficientTeamThatDoesNotCoverSkills() throws IOException {
        JsonObject json = readProblemJson("399");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String insufficientTeamCode = """
                import java.util.*;
                class Solution {
                    public int[] smallestSufficientTeam(String[] req_skills, List<List<String>> people) {
                        return new int[]{0};
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, insufficientTeamCode);

        assertFalse(result.isSuccess(), "p399 validator must reject a team that does not cover every required skill");
    }

    @Test
    void judgeDoesNotAcceptReorderedNestedListWhenProblemRequiresRowOrder() throws IOException {
        JsonObject json = readProblemJson("19");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String reversedPascalRows = """
                import java.util.*;
                class Solution {
                    public List<List<Integer>> generate(int numRows) {
                        List<List<Integer>> rows = new ArrayList<>();
                        for (int row = 0; row < numRows; row++) {
                            List<Integer> current = new ArrayList<>();
                            for (int col = 0; col <= row; col++) {
                                if (col == 0 || col == row) {
                                    current.add(1);
                                } else {
                                    current.add(rows.get(row - 1).get(col - 1) + rows.get(row - 1).get(col));
                                }
                            }
                            rows.add(current);
                        }
                        Collections.reverse(rows);
                        return rows;
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, reversedPascalRows);

        assertFalse(result.isSuccess(),
                "p19 Pascal's Triangle is row-order-sensitive; reversed rows must not be accepted");
    }

    @Test
    void judgeDoesNotAcceptReorderedIntervalsWhenProblemRequiresSortedOrder() throws IOException {
        JsonObject json = readProblemJson("182");
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String reversedMergedIntervals = """
                import java.util.*;
                class Solution {
                    public int[][] insert(int[][] intervals, int[] newInterval) {
                        List<int[]> merged = new ArrayList<>();
                        int i = 0;
                        while (i < intervals.length && intervals[i][1] < newInterval[0]) {
                            merged.add(intervals[i++]);
                        }
                        while (i < intervals.length && intervals[i][0] <= newInterval[1]) {
                            newInterval[0] = Math.min(newInterval[0], intervals[i][0]);
                            newInterval[1] = Math.max(newInterval[1], intervals[i][1]);
                            i++;
                        }
                        merged.add(newInterval);
                        while (i < intervals.length) {
                            merged.add(intervals[i++]);
                        }
                        Collections.reverse(merged);
                        return merged.toArray(new int[merged.size()][]);
                    }
                }
                """;

        SubmissionResult result = Judge.grade(problem, reversedMergedIntervals);

        assertFalse(result.isSuccess(),
                "p182 Insert Interval requires sorted interval order; reversed rows must not be accepted");
    }

    private void assertRepresentativeProblemAccepted(String problemId) throws IOException {
        JsonObject json = readProblemJson(problemId);
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        String code = json.getAsJsonArray("solutions").get(0).getAsJsonObject().get("code").getAsString();

        SubmissionResult result = Judge.grade(problem, code);

        assertTrue(result.isSuccess(), () -> "p" + problemId + " should be accepted through in-game Judge path: "
                + result.getMessage() + " details=" + summarizeDetails(result));
        assertEquals(result.getTotalCount(), result.getPassedCount(),
                () -> "p" + problemId + " should pass every official test through in-game Judge path");
    }

    private static JsonObject readProblemJson(String problemId) throws IOException {
        Path problemFile = QUESTION_BANK.resolve("p" + problemId + ".json");
        return JsonParser.parseString(Files.readString(problemFile, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String summarizeDetails(SubmissionResult result) {
        StringBuilder summary = new StringBuilder();
        for (SubmissionResult.TestCaseResult detail : result.getDetails()) {
            if (!detail.isPassed()) {
                summary.append("{input=").append(detail.getInput())
                        .append(", expected=").append(detail.getExpected())
                        .append(", actual=").append(detail.getActual())
                        .append(", error=").append(detail.getError())
                        .append("}");
            }
        }
        return summary.toString();
    }
}
