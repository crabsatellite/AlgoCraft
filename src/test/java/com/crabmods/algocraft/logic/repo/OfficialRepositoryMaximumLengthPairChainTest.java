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

class OfficialRepositoryMaximumLengthPairChainTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightySecondReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedPairChainProblemPreservesCache(
                "Remote p482 missing reviewed solution section",
                pairChainProblemWithInvalidFirstSolution(
                        "Remote p482 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsPairChainWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedPairChainProblemPreservesCache(
                "Remote p482 weak tests",
                pairChainProblemWithWeakTests("Remote p482 weak tests"),
                "Maximum Length of Pair Chain tests must cover");
    }

    @Test
    void forceRefreshRejectsPairChainWithoutGreedyRouteWithoutReplacingCache() throws IOException {
        assertRejectedPairChainProblemPreservesCache(
                "Remote p482 missing greedy route",
                pairChainProblemWithoutSolution(
                        "Remote p482 missing greedy route", "Greedy Sort by End"),
                "greedy sort-by-end route");
    }

    @Test
    void forceRefreshRejectsPairChainWithoutDpRouteWithoutReplacingCache() throws IOException {
        assertRejectedPairChainProblemPreservesCache(
                "Remote p482 missing DP route",
                pairChainProblemWithoutSolution(
                        "Remote p482 missing DP route", "Dynamic Programming by Start"),
                "quadratic dynamic-programming route");
    }

    @Test
    void forceRefreshRejectsPairChainWithoutBinarySearchRouteWithoutReplacingCache() throws IOException {
        assertRejectedPairChainProblemPreservesCache(
                "Remote p482 missing binary-search route",
                pairChainProblemWithoutSolution(
                        "Remote p482 missing binary-search route", "Binary Search Tail Ends"),
                "binary-search tail-end route");
    }

    private void assertRejectedPairChainProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Length of Pair Chain", repository.getProblems().get(481).getTitle());

        remote.publishProblem(482, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p482.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p482 pair-chain content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Length of Pair Chain", repository.getProblems().get(481).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p482.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Maximum Length of Pair Chain"));
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

    private static String pairChainProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validPairChainProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant chain length without checking strict pair compatibility, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String pairChainProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validPairChainProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "pairs = [[1,2],[2,3],[3,4],[4,5]]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("pairs = [[1,2],[2,3],[3,4]]", "2"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P482 baseline test set no longer contains the strict-adjacency case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String pairChainProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validPairChainProblem();
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
            throw new IOException("P482 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validPairChainProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p482.json");
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
        solution.addProperty("name", "Pair Chain Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for strict pair-chain selection.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int findLongestChain(int[][] pairs) { return 1; } }";
    }
}
