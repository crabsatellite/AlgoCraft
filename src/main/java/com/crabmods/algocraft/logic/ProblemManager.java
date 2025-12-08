package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ProblemManager {
    private static final List<Problem> problems = new ArrayList<>();
    private static final Gson gson = new Gson();

    public static void loadProblems() {
        problems.clear();
        // Load built-in (mocked for now)
        
        // Problem 0: A + B
        Problem p0 = new Problem();
        p0.id = "0";
        p0.title = "A + B Problem";
        p0.description = "Given two integers a and b, return their sum.";
        p0.difficulty = "easy";
        p0.initialCode = "class Solution {\n    public int aPlusB(int a, int b) {\n        // write your code here\n        return 0;\n    }\n}";
        p0.examples = new ArrayList<>();
        Problem.TestCase ex0 = new Problem.TestCase();
        ex0.input = "a = 1, b = 2";
        ex0.output = "3";
        p0.examples.add(ex0);
        
        p0.tests = new ArrayList<>();
        Problem.TestCase t0 = new Problem.TestCase();
        t0.input = "a = 10, b = 20";
        t0.output = "30";
        p0.tests.add(t0);
        problems.add(p0);

        Problem p1 = new Problem();
        p1.id = "1";
        p1.title = "Two Sum";
        p1.description = "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.";
        p1.difficulty = "easy";
        p1.initialCode = "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // write your code here\n        return new int[]{};\n    }\n}";
        p1.examples = new ArrayList<>();
        Problem.TestCase ex1 = new Problem.TestCase();
        ex1.input = "nums = [2,7,11,15], target = 9";
        ex1.output = "[0,1]";
        p1.examples.add(ex1);
        
        // Hidden tests
        p1.tests = new ArrayList<>();
        Problem.TestCase t1 = new Problem.TestCase();
        t1.input = "nums = [3,2,4], target = 6";
        t1.output = "[1,2]";
        p1.tests.add(t1);
        
        problems.add(p1);

        // Load from .minecraft/algorithm_challenges/
        File dir = new File(Minecraft.getInstance().gameDirectory, "algorithm_challenges");
        if (!dir.exists()) dir.mkdirs();
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try (FileReader reader = new FileReader(f)) {
                    Problem p = gson.fromJson(reader, Problem.class);
                    if (p != null) problems.add(p);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<Problem> getProblems() {
        if (problems.isEmpty()) loadProblems();
        return problems;
    }
}
