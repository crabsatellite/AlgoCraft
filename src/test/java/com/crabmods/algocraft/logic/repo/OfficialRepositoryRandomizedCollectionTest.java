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

class OfficialRepositoryRandomizedCollectionTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortiethReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedRandomizedCollectionProblemPreservesCache(
                "Remote p440 missing reviewed solution section",
                randomizedCollectionProblemWithInvalidFirstSolution(
                        "Remote p440 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsRandomizedCollectionWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedRandomizedCollectionProblemPreservesCache(
                "Remote p440 weak tests",
                randomizedCollectionProblemWithWeakTests("Remote p440 weak tests"),
                "RandomizedCollection tests must cover");
    }

    @Test
    void forceRefreshRejectsRandomizedCollectionWithoutLinkedHashSetBucketsWithoutReplacingCache()
            throws IOException {
        assertRejectedRandomizedCollectionProblemPreservesCache(
                "Remote p440 missing LinkedHashSet route",
                randomizedCollectionProblemWithoutSolution(
                        "Remote p440 missing LinkedHashSet route",
                        "ArrayList With LinkedHashSet Index Buckets"),
                "LinkedHashSet index-bucket route");
    }

    @Test
    void forceRefreshRejectsRandomizedCollectionWithoutDynamicIntArrayWithoutReplacingCache()
            throws IOException {
        assertRejectedRandomizedCollectionProblemPreservesCache(
                "Remote p440 missing dynamic int-array route",
                randomizedCollectionProblemWithoutSolution(
                        "Remote p440 missing dynamic int-array route",
                        "Dynamic Int Array With Index Sets"),
                "dynamic int-array index-set route");
    }

    @Test
    void forceRefreshRejectsRandomizedCollectionWithoutDenseSlotHashMapsWithoutReplacingCache()
            throws IOException {
        assertRejectedRandomizedCollectionProblemPreservesCache(
                "Remote p440 missing dense slot route",
                randomizedCollectionProblemWithoutSolution(
                        "Remote p440 missing dense slot route",
                        "Dense Slot HashMaps With Index Sets"),
                "dense-slot HashMaps with index-sets route");
    }

    @Test
    void forceRefreshRejectsRandomizedCollectionWithoutOccurrenceEntryDenseArrayWithoutReplacingCache()
            throws IOException {
        assertRejectedRandomizedCollectionProblemPreservesCache(
                "Remote p440 missing occurrence-entry route",
                randomizedCollectionProblemWithoutSolution(
                        "Remote p440 missing occurrence-entry route",
                        "Occurrence Entry Dense Array"),
                "occurrence-entry dense-array route");
    }

    @Test
    void forceRefreshRejectsRandomizedCollectionWithoutIndexBagBackPointersWithoutReplacingCache()
            throws IOException {
        assertRejectedRandomizedCollectionProblemPreservesCache(
                "Remote p440 missing index-bag route",
                randomizedCollectionProblemWithoutSolution(
                        "Remote p440 missing index-bag route",
                        "Index Bag With Back Pointers"),
                "index-bag with back-pointers route");
    }

    private void assertRejectedRandomizedCollectionProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals(
                "Insert Delete GetRandom O(1) - Duplicates allowed",
                repository.getProblems().get(439).getTitle());

        remote.publishProblem(440, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p440.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p440 RandomizedCollection content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals(
                "Insert Delete GetRandom O(1) - Duplicates allowed",
                repository.getProblems().get(439).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p440.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Insert Delete GetRandom O(1) - Duplicates allowed"));
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

    private static String randomizedCollectionProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validRandomizedCollectionProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Always return false and ignore the duplicate occurrence invariant, while still being "
                        + "long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String randomizedCollectionProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validRandomizedCollectionProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("-2147483648")) {
                editedTests.add(testCase(
                        "[\"RandomizedCollection\",\"insert\",\"remove\",\"remove\",\"insert\",\"getRandom\"]\n"
                                + "[[],[7],[7],[7],[7],[]]",
                        "[null,true,true,false,true,7]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P440 baseline test set no longer contains the integer-limits case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String randomizedCollectionProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validRandomizedCollectionProblem();
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
            throw new IOException("P440 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validRandomizedCollectionProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p440.json");
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
        solution.addProperty("name", "RandomizedCollection Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore every multiset operation.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required RandomizedCollection teaching "
                        + "routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class RandomizedCollection { public RandomizedCollection() {} public boolean insert(int val) { return false; } public boolean remove(int val) { return false; } public int getRandom() { return 0; } }";
    }
}
