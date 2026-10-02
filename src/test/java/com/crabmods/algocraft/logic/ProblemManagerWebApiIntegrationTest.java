package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.web.AlgoCraftWebServer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemManagerWebApiIntegrationTest {
    private static final Gson GSON = new Gson();
    private static final Path QUESTION_BANK = Paths.get(System.getProperty("user.dir"), "question_bank", "official");

    @TempDir
    Path tempConfigDir;

    @BeforeEach
    void setUp() {
        System.setProperty(ProgressManager.CONFIG_DIR_PROPERTY, tempConfigDir.toString());
        ProgressManager.resetForTest();
        SubmissionHistoryManager.resetForTest();
        ProblemManager.resetForTest();
    }

    @AfterEach
    void tearDown() {
        AlgoCraftWebServer.stop();
        AlgoCraftWebServer.setSolvedProblemNotifier(null);
        ProgressManager.resetForTest();
        SubmissionHistoryManager.resetForTest();
        ProblemManager.resetForTest();
        System.clearProperty(ProgressManager.CONFIG_DIR_PROPERTY);
    }

    @Test
    void webApiListsRunsAndSubmitsOfficialProblemThroughProblemManagerAndJudge() throws Exception {
        Problem problem = loadOfficialProblem("1");
        String code = problem.getSolutions().get(0).getCode();
        ProblemManager.replaceRepositoriesForTest(List.of(
                new StaticRepository("Official", 100, List.of(problem))
        ));

        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> problemsResponse = sendGet("/api/problems?repo=Official");
        assertEquals(200, problemsResponse.statusCode(), problemsResponse.body());
        JsonArray problems = JsonParser.parseString(problemsResponse.body()).getAsJsonArray();
        assertEquals(1, problems.size());
        assertEquals("1", problems.get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("Two Sum", problems.get(0).getAsJsonObject().get("title").getAsString());

        HttpResponse<String> runResponse = sendJson("/api/run", requestBody(code, problem.getId()));
        assertEquals(200, runResponse.statusCode(), runResponse.body());
        String output = JsonParser.parseString(runResponse.body())
                .getAsJsonObject()
                .get("output")
                .getAsString();
        assertTrue(output.contains("PASS"), output);
        assertFalse(output.contains("FAIL"), output);
        assertFalse(output.contains("ERROR:"), output);

        HttpResponse<String> submitResponse = sendJson("/api/submit", requestBody(code, problem.getId()));
        assertEquals(200, submitResponse.statusCode(), submitResponse.body());
        JsonObject result = JsonParser.parseString(submitResponse.body()).getAsJsonObject();
        assertTrue(result.get("isSuccess").getAsBoolean(), submitResponse.body());
        assertEquals("Accepted", result.get("message").getAsString());
        assertEquals(problem.getTotalTestCount(), result.get("totalCount").getAsInt());
        assertEquals(problem.getTotalTestCount(), result.get("passedCount").getAsInt());
    }

    @Test
    void webApiRunUsesProblemIdSpecificOutputSemantics() throws Exception {
        Problem problem = loadOfficialProblem("1");
        String reversedValidZeroBasedTwoSum = """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        for (int i = 0; i < nums.length; i++) {
                            for (int j = i + 1; j < nums.length; j++) {
                                if (nums[i] + nums[j] == target) {
                                    return new int[]{j, i};
                                }
                            }
                        }
                        return new int[0];
                    }
                }
                """;
        ProblemManager.replaceRepositoriesForTest(List.of(
                new StaticRepository("Official", 100, List.of(problem))
        ));

        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> runResponse = sendJson("/api/run", requestBody(reversedValidZeroBasedTwoSum, problem.getId()));
        assertEquals(200, runResponse.statusCode(), runResponse.body());
        String output = JsonParser.parseString(runResponse.body())
                .getAsJsonObject()
                .get("output")
                .getAsString();
        assertTrue(output.contains("PASS"), output);
        assertFalse(output.contains("FAIL"), output);
        assertFalse(output.contains("ERROR:"), output);
    }

    @Test
    void webImagesServeDeclaredDiagramBytesAndRejectUnlistedPaths() throws Exception {
        Problem problem = loadOfficialProblem("7");
        ProblemManager.replaceRepositoriesForTest(List.of(new StaticRepository("Official", 100, List.of(problem))));
        assertTrue(AlgoCraftWebServer.start(0));
        String file = problem.getVisuals().get(0).file();
        String imageUrl = "/api/problem-image?problemId=7&file=" + java.net.URLEncoder.encode(file, StandardCharsets.UTF_8);
        HttpResponse<byte[]> image = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(endpoint(imageUrl)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, image.statusCode());
        assertEquals("image/png", image.headers().firstValue("Content-Type").orElse(""));
        assertArrayEquals(Files.readAllBytes(ProblemAssetResolver.resolveImage(problem, problem.getVisuals().get(0)).orElseThrow()), image.body());
        assertEquals(404, sendGet("/api/problem-image?problemId=7&file=../p7.json").statusCode());
        assertEquals(404, sendGet("/api/problem-image?problemId=7&file=images/unlisted.png").statusCode());
        assertEquals(404, sendGet("/api/problem-image?problemId=missing&file=" + file).statusCode());
    }

    @Test
    void webApiRunDoesNotApplyZeroBasedTwoSumSemanticsToOneBasedProblem() throws Exception {
        Problem problem = loadOfficialProblem("22");
        String zeroBasedWrongForOneBasedTwoSum = """
                class Solution {
                    public int[] twoSum(int[] numbers, int target) {
                        return new int[]{0, 1};
                    }
                }
                """;
        ProblemManager.replaceRepositoriesForTest(List.of(
                new StaticRepository("Official", 100, List.of(problem))
        ));

        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> runResponse = sendJson("/api/run", requestBody(zeroBasedWrongForOneBasedTwoSum, problem.getId()));
        assertEquals(200, runResponse.statusCode(), runResponse.body());
        String output = JsonParser.parseString(runResponse.body())
                .getAsJsonObject()
                .get("output")
                .getAsString();
        assertTrue(output.contains("FAIL"), output);
        assertFalse(output.contains("ERROR:"), output);
    }

    @Test
    void webApiSubmitReturnsJsonErrorInsteadOfTimingOutWhenSolvedNotifierFails() throws Exception {
        Problem problem = loadOfficialProblem("1");
        String code = problem.getSolutions().get(0).getCode();
        ProblemManager.replaceRepositoriesForTest(List.of(
                new StaticRepository("Official", 100, List.of(problem))
        ));
        AlgoCraftWebServer.setSolvedProblemNotifier((solvedProblem, solvedCode) -> {
            throw new IllegalStateException("simulated game-thread handoff failure");
        });

        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> response = sendJson("/api/submit", requestBody(code, problem.getId()));

        assertEquals(500, response.statusCode(), response.body());
        JsonObject error = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("Internal server error", error.get("error").getAsString());
        assertFalse(response.body().contains("simulated game-thread handoff failure"),
                "internal exception messages should be logged server-side, not leaked to the web IDE");
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY", response.headers().firstValue("X-Frame-Options").orElse(""));

        HttpResponse<String> historyResponse = sendGet("/api/history");
        assertEquals(200, historyResponse.statusCode(), historyResponse.body());
        JsonArray history = JsonParser.parseString(historyResponse.body()).getAsJsonArray();
        assertEquals(0, history.size(),
                "a failed game-thread handoff must not leave an Accepted record in the web submission history");
    }

    @Test
    void webApiSubmitRejectsExtraConcurrentJudgeRequestButKeepsProblemBrowsingResponsive() throws Exception {
        Problem problem = loadOfficialProblem("1");
        String code = problem.getSolutions().get(0).getCode();
        ProblemManager.replaceRepositoriesForTest(List.of(
                new StaticRepository("Official", 100, List.of(problem))
        ));
        CountDownLatch occupiedJudgeSlots = new CountDownLatch(2);
        CountDownLatch releaseSubmissions = new CountDownLatch(1);
        AlgoCraftWebServer.setSolvedProblemNotifier((solvedProblem, solvedCode) -> {
            occupiedJudgeSlots.countDown();
            try {
                if (!releaseSubmissions.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting to release blocked web submissions");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while blocking web submission", e);
            }
        });

        assertTrue(AlgoCraftWebServer.start(0));

        CompletableFuture<HttpResponse<String>> first = sendJsonAsync("/api/submit", requestBody(code, problem.getId()));
        CompletableFuture<HttpResponse<String>> second = sendJsonAsync("/api/submit", requestBody(code, problem.getId()));
        try {
            assertTrue(occupiedJudgeSlots.await(30, TimeUnit.SECONDS),
                    "two successful submissions should occupy the web judge permit pool before the hostile request");

            HttpResponse<String> rejected = sendJson("/api/submit", requestBody(code, problem.getId()));
            assertEquals(429, rejected.statusCode(), rejected.body());
            JsonObject busyError = JsonParser.parseString(rejected.body()).getAsJsonObject();
            assertEquals("Code execution is busy; please retry soon", busyError.get("error").getAsString());
            assertEquals("nosniff", rejected.headers().firstValue("X-Content-Type-Options").orElse(""));
            assertEquals("DENY", rejected.headers().firstValue("X-Frame-Options").orElse(""));

            HttpResponse<String> problemsResponse = sendGet("/api/problems?repo=Official");
            assertEquals(200, problemsResponse.statusCode(), problemsResponse.body());
            JsonArray problems = JsonParser.parseString(problemsResponse.body()).getAsJsonArray();
            assertEquals(1, problems.size(), "lightweight browsing APIs should remain responsive while judge slots are full");
        } finally {
            releaseSubmissions.countDown();
        }

        assertEquals(200, first.get(30, TimeUnit.SECONDS).statusCode());
        assertEquals(200, second.get(30, TimeUnit.SECONDS).statusCode());
    }

    @Test
    void webApiRejectsOversizedRunAndSubmitBodiesBeforeParsingOrJudging() throws Exception {
        ProblemManager.replaceRepositoriesForTest(List.of());
        assertTrue(AlgoCraftWebServer.start(0));

        String oversizedBody = "{\"code\":\"" + "x".repeat(120_000) + "\",\"problemId\":\"1\"}";

        for (String path : List.of("/api/run", "/api/submit")) {
            HttpResponse<String> response = sendJson(path, oversizedBody);

            assertEquals(413, response.statusCode(), path + " should reject oversized request bodies");
            JsonObject error = JsonParser.parseString(response.body()).getAsJsonObject();
            assertEquals("Request body too large", error.get("error").getAsString());
            assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""));
            assertEquals("DENY", response.headers().firstValue("X-Frame-Options").orElse(""));
        }
    }

    private static Problem loadOfficialProblem(String problemId) throws Exception {
        Path problemFile = QUESTION_BANK.resolve("p" + problemId + ".json");
        Problem problem = GSON.fromJson(
                Files.readString(problemFile, StandardCharsets.UTF_8),
                Problem.class
        );
        problem.setAssetBaseDir(QUESTION_BANK);
        return problem;
    }

    private static String requestBody(String code, String problemId) {
        JsonObject body = new JsonObject();
        body.addProperty("code", code);
        body.addProperty("problemId", problemId);
        return GSON.toJson(body);
    }

    private static HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint(path))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> sendJson(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint(path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static CompletableFuture<HttpResponse<String>> sendJsonAsync(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder(endpoint(path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private static URI endpoint(String path) {
        return URI.create("http://127.0.0.1:" + AlgoCraftWebServer.getActivePort() + path);
    }

    private static final class StaticRepository implements ProblemRepository {
        private final String name;
        private final int priority;
        private final List<Problem> problems;

        private StaticRepository(String name, int priority, List<Problem> problems) {
            this.name = name;
            this.priority = priority;
            this.problems = new ArrayList<>(problems);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public List<Problem> getProblems() {
            return new ArrayList<>(problems);
        }

        @Override
        public CompletableFuture<Void> refresh() {
            return CompletableFuture.completedFuture(null);
        }
    }
}
