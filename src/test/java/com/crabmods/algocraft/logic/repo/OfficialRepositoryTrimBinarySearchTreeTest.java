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

class OfficialRepositoryTrimBinarySearchTreeTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFiveHundredthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedTrimBinarySearchTreeProblemPreservesCache(
                "Remote p500 missing reviewed solution section",
                trimBinarySearchTreeProblemWithInvalidFirstSolution("Remote p500 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsTrimBinarySearchTreeWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedTrimBinarySearchTreeProblemPreservesCache(
                "Remote p500 weak tests",
                trimBinarySearchTreeProblemWithWeakTests("Remote p500 weak tests"),
                "Trim a Binary Search Tree tests must cover BST pruning boundaries");
    }

    @Test
    void forceRefreshRejectsTrimBinarySearchTreeWithoutRecursiveRouteWithoutReplacingCache() throws IOException {
        assertRejectedTrimBinarySearchTreeProblemPreservesCache(
                "Remote p500 missing recursive route",
                trimBinarySearchTreeProblemWithoutSolution("Remote p500 missing recursive route",
                        "Recursive Range-Pruning DFS"),
                "recursive range-pruning route");
    }

    @Test
    void forceRefreshRejectsTrimBinarySearchTreeWithoutIterativeRelinkingRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedTrimBinarySearchTreeProblemPreservesCache(
                "Remote p500 missing iterative relinking",
                trimBinarySearchTreeProblemWithoutSolution("Remote p500 missing iterative relinking",
                        "Iterative Root and Side Relinking"),
                "iterative root and side relinking route");
    }

    @Test
    void forceRefreshRejectsTrimBinarySearchTreeWithoutStackRewireRouteWithoutReplacingCache() throws IOException {
        assertRejectedTrimBinarySearchTreeProblemPreservesCache(
                "Remote p500 missing stack rewire",
                trimBinarySearchTreeProblemWithoutSolution("Remote p500 missing stack rewire",
                        "Stack-Based Rewire Traversal"),
                "stack-based rewire traversal route");
    }

    @Test
    void forceRefreshRejectsTrimBinarySearchTreeWithoutClonedTrimRouteWithoutReplacingCache() throws IOException {
        assertRejectedTrimBinarySearchTreeProblemPreservesCache(
                "Remote p500 missing cloned trim",
                trimBinarySearchTreeProblemWithoutSolution("Remote p500 missing cloned trim",
                        "Non-Mutating Cloned Trim"),
                "non-mutating cloned trim route");
    }

    private void assertRejectedTrimBinarySearchTreeProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Trim a Binary Search Tree", repository.getProblems().get(499).getTitle());

        remote.publishProblem(500, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p500.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p500 trim-bst content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Trim a Binary Search Tree", repository.getProblems().get(499).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p500.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Trim a Binary Search Tree"));
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

    private static String trimBinarySearchTreeProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validTrimBinarySearchTreeProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the root without explaining BST subtree pruning.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String trimBinarySearchTreeProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validTrimBinarySearchTreeProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "root = [1], low = 2, high = 3".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("root = [1], low = 1, high = 1", "[1]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P500 baseline test set no longer contains the empty-output singleton case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String trimBinarySearchTreeProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validTrimBinarySearchTreeProblem();
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
            throw new IOException("P500 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validTrimBinarySearchTreeProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p500.json");
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
        solution.addProperty("name", "Trim Binary Search Tree Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the original root without pruning subtrees.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores the BST ordering and trimming bounds.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public TreeNode trimBST(TreeNode root, int low, int high) { return root; } }";
    }
}
