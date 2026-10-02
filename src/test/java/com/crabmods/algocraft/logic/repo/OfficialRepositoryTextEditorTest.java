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

class OfficialRepositoryTextEditorTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortyEighthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedTextEditorProblemPreservesCache(
                "Remote p448 missing reviewed solution section",
                textEditorProblemWithInvalidFirstSolution("Remote p448 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsTextEditorWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedTextEditorProblemPreservesCache(
                "Remote p448 weak tests",
                textEditorProblemWithWeakTests("Remote p448 weak tests"),
                "TextEditor tests must cover");
    }

    @Test
    void forceRefreshRejectsTextEditorWithoutTwoHalvesRouteWithoutReplacingCache() throws IOException {
        assertRejectedTextEditorProblemPreservesCache(
                "Remote p448 missing two-halves route",
                textEditorProblemWithoutSolution(
                        "Remote p448 missing two-halves route",
                        "Two StringBuilder Halves"),
                "two StringBuilder halves route");
    }

    @Test
    void forceRefreshRejectsTextEditorWithoutStringBuilderCursorRouteWithoutReplacingCache() throws IOException {
        assertRejectedTextEditorProblemPreservesCache(
                "Remote p448 missing cursor-index route",
                textEditorProblemWithoutSolution(
                        "Remote p448 missing cursor-index route",
                        "Single StringBuilder with Cursor Index"),
                "single StringBuilder cursor-index route");
    }

    @Test
    void forceRefreshRejectsTextEditorWithoutLinkedListCursorRouteWithoutReplacingCache() throws IOException {
        assertRejectedTextEditorProblemPreservesCache(
                "Remote p448 missing linked-list route",
                textEditorProblemWithoutSolution(
                        "Remote p448 missing linked-list route",
                        "Doubly Linked List Cursor"),
                "doubly linked-list cursor route");
    }

    @Test
    void forceRefreshRejectsTextEditorWithoutGapBufferRouteWithoutReplacingCache() throws IOException {
        assertRejectedTextEditorProblemPreservesCache(
                "Remote p448 missing gap-buffer route",
                textEditorProblemWithoutSolution(
                        "Remote p448 missing gap-buffer route",
                        "Gap Buffer"),
                "gap-buffer route");
    }

    @Test
    void forceRefreshRejectsTextEditorWithoutTreapRopeRouteWithoutReplacingCache() throws IOException {
        assertRejectedTextEditorProblemPreservesCache(
                "Remote p448 missing Treap route",
                textEditorProblemWithoutSolution(
                        "Remote p448 missing Treap route",
                        "Implicit Treap Rope"),
                "implicit Treap rope route");
    }

    private void assertRejectedTextEditorProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Text Editor", repository.getProblems().get(447).getTitle());

        remote.publishProblem(448, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p448.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p448 TextEditor content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Text Editor", repository.getProblems().get(447).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p448.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design Text Editor"));
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

    private static String textEditorProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validTextEditorProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Append every insertion to the end and ignore the cursor position, while still being long "
                        + "enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String textEditorProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validTextEditorProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("abcdefghijklmnopqrstuvwxyz")) {
                editedTests.add(testCase(
                        "[\"TextEditor\", \"addText\", \"deleteText\", \"addText\", \"cursorRight\", \"cursorLeft\", \"deleteText\", \"cursorLeft\", \"cursorRight\"]\n"
                                + "[[], [\"leetcode\"], [4], [\"practice\"], [3], [8], [10], [2], [6]]",
                        "[null, null, 4, null, \"etpractice\", \"leet\", 4, \"\", \"practi\"]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P448 baseline test set no longer contains the alphabet last-10 case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String textEditorProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validTextEditorProblem();
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
            throw new IOException("P448 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validTextEditorProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p448.json");
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
        solution.addProperty("name", "TextEditor Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore the cursor and return an empty view for every movement.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required TextEditor teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class TextEditor { public TextEditor() {} public void addText(String text) {} public int deleteText(int k) { return 0; } public String cursorLeft(int k) { return \"\"; } public String cursorRight(int k) { return \"\"; } }";
    }
}
