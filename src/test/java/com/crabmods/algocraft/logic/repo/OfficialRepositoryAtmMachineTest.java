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

class OfficialRepositoryAtmMachineTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortyFifthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedAtmProblemPreservesCache(
                "Remote p445 missing reviewed solution section",
                atmProblemWithInvalidFirstSolution("Remote p445 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsAtmWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedAtmProblemPreservesCache(
                "Remote p445 weak tests",
                atmProblemWithWeakTests("Remote p445 weak tests"),
                "ATM tests must cover");
    }

    @Test
    void forceRefreshRejectsAtmWithoutFixedArrayPlanRouteWithoutReplacingCache() throws IOException {
        assertRejectedAtmProblemPreservesCache(
                "Remote p445 missing fixed-array plan route",
                atmProblemWithoutSolution(
                        "Remote p445 missing fixed-array plan route",
                        "Fixed Array Greedy Plan"),
                "fixed-array greedy-plan route");
    }

    @Test
    void forceRefreshRejectsAtmWithoutTemporaryInventoryCopyRouteWithoutReplacingCache() throws IOException {
        assertRejectedAtmProblemPreservesCache(
                "Remote p445 missing temporary-copy route",
                atmProblemWithoutSolution(
                        "Remote p445 missing temporary-copy route",
                        "Temporary Inventory Copy"),
                "temporary-inventory-copy route");
    }

    @Test
    void forceRefreshRejectsAtmWithoutInPlaceRollbackRouteWithoutReplacingCache() throws IOException {
        assertRejectedAtmProblemPreservesCache(
                "Remote p445 missing in-place rollback route",
                atmProblemWithoutSolution(
                        "Remote p445 missing in-place rollback route",
                        "In-Place Greedy with Rollback"),
                "in-place greedy rollback route");
    }

    @Test
    void forceRefreshRejectsAtmWithoutRecursivePlannerRouteWithoutReplacingCache() throws IOException {
        assertRejectedAtmProblemPreservesCache(
                "Remote p445 missing recursive planner route",
                atmProblemWithoutSolution(
                        "Remote p445 missing recursive planner route",
                        "Recursive Denomination Planner"),
                "recursive denomination-planner route");
    }

    @Test
    void forceRefreshRejectsAtmWithoutDenominationObjectLedgerRouteWithoutReplacingCache() throws IOException {
        assertRejectedAtmProblemPreservesCache(
                "Remote p445 missing object ledger route",
                atmProblemWithoutSolution(
                        "Remote p445 missing object ledger route",
                        "Denomination Object Ledger"),
                "denomination-object ledger route");
    }

    private void assertRejectedAtmProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design ATM Machine", repository.getProblems().get(444).getTitle());

        remote.publishProblem(445, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p445.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p445 ATM content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design ATM Machine", repository.getProblems().get(444).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p445.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design ATM Machine"));
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

    private static String atmProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validAtmProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Treat the ATM like ordinary coin change and ignore the largest-denomination priority, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String atmProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validAtmProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("50000000")) {
                editedTests.add(testCase(
                        "[\"ATM\", \"deposit\", \"withdraw\", \"deposit\", \"withdraw\", \"withdraw\"]\n"
                                + "[[], [[0,0,1,2,1]], [600], [[0,1,0,1,1]], [600], [550]]",
                        "[null, null, [0,0,1,0,1], null, [-1], [0,1,0,0,1]]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P445 baseline test set no longer contains the large-count boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String atmProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validAtmProblem();
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
            throw new IOException("P445 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validAtmProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p445.json");
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
        solution.addProperty("name", "ATM Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Reject every withdrawal regardless of the deposited notes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required ATM teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class ATM { public ATM() {} public void deposit(int[] banknotesCount) {} public int[] withdraw(int amount) { return new int[] {-1}; } }";
    }
}
