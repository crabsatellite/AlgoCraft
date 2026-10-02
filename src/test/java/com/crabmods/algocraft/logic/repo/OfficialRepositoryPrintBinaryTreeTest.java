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

class OfficialRepositoryPrintBinaryTreeTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredNinetiethReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedPrintBinaryTreeProblemPreservesCache(
                "Remote p490 missing reviewed solution section",
                printBinaryTreeProblemWithInvalidFirstSolution("Remote p490 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsPrintBinaryTreeWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedPrintBinaryTreeProblemPreservesCache(
                "Remote p490 weak tests",
                printBinaryTreeProblemWithWeakTests("Remote p490 weak tests"),
                "Print Binary Tree tests must cover formatted layout boundaries");
    }

    @Test
    void forceRefreshRejectsPrintBinaryTreeWithoutDfsOffsetRouteWithoutReplacingCache() throws IOException {
        assertRejectedPrintBinaryTreeProblemPreservesCache(
                "Remote p490 missing DFS offset",
                printBinaryTreeProblemWithoutSolution(
                        "Remote p490 missing DFS offset", "DFS Height and Offset Placement"),
                "DFS height-and-offset placement route");
    }

    @Test
    void forceRefreshRejectsPrintBinaryTreeWithoutBfsCoordinateRouteWithoutReplacingCache() throws IOException {
        assertRejectedPrintBinaryTreeProblemPreservesCache(
                "Remote p490 missing BFS coordinates",
                printBinaryTreeProblemWithoutSolution(
                        "Remote p490 missing BFS coordinates", "BFS Queue With Coordinates"),
                "BFS coordinate-queue route");
    }

    @Test
    void forceRefreshRejectsPrintBinaryTreeWithoutIntervalRouteWithoutReplacingCache() throws IOException {
        assertRejectedPrintBinaryTreeProblemPreservesCache(
                "Remote p490 missing interval recursion",
                printBinaryTreeProblemWithoutSolution(
                        "Remote p490 missing interval recursion", "Column Interval Recursion"),
                "column-interval recursion route");
    }

    @Test
    void forceRefreshRejectsPrintBinaryTreeWithoutCoordinateCollectionRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedPrintBinaryTreeProblemPreservesCache(
                "Remote p490 missing coordinate collection",
                printBinaryTreeProblemWithoutSolution(
                        "Remote p490 missing coordinate collection", "Coordinate Collection Then Materialize"),
                "coordinate-collection materialization route");
    }

    private void assertRejectedPrintBinaryTreeProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Print Binary Tree", repository.getProblems().get(489).getTitle());

        remote.publishProblem(490, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p490.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p490 print-binary-tree content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Print Binary Tree", repository.getProblems().get(489).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p490.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Print Binary Tree"));
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

    private static String printBinaryTreeProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validPrintBinaryTreeProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty matrix without explaining formatted tree placement.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String printBinaryTreeProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validPrintBinaryTreeProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "root = [1,null,2,null,3,null,4]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("root = [1]", "[[\"1\"]]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P490 baseline test set no longer contains the deep right-chain formatting case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String printBinaryTreeProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validPrintBinaryTreeProblem();
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
            throw new IOException("P490 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validPrintBinaryTreeProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p490.json");
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
        solution.addProperty("name", "Print Binary Tree Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty matrix without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores tree height, node coordinates, and matrix width.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public java.util.List<java.util.List<String>> printTree(TreeNode root) { "
                + "return new java.util.ArrayList<>(); } }";
    }
}
