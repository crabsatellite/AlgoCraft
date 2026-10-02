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

class OfficialRepositoryOriginalArrayFromDoubledValuesTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtySecondReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedOriginalArrayProblemPreservesCache(
                "Remote p432 missing reviewed solution section",
                originalArrayProblemWithInvalidFirstSolution(
                        "Remote p432 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsOriginalArrayWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedOriginalArrayProblemPreservesCache(
                "Remote p432 weak tests",
                originalArrayProblemWithWeakTests("Remote p432 weak tests"),
                "Original Array From Doubled Values tests must cover");
    }

    @Test
    void forceRefreshRejectsOriginalArrayWithoutSortedCountMapWithoutReplacingCache() throws IOException {
        assertRejectedOriginalArrayProblemPreservesCache(
                "Remote p432 missing sorted count-map route",
                originalArrayProblemWithoutSolution(
                        "Remote p432 missing sorted count-map route",
                        "Sorted Count Map"),
                "sorted count-map matching route");
    }

    @Test
    void forceRefreshRejectsOriginalArrayWithoutCountingArrayWithoutReplacingCache() throws IOException {
        assertRejectedOriginalArrayProblemPreservesCache(
                "Remote p432 missing counting-array route",
                originalArrayProblemWithoutSolution(
                        "Remote p432 missing counting-array route",
                        "Counting Array by Value Range"),
                "bounded counting-array route");
    }

    @Test
    void forceRefreshRejectsOriginalArrayWithoutDemandQueueWithoutReplacingCache() throws IOException {
        assertRejectedOriginalArrayProblemPreservesCache(
                "Remote p432 missing demand-queue route",
                originalArrayProblemWithoutSolution(
                        "Remote p432 missing demand-queue route",
                        "Demand Queue on Sorted Values"),
                "sorted demand-queue route");
    }

    @Test
    void forceRefreshRejectsOriginalArrayWithoutTreeMapFrequencyWithoutReplacingCache() throws IOException {
        assertRejectedOriginalArrayProblemPreservesCache(
                "Remote p432 missing TreeMap route",
                originalArrayProblemWithoutSolution(
                        "Remote p432 missing TreeMap route",
                        "TreeMap Frequency Greedy"),
                "TreeMap frequency-greedy route");
    }

    @Test
    void forceRefreshRejectsOriginalArrayWithoutUsedIndexBaselineWithoutReplacingCache() throws IOException {
        assertRejectedOriginalArrayProblemPreservesCache(
                "Remote p432 missing used-index route",
                originalArrayProblemWithoutSolution(
                        "Remote p432 missing used-index route",
                        "Sorted Used-Index Baseline"),
                "used-index baseline route");
    }

    private void assertRejectedOriginalArrayProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Original Array From Doubled Values", repository.getProblems().get(431).getTitle());

        remote.publishProblem(432, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p432.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p432 Original Array From Doubled Values content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Original Array From Doubled Values", repository.getProblems().get(431).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p432.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Original Array From Doubled Values"));
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

    private static String originalArrayProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validOriginalArrayProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty array without checking doubled values, while still being long enough for "
                        + "the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String originalArrayProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validOriginalArrayProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("changed = [1,2,2,4,4,8]")) {
                editedTests.add(testCase("changed = [2,4]", "[2]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P432 baseline test set no longer contains the chain-doubles case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String originalArrayProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validOriginalArrayProblem();
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
            throw new IOException("P432 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validOriginalArrayProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p432.json");
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
        solution.addProperty("name", "Original Array Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty array without matching values to doubled values.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Original Array From Doubled Values "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { "
                + "public int[] findOriginalArray(int[] changed) { return new int[0]; } "
                + "}";
    }
}
