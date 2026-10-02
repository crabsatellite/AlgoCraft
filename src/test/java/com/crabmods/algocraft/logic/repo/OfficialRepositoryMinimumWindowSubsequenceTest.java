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

class OfficialRepositoryMinimumWindowSubsequenceTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtySeventhReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
                "Remote p437 missing reviewed solution section",
                minimumWindowSubsequenceProblemWithInvalidFirstSolution(
                        "Remote p437 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMinimumWindowSubsequenceWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
                "Remote p437 weak tests",
                minimumWindowSubsequenceProblemWithWeakTests("Remote p437 weak tests"),
                "Minimum Window Subsequence tests must cover");
    }

    @Test
    void forceRefreshRejectsMinimumWindowSubsequenceWithoutForwardBackwardScanWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
                "Remote p437 missing forward/backward route",
                minimumWindowSubsequenceProblemWithoutSolution(
                        "Remote p437 missing forward/backward route",
                        "Forward Backward Scan"),
                "forward/backward scan route");
    }

    @Test
    void forceRefreshRejectsMinimumWindowSubsequenceWithoutNextTableWithoutReplacingCache() throws IOException {
        assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
                "Remote p437 missing next table route",
                minimumWindowSubsequenceProblemWithoutSolution(
                        "Remote p437 missing next table route",
                        "Next Occurrence Table"),
                "next-occurrence table route");
    }

    @Test
    void forceRefreshRejectsMinimumWindowSubsequenceWithoutPositionListWithoutReplacingCache() throws IOException {
        assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
                "Remote p437 missing position-list route",
                minimumWindowSubsequenceProblemWithoutSolution(
                        "Remote p437 missing position-list route",
                        "Position Lists Binary Search"),
                "position-list binary-search route");
    }

    @Test
    void forceRefreshRejectsMinimumWindowSubsequenceWithoutForwardStartDpWithoutReplacingCache() throws IOException {
        assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
                "Remote p437 missing forward start-DP route",
                minimumWindowSubsequenceProblemWithoutSolution(
                        "Remote p437 missing forward start-DP route",
                        "Forward Start Dynamic Programming"),
                "forward start-DP route");
    }

    @Test
    void forceRefreshRejectsMinimumWindowSubsequenceWithoutBackwardEndDpWithoutReplacingCache() throws IOException {
        assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
                "Remote p437 missing backward end-DP route",
                minimumWindowSubsequenceProblemWithoutSolution(
                        "Remote p437 missing backward end-DP route",
                        "Backward End Dynamic Programming"),
                "backward end-DP route");
    }

    private void assertRejectedMinimumWindowSubsequenceProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Window Subsequence", repository.getProblems().get(436).getTitle());

        remote.publishProblem(437, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p437.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p437 Minimum Window Subsequence content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Window Subsequence", repository.getProblems().get(436).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p437.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimum Window Subsequence"));
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

    private static String minimumWindowSubsequenceProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMinimumWindowSubsequenceProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the empty string without tracking subsequence windows, while still being long "
                        + "enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String minimumWindowSubsequenceProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMinimumWindowSubsequenceProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("t = \"azde\"")) {
                editedTests.add(testCase("s = \"abc\", t = \"ac\"", "\"abc\""));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P437 baseline test set no longer contains the scattered-window case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String minimumWindowSubsequenceProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMinimumWindowSubsequenceProblem();
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
            throw new IOException("P437 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMinimumWindowSubsequenceProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p437.json");
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
        solution.addProperty("name", "Minimum Window Subsequence Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the empty string without matching subsequences.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Minimum Window Subsequence "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public String minWindow(String s, String t) { return \"\"; } }";
    }
}
