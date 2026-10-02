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

class OfficialRepositoryStockPriceTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedStockPriceProblemPreservesCache(
                "Remote p443 missing reviewed solution section",
                stockPriceProblemWithInvalidFirstSolution("Remote p443 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsStockPriceWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedStockPriceProblemPreservesCache(
                "Remote p443 weak tests",
                stockPriceProblemWithWeakTests("Remote p443 weak tests"),
                "StockPrice tests must cover");
    }

    @Test
    void forceRefreshRejectsStockPriceWithoutTreeMapPriceCountRouteWithoutReplacingCache() throws IOException {
        assertRejectedStockPriceProblemPreservesCache(
                "Remote p443 missing TreeMap price-count route",
                stockPriceProblemWithoutSolution(
                        "Remote p443 missing TreeMap price-count route",
                        "HashMap with TreeMap Price Counts"),
                "TreeMap price-count route");
    }

    @Test
    void forceRefreshRejectsStockPriceWithoutDualLazyHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedStockPriceProblemPreservesCache(
                "Remote p443 missing dual lazy-heap route",
                stockPriceProblemWithoutSolution(
                        "Remote p443 missing dual lazy-heap route",
                        "Dual Heaps with Lazy Deletion"),
                "dual lazy-heap route");
    }

    @Test
    void forceRefreshRejectsStockPriceWithoutOrderedActiveRecordRouteWithoutReplacingCache() throws IOException {
        assertRejectedStockPriceProblemPreservesCache(
                "Remote p443 missing ordered active-record route",
                stockPriceProblemWithoutSolution(
                        "Remote p443 missing ordered active-record route",
                        "Ordered Active Records TreeSet"),
                "ordered active-record TreeSet route");
    }

    @Test
    void forceRefreshRejectsStockPriceWithoutDynamicSegmentTreeRouteWithoutReplacingCache() throws IOException {
        assertRejectedStockPriceProblemPreservesCache(
                "Remote p443 missing dynamic segment-tree route",
                stockPriceProblemWithoutSolution(
                        "Remote p443 missing dynamic segment-tree route",
                        "Dynamic Segment Tree over Price Values"),
                "dynamic segment-tree price-domain route");
    }

    @Test
    void forceRefreshRejectsStockPriceWithoutOnDemandRebuildRouteWithoutReplacingCache() throws IOException {
        assertRejectedStockPriceProblemPreservesCache(
                "Remote p443 missing on-demand rebuild route",
                stockPriceProblemWithoutSolution(
                        "Remote p443 missing on-demand rebuild route",
                        "HashMap Counts with On-Demand Extremum Rebuild"),
                "on-demand extremum-rebuild baseline route");
    }

    private void assertRejectedStockPriceProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Stock Price Fluctuation", repository.getProblems().get(442).getTitle());

        remote.publishProblem(443, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p443.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p443 StockPrice content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Stock Price Fluctuation", repository.getProblems().get(442).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p443.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Stock Price Fluctuation"));
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

    private static String stockPriceProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validStockPriceProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return fixed extrema without removing corrected historical prices, while still being "
                        + "long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String stockPriceProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validStockPriceProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("1000000000")) {
                editedTests.add(testCase(
                        "[\"StockPrice\", \"update\", \"update\", \"current\", \"maximum\", \"update\", \"maximum\", \"update\", \"minimum\"]\n"
                                + "[[], [1, 10], [2, 5], [], [], [1, 3], [], [4, 2], []]",
                        "[null, null, null, 5, 10, null, 5, null, 2]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P443 baseline test set no longer contains the timestamp/value boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String stockPriceProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validStockPriceProblem();
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
            throw new IOException("P443 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validStockPriceProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p443.json");
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
        solution.addProperty("name", "StockPrice Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore all updates and return constant values.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required StockPrice teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class StockPrice { public StockPrice() {} public void update(int timestamp, int price) {} public int current() { return 0; } public int maximum() { return 0; } public int minimum() { return 0; } }";
    }
}
