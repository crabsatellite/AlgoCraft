package com.crabmods.algocraft.logic;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

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
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Grade a code submission against a problem's test cases.
     * Uses batch execution for optimal performance - compiles once, runs all tests.
     * 
     * @param problem The problem containing test cases
     * @param userCode The user's submitted code
     * @return SubmissionResult containing pass/fail status and details
     */
    public static SubmissionResult grade(Problem problem, String userCode) {
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
        
        // Add examples (visible)
        for (int i = 0; i < examples.size(); i++) {
            Problem.TestCase test = examples.get(i);
            if (test == null || !test.isValid()) {
                LOGGER.warn("Skipping invalid example test case {} for problem {}", i, problem.getId());
                continue;
            }
            allTests.add(new CodeExecutor.TestCase(test.getInput(), test.getOutput()));
            isHiddenFlags.add(false);
            validTests.add(test);
        }
        
        // Add hidden tests
        for (int i = 0; i < hiddenTests.size(); i++) {
            Problem.TestCase test = hiddenTests.get(i);
            if (test == null || !test.isValid()) {
                LOGGER.warn("Skipping invalid hidden test case {} for problem {}", i, problem.getId());
                continue;
            }
            allTests.add(new CodeExecutor.TestCase(test.getInput(), test.getOutput()));
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
        List<CodeExecutor.TestResult> testResults = CodeExecutor.executeBatch(userCode, allTests);
        
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

        LOGGER.debug("Judged problem {}: {} ({}/{})", 
            problem.getId(), result.getMessage(), result.getPassedCount(), result.getTotalCount());

        return result;
    }
}
