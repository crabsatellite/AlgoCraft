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

class OfficialRepositoryArrayPartitionTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtySixthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedArrayPartitionProblemPreservesCache(
                "Remote p466 missing reviewed solution section",
                arrayPartitionProblemWithInvalidFirstSolution("Remote p466 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsArrayPartitionWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedArrayPartitionProblemPreservesCache(
                "Remote p466 weak tests",
                arrayPartitionProblemWithWeakTests("Remote p466 weak tests"),
                "Array Partition tests must cover");
    }

    @Test
    void forceRefreshRejectsArrayPartitionWithoutLibrarySortRouteWithoutReplacingCache() throws IOException {
        assertRejectedArrayPartitionProblemPreservesCache(
                "Remote p466 missing library sort route",
                arrayPartitionProblemWithoutSolution(
                        "Remote p466 missing library sort route", "Library Sort Adjacent Greedy"),
                "library-sort adjacent greedy route");
    }

    @Test
    void forceRefreshRejectsArrayPartitionWithoutCountingRouteWithoutReplacingCache() throws IOException {
        assertRejectedArrayPartitionProblemPreservesCache(
                "Remote p466 missing counting route",
                arrayPartitionProblemWithoutSolution(
                        "Remote p466 missing counting route", "Bounded Counting Sweep"),
                "bounded counting sweep route");
    }

    @Test
    void forceRefreshRejectsArrayPartitionWithoutHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedArrayPartitionProblemPreservesCache(
                "Remote p466 missing heap route",
                arrayPartitionProblemWithoutSolution(
                        "Remote p466 missing heap route", "Min-Heap Adjacent Pairing"),
                "min-heap adjacent pairing route");
    }

    @Test
    void forceRefreshRejectsArrayPartitionWithoutMergeSortRouteWithoutReplacingCache() throws IOException {
        assertRejectedArrayPartitionProblemPreservesCache(
                "Remote p466 missing merge-sort route",
                arrayPartitionProblemWithoutSolution(
                        "Remote p466 missing merge-sort route", "Manual Merge Sort Then Pair"),
                "manual merge-sort pairing route");
    }

    private void assertRejectedArrayPartitionProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Array Partition", repository.getProblems().get(465).getTitle());

        remote.publishProblem(466, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p466.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p466 array partition content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Array Partition", repository.getProblems().get(465).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p466.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Array Partition"));
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

    private static String arrayPartitionProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validArrayPartitionProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without sorting, counting, heap pairing, or merge sorting, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String arrayPartitionProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validArrayPartitionProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "nums = [-10,-9,-8,-7,7,8,9,10]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [1,4,3,2]", "4"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P466 baseline test set no longer contains the mixed-sign long case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String arrayPartitionProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validArrayPartitionProblem();
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
            throw new IOException("P466 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validArrayPartitionProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p466.json");
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
        solution.addProperty("name", "Array Partition Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for the array pairing greedy proof.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int arrayPairSum(int[] nums) { return 0; } }";
    }
}
