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

class OfficialRepositorySumDigitsAfterConvertTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftyFourthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedSumDigitsProblemPreservesCache(
                "Remote p454 missing reviewed solution section",
                sumDigitsProblemWithInvalidFirstSolution("Remote p454 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsSumDigitsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedSumDigitsProblemPreservesCache(
                "Remote p454 weak tests",
                sumDigitsProblemWithWeakTests("Remote p454 weak tests"),
                "Sum of Digits After Convert tests must cover");
    }

    @Test
    void forceRefreshRejectsSumDigitsWithoutLiteralSimulationRouteWithoutReplacingCache() throws IOException {
        assertRejectedSumDigitsProblemPreservesCache(
                "Remote p454 missing literal route",
                sumDigitsProblemWithoutSolution("Remote p454 missing literal route",
                        "Literal Numeric String Simulation"),
                "literal numeric-string simulation route");
    }

    @Test
    void forceRefreshRejectsSumDigitsWithoutDirectFirstSumRouteWithoutReplacingCache() throws IOException {
        assertRejectedSumDigitsProblemPreservesCache(
                "Remote p454 missing direct route",
                sumDigitsProblemWithoutSolution("Remote p454 missing direct route",
                        "Direct First Digit Sum"),
                "direct first digit-sum route");
    }

    @Test
    void forceRefreshRejectsSumDigitsWithoutRecursiveRouteWithoutReplacingCache() throws IOException {
        assertRejectedSumDigitsProblemPreservesCache(
                "Remote p454 missing recursive route",
                sumDigitsProblemWithoutSolution("Remote p454 missing recursive route",
                        "Recursive Transform Decomposition"),
                "recursive transform decomposition route");
    }

    @Test
    void forceRefreshRejectsSumDigitsWithoutDigitalRootRouteWithoutReplacingCache() throws IOException {
        assertRejectedSumDigitsProblemPreservesCache(
                "Remote p454 missing digital-root route",
                sumDigitsProblemWithoutSolution("Remote p454 missing digital-root route",
                        "Bounded Digital Root Finish"),
                "bounded digital-root finish route");
    }

    private void assertRejectedSumDigitsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Sum of Digits of String After Convert", repository.getProblems().get(453).getTitle());

        remote.publishProblem(454, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p454.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p454 sum-digits content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Sum of Digits of String After Convert", repository.getProblems().get(453).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p454.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Sum of Digits of String After Convert"));
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

    private static String sumDigitsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validSumDigitsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero without converting letters or applying any repeated digit transform, "
                        + "while remaining long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String sumDigitsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validSumDigitsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "s = \"abcdefghijklmnopqrstuvwxyz\", k = 10".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("s = \"iiii\", k = 1", "36"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P454 baseline test set no longer contains the full-alphabet long-k case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String sumDigitsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validSumDigitsProblem();
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
            throw new IOException("P454 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validSumDigitsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p454.json");
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
        solution.addProperty("name", "Sum Digits Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero and do not inspect the string or transform counter.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required sum-digits teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int getLucky(String s, int k) { return 0; } }";
    }
}
