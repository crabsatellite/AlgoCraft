package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeExecutorContractTest {
    private static final Path CODE_EXECUTOR = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "logic", "CodeExecutor.java"
    );

    @Test
    void workerBatchResponseWithTooFewResultsBecomesPerCaseError() {
        List<CodeExecutor.TestCase> testCases = List.of(
                new CodeExecutor.TestCase("1", "1", "contract"),
                new CodeExecutor.TestCase("2", "2", "contract")
        );
        CodeExecutor.WorkerResponse response = CodeExecutor.WorkerResponse.batch(List.of(
                new CodeExecutor.TestResult(true, null, "1")
        ));

        List<CodeExecutor.TestResult> results =
                CodeExecutor.normalizeWorkerBatchResultsForTest(testCases, response);

        assertEquals(2, results.size());
        assertFalse(results.get(0).passed);
        assertFalse(results.get(1).passed);
        assertTrue(results.get(0).message.contains("Worker returned 1 results for 2 test cases"));
    }

    @Test
    void workerBatchResponseWithTooManyResultsBecomesPerCaseError() {
        List<CodeExecutor.TestCase> testCases = List.of(
                new CodeExecutor.TestCase("1", "1", "contract")
        );
        CodeExecutor.WorkerResponse response = CodeExecutor.WorkerResponse.batch(List.of(
                new CodeExecutor.TestResult(true, null, "1"),
                new CodeExecutor.TestResult(true, null, "extra")
        ));

        List<CodeExecutor.TestResult> results =
                CodeExecutor.normalizeWorkerBatchResultsForTest(testCases, response);

        assertEquals(1, results.size());
        assertFalse(results.getFirst().passed);
        assertTrue(results.getFirst().message.contains("Worker returned 2 results for 1 test cases"));
    }

    @Test
    void workerBatchResponseWithNullResultBecomesPerCaseError() {
        List<CodeExecutor.TestCase> testCases = List.of(
                new CodeExecutor.TestCase("1", "1", "contract"),
                new CodeExecutor.TestCase("2", "2", "contract")
        );
        CodeExecutor.WorkerResponse response = new CodeExecutor.WorkerResponse();
        response.batchResults = new ArrayList<>();
        response.batchResults.add(new CodeExecutor.WorkerTestResult(true, "PASS", "1"));
        response.batchResults.add(null);

        List<CodeExecutor.TestResult> results =
                CodeExecutor.normalizeWorkerBatchResultsForTest(testCases, response);

        assertEquals(2, results.size());
        assertFalse(results.get(0).passed);
        assertFalse(results.get(1).passed);
        assertTrue(results.get(0).message.contains("Worker returned null result at index 1"));
    }

    @Test
    void codeEnvelopeValidationRejectsEmptyAndOversizedSubmissions() {
        assertEquals("ERROR: No code provided", CodeExecutor.validateCodeEnvelope(""));
        assertEquals("ERROR: No code provided", CodeExecutor.validateCodeEnvelope(null));

        String oversized = "x".repeat(CodeExecutor.MAX_CODE_LENGTH + 1);
        assertEquals("ERROR: Code exceeds maximum length of " + CodeExecutor.MAX_CODE_LENGTH + " characters",
                CodeExecutor.validateCodeEnvelope(oversized));
        assertNull(CodeExecutor.validateCodeEnvelope("class Solution { int solve() { return 1; } }"));
    }

    @Test
    void publicEntrypointsRejectOversizedCodeBeforeWorkerRequestSerialization() throws IOException {
        String source = Files.readString(CODE_EXECUTOR, StandardCharsets.UTF_8);

        assertTrue(source.contains("""
                String envelopeError = validateCodeEnvelope(code);
                        if (envelopeError != null) {
                            return envelopeError;
                        }
                        WorkerRequest request = WorkerRequest.single(code, input, expectedOutput);
                """),
                "single-case execution must reject oversized code before creating a worker request");
        assertTrue(source.contains("""
                List<TestResult> envelopeErrors = validateCodeEnvelopeForBatch(code, testCases);
                        if (envelopeErrors != null) {
                            return envelopeErrors;
                        }
                        List<TestResult> testCaseErrors = validateBatchTestCases(testCases);
                        if (testCaseErrors != null) {
                            return testCaseErrors;
                        }
                        WorkerRequest request = WorkerRequest.batch(code, testCases);
                """),
                "batch execution must reject invalid envelopes and test case lists before creating a worker request");
    }

    @Test
    void executeBatchReturnsEnvelopeErrorsPerRequestedTestCase() {
        String oversized = "x".repeat(CodeExecutor.MAX_CODE_LENGTH + 1);

        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch(oversized, List.of(
                new CodeExecutor.TestCase("a", "1", "contract"),
                new CodeExecutor.TestCase("b", "2", "contract")
        ));

        assertEquals(2, results.size());
        assertFalse(results.get(0).passed);
        assertFalse(results.get(1).passed);
        assertTrue(results.get(0).message.contains("Code exceeds maximum length"), results.get(0).message);
        assertTrue(results.get(1).message.contains("Code exceeds maximum length"), results.get(1).message);
    }

    @Test
    void executeBatchReturnsEnvelopeErrorEvenWhenNoTestCasesWereProvided() {
        String oversized = "x".repeat(CodeExecutor.MAX_CODE_LENGTH + 1);

        List<CodeExecutor.TestResult> nullResults = CodeExecutor.executeBatch(oversized, null);
        List<CodeExecutor.TestResult> emptyResults = CodeExecutor.executeBatch(oversized, List.of());

        assertEquals(1, nullResults.size());
        assertFalse(nullResults.getFirst().passed);
        assertTrue(nullResults.getFirst().message.contains("Code exceeds maximum length"),
                nullResults.getFirst().message);
        assertEquals(1, emptyResults.size());
        assertFalse(emptyResults.getFirst().passed);
        assertTrue(emptyResults.getFirst().message.contains("Code exceeds maximum length"),
                emptyResults.getFirst().message);
    }

    @Test
    void executeBatchRejectsMissingOrNullTestCasesBeforeWorkerExecution() {
        String validCode = "class Solution { public int solve() { return 1; } }";

        List<CodeExecutor.TestResult> nullResults = CodeExecutor.executeBatch(validCode, null);
        List<CodeExecutor.TestResult> emptyResults = CodeExecutor.executeBatch(validCode, List.of());
        List<CodeExecutor.TestCase> testsWithNullCase = new ArrayList<>();
        testsWithNullCase.add(new CodeExecutor.TestCase("", "1", "contract"));
        testsWithNullCase.add(null);
        List<CodeExecutor.TestResult> nullCaseResults = CodeExecutor.executeBatch(validCode, testsWithNullCase);

        assertEquals(1, nullResults.size());
        assertFalse(nullResults.getFirst().passed);
        assertTrue(nullResults.getFirst().message.contains("No test cases provided"), nullResults.getFirst().message);
        assertEquals(1, emptyResults.size());
        assertFalse(emptyResults.getFirst().passed);
        assertTrue(emptyResults.getFirst().message.contains("No test cases provided"), emptyResults.getFirst().message);
        assertEquals(2, nullCaseResults.size());
        assertFalse(nullCaseResults.get(0).passed);
        assertFalse(nullCaseResults.get(1).passed);
        assertTrue(nullCaseResults.getFirst().message.contains("Null test case at index 1"),
                nullCaseResults.getFirst().message);
    }

    @Test
    void executePrefersInputCompatibleEntrypointOverPublicHelperMethod() {
        String result = CodeExecutor.execute("""
                class Solution {
                    public int helper(int[] nums) {
                        return 999;
                    }

                    int[] twoSum(int[] nums, int target) {
                        return new int[] {0, 1};
                    }
                }
                """, "nums = [2,7,11,15], target = 9", "[0,1]");

        assertEquals("PASS", result);
    }

    @Test
    void executeBatchPrefersProblemTemplateEntrypointOverSameShapeHelperMethod() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int[] calculate(int[] nums, int target) {
                        return new int[] {9, 9};
                    }

                    int[] twoSum(int[] nums, int target) {
                        return new int[] {0, 1};
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "nums = [2,7,11,15], target = 9",
                "[0,1]",
                "1",
                "twoSum"
        )));

        assertEquals(1, results.size());
        assertTrue(results.getFirst().passed, results.getFirst().message);
    }

    @Test
    void removeDuplicatesBatchRequiresMutatedUniquePrefixNotJustReturnedLength() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int removeDuplicates(int[] nums) {
                        return 2;
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "nums = [1,1,2]",
                "2",
                "26",
                "removeDuplicates"
        )));

        assertEquals(1, results.size());
        assertFalse(results.getFirst().passed,
                "p26 must not pass when the return value is correct but nums[0..k) was not compacted");
    }

    @Test
    void removeDuplicatesBatchAcceptsReturnedLengthAndMutatedUniquePrefix() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int removeDuplicates(int[] nums) {
                        int write = 0;
                        for (int value : nums) {
                            if (write == 0 || nums[write - 1] != value) {
                                nums[write] = value;
                                write++;
                            }
                        }
                        return write;
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "nums = [0,0,1,1,1,2,2,3,3,4]",
                "5",
                "26",
                "removeDuplicates"
        )));

        assertEquals(1, results.size());
        assertTrue(results.getFirst().passed, results.getFirst().message);
    }

    @Test
    void removeElementBatchRequiresMutatedPrefixNotJustReturnedLength() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int removeElement(int[] nums, int val) {
                        return 2;
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "nums = [3,2,2,3], val = 3",
                "2",
                "27",
                "removeElement"
        )));

        assertEquals(1, results.size());
        assertFalse(results.getFirst().passed,
                "p27 must not pass when the return value is correct but nums[0..k) still contains removed values");
    }

    @Test
    void removeElementBatchAcceptsReturnedLengthAndUnorderedMutatedPrefix() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int removeElement(int[] nums, int val) {
                        int left = 0;
                        int right = nums.length;
                        while (left < right) {
                            if (nums[left] == val) {
                                nums[left] = nums[right - 1];
                                right--;
                            } else {
                                left++;
                            }
                        }
                        return right;
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "nums = [0,1,2,2,3,0,4,2], val = 2",
                "5",
                "27",
                "removeElement"
        )));

        assertEquals(1, results.size());
        assertTrue(results.getFirst().passed, results.getFirst().message);
    }

    @Test
    void crackingSafeBatchAcceptsAnyMinimumCoveringSequence() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public String crackSafe(int n, int k) {
                        return "00110";
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "n = 2, k = 2",
                "\"01100\"",
                "376",
                "crackSafe"
        )));

        assertEquals(1, results.size());
        assertTrue(results.getFirst().passed,
                "p376 should accept any minimum sequence covering every length-n password");
    }

    @Test
    void crackingSafeBatchRejectsIncompleteCoveringSequence() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public String crackSafe(int n, int k) {
                        return "0011";
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "n = 2, k = 2",
                "\"01100\"",
                "376",
                "crackSafe"
        )));

        assertEquals(1, results.size());
        assertFalse(results.getFirst().passed,
                "p376 should reject strings that do not cover every length-n password");
    }

    @Test
    void extractsPreferredEntrypointNameFromProblemTemplate() {
        assertEquals("twoSum", CodeExecutor.preferredMethodNameFromInitialCode("""
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        return new int[] {};
                    }
                }
                """));
    }

    @Test
    void preferredEntrypointExtractionSkipsPrivateTemplateHelperMethods() {
        String initialCode = """
                class Solution {
                    private int[] buildAnswer(int[] nums, int target) {
                        return new int[] {};
                    }

                    public int[] twoSum(int[] nums, int target) {
                        return new int[] {};
                    }
                }
                """;

        assertEquals("twoSum", CodeExecutor.preferredMethodNameFromInitialCode(initialCode));

        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class Solution {
                    public int[] buildAnswer(int[] nums, int target) {
                        return new int[] {9, 9};
                    }

                    public int[] twoSum(int[] nums, int target) {
                        return new int[] {0, 1};
                    }
                }
                """, List.of(new CodeExecutor.TestCase(
                "nums = [2,7,11,15], target = 9",
                "[0,1]",
                "1",
                CodeExecutor.preferredMethodNameFromInitialCode(initialCode)
        )));

        assertEquals(1, results.size());
        assertTrue(results.getFirst().passed, results.getFirst().message);
    }

    @Test
    void randomizedSetGetRandomMustReturnCurrentMemberInsteadOfAnyNonNullValue() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class RandomizedSet {
                    private final java.util.Set<Integer> values = new java.util.HashSet<>();

                    public RandomizedSet() {
                    }

                    public boolean insert(int val) {
                        return values.add(val);
                    }

                    public boolean remove(int val) {
                        return values.remove(val);
                    }

                    public int getRandom() {
                        return 999;
                    }
                }
                """, List.of(randomizedSetExample()));

        assertEquals(1, results.size());
        assertFalse(results.getFirst().passed,
                "getRandom must not pass merely because it returned a non-null integer");
        assertTrue(results.getFirst().actualOutput.contains("999"), results.getFirst().actualOutput);
    }

    @Test
    void randomizedSetGetRandomAllowsAnyCurrentlyPresentMember() {
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch("""
                class RandomizedSet {
                    private final java.util.Set<Integer> values = new java.util.HashSet<>();

                    public RandomizedSet() {
                    }

                    public boolean insert(int val) {
                        return values.add(val);
                    }

                    public boolean remove(int val) {
                        return values.remove(val);
                    }

                    public int getRandom() {
                        return values.contains(1) ? 1 : values.iterator().next();
                    }
                }
                """, List.of(randomizedSetExample()));

        assertEquals(1, results.size());
        assertTrue(results.getFirst().passed,
                "getRandom should accept a different value from the current set than the sample output shows");
    }

    private static CodeExecutor.TestCase randomizedSetExample() {
        return new CodeExecutor.TestCase("""
                ["RandomizedSet", "insert", "remove", "insert", "getRandom", "remove", "insert", "getRandom"]
                [[], [1], [2], [2], [], [1], [2], []]
                """, "[null,true,false,true,2,true,false,2]", "p439");
    }
}
