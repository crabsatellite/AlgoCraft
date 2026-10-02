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

class OfficialRepositoryHammingDistanceTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtyFirstReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedHammingDistanceProblemPreservesCache(
                "Remote p461 missing reviewed solution section",
                hammingDistanceProblemWithInvalidFirstSolution("Remote p461 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsHammingDistanceWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedHammingDistanceProblemPreservesCache(
                "Remote p461 weak tests",
                hammingDistanceProblemWithWeakTests("Remote p461 weak tests"),
                "Hamming Distance tests must cover");
    }

    @Test
    void forceRefreshRejectsHammingDistanceWithoutLibraryBitCountRouteWithoutReplacingCache() throws IOException {
        assertRejectedHammingDistanceProblemPreservesCache(
                "Remote p461 missing library bit-count route",
                hammingDistanceProblemWithoutSolution("Remote p461 missing library bit-count route",
                        "XOR and Library Bit Count"),
                "XOR library bit-count route");
    }

    @Test
    void forceRefreshRejectsHammingDistanceWithoutKernighanRouteWithoutReplacingCache() throws IOException {
        assertRejectedHammingDistanceProblemPreservesCache(
                "Remote p461 missing Kernighan route",
                hammingDistanceProblemWithoutSolution("Remote p461 missing Kernighan route",
                        "Brian Kernighan Bit Clearing"),
                "Kernighan bit-clearing route");
    }

    @Test
    void forceRefreshRejectsHammingDistanceWithoutFixedWidthRouteWithoutReplacingCache() throws IOException {
        assertRejectedHammingDistanceProblemPreservesCache(
                "Remote p461 missing fixed-width route",
                hammingDistanceProblemWithoutSolution("Remote p461 missing fixed-width route",
                        "Fixed-Width Bit Scan"),
                "fixed-width bit-scan route");
    }

    @Test
    void forceRefreshRejectsHammingDistanceWithoutNibbleLookupRouteWithoutReplacingCache() throws IOException {
        assertRejectedHammingDistanceProblemPreservesCache(
                "Remote p461 missing nibble lookup route",
                hammingDistanceProblemWithoutSolution("Remote p461 missing nibble lookup route",
                        "Nibble Lookup Table"),
                "nibble lookup-table route");
    }

    private void assertRejectedHammingDistanceProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Hamming Distance", repository.getProblems().get(460).getTitle());

        remote.publishProblem(461, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p461.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p461 hamming-distance content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Hamming Distance", repository.getProblems().get(460).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p461.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Hamming Distance"));
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

    private static String hammingDistanceProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validHammingDistanceProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero for every input without XOR, bit counting, scanning, or lookup decomposition, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String hammingDistanceProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validHammingDistanceProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "x = 1431655765, y = 715827882".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("x = 1, y = 4", "2"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P461 baseline test set no longer contains the alternating-bit case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String hammingDistanceProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validHammingDistanceProblem();
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
            throw new IOException("P461 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validHammingDistanceProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p461.json");
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
        solution.addProperty("name", "Hamming Distance Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without XOR, bit counting, Kernighan clearing, bit scanning, or lookup tables.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Hamming-distance teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int hammingDistance(int x, int y) { return 0; } }";
    }
}
