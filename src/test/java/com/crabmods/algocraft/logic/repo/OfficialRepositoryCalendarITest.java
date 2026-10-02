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

class OfficialRepositoryCalendarITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortyNinthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 missing reviewed solution section",
                calendarProblemWithInvalidFirstSolution("Remote p449 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsCalendarWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 weak tests",
                calendarProblemWithWeakTests("Remote p449 weak tests"),
                "MyCalendar I tests must cover");
    }

    @Test
    void forceRefreshRejectsCalendarWithoutTreeMapRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 missing TreeMap route",
                calendarProblemWithoutSolution("Remote p449 missing TreeMap route", "TreeMap Neighbor Check"),
                "TreeMap neighbor-check route");
    }

    @Test
    void forceRefreshRejectsCalendarWithoutBruteForceRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 missing brute-force route",
                calendarProblemWithoutSolution("Remote p449 missing brute-force route", "Brute Force Interval List"),
                "brute-force interval-list route");
    }

    @Test
    void forceRefreshRejectsCalendarWithoutSortedArrayRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 missing sorted-array route",
                calendarProblemWithoutSolution("Remote p449 missing sorted-array route",
                        "Sorted Arrays with Binary Search"),
                "sorted-array binary-search insertion route");
    }

    @Test
    void forceRefreshRejectsCalendarWithoutSweepLineRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 missing sweep-line route",
                calendarProblemWithoutSolution("Remote p449 missing sweep-line route",
                        "Sweep-Line Delta with Rollback"),
                "sweep-line rollback route");
    }

    @Test
    void forceRefreshRejectsCalendarWithoutSegmentTreeRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 missing segment-tree route",
                calendarProblemWithoutSolution("Remote p449 missing segment-tree route",
                        "Dynamic Segment Tree Occupancy"),
                "dynamic segment-tree occupancy route");
    }

    @Test
    void forceRefreshRejectsCalendarSegmentTreeWithoutCarryPropagationWithoutReplacingCache() throws IOException {
        assertRejectedCalendarProblemPreservesCache(
                "Remote p449 broken segment-tree carry",
                calendarProblemWithBrokenSegmentTreeCarry("Remote p449 broken segment-tree carry"),
                "dynamic segment-tree occupancy route");
    }

    private void assertRejectedCalendarProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("My Calendar I", repository.getProblems().get(448).getTitle());

        remote.publishProblem(449, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p449.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p449 MyCalendar content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("My Calendar I", repository.getProblems().get(448).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p449.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("My Calendar I"));
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

    private static String calendarProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validCalendarProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Always accept each interval and ignore overlaps, while still being long enough for the "
                        + "generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String calendarProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validCalendarProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("999999999")) {
                editedTests.add(testCase(
                        "[\"MyCalendar\", \"book\", \"book\", \"book\"]\n"
                                + "[[], [10, 20], [15, 25], [20, 30]]",
                        "[null, true, false, true]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P449 baseline test set no longer contains the coordinate-bound case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String calendarProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validCalendarProblem();
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
            throw new IOException("P449 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static String calendarProblemWithBrokenSegmentTreeCarry(String title) throws IOException {
        JsonObject problem = validCalendarProblem();
        problem.addProperty("title", title);
        JsonArray solutions = problem.getAsJsonArray("solutions");
        boolean edited = false;
        for (JsonElement element : solutions) {
            JsonObject solution = element.getAsJsonObject();
            if ("Dynamic Segment Tree Occupancy".equals(solution.get("name").getAsString())) {
                String code = solution.get("code").getAsString();
                String fixedSnippet = "if (node == null) {\n            return carry;\n        }";
                if (!code.contains(fixedSnippet)) {
                    throw new IOException("P449 segment-tree solution no longer contains carry propagation guard");
                }
                solution.addProperty("code", code.replace(fixedSnippet,
                        "if (node == null) {\n            return 0;\n        }"));
                edited = true;
            }
        }
        if (!edited) {
            throw new IOException("P449 baseline solutions no longer contain Dynamic Segment Tree Occupancy");
        }
        return GSON.toJson(problem);
    }

    private static JsonObject validCalendarProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p449.json");
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
        solution.addProperty("name", "MyCalendar Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Accept nothing and do not store any interval.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required MyCalendar teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class MyCalendar { public MyCalendar() {} public boolean book(int start, int end) { return false; } }";
    }
}
