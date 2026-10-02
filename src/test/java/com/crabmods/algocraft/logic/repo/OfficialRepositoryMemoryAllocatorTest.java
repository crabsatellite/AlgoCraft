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

class OfficialRepositoryMemoryAllocatorTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFifteenthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignMemoryAllocatorProblemPreservesCache(
                "Remote p415 missing reviewed solution section",
                memoryAllocatorProblemWithInvalidFirstSolution(
                        "Remote p415 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsDesignMemoryAllocatorWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedDesignMemoryAllocatorProblemPreservesCache(
                "Remote p415 weak tests",
                memoryAllocatorProblemWithWeakTests("Remote p415 weak tests"),
                "Design Memory Allocator tests must cover");
    }

    @Test
    void forceRefreshRejectsDesignMemoryAllocatorWithoutDirectCellArrayWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignMemoryAllocatorProblemPreservesCache(
                "Remote p415 missing direct array route",
                memoryAllocatorProblemWithoutSolution(
                        "Remote p415 missing direct array route",
                        "Direct Cell Array First Fit"),
                "direct cell-array first-fit route");
    }

    @Test
    void forceRefreshRejectsDesignMemoryAllocatorWithoutBitSetWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignMemoryAllocatorProblemPreservesCache(
                "Remote p415 missing BitSet route",
                memoryAllocatorProblemWithoutSolution(
                        "Remote p415 missing BitSet route",
                        "BitSet Free Run Search"),
                "BitSet free-run search route");
    }

    @Test
    void forceRefreshRejectsDesignMemoryAllocatorWithoutAllocatedIntervalTreeMapWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignMemoryAllocatorProblemPreservesCache(
                "Remote p415 missing allocated interval route",
                memoryAllocatorProblemWithoutSolution(
                        "Remote p415 missing allocated interval route",
                        "Allocated Interval TreeMap"),
                "allocated-interval TreeMap gap route");
    }

    @Test
    void forceRefreshRejectsDesignMemoryAllocatorWithoutFreeIntervalTreeMapWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignMemoryAllocatorProblemPreservesCache(
                "Remote p415 missing free interval route",
                memoryAllocatorProblemWithoutSolution(
                        "Remote p415 missing free interval route",
                        "TreeMap Free Intervals with Coalescing"),
                "coalescing free-interval TreeMap route");
    }

    @Test
    void forceRefreshRejectsDesignMemoryAllocatorWithoutSegmentTreeWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignMemoryAllocatorProblemPreservesCache(
                "Remote p415 missing segment tree route",
                memoryAllocatorProblemWithoutSolution(
                        "Remote p415 missing segment tree route",
                        "Segment Tree Leftmost Free Block"),
                "segment-tree leftmost free-block route");
    }

    private void assertRejectedDesignMemoryAllocatorProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Memory Allocator", repository.getProblems().get(414).getTitle());

        remote.publishProblem(415, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p415.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p415 Design Memory Allocator content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Memory Allocator", repository.getProblems().get(414).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p415.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design Memory Allocator"));
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

    private static String memoryAllocatorProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMemoryAllocatorProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a deliberately invalid placeholder that never records ownership and always "
                        + "claims allocation failed, while keeping enough detail for the basic description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Allocator { public Allocator(int n) {} "
                        + "public int allocate(int size, int mID) { return -1; } "
                        + "public int free(int mID) { return 0; } }");
        return GSON.toJson(problem);
    }

    private static String memoryAllocatorProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMemoryAllocatorProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[[1],[1,7]")) {
                editedTests.add(testCase(
                        "[\"Allocator\",\"allocate\"]\n[[1],[1,1]]",
                        "[null,0]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P415 baseline test set no longer contains the one-cell coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String memoryAllocatorProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validMemoryAllocatorProblem();
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
            throw new IOException("P415 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMemoryAllocatorProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p415.json");
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
        solution.addProperty("name", "Memory Allocator Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return fixed placeholder values without storing memory ownership.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Design Memory Allocator teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Allocator { public Allocator(int n) {} "
                        + "public int allocate(int size, int mID) { return -1; } "
                        + "public int free(int mID) { return 0; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
