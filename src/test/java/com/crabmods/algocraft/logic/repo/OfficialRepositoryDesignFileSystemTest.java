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

class OfficialRepositoryDesignFileSystemTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredThirtiethReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignFileSystemProblemPreservesCache(
                "Remote p430 missing reviewed solution section",
                designFileSystemProblemWithInvalidFirstSolution(
                        "Remote p430 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsDesignFileSystemWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedDesignFileSystemProblemPreservesCache(
                "Remote p430 weak tests",
                designFileSystemProblemWithWeakTests("Remote p430 weak tests"),
                "Design File System tests must cover");
    }

    @Test
    void forceRefreshRejectsDesignFileSystemWithoutFullPathHashMapWithoutReplacingCache() throws IOException {
        assertRejectedDesignFileSystemProblemPreservesCache(
                "Remote p430 missing full-path HashMap route",
                designFileSystemProblemWithoutSolution(
                        "Remote p430 missing full-path HashMap route",
                        "Full Path HashMap"),
                "full-path HashMap route");
    }

    @Test
    void forceRefreshRejectsDesignFileSystemWithoutObjectTrieWithoutReplacingCache() throws IOException {
        assertRejectedDesignFileSystemProblemPreservesCache(
                "Remote p430 missing object trie-node route",
                designFileSystemProblemWithoutSolution(
                        "Remote p430 missing object trie-node route",
                        "Object Trie Nodes"),
                "object trie-node route");
    }

    @Test
    void forceRefreshRejectsDesignFileSystemWithoutArrayIndexedTrieWithoutReplacingCache() throws IOException {
        assertRejectedDesignFileSystemProblemPreservesCache(
                "Remote p430 missing array-indexed trie route",
                designFileSystemProblemWithoutSolution(
                        "Remote p430 missing array-indexed trie route",
                        "Array Indexed Trie"),
                "array-indexed trie route");
    }

    @Test
    void forceRefreshRejectsDesignFileSystemWithoutPathIdRegistryWithoutReplacingCache() throws IOException {
        assertRejectedDesignFileSystemProblemPreservesCache(
                "Remote p430 missing path-id registry route",
                designFileSystemProblemWithoutSolution(
                        "Remote p430 missing path-id registry route",
                        "Path Id Registry"),
                "path-id registry route");
    }

    @Test
    void forceRefreshRejectsDesignFileSystemWithoutParentChildrenIndexWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignFileSystemProblemPreservesCache(
                "Remote p430 missing parent-children index route",
                designFileSystemProblemWithoutSolution(
                        "Remote p430 missing parent-children index route",
                        "Parent Children Index"),
                "parent-children index route");
    }

    private void assertRejectedDesignFileSystemProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design File System", repository.getProblems().get(429).getTitle());

        remote.publishProblem(430, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p430.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p430 Design File System content before cache replacement; "
                        + "actual: " + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design File System", repository.getProblems().get(429).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p430.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design File System"));
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

    private static String designFileSystemProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validDesignFileSystemProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a no-op file system that never stores paths, while still being long enough for "
                        + "the generic teaching-description check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty("code", fallbackSolutionCode());
        return GSON.toJson(problem);
    }

    private static String designFileSystemProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validDesignFileSystemProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[\"/ab/c\",2]")) {
                editedTests.add(testCase(
                        "[\"FileSystem\",\"createPath\",\"get\"]\n[[],[\"/x\",1],[\"/x\"]]",
                        "[null,true,1]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P430 baseline test set no longer contains the prefix-not-parent case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String designFileSystemProblemWithoutSolution(String title, String missingSolutionName)
            throws IOException {
        JsonObject problem = validDesignFileSystemProblem();
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
            throw new IOException("P430 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validDesignFileSystemProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p430.json");
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
        solution.addProperty("name", "Design File System Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a no-op file system that never stores paths.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Design File System teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty("code", fallbackSolutionCode());
        solution.addProperty("language", "java");
        return solution;
    }

    private static String fallbackSolutionCode() {
        return "class FileSystem { "
                + "public FileSystem() {} "
                + "public boolean createPath(String path, int value) { return false; } "
                + "public int get(String path) { return -1; } "
                + "}";
    }
}
