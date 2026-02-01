package com.crabmods.algocraft.logic;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a code submission evaluation.
 * Contains information about test case results, timing, and overall status.
 * 
 * Uses @SerializedName annotations to ensure JSON compatibility with the web API.
 */
public class SubmissionResult {
    @SerializedName("isSuccess")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("passedCount")
    private int passedCount;
    
    @SerializedName("totalCount")
    private int totalCount;
    
    @SerializedName("executionTimeMs")
    private long executionTimeMs;
    
    @SerializedName("details")
    private final List<TestCaseResult> details = new ArrayList<>();

    /**
     * Check if the submission was successful (all tests passed).
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Get the result message (e.g., "Accepted", "Wrong Answer", "Runtime Error").
     */
    public String getMessage() {
        return message != null ? message : (success ? "Accepted" : "Unknown Error");
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the number of passed test cases.
     */
    public int getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(int passedCount) {
        this.passedCount = passedCount;
    }
    
    public void incrementPassedCount() {
        this.passedCount++;
    }

    /**
     * Get the total number of test cases.
     */
    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * Get the total execution time in milliseconds.
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Get detailed results for each test case.
     */
    public List<TestCaseResult> getDetails() {
        return Collections.unmodifiableList(details);
    }
    
    /**
     * Add a test case result.
     */
    public void addDetail(TestCaseResult result) {
        if (result != null) {
            details.add(result);
        }
    }

    /**
     * Get the pass rate as a percentage (0-100).
     */
    public double getPassRate() {
        if (totalCount == 0) return 0;
        return (double) passedCount / totalCount * 100;
    }
    
    /**
     * Get a summary string for display.
     */
    public String getSummary() {
        return String.format("%s (%d/%d tests passed in %dms)", 
            getMessage(), passedCount, totalCount, executionTimeMs);
    }

    /**
     * Represents the result of a single test case.
     */
    public static class TestCaseResult {
        private String input;
        private String expected;
        private String actual;
        private boolean passed;
        private String error;

        public String getInput() {
            return input != null ? input : "";
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getExpected() {
            return expected != null ? expected : "";
        }

        public void setExpected(String expected) {
            this.expected = expected;
        }

        public String getActual() {
            return actual != null ? actual : "";
        }

        public void setActual(String actual) {
            this.actual = actual;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
        
        /**
         * Check if this test case resulted in an error (as opposed to wrong answer).
         */
        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
    }
}
