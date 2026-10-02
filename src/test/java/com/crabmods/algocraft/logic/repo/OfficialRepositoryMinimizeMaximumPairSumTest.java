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

class OfficialRepositoryMinimizeMaximumPairSumTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedPairSumProblemPreservesCache(
                "Remote p483 missing reviewed solution section",
                pairSumProblemWithInvalidFirstSolution(
                        "Remote p483 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsPairSumWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedPairSumProblemPreservesCache(
                "Remote p483 weak tests",
                pairSumProblemWithWeakTests("Remote p483 weak tests"),
                "Minimize Maximum Pair Sum tests must cover");
    }

    @Test
    void forceRefreshRejectsPairSumWithoutSortRouteWithoutReplacingCache() throws IOException {
        assertRejectedPairSumProblemPreservesCache(
                "Remote p483 missing sort route",
                pairSumProblemWithoutSolution(
                        "Remote p483 missing sort route", "Sort Opposite Ends"),
                "sort opposite-ends greedy route");
    }

    @Test
    void forceRefreshRejectsPairSumWithoutBinarySearchRouteWithoutReplacingCache() throws IOException {
        assertRejectedPairSumProblemPreservesCache(
                "Remote p483 missing binary-search route",
                pairSumProblemWithoutSolution(
                        "Remote p483 missing binary-search route", "Binary Search on Maximum Pair Sum"),
                "answer-space binary-search route");
    }

    @Test
    void forceRefreshRejectsPairSumWithoutTreeMapRouteWithoutReplacingCache() throws IOException {
        assertRejectedPairSumProblemPreservesCache(
                "Remote p483 missing TreeMap route",
                pairSumProblemWithoutSolution(
                        "Remote p483 missing TreeMap route", "TreeMap Multiset Pairing"),
                "TreeMap ordered-multiset route");
    }

    private void assertRejectedPairSumProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimize Maximum Pair Sum in Array", repository.getProblems().get(482).getTitle());

        remote.publishProblem(483, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p483.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p483 pair-sum content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimize Maximum Pair Sum in Array", repository.getProblems().get(482).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p483.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimize Maximum Pair Sum in Array"));
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

    private static String pairSumProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validPairSumProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant without forming every pair or proving the minimax exchange argument.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String pairSumProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validPairSumProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "nums = [1000000000,1,1000000000,1]"
                    .equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [2,2]", "4"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P483 baseline test set no longer contains the upper-bound case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String pairSumProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validPairSumProblem();
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
            throw new IOException("P483 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validPairSumProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p483.json");
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
        solution.addProperty("name", "Pair Sum Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for minimizing the maximum pair sum.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int minPairSum(int[] nums) { return 0; } }";
    }
}
