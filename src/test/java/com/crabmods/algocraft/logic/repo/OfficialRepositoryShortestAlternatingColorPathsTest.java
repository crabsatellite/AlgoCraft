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

class OfficialRepositoryShortestAlternatingColorPathsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedShortestAlternatingColorPathsProblemPreservesCache(
                "Remote p434 missing reviewed solution section",
                shortestAlternatingColorPathsProblemWithInvalidFirstSolution(
                        "Remote p434 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsShortestAlternatingColorPathsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedShortestAlternatingColorPathsProblemPreservesCache(
                "Remote p434 weak tests",
                shortestAlternatingColorPathsProblemWithWeakTests("Remote p434 weak tests"),
                "Shortest Alternating Color Paths tests must cover");
    }

    @Test
    void forceRefreshRejectsShortestAlternatingColorPathsWithoutLastColorBfsWithoutReplacingCache()
            throws IOException {
        assertRejectedShortestAlternatingColorPathsProblemPreservesCache(
                "Remote p434 missing last-color BFS route",
                shortestAlternatingColorPathsProblemWithoutSolution(
                        "Remote p434 missing last-color BFS route",
                        "BFS With Last Color State"),
                "last-color BFS state route");
    }

    @Test
    void forceRefreshRejectsShortestAlternatingColorPathsWithoutLayeredGraphBfsWithoutReplacingCache()
            throws IOException {
        assertRejectedShortestAlternatingColorPathsProblemPreservesCache(
                "Remote p434 missing layered graph BFS route",
                shortestAlternatingColorPathsProblemWithoutSolution(
                        "Remote p434 missing layered graph BFS route",
                        "Layered Graph BFS"),
                "layered graph BFS route");
    }

    @Test
    void forceRefreshRejectsShortestAlternatingColorPathsWithoutDijkstraWithoutReplacingCache()
            throws IOException {
        assertRejectedShortestAlternatingColorPathsProblemPreservesCache(
                "Remote p434 missing Dijkstra route",
                shortestAlternatingColorPathsProblemWithoutSolution(
                        "Remote p434 missing Dijkstra route",
                        "Dijkstra On Color State Graph"),
                "Dijkstra color-state graph route");
    }

    @Test
    void forceRefreshRejectsShortestAlternatingColorPathsWithoutBellmanFordWithoutReplacingCache()
            throws IOException {
        assertRejectedShortestAlternatingColorPathsProblemPreservesCache(
                "Remote p434 missing Bellman-Ford route",
                shortestAlternatingColorPathsProblemWithoutSolution(
                        "Remote p434 missing Bellman-Ford route",
                        "State Relaxation Bellman-Ford"),
                "Bellman-Ford state-relaxation route");
    }

    private void assertRejectedShortestAlternatingColorPathsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Shortest Alternating Color Paths", repository.getProblems().get(433).getTitle());

        remote.publishProblem(434, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p434.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p434 Shortest Alternating Color Paths content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Shortest Alternating Color Paths", repository.getProblems().get(433).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p434.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Shortest Alternating Color Paths"));
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

    private static String shortestAlternatingColorPathsProblemWithInvalidFirstSolution(String title)
            throws IOException {
        JsonObject problem = validShortestAlternatingColorPathsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero distances without tracking edge colors, while still being long enough "
                        + "for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(n)\n"
                        + "- Space Complexity: O(n)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String shortestAlternatingColorPathsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validShortestAlternatingColorPathsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains(
                    "n = 4, redEdges = [[0,1],[1,2]], blueEdges = [[0,1],[2,3]]")) {
                editedTests.add(testCase("n = 3, redEdges = [[0,1]], blueEdges = [[1,2]]", "[0,1,2]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P434 baseline test set no longer contains the opposite-color revisit case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String shortestAlternatingColorPathsProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validShortestAlternatingColorPathsProblem();
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
            throw new IOException("P434 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validShortestAlternatingColorPathsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p434.json");
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
        solution.addProperty("name", "Shortest Alternating Paths Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(n)");
        solution.addProperty("spaceComplexity", "O(n)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a zero-filled answer without modeling colored states.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Shortest Alternating Color Paths "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(n)\n"
                        + "- Space Complexity: O(n)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { "
                + "public int[] shortestAlternatingPaths(int n, int[][] redEdges, int[][] blueEdges) { "
                + "return new int[n]; } "
                + "}";
    }
}
