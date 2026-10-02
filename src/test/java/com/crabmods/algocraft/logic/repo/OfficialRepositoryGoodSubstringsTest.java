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

class OfficialRepositoryGoodSubstringsTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFiftyThirdReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedGoodSubstringsProblemPreservesCache(
                "Remote p453 missing reviewed solution section",
                goodSubstringsProblemWithInvalidFirstSolution("Remote p453 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsGoodSubstringsWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedGoodSubstringsProblemPreservesCache(
                "Remote p453 weak tests",
                goodSubstringsProblemWithWeakTests("Remote p453 weak tests"),
                "Count Good Substrings tests must cover");
    }

    @Test
    void forceRefreshRejectsGoodSubstringsWithoutFixedComparisonRouteWithoutReplacingCache() throws IOException {
        assertRejectedGoodSubstringsProblemPreservesCache(
                "Remote p453 missing fixed comparison route",
                goodSubstringsProblemWithoutSolution("Remote p453 missing fixed comparison route",
                        "Fixed Three-Character Comparison"),
                "fixed three-character comparison route");
    }

    @Test
    void forceRefreshRejectsGoodSubstringsWithoutFrequencyRouteWithoutReplacingCache() throws IOException {
        assertRejectedGoodSubstringsProblemPreservesCache(
                "Remote p453 missing frequency route",
                goodSubstringsProblemWithoutSolution("Remote p453 missing frequency route",
                        "Frequency Window With Duplicate Count"),
                "frequency-window duplicate-count route");
    }

    @Test
    void forceRefreshRejectsGoodSubstringsWithoutBitmaskRouteWithoutReplacingCache() throws IOException {
        assertRejectedGoodSubstringsProblemPreservesCache(
                "Remote p453 missing bitmask route",
                goodSubstringsProblemWithoutSolution("Remote p453 missing bitmask route",
                        "Three-Character Bitmask"),
                "three-character bitmask route");
    }

    @Test
    void forceRefreshRejectsGoodSubstringsWithoutHashSetRouteWithoutReplacingCache() throws IOException {
        assertRejectedGoodSubstringsProblemPreservesCache(
                "Remote p453 missing HashSet route",
                goodSubstringsProblemWithoutSolution("Remote p453 missing HashSet route",
                        "HashSet Window Baseline"),
                "HashSet window baseline route");
    }

    private void assertRejectedGoodSubstringsProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Good Substrings of Length Three", repository.getProblems().get(452).getTitle());

        remote.publishProblem(453, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p453.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p453 good-substrings content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Count Good Substrings of Length Three", repository.getProblems().get(452).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p453.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Count Good Substrings of Length Three"));
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

    private static String goodSubstringsProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validGoodSubstringsProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero for every string and ignore all length-three windows, while still being long "
                        + "enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String goodSubstringsProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validGoodSubstringsProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && "s = \"\"".equals(test.get("input").getAsString())) {
                editedTests.add(testCase("s = \"xyzzaz\"", "1"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P453 baseline test set no longer contains the empty-string case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String goodSubstringsProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validGoodSubstringsProblem();
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
            throw new IOException("P453 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validGoodSubstringsProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p453.json");
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
        solution.addProperty("name", "Good Substrings Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return zero and do not inspect any length-three substring.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required good-substring teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int countGoodSubstrings(String s) { return 0; } }";
    }
}
