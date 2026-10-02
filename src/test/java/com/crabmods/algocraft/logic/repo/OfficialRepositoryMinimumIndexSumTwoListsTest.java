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

class OfficialRepositoryMinimumIndexSumTwoListsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSeventyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumIndexSumTwoListsProblemPreservesCache(
                "Remote p473 missing reviewed solution section",
                minimumIndexSumTwoListsProblemWithInvalidFirstSolution(
                        "Remote p473 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMinimumIndexSumTwoListsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumIndexSumTwoListsProblemPreservesCache(
                "Remote p473 weak tests",
                minimumIndexSumTwoListsProblemWithWeakTests("Remote p473 weak tests"),
                "Minimum Index Sum of Two Lists tests must cover");
    }

    @Test
    void forceRefreshRejectsMinimumIndexSumTwoListsWithoutHashMapRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumIndexSumTwoListsProblemPreservesCache(
                "Remote p473 missing hash-map route",
                minimumIndexSumTwoListsProblemWithoutSolution(
                        "Remote p473 missing hash-map route", "HashMap Index Lookup"),
                "hash-map index lookup route");
    }

    @Test
    void forceRefreshRejectsMinimumIndexSumTwoListsWithoutIndexSumLayerRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumIndexSumTwoListsProblemPreservesCache(
                "Remote p473 missing index-sum layer route",
                minimumIndexSumTwoListsProblemWithoutSolution(
                        "Remote p473 missing index-sum layer route", "Index-Sum Layer Search"),
                "index-sum layer search route");
    }

    @Test
    void forceRefreshRejectsMinimumIndexSumTwoListsWithoutBruteForceBaselineWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumIndexSumTwoListsProblemPreservesCache(
                "Remote p473 missing brute-force route",
                minimumIndexSumTwoListsProblemWithoutSolution(
                        "Remote p473 missing brute-force route", "Brute Force Index Sum Baseline"),
                "brute-force index-sum baseline route");
    }

    private void assertRejectedMinimumIndexSumTwoListsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Index Sum of Two Lists", repository.getProblems().get(472).getTitle());

        remote.publishProblem(473, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p473.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p473 minimum-index-sum content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Index Sum of Two Lists", repository.getProblems().get(472).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p473.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimum Index Sum of Two Lists"));
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

    private static String minimumIndexSumTwoListsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMinimumIndexSumTwoListsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty array without checking common restaurant names, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String minimumIndexSumTwoListsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMinimumIndexSumTwoListsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "list1 = [\"New York Grill\",\"KFC\",\"Ramen House\"], list2 = [\"Taco Bell\",\"Ramen House\",\"New York Grill\"]"
                    .equals(test.get("input").getAsString())) {
                editedTests.add(testCase(
                        "list1 = [\"Shogun\",\"Tapioca Express\",\"Burger King\",\"KFC\"], list2 = [\"KFC\",\"Shogun\",\"Burger King\"]",
                        "[\"Shogun\"]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P473 baseline test set no longer contains the space-bearing restaurant case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String minimumIndexSumTwoListsProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validMinimumIndexSumTwoListsProblem();
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
            throw new IOException("P473 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMinimumIndexSumTwoListsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p473.json");
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
        solution.addProperty("name", "Minimum Index Sum Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty answer without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for index-sum minimization.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public String[] findRestaurant(String[] list1, String[] list2) { "
                + "return new String[0]; } }";
    }
}
