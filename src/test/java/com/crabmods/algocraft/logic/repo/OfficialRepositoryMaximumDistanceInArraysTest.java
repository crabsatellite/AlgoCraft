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

class OfficialRepositoryMaximumDistanceInArraysTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSeventySeventhReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumDistanceInArraysProblemPreservesCache(
                "Remote p477 missing reviewed solution section",
                maximumDistanceInArraysProblemWithInvalidFirstSolution(
                        "Remote p477 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMaximumDistanceInArraysWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMaximumDistanceInArraysProblemPreservesCache(
                "Remote p477 weak tests",
                maximumDistanceInArraysProblemWithWeakTests("Remote p477 weak tests"),
                "Maximum Distance in Arrays tests must cover");
    }

    @Test
    void forceRefreshRejectsMaximumDistanceInArraysWithoutGlobalEndpointRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumDistanceInArraysProblemPreservesCache(
                "Remote p477 missing global endpoint route",
                maximumDistanceInArraysProblemWithoutSolution(
                        "Remote p477 missing global endpoint route", "Track Global Min and Max"),
                "update-order global endpoint scan route");
    }

    @Test
    void forceRefreshRejectsMaximumDistanceInArraysWithoutSortedEndpointRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumDistanceInArraysProblemPreservesCache(
                "Remote p477 missing sorted endpoint route",
                maximumDistanceInArraysProblemWithoutSolution(
                        "Remote p477 missing sorted endpoint route", "Sort Endpoint Candidates"),
                "sorted endpoint-candidate route");
    }

    @Test
    void forceRefreshRejectsMaximumDistanceInArraysWithoutPrefixSuffixRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMaximumDistanceInArraysProblemPreservesCache(
                "Remote p477 missing prefix suffix route",
                maximumDistanceInArraysProblemWithoutSolution(
                        "Remote p477 missing prefix suffix route", "Prefix and Suffix Extremes"),
                "prefix/suffix exclusion route");
    }

    private void assertRejectedMaximumDistanceInArraysProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Distance in Arrays", repository.getProblems().get(476).getTitle());

        remote.publishProblem(477, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p477.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p477 maximum-distance content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Maximum Distance in Arrays", repository.getProblems().get(476).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p477.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Maximum Distance in Arrays"));
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

    private static String maximumDistanceInArraysProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMaximumDistanceInArraysProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without enforcing the different-array endpoint rule, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String maximumDistanceInArraysProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMaximumDistanceInArraysProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "arrays = [[1,100],[2],[3]]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("arrays = [[1],[1]]", "0"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P477 baseline test set no longer contains the same-array-extremes case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String maximumDistanceInArraysProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validMaximumDistanceInArraysProblem();
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
            throw new IOException("P477 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMaximumDistanceInArraysProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p477.json");
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
        solution.addProperty("name", "Maximum Distance Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for checking different-array endpoints.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int maxDistance(java.util.List<java.util.List<Integer>> arrays) { return 0; } }";
    }
}
