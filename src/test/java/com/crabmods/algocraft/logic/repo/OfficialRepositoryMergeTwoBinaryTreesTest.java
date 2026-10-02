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

class OfficialRepositoryMergeTwoBinaryTreesTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSeventySixthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMergeTwoBinaryTreesProblemPreservesCache(
                "Remote p476 missing reviewed solution section",
                mergeTwoBinaryTreesProblemWithInvalidFirstSolution(
                        "Remote p476 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMergeTwoBinaryTreesWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMergeTwoBinaryTreesProblemPreservesCache(
                "Remote p476 weak tests",
                mergeTwoBinaryTreesProblemWithWeakTests("Remote p476 weak tests"),
                "Merge Two Binary Trees tests must cover");
    }

    @Test
    void forceRefreshRejectsMergeTwoBinaryTreesWithoutNewTreeRouteWithoutReplacingCache() throws IOException {
        assertRejectedMergeTwoBinaryTreesProblemPreservesCache(
                "Remote p476 missing new-tree route",
                mergeTwoBinaryTreesProblemWithoutSolution(
                        "Remote p476 missing new-tree route", "Recursive New Tree Merge"),
                "recursive new-tree merge route");
    }

    @Test
    void forceRefreshRejectsMergeTwoBinaryTreesWithoutDfsRouteWithoutReplacingCache() throws IOException {
        assertRejectedMergeTwoBinaryTreesProblemPreservesCache(
                "Remote p476 missing DFS route",
                mergeTwoBinaryTreesProblemWithoutSolution(
                        "Remote p476 missing DFS route", "Recursive In-place DFS Merge"),
                "recursive in-place DFS merge route");
    }

    @Test
    void forceRefreshRejectsMergeTwoBinaryTreesWithoutBfsRouteWithoutReplacingCache() throws IOException {
        assertRejectedMergeTwoBinaryTreesProblemPreservesCache(
                "Remote p476 missing BFS route",
                mergeTwoBinaryTreesProblemWithoutSolution(
                        "Remote p476 missing BFS route", "Iterative BFS In-place Merge"),
                "iterative BFS in-place merge route");
    }

    private void assertRejectedMergeTwoBinaryTreesProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Merge Two Binary Trees", repository.getProblems().get(475).getTitle());

        remote.publishProblem(476, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p476.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p476 tree-merge content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Merge Two Binary Trees", repository.getProblems().get(475).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p476.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Merge Two Binary Trees"));
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

    private static String mergeTwoBinaryTreesProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMergeTwoBinaryTreesProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return one input tree without applying the required positional merge rule, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String mergeTwoBinaryTreesProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMergeTwoBinaryTreesProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "root1 = [], root2 = []".equals(test.get("input").getAsString())) {
                editedTests.add(testCase(
                        "root1 = [1,3,2,5], root2 = [2,1,3,null,4,null,7]",
                        "[3,4,5,5,4,null,7]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P476 baseline test set no longer contains the both-empty case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String mergeTwoBinaryTreesProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validMergeTwoBinaryTreesProblem();
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
            throw new IOException("P476 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMergeTwoBinaryTreesProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p476.json");
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
        solution.addProperty("name", "Merge Trees Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return one input tree without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for merging binary tree positions.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public TreeNode mergeTrees(TreeNode root1, TreeNode root2) { return root1; } }";
    }
}
