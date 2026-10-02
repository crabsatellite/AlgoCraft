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

class OfficialRepositoryCountPairsBelowTargetTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtyFirstReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedCountPairsBelowTargetProblemPreservesCache(
                "Remote p431 missing reviewed solution section",
                countPairsBelowTargetProblemWithInvalidFirstSolution(
                        "Remote p431 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsCountPairsBelowTargetWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedCountPairsBelowTargetProblemPreservesCache(
                "Remote p431 weak tests",
                countPairsBelowTargetProblemWithWeakTests("Remote p431 weak tests"),
                "Count Pairs Below Target tests must cover");
    }

    @Test
    void forceRefreshRejectsCountPairsBelowTargetWithoutSortedTwoPointersWithoutReplacingCache()
            throws IOException {
        assertRejectedCountPairsBelowTargetProblemPreservesCache(
                "Remote p431 missing sorted two-pointer route",
                countPairsBelowTargetProblemWithoutSolution(
                        "Remote p431 missing sorted two-pointer route",
                        "Sorted Two Pointers"),
                "sorted two-pointer batch-count route");
    }

    @Test
    void forceRefreshRejectsCountPairsBelowTargetWithoutSortedBinarySearchWithoutReplacingCache()
            throws IOException {
        assertRejectedCountPairsBelowTargetProblemPreservesCache(
                "Remote p431 missing sorted binary-search route",
                countPairsBelowTargetProblemWithoutSolution(
                        "Remote p431 missing sorted binary-search route",
                        "Sorted Binary Search Counting"),
                "sorted binary-search boundary route");
    }

    @Test
    void forceRefreshRejectsCountPairsBelowTargetWithoutFenwickWithoutReplacingCache() throws IOException {
        assertRejectedCountPairsBelowTargetProblemPreservesCache(
                "Remote p431 missing Fenwick route",
                countPairsBelowTargetProblemWithoutSolution(
                        "Remote p431 missing Fenwick route",
                        "Online Fenwick Compression"),
                "online Fenwick compression route");
    }

    @Test
    void forceRefreshRejectsCountPairsBelowTargetWithoutMergeSortWithoutReplacingCache() throws IOException {
        assertRejectedCountPairsBelowTargetProblemPreservesCache(
                "Remote p431 missing merge-sort route",
                countPairsBelowTargetProblemWithoutSolution(
                        "Remote p431 missing merge-sort route",
                        "Merge Sort Pair Counting"),
                "merge-sort pair-counting route");
    }

    @Test
    void forceRefreshRejectsCountPairsBelowTargetWithoutBruteForceWithoutReplacingCache() throws IOException {
        assertRejectedCountPairsBelowTargetProblemPreservesCache(
                "Remote p431 missing brute-force route",
                countPairsBelowTargetProblemWithoutSolution(
                        "Remote p431 missing brute-force route",
                        "Brute Force Pairs"),
                "brute-force pair baseline route");
    }

    private void assertRejectedCountPairsBelowTargetProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Pairs Below Target", repository.getProblems().get(430).getTitle());

        remote.publishProblem(431, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p431.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p431 Count Pairs Below Target content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Pairs Below Target", repository.getProblems().get(430).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p431.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Count Pairs Below Target"));
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

    private static String countPairsBelowTargetProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validCountPairsBelowTargetProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant answer instead of explaining any real pair-counting invariant, while "
                        + "still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String countPairsBelowTargetProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validCountPairsBelowTargetProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("nums = [1,1,2,2], target = 3")) {
                editedTests.add(testCase("nums = [2], target = 9", "0"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P431 baseline test set no longer contains the strict-inequality duplicate case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String countPairsBelowTargetProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validCountPairsBelowTargetProblem();
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
            throw new IOException("P431 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validCountPairsBelowTargetProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p431.json");
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
        solution.addProperty("name", "Count Pairs Below Target Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant answer without using the pair-counting condition.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Count Pairs Below Target "
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
                + "public int countPairs(java.util.List<Integer> nums, int target) { return 0; } "
                + "}";
    }
}
