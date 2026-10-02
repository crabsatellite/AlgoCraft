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

class OfficialRepositoryMinimumOperationsCollectElementsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredNinetySecondReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumOperationsCollectElementsProblemPreservesCache(
                "Remote p492 missing reviewed solution section",
                minimumOperationsProblemWithInvalidFirstSolution("Remote p492 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMinimumOperationsCollectElementsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumOperationsCollectElementsProblemPreservesCache(
                "Remote p492 weak tests",
                minimumOperationsProblemWithWeakTests("Remote p492 weak tests"),
                "Minimum Operations tests must cover suffix-collection boundaries");
    }

    @Test
    void forceRefreshRejectsMinimumOperationsCollectElementsWithoutHashSetRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumOperationsCollectElementsProblemPreservesCache(
                "Remote p492 missing HashSet route",
                minimumOperationsProblemWithoutSolution(
                        "Remote p492 missing HashSet route", "Reverse HashSet Removal"),
                "reverse HashSet-removal route");
    }

    @Test
    void forceRefreshRejectsMinimumOperationsCollectElementsWithoutBooleanRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumOperationsCollectElementsProblemPreservesCache(
                "Remote p492 missing boolean route",
                minimumOperationsProblemWithoutSolution(
                        "Remote p492 missing boolean route", "Boolean Seen With Remaining Count"),
                "boolean remaining-count route");
    }

    @Test
    void forceRefreshRejectsMinimumOperationsCollectElementsWithoutBitSetRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumOperationsCollectElementsProblemPreservesCache(
                "Remote p492 missing BitSet route",
                minimumOperationsProblemWithoutSolution(
                        "Remote p492 missing BitSet route", "BitSet Reverse Scan"),
                "BitSet reverse-scan route");
    }

    @Test
    void forceRefreshRejectsMinimumOperationsCollectElementsWithoutBoundaryRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumOperationsCollectElementsProblemPreservesCache(
                "Remote p492 missing boundary route",
                minimumOperationsProblemWithoutSolution(
                        "Remote p492 missing boundary route", "Rightmost Boundary Scan"),
                "rightmost-boundary scan route");
    }

    private void assertRejectedMinimumOperationsCollectElementsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Operations to Collect Elements", repository.getProblems().get(491).getTitle());

        remote.publishProblem(492, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p492.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p492 collect-elements content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Operations to Collect Elements", repository.getProblems().get(491).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p492.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimum Operations to Collect Elements"));
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

    private static String minimumOperationsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMinimumOperationsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return one without explaining suffix collection.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String minimumOperationsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMinimumOperationsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "nums = [1,2,3,9,8,7], k = 3".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [1], k = 1", "1"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P492 baseline test set no longer contains the noisy suffix case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String minimumOperationsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMinimumOperationsProblem();
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
            throw new IOException("P492 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMinimumOperationsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p492.json");
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
        solution.addProperty("name", "Collect Elements Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return one without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores the removable suffix and required values.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int minOperations(java.util.List<Integer> nums, int k) { return 1; } }";
    }
}
