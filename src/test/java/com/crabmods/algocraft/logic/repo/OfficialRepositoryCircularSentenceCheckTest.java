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

class OfficialRepositoryCircularSentenceCheckTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentySeventhReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedCircularSentenceCheckProblemPreservesCache(
                "Remote p427 missing reviewed solution section",
                circularSentenceCheckProblemWithInvalidFirstSolution(
                        "Remote p427 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsCircularSentenceCheckWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedCircularSentenceCheckProblemPreservesCache(
                "Remote p427 weak tests",
                circularSentenceCheckProblemWithWeakTests("Remote p427 weak tests"),
                "Circular Sentence Check tests must cover");
    }

    @Test
    void forceRefreshRejectsCircularSentenceCheckWithoutSplitBoundaryWithoutReplacingCache()
            throws IOException {
        assertRejectedCircularSentenceCheckProblemPreservesCache(
                "Remote p427 missing split word-boundary route",
                circularSentenceCheckProblemWithoutSolution(
                        "Remote p427 missing split word-boundary route",
                        "Split Word Boundary Check"),
                "split word-boundary route");
    }

    @Test
    void forceRefreshRejectsCircularSentenceCheckWithoutSpaceBoundaryScanWithoutReplacingCache()
            throws IOException {
        assertRejectedCircularSentenceCheckProblemPreservesCache(
                "Remote p427 missing space-boundary scan route",
                circularSentenceCheckProblemWithoutSolution(
                        "Remote p427 missing space-boundary scan route",
                        "Space Boundary Scan"),
                "space-boundary scan route");
    }

    @Test
    void forceRefreshRejectsCircularSentenceCheckWithoutDelimiterJumpScanWithoutReplacingCache()
            throws IOException {
        assertRejectedCircularSentenceCheckProblemPreservesCache(
                "Remote p427 missing delimiter-jump scan route",
                circularSentenceCheckProblemWithoutSolution(
                        "Remote p427 missing delimiter-jump scan route",
                        "Delimiter Jump Scan"),
                "delimiter-jump scan route");
    }

    @Test
    void forceRefreshRejectsCircularSentenceCheckWithoutBoundaryCharacterArraysWithoutReplacingCache()
            throws IOException {
        assertRejectedCircularSentenceCheckProblemPreservesCache(
                "Remote p427 missing boundary-character arrays route",
                circularSentenceCheckProblemWithoutSolution(
                        "Remote p427 missing boundary-character arrays route",
                        "Boundary Character Arrays"),
                "boundary-character arrays route");
    }

    @Test
    void forceRefreshRejectsCircularSentenceCheckWithoutRecursiveWordLinkWithoutReplacingCache()
            throws IOException {
        assertRejectedCircularSentenceCheckProblemPreservesCache(
                "Remote p427 missing recursive word-link route",
                circularSentenceCheckProblemWithoutSolution(
                        "Remote p427 missing recursive word-link route",
                        "Recursive Word Link Check"),
                "recursive word-link route");
    }

    private void assertRejectedCircularSentenceCheckProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Circular Sentence Check", repository.getProblems().get(426).getTitle());

        remote.publishProblem(427, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p427.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p427 Circular Sentence Check content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Circular Sentence Check", repository.getProblems().get(426).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p427.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Circular Sentence Check"));
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

    private static String circularSentenceCheckProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validCircularSentenceCheckProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return true without checking any word boundary, while still being long enough for "
                        + "the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Solution { public boolean isCircularSentence(String sentence) { return true; } }");
        return GSON.toJson(problem);
    }

    private static String circularSentenceCheckProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validCircularSentenceCheckProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("ab bc cd")) {
                editedTests.add(testCase("sentence = \"za az\"", "true"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P427 baseline test set no longer contains the final-wraparound case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String circularSentenceCheckProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validCircularSentenceCheckProblem();
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
            throw new IOException("P427 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validCircularSentenceCheckProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p427.json");
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
        solution.addProperty("name", "Circular Sentence Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return true without checking the sentence boundaries.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Circular Sentence Check "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Solution { public boolean isCircularSentence(String sentence) { return true; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
