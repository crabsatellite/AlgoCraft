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

class OfficialRepositoryReshapeMatrixTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredSixtyEighthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedReshapeMatrixProblemPreservesCache(
                "Remote p468 missing reviewed solution section",
                reshapeMatrixProblemWithInvalidFirstSolution("Remote p468 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsReshapeMatrixWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedReshapeMatrixProblemPreservesCache(
                "Remote p468 weak tests",
                reshapeMatrixProblemWithWeakTests("Remote p468 weak tests"),
                "Reshape the Matrix tests must cover");
    }

    @Test
    void forceRefreshRejectsReshapeMatrixWithoutLinearIndexRouteWithoutReplacingCache() throws IOException {
        assertRejectedReshapeMatrixProblemPreservesCache(
                "Remote p468 missing linear route",
                reshapeMatrixProblemWithoutSolution(
                        "Remote p468 missing linear route", "Linear Index Mapping"),
                "linear index mapping route");
    }

    @Test
    void forceRefreshRejectsReshapeMatrixWithoutSourceCounterRouteWithoutReplacingCache() throws IOException {
        assertRejectedReshapeMatrixProblemPreservesCache(
                "Remote p468 missing source counter route",
                reshapeMatrixProblemWithoutSolution(
                        "Remote p468 missing source counter route", "Source Scan with Target Counter"),
                "source-scan target-counter route");
    }

    @Test
    void forceRefreshRejectsReshapeMatrixWithoutFlattenArrayRouteWithoutReplacingCache() throws IOException {
        assertRejectedReshapeMatrixProblemPreservesCache(
                "Remote p468 missing flatten array route",
                reshapeMatrixProblemWithoutSolution(
                        "Remote p468 missing flatten array route", "Flatten Array Then Rebuild"),
                "array flatten-and-rebuild route");
    }

    @Test
    void forceRefreshRejectsReshapeMatrixWithoutQueueRouteWithoutReplacingCache() throws IOException {
        assertRejectedReshapeMatrixProblemPreservesCache(
                "Remote p468 missing queue route",
                reshapeMatrixProblemWithoutSolution(
                        "Remote p468 missing queue route", "Flatten Queue Then Fill"),
                "queue flatten-and-fill route");
    }

    private void assertRejectedReshapeMatrixProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Reshape the Matrix", repository.getProblems().get(467).getTitle());

        remote.publishProblem(468, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p468.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p468 reshape matrix content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Reshape the Matrix", repository.getProblems().get(467).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p468.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Reshape the Matrix"));
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

    private static String reshapeMatrixProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validReshapeMatrixProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the original matrix without preserving row-major reshape semantics, "
                        + "while still being long enough for the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String reshapeMatrixProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validReshapeMatrixProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && "mat = [[-1,-2,3],[4,0,-5]], r = 1, c = 6"
                    .equals(test.get("input").getAsString())) {
                editedTests.add(testCase("mat = [[1,2],[3,4]], r = 1, c = 4", "[[1,2,3,4]]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P468 baseline test set no longer contains the negative-value reshape case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String reshapeMatrixProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validReshapeMatrixProblem();
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
            throw new IOException("P468 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validReshapeMatrixProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p468.json");
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
        solution.addProperty("name", "Reshape Matrix Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return the original matrix without applying the required row-major reshape routes.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not a valid route for the matrix reshape recurrence.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class Solution { public int[][] matrixReshape(int[][] mat, int r, int c) { return mat; } }";
    }
}
