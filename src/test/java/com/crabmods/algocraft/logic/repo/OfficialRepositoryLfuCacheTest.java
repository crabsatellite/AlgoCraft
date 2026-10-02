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

class OfficialRepositoryLfuCacheTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtyEighthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 missing reviewed solution section",
                lfuCacheProblemWithInvalidFirstSolution("Remote p438 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsLfuCacheWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 weak tests",
                lfuCacheProblemWithWeakTests("Remote p438 weak tests"),
                "LFU Cache tests must cover");
    }

    @Test
    void forceRefreshRejectsLfuCacheTreeSetSolutionWithoutReplacingCache() throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 TreeSet route",
                lfuCacheProblemWithTreeSetSolution("Remote p438 TreeSet route"),
                "not use TreeSet");
    }

    @Test
    void forceRefreshRejectsLfuCacheWithoutLinkedHashSetBucketsWithoutReplacingCache() throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 missing LinkedHashSet route",
                lfuCacheProblemWithoutSolution(
                        "Remote p438 missing LinkedHashSet route",
                        "LinkedHashSet Frequency Buckets"),
                "LinkedHashSet frequency-bucket route");
    }

    @Test
    void forceRefreshRejectsLfuCacheWithoutLinkedHashMapBucketsWithoutReplacingCache() throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 missing LinkedHashMap route",
                lfuCacheProblemWithoutSolution(
                        "Remote p438 missing LinkedHashMap route",
                        "LinkedHashMap Value Buckets"),
                "LinkedHashMap value-bucket route");
    }

    @Test
    void forceRefreshRejectsLfuCacheWithoutIntrusiveNodeListsWithoutReplacingCache() throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 missing intrusive node-list route",
                lfuCacheProblemWithoutSolution(
                        "Remote p438 missing intrusive node-list route",
                        "Intrusive Node Lists Per Frequency"),
                "intrusive node-list-per-frequency route");
    }

    @Test
    void forceRefreshRejectsLfuCacheWithoutFrequencyBucketListWithoutReplacingCache() throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 missing frequency bucket-list route",
                lfuCacheProblemWithoutSolution(
                        "Remote p438 missing frequency bucket-list route",
                        "Frequency Bucket Linked List"),
                "frequency-bucket linked-list route");
    }

    @Test
    void forceRefreshRejectsLfuCacheWithoutDirectAddressTableWithoutReplacingCache() throws IOException {
        assertRejectedLfuCacheProblemPreservesCache(
                "Remote p438 missing direct-address route",
                lfuCacheProblemWithoutSolution(
                        "Remote p438 missing direct-address route",
                        "Direct Address Key Table With Frequency Buckets"),
                "direct-address key-table route");
    }

    private void assertRejectedLfuCacheProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("LFU Cache", repository.getProblems().get(437).getTitle());

        remote.publishProblem(438, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p438.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p438 LFU Cache content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("LFU Cache", repository.getProblems().get(437).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p438.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("LFU Cache"));
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

    private static String lfuCacheProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validLfuCacheProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return without storing any cache state, while still being long enough for the generic "
                        + "teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String lfuCacheProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validLfuCacheProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[[1],[1,1],[1],[2,2],[1],[2]]")) {
                editedTests.add(testCase(
                        "[\"LFUCache\",\"put\",\"get\",\"put\",\"get\"]\n[[0],[1,1],[1],[2,2],[2]]",
                        "[null,null,-1,null,-1]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P438 baseline test set no longer contains the capacity-one case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String lfuCacheProblemWithTreeSetSolution(String title) throws IOException {
        JsonObject problem = validLfuCacheProblem();
        problem.addProperty("title", title);
        problem.getAsJsonArray("solutions").set(0, treeSetSolution());
        return GSON.toJson(problem);
    }

    private static String lfuCacheProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validLfuCacheProblem();
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
            throw new IOException("P438 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validLfuCacheProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p438.json");
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject().deepCopy();
    }

    private static JsonObject testCase(String input, String output) {
        JsonObject test = new JsonObject();
        test.addProperty("input", input);
        test.addProperty("output", output);
        return test;
    }

    private static JsonObject treeSetSolution() {
        JsonObject solution = fallbackSolution();
        solution.addProperty("name", "TreeSet Ordered by Frequency and Time");
        solution.addProperty("timeComplexity", "O(log n) per operation");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Store entries in a TreeSet ordered by frequency and time.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally violates the O(1) LFU requirement.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(log n)\n"
                        + "- Space Complexity: O(capacity)");
        solution.addProperty("code", "class LFUCache { private java.util.TreeSet<Integer> order = new java.util.TreeSet<>(); public LFUCache(int capacity) {} public int get(int key) { return -1; } public void put(int key, int value) {} }");
        return solution;
    }

    private static JsonObject fallbackSolution() {
        JsonObject solution = new JsonObject();
        solution.addProperty("name", "LFU Cache Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore every cache operation.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required LFU Cache teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class LFUCache { public LFUCache(int capacity) {} public int get(int key) { return -1; } public void put(int key, int value) {} }";
    }
}
