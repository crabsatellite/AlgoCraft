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

class OfficialRepositoryKthSmallestMultiplicationTableTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredNinetyFirstReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedKthSmallestMultiplicationTableProblemPreservesCache(
                "Remote p491 missing reviewed solution section",
                kthSmallestMultiplicationTableProblemWithInvalidFirstSolution(
                        "Remote p491 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsKthSmallestMultiplicationTableWeakTestsWithoutReplacingCache()
            throws IOException {
        assertRejectedKthSmallestMultiplicationTableProblemPreservesCache(
                "Remote p491 weak tests",
                kthSmallestMultiplicationTableProblemWithWeakTests("Remote p491 weak tests"),
                "Multiplication Table tests must cover rank-counting boundaries");
    }

    @Test
    void forceRefreshRejectsKthSmallestMultiplicationTableWithoutSmallerDimensionRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedKthSmallestMultiplicationTableProblemPreservesCache(
                "Remote p491 missing smaller dimension count",
                kthSmallestMultiplicationTableProblemWithoutSolution(
                        "Remote p491 missing smaller dimension count",
                        "Binary Search with Smaller-Dimension Count"),
                "smaller-dimension row-count route");
    }

    @Test
    void forceRefreshRejectsKthSmallestMultiplicationTableWithoutEarlyStoppingRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedKthSmallestMultiplicationTableProblemPreservesCache(
                "Remote p491 missing early stopping",
                kthSmallestMultiplicationTableProblemWithoutSolution(
                        "Remote p491 missing early stopping",
                        "Early-Stopping Feasibility Predicate"),
                "early-stopping feasibility route");
    }

    @Test
    void forceRefreshRejectsKthSmallestMultiplicationTableWithoutStaircaseRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedKthSmallestMultiplicationTableProblemPreservesCache(
                "Remote p491 missing staircase count",
                kthSmallestMultiplicationTableProblemWithoutSolution(
                        "Remote p491 missing staircase count",
                        "Staircase Count Binary Search"),
                "staircase counting route");
    }

    @Test
    void forceRefreshRejectsKthSmallestMultiplicationTableWithoutQuotientGroupedRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedKthSmallestMultiplicationTableProblemPreservesCache(
                "Remote p491 missing quotient grouping",
                kthSmallestMultiplicationTableProblemWithoutSolution(
                        "Remote p491 missing quotient grouping",
                        "Quotient-Grouped Count Binary Search"),
                "quotient-grouped counting route");
    }

    private void assertRejectedKthSmallestMultiplicationTableProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Kth Smallest Number in Multiplication Table", repository.getProblems().get(490).getTitle());

        remote.publishProblem(491, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p491.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p491 multiplication-table content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Kth Smallest Number in Multiplication Table", repository.getProblems().get(490).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p491.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Kth Smallest Number in Multiplication Table"));
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

    private static String kthSmallestMultiplicationTableProblemWithInvalidFirstSolution(String title)
            throws IOException {
        JsonObject problem = validKthSmallestMultiplicationTableProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return one without explaining answer-space rank counting.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String kthSmallestMultiplicationTableProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validKthSmallestMultiplicationTableProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "m = 30000, n = 30000, k = 900000000".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("m = 3, n = 3, k = 5", "3"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P491 baseline test set no longer contains the max-rank case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String kthSmallestMultiplicationTableProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validKthSmallestMultiplicationTableProblem();
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
            throw new IOException("P491 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validKthSmallestMultiplicationTableProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p491.json");
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
        solution.addProperty("name", "Multiplication Table Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return one without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores duplicates, rank counting, and value binary search.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int findKthNumber(int m, int n, int k) { return 1; } }";
    }
}
