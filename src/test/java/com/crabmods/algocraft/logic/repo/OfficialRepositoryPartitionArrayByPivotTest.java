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

class OfficialRepositoryPartitionArrayByPivotTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentyFifthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedPartitionArrayByPivotProblemPreservesCache(
                "Remote p425 missing reviewed solution section",
                partitionArrayByPivotProblemWithInvalidFirstSolution(
                        "Remote p425 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsPartitionArrayByPivotWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedPartitionArrayByPivotProblemPreservesCache(
                "Remote p425 weak tests",
                partitionArrayByPivotProblemWithWeakTests("Remote p425 weak tests"),
                "Partition Array by Pivot tests must cover");
    }

    @Test
    void forceRefreshRejectsPartitionArrayByPivotWithoutThreePassStableFillWithoutReplacingCache()
            throws IOException {
        assertRejectedPartitionArrayByPivotProblemPreservesCache(
                "Remote p425 missing three-pass stable-fill route",
                partitionArrayByPivotProblemWithoutSolution(
                        "Remote p425 missing three-pass stable-fill route", "Three Pass Stable Fill"),
                "three-pass stable-fill route");
    }

    @Test
    void forceRefreshRejectsPartitionArrayByPivotWithoutThreeBucketsWithoutReplacingCache() throws IOException {
        assertRejectedPartitionArrayByPivotProblemPreservesCache(
                "Remote p425 missing three-bucket route",
                partitionArrayByPivotProblemWithoutSolution(
                        "Remote p425 missing three-bucket route", "Three Buckets"),
                "three-bucket route");
    }

    @Test
    void forceRefreshRejectsPartitionArrayByPivotWithoutPrecountedStableIndexWithoutReplacingCache()
            throws IOException {
        assertRejectedPartitionArrayByPivotProblemPreservesCache(
                "Remote p425 missing pre-counted stable-index route",
                partitionArrayByPivotProblemWithoutSolution(
                        "Remote p425 missing pre-counted stable-index route", "Pre-counted Stable Index Fill"),
                "pre-counted stable-index route");
    }

    @Test
    void forceRefreshRejectsPartitionArrayByPivotWithoutOppositeEndStableFillWithoutReplacingCache()
            throws IOException {
        assertRejectedPartitionArrayByPivotProblemPreservesCache(
                "Remote p425 missing opposite-end stable-fill route",
                partitionArrayByPivotProblemWithoutSolution(
                        "Remote p425 missing opposite-end stable-fill route", "Opposite-End Stable Fill"),
                "opposite-end stable-fill route");
    }

    @Test
    void forceRefreshRejectsPartitionArrayByPivotWithoutCategoryIndexSortWithoutReplacingCache()
            throws IOException {
        assertRejectedPartitionArrayByPivotProblemPreservesCache(
                "Remote p425 missing category-and-original-index sort route",
                partitionArrayByPivotProblemWithoutSolution(
                        "Remote p425 missing category-and-original-index sort route",
                        "Category and Original Index Sort"),
                "category-and-original-index sort route");
    }

    private void assertRejectedPartitionArrayByPivotProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Partition Array by Pivot", repository.getProblems().get(424).getTitle());

        remote.publishProblem(425, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p425.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p425 Partition Array by Pivot content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Partition Array by Pivot", repository.getProblems().get(424).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p425.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Partition Array by Pivot"));
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

    private static String partitionArrayByPivotProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validPartitionArrayByPivotProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the input without building the three required stable regions, while still being "
                        + "long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Solution { public int[] pivotArray(int[] nums, int pivot) { return nums; } }");
        return GSON.toJson(problem);
    }

    private static String partitionArrayByPivotProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validPartitionArrayByPivotProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[7,6,8]")) {
                editedTests.add(testCase("nums = [1,2], pivot = 2", "[1,2]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P425 baseline test set no longer contains the all-greater case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String partitionArrayByPivotProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validPartitionArrayByPivotProblem();
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
            throw new IOException("P425 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validPartitionArrayByPivotProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p425.json");
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
        solution.addProperty("name", "Pivot Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the original array without producing the required partitioned regions.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Partition Array by Pivot "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Solution { public int[] pivotArray(int[] nums, int pivot) { return nums; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
