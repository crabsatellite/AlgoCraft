package com.crabmods.algocraft.logic;

import java.util.List;
import java.util.ArrayList;

public class Problem {
    public String id;
    public String title;
    public String description; // Markdown content
    public String difficulty; // "EASY", "MEDIUM", "HARD"
    public String initialCode;
    public List<String> tags = new ArrayList<>();
    public List<TestCase> examples = new ArrayList<>();
    public List<TestCase> tests = new ArrayList<>();
    
    // Metadata
    public String author;
    public String version;
    public long lastModified;

    public static class TestCase {
        public String input;
        public String output;
        public boolean hidden;
    }
}
