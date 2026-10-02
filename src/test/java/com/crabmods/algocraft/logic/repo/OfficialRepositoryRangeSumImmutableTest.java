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

class OfficialRepositoryRangeSumImmutableTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEighteenthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedRangeSumProblemPreservesCache(
                "Remote p418 missing reviewed solution section",
                rangeSumProblemWithInvalidFirstSolution("Remote p418 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsRangeSumImmutableWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumProblemPreservesCache(
                "Remote p418 weak tests",
                rangeSumProblemWithWeakTests("Remote p418 weak tests"),
                "Range Sum Query - Immutable tests must cover");
    }

    @Test
    void forceRefreshRejectsRangeSumImmutableWithoutPrefixSumWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumProblemPreservesCache(
                "Remote p418 missing prefix-sum route",
                rangeSumProblemWithoutSolution("Remote p418 missing prefix-sum route", "Prefix Sum Array"),
                "prefix-sum route");
    }

    @Test
    void forceRefreshRejectsRangeSumImmutableWithoutFenwickWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumProblemPreservesCache(
                "Remote p418 missing Fenwick tree route",
                rangeSumProblemWithoutSolution("Remote p418 missing Fenwick tree route", "Fenwick Tree Without Updates"),
                "Fenwick tree route");
    }

    @Test
    void forceRefreshRejectsRangeSumImmutableWithoutSegmentTreeWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumProblemPreservesCache(
                "Remote p418 missing static segment-tree route",
                rangeSumProblemWithoutSolution("Remote p418 missing static segment-tree route", "Static Segment Tree"),
                "static segment-tree route");
    }

    @Test
    void forceRefreshRejectsRangeSumImmutableWithoutBlockDecompositionWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumProblemPreservesCache(
                "Remote p418 missing block-decomposition route",
                rangeSumProblemWithoutSolution("Remote p418 missing block-decomposition route", "Block Decomposition"),
                "block-decomposition route");
    }

    @Test
    void forceRefreshRejectsRangeSumImmutableWithoutDirectScanWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumProblemPreservesCache(
                "Remote p418 missing direct-scan route",
                rangeSumProblemWithoutSolution("Remote p418 missing direct-scan route", "Direct Scan Baseline"),
                "direct-scan baseline route");
    }

    private void assertRejectedRangeSumProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Range Sum Query - Immutable", repository.getProblems().get(417).getTitle());

        remote.publishProblem(418, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p418.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p418 Range Sum content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Range Sum Query - Immutable", repository.getProblems().get(417).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p418.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Range Sum Query - Immutable"));
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

    private static String rangeSumProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validRangeSumProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a deliberately invalid placeholder that returns zero for every range while still "
                        + "being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class NumArray { public NumArray(int[] nums) {} "
                        + "public int sumRange(int left, int right) { return 0; } }");
        return GSON.toJson(problem);
    }

    private static String rangeSumProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validRangeSumProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[[[100000,100000")) {
                editedTests.add(testCase(
                        "[\"NumArray\",\"sumRange\"]\n[[[1]],[0,0]]",
                        "[null,1]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P418 baseline test set no longer contains the large-value coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String rangeSumProblemWithoutSolution(String title, String missingSolutionName) throws IOException {
        JsonObject problem = validRangeSumProblem();
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
            throw new IOException("P418 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validRangeSumProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p418.json");
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
        solution.addProperty("name", "Range Sum Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder value without storing the input array or reading the requested range.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Range Sum Query teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class NumArray { public NumArray(int[] nums) {} "
                        + "public int sumRange(int left, int right) { return 0; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
