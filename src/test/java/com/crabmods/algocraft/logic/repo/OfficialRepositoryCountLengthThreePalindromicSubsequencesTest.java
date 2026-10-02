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

class OfficialRepositoryCountLengthThreePalindromicSubsequencesTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedPalindromeProblemPreservesCache(
                "Remote p484 missing reviewed solution section",
                palindromeProblemWithInvalidFirstSolution(
                        "Remote p484 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsPalindromeWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedPalindromeProblemPreservesCache(
                "Remote p484 weak tests",
                palindromeProblemWithWeakTests("Remote p484 weak tests"),
                "Count Length-Three Palindromic Subsequences tests must cover");
    }

    @Test
    void forceRefreshRejectsPalindromeWithoutFirstLastRouteWithoutReplacingCache() throws IOException {
        assertRejectedPalindromeProblemPreservesCache(
                "Remote p484 missing first-last route",
                palindromeProblemWithoutSolution(
                        "Remote p484 missing first-last route", "First Last Window"),
                "first/last outer-character window route");
    }

    @Test
    void forceRefreshRejectsPalindromeWithoutPrefixSuffixRouteWithoutReplacingCache() throws IOException {
        assertRejectedPalindromeProblemPreservesCache(
                "Remote p484 missing prefix-suffix route",
                palindromeProblemWithoutSolution(
                        "Remote p484 missing prefix-suffix route", "Prefix Suffix Presence Tables"),
                "prefix/suffix presence-table route");
    }

    @Test
    void forceRefreshRejectsPalindromeWithoutBitmaskRouteWithoutReplacingCache() throws IOException {
        assertRejectedPalindromeProblemPreservesCache(
                "Remote p484 missing bitmask route",
                palindromeProblemWithoutSolution(
                        "Remote p484 missing bitmask route", "Streaming Middle Bitmasks"),
                "streaming bitmask route");
    }

    private void assertRejectedPalindromeProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Length-Three Palindromic Subsequences", repository.getProblems().get(483).getTitle());

        remote.publishProblem(484, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p484.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p484 palindrome content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Length-Three Palindromic Subsequences", repository.getProblems().get(483).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p484.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Count Length-Three Palindromic Subsequences"));
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

    private static String palindromeProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validPalindromeProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant without distinguishing unique palindrome strings from index triples.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String palindromeProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validPalindromeProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "s = \"abcabc\"".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("s = \"abc\"", "0"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P484 baseline test set no longer contains the overlapping-family case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String palindromeProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validPalindromeProblem();
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
            throw new IOException("P484 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validPalindromeProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p484.json");
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
        solution.addProperty("name", "Palindrome Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for counting distinct palindromic subsequences.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int countPalindromicSubsequence(String s) { return 0; } }";
    }
}
