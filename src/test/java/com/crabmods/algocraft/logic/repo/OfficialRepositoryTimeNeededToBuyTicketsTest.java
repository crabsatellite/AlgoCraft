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

class OfficialRepositoryTimeNeededToBuyTicketsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentySixthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
                "Remote p426 missing reviewed solution section",
                timeNeededToBuyTicketsProblemWithInvalidFirstSolution(
                        "Remote p426 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsTimeNeededToBuyTicketsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
                "Remote p426 weak tests",
                timeNeededToBuyTicketsProblemWithWeakTests("Remote p426 weak tests"),
                "Time Needed to Buy Tickets tests must cover");
    }

    @Test
    void forceRefreshRejectsTimeNeededToBuyTicketsWithoutSinglePassFormulaWithoutReplacingCache()
            throws IOException {
        assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
                "Remote p426 missing single-pass capped-contribution route",
                timeNeededToBuyTicketsProblemWithoutSolution(
                        "Remote p426 missing single-pass capped-contribution route",
                        "Single Pass Capped Contribution"),
                "single-pass capped-contribution route");
    }

    @Test
    void forceRefreshRejectsTimeNeededToBuyTicketsWithoutSplitCapsWithoutReplacingCache() throws IOException {
        assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
                "Remote p426 missing split prefix/suffix caps route",
                timeNeededToBuyTicketsProblemWithoutSolution(
                        "Remote p426 missing split prefix/suffix caps route",
                        "Split Prefix and Suffix Caps"),
                "split prefix/suffix caps route");
    }

    @Test
    void forceRefreshRejectsTimeNeededToBuyTicketsWithoutQueueSimulationWithoutReplacingCache()
            throws IOException {
        assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
                "Remote p426 missing queue-simulation route",
                timeNeededToBuyTicketsProblemWithoutSolution(
                        "Remote p426 missing queue-simulation route", "Queue Simulation"),
                "queue-simulation route");
    }

    @Test
    void forceRefreshRejectsTimeNeededToBuyTicketsWithoutCyclicArraySimulationWithoutReplacingCache()
            throws IOException {
        assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
                "Remote p426 missing cyclic-array simulation route",
                timeNeededToBuyTicketsProblemWithoutSolution(
                        "Remote p426 missing cyclic-array simulation route", "Cyclic Array Simulation"),
                "cyclic-array simulation route");
    }

    @Test
    void forceRefreshRejectsTimeNeededToBuyTicketsWithoutRoundLayerCountingWithoutReplacingCache()
            throws IOException {
        assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
                "Remote p426 missing round-layer counting route",
                timeNeededToBuyTicketsProblemWithoutSolution(
                        "Remote p426 missing round-layer counting route", "Round Layer Counting"),
                "round-layer counting route");
    }

    private void assertRejectedTimeNeededToBuyTicketsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Time Needed to Buy Tickets", repository.getProblems().get(425).getTitle());

        remote.publishProblem(426, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p426.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p426 Time Needed to Buy Tickets content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Time Needed to Buy Tickets", repository.getProblems().get(425).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p426.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Time Needed to Buy Tickets"));
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

    private static String timeNeededToBuyTicketsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validTimeNeededToBuyTicketsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the target ticket count without accounting for anyone else in the queue, while "
                        + "still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Solution { public int timeRequiredToBuy(int[] tickets, int k) { return tickets[k]; } }");
        return GSON.toJson(problem);
    }

    private static String timeNeededToBuyTicketsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validTimeNeededToBuyTicketsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[3,3,3]")) {
                editedTests.add(testCase("tickets = [1,1], k = 0", "1"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P426 baseline test set no longer contains the equal-demands case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String timeNeededToBuyTicketsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validTimeNeededToBuyTicketsProblem();
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
            throw new IOException("P426 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validTimeNeededToBuyTicketsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p426.json");
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
        solution.addProperty("name", "Tickets Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the target person's ticket count without modeling the queue.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Time Needed to Buy Tickets "
                        + "teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Solution { public int timeRequiredToBuy(int[] tickets, int k) { return tickets[k]; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
