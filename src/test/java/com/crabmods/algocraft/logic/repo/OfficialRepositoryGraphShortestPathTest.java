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

class OfficialRepositoryGraphShortestPathTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSeventeenthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignGraphProblemPreservesCache(
                "Remote p417 missing reviewed solution section",
                graphProblemWithInvalidFirstSolution("Remote p417 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsDesignGraphWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedDesignGraphProblemPreservesCache(
                "Remote p417 weak tests",
                graphProblemWithWeakTests("Remote p417 weak tests"),
                "Design Graph With Shortest Path Calculator tests must cover");
    }

    @Test
    void forceRefreshRejectsDesignGraphWithoutAdjacencyListDijkstraWithoutReplacingCache() throws IOException {
        assertRejectedDesignGraphProblemPreservesCache(
                "Remote p417 missing adjacency-list Dijkstra route",
                graphProblemWithoutSolution(
                        "Remote p417 missing adjacency-list Dijkstra route",
                        "Dijkstra with Adjacency List"),
                "adjacency-list Dijkstra route");
    }

    @Test
    void forceRefreshRejectsDesignGraphWithoutDenseMatrixDijkstraWithoutReplacingCache() throws IOException {
        assertRejectedDesignGraphProblemPreservesCache(
                "Remote p417 missing dense-matrix Dijkstra route",
                graphProblemWithoutSolution(
                        "Remote p417 missing dense-matrix Dijkstra route",
                        "Dense Matrix Dijkstra"),
                "dense-matrix Dijkstra route");
    }

    @Test
    void forceRefreshRejectsDesignGraphWithoutIncrementalFloydWithoutReplacingCache() throws IOException {
        assertRejectedDesignGraphProblemPreservesCache(
                "Remote p417 missing incremental Floyd route",
                graphProblemWithoutSolution(
                        "Remote p417 missing incremental Floyd route",
                        "Floyd-Warshall with Incremental Edge Update"),
                "incremental Floyd-Warshall route");
    }

    @Test
    void forceRefreshRejectsDesignGraphWithoutBellmanFordWithoutReplacingCache() throws IOException {
        assertRejectedDesignGraphProblemPreservesCache(
                "Remote p417 missing Bellman-Ford route",
                graphProblemWithoutSolution(
                        "Remote p417 missing Bellman-Ford route",
                        "Bellman-Ford on Edge List"),
                "Bellman-Ford edge-list route");
    }

    @Test
    void forceRefreshRejectsDesignGraphWithoutCachedDijkstraWithoutReplacingCache() throws IOException {
        assertRejectedDesignGraphProblemPreservesCache(
                "Remote p417 missing cached Dijkstra route",
                graphProblemWithoutSolution(
                        "Remote p417 missing cached Dijkstra route",
                        "Cached Dijkstra by Source"),
                "cached Dijkstra route");
    }

    private void assertRejectedDesignGraphProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Graph With Shortest Path Calculator", repository.getProblems().get(416).getTitle());

        remote.publishProblem(417, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p417.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p417 Design Graph content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Graph With Shortest Path Calculator", repository.getProblems().get(416).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p417.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design Graph With Shortest Path Calculator"));
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

    private static String graphProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validGraphProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a deliberately invalid placeholder that ignores every edge and returns no path "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Graph { public Graph(int n, int[][] edges) {} "
                        + "public void addEdge(int[] edge) {} "
                        + "public int shortestPath(int node1, int node2) { return -1; } }");
        return GSON.toJson(problem);
    }

    private static String graphProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validGraphProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[[3,[]]")) {
                editedTests.add(testCase(
                        "[\"Graph\",\"shortestPath\"]\n[[1,[]],[0,0]]",
                        "[null,0]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P417 baseline test set no longer contains the empty graph coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String graphProblemWithoutSolution(String title, String missingSolutionName) throws IOException {
        JsonObject problem = validGraphProblem();
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
            throw new IOException("P417 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validGraphProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p417.json");
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
        solution.addProperty("name", "Design Graph Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return placeholder values without storing graph edges or computing shortest paths.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Design Graph teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Graph { public Graph(int n, int[][] edges) {} "
                        + "public void addEdge(int[] edge) {} "
                        + "public int shortestPath(int node1, int node2) { return -1; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
