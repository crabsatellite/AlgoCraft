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

class OfficialRepositoryMinimumFallingPathSumIITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtyNinthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumFallingPathSumIIProblemPreservesCache(
                "Remote p469 missing reviewed solution section",
                minimumFallingPathSumIIProblemWithInvalidFirstSolution(
                        "Remote p469 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMinimumFallingPathSumIIWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumFallingPathSumIIProblemPreservesCache(
                "Remote p469 weak tests",
                minimumFallingPathSumIIProblemWithWeakTests("Remote p469 weak tests"),
                "Minimum Falling Path Sum II tests must cover");
    }

    @Test
    void forceRefreshRejectsMinimumFallingPathSumIIWithoutFullTableRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumFallingPathSumIIProblemPreservesCache(
                "Remote p469 missing full table route",
                minimumFallingPathSumIIProblemWithoutSolution(
                        "Remote p469 missing full table route", "Full Table Two Minimums DP"),
                "full-table two-minimum DP route");
    }

    @Test
    void forceRefreshRejectsMinimumFallingPathSumIIWithoutPrefixSuffixRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumFallingPathSumIIProblemPreservesCache(
                "Remote p469 missing prefix suffix route",
                minimumFallingPathSumIIProblemWithoutSolution(
                        "Remote p469 missing prefix suffix route", "Prefix Suffix Minima DP"),
                "prefix/suffix minima DP route");
    }

    @Test
    void forceRefreshRejectsMinimumFallingPathSumIIWithoutRollingTwoMinimumsRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumFallingPathSumIIProblemPreservesCache(
                "Remote p469 missing rolling route",
                minimumFallingPathSumIIProblemWithoutSolution(
                        "Remote p469 missing rolling route", "Rolling Two Minimums DP"),
                "rolling two-minimum DP route");
    }

    @Test
    void forceRefreshRejectsMinimumFallingPathSumIIWithoutInPlaceRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumFallingPathSumIIProblemPreservesCache(
                "Remote p469 missing in-place route",
                minimumFallingPathSumIIProblemWithoutSolution(
                        "Remote p469 missing in-place route", "In-Place Two Minimums DP"),
                "in-place two-minimum DP route");
    }

    private void assertRejectedMinimumFallingPathSumIIProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Falling Path Sum Without Same Column", repository.getProblems().get(468).getTitle());

        remote.publishProblem(469, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p469.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p469 minimum falling path sum content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Falling Path Sum Without Same Column", repository.getProblems().get(468).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p469.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimum Falling Path Sum Without Same Column"));
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

    private static String minimumFallingPathSumIIProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMinimumFallingPathSumIIProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without enforcing the adjacent-row column restriction, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String minimumFallingPathSumIIProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMinimumFallingPathSumIIProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "grid = [[-99,0,0],[-99,0,0],[-99,0,0]]"
                    .equals(test.get("input").getAsString())) {
                editedTests.add(testCase("grid = [[1,2,3],[4,5,6],[7,8,9]]", "13"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P469 baseline test set no longer contains the repeated-column trap case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String minimumFallingPathSumIIProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMinimumFallingPathSumIIProblem();
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
            throw new IOException("P469 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMinimumFallingPathSumIIProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p469.json");
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
        solution.addProperty("name", "Minimum Falling Path Sum Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant without checking the adjacent-row same-column restriction.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid dynamic-programming route for this problem.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int minFallingPathSum(int[][] grid) { return 0; } }";
    }
}
