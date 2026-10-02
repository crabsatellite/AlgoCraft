package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.logic.repo.LocalProblemRepository;
import com.crabmods.algocraft.logic.repo.OfficialRepository;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.logic.repo.RepositoryManager;
import com.crabmods.algocraft.logic.repo.RepositoryMetadata;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

public class ProblemManager {
    private static final System.Logger LOGGER = System.getLogger(ProblemManager.class.getName());
    private static final Object repositoriesLock = new Object();
    private static final List<ProblemRepository> repositories = new ArrayList<>();
    private static final AtomicReference<Map<String, Problem>> problemCache = new AtomicReference<>(Map.of());
    private static OfficialRepository officialRepository;
    private static boolean initialized = false;
    private static boolean initialSyncComplete = false;

    public static void init() {
        if (initialized) return;

        LOGGER.log(System.Logger.Level.INFO, "Initializing ProblemManager...");

        ProgressManager.init();
        RepositoryManager.init();

        // 1. Official Repository (synced from GitHub) - Highest priority
        officialRepository = RepositoryManager.getOfficialRepository();

        // 2. Local Repository (User created)
        File localDir = FMLPaths.GAMEDIR.get().resolve("algorithm_challenges/user").toFile();
        if (!localDir.exists()) localDir.mkdirs();

        synchronized (repositoriesLock) {
            repositories.add(officialRepository);
            repositories.add(new LocalProblemRepository("User", localDir.toPath(), "user"));

            // 3. Custom Downloaded Repositories
            for (RepositoryMetadata meta : RepositoryManager.getRepositories()) {
                File repoDir = RepositoryManager.getRepositoryDir(meta);
                repositories.add(new LocalProblemRepository(meta.name, repoDir.toPath()));
            }
            sortRepositoriesByPriority();
        }

        initialized = true;

        // The initial load is cache-only and must finish before callers render a problem list.
        // Network updates remain explicit through forceRefreshOfficial().
        refreshAll();
        initialSyncComplete = true;
        LOGGER.log(System.Logger.Level.INFO,
                "Initial sync complete. Total problems: " + problemCache.get().size());
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
        if (!initialized) {
            return CompletableFuture.failedFuture(new IllegalStateException("ProblemManager is not initialized"));
        }
        OfficialRepository repository = officialRepository;
        if (repository == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Official repository is not initialized"));
        }
        return repository.forceRefresh().thenRun(() -> {
            rebuildCache();
            LOGGER.log(System.Logger.Level.INFO,
                    "Force refresh complete. Total problems: " + problemCache.get().size());
        });
    }

