package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.logic.repo.BuiltInProblemRepository;
import com.crabmods.algocraft.logic.repo.LocalProblemRepository;
import com.crabmods.algocraft.logic.repo.OfficialRepository;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.logic.repo.RepositoryManager;
import com.crabmods.algocraft.logic.repo.RepositoryMetadata;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class ProblemManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<ProblemRepository> repositories = new ArrayList<>();
    private static final Map<String, Problem> problemCache = new ConcurrentHashMap<>();
    private static OfficialRepository officialRepository;
    private static boolean initialized = false;
    private static boolean initialSyncComplete = false;

    public static void init() {
        if (initialized) return;
        
        LOGGER.info("Initializing ProblemManager...");
        
        ProgressManager.init();
        RepositoryManager.init();
        
        // 1. Official Repository (synced from GitHub) - Highest priority
        officialRepository = RepositoryManager.getOfficialRepository();
        repositories.add(officialRepository);
        
        // 2. Built-in Repository (fallback)
        repositories.add(new BuiltInProblemRepository(Minecraft.getInstance().getResourceManager()));
        
        // 3. Local Repository (User created)
        File localDir = new File(Minecraft.getInstance().gameDirectory, "algorithm_challenges/user");
        if (!localDir.exists()) localDir.mkdirs();
        repositories.add(new LocalProblemRepository("User", localDir.toPath(), "user"));
        
        // 4. Dev Environment Official Repo (only in dev)
        File devOfficialDir = new File("../question_bank/official");
        if (devOfficialDir.exists()) {
            LOGGER.info("Dev environment detected, loading local question bank");
            repositories.add(new LocalProblemRepository("Official (Dev)", devOfficialDir.toPath(), "dev"));
        }

        // 5. Custom Downloaded Repositories
        for (RepositoryMetadata meta : RepositoryManager.getRepositories()) {
            File repoDir = RepositoryManager.getRepositoryDir(meta);
            repositories.add(new LocalProblemRepository(meta.name, repoDir.toPath(), meta.name.toLowerCase()));
        }
        
        // Sort by priority (higher first)
        repositories.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        initialized = true;
        
        // Start async refresh
        refreshAllAsync().thenRun(() -> {
            initialSyncComplete = true;
            LOGGER.info("Initial sync complete. Total problems: {}", problemCache.size());
        });
    }
    
    /**
     * Check if the initial sync is complete.
     */
    public static boolean isInitialSyncComplete() {
        return initialSyncComplete;
    }
    
    /**
     * Get the official repository for status checking.
     */
    public static OfficialRepository getOfficialRepository() {
        return officialRepository;
    }
    
    /**
     * Force refresh the official repository from remote.
     */
    public static CompletableFuture<Void> forceRefreshOfficial() {
        if (officialRepository == null) return CompletableFuture.completedFuture(null);
        return officialRepository.forceRefresh().thenRun(() -> {
            rebuildCache();
            LOGGER.info("Force refresh complete. Total problems: {}", problemCache.size());
        });
    }
    
    /**
     * Async refresh all repositories.
     */
    public static CompletableFuture<Void> refreshAllAsync() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ProblemRepository repo : repositories) {
            futures.add(repo.refresh().exceptionally(e -> {
                LOGGER.error("Failed to refresh repository: {}", repo.getName(), e);
                return null;
            }));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(ProblemManager::rebuildCache);
    }
    
    /**
     * Rebuild the problem cache from all repositories.
     */
    private static void rebuildCache() {
        problemCache.clear();
        for (ProblemRepository repo : repositories) {
            for (Problem p : repo.getProblems()) {
                // First repository to add a problem wins (respects priority)
                problemCache.putIfAbsent(p.id, p);
            }
        }
        
        // Add hardcoded fallback if empty (for testing)
        if (problemCache.isEmpty()) {
            addFallbackProblems();
        }
        
        LOGGER.debug("Problem cache rebuilt with {} problems", problemCache.size());
    }

    /**
     * Synchronously refresh all repositories and rebuild cache.
     * Blocks until complete. Use refreshAllAsync() for non-blocking refresh.
     */
    public static void refreshAll() {
        refreshAllAsync().join();
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

    public static List<ProblemRepository> getRepositories() {
        if (!initialized) init();
        return new ArrayList<>(repositories);
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
