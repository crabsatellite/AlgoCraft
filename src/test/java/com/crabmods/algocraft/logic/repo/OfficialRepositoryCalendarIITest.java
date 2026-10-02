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

class OfficialRepositoryCalendarIITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftiethReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 missing reviewed solution section",
                calendarTwoProblemWithInvalidFirstSolution("Remote p450 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsCalendarTwoWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 weak tests",
                calendarTwoProblemWithWeakTests("Remote p450 weak tests"),
                "MyCalendar II tests must cover");
    }

    @Test
    void forceRefreshRejectsCalendarTwoWithoutSingleDoubleListsRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 missing single-double route",
                calendarTwoProblemWithoutSolution("Remote p450 missing single-double route",
                        "Single and Double Booking Lists"),
                "single-and-double booking lists route");
    }

    @Test
    void forceRefreshRejectsCalendarTwoWithoutSweepLineRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 missing sweep-line route",
                calendarTwoProblemWithoutSolution("Remote p450 missing sweep-line route",
                        "TreeMap Sweep-Line with Rollback"),
                "TreeMap sweep-line rollback route");
    }

    @Test
    void forceRefreshRejectsCalendarTwoWithoutSegmentTreeRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 missing segment-tree route",
                calendarTwoProblemWithoutSolution("Remote p450 missing segment-tree route",
                        "Dynamic Segment Tree Max Count"),
                "dynamic segment-tree max-count route with carry propagation");
    }

    @Test
    void forceRefreshRejectsCalendarTwoWithoutCompressionRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 missing compression route",
                calendarTwoProblemWithoutSolution("Remote p450 missing compression route",
                        "Coordinate Compression Rebuild"),
                "coordinate-compression rebuild route");
    }

    @Test
    void forceRefreshRejectsCalendarTwoWithoutDisjointSegmentRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 missing disjoint-segment route",
                calendarTwoProblemWithoutSolution("Remote p450 missing disjoint-segment route",
                        "Ordered Disjoint Segment Counts"),
                "ordered disjoint segment-count route");
    }

    @Test
    void forceRefreshRejectsCalendarTwoSegmentTreeWithoutCarryPropagationWithoutReplacingCache() throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 broken segment-tree carry",
                calendarTwoProblemWithBrokenSegmentTreeCarry("Remote p450 broken segment-tree carry"),
                "dynamic segment-tree max-count route with carry propagation");
    }

    @Test
    void forceRefreshRejectsCalendarTwoDisjointSegmentsWithUnsupportedSetValueWithoutReplacingCache()
            throws IOException {
        assertRejectedCalendarTwoProblemPreservesCache(
                "Remote p450 broken disjoint segment update",
                calendarTwoProblemWithBrokenDisjointSegmentUpdate("Remote p450 broken disjoint segment update"),
                "ordered disjoint segment-count route");
    }

    private void assertRejectedCalendarTwoProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("My Calendar II", repository.getProblems().get(449).getTitle());

        remote.publishProblem(450, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p450.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p450 MyCalendarTwo content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("My Calendar II", repository.getProblems().get(449).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p450.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("My Calendar II"));
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

    private static String calendarTwoProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validCalendarTwoProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Always accept each interval and ignore triple bookings, while still being long enough "
                        + "for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String calendarTwoProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validCalendarTwoProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("999999999")) {
                editedTests.add(testCase(
                        "[\"MyCalendarTwo\", \"book\", \"book\", \"book\", \"book\", \"book\", \"book\"]\n"
                                + "[[], [10, 20], [50, 60], [10, 40], [5, 15], [5, 10], [25, 55]]",
                        "[null, true, true, true, false, true, true]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P450 baseline test set no longer contains the coordinate-bound case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String calendarTwoProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validCalendarTwoProblem();
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
            throw new IOException("P450 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static String calendarTwoProblemWithBrokenSegmentTreeCarry(String title) throws IOException {
        JsonObject problem = validCalendarTwoProblem();
        problem.addProperty("title", title);
        boolean edited = false;
        for (JsonElement element : problem.getAsJsonArray("solutions")) {
            JsonObject solution = element.getAsJsonObject();
            if ("Dynamic Segment Tree Max Count".equals(solution.get("name").getAsString())) {
                String code = solution.get("code").getAsString();
                String fixedSnippet = "if (node == null) {\n            return carry;\n        }";
                if (!code.contains(fixedSnippet)) {
                    throw new IOException("P450 segment-tree solution no longer contains carry propagation guard");
                }
                solution.addProperty("code", code.replace(fixedSnippet,
                        "if (node == null) {\n            return 0;\n        }"));
                edited = true;
            }
        }
        if (!edited) {
            throw new IOException("P450 baseline solutions no longer contain Dynamic Segment Tree Max Count");
        }
        return GSON.toJson(problem);
    }

    private static String calendarTwoProblemWithBrokenDisjointSegmentUpdate(String title) throws IOException {
        JsonObject problem = validCalendarTwoProblem();
        problem.addProperty("title", title);
        boolean edited = false;
        for (JsonElement element : problem.getAsJsonArray("solutions")) {
            JsonObject solution = element.getAsJsonObject();
            if ("Ordered Disjoint Segment Counts".equals(solution.get("name").getAsString())) {
                String code = solution.get("code").getAsString();
                String fixedSnippet = "counts.put(entry.getKey(), entry.getValue() + 1);";
                if (!code.contains(fixedSnippet)) {
                    throw new IOException("P450 disjoint segment solution no longer contains put-based update");
                }
                solution.addProperty("code", code.replace(fixedSnippet,
                        "entry.setValue(entry.getValue() + 1);"));
                edited = true;
            }
        }
        if (!edited) {
            throw new IOException("P450 baseline solutions no longer contain Ordered Disjoint Segment Counts");
        }
        return GSON.toJson(problem);
    }

    private static JsonObject validCalendarTwoProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p450.json");
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
        solution.addProperty("name", "MyCalendarTwo Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Accept nothing and do not store any interval.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required MyCalendarTwo teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class MyCalendarTwo { public MyCalendarTwo() {} public boolean book(int start, int end) { return false; } }";
    }
}
