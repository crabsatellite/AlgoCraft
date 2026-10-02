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

class OfficialRepositorySubarrayProductTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftySecondReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedSubarrayProductProblemPreservesCache(
                "Remote p452 missing reviewed solution section",
                subarrayProductProblemWithInvalidFirstSolution("Remote p452 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsSubarrayProductWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedSubarrayProductProblemPreservesCache(
                "Remote p452 weak tests",
                subarrayProductProblemWithWeakTests("Remote p452 weak tests"),
                "Subarray Product tests must cover");
    }

    @Test
    void forceRefreshRejectsSubarrayProductWithoutSlidingWindowRouteWithoutReplacingCache() throws IOException {
        assertRejectedSubarrayProductProblemPreservesCache(
                "Remote p452 missing sliding-window route",
                subarrayProductProblemWithoutSolution("Remote p452 missing sliding-window route",
                        "Sliding Window Product"),
                "sliding-window product route");
    }

    @Test
    void forceRefreshRejectsSubarrayProductWithoutResetAwareRouteWithoutReplacingCache() throws IOException {
        assertRejectedSubarrayProductProblemPreservesCache(
                "Remote p452 missing reset-aware route",
                subarrayProductProblemWithoutSolution("Remote p452 missing reset-aware route",
                        "Reset-Aware Sliding Window"),
                "reset-aware sliding-window route");
    }

    @Test
    void forceRefreshRejectsSubarrayProductWithoutPrefixLogRouteWithoutReplacingCache() throws IOException {
        assertRejectedSubarrayProductProblemPreservesCache(
                "Remote p452 missing prefix-log route",
                subarrayProductProblemWithoutSolution("Remote p452 missing prefix-log route",
                        "Prefix Log Binary Search"),
                "prefix-log binary-search route");
    }

    @Test
    void forceRefreshRejectsSubarrayProductWithoutBruteForceRouteWithoutReplacingCache() throws IOException {
        assertRejectedSubarrayProductProblemPreservesCache(
                "Remote p452 missing brute-force route",
                subarrayProductProblemWithoutSolution("Remote p452 missing brute-force route",
                        "Brute Force With Early Break"),
                "brute-force early-break route");
    }

    private void assertRejectedSubarrayProductProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Subarray Product Less Than K", repository.getProblems().get(451).getTitle());

        remote.publishProblem(452, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p452.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p452 subarray-product content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Subarray Product Less Than K", repository.getProblems().get(451).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p452.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Subarray Product Less Than K"));
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

    private static String subarrayProductProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validSubarrayProductProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero for every input and ignore positive product windows, while still being long "
                        + "enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String subarrayProductProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validSubarrayProductProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("nums = [100], k = 100")) {
                editedTests.add(testCase("nums = [10,5,2,6], k = 100", "8"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P452 baseline test set no longer contains the equality-boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String subarrayProductProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validSubarrayProductProblem();
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
            throw new IOException("P452 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validSubarrayProductProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p452.json");
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
        solution.addProperty("name", "Subarray Product Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero and do not inspect any subarray product.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required subarray-product teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int numSubarrayProductLessThanK(int[] nums, int k) { return 0; } }";
    }
}
