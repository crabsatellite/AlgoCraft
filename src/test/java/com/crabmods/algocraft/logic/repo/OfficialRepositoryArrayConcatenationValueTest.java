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

class OfficialRepositoryArrayConcatenationValueTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredTwentyFirstReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedArrayConcatenationProblemPreservesCache(
                "Remote p421 missing reviewed solution section",
                arrayConcatenationProblemWithInvalidFirstSolution("Remote p421 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsArrayConcatenationWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedArrayConcatenationProblemPreservesCache(
                "Remote p421 weak tests",
                arrayConcatenationProblemWithWeakTests("Remote p421 weak tests"),
                "Find the Array Concatenation Value tests must cover");
    }

    @Test
    void forceRefreshRejectsArrayConcatenationWithoutStringTwoPointersWithoutReplacingCache() throws IOException {
        assertRejectedArrayConcatenationProblemPreservesCache(
                "Remote p421 missing string two-pointer route",
                arrayConcatenationProblemWithoutSolution("Remote p421 missing string two-pointer route",
                        "Two Pointers with Strings"),
                "string two-pointer route");
    }

    @Test
    void forceRefreshRejectsArrayConcatenationWithoutArithmeticMultiplierWithoutReplacingCache() throws IOException {
        assertRejectedArrayConcatenationProblemPreservesCache(
                "Remote p421 missing arithmetic digit-multiplier route",
                arrayConcatenationProblemWithoutSolution("Remote p421 missing arithmetic digit-multiplier route",
                        "Arithmetic Digit Multiplier"),
                "arithmetic digit-multiplier route");
    }

    @Test
    void forceRefreshRejectsArrayConcatenationWithoutDequeSimulationWithoutReplacingCache() throws IOException {
        assertRejectedArrayConcatenationProblemPreservesCache(
                "Remote p421 missing deque simulation route",
                arrayConcatenationProblemWithoutSolution("Remote p421 missing deque simulation route",
                        "Deque Operation Simulation"),
                "deque simulation route");
    }

    @Test
    void forceRefreshRejectsArrayConcatenationWithoutRecursiveReductionWithoutReplacingCache() throws IOException {
        assertRejectedArrayConcatenationProblemPreservesCache(
                "Remote p421 missing recursive inward-reduction route",
                arrayConcatenationProblemWithoutSolution("Remote p421 missing recursive inward-reduction route",
                        "Recursive Inward Reduction"),
                "recursive inward-reduction route");
    }

    @Test
    void forceRefreshRejectsArrayConcatenationWithoutPowerThresholdsWithoutReplacingCache() throws IOException {
        assertRejectedArrayConcatenationProblemPreservesCache(
                "Remote p421 missing constraint-based power-threshold route",
                arrayConcatenationProblemWithoutSolution("Remote p421 missing constraint-based power-threshold route",
                        "Constraint-Based Power Thresholds"),
                "constraint-based power-threshold route");
    }

    private void assertRejectedArrayConcatenationProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Find the Array Concatenation Value", repository.getProblems().get(420).getTitle());

        remote.publishProblem(421, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p421.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p421 Array Concatenation content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Find the Array Concatenation Value", repository.getProblems().get(420).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p421.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Find the Array Concatenation Value"));
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

    private static String arrayConcatenationProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validArrayConcatenationProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder total without pairing array ends while still being long enough for "
                        + "the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Solution { public long findTheArrayConcVal(int[] nums) { return 0; } }");
        return GSON.toJson(problem);
    }

    private static String arrayConcatenationProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validArrayConcatenationProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[100000,1,2,100000]")) {
                editedTests.add(testCase("nums = [2]", "2"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P421 baseline test set no longer contains the maximum-width coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String arrayConcatenationProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validArrayConcatenationProblem();
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
            throw new IOException("P421 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validArrayConcatenationProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p421.json");
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
        solution.addProperty("name", "Array Concatenation Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return a placeholder value without simulating the array concatenation process.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Array Concatenation teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Solution { public long findTheArrayConcVal(int[] nums) { return 0; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
