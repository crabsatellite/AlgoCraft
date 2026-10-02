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

class OfficialRepositoryLongestUnequalAdjacentGroupsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
                "Remote p424 missing reviewed solution section",
                longestUnequalAdjacentGroupsProblemWithInvalidFirstSolution(
                        "Remote p424 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsLongestUnequalAdjacentGroupsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
                "Remote p424 weak tests",
                longestUnequalAdjacentGroupsProblemWithWeakTests("Remote p424 weak tests"),
                "Longest Unequal Adjacent Groups tests must cover");
    }

    @Test
    void forceRefreshRejectsLongestUnequalAdjacentGroupsWithoutRunBoundaryGreedyWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
                "Remote p424 missing run-boundary greedy route",
                longestUnequalAdjacentGroupsProblemWithoutSolution(
                        "Remote p424 missing run-boundary greedy route", "Run Boundary Greedy"),
                "run-boundary greedy route");
    }

    @Test
    void forceRefreshRejectsLongestUnequalAdjacentGroupsWithoutRunCompressionWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
                "Remote p424 missing explicit run-compression route",
                longestUnequalAdjacentGroupsProblemWithoutSolution(
                        "Remote p424 missing explicit run-compression route", "Explicit Run Compression"),
                "explicit run-compression route");
    }

    @Test
    void forceRefreshRejectsLongestUnequalAdjacentGroupsWithoutTwoStateDpWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
                "Remote p424 missing two-state DP route",
                longestUnequalAdjacentGroupsProblemWithoutSolution(
                        "Remote p424 missing two-state DP route", "Two-State Dynamic Programming"),
                "two-state DP route");
    }

    @Test
    void forceRefreshRejectsLongestUnequalAdjacentGroupsWithoutQuadraticSubsequenceDpWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
                "Remote p424 missing quadratic subsequence-DP route",
                longestUnequalAdjacentGroupsProblemWithoutSolution(
                        "Remote p424 missing quadratic subsequence-DP route", "Quadratic Subsequence DP"),
                "quadratic subsequence-DP route");
    }

    @Test
    void forceRefreshRejectsLongestUnequalAdjacentGroupsWithoutRecursiveMemoizedChoiceWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
                "Remote p424 missing recursive memoized-choice route",
                longestUnequalAdjacentGroupsProblemWithoutSolution(
                        "Remote p424 missing recursive memoized-choice route", "Recursive Memoized Choice"),
                "recursive memoized-choice route");
    }

    private void assertRejectedLongestUnequalAdjacentGroupsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Longest Unequal Adjacent Groups Subsequence", repository.getProblems().get(423).getTitle());

        remote.publishProblem(424, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p424.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p424 Longest Unequal Adjacent Groups content before cache "
                        + "replacement; actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Longest Unequal Adjacent Groups Subsequence", repository.getProblems().get(423).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p424.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Longest Unequal Adjacent Groups Subsequence"));
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

    private static String longestUnequalAdjacentGroupsProblemWithInvalidFirstSolution(String title)
            throws IOException {
        JsonObject problem = validLongestUnequalAdjacentGroupsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the input length without checking whether neighboring chosen group values differ, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Solution { public int longestAlternatingGroups(int[] groups) { return groups.length; } }");
        return GSON.toJson(problem);
    }

    private static String longestUnequalAdjacentGroupsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validLongestUnequalAdjacentGroupsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[0,0,0,1,1,1]")) {
                editedTests.add(testCase("groups = [1,0]", "2"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P424 baseline test set no longer contains the two-long-runs case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String longestUnequalAdjacentGroupsProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validLongestUnequalAdjacentGroupsProblem();
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
            throw new IOException("P424 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validLongestUnequalAdjacentGroupsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p424.json");
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
        solution.addProperty("name", "Longest Unequal Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder length without enforcing adjacent group inequality.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Longest Unequal Adjacent Groups "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Solution { public int longestAlternatingGroups(int[] groups) { return groups.length; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
