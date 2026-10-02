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

class OfficialRepositoryWalkingRobotSimulationTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftySixthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedWalkingRobotProblemPreservesCache(
                "Remote p456 missing reviewed solution section",
                walkingRobotProblemWithInvalidFirstSolution("Remote p456 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsWalkingRobotWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedWalkingRobotProblemPreservesCache(
                "Remote p456 weak tests",
                walkingRobotProblemWithWeakTests("Remote p456 weak tests"),
                "Walking Robot Simulation tests must cover");
    }

    @Test
    void forceRefreshRejectsWalkingRobotWithoutEncodedHashSetRouteWithoutReplacingCache() throws IOException {
        assertRejectedWalkingRobotProblemPreservesCache(
                "Remote p456 missing encoded route",
                walkingRobotProblemWithoutSolution("Remote p456 missing encoded route",
                        "Encoded HashSet Step Simulation"),
                "encoded HashSet step-simulation route");
    }

    @Test
    void forceRefreshRejectsWalkingRobotWithoutVectorRotationRouteWithoutReplacingCache() throws IOException {
        assertRejectedWalkingRobotProblemPreservesCache(
                "Remote p456 missing vector route",
                walkingRobotProblemWithoutSolution("Remote p456 missing vector route",
                        "Direction Vector Rotation Simulation"),
                "direction-vector rotation route");
    }

    @Test
    void forceRefreshRejectsWalkingRobotWithoutTreeSetAxisRouteWithoutReplacingCache() throws IOException {
        assertRejectedWalkingRobotProblemPreservesCache(
                "Remote p456 missing TreeSet route",
                walkingRobotProblemWithoutSolution("Remote p456 missing TreeSet route",
                        "Axis Obstacle TreeSets"),
                "TreeSet axis-jump route");
    }

    @Test
    void forceRefreshRejectsWalkingRobotWithoutSortedBinarySearchRouteWithoutReplacingCache() throws IOException {
        assertRejectedWalkingRobotProblemPreservesCache(
                "Remote p456 missing binary-search route",
                walkingRobotProblemWithoutSolution("Remote p456 missing binary-search route",
                        "Sorted Axis Lists With Binary Search"),
                "sorted-list binary-search axis-jump route");
    }

    private void assertRejectedWalkingRobotProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Walking Robot Simulation", repository.getProblems().get(455).getTitle());

        remote.publishProblem(456, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p456.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p456 walking-robot content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Walking Robot Simulation", repository.getProblems().get(455).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p456.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Walking Robot Simulation"));
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

    private static String walkingRobotProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validWalkingRobotProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Pretend the robot never moves and return zero without modeling obstacle blocking, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String walkingRobotProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validWalkingRobotProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "commands = [9,9,-1,9], obstacles = [[30000,30000],[-30000,-30000]]"
                    .equals(test.get("input").getAsString())) {
                editedTests.add(testCase("commands = [4,-1,3], obstacles = []", "25"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P456 baseline test set no longer contains the far-obstacle case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String walkingRobotProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validWalkingRobotProblem();
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
            throw new IOException("P456 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validWalkingRobotProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p456.json");
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
        solution.addProperty("name", "Walking Robot Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero and do not simulate commands, directions, or obstacle blocking.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required walking-robot teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int robotSim(int[] commands, int[][] obstacles) { return 0; } }";
    }
}
