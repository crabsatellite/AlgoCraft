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

class OfficialRepositoryStudentAttendanceRecordITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedStudentAttendanceRecordIProblemPreservesCache(
                "Remote p463 missing reviewed solution section",
                attendanceProblemWithInvalidFirstSolution("Remote p463 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsStudentAttendanceRecordIWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedStudentAttendanceRecordIProblemPreservesCache(
                "Remote p463 weak tests",
                attendanceProblemWithWeakTests("Remote p463 weak tests"),
                "Student Attendance Record I tests must cover");
    }

    @Test
    void forceRefreshRejectsStudentAttendanceRecordIWithoutSinglePassRouteWithoutReplacingCache() throws IOException {
        assertRejectedStudentAttendanceRecordIProblemPreservesCache(
                "Remote p463 missing single-pass route",
                attendanceProblemWithoutSolution(
                        "Remote p463 missing single-pass route", "Single-Pass Rule Counters"),
                "single-pass counter route");
    }

    @Test
    void forceRefreshRejectsStudentAttendanceRecordIWithoutDirectRuleRouteWithoutReplacingCache() throws IOException {
        assertRejectedStudentAttendanceRecordIProblemPreservesCache(
                "Remote p463 missing direct rule route",
                attendanceProblemWithoutSolution(
                        "Remote p463 missing direct rule route", "Two Independent Rule Checks"),
                "direct independent-rule check route");
    }

    @Test
    void forceRefreshRejectsStudentAttendanceRecordIWithoutAutomatonRouteWithoutReplacingCache() throws IOException {
        assertRejectedStudentAttendanceRecordIProblemPreservesCache(
                "Remote p463 missing automaton route",
                attendanceProblemWithoutSolution(
                        "Remote p463 missing automaton route", "Finite-State Rule Automaton"),
                "finite-state automaton route");
    }

    @Test
    void forceRefreshRejectsStudentAttendanceRecordIWithoutRegexRouteWithoutReplacingCache() throws IOException {
        assertRejectedStudentAttendanceRecordIProblemPreservesCache(
                "Remote p463 missing regex route",
                attendanceProblemWithoutSolution(
                        "Remote p463 missing regex route", "Regular Expression Rejection"),
                "regex rejection route");
    }

    private void assertRejectedStudentAttendanceRecordIProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Student Attendance Record I", repository.getProblems().get(462).getTitle());

        remote.publishProblem(463, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p463.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p463 attendance content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Student Attendance Record I", repository.getProblems().get(462).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p463.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Student Attendance Record I"));
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

    private static String attendanceProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validStudentAttendanceRecordIProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return true for every record without checking absences or late streaks, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String attendanceProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validStudentAttendanceRecordIProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "s = \"PALAP\"".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("s = \"PPALLP\"", "true"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P463 baseline test set no longer contains the separated-absence rejection case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String attendanceProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validStudentAttendanceRecordIProblem();
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
            throw new IOException("P463 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validStudentAttendanceRecordIProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p463.json");
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
        solution.addProperty("name", "Student Attendance Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return true without counting absences, scanning late streaks, state transitions, or regex checks.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required attendance teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public boolean checkRecord(String s) { return true; } }";
    }
}
