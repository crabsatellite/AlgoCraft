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

class OfficialRepositoryReverseWordsInAStringIIITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedReverseWordsProblemPreservesCache(
                "Remote p464 missing reviewed solution section",
                reverseWordsProblemWithInvalidFirstSolution("Remote p464 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsReverseWordsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedReverseWordsProblemPreservesCache(
                "Remote p464 weak tests",
                reverseWordsProblemWithWeakTests("Remote p464 weak tests"),
                "Reverse Words in a String III tests must cover");
    }

    @Test
    void forceRefreshRejectsReverseWordsStringSplitSolutionWithoutReplacingCache() throws IOException {
        assertRejectedReverseWordsProblemPreservesCache(
                "Remote p464 String.split solution",
                reverseWordsProblemWithStringSplitSolution("Remote p464 String.split solution"),
                "must not use String.split");
    }

    @Test
    void forceRefreshRejectsReverseWordsWithoutTokenizeRouteWithoutReplacingCache() throws IOException {
        assertRejectedReverseWordsProblemPreservesCache(
                "Remote p464 missing tokenize route",
                reverseWordsProblemWithoutSolution(
                        "Remote p464 missing tokenize route", "Tokenize and Reverse Each Word"),
                "tokenize-and-rebuild route");
    }

    @Test
    void forceRefreshRejectsReverseWordsWithoutInPlaceRouteWithoutReplacingCache() throws IOException {
        assertRejectedReverseWordsProblemPreservesCache(
                "Remote p464 missing in-place route",
                reverseWordsProblemWithoutSolution(
                        "Remote p464 missing in-place route", "In-Place Word Boundary Reversal"),
                "in-place word-boundary reversal route");
    }

    @Test
    void forceRefreshRejectsReverseWordsWithoutSpanCopyRouteWithoutReplacingCache() throws IOException {
        assertRejectedReverseWordsProblemPreservesCache(
                "Remote p464 missing span-copy route",
                reverseWordsProblemWithoutSolution(
                        "Remote p464 missing span-copy route", "Reverse Spans Directly Into Output"),
                "direct span-copy route");
    }

    @Test
    void forceRefreshRejectsReverseWordsWithoutManualBufferRouteWithoutReplacingCache() throws IOException {
        assertRejectedReverseWordsProblemPreservesCache(
                "Remote p464 missing manual buffer route",
                reverseWordsProblemWithoutSolution(
                        "Remote p464 missing manual buffer route", "Manual Word Buffer"),
                "manual word-buffer route");
    }

    private void assertRejectedReverseWordsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Reverse Words in a String III", repository.getProblems().get(463).getTitle());

        remote.publishProblem(464, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p464.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p464 reverse-words content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Reverse Words in a String III", repository.getProblems().get(463).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p464.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Reverse Words in a String III"));
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

    private static String reverseWordsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validReverseWordsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the input unchanged without reversing any word, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String reverseWordsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validReverseWordsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "s = \"Mix3d CASE\"".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("s = \"God Ding\"", "\"doG gniD\""));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P464 baseline test set no longer contains the mixed-case digit case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String reverseWordsProblemWithStringSplitSolution(String title) throws IOException {
        JsonObject problem = validReverseWordsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use String.split to tokenize the sentence, then rebuild the answer word by word.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally uses a sandbox-hostile tokenization primitive so the "
                        + "remote validator proves it will not enter the offline cache.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(n)\n"
                        + "- Space Complexity: O(n)");
        firstSolution.addProperty(
                "code",
                "class Solution { public String reverseWords(String s) { String[] words = s.split(\" \"); return s; } }");
        return GSON.toJson(problem);
    }

    private static String reverseWordsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validReverseWordsProblem();
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
            throw new IOException("P464 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validReverseWordsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p464.json");
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
        solution.addProperty("name", "Reverse Words Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the original input without splitting, reversing spans, or buffering words.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required reverse-words teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public String reverseWords(String s) { return s; } }";
    }
}
