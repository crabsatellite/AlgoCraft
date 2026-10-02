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

class OfficialRepositoryBitsetTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirteenthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignBitsetProblemPreservesCache(
                "Remote p413 missing reviewed solution section",
                bitsetProblemWithInvalidFirstSolution("Remote p413 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsDesignBitsetWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedDesignBitsetProblemPreservesCache(
                "Remote p413 weak tests",
                bitsetProblemWithWeakTests("Remote p413 weak tests"),
                "Design Bitset tests must cover");
    }

    @Test
    void forceRefreshRejectsDesignBitsetWithoutDualCharViewsWithoutReplacingCache() throws IOException {
        assertRejectedDesignBitsetProblemPreservesCache(
                "Remote p413 missing dual char views",
                bitsetProblemWithoutSolution("Remote p413 missing dual char views", "Dual Char Views with Flip Flag"),
                "dual char views with flip flag route");
    }

    @Test
    void forceRefreshRejectsDesignBitsetWithoutRawBooleanXorWithoutReplacingCache() throws IOException {
        assertRejectedDesignBitsetProblemPreservesCache(
                "Remote p413 missing raw boolean XOR",
                bitsetProblemWithoutSolution("Remote p413 missing raw boolean XOR", "Raw Boolean Array with XOR Flag"),
                "raw boolean array with XOR flag route");
    }

    @Test
    void forceRefreshRejectsDesignBitsetWithoutJavaBitSetWithoutReplacingCache() throws IOException {
        assertRejectedDesignBitsetProblemPreservesCache(
                "Remote p413 missing Java BitSet",
                bitsetProblemWithoutSolution("Remote p413 missing Java BitSet", "Java BitSet with Global Flip Flag"),
                "Java BitSet with global flip flag route");
    }

    @Test
    void forceRefreshRejectsDesignBitsetWithoutComplementarySetsWithoutReplacingCache() throws IOException {
        assertRejectedDesignBitsetProblemPreservesCache(
                "Remote p413 missing complementary sets",
                bitsetProblemWithoutSolution("Remote p413 missing complementary sets", "Complementary Ones and Zeros Sets"),
                "complementary ones/zeros sets route");
    }

    @Test
    void forceRefreshRejectsDesignBitsetWithoutLazySegmentTreeWithoutReplacingCache() throws IOException {
        assertRejectedDesignBitsetProblemPreservesCache(
                "Remote p413 missing lazy segment tree",
                bitsetProblemWithoutSolution("Remote p413 missing lazy segment tree", "Lazy Segment Tree with Range Flip"),
                "lazy segment tree range flip route");
    }

    private void assertRejectedDesignBitsetProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Bitset", repository.getProblems().get(412).getTitle());

        remote.publishProblem(413, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p413.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p413 Design Bitset content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Bitset", repository.getProblems().get(412).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p413.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design Bitset"));
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

    private static String bitsetProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validBitsetProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a deliberately wrong placeholder that never stores bit values, "
                        + "so the description reaches the reviewed-section checks while still missing "
                        + "the required insight heading.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class Bitset { public Bitset(int size) {} public void fix(int idx) {} "
                        + "public void unfix(int idx) {} public void flip() {} "
                        + "public boolean all() { return false; } public boolean one() { return false; } "
                        + "public int count() { return 0; } public String toString() { return \"\"; } }");
        return GSON.toJson(problem);
    }

    private static String bitsetProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validBitsetProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced
                    && test.get("input").getAsString().contains("\"toString\",\"flip\",\"toString\"")) {
                editedTests.add(testCase(
                        "[\"Bitset\",\"fix\",\"toString\"]\n[[1],[0],[]]",
                        "[null,null,\"1\"]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P413 baseline test set no longer contains the repeated toString coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String bitsetProblemWithoutSolution(String title, String missingSolutionName) throws IOException {
        JsonObject problem = validBitsetProblem();
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
            throw new IOException("P413 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validBitsetProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p413.json");
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
        solution.addProperty("name", "Bitset Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return fixed placeholder values for every operation.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Design Bitset teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class Bitset { private final int size; public Bitset(int size) { this.size = size; } "
                        + "public void fix(int idx) { if (idx < 0 || idx >= size) throw new IllegalArgumentException(); } "
                        + "public void unfix(int idx) {} public void flip() {} public boolean all() { return false; } "
                        + "public boolean one() { return false; } public int count() { return 0; } "
                        + "public String toString() { return \"\"; } }");
        solution.addProperty("language", "java");
        return solution;
    }

}