    /**
     * Async refresh all repositories.
     */
    public static CompletableFuture<Void> refreshAllAsync() {
        List<ProblemRepository> repositorySnapshot = repositorySnapshot();
        List<RepositoryRefreshFailure> failures = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ProblemRepository repo : repositorySnapshot) {
            CompletableFuture<Void> refreshFuture;
            try {
                refreshFuture = repo.refresh();
            } catch (Throwable e) {
                recordRefreshFailure(repo, e, failures);
                continue;
            }
            if (refreshFuture == null) {
                recordRefreshFailure(repo,
                        new IllegalStateException("Repository refresh returned null future"),
                        failures);
                continue;
            }
            futures.add(refreshFuture.handle((ignored, e) -> {
                if (e != null) {
                    recordRefreshFailure(repo, e, failures);
                }
                return null;
            }));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                if (!failures.isEmpty()) {
                    throw new CompletionException(combineRefreshFailures(failures));
                }
                rebuildCache(repositorySnapshot);
            });
    }

    /**
     * Rebuild the problem cache from all repositories.
     */
    private static void rebuildCache() {
        rebuildCache(repositorySnapshot());
    }

    private static void rebuildCache(List<ProblemRepository> repositorySnapshot) {
        Map<String, Problem> rebuilt = ProblemCacheSnapshot.build(repositorySnapshot);
        problemCache.set(rebuilt);
        LOGGER.log(System.Logger.Level.DEBUG, "Problem cache rebuilt with " + rebuilt.size() + " problems");
    }

    /**
     * Synchronously refresh all repositories and rebuild cache.
     * Blocks until complete. Use refreshAllAsync() for non-blocking refresh.
     */
    public static void refreshAll() {
        refreshAllAsync().join();
    }

    public static CompletableFuture<Void> downloadAndUpdateRepository(String name, String url) {
        return RepositoryManager.installOrUpdateRepository(name, url).thenCompose(meta -> {
            File repoDir = RepositoryManager.getRepositoryDir(meta);
            ProblemRepository repositoryToRefresh = null;
            boolean addedRepository = false;
            for (ProblemRepository repo : repositorySnapshot()) {
                if (repo instanceof LocalProblemRepository && repo.getName().equals(meta.name)) {
                    repositoryToRefresh = repo;
                    break;
                }
            }

            if (repositoryToRefresh == null) {
                repositoryToRefresh = new LocalProblemRepository(meta.name, repoDir.toPath());
                addedRepository = true;
            }

            return addedRepository
                    ? refreshNewRepositoryAndPublish(repositoryToRefresh)
                    : refreshRepositoryAndRebuildCache(repositoryToRefresh);
        });
    }

    public static CompletableFuture<Void> addRepository(ProblemRepository repo) {
        return refreshNewRepositoryAndPublish(repo);
    }

    private static CompletableFuture<Void> refreshNewRepositoryAndPublish(ProblemRepository repo) {
        return refreshRepository(repo).thenRun(() -> {
            synchronized (repositoriesLock) {
                repositories.add(repo);
                sortRepositoriesByPriority();
            }
            rebuildCache();
        });
    }

    private static CompletableFuture<Void> refreshRepositoryAndRebuildCache(ProblemRepository repo) {
        return refreshRepository(repo).thenRun(ProblemManager::rebuildCache);
    }

    private static CompletableFuture<Void> refreshRepository(ProblemRepository repo) {
        CompletableFuture<Void> refreshFuture;
        try {
            refreshFuture = repo.refresh();
        } catch (Throwable error) {
            return CompletableFuture.failedFuture(error);
        }
        if (refreshFuture == null) {
            IllegalStateException error = new IllegalStateException("Repository refresh returned null future");
            return CompletableFuture.failedFuture(error);
        }
        return refreshFuture;
    }

    static void replaceRepositoriesForTest(List<ProblemRepository> testRepositories) {
        synchronized (repositoriesLock) {
            repositories.clear();
            repositories.addAll(testRepositories != null ? testRepositories : List.of());
            sortRepositoriesByPriority();
        }
        officialRepository = null;
        initialized = true;
        initialSyncComplete = true;
        rebuildCache();
    }

    static void resetForTest() {
        synchronized (repositoriesLock) {
            repositories.clear();
        }
        problemCache.set(Map.of());
        officialRepository = null;
        initialized = false;
        initialSyncComplete = false;
    }

    public static List<ProblemRepository> getRepositories() {
        if (!initialized) init();
        return repositorySnapshot();
    }

    public static List<Problem> getProblems() {
        if (!initialized) init();
        return new ArrayList<>(problemCache.get().values());
    }

    public static Problem getProblem(String id) {
        if (!initialized) init();
        return problemCache.get().get(id);
    }

    private static List<ProblemRepository> repositorySnapshot() {
        synchronized (repositoriesLock) {
            return new ArrayList<>(repositories);
        }
    }

    private static void sortRepositoriesByPriority() {
        repositories.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    private static void recordRefreshFailure(ProblemRepository repo, Throwable error,
                                             List<RepositoryRefreshFailure> failures) {
        RepositoryRefreshFailure failure = new RepositoryRefreshFailure(repo.getName(), unwrapCompletion(error));
        failures.add(failure);
        LOGGER.log(System.Logger.Level.ERROR, failure.getMessage(), failure.getCause());
    }

    private static IllegalStateException combineRefreshFailures(List<RepositoryRefreshFailure> failures) {
        List<RepositoryRefreshFailure> snapshot;
        synchronized (failures) {
            snapshot = new ArrayList<>(failures);
        }

        StringBuilder names = new StringBuilder();
        for (RepositoryRefreshFailure failure : snapshot) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(failure.repositoryName());
        }

        IllegalStateException combined = new IllegalStateException(
                "Failed to refresh " + snapshot.size() + " problem repositories: " + names);
        for (RepositoryRefreshFailure failure : snapshot) {
            combined.addSuppressed(failure);
        }
        return combined;
    }

    private static Throwable unwrapCompletion(Throwable error) {
        if (error instanceof CompletionException completionException
                && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return error;
    }

    private static final class RepositoryRefreshFailure extends RuntimeException {
        private final String repositoryName;

        private RepositoryRefreshFailure(String repositoryName, Throwable cause) {
            super("Failed to refresh repository '" + repositoryName + "': " + cause.getMessage(), cause);
            this.repositoryName = repositoryName;
        }

        private String repositoryName() {
            return repositoryName;
        }
    }
}
