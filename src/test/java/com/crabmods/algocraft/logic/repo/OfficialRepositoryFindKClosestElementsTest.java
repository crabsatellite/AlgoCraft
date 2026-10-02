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

class OfficialRepositoryFindKClosestElementsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredNinetyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedFindKClosestElementsProblemPreservesCache(
                "Remote p493 missing reviewed solution section",
                findKClosestProblemWithInvalidFirstSolution("Remote p493 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsFindKClosestElementsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedFindKClosestElementsProblemPreservesCache(
                "Remote p493 weak tests",
                findKClosestProblemWithWeakTests("Remote p493 weak tests"),
                "Find K Closest Elements tests must cover closest-window boundaries");
    }

    @Test
    void forceRefreshRejectsFindKClosestElementsWithoutBinarySearchRouteWithoutReplacingCache() throws IOException {
        assertRejectedFindKClosestElementsProblemPreservesCache(
                "Remote p493 missing binary search route",
                findKClosestProblemWithoutSolution(
                        "Remote p493 missing binary search route", "Binary Search for Left Bound"),
                "left-bound binary search route");
    }

    @Test
    void forceRefreshRejectsFindKClosestElementsWithoutShrinkRouteWithoutReplacingCache() throws IOException {
        assertRejectedFindKClosestElementsProblemPreservesCache(
                "Remote p493 missing shrink route",
                findKClosestProblemWithoutSolution(
                        "Remote p493 missing shrink route", "Two Pointer Shrink Window"),
                "two-pointer shrink-window route");
    }

    @Test
    void forceRefreshRejectsFindKClosestElementsWithoutExpansionRouteWithoutReplacingCache() throws IOException {
        assertRejectedFindKClosestElementsProblemPreservesCache(
                "Remote p493 missing expansion route",
                findKClosestProblemWithoutSolution(
                        "Remote p493 missing expansion route", "Expand Around Insertion Point"),
                "insertion-point expansion route");
    }

    @Test
    void forceRefreshRejectsFindKClosestElementsWithoutHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedFindKClosestElementsProblemPreservesCache(
                "Remote p493 missing heap route",
                findKClosestProblemWithoutSolution(
                        "Remote p493 missing heap route", "Bounded Max Heap"),
                "bounded max-heap route");
    }

    private void assertRejectedFindKClosestElementsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Find K Closest Elements", repository.getProblems().get(492).getTitle());

        remote.publishProblem(493, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p493.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p493 closest-elements content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Find K Closest Elements", repository.getProblems().get(492).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p493.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Find K Closest Elements"));
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

    private static String findKClosestProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validFindKClosestProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty list without explaining the closest-window invariant.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String findKClosestProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validFindKClosestProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "arr = [-10000,-9999,0,9999,10000], k = 3, x = 9998"
                    .equals(test.get("input").getAsString())) {
                editedTests.add(testCase("arr = [1,2,3,4,5], k = 4, x = 3", "[1,2,3,4]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P493 baseline test set no longer contains the value-boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String findKClosestProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validFindKClosestProblem();
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
            throw new IOException("P493 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validFindKClosestProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p493.json");
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
        solution.addProperty("name", "Closest Elements Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty list without using any required closest-window route.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores distance ordering, tie-breaking, and sorted output.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public java.util.List<Integer> findClosestElements(int[] arr, int k, int x) { "
                + "return java.util.List.of(); } }";
    }
}
