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

class OfficialRepositoryBinaryTreeTiltTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtySeventhReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedBinaryTreeTiltProblemPreservesCache(
                "Remote p467 missing reviewed solution section",
                binaryTreeTiltProblemWithInvalidFirstSolution("Remote p467 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsBinaryTreeTiltWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedBinaryTreeTiltProblemPreservesCache(
                "Remote p467 weak tests",
                binaryTreeTiltProblemWithWeakTests("Remote p467 weak tests"),
                "Binary Tree Tilt tests must cover");
    }

    @Test
    void forceRefreshRejectsBinaryTreeTiltWithoutRecursiveAccumulatorRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedBinaryTreeTiltProblemPreservesCache(
                "Remote p467 missing recursive accumulator route",
                binaryTreeTiltProblemWithoutSolution(
                        "Remote p467 missing recursive accumulator route", "Recursive Postorder Accumulator"),
                "recursive postorder accumulator route");
    }

    @Test
    void forceRefreshRejectsBinaryTreeTiltWithoutRecursivePairRouteWithoutReplacingCache() throws IOException {
        assertRejectedBinaryTreeTiltProblemPreservesCache(
                "Remote p467 missing recursive pair route",
                binaryTreeTiltProblemWithoutSolution(
                        "Remote p467 missing recursive pair route", "Recursive Sum and Tilt Pair"),
                "recursive sum-and-tilt pair route");
    }

    @Test
    void forceRefreshRejectsBinaryTreeTiltWithoutTwoStackRouteWithoutReplacingCache() throws IOException {
        assertRejectedBinaryTreeTiltProblemPreservesCache(
                "Remote p467 missing two-stack route",
                binaryTreeTiltProblemWithoutSolution(
                        "Remote p467 missing two-stack route", "Two-Stack Iterative Postorder"),
                "two-stack iterative postorder route");
    }

    @Test
    void forceRefreshRejectsBinaryTreeTiltWithoutVisitedFlagRouteWithoutReplacingCache() throws IOException {
        assertRejectedBinaryTreeTiltProblemPreservesCache(
                "Remote p467 missing visited-flag route",
                binaryTreeTiltProblemWithoutSolution(
                        "Remote p467 missing visited-flag route", "Visited-Flag Iterative Postorder"),
                "visited-flag iterative postorder route");
    }

    private void assertRejectedBinaryTreeTiltProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Binary Tree Tilt", repository.getProblems().get(466).getTitle());

        remote.publishProblem(467, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p467.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p467 binary tree tilt content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Binary Tree Tilt", repository.getProblems().get(466).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p467.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Binary Tree Tilt"));
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

    private static String binaryTreeTiltProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validBinaryTreeTiltProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without computing child subtree sums, while still being long enough "
                        + "for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String binaryTreeTiltProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validBinaryTreeTiltProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "root = [0,-3,5,-2,-1,4,6]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("root = [1,2,3]", "1"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P467 baseline test set no longer contains the negative-value tree case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String binaryTreeTiltProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validBinaryTreeTiltProblem();
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
            throw new IOException("P467 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validBinaryTreeTiltProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p467.json");
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
        solution.addProperty("name", "Binary Tree Tilt Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without computing the reviewed child-sum teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for the tilt recurrence.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int findTilt(TreeNode root) { return 0; } }";
    }
}
