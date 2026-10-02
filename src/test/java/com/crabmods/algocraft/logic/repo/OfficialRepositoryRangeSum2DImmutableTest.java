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

class OfficialRepositoryRangeSum2DImmutableTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredNineteenthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedRangeSum2DProblemPreservesCache(
                "Remote p419 missing reviewed solution section",
                rangeSum2DProblemWithInvalidFirstSolution("Remote p419 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsRangeSum2DImmutableWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedRangeSum2DProblemPreservesCache(
                "Remote p419 weak tests",
                rangeSum2DProblemWithWeakTests("Remote p419 weak tests"),
                "Range Sum Query 2D - Immutable tests must cover");
    }

    @Test
    void forceRefreshRejectsRangeSum2DImmutableWithout2DPrefixWithoutReplacingCache() throws IOException {
        assertRejectedRangeSum2DProblemPreservesCache(
                "Remote p419 missing 2D prefix route",
                rangeSum2DProblemWithoutSolution("Remote p419 missing 2D prefix route", "2D Prefix Sum"),
                "2D prefix-sum route");
    }

    @Test
    void forceRefreshRejectsRangeSum2DImmutableWithoutRowWisePrefixWithoutReplacingCache() throws IOException {
        assertRejectedRangeSum2DProblemPreservesCache(
                "Remote p419 missing row-wise prefix route",
                rangeSum2DProblemWithoutSolution("Remote p419 missing row-wise prefix route", "Row-wise Prefix Sum"),
                "row-wise prefix-sum route");
    }

    @Test
    void forceRefreshRejectsRangeSum2DImmutableWithoutColumnWisePrefixWithoutReplacingCache() throws IOException {
        assertRejectedRangeSum2DProblemPreservesCache(
                "Remote p419 missing column-wise prefix route",
                rangeSum2DProblemWithoutSolution(
                        "Remote p419 missing column-wise prefix route",
                        "Column-wise Prefix Sum"),
                "column-wise prefix-sum route");
    }

    @Test
    void forceRefreshRejectsRangeSum2DImmutableWithoutFenwickWithoutReplacingCache() throws IOException {
        assertRejectedRangeSum2DProblemPreservesCache(
                "Remote p419 missing 2D Fenwick route",
                rangeSum2DProblemWithoutSolution("Remote p419 missing 2D Fenwick route", "Static 2D Fenwick Tree"),
                "static 2D Fenwick tree route");
    }

    @Test
    void forceRefreshRejectsRangeSum2DImmutableWithoutDirectScanWithoutReplacingCache() throws IOException {
        assertRejectedRangeSum2DProblemPreservesCache(
                "Remote p419 missing direct region scan route",
                rangeSum2DProblemWithoutSolution(
                        "Remote p419 missing direct region scan route",
                        "Direct Region Scan Baseline"),
                "direct region scan baseline route");
    }

    private void assertRejectedRangeSum2DProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Range Sum Query 2D - Immutable", repository.getProblems().get(418).getTitle());

        remote.publishProblem(419, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p419.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p419 Range Sum 2D content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Range Sum Query 2D - Immutable", repository.getProblems().get(418).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p419.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Range Sum Query 2D - Immutable"));
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

    private static String rangeSum2DProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validRangeSum2DProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a deliberately invalid placeholder that returns zero for every rectangle while "
                        + "still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class NumMatrix { public NumMatrix(int[][] matrix) {} "
                        + "public int sumRegion(int row1, int col1, int row2, int col2) { return 0; } }");
        return GSON.toJson(problem);
    }

    private static String rangeSum2DProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validRangeSum2DProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[[[[100000,100000")) {
                editedTests.add(testCase(
                        "[\"NumMatrix\",\"sumRegion\"]\n[[[[1]]],[0,0,0,0]]",
                        "[null,1]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P419 baseline test set no longer contains the large-value coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String rangeSum2DProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validRangeSum2DProblem();
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
            throw new IOException("P419 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validRangeSum2DProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p419.json");
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
        solution.addProperty("name", "Range Sum 2D Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder value without storing the matrix or reading the requested region.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Range Sum Query 2D teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class NumMatrix { public NumMatrix(int[][] matrix) {} "
                        + "public int sumRegion(int row1, int col1, int row2, int col2) { return 0; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
