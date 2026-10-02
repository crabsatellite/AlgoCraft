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

class OfficialRepositorySimpleBankTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFortyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedBankProblemPreservesCache(
                "Remote p444 missing reviewed solution section",
                bankProblemWithInvalidFirstSolution("Remote p444 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsBankWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedBankProblemPreservesCache(
                "Remote p444 weak tests",
                bankProblemWithWeakTests("Remote p444 weak tests"),
                "Bank tests must cover");
    }

    @Test
    void forceRefreshRejectsBankWithoutZeroIndexedArrayRouteWithoutReplacingCache() throws IOException {
        assertRejectedBankProblemPreservesCache(
                "Remote p444 missing zero-indexed array route",
                bankProblemWithoutSolution(
                        "Remote p444 missing zero-indexed array route",
                        "Zero-Indexed Array Simulation"),
                "zero-indexed array simulation route");
    }

    @Test
    void forceRefreshRejectsBankWithoutPaddedArrayRouteWithoutReplacingCache() throws IOException {
        assertRejectedBankProblemPreservesCache(
                "Remote p444 missing padded-array route",
                bankProblemWithoutSolution(
                        "Remote p444 missing padded-array route",
                        "One-Indexed Padded Array Ledger"),
                "one-indexed padded-array route");
    }

    @Test
    void forceRefreshRejectsBankWithoutHashMapAccountRouteWithoutReplacingCache() throws IOException {
        assertRejectedBankProblemPreservesCache(
                "Remote p444 missing HashMap account route",
                bankProblemWithoutSolution(
                        "Remote p444 missing HashMap account route",
                        "HashMap Account Table"),
                "HashMap account-table route");
    }

    @Test
    void forceRefreshRejectsBankWithoutAccountObjectRouteWithoutReplacingCache() throws IOException {
        assertRejectedBankProblemPreservesCache(
                "Remote p444 missing account-object route",
                bankProblemWithoutSolution(
                        "Remote p444 missing account-object route",
                        "Account Object Ledger"),
                "account-object ledger route");
    }

    @Test
    void forceRefreshRejectsBankWithoutListGuardRouteWithoutReplacingCache() throws IOException {
        assertRejectedBankProblemPreservesCache(
                "Remote p444 missing List guard route",
                bankProblemWithoutSolution(
                        "Remote p444 missing List guard route",
                        "List Ledger with Transaction Guards"),
                "List ledger with transaction guards route");
    }

    private void assertRejectedBankProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Simple Bank System", repository.getProblems().get(443).getTitle());

        remote.publishProblem(444, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p444.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p444 Bank content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Simple Bank System", repository.getProblems().get(443).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p444.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Simple Bank System"));
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

    private static String bankProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validBankProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Ignore failed transaction atomicity and mutate balances before checks, while still being "
                        + "long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String bankProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validBankProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("1000000000000")) {
                editedTests.add(testCase(
                        "[\"Bank\", \"withdraw\", \"transfer\", \"deposit\", \"transfer\", \"withdraw\"]\n"
                                + "[[[10, 100, 20, 50, 30]], [3, 10], [5, 1, 20], [5, 20], [3, 4, 15], [10, 50]]",
                        "[null, true, true, true, false, false]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P444 baseline test set no longer contains the maximum-money boundary case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String bankProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validBankProblem();
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
            throw new IOException("P444 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validBankProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p444.json");
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
        solution.addProperty("name", "Bank Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Reject every transaction.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Bank teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Bank { public Bank(long[] balance) {} public boolean transfer(int account1, int account2, long money) { return false; } public boolean deposit(int account, long money) { return false; } public boolean withdraw(int account, long money) { return false; } }";
    }
}
