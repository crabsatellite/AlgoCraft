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

class OfficialRepositoryLongestHarmoniousSubsequenceTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSeventyFirstReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestHarmoniousSubsequenceProblemPreservesCache(
                "Remote p471 missing reviewed solution section",
                longestHarmoniousSubsequenceProblemWithInvalidFirstSolution(
                        "Remote p471 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsLongestHarmoniousSubsequenceWeakTestsWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestHarmoniousSubsequenceProblemPreservesCache(
                "Remote p471 weak tests",
                longestHarmoniousSubsequenceProblemWithWeakTests("Remote p471 weak tests"),
                "Longest Harmonious Subsequence tests must cover");
    }

    @Test
    void forceRefreshRejectsLongestHarmoniousSubsequenceWithoutHashMapRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestHarmoniousSubsequenceProblemPreservesCache(
                "Remote p471 missing hash-map route",
                longestHarmoniousSubsequenceProblemWithoutSolution(
                        "Remote p471 missing hash-map route", "HashMap Frequency Count"),
                "hash-map frequency-count route");
    }

    @Test
    void forceRefreshRejectsLongestHarmoniousSubsequenceWithoutSortedWindowRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestHarmoniousSubsequenceProblemPreservesCache(
                "Remote p471 missing sorted-window route",
                longestHarmoniousSubsequenceProblemWithoutSolution(
                        "Remote p471 missing sorted-window route", "Sort and Sliding Window"),
                "sorted sliding-window route");
    }

    @Test
    void forceRefreshRejectsLongestHarmoniousSubsequenceWithoutOrderedMapRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedLongestHarmoniousSubsequenceProblemPreservesCache(
                "Remote p471 missing ordered-map route",
                longestHarmoniousSubsequenceProblemWithoutSolution(
                        "Remote p471 missing ordered-map route", "Ordered Frequency Map"),
                "ordered frequency-map route");
    }

    private void assertRejectedLongestHarmoniousSubsequenceProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Longest Harmonious Subsequence", repository.getProblems().get(470).getTitle());

        remote.publishProblem(471, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p471.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p471 harmonious-subsequence content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Longest Harmonious Subsequence", repository.getProblems().get(470).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p471.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Longest Harmonious Subsequence"));
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

    private static String longestHarmoniousSubsequenceProblemWithInvalidFirstSolution(String title)
            throws IOException {
        JsonObject problem = validLongestHarmoniousSubsequenceProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without requiring two values whose difference is exactly one, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String longestHarmoniousSubsequenceProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validLongestHarmoniousSubsequenceProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "nums = [0,2,4,6]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [1,2,3,4]", "2"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P471 baseline test set no longer contains the no-adjacent-values case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String longestHarmoniousSubsequenceProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validLongestHarmoniousSubsequenceProblem();
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
            throw new IOException("P471 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validLongestHarmoniousSubsequenceProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p471.json");
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
        solution.addProperty("name", "Longest Harmonious Subsequence Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for exact-difference subsequences.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int findLHS(int[] nums) { return 0; } }";
    }
}
