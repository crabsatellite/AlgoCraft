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

class OfficialRepositoryCalendarIIITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftyFirstReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 missing reviewed solution section",
                calendarThreeProblemWithInvalidFirstSolution("Remote p451 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsCalendarThreeWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 weak tests",
                calendarThreeProblemWithWeakTests("Remote p451 weak tests"),
                "MyCalendar III tests must cover");
    }

    @Test
    void forceRefreshRejectsCalendarThreeWithoutTreeMapRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 missing TreeMap route",
                calendarThreeProblemWithoutSolution("Remote p451 missing TreeMap route",
                        "TreeMap Sweep-Line Delta"),
                "TreeMap sweep-line delta route");
    }

    @Test
    void forceRefreshRejectsCalendarThreeWithoutSegmentTreeRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 missing segment-tree route",
                calendarThreeProblemWithoutSolution("Remote p451 missing segment-tree route",
                        "Dynamic Segment Tree Range Add"),
                "dynamic segment-tree range-add route");
    }

    @Test
    void forceRefreshRejectsCalendarThreeWithoutCompressionRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 missing compression route",
                calendarThreeProblemWithoutSolution("Remote p451 missing compression route",
                        "Coordinate Compression Difference Rebuild"),
                "coordinate-compression rebuild route");
    }

    @Test
    void forceRefreshRejectsCalendarThreeWithoutDisjointSegmentRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 missing disjoint-segment route",
                calendarThreeProblemWithoutSolution("Remote p451 missing disjoint-segment route",
                        "Ordered Disjoint Segment Counts"),
                "ordered disjoint segment-count route");
    }

    @Test
    void forceRefreshRejectsCalendarThreeWithoutEndpointScanRouteWithoutReplacingCache() throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 missing endpoint-scan route",
                calendarThreeProblemWithoutSolution("Remote p451 missing endpoint-scan route",
                        "Start-Endpoint Scan Baseline"),
                "start-endpoint scan baseline route");
    }

    @Test
    void forceRefreshRejectsCalendarThreeDisjointSegmentsWithUnsupportedSetValueWithoutReplacingCache()
            throws IOException {
        assertRejectedCalendarThreeProblemPreservesCache(
                "Remote p451 broken disjoint segment update",
                calendarThreeProblemWithBrokenDisjointSegmentUpdate("Remote p451 broken disjoint segment update"),
                "ordered disjoint segment-count route");
    }

    private void assertRejectedCalendarThreeProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("My Calendar III", repository.getProblems().get(450).getTitle());

        remote.publishProblem(451, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p451.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p451 MyCalendarThree content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("My Calendar III", repository.getProblems().get(450).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p451.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("My Calendar III"));
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

    private static String calendarThreeProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validCalendarThreeProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Always return zero and ignore the maximum-overlap depth, while still being long enough "
                        + "for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String calendarThreeProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validCalendarThreeProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("999999999")) {
                editedTests.add(testCase(
                        "[\"MyCalendarThree\", \"book\", \"book\", \"book\", \"book\", \"book\", \"book\"]\n"
                                + "[[], [10, 20], [50, 60], [10, 40], [5, 15], [5, 10], [25, 55]]",
                        "[null, 1, 1, 2, 3, 3, 3]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P451 baseline test set no longer contains the coordinate-bound case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String calendarThreeProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validCalendarThreeProblem();
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
            throw new IOException("P451 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static String calendarThreeProblemWithBrokenDisjointSegmentUpdate(String title) throws IOException {
        JsonObject problem = validCalendarThreeProblem();
        problem.addProperty("title", title);
        boolean edited = false;
        for (JsonElement element : problem.getAsJsonArray("solutions")) {
            JsonObject solution = element.getAsJsonObject();
            if ("Ordered Disjoint Segment Counts".equals(solution.get("name").getAsString())) {
                String code = solution.get("code").getAsString();
                String fixedSnippet = "counts.put(entry.getKey(), next);";
                if (!code.contains(fixedSnippet)) {
                    throw new IOException("P451 disjoint segment solution no longer contains put-based update");
                }
                solution.addProperty("code", code.replace(fixedSnippet, "entry.setValue(next);"));
                edited = true;
            }
        }
        if (!edited) {
            throw new IOException("P451 baseline solutions no longer contain Ordered Disjoint Segment Counts");
        }
        return GSON.toJson(problem);
    }

    private static JsonObject validCalendarThreeProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p451.json");
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
        solution.addProperty("name", "MyCalendarThree Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore all intervals and always return zero.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required MyCalendarThree teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class MyCalendarThree { public MyCalendarThree() {} public int book(int start, int end) { return 0; } }";
    }
}
