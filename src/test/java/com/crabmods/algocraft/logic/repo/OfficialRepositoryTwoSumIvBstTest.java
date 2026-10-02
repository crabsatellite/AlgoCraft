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

class OfficialRepositoryTwoSumIvBstTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightyEighthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedTwoSumIvBstProblemPreservesCache(
                "Remote p488 missing reviewed solution section",
                twoSumIvBstProblemWithInvalidFirstSolution("Remote p488 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsTwoSumIvBstWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedTwoSumIvBstProblemPreservesCache(
                "Remote p488 weak tests",
                twoSumIvBstProblemWithWeakTests("Remote p488 weak tests"),
                "Two Sum IV tests must cover BST two-sum boundaries");
    }

    @Test
    void forceRefreshRejectsTwoSumIvBstWithoutDfsHashSetRouteWithoutReplacingCache() throws IOException {
        assertRejectedTwoSumIvBstProblemPreservesCache(
                "Remote p488 missing DFS hash set",
                twoSumIvBstProblemWithoutSolution("Remote p488 missing DFS hash set", "DFS HashSet Complements"),
                "DFS hash-set complement route");
    }

    @Test
    void forceRefreshRejectsTwoSumIvBstWithoutBfsHashSetRouteWithoutReplacingCache() throws IOException {
        assertRejectedTwoSumIvBstProblemPreservesCache(
                "Remote p488 missing BFS hash set",
                twoSumIvBstProblemWithoutSolution("Remote p488 missing BFS hash set", "BFS HashSet Complements"),
                "BFS hash-set complement route");
    }

    @Test
    void forceRefreshRejectsTwoSumIvBstWithoutInorderTwoPointerRouteWithoutReplacingCache() throws IOException {
        assertRejectedTwoSumIvBstProblemPreservesCache(
                "Remote p488 missing inorder two pointers",
                twoSumIvBstProblemWithoutSolution(
                        "Remote p488 missing inorder two pointers", "Inorder List and Two Pointers"),
                "inorder-list two-pointer route");
    }

    @Test
    void forceRefreshRejectsTwoSumIvBstWithoutDualIteratorRouteWithoutReplacingCache() throws IOException {
        assertRejectedTwoSumIvBstProblemPreservesCache(
                "Remote p488 missing dual iterator",
                twoSumIvBstProblemWithoutSolution("Remote p488 missing dual iterator", "Dual BST Iterators"),
                "dual BST iterator route");
    }

    @Test
    void forceRefreshRejectsTwoSumIvBstWithoutComplementSearchRouteWithoutReplacingCache() throws IOException {
        assertRejectedTwoSumIvBstProblemPreservesCache(
                "Remote p488 missing complement search",
                twoSumIvBstProblemWithoutSolution(
                        "Remote p488 missing complement search", "Search Each Complement in the BST"),
                "direct BST complement-search route");
    }

    private void assertRejectedTwoSumIvBstProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Two Sum IV - Input is a BST", repository.getProblems().get(487).getTitle());

        remote.publishProblem(488, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p488.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p488 BST two-sum content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Two Sum IV - Input is a BST", repository.getProblems().get(487).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p488.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Two Sum IV - Input is a BST"));
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

    private static String twoSumIvBstProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validTwoSumIvBstProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false without checking any BST node values.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String twoSumIvBstProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validTwoSumIvBstProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "root = [2,1,4], k = 4".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("root = [2,1,3], k = 5", "true"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P488 baseline test set no longer contains the self-pair trap case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String twoSumIvBstProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validTwoSumIvBstProblem();
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
            throw new IOException("P488 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validTwoSumIvBstProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p488.json");
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
        solution.addProperty("name", "Two Sum IV Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores BST ordering, complement lookup, and self-pair rules.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public boolean findTarget(TreeNode root, int k) { return false; } }";
    }
}
