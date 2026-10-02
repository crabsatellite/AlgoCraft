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

class OfficialRepositoryNeighboringBitwiseXorValidityTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSeventyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedNeighboringBitwiseXorValidityProblemPreservesCache(
                "Remote p474 missing reviewed solution section",
                neighboringBitwiseXorValidityProblemWithInvalidFirstSolution(
                        "Remote p474 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsNeighboringBitwiseXorValidityWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedNeighboringBitwiseXorValidityProblemPreservesCache(
                "Remote p474 weak tests",
                neighboringBitwiseXorValidityProblemWithWeakTests("Remote p474 weak tests"),
                "Neighboring Bitwise XOR Validity tests must cover");
    }

    @Test
    void forceRefreshRejectsNeighboringBitwiseXorValidityWithoutParityRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedNeighboringBitwiseXorValidityProblemPreservesCache(
                "Remote p474 missing parity route",
                neighboringBitwiseXorValidityProblemWithoutSolution(
                        "Remote p474 missing parity route", "XOR Parity Invariant"),
                "XOR parity-invariant route");
    }

    @Test
    void forceRefreshRejectsNeighboringBitwiseXorValidityWithoutZeroStartRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedNeighboringBitwiseXorValidityProblemPreservesCache(
                "Remote p474 missing zero-start route",
                neighboringBitwiseXorValidityProblemWithoutSolution(
                        "Remote p474 missing zero-start route", "Construct Zero-Start Candidate"),
                "zero-start candidate-construction route");
    }

    @Test
    void forceRefreshRejectsNeighboringBitwiseXorValidityWithoutTwoStartRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedNeighboringBitwiseXorValidityProblemPreservesCache(
                "Remote p474 missing two-start route",
                neighboringBitwiseXorValidityProblemWithoutSolution(
                        "Remote p474 missing two-start route", "Try Both Starting Bits"),
                "two-start rolling-simulation route");
    }

    private void assertRejectedNeighboringBitwiseXorValidityProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Neighboring Bitwise XOR Validity", repository.getProblems().get(473).getTitle());

        remote.publishProblem(474, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p474.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p474 neighboring-xor content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Neighboring Bitwise XOR Validity", repository.getProblems().get(473).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p474.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Neighboring Bitwise XOR Validity"));
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

    private static String neighboringBitwiseXorValidityProblemWithInvalidFirstSolution(String title)
            throws IOException {
        JsonObject problem = validNeighboringBitwiseXorValidityProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false without checking the circular XOR equations, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String neighboringBitwiseXorValidityProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validNeighboringBitwiseXorValidityProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "derived = [1,0,1,0,1]".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("derived = [1,0]", "false"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P474 baseline test set no longer contains the odd alternating false case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String neighboringBitwiseXorValidityProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validNeighboringBitwiseXorValidityProblem();
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
            throw new IOException("P474 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validNeighboringBitwiseXorValidityProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p474.json");
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
        solution.addProperty("name", "Neighboring XOR Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return false without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for circular XOR validation.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public boolean doesValidArrayExist(int[] derived) { return false; } }";
    }
}
