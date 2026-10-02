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

class OfficialRepositorySetMismatchTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightyFirstReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedSetMismatchProblemPreservesCache(
                "Remote p481 missing reviewed solution section",
                setMismatchProblemWithInvalidFirstSolution(
                        "Remote p481 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsSetMismatchWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedSetMismatchProblemPreservesCache(
                "Remote p481 weak tests",
                setMismatchProblemWithWeakTests("Remote p481 weak tests"),
                "Set Mismatch tests must cover");
    }

    @Test
    void forceRefreshRejectsSetMismatchWithoutIndexMarkingRouteWithoutReplacingCache() throws IOException {
        assertRejectedSetMismatchProblemPreservesCache(
                "Remote p481 missing index marking route",
                setMismatchProblemWithoutSolution(
                        "Remote p481 missing index marking route", "In-Place Index Marking"),
                "in-place index-marking route");
    }

    @Test
    void forceRefreshRejectsSetMismatchWithoutSumEquationsRouteWithoutReplacingCache() throws IOException {
        assertRejectedSetMismatchProblemPreservesCache(
                "Remote p481 missing sum equations route",
                setMismatchProblemWithoutSolution(
                        "Remote p481 missing sum equations route", "Sum and Square Equations"),
                "sum-and-square-equations route");
    }

    @Test
    void forceRefreshRejectsSetMismatchWithoutXorPartitionRouteWithoutReplacingCache() throws IOException {
        assertRejectedSetMismatchProblemPreservesCache(
                "Remote p481 missing XOR partition route",
                setMismatchProblemWithoutSolution(
                        "Remote p481 missing XOR partition route", "XOR Partition"),
                "XOR partition route");
    }

    @Test
    void forceRefreshRejectsSetMismatchWithoutSortGapRouteWithoutReplacingCache() throws IOException {
        assertRejectedSetMismatchProblemPreservesCache(
                "Remote p481 missing sort route",
                setMismatchProblemWithoutSolution(
                        "Remote p481 missing sort route", "Sort and Gap Scan"),
                "sort-and-gap-scan route");
    }

    private void assertRejectedSetMismatchProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Set Mismatch", repository.getProblems().get(480).getTitle());

        remote.publishProblem(481, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p481.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p481 set-mismatch content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Set Mismatch", repository.getProblems().get(480).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p481.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Set Mismatch"));
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

    private static String setMismatchProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validSetMismatchProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant pair without detecting which number is duplicated or missing, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String setMismatchProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validSetMismatchProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "nums = [4,1,2,3,6,7,8,8,9]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [1,2,2,4]", "[2,3]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P481 baseline test set no longer contains the separated middle-gap case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String setMismatchProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validSetMismatchProblem();
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
            throw new IOException("P481 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validSetMismatchProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p481.json");
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
        solution.addProperty("name", "Set Mismatch Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for detecting set mismatch values.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int[] findErrorNums(int[] nums) { return new int[]{1, 2}; } }";
    }
}
