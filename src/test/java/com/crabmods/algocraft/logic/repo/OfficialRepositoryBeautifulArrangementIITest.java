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

class OfficialRepositoryBeautifulArrangementIITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredNinetySeventhReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedBeautifulArrangementIIProblemPreservesCache(
                "Remote p497 missing reviewed solution section",
                beautifulArrangementIIProblemWithInvalidFirstSolution("Remote p497 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsBeautifulArrangementIIWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedBeautifulArrangementIIProblemPreservesCache(
                "Remote p497 weak tests",
                beautifulArrangementIIProblemWithWeakTests("Remote p497 weak tests"),
                "Beautiful Arrangement II tests must cover permutation-property boundaries");
    }

    @Test
    void forceRefreshRejectsBeautifulArrangementIIWithoutPrefixZigzagRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedBeautifulArrangementIIProblemPreservesCache(
                "Remote p497 missing prefix zigzag route",
                beautifulArrangementIIProblemWithoutSolution(
                        "Remote p497 missing prefix zigzag route", "Prefix Zigzag Construction"),
                "prefix zigzag route");
    }

    @Test
    void forceRefreshRejectsBeautifulArrangementIIWithoutTailZigzagRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedBeautifulArrangementIIProblemPreservesCache(
                "Remote p497 missing tail zigzag route",
                beautifulArrangementIIProblemWithoutSolution(
                        "Remote p497 missing tail zigzag route", "Increasing Prefix Then Tail Zigzag"),
                "increasing-prefix tail-zigzag route");
    }

    @Test
    void forceRefreshRejectsBeautifulArrangementIIWithoutDifferenceWalkRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedBeautifulArrangementIIProblemPreservesCache(
                "Remote p497 missing difference walk route",
                beautifulArrangementIIProblemWithoutSolution(
                        "Remote p497 missing difference walk route", "Descending Difference Walk"),
                "descending-difference walk route");
    }

    @Test
    void forceRefreshRejectsBeautifulArrangementIIWithoutGlobalExtremesRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedBeautifulArrangementIIProblemPreservesCache(
                "Remote p497 missing global extremes route",
                beautifulArrangementIIProblemWithoutSolution(
                        "Remote p497 missing global extremes route", "Global Extremes While Shrinking K"),
                "global-extremes shrinking route");
    }

    private void assertRejectedBeautifulArrangementIIProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Beautiful Arrangement II", repository.getProblems().get(496).getTitle());

        remote.publishProblem(497, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p497.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p497 beautiful-arrangement content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Beautiful Arrangement II", repository.getProblems().get(496).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p497.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Beautiful Arrangement II"));
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

    private static String beautifulArrangementIIProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validBeautifulArrangementIIProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a fixed increasing array without explaining the required adjacent differences.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(n)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String beautifulArrangementIIProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validBeautifulArrangementIIProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "n = 10000, k = 9999".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("n = 3, k = 1", "[1,2,3]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P497 baseline test set no longer contains the maximum-boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String beautifulArrangementIIProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validBeautifulArrangementIIProblem();
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
            throw new IOException("P497 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validBeautifulArrangementIIProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p497.json");
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
        solution.addProperty("name", "Beautiful Arrangement Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(n)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an increasing array without constructing the requested difference count.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally produces only one adjacent-difference value.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(n)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int[] constructArray(int n, int k) { int[] a = new int[n]; "
                + "for (int i = 0; i < n; i++) a[i] = i + 1; return a; } }";
    }
}
