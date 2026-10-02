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

class OfficialRepositoryIslandPerimeterTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtiethReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedIslandPerimeterProblemPreservesCache(
                "Remote p460 missing reviewed solution section",
                islandPerimeterProblemWithInvalidFirstSolution("Remote p460 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsIslandPerimeterWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedIslandPerimeterProblemPreservesCache(
                "Remote p460 weak tests",
                islandPerimeterProblemWithWeakTests("Remote p460 weak tests"),
                "Island Perimeter tests must cover");
    }

    @Test
    void forceRefreshRejectsIslandPerimeterWithoutExposedBoundaryRouteWithoutReplacingCache() throws IOException {
        assertRejectedIslandPerimeterProblemPreservesCache(
                "Remote p460 missing exposed-boundary route",
                islandPerimeterProblemWithoutSolution("Remote p460 missing exposed-boundary route",
                        "Exposed Boundary Scan"),
                "exposed-boundary scan route");
    }

    @Test
    void forceRefreshRejectsIslandPerimeterWithoutSharedEdgeRouteWithoutReplacingCache() throws IOException {
        assertRejectedIslandPerimeterProblemPreservesCache(
                "Remote p460 missing shared-edge route",
                islandPerimeterProblemWithoutSolution("Remote p460 missing shared-edge route",
                        "Land Cells Minus Shared Edges"),
                "shared-edge formula route");
    }

    @Test
    void forceRefreshRejectsIslandPerimeterWithoutDfsRouteWithoutReplacingCache() throws IOException {
        assertRejectedIslandPerimeterProblemPreservesCache(
                "Remote p460 missing DFS route",
                islandPerimeterProblemWithoutSolution("Remote p460 missing DFS route",
                        "DFS Boundary Count"),
                "DFS boundary-count route");
    }

    @Test
    void forceRefreshRejectsIslandPerimeterWithoutBfsRouteWithoutReplacingCache() throws IOException {
        assertRejectedIslandPerimeterProblemPreservesCache(
                "Remote p460 missing BFS route",
                islandPerimeterProblemWithoutSolution("Remote p460 missing BFS route",
                        "BFS Boundary Count"),
                "BFS boundary-count route");
    }

    private void assertRejectedIslandPerimeterProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Island Perimeter", repository.getProblems().get(459).getTitle());

        remote.publishProblem(460, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p460.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p460 island-perimeter content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Island Perimeter", repository.getProblems().get(459).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p460.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Island Perimeter"));
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

    private static String islandPerimeterProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validIslandPerimeterProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero for every grid without checking boundary edges, shared edges, "
                        + "or graph traversal, while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String islandPerimeterProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validIslandPerimeterProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "grid = [[0,1,0],[1,1,1],[0,1,0]]"
                    .equals(test.get("input").getAsString())) {
                editedTests.add(testCase("grid = [[1]]", "4"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P460 baseline test set no longer contains the T-shape case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String islandPerimeterProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validIslandPerimeterProblem();
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
            throw new IOException("P460 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validIslandPerimeterProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p460.json");
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
        solution.addProperty("name", "Island Perimeter Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without counting exposed edges, shared edges, DFS boundaries, or BFS boundaries.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required island-perimeter teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int islandPerimeter(int[][] grid) { return 0; } }";
    }
}
