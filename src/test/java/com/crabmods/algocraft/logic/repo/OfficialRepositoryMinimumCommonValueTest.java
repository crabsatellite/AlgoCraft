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

import static org.junit.jupiter.api.Assertions.*;

class OfficialRepositoryMinimumCommonValueTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentySecondReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumCommonValueProblemPreservesCache(
                "Remote p422 missing reviewed solution section",
                minimumCommonValueProblemWithInvalidFirstSolution("Remote p422 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMinimumCommonValueWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumCommonValueProblemPreservesCache(
                "Remote p422 weak tests",
                minimumCommonValueProblemWithWeakTests("Remote p422 weak tests"),
                "Minimum Common Value tests must cover");
    }

    @Test
    void forceRefreshRejectsMinimumCommonValueWithoutMergeTwoPointersWithoutReplacingCache() throws IOException {
        assertRejectedMinimumCommonValueProblemPreservesCache(
                "Remote p422 missing merge-style two-pointer route",
                minimumCommonValueProblemWithoutSolution("Remote p422 missing merge-style two-pointer route",
                        "Merge-Style Two Pointers"),
                "merge-style two-pointer route");
    }

    @Test
    void forceRefreshRejectsMinimumCommonValueWithoutDuplicateBlockSkippingWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumCommonValueProblemPreservesCache(
                "Remote p422 missing duplicate-block skipping route",
                minimumCommonValueProblemWithoutSolution("Remote p422 missing duplicate-block skipping route",
                        "Duplicate-Block Skipping"),
                "duplicate-block skipping route");
    }

    @Test
    void forceRefreshRejectsMinimumCommonValueWithoutHashSetWithoutReplacingCache() throws IOException {
        assertRejectedMinimumCommonValueProblemPreservesCache(
                "Remote p422 missing hash-set lookup route",
                minimumCommonValueProblemWithoutSolution("Remote p422 missing hash-set lookup route",
                        "Hash Set Lookup"),
                "hash-set lookup route");
    }

    @Test
    void forceRefreshRejectsMinimumCommonValueWithoutBinarySearchCandidatesWithoutReplacingCache()
            throws IOException {
        assertRejectedMinimumCommonValueProblemPreservesCache(
                "Remote p422 missing binary-search shorter-candidates route",
                minimumCommonValueProblemWithoutSolution("Remote p422 missing binary-search shorter-candidates route",
                        "Binary Search Shorter Candidates"),
                "binary-search shorter-candidates route");
    }

    @Test
    void forceRefreshRejectsMinimumCommonValueWithoutLowerBoundJumpsWithoutReplacingCache() throws IOException {
        assertRejectedMinimumCommonValueProblemPreservesCache(
                "Remote p422 missing alternating lower-bound jumps route",
                minimumCommonValueProblemWithoutSolution("Remote p422 missing alternating lower-bound jumps route",
                        "Alternating Lower-Bound Jumps"),
                "alternating lower-bound jumps route");
    }

    private void assertRejectedMinimumCommonValueProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Common Value in Two Sorted Arrays", repository.getProblems().get(421).getTitle());

        remote.publishProblem(422, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p422.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p422 Minimum Common Value content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Minimum Common Value in Two Sorted Arrays", repository.getProblems().get(421).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p422.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Minimum Common Value in Two Sorted Arrays"));
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

    private static String minimumCommonValueProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMinimumCommonValueProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder value without comparing the two sorted arrays while still being "
                        + "long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Solution { public int getCommon(int[] nums1, int[] nums2) { return -1; } }");
        return GSON.toJson(problem);
    }

    private static String minimumCommonValueProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMinimumCommonValueProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("-1000000000")) {
                editedTests.add(testCase("nums1 = [2], nums2 = [2]", "2"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P422 baseline test set no longer contains the extreme-value coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String minimumCommonValueProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMinimumCommonValueProblem();
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
            throw new IOException("P422 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMinimumCommonValueProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p422.json");
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
        solution.addProperty("name", "Minimum Common Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder value without searching for any common value.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Minimum Common Value teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Solution { public int getCommon(int[] nums1, int[] nums2) { return -1; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
