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

class OfficialRepositoryVideoSharingPlatformTest {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    @TempDir
    Path cacheDir;

    @Test
    void forceRefreshRejectsFourHundredFourteenthReviewedProblemSolutionWithoutKeyInsightWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
                "Remote p414 missing reviewed solution section",
                videoSharingPlatformProblemWithInvalidFirstSolution(
                        "Remote p414 missing reviewed solution section"),
                "Key Insight");
    }

    @Test
    void forceRefreshRejectsDesignVideoSharingPlatformWeakTestsWithoutReplacingCache() throws IOException {
        assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
                "Remote p414 weak tests",
                videoSharingPlatformProblemWithWeakTests("Remote p414 weak tests"),
                "Design Video Sharing Platform tests must cover");
    }

    @Test
    void forceRefreshRejectsDesignVideoSharingPlatformWithoutPriorityQueueWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
                "Remote p414 missing PriorityQueue route",
                videoSharingPlatformProblemWithoutSolution(
                        "Remote p414 missing PriorityQueue route",
                        "HashMap and PriorityQueue Free IDs"),
                "HashMap plus PriorityQueue reusable-ID route");
    }

    @Test
    void forceRefreshRejectsDesignVideoSharingPlatformWithoutTreeSetWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
                "Remote p414 missing TreeSet route",
                videoSharingPlatformProblemWithoutSolution(
                        "Remote p414 missing TreeSet route",
                        "TreeSet Reusable IDs with Video Records"),
                "TreeSet reusable-ID route");
    }

    @Test
    void forceRefreshRejectsDesignVideoSharingPlatformWithoutArrayListScanWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
                "Remote p414 missing ArrayList scan",
                videoSharingPlatformProblemWithoutSolution(
                        "Remote p414 missing ArrayList scan",
                        "ArrayList Slot Scan Baseline"),
                "ArrayList slot scan baseline route");
    }

    @Test
    void forceRefreshRejectsDesignVideoSharingPlatformWithoutBitSetWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
                "Remote p414 missing BitSet route",
                videoSharingPlatformProblemWithoutSolution(
                        "Remote p414 missing BitSet route",
                        "BitSet Free Slot Index"),
                "BitSet free-slot index route");
    }

    @Test
    void forceRefreshRejectsDesignVideoSharingPlatformWithoutCustomHeapWithoutReplacingCache()
            throws IOException {
        assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
                "Remote p414 missing custom heap",
                videoSharingPlatformProblemWithoutSolution(
                        "Remote p414 missing custom heap",
                        "Custom Binary Min Heap for Released IDs"),
                "custom binary min-heap reusable-ID route");
    }

    private void assertRejectedDesignVideoSharingPlatformProblemPreservesCache(
            String remoteTitle, String remoteProblemJson, String expectedMessageFragment) throws IOException {
        FileBackedOfficialRemote remote = FileBackedOfficialRemote.fromOfficialBank();
        OfficialRepository repository = new OfficialRepository(cacheDir, remote.baseUrl(), remote, true);
        repository.forceRefresh().join();

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Video Sharing Platform", repository.getProblems().get(413).getTitle());

        remote.publishProblem(414, remoteProblemJson);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.forceRefresh().join());
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        assertTrue(message != null
                        && message.contains("p414.json")
                        && message.contains(expectedMessageFragment),
                "failure should identify invalid p414 Design Video Sharing Platform content before cache replacement; actual: "
                        + message);

        assertEquals(RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT, repository.getProblems().size());
        assertEquals("Design Video Sharing Platform", repository.getProblems().get(413).getTitle());
        String cachedProblem = Files.readString(cacheDir.resolve("p414.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("Design Video Sharing Platform"));
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

    private static String videoSharingPlatformProblemWithInvalidFirstSolution(String title) throws IOException {
        JsonObject problem = validVideoSharingPlatformProblem();
        problem.addProperty("title", title);
        JsonObject firstSolution = problem.getAsJsonArray("solutions").get(0).getAsJsonObject();
        firstSolution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Use a deliberately invalid placeholder that ignores uploaded videos and always "
                        + "returns empty results, while keeping enough words to pass the basic length check.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        firstSolution.addProperty(
                "code",
                "class VideoSharingPlatform { public VideoSharingPlatform() {} "
                        + "public int upload(String video) { return 0; } public void remove(int videoId) {} "
                        + "public String watch(int videoId, int startMinute, int endMinute) { return \"-1\"; } "
                        + "public void like(int videoId) {} public void dislike(int videoId) {} "
                        + "public int[] getLikesAndDislikes(int videoId) { return new int[]{-1}; } "
                        + "public int getViews(int videoId) { return -1; } }");
        return GSON.toJson(problem);
    }

    private static String videoSharingPlatformProblemWithWeakTests(String title) throws IOException {
        JsonObject problem = validVideoSharingPlatformProblem();
        problem.addProperty("title", title);
        JsonArray editedTests = new JsonArray();
        boolean replaced = false;
        for (JsonElement element : problem.getAsJsonArray("tests")) {
            JsonObject test = element.getAsJsonObject();
            if (!replaced && test.get("input").getAsString().contains("[\"98765\"]")) {
                editedTests.add(testCase(
                        "[\"VideoSharingPlatform\",\"upload\",\"watch\"]\n[[],[\"1\"],[0,0,1]]",
                        "[null,0,\"1\"]"));
                replaced = true;
            } else {
                editedTests.add(element.deepCopy());
            }
        }
        if (!replaced) {
            throw new IOException("P414 baseline test set no longer contains the watch clamp coverage case");
        }
        problem.add("tests", editedTests);
        return GSON.toJson(problem);
    }

    private static String videoSharingPlatformProblemWithoutSolution(
            String title, String missingSolutionName) throws IOException {
        JsonObject problem = validVideoSharingPlatformProblem();
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
            throw new IOException("P414 baseline solutions no longer contain " + missingSolutionName);
        }
        editedSolutions.add(fallbackSolution());
        problem.add("solutions", editedSolutions);
        return GSON.toJson(problem);
    }

    private static JsonObject validVideoSharingPlatformProblem() throws IOException {
        Path path = OFFICIAL_BANK_ROOT.resolve("p414.json");
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
        solution.addProperty("name", "Video Sharing Placeholder Fixture");
        solution.addProperty("timeComplexity", "O(1)");
        solution.addProperty("spaceComplexity", "O(1)");
        solution.addProperty(
                "description",
                "## Approach\n\n"
                        + "Return fixed invalid answers and ignore every uploaded video.\n\n"
                        + "## Key Insight\n\n"
                        + "This fixture is intentionally not one of the required Video Sharing Platform teaching routes.\n\n"
                        + "## Complexity Analysis\n"
                        + "- Time Complexity: O(1)\n"
                        + "- Space Complexity: O(1)");
        solution.addProperty(
                "code",
                "class VideoSharingPlatform { public VideoSharingPlatform() {} "
                        + "public int upload(String video) { return 999; } public void remove(int videoId) {} "
                        + "public String watch(int videoId, int startMinute, int endMinute) { return \"-1\"; } "
                        + "public void like(int videoId) {} public void dislike(int videoId) {} "
                        + "public int[] getLikesAndDislikes(int videoId) { return new int[]{-1}; } "
                        + "public int getViews(int videoId) { return -1; } }");
        solution.addProperty("language", "java");
        return solution;
    }
}
