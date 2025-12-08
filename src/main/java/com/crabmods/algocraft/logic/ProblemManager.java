package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.logic.repo.BuiltInProblemRepository;
import com.crabmods.algocraft.logic.repo.LocalProblemRepository;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.logic.repo.RepositoryManager;
import com.crabmods.algocraft.logic.repo.RepositoryMetadata;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class ProblemManager {
    private static final List<ProblemRepository> repositories = new ArrayList<>();
    private static final Map<String, Problem> problemCache = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        
        ProgressManager.init();
        RepositoryManager.init();
        
        // 1. Built-in Repository
        repositories.add(new BuiltInProblemRepository(Minecraft.getInstance().getResourceManager()));
        
        // 2. Local Repository (User created)
        File localDir = new File(Minecraft.getInstance().gameDirectory, "algorithm_challenges/user");
        if (!localDir.exists()) localDir.mkdirs();
        repositories.add(new LocalProblemRepository("Local", localDir.toPath()));
        
        // 3. Downloaded Repositories
        for (RepositoryMetadata meta : RepositoryManager.getRepositories()) {
            File repoDir = RepositoryManager.getRepositoryDir(meta);
            repositories.add(new LocalProblemRepository(meta.name, repoDir.toPath()));
        }
        
        initialized = true;
        refreshAll();
    }

    public static void refreshAll() {
        problemCache.clear();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ProblemRepository repo : repositories) {
            futures.add(repo.refresh().thenAccept(v -> {
                for (Problem p : repo.getProblems()) {
                    problemCache.put(p.id, p);
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Add hardcoded fallback if empty (for testing)
        if (problemCache.isEmpty()) {
            addFallbackProblems();
        }
    }
    
    public static CompletableFuture<Void> downloadAndUpdateRepository(String name, String url) {
        // Check if exists
        boolean exists = RepositoryManager.getRepositories().stream().anyMatch(r -> r.name.equals(name));
        if (!exists) {
            RepositoryManager.addRepository(name, url);
        }
        
        RepositoryMetadata meta = RepositoryManager.getRepositories().stream()
                .filter(r -> r.name.equals(name))
                .findFirst()
                .orElseThrow();
                
        return RepositoryManager.updateRepository(meta).thenRun(() -> {
            // Reload this specific repo
            File repoDir = RepositoryManager.getRepositoryDir(meta);
            
            // Check if we already have a repository object for this
            boolean alreadyLoaded = false;
            for (ProblemRepository repo : repositories) {
                if (repo instanceof LocalProblemRepository && repo.getName().equals(name)) {
                    repo.refresh().join(); // Refresh the existing one
                    alreadyLoaded = true;
                    break;
                }
            }
            
            if (!alreadyLoaded) {
                LocalProblemRepository newRepo = new LocalProblemRepository(name, repoDir.toPath());
                repositories.add(newRepo);
                newRepo.refresh().join();
            }
            
            // Re-populate cache
            refreshAll();
        });
    }
    
    public static void addRepository(ProblemRepository repo) {
        repositories.add(repo);
        repo.refresh().thenAccept(v -> {
            for (Problem p : repo.getProblems()) {
                problemCache.put(p.id, p);
            }
        });
    }

    public static List<Problem> getProblems() {
        if (!initialized) init();
        return new ArrayList<>(problemCache.values());
    }
    
    public static Problem getProblem(String id) {
        if (!initialized) init();
        return problemCache.get(id);
    }

    private static void addFallbackProblems() {
        // Problem 0: A + B
        Problem p0 = new Problem();
        p0.id = "0";
        p0.title = "A + B Problem";
        p0.description = "Given two integers a and b, return their sum.";
        p0.difficulty = "EASY";
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
        problemCache.put(p0.id, p0);

        Problem p1 = new Problem();
        p1.id = "1";
        p1.title = "Two Sum";
        p1.description = "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.";
        p1.difficulty = "EASY";
        p1.initialCode = "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // write your code here\n        return new int[]{};\n    }\n}";
        p1.examples = new ArrayList<>();
        Problem.TestCase ex1 = new Problem.TestCase();
        ex1.input = "nums = [2,7,11,15], target = 9";
        ex1.output = "[0,1]";
        p1.examples.add(ex1);
        
        p1.tests = new ArrayList<>();
        Problem.TestCase t1 = new Problem.TestCase();
        t1.input = "nums = [3,2,4], target = 6";
        t1.output = "[1,2]";
        p1.tests.add(t1);
        problemCache.put(p1.id, p1);
    }
}
