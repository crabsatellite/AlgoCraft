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

class OfficialRepositoryMaximumNumberOfPairsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumNumberOfPairsProblemPreservesCache(
                "Remote p423 missing reviewed solution section",
                maximumNumberOfPairsProblemWithInvalidFirstSolution(
                        "Remote p423 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMaximumNumberOfPairsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMaximumNumberOfPairsProblemPreservesCache(
                "Remote p423 weak tests",
                maximumNumberOfPairsProblemWithWeakTests("Remote p423 weak tests"),
                "Maximum Number of Pairs tests must cover");
    }

    @Test
    void forceRefreshRejectsMaximumNumberOfPairsWithoutBoundedFrequencyArrayWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumNumberOfPairsProblemPreservesCache(
                "Remote p423 missing bounded frequency-array route",
                maximumNumberOfPairsProblemWithoutSolution("Remote p423 missing bounded frequency-array route",
                        "Bounded Frequency Array"),
                "bounded frequency-array route");
    }

    @Test
    void forceRefreshRejectsMaximumNumberOfPairsWithoutHashMapFrequencyCountsWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumNumberOfPairsProblemPreservesCache(
                "Remote p423 missing HashMap frequency-count route",
                maximumNumberOfPairsProblemWithoutSolution("Remote p423 missing HashMap frequency-count route",
                        "HashMap Frequency Counts"),
                "HashMap frequency-count route");
    }

    @Test
    void forceRefreshRejectsMaximumNumberOfPairsWithoutToggleSetWithoutReplacingCache() throws IOException {
        assertRejectedMaximumNumberOfPairsProblemPreservesCache(
                "Remote p423 missing toggle-set route",
                maximumNumberOfPairsProblemWithoutSolution("Remote p423 missing toggle-set route",
                        "Toggle Set of Waiting Singles"),
                "toggle-set route");
    }

    @Test
    void forceRefreshRejectsMaximumNumberOfPairsWithoutSortAdjacentValuesWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumNumberOfPairsProblemPreservesCache(
                "Remote p423 missing sort-adjacent-values route",
                maximumNumberOfPairsProblemWithoutSolution("Remote p423 missing sort-adjacent-values route",
                        "Sort Adjacent Equal Values"),
                "sort-adjacent-values route");
    }

    @Test
    void forceRefreshRejectsMaximumNumberOfPairsWithoutBoundedParityTableWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumNumberOfPairsProblemPreservesCache(
                "Remote p423 missing bounded parity-table route",
                maximumNumberOfPairsProblemWithoutSolution("Remote p423 missing bounded parity-table route",
                        "Bounded Parity Table"),
                "bounded parity-table route");
    }

    private void assertRejectedMaximumNumberOfPairsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Number of Pairs in Array", repository.getProblems().get(422).getTitle());

        remote.publishProblem(423, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p423.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p423 Maximum Number of Pairs content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Number of Pairs in Array", repository.getProblems().get(422).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p423.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Maximum Number of Pairs in Array"));
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

    private static String maximumNumberOfPairsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMaximumNumberOfPairsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the number of unused elements without counting equal-value pairs while still "
                        + "being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Solution { public int[] numberOfPairs(int[] nums) { return new int[]{0, nums.length}; } }");
        return GSON.toJson(problem);
    }

    private static String maximumNumberOfPairsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMaximumNumberOfPairsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[0,0,100,100,100,1]")) {
                editedTests.add(testCase("nums = [2]", "[0,1]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P423 baseline test set no longer contains the value-range boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String maximumNumberOfPairsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMaximumNumberOfPairsProblem();
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
            throw new IOException("P423 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMaximumNumberOfPairsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p423.json");
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
        solution.addProperty("name", "Maximum Pairs Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder result without grouping equal values into pairs.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Maximum Number of Pairs "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Solution { public int[] numberOfPairs(int[] nums) { return new int[]{0, nums.length}; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
