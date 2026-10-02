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

class OfficialRepositoryFoodRatingSystemTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortySixthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedFoodRatingsProblemPreservesCache(
                "Remote p446 missing reviewed solution section",
                foodRatingsProblemWithInvalidFirstSolution("Remote p446 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsFoodRatingsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedFoodRatingsProblemPreservesCache(
                "Remote p446 weak tests",
                foodRatingsProblemWithWeakTests("Remote p446 weak tests"),
                "FoodRatings tests must cover");
    }

    @Test
    void forceRefreshRejectsFoodRatingsWithoutOrderedSetRouteWithoutReplacingCache() throws IOException {
        assertRejectedFoodRatingsProblemPreservesCache(
                "Remote p446 missing ordered-set route",
                foodRatingsProblemWithoutSolution(
                        "Remote p446 missing ordered-set route",
                        "Ordered Set per Cuisine"),
                "ordered-set per cuisine route");
    }

    @Test
    void forceRefreshRejectsFoodRatingsWithoutLazyHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedFoodRatingsProblemPreservesCache(
                "Remote p446 missing lazy-heap route",
                foodRatingsProblemWithoutSolution(
                        "Remote p446 missing lazy-heap route",
                        "Lazy Heap with Current-Rating Map"),
                "lazy-heap current-rating route");
    }

    @Test
    void forceRefreshRejectsFoodRatingsWithoutRatingBucketsRouteWithoutReplacingCache() throws IOException {
        assertRejectedFoodRatingsProblemPreservesCache(
                "Remote p446 missing rating-buckets route",
                foodRatingsProblemWithoutSolution(
                        "Remote p446 missing rating-buckets route",
                        "Rating Buckets per Cuisine"),
                "rating-buckets per cuisine route");
    }

    @Test
    void forceRefreshRejectsFoodRatingsWithoutIndexedHeapRouteWithoutReplacingCache() throws IOException {
        assertRejectedFoodRatingsProblemPreservesCache(
                "Remote p446 missing indexed-heap route",
                foodRatingsProblemWithoutSolution(
                        "Remote p446 missing indexed-heap route",
                        "Indexed Binary Heap per Cuisine"),
                "indexed binary-heap route");
    }

    @Test
    void forceRefreshRejectsFoodRatingsWithoutSquareRootBlocksRouteWithoutReplacingCache() throws IOException {
        assertRejectedFoodRatingsProblemPreservesCache(
                "Remote p446 missing square-root block route",
                foodRatingsProblemWithoutSolution(
                        "Remote p446 missing square-root block route",
                        "Square-Root Cuisine Blocks"),
                "square-root cuisine-blocks route");
    }

    private void assertRejectedFoodRatingsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Food Rating System", repository.getProblems().get(445).getTitle());

        remote.publishProblem(446, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p446.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p446 FoodRatings content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Food Rating System", repository.getProblems().get(445).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p446.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design Food Rating System"));
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

    private static String foodRatingsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validFoodRatingsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Scan the whole problem once and return whichever food looked best initially, while still "
                        + "being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String foodRatingsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validFoodRatingsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("100000000")) {
                editedTests.add(testCase(
                        "[\"FoodRatings\", \"highestRated\", \"highestRated\", \"changeRating\", \"highestRated\", \"changeRating\", \"highestRated\"]\n"
                                + "[[[\"kimchi\", \"miso\", \"sushi\", \"moussaka\", \"ramen\", \"bulgogi\"], [\"korean\", \"japanese\", \"japanese\", \"greek\", \"japanese\", \"korean\"], [9, 12, 8, 15, 14, 7]], [\"korean\"], [\"japanese\"], [\"sushi\", 16], [\"japanese\"], [\"ramen\", 16], [\"japanese\"]]",
                        "[null, \"kimchi\", \"ramen\", null, \"sushi\", null, \"ramen\"]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P446 baseline test set no longer contains the rating boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String foodRatingsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validFoodRatingsProblem();
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
            throw new IOException("P446 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validFoodRatingsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p446.json");
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
        solution.addProperty("name", "FoodRatings Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore all rating updates and return an empty string for every cuisine.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required FoodRatings teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class FoodRatings { public FoodRatings(String[] foods, String[] cuisines, int[] ratings) {} public void changeRating(String food, int newRating) {} public String highestRated(String cuisine) { return \"\"; } }";
    }
}
