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

class OfficialRepositoryMaximumAverageSubarrayITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightiethReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumAverageProblemPreservesCache(
                "Remote p480 missing reviewed solution section",
                maximumAverageProblemWithInvalidFirstSolution(
                        "Remote p480 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMaximumAverageWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMaximumAverageProblemPreservesCache(
                "Remote p480 weak tests",
                maximumAverageProblemWithWeakTests("Remote p480 weak tests"),
                "Maximum Average Subarray I tests must cover");
    }

    @Test
    void forceRefreshRejectsMaximumAverageWithoutSlidingWindowRouteWithoutReplacingCache() throws IOException {
        assertRejectedMaximumAverageProblemPreservesCache(
                "Remote p480 missing sliding route",
                maximumAverageProblemWithoutSolution(
                        "Remote p480 missing sliding route", "Sliding Window Running Sum"),
                "sliding-window running-sum route");
    }

    @Test
    void forceRefreshRejectsMaximumAverageWithoutPrefixSumRouteWithoutReplacingCache() throws IOException {
        assertRejectedMaximumAverageProblemPreservesCache(
                "Remote p480 missing prefix route",
                maximumAverageProblemWithoutSolution(
                        "Remote p480 missing prefix route", "Prefix Sum Window Queries"),
                "prefix-sum window-query route");
    }

    @Test
    void forceRefreshRejectsMaximumAverageWithoutBinarySearchRouteWithoutReplacingCache() throws IOException {
        assertRejectedMaximumAverageProblemPreservesCache(
                "Remote p480 missing binary-search route",
                maximumAverageProblemWithoutSolution(
                        "Remote p480 missing binary-search route", "Binary Search on the Average"),
                "answer-space binary-search route");
    }

    private void assertRejectedMaximumAverageProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Average Subarray I", repository.getProblems().get(479).getTitle());

        remote.publishProblem(480, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p480.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p480 maximum-average content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Average Subarray I", repository.getProblems().get(479).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p480.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Maximum Average Subarray I"));
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

    private static String maximumAverageProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMaximumAverageProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant number without maintaining any fixed-length window, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String maximumAverageProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMaximumAverageProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "nums = [7,4,5,8,8,3,9,8,7,6], k = 6".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [5], k = 1", "5.00000"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P480 baseline test set no longer contains the fractional-precision case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String maximumAverageProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validMaximumAverageProblem();
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
            throw new IOException("P480 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMaximumAverageProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p480.json");
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
        solution.addProperty("name", "Maximum Average Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for fixed-length maximum averages.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public double findMaxAverage(int[] nums, int k) { return 0.0; } }";
    }
}
