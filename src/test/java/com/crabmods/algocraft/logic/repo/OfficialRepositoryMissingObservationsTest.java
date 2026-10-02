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

class OfficialRepositoryMissingObservationsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftyFifthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedMissingObservationsProblemPreservesCache(
                "Remote p455 missing reviewed solution section",
                missingObservationsProblemWithInvalidFirstSolution("Remote p455 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsMissingObservationsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedMissingObservationsProblemPreservesCache(
                "Remote p455 weak tests",
                missingObservationsProblemWithWeakTests("Remote p455 weak tests"),
                "Find Missing Observations tests must cover");
    }

    @Test
    void forceRefreshRejectsMissingObservationsWithoutQuotientRemainderRouteWithoutReplacingCache()
            throws IOException {
        assertRejectedMissingObservationsProblemPreservesCache(
                "Remote p455 missing quotient route",
                missingObservationsProblemWithoutSolution("Remote p455 missing quotient route",
                        "Even Quotient Remainder Distribution"),
                "quotient-remainder distribution route");
    }

    @Test
    void forceRefreshRejectsMissingObservationsWithoutOnesTopUpRouteWithoutReplacingCache() throws IOException {
        assertRejectedMissingObservationsProblemPreservesCache(
                "Remote p455 missing ones top-up route",
                missingObservationsProblemWithoutSolution("Remote p455 missing ones top-up route",
                        "Start From Ones Greedy Top-Up"),
                "ones plus greedy top-up route");
    }

    @Test
    void forceRefreshRejectsMissingObservationsWithoutLowerBoundRouteWithoutReplacingCache() throws IOException {
        assertRejectedMissingObservationsProblemPreservesCache(
                "Remote p455 missing lower-bound route",
                missingObservationsProblemWithoutSolution("Remote p455 missing lower-bound route",
                        "Feasibility-Preserving Lower Bound Fill"),
                "lower-bound feasibility fill route");
    }

    @Test
    void forceRefreshRejectsMissingObservationsWithoutUpperReserveRouteWithoutReplacingCache() throws IOException {
        assertRejectedMissingObservationsProblemPreservesCache(
                "Remote p455 missing upper-reserve route",
                missingObservationsProblemWithoutSolution("Remote p455 missing upper-reserve route",
                        "Upper Reserve Fill"),
                "upper-reserve fill route");
    }

    private void assertRejectedMissingObservationsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Find Missing Observations", repository.getProblems().get(454).getTitle());

        remote.publishProblem(455, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p455.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p455 missing-observations content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Find Missing Observations", repository.getProblems().get(454).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p455.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Find Missing Observations"));
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

    private static String missingObservationsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validMissingObservationsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty array without computing the required missing dice sum, while still "
                        + "being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String missingObservationsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validMissingObservationsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "rolls = [6,1,5], mean = 3, n = 10".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("rolls = [3,2,4,3], mean = 4, n = 2", "[6,6]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P455 baseline test set no longer contains the large-n case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String missingObservationsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validMissingObservationsProblem();
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
            throw new IOException("P455 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validMissingObservationsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p455.json");
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
        solution.addProperty("name", "Missing Observations Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return an empty array and do not model dice feasibility.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required missing-observations teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int[] missingRolls(int[] rolls, int mean, int n) { return new int[0]; } }";
    }
}
