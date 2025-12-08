package com.crabmods.algocraft.logic;

import java.util.List;

public class Problem {
    public String id;
    public String title;
    public String description;
    public String difficulty; // "easy", "medium", "hard", "insane"
    public String initialCode;
    public List<TestCase> examples;
    public List<TestCase> tests;

    public static class TestCase {
        public String input;
        public String output;
    }
}
