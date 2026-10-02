package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.logic.repo.ProblemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemManagerCacheTest {
    private static final Path PROBLEM_MANAGER = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "logic", "ProblemManager.java"
    );

    @AfterEach
    void tearDown() {
        ProblemManager.resetForTest();
    }

    @Test
    void cacheSnapshotPreservesRepositoryPriorityAndStableOrder() {
        Problem officialOne = problem("1", "Official One");
        Problem officialThree = problem("3", "Official Three");
        Problem userOne = problem("1", "User One");
        Problem userTwo = problem("2", "User Two");

        Map<String, Problem> snapshot = ProblemCacheSnapshot.build(List.of(
                new FakeRepository("Official", 100, List.of(officialOne, officialThree)),
                new FakeRepository("User", 50, List.of(userOne, userTwo))
        ));

        assertEquals(List.of("1", "3", "2"), new ArrayList<>(snapshot.keySet()),
                "cache iteration order should match the visible repository priority order");
        assertSame(officialOne, snapshot.get("1"),
                "higher-priority official problem should win duplicate ids");
        assertSame(officialThree, snapshot.get("3"));
        assertSame(userTwo, snapshot.get("2"));
    }

    @Test
    void cacheSnapshotIsImmutableAndIndependentOfLaterRepositoryMutation() {
        List<Problem> mutableProblems = new ArrayList<>();
        Problem first = problem("1", "First");
        mutableProblems.add(first);
        FakeRepository repository = new FakeRepository("Mutable", 50, mutableProblems);

        Map<String, Problem> snapshot = ProblemCacheSnapshot.build(List.of(repository));
        mutableProblems.add(problem("2", "Second"));

        assertEquals(List.of("1"), new ArrayList<>(snapshot.keySet()),
                "published cache snapshots should not observe later repository list mutations");
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put("3", problem("3", "Third")),
                "published cache snapshots should be immutable");
    }

    @Test
    void problemManagerPublishesWholeCacheSnapshotsInsteadOfClearingLiveCache() throws IOException {
        String source = Files.readString(PROBLEM_MANAGER, StandardCharsets.UTF_8);

        assertTrue(source.contains("AtomicReference<Map<String, Problem>> problemCache"),
                "ProblemManager should publish the cache by replacing an immutable snapshot");
        assertTrue(source.contains("problemCache.set(rebuilt)"),
                "rebuildCache should atomically publish a fully-built cache");
        assertTrue(source.contains("ProblemCacheSnapshot.build"),
                "cache construction should stay isolated from ProblemManager's FML dependencies");
        assertFalse(source.contains("problemCache.clear()"),
                "rebuildCache must not expose an empty live cache during refresh");
        assertFalse(source.contains("problemCache.putIfAbsent"),
                "rebuildCache must not expose a partially-populated live cache during refresh");
        assertFalse(source.contains("problemCache.put("),
                "ad hoc repository additions must not bypass priority-aware snapshot rebuilds");
    }

    @Test
    void firstProblemAccessBuildsOfflineCacheBeforeReturningToTheIde() throws IOException {
        String source = Files.readString(PROBLEM_MANAGER, StandardCharsets.UTF_8);

        assertTrue(source.contains("The initial load is cache-only and must finish before callers render a problem list"),
                "ProblemManager should document why first IDE access cannot publish an empty problem list");
        assertTrue(source.contains("Network updates remain explicit through forceRefreshOfficial()"),
                "ProblemManager startup must remain offline-safe while doing the synchronous cache load");
        assertTrue(source.contains("refreshAll();\n        initialSyncComplete = true;"),
                "ProblemManager.init should build the offline cache before getProblems can return to the IDE");
        assertFalse(source.contains("refreshAllAsync().thenRun"),
                "ProblemManager.init must not defer the first visible problem cache rebuild asynchronously");
    }

    @Test
    void problemManagerDoesNotUseLegacyBuiltInProblemFallback() throws IOException {
        Path builtInRepository = Paths.get(
                System.getProperty("user.dir"),
                "src", "main", "java", "com", "crabmods", "algocraft", "logic", "repo",
                "BuiltInProblemRepository.java"
        );
        String source = Files.readString(PROBLEM_MANAGER, StandardCharsets.UTF_8);

        assertFalse(Files.exists(builtInRepository),
                "legacy built-in problem resources should not remain as a silent fallback question bank");
        assertFalse(source.contains("BuiltInProblemRepository"),
                "ProblemManager should not mix legacy built-in resources into the visible question bank");
        assertTrue(source.contains("RepositoryManager.getOfficialRepository()"),
                "official problems should come from the explicit remote-backed official cache");
        assertTrue(source.contains("RepositoryManager.getRepositories()"),
                "custom repositories should enter through RepositoryManager metadata, not a resource fallback");
    }

    @Test
    void forceRefreshOfficialFailsClosedWhenOfficialRepositoryIsMissing() {
        CompletionException missingManager = assertThrows(
                CompletionException.class,
                () -> ProblemManager.forceRefreshOfficial().join()
        );
        assertTrue(missingManager.getCause().getMessage().contains("ProblemManager is not initialized"),
                "Update Official must not report success before ProblemManager is initialized");

        ProblemManager.replaceRepositoriesForTest(List.of());

        CompletionException missingOfficialRepository = assertThrows(
                CompletionException.class,
                () -> ProblemManager.forceRefreshOfficial().join()
        );
        assertTrue(missingOfficialRepository.getCause().getMessage().contains("Official repository is not initialized"),
                "Update Official must not report success when the official repository is unavailable");
    }

    @Test
    void addRepositoryFutureCompletesOnlyAfterRefreshPublishesCache() {
        ProblemManager.replaceRepositoriesForTest(List.of());
        ControlledRepository repository = new ControlledRepository("Async Course", 50);
        Problem asyncProblem = problem("99", "Async Problem");

        CompletableFuture<Void> refresh = ProblemManager.addRepository(repository);

        assertFalse(refresh.isDone(),
                "addRepository should return the refresh future instead of completing before cache publication");
        assertEquals(List.of(), ProblemManager.getRepositories().stream()
                        .map(ProblemRepository::getName)
                        .toList(),
                "a new repository must not be selectable before its first refresh is validated");
        assertEquals(List.of(), ProblemManager.getProblems(),
                "a pending repository refresh must not be visible as a successful cache update");

        repository.replaceProblems(List.of(asyncProblem));
        repository.completeRefresh();
        refresh.join();

        assertEquals(List.of("Async Course"), ProblemManager.getRepositories().stream()
                        .map(ProblemRepository::getName)
                        .toList(),
                "a new repository should become selectable only after its first refresh succeeds");
        assertSame(asyncProblem, ProblemManager.getProblem("99"),
                "the returned future should complete after the refreshed repository is visible in the cache");
    }

    @Test
    void addRepositoryPropagatesRefreshFailureWithoutPublishingPartialCache() {
        Problem existingProblem = problem("1", "Stable Problem");
        ProblemManager.replaceRepositoriesForTest(List.of(
                new FakeRepository("Stable", 50, List.of(existingProblem))
        ));
        ControlledRepository brokenRepository = new ControlledRepository("Broken Course", 50);
        brokenRepository.replaceProblems(List.of(problem("2", "Should Not Publish")));

        CompletableFuture<Void> refresh = ProblemManager.addRepository(brokenRepository);
        assertEquals(List.of("Stable"), ProblemManager.getRepositories().stream()
                        .map(ProblemRepository::getName)
                        .toList(),
                "a pending first refresh must not publish the new repository into IDE lists");
        brokenRepository.failRefresh(new IllegalStateException("simulated refresh failure"));

        RuntimeException failure = assertThrows(RuntimeException.class, refresh::join,
                "addRepository callers must be able to report repository refresh failures");
        assertTrue(failure.getMessage().contains("simulated refresh failure")
                        || (failure.getCause() != null
                        && failure.getCause().getMessage().contains("simulated refresh failure")),
                "the failed future should preserve the repository refresh error");
        assertSame(existingProblem, ProblemManager.getProblem("1"));
        assertFalse(ProblemManager.getProblems().stream().anyMatch(problem -> problem.getId().equals("2")),
                "a failed refresh must not publish problems from the broken repository");
        assertEquals(List.of("Stable"), ProblemManager.getRepositories().stream()
                        .map(ProblemRepository::getName)
                        .toList(),
                "a failed addRepository refresh must not leave a broken repository visible in IDE lists");
    }

    @Test
    void addRepositoryKeepsRepositoryEntryHiddenWhenRefreshFailsBeforeFutureCompletion() {
        Problem existingProblem = problem("1", "Stable Problem");
        ProblemManager.replaceRepositoriesForTest(List.of(
                new FakeRepository("Stable", 50, List.of(existingProblem))
        ));
        ControlledRepository brokenRepository = new ControlledRepository("Broken Course", 60);
        brokenRepository.replaceProblems(List.of(problem("2", "Should Not Publish")));

        CompletableFuture<Void> refresh = ProblemManager.addRepository(brokenRepository);
        assertEquals(List.of("Stable"), ProblemManager.getRepositories().stream()
                        .map(ProblemRepository::getName)
                        .toList(),
                "a pending addRepository refresh must not expose an unvalidated repository");

        brokenRepository.failRefresh(new IllegalStateException("simulated async refresh failure"));

        CompletionException failure = assertThrows(CompletionException.class, refresh::join,
                "addRepository should surface the refresh failure");
        assertTrue(failure.getCause().getMessage().contains("simulated async refresh failure"),
                "the failed future should preserve the repository refresh error");
        assertEquals(List.of("Stable"), ProblemManager.getRepositories().stream()
                        .map(ProblemRepository::getName)
                        .toList(),
                "a repository whose first refresh failed must stay out of the visible repository list");
        assertSame(existingProblem, ProblemManager.getProblem("1"));
        assertFalse(ProblemManager.getProblems().stream().anyMatch(problem -> problem.getId().equals("2")),
                "failed new repositories must not publish partial problems");
    }

    @Test
    void refreshAllAsyncPropagatesFailuresWithoutPublishingPartialCache() {
        Problem existingProblem = problem("1", "Stable Problem");
        ControlledRepository stableRepository = new ControlledRepository("Stable Course", 50);
        stableRepository.replaceProblems(List.of(existingProblem));
        ControlledRepository brokenRepository = new ControlledRepository("Broken Course", 40);
        ProblemManager.replaceRepositoriesForTest(List.of(stableRepository, brokenRepository));

        CompletableFuture<Void> refresh = ProblemManager.refreshAllAsync();

        brokenRepository.replaceProblems(List.of(problem("2", "Should Not Publish")));
        brokenRepository.failRefresh(new IllegalStateException("simulated refresh failure"));
        assertFalse(refresh.isDone(),
                "refreshAllAsync should wait for every repository before reporting the aggregate result");

        stableRepository.completeRefresh();
        CompletionException failure = assertThrows(CompletionException.class, refresh::join,
                "refreshAllAsync callers must be able to report repository refresh failures");
        assertTrue(failure.getCause().getMessage().contains("Broken Course"),
                "the failed future should identify the repository that failed");
        assertTrue(failure.getCause().getSuppressed()[0].getCause().getMessage()
                        .contains("simulated refresh failure"),
                "the failed future should preserve the repository refresh error");
        assertSame(existingProblem, ProblemManager.getProblem("1"));
        assertFalse(ProblemManager.getProblems().stream().anyMatch(problem -> problem.getId().equals("2")),
                "a failed aggregate refresh must not publish problems from the broken repository");
    }

    private static Problem problem(String id, String title) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setTitle(title);
        problem.setDescription("Problem " + title);
        problem.setDifficulty("EASY");
        problem.setInitialCode("class Solution { public int solve() { return 1; } }");
        return problem;
    }

    private static final class FakeRepository implements ProblemRepository {
        private final String name;
        private final int priority;
        private final List<Problem> problems;

        private FakeRepository(String name, int priority, List<Problem> problems) {
            this.name = name;
            this.priority = priority;
            this.problems = problems;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public List<Problem> getProblems() {
            return new ArrayList<>(problems);
        }

        @Override
        public CompletableFuture<Void> refresh() {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ControlledRepository implements ProblemRepository {
        private final String name;
        private final int priority;
        private final CompletableFuture<Void> refreshFuture = new CompletableFuture<>();
        private final List<Problem> problems = new ArrayList<>();

        private ControlledRepository(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        private void replaceProblems(List<Problem> replacement) {
            problems.clear();
            problems.addAll(replacement);
        }

        private void completeRefresh() {
            refreshFuture.complete(null);
        }

        private void failRefresh(Throwable error) {
            refreshFuture.completeExceptionally(error);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public List<Problem> getProblems() {
            return new ArrayList<>(problems);
        }

        @Override
        public CompletableFuture<Void> refresh() {
            return refreshFuture;
        }
    }
}
