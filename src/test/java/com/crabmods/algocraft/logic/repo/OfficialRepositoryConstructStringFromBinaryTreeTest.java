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

class OfficialRepositoryConstructStringFromBinaryTreeTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSeventyFifthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedConstructStringFromBinaryTreeProblemPreservesCache(
                "Remote p475 missing reviewed solution section",
                constructStringFromBinaryTreeProblemWithInvalidFirstSolution(
                        "Remote p475 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsConstructStringFromBinaryTreeWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedConstructStringFromBinaryTreeProblemPreservesCache(
                "Remote p475 weak tests",
                constructStringFromBinaryTreeProblemWithWeakTests("Remote p475 weak tests"),
                "Construct String from Binary Tree tests must cover");
    }

    @Test
    void forceRefreshRejectsConstructStringFromBinaryTreeWithoutStringBuilderRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedConstructStringFromBinaryTreeProblemPreservesCache(
                "Remote p475 missing StringBuilder route",
                constructStringFromBinaryTreeProblemWithoutSolution(
                        "Remote p475 missing StringBuilder route", "Recursive StringBuilder Preorder"),
                "recursive StringBuilder preorder route");
    }

    @Test
    void forceRefreshRejectsConstructStringFromBinaryTreeWithoutReturnedStringRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedConstructStringFromBinaryTreeProblemPreservesCache(
                "Remote p475 missing returned-string route",
                constructStringFromBinaryTreeProblemWithoutSolution(
                        "Remote p475 missing returned-string route", "Recursive Returned Strings"),
                "recursive returned-string route");
    }

    @Test
    void forceRefreshRejectsConstructStringFromBinaryTreeWithoutStackTokenRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedConstructStringFromBinaryTreeProblemPreservesCache(
                "Remote p475 missing stack-token route",
                constructStringFromBinaryTreeProblemWithoutSolution(
                        "Remote p475 missing stack-token route", "Explicit Stack Token Simulation"),
                "explicit stack token-simulation route");
    }

    private void assertRejectedConstructStringFromBinaryTreeProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Construct String from Binary Tree", repository.getProblems().get(474).getTitle());

        remote.publishProblem(475, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p475.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p475 tree-string content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Construct String from Binary Tree", repository.getProblems().get(474).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p475.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Construct String from Binary Tree"));
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

    private static String constructStringFromBinaryTreeProblemWithInvalidFirstSolution(String title)
            throws IOException {
        JsonObject problem = validConstructStringFromBinaryTreeProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty string without preserving the tree-to-string mapping, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String constructStringFromBinaryTreeProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validConstructStringFromBinaryTreeProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "root = [1,2,null,null,3]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("root = [1,2,3,4]", "\"1(2(4))(3)\""));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P475 baseline test set no longer contains the missing-left nested case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String constructStringFromBinaryTreeProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validConstructStringFromBinaryTreeProblem();
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
            throw new IOException("P475 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validConstructStringFromBinaryTreeProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p475.json");
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
        solution.addProperty("name", "Construct String Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty string without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for preserving binary-tree structure.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public String tree2str(TreeNode root) { return \"\"; } }";
    }
}
