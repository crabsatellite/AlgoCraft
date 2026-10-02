package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeContractTest {
    @Test
    void judgeRejectsShortBatchResultInsteadOfAcceptingPartialPass() {
        Problem problem = twoCaseProblem();

        SubmissionResult result = Judge.gradeForTest(problem, """
                class Solution {
                    public int solve(int value) {
                        return value;
                    }
                }
                """, (code, testCases) -> List.of(new CodeExecutor.TestResult(true, null, "1")));

        assertFalse(result.isSuccess(),
                "Judge must fail closed when the execution engine returns fewer results than requested");
        assertEquals("Internal Judge Error", result.getMessage());
        assertEquals(2, result.getTotalCount());
        assertEquals(0, result.getPassedCount(),
                "partial execution results must not be counted as accepted progress");
    }

    @Test
    void judgeRejectsExtraBatchResultsInsteadOfTrustingExecutorShape() {
        Problem problem = twoCaseProblem();

        SubmissionResult result = Judge.gradeForTest(problem, """
                class Solution {
                    public int solve(int value) {
                        return value;
                    }
                }
                """, (code, testCases) -> List.of(
                new CodeExecutor.TestResult(true, null, "1"),
                new CodeExecutor.TestResult(true, null, "2"),
                new CodeExecutor.TestResult(true, null, "3")
        ));

        assertFalse(result.isSuccess(),
                "Judge must fail closed when the execution engine returns more results than requested");
        assertEquals("Internal Judge Error", result.getMessage());
        assertEquals(2, result.getTotalCount());
    }

    @Test
    void judgeRejectsNullBatchResultInsteadOfThrowingOrAccepting() {
        Problem problem = twoCaseProblem();

        SubmissionResult result = Judge.gradeForTest(problem, """
                class Solution {
                    public int solve(int value) {
                        return value;
                    }
                }
                """, (code, testCases) -> {
            List<CodeExecutor.TestResult> results = new java.util.ArrayList<>();
            results.add(new CodeExecutor.TestResult(true, "PASS", "1"));
            results.add(null);
            return results;
        });

        assertFalse(result.isSuccess(),
                "Judge must fail closed when the execution engine returns a null result");
        assertEquals("Internal Judge Error", result.getMessage());
        assertEquals(2, result.getTotalCount());
        assertEquals(0, result.getPassedCount());
    }

    @Test
    void judgeRejectsInvalidExampleTestCaseInsteadOfSkippingIt() {
        Problem problem = twoCaseProblem();
        problem.setExamples(List.of(
                testCase("1", "1"),
                testCase("2", null)
        ));

        SubmissionResult result = Judge.gradeForTest(problem, """
                class Solution {
                    public int solve(int value) {
                        return value;
                    }
                }
                """, (code, testCases) -> {
            throw new AssertionError("Judge must not execute code when an example test case is malformed");
        });

        assertFalse(result.isSuccess(),
                "Judge must fail closed when an example test case is malformed");
        assertEquals("Invalid problem test case", result.getMessage());
        assertEquals(2, result.getTotalCount());
        assertEquals(0, result.getPassedCount());
    }

    @Test
    void judgeRejectsInvalidHiddenTestCaseInsteadOfSkippingIt() {
        Problem problem = twoCaseProblem();
        problem.setTests(List.of(testCase("3", null)));

        SubmissionResult result = Judge.gradeForTest(problem, """
                class Solution {
                    public int solve(int value) {
                        return value;
                    }
                }
                """, (code, testCases) -> {
            throw new AssertionError("Judge must not execute code when a hidden test case is malformed");
        });

        assertFalse(result.isSuccess(),
                "Judge must fail closed when a hidden test case is malformed");
        assertEquals("Invalid problem test case", result.getMessage());
        assertEquals(3, result.getTotalCount());
        assertEquals(0, result.getPassedCount());
    }

    @Test
    void judgeAcceptsDesignClassMixedOutputArrayDoubleWithinTolerance() {
        Problem problem = new Problem();
        problem.setId("design-double-contract");
        problem.setTitle("Design Double Contract");
        problem.setDifficulty("MEDIUM");
        problem.setInitialCode("""
                class AverageBox {
                    public AverageBox() {
                    }

                    public double value() {
                        return 0.0;
                    }
                }
                """);
        problem.setTests(List.of(testCase(
                "[\"AverageBox\",\"value\"]\n[[],[]]",
                "[null,1.00000]")));

        SubmissionResult result = Judge.grade(problem, """
                class AverageBox {
                    public AverageBox() {
                    }

                    public double value() {
                        return 1.000001;
                    }
                }
                """);

        assertTrue(result.isSuccess(), () -> "design class outputs mix null and double values, so Judge "
                + "must compare array elements with floating-point tolerance: " + result.getMessage());
        assertEquals(1, result.getPassedCount());
    }

    private static Problem twoCaseProblem() {
        Problem problem = new Problem();
        problem.setId("judge-contract");
        problem.setTitle("Judge Contract");
        problem.setDifficulty("EASY");
        problem.setInitialCode("""
                class Solution {
                    public int solve(int value) {
                        return value;
                    }
                }
                """);
        problem.setExamples(List.of(
                testCase("1", "1"),
                testCase("2", "2")
        ));
        return problem;
    }

    private static Problem.TestCase testCase(String input, String output) {
        Problem.TestCase testCase = new Problem.TestCase();
        testCase.setInput(input);
        testCase.setOutput(output);
        return testCase;
    }
}
