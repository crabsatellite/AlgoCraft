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

class OfficialRepositoryAppendCharactersToMakeSubsequenceTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedAppendCharactersProblemPreservesCache(
                "Remote p433 missing reviewed solution section",
                appendCharactersProblemWithInvalidFirstSolution(
                        "Remote p433 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsAppendCharactersWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedAppendCharactersProblemPreservesCache(
                "Remote p433 weak tests",
                appendCharactersProblemWithWeakTests("Remote p433 weak tests"),
                "Append Characters to Make Subsequence tests must cover");
    }

    @Test
    void forceRefreshRejectsAppendCharactersWithoutTwoPointersWithoutReplacingCache() throws IOException {
        assertRejectedAppendCharactersProblemPreservesCache(
                "Remote p433 missing two-pointer route",
                appendCharactersProblemWithoutSolution(
                        "Remote p433 missing two-pointer route",
                        "Two Pointer Prefix Scan"),
                "two-pointer prefix-scan route");
    }

    @Test
    void forceRefreshRejectsAppendCharactersWithoutIndexOfWithoutReplacingCache() throws IOException {
        assertRejectedAppendCharactersProblemPreservesCache(
                "Remote p433 missing IndexOf route",
                appendCharactersProblemWithoutSolution(
                        "Remote p433 missing IndexOf route",
                        "IndexOf From Previous Match"),
                "IndexOf-from-previous-match route");
    }

    @Test
    void forceRefreshRejectsAppendCharactersWithoutPositionListsWithoutReplacingCache() throws IOException {
        assertRejectedAppendCharactersProblemPreservesCache(
                "Remote p433 missing position-list route",
                appendCharactersProblemWithoutSolution(
                        "Remote p433 missing position-list route",
                        "Position Lists Binary Search"),
                "position-list binary-search route");
    }

    @Test
    void forceRefreshRejectsAppendCharactersWithoutIndexQueuesWithoutReplacingCache() throws IOException {
        assertRejectedAppendCharactersProblemPreservesCache(
                "Remote p433 missing character queue route",
                appendCharactersProblemWithoutSolution(
                        "Remote p433 missing character queue route",
                        "Character Index Queues"),
                "character index-queue route");
    }

    @Test
    void forceRefreshRejectsAppendCharactersWithoutNextOccurrenceWithoutReplacingCache() throws IOException {
        assertRejectedAppendCharactersProblemPreservesCache(
                "Remote p433 missing next-occurrence route",
                appendCharactersProblemWithoutSolution(
                        "Remote p433 missing next-occurrence route",
                        "Next Occurrence Table"),
                "next-occurrence table route");
    }

    private void assertRejectedAppendCharactersProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Append Characters to Make Subsequence", repository.getProblems().get(432).getTitle());

        remote.publishProblem(433, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p433.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p433 Append Characters content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Append Characters to Make Subsequence", repository.getProblems().get(432).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p433.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Append Characters to Make Subsequence"));
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

    private static String appendCharactersProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validAppendCharactersProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the full target length without matching any prefix, while still being long enough "
                        + "for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String appendCharactersProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validAppendCharactersProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("s = \"abdc\", t = \"abcde\"")) {
                editedTests.add(testCase("s = \"abc\", t = \"abc\"", "0"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P433 baseline test set no longer contains the order-trap case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String appendCharactersProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validAppendCharactersProblem();
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
            throw new IOException("P433 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validAppendCharactersProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p433.json");
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
        solution.addProperty("name", "Append Characters Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the full target length without attempting to match a prefix.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Append Characters teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { "
                + "public int appendCharacters(String s, String t) { return t.length(); } "
                + "}";
    }
}
