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

class OfficialRepositoryTwoKeysKeyboardTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightySixthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedKeyboardProblemPreservesCache(
                "Remote p486 missing reviewed solution section",
                keyboardProblemWithInvalidFirstSolution("Remote p486 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsKeyboardWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedKeyboardProblemPreservesCache(
                "Remote p486 weak tests",
                keyboardProblemWithWeakTests("Remote p486 weak tests"),
                "2 Keys Keyboard tests must cover factorization boundaries");
    }

    @Test
    void forceRefreshRejectsKeyboardWithoutPrimeFactorizationRouteWithoutReplacingCache() throws IOException {
        assertRejectedKeyboardProblemPreservesCache(
                "Remote p486 missing prime factorization",
                keyboardProblemWithoutSolution("Remote p486 missing prime factorization", "Prime Factorization"),
                "prime-factorization route");
    }

    @Test
    void forceRefreshRejectsKeyboardWithoutLargestDivisorRouteWithoutReplacingCache() throws IOException {
        assertRejectedKeyboardProblemPreservesCache(
                "Remote p486 missing largest divisor",
                keyboardProblemWithoutSolution("Remote p486 missing largest divisor", "Largest Divisor Recurrence"),
                "largest-divisor recurrence route");
    }

    @Test
    void forceRefreshRejectsKeyboardWithoutBottomUpDpRouteWithoutReplacingCache() throws IOException {
        assertRejectedKeyboardProblemPreservesCache(
                "Remote p486 missing bottom-up DP",
                keyboardProblemWithoutSolution("Remote p486 missing bottom-up DP", "Bottom-Up Divisor DP"),
                "bottom-up divisor DP route");
    }

    @Test
    void forceRefreshRejectsKeyboardWithoutTopDownDpRouteWithoutReplacingCache() throws IOException {
        assertRejectedKeyboardProblemPreservesCache(
                "Remote p486 missing top-down DP",
                keyboardProblemWithoutSolution("Remote p486 missing top-down DP", "Top-Down Memoized Divisor DP"),
                "top-down memoized divisor DP route");
    }

    private void assertRejectedKeyboardProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("2 Keys Keyboard", repository.getProblems().get(485).getTitle());

        remote.publishProblem(486, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p486.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p486 keyboard content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("2 Keys Keyboard", repository.getProblems().get(485).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p486.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("2 Keys Keyboard"));
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

    private static String keyboardProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validKeyboardProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the target value without explaining copy-paste factorization.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String keyboardProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validKeyboardProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "n = 1000".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("n = 10", "7"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P486 baseline test set no longer contains the upper-bound factorization case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String keyboardProblemWithoutSolution(String title, String missingSolutionName) throws IOException {
        JsonObject problem = validKeyboardProblem();
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
            throw new IOException("P486 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validKeyboardProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p486.json");
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
        solution.addProperty("name", "Keyboard Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores divisors and copy-paste growth phases.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int minSteps(int n) { return 0; } }";
    }
}
