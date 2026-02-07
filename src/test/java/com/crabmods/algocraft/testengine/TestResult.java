package com.crabmods.algocraft.testengine;

/**
 * Result of a single test case execution.
 */
public class TestResult {
    public final boolean passed;
    public final String input;
    public final String expected;
    public final String actual;
    public final String error;

    private TestResult(boolean passed, String input, String expected, String actual, String error) {
        this.passed = passed;
        this.input = input;
        this.expected = expected;
        this.actual = actual;
        this.error = error;
    }

    public static TestResult pass() {
        return new TestResult(true, null, null, null, null);
    }

    public static TestResult fail(String input, String expected, String actual) {
        return new TestResult(false, input, expected, actual, null);
    }

    public static TestResult error(String error) {
        return new TestResult(false, null, null, null, error);
    }

    @Override
    public String toString() {
        if (passed) return "PASS";
        if (error != null) return "ERROR: " + error;
        return "FAIL: expected=" + expected + ", actual=" + actual;
    }
}
