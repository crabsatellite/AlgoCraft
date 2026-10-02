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

class OfficialRepositoryMinimumMovesIITest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtySecondReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumMovesIIProblemPreservesCache(
                "Remote p462 missing reviewed solution section",
                minimumMovesIIProblemWithInvalidFirstSolution("Remote p462 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMinimumMovesIIWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumMovesIIProblemPreservesCache(
                "Remote p462 weak tests",
                minimumMovesIIProblemWithWeakTests("Remote p462 weak tests"),
                "Minimum Moves II tests must cover");
    }

    @Test
    void forceRefreshRejectsMinimumMovesIIWithoutSortMedianRouteWithoutReplacingCache() throws IOException {
        assertRejectedMinimumMovesIIProblemPreservesCache(
                "Remote p462 missing sort median route",
                minimumMovesIIProblemWithoutSolution("Remote p462 missing sort median route", "Sort and Median"),
                "sort-to-median absolute-distance route");
    }

    @Test
    void forceRefreshRejectsMinimumMovesIIWithoutPairedExtremesRouteWithoutReplacingCache() throws IOException {
        assertRejectedMinimumMovesIIProblemPreservesCache(
                "Remote p462 missing paired extremes route",
                minimumMovesIIProblemWithoutSolution(
                        "Remote p462 missing paired extremes route", "Sort and Pair Extremes"),
                "paired-extremes median-interval route");
    }

    @Test
    void forceRefreshRejectsMinimumMovesIIWithoutQuickselectRouteWithoutReplacingCache() throws IOException {
        assertRejectedMinimumMovesIIProblemPreservesCache(
                "Remote p462 missing quickselect route",
                minimumMovesIIProblemWithoutSolution(
                        "Remote p462 missing quickselect route", "Randomized Quickselect Median"),
                "quickselect median route");
    }

    private void assertRejectedMinimumMovesIIProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Moves to Equal Array Elements II", repository.getProblems().get(461).getTitle());

        remote.publishProblem(462, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p462.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p462 minimum-moves content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Moves to Equal Array Elements II", repository.getProblems().get(461).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p462.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimum Moves to Equal Array Elements II"));
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

    private static String minimumMovesIIProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMinimumMovesIIProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without reasoning about absolute distances or the middle of the sorted order, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String minimumMovesIIProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMinimumMovesIIProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "nums = [-1000000000,0,1000000000]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [1,2,3]", "2"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P462 baseline test set no longer contains the int-sized extreme-distance case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String minimumMovesIIProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMinimumMovesIIProblem();
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
            throw new IOException("P462 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMinimumMovesIIProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p462.json");
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
        solution.addProperty("name", "Minimum Moves II Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without sorting, two-pointer distance sums, or selection.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required minimum-moves teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int minMoves2(int[] nums) { return 0; } }";
    }
}
