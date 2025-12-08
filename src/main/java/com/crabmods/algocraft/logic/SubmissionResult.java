package com.crabmods.algocraft.logic;

import java.util.ArrayList;
import java.util.List;

public class SubmissionResult {
    public boolean isSuccess;
    public String message;
    public int passedCount;
    public int totalCount;
    public long executionTimeMs;
    public List<TestCaseResult> details = new ArrayList<>();

    public static class TestCaseResult {
        public String input;
        public String expected;
        public String actual;
        public boolean passed;
        public String error;
    }
}
