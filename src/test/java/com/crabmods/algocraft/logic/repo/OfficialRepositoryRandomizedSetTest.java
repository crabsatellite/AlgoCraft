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

class OfficialRepositoryRandomizedSetTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtyNinthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedRandomizedSetProblemPreservesCache(
                "Remote p439 missing reviewed solution section",
                randomizedSetProblemWithInvalidFirstSolution("Remote p439 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsRandomizedSetWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedRandomizedSetProblemPreservesCache(
                "Remote p439 weak tests",
                randomizedSetProblemWithWeakTests("Remote p439 weak tests"),
                "RandomizedSet tests must cover");
    }

    @Test
    void forceRefreshRejectsRandomizedSetWithoutArrayListIndexMapWithoutReplacingCache() throws IOException {
        assertRejectedRandomizedSetProblemPreservesCache(
                "Remote p439 missing ArrayList route",
                randomizedSetProblemWithoutSolution("Remote p439 missing ArrayList route", "ArrayList Index Map"),
                "ArrayList index-map route");
    }

    @Test
    void forceRefreshRejectsRandomizedSetWithoutDynamicIntArrayWithoutReplacingCache() throws IOException {
        assertRejectedRandomizedSetProblemPreservesCache(
                "Remote p439 missing dynamic int-array route",
                randomizedSetProblemWithoutSolution(
                        "Remote p439 missing dynamic int-array route",
                        "Dynamic Int Array Index Map"),
                "dynamic int-array index-map route");
    }

    @Test
    void forceRefreshRejectsRandomizedSetWithoutDenseSlotHashMapsWithoutReplacingCache() throws IOException {
        assertRejectedRandomizedSetProblemPreservesCache(
                "Remote p439 missing dense slot route",
                randomizedSetProblemWithoutSolution("Remote p439 missing dense slot route", "Dense Slot HashMaps"),
                "dense-slot HashMaps route");
    }

    @Test
    void forceRefreshRejectsRandomizedSetWithoutEntryObjectDenseArrayWithoutReplacingCache() throws IOException {
        assertRejectedRandomizedSetProblemPreservesCache(
                "Remote p439 missing entry-object route",
                randomizedSetProblemWithoutSolution(
                        "Remote p439 missing entry-object route",
                        "Entry Object Dense Array"),
                "entry-object dense-array route");
    }

    @Test
    void forceRefreshRejectsRandomizedSetWithoutPrimitiveOpenAddressingWithoutReplacingCache() throws IOException {
        assertRejectedRandomizedSetProblemPreservesCache(
                "Remote p439 missing primitive open-addressing route",
                randomizedSetProblemWithoutSolution(
                        "Remote p439 missing primitive open-addressing route",
                        "Primitive Open Addressing Index Map"),
                "primitive open-addressing index-map route");
    }

    private void assertRejectedRandomizedSetProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Insert Delete GetRandom O(1)", repository.getProblems().get(438).getTitle());

        remote.publishProblem(439, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p439.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p439 RandomizedSet content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Insert Delete GetRandom O(1)", repository.getProblems().get(438).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p439.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Insert Delete GetRandom O(1)"));
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

    private static String randomizedSetProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validRandomizedSetProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Always return false and ignore the random-member invariant, while still being long enough "
                        + "for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String randomizedSetProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validRandomizedSetProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("-2147483648")) {
                editedTests.add(testCase(
                        "[\"RandomizedSet\",\"insert\",\"remove\",\"insert\",\"getRandom\"]\n[[],[7],[7],[7],[]]",
                        "[null,true,true,true,7]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P439 baseline test set no longer contains the integer-limits case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String randomizedSetProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validRandomizedSetProblem();
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
            throw new IOException("P439 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validRandomizedSetProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p439.json");
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
        solution.addProperty("name", "RandomizedSet Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore every set operation.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required RandomizedSet teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class RandomizedSet { public RandomizedSet() {} public boolean insert(int val) { return false; } public boolean remove(int val) { return false; } public int getRandom() { return 0; } }";
    }
}
