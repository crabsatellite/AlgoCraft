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

class OfficialRepositoryCountWordsWithPrefixTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtySixthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedCountWordsWithPrefixProblemPreservesCache(
                "Remote p436 missing reviewed solution section",
                countWordsWithPrefixProblemWithInvalidFirstSolution(
                        "Remote p436 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsCountWordsWithPrefixWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedCountWordsWithPrefixProblemPreservesCache(
                "Remote p436 weak tests",
                countWordsWithPrefixProblemWithWeakTests("Remote p436 weak tests"),
                "Count Words With Prefix tests must cover");
    }

    @Test
    void forceRefreshRejectsCountWordsWithPrefixWithoutStartsWithWithoutReplacingCache() throws IOException {
        assertRejectedCountWordsWithPrefixProblemPreservesCache(
                "Remote p436 missing startsWith route",
                countWordsWithPrefixProblemWithoutSolution(
                        "Remote p436 missing startsWith route",
                        "StartsWith Library Scan"),
                "startsWith library-scan route");
    }

    @Test
    void forceRefreshRejectsCountWordsWithPrefixWithoutManualCheckWithoutReplacingCache() throws IOException {
        assertRejectedCountWordsWithPrefixProblemPreservesCache(
                "Remote p436 missing manual character route",
                countWordsWithPrefixProblemWithoutSolution(
                        "Remote p436 missing manual character route",
                        "Manual Character Check"),
                "manual character-check route");
    }

    @Test
    void forceRefreshRejectsCountWordsWithPrefixWithoutTrieWithoutReplacingCache() throws IOException {
        assertRejectedCountWordsWithPrefixProblemPreservesCache(
                "Remote p436 missing Trie route",
                countWordsWithPrefixProblemWithoutSolution(
                        "Remote p436 missing Trie route",
                        "Trie Pass Counter"),
                "Trie pass-counter route");
    }

    @Test
    void forceRefreshRejectsCountWordsWithPrefixWithoutSortedRangeWithoutReplacingCache() throws IOException {
        assertRejectedCountWordsWithPrefixProblemPreservesCache(
                "Remote p436 missing sorted range route",
                countWordsWithPrefixProblemWithoutSolution(
                        "Remote p436 missing sorted range route",
                        "Sorted Range Binary Search"),
                "sorted-range binary-search route");
    }

    @Test
    void forceRefreshRejectsCountWordsWithPrefixWithoutPrefixFrequencyWithoutReplacingCache() throws IOException {
        assertRejectedCountWordsWithPrefixProblemPreservesCache(
                "Remote p436 missing prefix-frequency route",
                countWordsWithPrefixProblemWithoutSolution(
                        "Remote p436 missing prefix-frequency route",
                        "Prefix Frequency Hash Map"),
                "prefix-frequency HashMap route");
    }

    private void assertRejectedCountWordsWithPrefixProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Words With Prefix", repository.getProblems().get(435).getTitle());

        remote.publishProblem(436, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p436.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p436 Count Words With Prefix content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Words With Prefix", repository.getProblems().get(435).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p436.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Count Words With Prefix"));
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

    private static String countWordsWithPrefixProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validCountWordsWithPrefixProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without checking any word prefix, while still being long enough for the "
                        + "generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String countWordsWithPrefixProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validCountWordsWithPrefixProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("pref = \"zz\"")) {
                editedTests.add(testCase("words = [\"a\",\"ab\",\"abc\"], pref = \"a\"", "3"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P436 baseline test set no longer contains the z-boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String countWordsWithPrefixProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validCountWordsWithPrefixProblem();
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
            throw new IOException("P436 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validCountWordsWithPrefixProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p436.json");
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
        solution.addProperty("name", "Count Words Prefix Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without scanning the words.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Count Words With Prefix teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int prefixCount(String[] words, String pref) { return 0; } }";
    }
}
