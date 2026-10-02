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

class OfficialRepositoryNumberContainerSystemTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortySeventhReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedNumberContainersProblemPreservesCache(
                "Remote p447 missing reviewed solution section",
                numberContainersProblemWithInvalidFirstSolution("Remote p447 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsNumberContainersWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedNumberContainersProblemPreservesCache(
                "Remote p447 weak tests",
                numberContainersProblemWithWeakTests("Remote p447 weak tests"),
                "NumberContainers tests must cover");
    }

    @Test
    void forceRefreshRejectsNumberContainersWithoutTreeSetRouteWithoutReplacingCache() throws IOException {
        assertRejectedNumberContainersProblemPreservesCache(
                "Remote p447 missing TreeSet route",
                numberContainersProblemWithoutSolution(
                        "Remote p447 missing TreeSet route",
                        "TreeSet of Indices per Number"),
                "TreeSet indices per number route");
    }

    @Test
    void forceRefreshRejectsNumberContainersWithoutLazyHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedNumberContainersProblemPreservesCache(
                "Remote p447 missing lazy-heap route",
                numberContainersProblemWithoutSolution(
                        "Remote p447 missing lazy-heap route",
                        "Lazy Min-Heap per Number"),
                "lazy min-heap per number route");
    }

    @Test
    void forceRefreshRejectsNumberContainersWithoutGlobalRecordRouteWithoutReplacingCache() throws IOException {
        assertRejectedNumberContainersProblemPreservesCache(
                "Remote p447 missing global-record route",
                numberContainersProblemWithoutSolution(
                        "Remote p447 missing global-record route",
                        "Global Ordered Number-Index Records"),
                "global ordered number-index records route");
    }

    @Test
    void forceRefreshRejectsNumberContainersWithoutIndexedHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedNumberContainersProblemPreservesCache(
                "Remote p447 missing indexed-heap route",
                numberContainersProblemWithoutSolution(
                        "Remote p447 missing indexed-heap route",
                        "Indexed Min-Heap per Number"),
                "indexed min-heap per number route");
    }

    @Test
    void forceRefreshRejectsNumberContainersWithoutDynamicSegmentTreeRouteWithoutReplacingCache() throws IOException {
        assertRejectedNumberContainersProblemPreservesCache(
                "Remote p447 missing dynamic-segment-tree route",
                numberContainersProblemWithoutSolution(
                        "Remote p447 missing dynamic-segment-tree route",
                        "Dynamic Segment Tree per Number"),
                "dynamic segment-tree per number route");
    }

    private void assertRejectedNumberContainersProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Number Container System", repository.getProblems().get(446).getTitle());

        remote.publishProblem(447, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p447.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p447 NumberContainers content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Number Container System", repository.getProblems().get(446).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p447.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design Number Container System"));
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

    private static String numberContainersProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validNumberContainersProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the smallest index that ever held a number without removing replaced indices, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String numberContainersProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validNumberContainersProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("1000000000")) {
                editedTests.add(testCase(
                        "[\"NumberContainers\", \"find\", \"change\", \"change\", \"change\", \"change\", \"find\", \"change\", \"find\"]\n"
                                + "[[], [10], [2, 10], [1, 10], [3, 10], [5, 10], [10], [1, 20], [10]]",
                        "[null, -1, null, null, null, null, 1, null, 2]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P447 baseline test set no longer contains the sparse boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String numberContainersProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validNumberContainersProblem();
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
            throw new IOException("P447 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validNumberContainersProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p447.json");
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
        solution.addProperty("name", "NumberContainers Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore all updates and return `-1` for every query.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required NumberContainers teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class NumberContainers { public NumberContainers() {} public void change(int index, int number) {} public int find(int number) { return -1; } }";
    }
}
