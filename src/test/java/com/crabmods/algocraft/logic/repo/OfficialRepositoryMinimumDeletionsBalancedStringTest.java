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

class OfficialRepositoryMinimumDeletionsBalancedStringTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtyFifthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumDeletionsProblemPreservesCache(
                "Remote p435 missing reviewed solution section",
                minimumDeletionsProblemWithInvalidFirstSolution(
                        "Remote p435 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMinimumDeletionsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumDeletionsProblemPreservesCache(
                "Remote p435 weak tests",
                minimumDeletionsProblemWithWeakTests("Remote p435 weak tests"),
                "Minimum Deletions to Make String Balanced tests must cover");
    }

    @Test
    void forceRefreshRejectsMinimumDeletionsWithoutOnePassDpWithoutReplacingCache() throws IOException {
        assertRejectedMinimumDeletionsProblemPreservesCache(
                "Remote p435 missing one-pass DP route",
                minimumDeletionsProblemWithoutSolution(
                        "Remote p435 missing one-pass DP route",
                        "One Pass DP Delete A Or Previous Bs"),
                "one-pass deletion-choice DP route");
    }

    @Test
    void forceRefreshRejectsMinimumDeletionsWithoutPrefixSuffixWithoutReplacingCache() throws IOException {
        assertRejectedMinimumDeletionsProblemPreservesCache(
                "Remote p435 missing prefix/suffix route",
                minimumDeletionsProblemWithoutSolution(
                        "Remote p435 missing prefix/suffix route",
                        "Prefix Suffix Split Counts"),
                "prefix/suffix split-count route");
    }

    @Test
    void forceRefreshRejectsMinimumDeletionsWithoutUnmatchedBGreedyWithoutReplacingCache() throws IOException {
        assertRejectedMinimumDeletionsProblemPreservesCache(
                "Remote p435 missing unmatched-b greedy route",
                minimumDeletionsProblemWithoutSolution(
                        "Remote p435 missing unmatched-b greedy route",
                        "Greedy Stack Of Unmatched B"),
                "unmatched-b greedy stack route");
    }

    @Test
    void forceRefreshRejectsMinimumDeletionsWithoutLongestKeptWithoutReplacingCache() throws IOException {
        assertRejectedMinimumDeletionsProblemPreservesCache(
                "Remote p435 missing longest kept route",
                minimumDeletionsProblemWithoutSolution(
                        "Remote p435 missing longest kept route",
                        "Longest Kept Balanced Subsequence"),
                "longest kept balanced-subsequence route");
    }

    @Test
    void forceRefreshRejectsMinimumDeletionsWithoutTwoStateDpWithoutReplacingCache() throws IOException {
        assertRejectedMinimumDeletionsProblemPreservesCache(
                "Remote p435 missing two-state DP route",
                minimumDeletionsProblemWithoutSolution(
                        "Remote p435 missing two-state DP route",
                        "Two-State Dynamic Programming"),
                "two-state dynamic-programming route");
    }

    private void assertRejectedMinimumDeletionsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Deletions to Make String Balanced", repository.getProblems().get(434).getTitle());

        remote.publishProblem(435, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p435.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p435 Minimum Deletions content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Deletions to Make String Balanced", repository.getProblems().get(434).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p435.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimum Deletions to Make String Balanced"));
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

    private static String minimumDeletionsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMinimumDeletionsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero for every string without modeling b-before-a inversions, while still being "
                        + "long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String minimumDeletionsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMinimumDeletionsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("s = \"aabbbbba\"")) {
                editedTests.add(testCase("s = \"ab\"", "0"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P435 baseline test set no longer contains the trailing-a after b-run case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String minimumDeletionsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMinimumDeletionsProblem();
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
            throw new IOException("P435 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMinimumDeletionsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p435.json");
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
        solution.addProperty("name", "Minimum Deletions Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without checking the relative order of a and b characters.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Minimum Deletions teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int minimumDeletions(String s) { return 0; } }";
    }
}
