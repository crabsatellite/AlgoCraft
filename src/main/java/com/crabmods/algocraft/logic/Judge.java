package com.crabmods.algocraft.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Judges user code submissions against problem test cases.
 * Handles both example (visible) and hidden test cases.
 * 
 * <p>Uses batch execution to compile code once and run all tests,
 * significantly improving performance over per-test compilation.
 */
public class Judge {

    private static final System.Logger LOGGER = System.getLogger(Judge.class.getName());

    @FunctionalInterface
    interface BatchExecutor {
        List<CodeExecutor.TestResult> execute(String code, List<CodeExecutor.TestCase> testCases);
    }
    
    /**
     * Grade a code submission against a problem's test cases.
     * Uses batch execution for optimal performance - compiles once, runs all tests.
     * 
     * @param problem The problem containing test cases
     * @param userCode The user's submitted code
     * @return SubmissionResult containing pass/fail status and details
     */
    public static SubmissionResult grade(Problem problem, String userCode) {
        return grade(problem, userCode, CodeExecutor::executeBatch);
    }

    static SubmissionResult gradeForTest(Problem problem, String userCode, BatchExecutor batchExecutor) {
        return grade(problem, userCode, batchExecutor);
    }

    private static SubmissionResult grade(Problem problem, String userCode, BatchExecutor batchExecutor) {
        SubmissionResult result = new SubmissionResult();
        
        // Validate inputs
        if (problem == null) {
            result.setSuccess(false);
            result.setMessage("Invalid problem");
            result.setTotalCount(0);
            return result;
        }
        
        if (userCode == null || userCode.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("No code submitted");
            result.setTotalCount(0);
            return result;
        }
        
        // Collect all valid test cases
        List<Problem.TestCase> examples = problem.getExamples();
        List<Problem.TestCase> hiddenTests = problem.getTests();
        
        // Build test case list for batch execution
        List<CodeExecutor.TestCase> allTests = new ArrayList<>();
        List<Boolean> isHiddenFlags = new ArrayList<>();
        List<Problem.TestCase> validTests = new ArrayList<>();
        String preferredMethodName = CodeExecutor.preferredMethodNameFromInitialCode(problem.getInitialCode());
        
        // Add examples (visible)
        for (int i = 0; i < examples.size(); i++) {
            Problem.TestCase test = examples.get(i);
            if (test == null || !test.isValid()) {
                return invalidProblemTestCase(result, problem, "example", i,
                        examples.size() + hiddenTests.size());
            }
            allTests.add(new CodeExecutor.TestCase(
                    test.getInput(),
                    test.getOutput(),
                    problem.getId(),
                    preferredMethodName
            ));
            isHiddenFlags.add(false);
            validTests.add(test);
        }
        
        // Add hidden tests
        for (int i = 0; i < hiddenTests.size(); i++) {
            Problem.TestCase test = hiddenTests.get(i);
            if (test == null || !test.isValid()) {
                return invalidProblemTestCase(result, problem, "hidden", i,
                        examples.size() + hiddenTests.size());
            }
            allTests.add(new CodeExecutor.TestCase(
                    test.getInput(),
                    test.getOutput(),
                    problem.getId(),
                    preferredMethodName
            ));
            isHiddenFlags.add(true);
            validTests.add(test);
        }
        
        result.setTotalCount(allTests.size());
        
        if (allTests.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("No test cases available");
            return result;
        }
        
        result.setSuccess(true);
        long startTime = System.currentTimeMillis();
        
        boolean hasRuntimeError = false;
        boolean hasCompileError = false;
        boolean hasTimeLimit = false;
        
        // Execute all tests in batch (compiles once!)
        List<CodeExecutor.TestResult> testResults = batchExecutor.execute(userCode, allTests);
        if (testResults == null || testResults.size() != allTests.size()) {
            result.setSuccess(false);
            result.setMessage("Internal Judge Error");
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            LOGGER.log(System.Logger.Level.ERROR,
                    "Judge batch result count mismatch for problem " + problem.getId()
                            + ": expected " + allTests.size()
                            + ", got " + (testResults == null ? "null" : testResults.size()));
            return result;
        }
        for (int i = 0; i < testResults.size(); i++) {
            if (testResults.get(i) == null) {
                result.setSuccess(false);
                result.setMessage("Internal Judge Error");
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                LOGGER.log(System.Logger.Level.ERROR,
                        "Judge batch result was null for problem " + problem.getId() + " at index " + i);
                return result;
            }
        }

        // Process results
        for (int i = 0; i < testResults.size(); i++) {
            CodeExecutor.TestResult execResult = testResults.get(i);
            boolean isHidden = isHiddenFlags.get(i);
            Problem.TestCase originalTest = validTests.get(i);
            
            SubmissionResult.TestCaseResult caseResult = new SubmissionResult.TestCaseResult();
            
            // Set input/expected display (hide for hidden tests)
            if (isHidden) {
                caseResult.setInput("[Hidden Test]");
                caseResult.setExpected("[Hidden]");
            } else {
                caseResult.setInput(originalTest.getInput());
                caseResult.setExpected(originalTest.getOutput());
            }
            
            if (execResult.passed) {
                caseResult.setPassed(true);
                caseResult.setActual(isHidden ? "[Hidden]" : originalTest.getOutput());
                result.incrementPassedCount();
            } else {
                caseResult.setPassed(false);
                result.setSuccess(false);
                
                if (execResult.message != null && execResult.message.startsWith("ERROR:")) {
                    caseResult.setError(execResult.message);
                    caseResult.setActual("Error");
                    
                    if (execResult.isCompileError()) hasCompileError = true;
                    else if (execResult.isTimeLimit()) hasTimeLimit = true;
                    else if (execResult.isRuntimeError()) hasRuntimeError = true;
                } else {
                    // Wrong answer
                    String actualOutput = execResult.actualOutput != null ? execResult.actualOutput : "Unknown";
                    caseResult.setActual(isHidden ? "[Hidden]" : actualOutput);
                }
            }
            
            result.addDetail(caseResult);
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        
        // Determine final message based on error types
        if (result.isSuccess()) {
            result.setMessage("Accepted");
        } else if (hasCompileError) {
            result.setMessage("Compilation Error");
        } else if (hasTimeLimit) {
            result.setMessage("Time Limit Exceeded");
        } else if (hasRuntimeError) {
            result.setMessage("Runtime Error");
        } else {
            result.setMessage("Wrong Answer");
        }

        LOGGER.log(System.Logger.Level.DEBUG,
                "Judged problem " + problem.getId() + ": " + result.getMessage()
                        + " (" + result.getPassedCount() + "/" + result.getTotalCount() + ")");

        return result;
    }

    private static SubmissionResult invalidProblemTestCase(SubmissionResult result, Problem problem, String kind,
                                                           int index, int totalCount) {
        result.setSuccess(false);
        result.setMessage("Invalid problem test case");
        result.setTotalCount(totalCount);
        LOGGER.log(System.Logger.Level.ERROR,
                "Invalid " + kind + " test case " + index + " for problem " + problem.getId());
        return result;
    }
}
