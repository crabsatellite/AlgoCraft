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

class OfficialRepositoryRangeSumMutableTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentiethReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedRangeSumMutableProblemPreservesCache(
                "Remote p420 missing reviewed solution section",
                rangeSumMutableProblemWithInvalidFirstSolution("Remote p420 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsRangeSumMutableWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumMutableProblemPreservesCache(
                "Remote p420 weak tests",
                rangeSumMutableProblemWithWeakTests("Remote p420 weak tests"),
                "Range Sum Query - Mutable tests must cover");
    }

    @Test
    void forceRefreshRejectsRangeSumMutableWithoutFenwickWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumMutableProblemPreservesCache(
                "Remote p420 missing Fenwick tree route",
                rangeSumMutableProblemWithoutSolution("Remote p420 missing Fenwick tree route",
                        "Fenwick Tree with Stored Values"),
                "Fenwick tree route");
    }

    @Test
    void forceRefreshRejectsRangeSumMutableWithoutRecursiveSegmentTreeWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumMutableProblemPreservesCache(
                "Remote p420 missing recursive segment-tree route",
                rangeSumMutableProblemWithoutSolution("Remote p420 missing recursive segment-tree route",
                        "Recursive Segment Tree"),
                "recursive segment-tree route");
    }

    @Test
    void forceRefreshRejectsRangeSumMutableWithoutIterativeSegmentTreeWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumMutableProblemPreservesCache(
                "Remote p420 missing iterative segment-tree route",
                rangeSumMutableProblemWithoutSolution("Remote p420 missing iterative segment-tree route",
                        "Iterative Segment Tree"),
                "iterative segment-tree route");
    }

    @Test
    void forceRefreshRejectsRangeSumMutableWithoutBlockDecompositionWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumMutableProblemPreservesCache(
                "Remote p420 missing mutable block-decomposition route",
                rangeSumMutableProblemWithoutSolution("Remote p420 missing mutable block-decomposition route",
                        "Mutable Block Decomposition"),
                "mutable block-decomposition route");
    }

    @Test
    void forceRefreshRejectsRangeSumMutableWithoutDirectScanWithoutReplacingCache() throws IOException {
        assertRejectedRangeSumMutableProblemPreservesCache(
                "Remote p420 missing direct-scan route",
                rangeSumMutableProblemWithoutSolution("Remote p420 missing direct-scan route",
                        "Direct Scan Baseline"),
                "direct-scan baseline route");
    }

    private void assertRejectedRangeSumMutableProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Range Sum Query - Mutable", repository.getProblems().get(419).getTitle());

        remote.publishProblem(420, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p420.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p420 Range Sum content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Range Sum Query - Mutable", repository.getProblems().get(419).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p420.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Range Sum Query - Mutable"));
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

    private static String rangeSumMutableProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validRangeSumMutableProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a deliberately invalid placeholder that ignores every update and returns zero for "
                        + "every range while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class NumArray { public NumArray(int[] nums) {} public void update(int index, int val) {} "
                        + "public int sumRange(int left, int right) { return 0; } }");
        return GSON.toJson(problem);
    }

    private static String rangeSumMutableProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validRangeSumMutableProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[[[100,-100,100")) {
                editedTests.add(testCase(
                        "[\"NumArray\",\"sumRange\"]\n[[[1]],[0,0]]",
                        "[null,1]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P420 baseline test set no longer contains the larger mixed-value coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String rangeSumMutableProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validRangeSumMutableProblem();
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
            throw new IOException("P420 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validRangeSumMutableProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p420.json");
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
        solution.addProperty("name", "Range Sum Mutable Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder value without storing updates or reading the requested range.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required mutable Range Sum teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class NumArray { public NumArray(int[] nums) {} public void update(int index, int val) {} "
                        + "public int sumRange(int left, int right) { return 0; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
