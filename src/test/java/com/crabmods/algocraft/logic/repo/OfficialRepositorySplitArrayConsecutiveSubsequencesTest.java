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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialRepositorySplitArrayConsecutiveSubsequencesTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredNinetyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedSplitArrayProblemPreservesCache(
                "Remote p494 missing reviewed solution section",
                splitArrayProblemWithInvalidFirstSolution("Remote p494 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsSplitArrayWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedSplitArrayProblemPreservesCache(
                "Remote p494 weak tests",
                splitArrayProblemWithWeakTests("Remote p494 weak tests"),
                "Split Array tests must cover consecutive-subsequence boundaries");
    }

    @Test
    void forceRefreshRejectsSplitArrayWithoutTwoMapRouteWithoutReplacingCache() throws IOException {
        assertRejectedSplitArrayProblemPreservesCache(
                "Remote p494 missing two-map route",
                splitArrayProblemWithoutSolution(
                        "Remote p494 missing two-map route", "Greedy with Two HashMaps"),
                "two-map greedy route");
    }

    @Test
    void forceRefreshRejectsSplitArrayWithoutGlobalHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedSplitArrayProblemPreservesCache(
                "Remote p494 missing global heap route",
                splitArrayProblemWithoutSolution(
                        "Remote p494 missing global heap route", "Global Min Heap of Active Subsequences"),
                "global active-subsequence heap route");
    }

    @Test
    void forceRefreshRejectsSplitArrayWithoutLengthBucketRouteWithoutReplacingCache() throws IOException {
        assertRejectedSplitArrayProblemPreservesCache(
                "Remote p494 missing length bucket route",
                splitArrayProblemWithoutSolution(
                        "Remote p494 missing length bucket route", "Length Bucket Greedy"),
                "length-bucket greedy route");
    }

    @Test
    void forceRefreshRejectsSplitArrayWithoutTailGroupedHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedSplitArrayProblemPreservesCache(
                "Remote p494 missing tail grouped route",
                splitArrayProblemWithoutSolution(
                        "Remote p494 missing tail grouped route", "Tail-Grouped Shortest Length Heaps"),
                "tail-grouped shortest-length heap route");
    }

    private void assertRejectedSplitArrayProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Split Array into Consecutive Subsequences", repository.getProblems().get(493).getTitle());

        remote.publishProblem(494, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p494.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p494 split-array content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Split Array into Consecutive Subsequences", repository.getProblems().get(493).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p494.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Split Array into Consecutive Subsequences"));
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

    private static String splitArrayProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validSplitArrayProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false without explaining consecutive subsequences.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String splitArrayProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validSplitArrayProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "nums = [1,2,3,4,5,5,6,7]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [1,2,3,4,5]", "true"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P494 baseline test set no longer contains the shortest-extension trap case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String splitArrayProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validSplitArrayProblem();
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
            throw new IOException("P494 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validSplitArrayProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p494.json");
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
        solution.addProperty("name", "Split Array Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false without using any required consecutive-subsequence route.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores sequence length, gaps, and duplicate pressure.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public boolean isPossible(int[] nums) { return false; } }";
    }
}
