package com.crabmods.algocraft.logic;

public class Judge {
    
    public static SubmissionResult grade(Problem problem, String userCode) {
        SubmissionResult result = new SubmissionResult();
        result.totalCount = (problem.examples != null ? problem.examples.size() : 0) + (problem.tests != null ? problem.tests.size() : 0);
        result.isSuccess = true;
        long startTime = System.currentTimeMillis();

        // 1. Run Examples
        if (problem.examples != null) {
            for (Problem.TestCase test : problem.examples) {
                if (!runSingleTest(userCode, test, result)) {
                    result.isSuccess = false;
                    // We continue running to give full feedback, or break if we want "fail fast"
                }
            }
        }

        // 2. Run Hidden Tests
        if (problem.tests != null) {
            for (Problem.TestCase test : problem.tests) {
                if (!runSingleTest(userCode, test, result)) {
                    result.isSuccess = false;
                    result.message = "Hidden Test Failed";
                    // Usually we don't show details for hidden tests in real platforms, 
                    // but for this mod we might want to show "Hidden Test #X Failed"
                }
            }
        }

        result.executionTimeMs = System.currentTimeMillis() - startTime;
        
        if (result.isSuccess) {
            result.message = "Accepted";
        } else if (result.message == null) {
            result.message = "Wrong Answer";
        }

        return result;
    }

    private static boolean runSingleTest(String code, Problem.TestCase test, SubmissionResult result) {
        SubmissionResult.TestCaseResult caseResult = new SubmissionResult.TestCaseResult();
        caseResult.input = test.input;
        caseResult.expected = test.output;
        
        String execResult = CodeExecutor.execute(code, test.input, test.output);
        
        if (execResult.startsWith("ERROR:")) {
            caseResult.passed = false;
            caseResult.error = execResult;
            caseResult.actual = "Runtime Error";
            result.details.add(caseResult);
            result.message = "Runtime Error";
            return false;
        } else if (execResult.startsWith("FAIL:")) {
            caseResult.passed = false;
            caseResult.actual = execResult.replace("FAIL: Expected " + test.output + ", got ", "");
            result.details.add(caseResult);
            return false;
        } else {
            caseResult.passed = true;
            caseResult.actual = test.output;
            result.passedCount++;
            result.details.add(caseResult);
            return true;
        }
    }
}
