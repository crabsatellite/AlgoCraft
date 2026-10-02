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

class OfficialRepositoryCircularArrayLoopTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftySeventhReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedCircularArrayLoopProblemPreservesCache(
                "Remote p457 missing reviewed solution section",
                circularArrayLoopProblemWithInvalidFirstSolution("Remote p457 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsCircularArrayLoopWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedCircularArrayLoopProblemPreservesCache(
                "Remote p457 weak tests",
                circularArrayLoopProblemWithWeakTests("Remote p457 weak tests"),
                "Circular Array Loop tests must cover");
    }

    @Test
    void forceRefreshRejectsCircularArrayLoopWithoutFastSlowRouteWithoutReplacingCache() throws IOException {
        assertRejectedCircularArrayLoopProblemPreservesCache(
                "Remote p457 missing fast-slow route",
                circularArrayLoopProblemWithoutSolution("Remote p457 missing fast-slow route",
                        "Fast and Slow Pointers with In-Place Marking"),
                "fast/slow in-place marking route");
    }

    @Test
    void forceRefreshRejectsCircularArrayLoopWithoutColorTraversalRouteWithoutReplacingCache() throws IOException {
        assertRejectedCircularArrayLoopProblemPreservesCache(
                "Remote p457 missing color route",
                circularArrayLoopProblemWithoutSolution("Remote p457 missing color route",
                        "Visited Color Array Traversal"),
                "visited color-array traversal route");
    }

    @Test
    void forceRefreshRejectsCircularArrayLoopWithoutPathIndexMapRouteWithoutReplacingCache() throws IOException {
        assertRejectedCircularArrayLoopProblemPreservesCache(
                "Remote p457 missing path-index route",
                circularArrayLoopProblemWithoutSolution("Remote p457 missing path-index route",
                        "Path Index Map Detection"),
                "path-index map detection route");
    }

    @Test
    void forceRefreshRejectsCircularArrayLoopWithoutTimestampRouteWithoutReplacingCache() throws IOException {
        assertRejectedCircularArrayLoopProblemPreservesCache(
                "Remote p457 missing timestamp route",
                circularArrayLoopProblemWithoutSolution("Remote p457 missing timestamp route",
                        "Timestamped Path Arrays"),
                "timestamped path-array route");
    }

    private void assertRejectedCircularArrayLoopProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Circular Array Loop", repository.getProblems().get(456).getTitle());

        remote.publishProblem(457, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p457.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p457 circular-array-loop content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Circular Array Loop", repository.getProblems().get(456).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p457.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Circular Array Loop"));
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

    private static String circularArrayLoopProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validCircularArrayLoopProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false for every input without checking direction consistency or self loops, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String circularArrayLoopProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validCircularArrayLoopProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "nums = [5,1,1,1,1]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("nums = [2,-1,1,2,2]", "true"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P457 baseline test set no longer contains the self-loop tail case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String circularArrayLoopProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validCircularArrayLoopProblem();
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
            throw new IOException("P457 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validCircularArrayLoopProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p457.json");
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
        solution.addProperty("name", "Circular Array Loop Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false and do not model same-direction cycles, wraparound, or self-loop rejection.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required circular-array-loop teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public boolean circularArrayLoop(int[] nums) { return false; } }";
    }
}
