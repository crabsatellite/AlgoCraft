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

class OfficialRepositoryDota2SenateTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredEightyFifthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedSenateProblemPreservesCache(
                "Remote p485 missing reviewed solution section",
                senateProblemWithInvalidFirstSolution("Remote p485 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsSenateWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedSenateProblemPreservesCache(
                "Remote p485 weak tests",
                senateProblemWithWeakTests("Remote p485 weak tests"),
                "Dota2 Senate tests must lock down");
    }

    @Test
    void forceRefreshRejectsSenateWithoutTwoQueueRouteWithoutReplacingCache() throws IOException {
        assertRejectedSenateProblemPreservesCache(
                "Remote p485 missing two queues",
                senateProblemWithoutSolution("Remote p485 missing two queues", "Two Queues Greedy"),
                "two-queue index greedy route");
    }

    @Test
    void forceRefreshRejectsSenateWithoutSingleQueueRouteWithoutReplacingCache() throws IOException {
        assertRejectedSenateProblemPreservesCache(
                "Remote p485 missing single queue",
                senateProblemWithoutSolution("Remote p485 missing single queue", "Single Queue Pending Bans"),
                "single-queue pending-ban route");
    }

    @Test
    void forceRefreshRejectsSenateWithoutBooleanSimulationRouteWithoutReplacingCache() throws IOException {
        assertRejectedSenateProblemPreservesCache(
                "Remote p485 missing boolean simulation",
                senateProblemWithoutSolution("Remote p485 missing boolean simulation", "Boolean Array Round Simulation"),
                "boolean-array round simulation route");
    }

    @Test
    void forceRefreshRejectsSenateWithoutRoundBufferRouteWithoutReplacingCache() throws IOException {
        assertRejectedSenateProblemPreservesCache(
                "Remote p485 missing round buffer",
                senateProblemWithoutSolution("Remote p485 missing round buffer", "Round Buffer Simulation"),
                "round-buffer simulation route");
    }

    private void assertRejectedSenateProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Dota2 Senate", repository.getProblems().get(484).getTitle());

        remote.publishProblem(485, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p485.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p485 senate content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Dota2 Senate", repository.getProblems().get(484).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p485.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Dota2 Senate"));
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

    private static String senateProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validSenateProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant without preserving circular voting order.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String senateProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validSenateProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "senate = \"DRRDRDRDRDDRDRDR\"".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("senate = \"RD\"", "\"Radiant\""));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P485 baseline test set no longer contains the long circular-order case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String senateProblemWithoutSolution(String title, String missingSolutionName) throws IOException {
        JsonObject problem = validSenateProblem();
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
            throw new IOException("P485 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validSenateProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p485.json");
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
        solution.addProperty("name", "Senate Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a constant value without applying any of the required reviewed teaching routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture intentionally ignores circular turn order and pending bans.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public String predictPartyVictory(String senate) { return \"Radiant\"; } }";
    }
}
