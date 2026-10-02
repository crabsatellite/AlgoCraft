package com.crabmods.algocraft.web;

import com.crabmods.algocraft.logic.CodeExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

class AlgoCraftWebServerTest {
    @AfterEach
    void tearDown() {
        AlgoCraftWebServer.stop();
    }

    @Test
    void startIsIdempotentAndStopClearsRunningState() {
        assertTrue(AlgoCraftWebServer.start(0));
        int firstPort = AlgoCraftWebServer.getActivePort();
        assertTrue(firstPort > 0);
        assertTrue(AlgoCraftWebServer.isRunning());

        assertTrue(AlgoCraftWebServer.start(firstPort));
        assertEquals(firstPort, AlgoCraftWebServer.getActivePort(),
                "repeated starts should keep the existing server instead of rebinding the port");
        assertNull(AlgoCraftWebServer.getLastStartError());

        AlgoCraftWebServer.stop();

        assertFalse(AlgoCraftWebServer.isRunning());
        assertEquals(-1, AlgoCraftWebServer.getActivePort());
    }

    @Test
    void webServerBindsOnlyToLoopbackForCodeExecutionApis() {
        assertTrue(AlgoCraftWebServer.start(0));

        InetSocketAddress address = AlgoCraftWebServer.getActiveAddressForTest();

        assertNotNull(address, "running web server should expose its bound address to tests");
        assertNotNull(address.getAddress(), "web server should bind to a concrete address");
        assertTrue(address.getAddress().isLoopbackAddress(),
                "web IDE exposes code execution APIs and must not listen on LAN interfaces");
    }

    @Test
    void webServerUsesBoundedBackpressureExecutorForCodeExecutionApis() {
        assertTrue(AlgoCraftWebServer.start(0));

        ExecutorService executor = AlgoCraftWebServer.getExecutorForTest();

        assertInstanceOf(ThreadPoolExecutor.class, executor,
                "web IDE run/submit APIs must not use an unbounded cached thread pool");
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
        assertEquals(AlgoCraftWebServer.getWebExecutorMaxThreadsForTest(), pool.getCorePoolSize());
        assertEquals(AlgoCraftWebServer.getWebExecutorMaxThreadsForTest(), pool.getMaximumPoolSize());
        assertTrue(pool.getMaximumPoolSize() <= 4,
                "web IDE must keep code execution request handlers under a small fixed thread cap");
        assertEquals(AlgoCraftWebServer.getWebExecutorQueueCapacityForTest(),
                pool.getQueue().remainingCapacity(),
                "web IDE must use a finite queue so request pressure cannot allocate unbounded threads");
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class, pool.getRejectedExecutionHandler(),
                "queue saturation should apply backpressure instead of dropping requests or spawning threads");
    }

    @Test
    void startReportsPortConflictWithoutLeavingServerRunning() throws IOException {
        try (ServerSocket blocker = new ServerSocket(0)) {
            int blockedPort = blocker.getLocalPort();

            assertFalse(AlgoCraftWebServer.start(blockedPort));
            assertFalse(AlgoCraftWebServer.isRunning());
            assertEquals(-1, AlgoCraftWebServer.getActivePort());
            assertNotNull(AlgoCraftWebServer.getLastStartError());
            assertTrue(AlgoCraftWebServer.getLastStartError().getMessage().contains("Address already in use")
                            || AlgoCraftWebServer.getLastStartError().getMessage().contains("bind"),
                    "port conflicts should be observable without dumping an uncontrolled stack trace");
        }
    }

    @Test
    void apiRunRejectsMalformedJsonWithBadRequest() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> response = sendJson("/api/run", "{not json");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Invalid JSON"));
    }

    @Test
    void apiSubmitRejectsMissingRequiredFieldsWithBadRequest() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> response = sendJson("/api/submit", "{\"code\":\"class Solution {}\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("problemId"));
    }

    @Test
    void apiRunRejectsOversizedBodyBeforeJsonParsing() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> response = sendJson("/api/run", "x".repeat(100_001));

        assertEquals(413, response.statusCode());
        assertTrue(response.body().contains("Request body too large"));
    }

    @Test
    void apiRunRejectsBlankCodeBeforeProblemLookup() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> response = sendJson("/api/run", requestBody("   ", "1"));

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Code is empty or too large"));
    }

    @Test
    void apiSubmitRejectsOversizedCodeBeforeJudge() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> response = sendJson(
                "/api/submit",
                requestBody("x".repeat(CodeExecutor.MAX_CODE_LENGTH + 1), "1")
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Code is empty or too large"));
    }

    @Test
    void apiSubmitRejectsOversizedProblemIdBeforeProblemLookup() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpResponse<String> response = sendJson(
                "/api/submit",
                requestBody("class Solution {}", "x".repeat(257))
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Problem id is empty or too large"));
    }

    @Test
    void apiErrorResponsesKeepSecurityHeadersAndLocalhostCors() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/run"))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("Origin", "http://localhost:3000")
                .POST(HttpRequest.BodyPublishers.ofString("{not json"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertEquals("http://localhost:3000",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals("nosniff",
                response.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY",
                response.headers().firstValue("X-Frame-Options").orElse(""));
    }

    @Test
    void apiEndpointsRejectUnsupportedMethodsInsteadOfHanging() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/repositories"))
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertTrue(response.headers().firstValue("Allow").orElse("").contains("GET"));
    }

    @Test
    void frontendPageKeepsSecurityHeadersAndRejectsUnsupportedMethods() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest getRequest = HttpRequest.newBuilder(endpoint("/"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertEquals("nosniff", getResponse.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY", getResponse.headers().firstValue("X-Frame-Options").orElse(""));

        HttpRequest postRequest = HttpRequest.newBuilder(endpoint("/"))
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, postResponse.statusCode());
        assertTrue(postResponse.headers().firstValue("Allow").orElse("").contains("GET"));
    }

    @Test
    void frontendRendersRemoteProblemDescriptionsAsTextNotHtml() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(endpoint("/"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("function renderPlainTextWithBreaks"),
                "frontend should use a text-node renderer for remote problem descriptions");
        assertTrue(response.body().contains("document.createTextNode(line)"),
                "remote problem description lines must enter the DOM as text nodes");
        assertFalse(response.body().contains("problem-desc').innerHTML = currentProblem.description"),
                "remote problem descriptions must not be injected with innerHTML");
    }

    @Test
    void frontendRendersSubmissionHistoryRowsAsTextNotHtml() throws Exception {
        assertTrue(AlgoCraftWebServer.start(0));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(endpoint("/"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("function appendHistoryCell"),
                "frontend should use a text-node renderer for submission history cells");
        assertTrue(response.body().contains("td.textContent = text"),
                "remote problem titles in submission history must enter the DOM as text");
        assertFalse(response.body().contains("tr.innerHTML = `"),
                "submission history rows must not be built with innerHTML templates");
        assertFalse(response.body().contains("${record.problemTitle}"),
                "remote problem titles must not be interpolated into HTML");
    }

    private static HttpResponse<String> sendJson(String path, String body) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(endpoint(path))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String requestBody(String code, String problemId) {
        return "{\"code\":\"" + code + "\",\"problemId\":\"" + problemId + "\"}";
    }

    private static URI endpoint(String path) {
        return URI.create("http://127.0.0.1:" + AlgoCraftWebServer.getActivePort() + path);
    }
}
