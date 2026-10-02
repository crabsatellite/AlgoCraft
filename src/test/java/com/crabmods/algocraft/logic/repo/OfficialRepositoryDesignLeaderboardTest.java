package com.crabmods.algocraft.logic.repo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class OfficialRepositoryDesignLeaderboardTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentyNinthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignLeaderboardProblemPreservesCache(
                "Remote p429 missing reviewed solution section",
                designLeaderboardProblemWithInvalidFirstSolution(
                        "Remote p429 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsDesignLeaderboardWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedDesignLeaderboardProblemPreservesCache(
                "Remote p429 weak tests",
                designLeaderboardProblemWithWeakTests("Remote p429 weak tests"),
                "Design Leaderboard tests must cover");
    }

    @Test
    void forceRefreshRejectsDesignLeaderboardWithoutTreeMapCountsWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignLeaderboardProblemPreservesCache(
                "Remote p429 missing TreeMap score-count route",
                designLeaderboardProblemWithoutSolution(
                        "Remote p429 missing TreeMap score-count route",
                        "HashMap and TreeMap Score Counts"),
                "TreeMap score-count route");
    }

    @Test
    void forceRefreshRejectsDesignLeaderboardWithoutSortOnQueryWithoutReplacingCache() throws IOException {
        assertRejectedDesignLeaderboardProblemPreservesCache(
                "Remote p429 missing sort-on-query route",
                designLeaderboardProblemWithoutSolution(
                        "Remote p429 missing sort-on-query route",
                        "HashMap with Sort on Query"),
                "sort-on-query route");
    }

    @Test
    void forceRefreshRejectsDesignLeaderboardWithoutMinHeapTopKWithoutReplacingCache() throws IOException {
        assertRejectedDesignLeaderboardProblemPreservesCache(
                "Remote p429 missing min-heap Top-K route",
                designLeaderboardProblemWithoutSolution(
                        "Remote p429 missing min-heap Top-K route",
                        "HashMap with Min-Heap Top K"),
                "min-heap Top-K route");
    }

    @Test
    void forceRefreshRejectsDesignLeaderboardWithoutBoundedBucketsWithoutReplacingCache() throws IOException {
        assertRejectedDesignLeaderboardProblemPreservesCache(
                "Remote p429 missing bounded score-bucket route",
                designLeaderboardProblemWithoutSolution(
                        "Remote p429 missing bounded score-bucket route",
                        "Bounded Score Bucket Counts"),
                "bounded score-bucket route");
    }

    @Test
    void forceRefreshRejectsDesignLeaderboardWithoutTreeSetRecordsWithoutReplacingCache() throws IOException {
        assertRejectedDesignLeaderboardProblemPreservesCache(
                "Remote p429 missing TreeSet player-record route",
                designLeaderboardProblemWithoutSolution(
                        "Remote p429 missing TreeSet player-record route",
                        "TreeSet of Player Score Records"),
                "TreeSet player-record route");
    }

    private void assertRejectedDesignLeaderboardProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design a Leaderboard", repository.getProblems().get(428).getTitle());

        remote.publishProblem(429, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p429.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p429 Design Leaderboard content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design a Leaderboard", repository.getProblems().get(428).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p429.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design a Leaderboard"));
        assertFalse(cachedProblem.contains(remoteTitle));
        assertNoTemporaryRepositoryDirs();
    }

    private void assertNoTemporaryRepositoryDirs() throws IOException {
        Path normalizedCacheDir = cacheDir.toAbsolutePath().normalize();
        Path parent = normalizedCacheDir.getParent();
        String cacheName = normalizedCacheDir.getFileName().toString();

        try (var stream = Files.list(parent)) {
            List<Path> leftovers = stream
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(cacheName + ".update-") || name.startsWith(cacheName + ".backup-");
                    })
                    .toList();
            assertTrue(leftovers.isEmpty(), "repository update should not leave staging or backup directories: "
                    + leftovers);
        }
    }

    private static String designLeaderboardProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validDesignLeaderboardProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a no-op leaderboard that never stores player scores, while still being long "
                        + "enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String designLeaderboardProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validDesignLeaderboardProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[[],[1,40],[2,40],[3,10]")) {
                editedTests.add(testCase(
                        "[\"Leaderboard\",\"addScore\",\"top\"]\n[[],[1,5],[1]]",
                        "[null,null,5]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P429 baseline test set no longer contains the equal-score case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String designLeaderboardProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validDesignLeaderboardProblem();
        problem.addProperty("title", title);
        JsonArray editedSolutions = new JsonArray();
        boolean removed = false;
        for (JsonElement element : problem.getAsJsonArray("solutions")) {
            JsonObject solution = element.getAsJsonObject();
            if (missingSolutionName.equals(solution.get("name").getAsString())) {
                removed = true;
            } else {
                editedSolutions.add(element.deepCopy());
            }
        }
        if (!removed) {
            throw new IOException("P429 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validDesignLeaderboardProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p429.json");
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject().deepCopy();
    }

    private static JsonObject testCase(String input, String output) {
        JsonObject test = new JsonObject();
        test.addProperty("input", input);
        test.addProperty("output", output);
        return test;
    }

    private static JsonObject fallbackSolution() {
        JsonObject solution = new JsonObject();
        solution.addProperty("name", "Design Leaderboard Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a no-op leaderboard that never stores player scores.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Design Leaderboard "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Leaderboard { "
                + "public Leaderboard() {} "
                + "public void addScore(int playerId, int score) {} "
                + "public int top(int K) { return 0; } "
                + "public void reset(int playerId) {} "
                + "}";
    }
}
