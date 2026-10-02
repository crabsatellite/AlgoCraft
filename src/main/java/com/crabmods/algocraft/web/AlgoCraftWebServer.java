package com.crabmods.algocraft.web;

import com.crabmods.algocraft.Config;
import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemAssetResolver;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.ProgressManager;
import com.crabmods.algocraft.logic.SubmissionHistoryManager;
import com.crabmods.algocraft.logic.SubmissionRecord;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class AlgoCraftWebServer {
    private static final System.Logger LOGGER = System.getLogger(AlgoCraftWebServer.class.getName());
    private static final Object SERVER_LOCK = new Object();
    private static HttpServer server;
    private static ExecutorService executor;
    private static volatile int activePort = -1;
    private static volatile IOException lastStartError;
    private static final Gson gson = new Gson();
    public static final String MONACO_EDITOR_VERSION = "0.55.1";
    private static final int MAX_PROBLEM_ID_LENGTH = 256;
    private static final int WEB_EXECUTOR_MAX_THREADS = 4;
    private static final int WEB_EXECUTOR_QUEUE_CAPACITY = 64;
    private static final AtomicInteger WEB_THREAD_IDS = new AtomicInteger();
    private static final int WEB_JUDGE_MAX_CONCURRENT = 2;
    private static final Semaphore WEB_JUDGE_PERMITS = new Semaphore(WEB_JUDGE_MAX_CONCURRENT);
    private static final BiConsumer<Problem, String> NO_OP_SOLVED_PROBLEM_NOTIFIER = (problem, code) -> {};
    private static volatile BiConsumer<Problem, String> solvedProblemNotifier = NO_OP_SOLVED_PROBLEM_NOTIFIER;

    public static int getPort() {
        if (Boolean.getBoolean("algocraft.bankClientTest") || Boolean.getBoolean("algocraft.ideSmokeTest")) return Integer.getInteger("algocraft.webPort", 3000);
        try {
            return Config.WEB_SERVER_PORT.get();
        } catch (Throwable e) {
            return 3000;
        }
    }

    public static String getLocalUrl() {
        int port = getActivePort();
        return "http://localhost:" + (port > 0 ? port : getPort());
    }

    public static void setSolvedProblemNotifier(BiConsumer<Problem, String> notifier) {
        solvedProblemNotifier = notifier != null ? notifier : NO_OP_SOLVED_PROBLEM_NOTIFIER;
    }

    private static volatile java.util.function.BiFunction<Problem, String, java.util.concurrent.CompletableFuture<SubmissionResult>> submissionAction;
    public static void setSubmissionAction(java.util.function.BiFunction<Problem, String, java.util.concurrent.CompletableFuture<SubmissionResult>> action) { submissionAction = action; }
    // Rate limiting: per-IP request tracking
    private static int getMaxRequestsPerMinute() {
        try { return Config.RATE_LIMIT_PER_MINUTE.get(); }
        catch (Throwable e) { return 60; }
    }
    private static final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    // Maximum request body size (from config, default 100KB)
    private static int getMaxRequestBodySize() {
        try { return Config.MAX_REQUEST_SIZE.get(); }
        catch (Throwable e) { return 100_000; }
    }

    private static class RateLimitEntry {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();

        boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                // Reset window
                count.set(1);
                windowStart = now;
                return true;
            }
            return count.incrementAndGet() <= getMaxRequestsPerMinute();
        }
    }

    public static boolean start() {
        return start(getPort());
    }

    public static boolean start(int port) {
        synchronized (SERVER_LOCK) {
            if (server != null) {
                lastStartError = null;
                return true;
            }

            try {
                HttpServer createdServer = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), port),
                        0
                );
                createdServer.createContext("/", failClosed(new StaticHandler()));
                createdServer.createContext("/api/catalog", failClosed(exchange -> {
                    if (!"GET".equals(exchange.getRequestMethod())) { sendMethodNotAllowed(exchange, "GET"); return; }
                    if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
                    var state = new JsonObject();
                    state.addProperty("server", com.crabmods.algocraft.logic.catalog.ClientCatalog.serverId());
                    state.addProperty("revision", com.crabmods.algocraft.logic.catalog.ClientCatalog.revision());
                    sendResponse(exchange, gson.toJson(state));
                }));
                createdServer.createContext("/api/repositories", failClosed(new RepositoriesHandler()));
                createdServer.createContext("/api/problems", failClosed(new ProblemsHandler()));
                createdServer.createContext("/api/problem-image", failClosed(new ProblemImageHandler()));
                createdServer.createContext("/api/run", failClosed(new RunHandler()));
                createdServer.createContext("/api/submit", failClosed(new SubmitHandler()));
                createdServer.createContext("/api/history", failClosed(new HistoryHandler()));
                createdServer.createContext("/api/translations", failClosed(new TranslationsHandler()));
                ExecutorService createdExecutor = createBoundedExecutor();
                createdServer.setExecutor(createdExecutor);
                createdServer.start();

                server = createdServer;
                executor = createdExecutor;
                activePort = createdServer.getAddress().getPort();
                lastStartError = null;
                System.out.println("AlgoCraft Web Server started on port " + activePort);
                return true;
            } catch (IOException e) {
                lastStartError = e;
                activePort = -1;
                LOGGER.log(System.Logger.Level.WARNING,
                        "AlgoCraft Web Server failed to start on port " + port + ": " + e.getMessage(), e);
                return false;
            }
        }
    }

    public static void stop() {
        synchronized (SERVER_LOCK) {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            activePort = -1;
            lastStartError = null;
        }
    }

    public static boolean isRunning() {
        return server != null;
    }

    public static int getActivePort() {
        return activePort;
    }

    public static IOException getLastStartError() {
        return lastStartError;
    }

    static InetSocketAddress getActiveAddressForTest() {
        synchronized (SERVER_LOCK) {
            return server != null ? server.getAddress() : null;
        }
    }

    static ExecutorService getExecutorForTest() {
        synchronized (SERVER_LOCK) {
            return executor;
        }
    }

    static int getWebExecutorMaxThreadsForTest() {
        return WEB_EXECUTOR_MAX_THREADS;
    }

    static int getWebExecutorQueueCapacityForTest() {
        return WEB_EXECUTOR_QUEUE_CAPACITY;
    }

    static int getWebJudgeMaxConcurrentForTest() {
        return WEB_JUDGE_MAX_CONCURRENT;
    }

    private static ExecutorService createBoundedExecutor() {
        ThreadPoolExecutor boundedExecutor = new ThreadPoolExecutor(
                WEB_EXECUTOR_MAX_THREADS,
                WEB_EXECUTOR_MAX_THREADS,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(WEB_EXECUTOR_QUEUE_CAPACITY),
                task -> {
                    Thread thread = new Thread(task, "AlgoCraft-Web-" + WEB_THREAD_IDS.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        boundedExecutor.allowCoreThreadTimeOut(true);
        return boundedExecutor;
    }

    private static HttpHandler failClosed(HttpHandler handler) {
        return exchange -> {
            try {
                handler.handle(exchange);
            } catch (Throwable throwable) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "Unhandled AlgoCraft Web API error for " + exchange.getRequestURI(), throwable);
                try {
                    sendError(exchange, 500, "Internal server error");
                } catch (IOException sendFailure) {
                    LOGGER.log(System.Logger.Level.ERROR,
                            "Failed to send AlgoCraft Web API error response", sendFailure);
                    exchange.close();
                }
            }
        };
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            
            // In a real mod, you would load this from resources
            // For now, we will serve a simple HTML string
            String response = getFrontendHtml();
            
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class RepositoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            List<ProblemRepository> repos = com.crabmods.algocraft.logic.catalog.ClientCatalog.getRepositories();
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            for (ProblemRepository repo : repos) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("name", repo.getName());
                jsonArray.add(obj);
            }
            sendResponse(exchange, gson.toJson(jsonArray));
        }
    }

    static class ProblemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            String query = exchange.getRequestURI().getQuery();
            String repoName = null;
            String lang = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if (pair[0].equals("repo")) {
                            repoName = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        } else if (pair[0].equals("lang")) {
                            String l = pair[1].toLowerCase();
                            // Map browser locale to lang file format
                            if (l.equals("zh") || l.startsWith("zh-") || l.equals("zh_cn")) {
                                lang = "zh_cn";
                            } else if (l.equals("ja") || l.startsWith("ja-") || l.equals("ja_jp")) {
                                lang = "ja_jp";
                            }
                        }
                    }
                }
            }

            List<Problem> problems;
            if (repoName != null) {
                String finalRepoName = repoName;
                ProblemRepository repo = com.crabmods.algocraft.logic.catalog.ClientCatalog.getRepositories().stream()
                    .filter(r -> r.getName().equals(finalRepoName))
                    .findFirst()
                    .orElse(null);
                problems = repo != null ? repo.getProblems() : java.util.Collections.emptyList();
            } else {
                problems = com.crabmods.algocraft.logic.catalog.ClientCatalog.getProblems();
            }

            final String finalLang = lang;
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            for (Problem p : problems) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("id", p.getId());
                obj.addProperty("version", p.getPublicationVersion());
                obj.addProperty("scope", p.isPublished() ? "server" : "practice");
                // Use translated title/description if language specified
                obj.addProperty("title", p.getTitle(finalLang));
                obj.addProperty("description", p.getDescription(finalLang));
                obj.addProperty("difficulty", p.getDifficulty());
                obj.addProperty("initialCode", p.getInitialCode());
                obj.addProperty("passed", ProgressManager.isPassed(p.getId()));

                // Add tags
                com.google.gson.JsonArray tagsArray = new com.google.gson.JsonArray();
                for (String tag : p.getTags()) {
                    tagsArray.add(tag);
                }
                obj.add("tags", tagsArray);

                // Add examples
                com.google.gson.JsonArray examplesArray = new com.google.gson.JsonArray();
                for (Problem.TestCase ex : p.getExamples()) {
                    com.google.gson.JsonObject exObj = new com.google.gson.JsonObject();
                    exObj.addProperty("input", ex.getInput());
                    exObj.addProperty("output", ex.getOutput());
                    examplesArray.add(exObj);
                }
                obj.add("examples", examplesArray);
                com.google.gson.JsonArray visuals = new com.google.gson.JsonArray();
                for (Problem.Visual visual : p.getVisuals(finalLang)) {
                    JsonObject item = new JsonObject();
                    item.addProperty("file", visual.file());
                    item.addProperty("caption", visual.caption());
                    visuals.add(item);
                }
                obj.add("visuals", visuals);

                jsonArray.add(obj);
            }
            sendResponse(exchange, gson.toJson(jsonArray));
        }
    }

    static class ProblemImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> parameters = new java.util.HashMap<>();
            String query = exchange.getRequestURI().getRawQuery();
            if (query != null) for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2) parameters.put(pair[0], java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
            Problem problem = com.crabmods.algocraft.logic.catalog.ClientCatalog.getProblem(parameters.getOrDefault("problemId", ""));
            String file = parameters.getOrDefault("file", "");
            Problem.Visual visual = problem == null ? null : problem.getVisuals().stream()
                    .filter(v -> v.file().equals(file)).findFirst().orElse(null);
            var resolved = ProblemAssetResolver.resolveImage(problem, visual);
            if (resolved.isEmpty()) {
                sendError(exchange, 404, "Problem image not found");
                return;
            }
            java.nio.file.Path image = resolved.get();
            // Match the real paths as well, so a symlink cannot escape the repository.
            if (!image.toRealPath().startsWith(problem.getAssetBaseDir().toRealPath())) {
                sendError(exchange, 404, "Problem image not found");
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, java.nio.file.Files.size(image));
            try (OutputStream output = exchange.getResponseBody()) {
                java.nio.file.Files.copy(image, output);
            }
        }
    }

    static class RunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "POST");
                return;
            }
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            JsonObject body = readJsonObject(exchange);
            if (body == null) return;
            String code = requireStringField(exchange, body, "code");
            if (code == null) return;
            String problemId = requireStringField(exchange, body, "problemId");
            if (problemId == null) return;
            if (!validateProblemAndCodeFields(exchange, problemId, code)) return;

            Problem problem = com.crabmods.algocraft.logic.catalog.ClientCatalog.getProblem(problemId);
            if (problem == null) {
                sendError(exchange, 404, "Problem not found");
                return;
            }

            if (!acquireWebJudgePermit(exchange)) return;
            try {
                java.util.List<CodeExecutor.TestCase> testCases = new java.util.ArrayList<>();
                String preferredMethodName = CodeExecutor.preferredMethodNameFromInitialCode(problem.getInitialCode());
                for (Problem.TestCase test : problem.getExamples()) {
                    testCases.add(new CodeExecutor.TestCase(
                            test.getInput(),
                            test.getOutput(),
                            problem.getId(),
                            preferredMethodName
                    ));
                }
                java.util.List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch(code, testCases);

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < problem.getExamples().size(); i++) {
                    Problem.TestCase test = problem.getExamples().get(i);
                    CodeExecutor.TestResult result = results.get(i);
                    String res = result.passed ? "PASS" : (result.message != null ? result.message : "FAIL");
                    sb.append("Input: ").append(test.getInput()).append("\n");
                    sb.append("Result: ").append(res).append("\n\n");
                }

                JsonObject response = new JsonObject();
                response.addProperty("output", sb.toString());
                sendResponse(exchange, gson.toJson(response));
            } finally {
                releaseWebJudgePermit();
            }
        }
    }

    static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            List<SubmissionRecord> history = SubmissionHistoryManager.getHistory();
            sendResponse(exchange, gson.toJson(history));
        }
    }

    static class TranslationsHandler implements HttpHandler {
        private static final String[] SUPPORTED_LANGS = {"en_us", "zh_cn"};

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            
            // Get language parameter, default to en_us
            String query = exchange.getRequestURI().getQuery();
            String lang = "en_us";
            if (query != null && query.startsWith("lang=")) {
                String requestedLang = query.substring(5).toLowerCase();
                // Map browser locale to mod lang file
                if (requestedLang.equals("zh") || requestedLang.equals("zh_cn") || requestedLang.startsWith("zh-")) {
                    lang = "zh_cn";
                } else if (requestedLang.equals("ja") || requestedLang.startsWith("ja-")) {
                    lang = "ja_jp";
                }
                // Validate language is supported
                boolean isSupported = false;
                for (String supported : SUPPORTED_LANGS) {
                    if (supported.equals(lang)) {
                        isSupported = true;
                        break;
                    }
                }
                if (!isSupported) {
                    lang = "en_us";
                }
            }
            
            // Read language file from mod resources
            String langPath = "/assets/algocraft/lang/" + lang + ".json";
            String langJson = readResourceFile(langPath);
            
            if (langJson == null) {
                // Fallback to en_us
                langJson = readResourceFile("/assets/algocraft/lang/en_us.json");
            }
            
            if (langJson == null) {
                sendError(exchange, 500, "Translation resources unavailable");
                return;
            }
            
            // Filter only web.* translations to reduce payload
            JsonObject allTranslations = gson.fromJson(langJson, JsonObject.class);
            JsonObject webTranslations = new JsonObject();
            for (String key : allTranslations.keySet()) {
                if (key.startsWith("algocraft.web.")) {
                    // Convert key format: algocraft.web.nav.history -> nav.history
                    String shortKey = key.substring("algocraft.web.".length());
                    webTranslations.addProperty(shortKey, allTranslations.get(key).getAsString());
                }
            }
            
            sendResponse(exchange, gson.toJson(webTranslations));
        }
        
        private String readResourceFile(String path) {
            try (java.io.InputStream is = AlgoCraftWebServer.class.getResourceAsStream(path)) {
                if (is == null) return null;
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
    }

    static class SubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "POST");
                return;
            }
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            JsonObject body = readJsonObject(exchange);
            if (body == null) return;
            String code = requireStringField(exchange, body, "code");
            if (code == null) return;
            String problemId = requireStringField(exchange, body, "problemId");
            if (problemId == null) return;
            if (!validateProblemAndCodeFields(exchange, problemId, code)) return;

            Problem problem = com.crabmods.algocraft.logic.catalog.ClientCatalog.getProblem(problemId);
            if (problem == null) {
                sendError(exchange, 404, "Problem not found");
                return;
            }

            if (problem.isPublished() && (!body.has("version") || !body.get("version").isJsonPrimitive()
                    || !problem.getPublicationVersion().equals(body.get("version").getAsString()))) {
                sendError(exchange, 409, "Bank version changed. Reload the problem list."); return;
            }
            if (!acquireWebJudgePermit(exchange)) return;
            try {
                SubmissionResult result;
                var action = submissionAction;
                if (action == null) result = Judge.grade(problem, code);
                else {
                    try { result = action.apply(problem, code).get(125, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Exception e) { sendError(exchange, 503, "Server judge unavailable: " + e.getClass().getSimpleName()); return; }
                }

                // A successful web judge result only becomes visible history after
                // the game-thread handoff accepts it for server-side submission.
                if (result.isSuccess() && submissionAction == null) {
                    solvedProblemNotifier.accept(problem, code);
                }

                // Save submission record
                SubmissionHistoryManager.saveRecord(new SubmissionRecord(
                    System.currentTimeMillis(),
                    problem.getId(),
                    problem.getTitle(),
                    result.isSuccess() ? "Accepted" : (result.getMessage() != null ? result.getMessage() : "Wrong Answer"),
                    result.getExecutionTimeMs(),
                    result.getPassedCount(),
                    result.getTotalCount()
                ));

                sendResponse(exchange, gson.toJson(result));
            } finally {
                releaseWebJudgePermit();
            }
        }
    }

    /**
     * Check if the origin is an allowed localhost address.
     */
    private static boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        return origin.matches("https?://localhost(:\\d+)?")
            || origin.matches("https?://127\\.0\\.0\\.1(:\\d+)?");
    }

    /**
     * Check rate limit for the given IP. Returns true if request is allowed.
     */
    private static boolean checkRateLimit(HttpExchange exchange) {
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        RateLimitEntry entry = rateLimitMap.computeIfAbsent(clientIp, k -> new RateLimitEntry());
        return entry.tryAcquire();
    }

    /**
     * Handle CORS preflight OPTIONS requests.
     * Returns true if the request was an OPTIONS request and was handled.
     */
    private static boolean handleCorsPreflightIfNeeded(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    /**
     * Set CORS headers, restricting origin to localhost only.
     */
    private static void setCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (isAllowedOrigin(origin)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        }
    }

    /**
     * Read request body with size limit enforcement.
     * Returns null and sends 413 if body exceeds limit.
     */
    private static byte[] readRequestBody(HttpExchange exchange) throws IOException {
        int limit = getMaxRequestBodySize();
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(8192, limit))) {
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while ((read = exchange.getRequestBody().read(chunk)) != -1) {
                total += read;
                if (total > limit) {
                    sendError(exchange, 413, "Request body too large");
                    return null;
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private static JsonObject readJsonObject(HttpExchange exchange) throws IOException {
        byte[] rawBody = readRequestBody(exchange);
        if (rawBody == null) return null;

        try {
            JsonElement json = JsonParser.parseString(new String(rawBody, StandardCharsets.UTF_8));
            if (json == null || !json.isJsonObject()) {
                sendError(exchange, 400, "Invalid JSON object");
                return null;
            }
            return json.getAsJsonObject();
        } catch (JsonParseException e) {
            sendError(exchange, 400, "Invalid JSON");
            return null;
        }
    }

    private static String requireStringField(HttpExchange exchange, JsonObject body, String field) throws IOException {
        if (!body.has(field)
                || body.get(field).isJsonNull()
                || !body.get(field).isJsonPrimitive()
                || !body.get(field).getAsJsonPrimitive().isString()) {
            sendError(exchange, 400, "Missing or invalid required field: " + field);
            return null;
        }
        return body.get(field).getAsString();
    }

    private static boolean validateProblemAndCodeFields(HttpExchange exchange, String problemId, String code)
            throws IOException {
        if (!isProblemIdAcceptable(problemId)) {
            sendError(exchange, 400, "Problem id is empty or too large");
            return false;
        }
        if (!isCodeAcceptable(code)) {
            sendError(exchange, 400, "Code is empty or too large");
            return false;
        }
        return true;
    }

    private static boolean isProblemIdAcceptable(String problemId) {
        return problemId != null && !problemId.isBlank() && problemId.length() <= MAX_PROBLEM_ID_LENGTH;
    }

    private static boolean isCodeAcceptable(String code) {
        return code != null && !code.isBlank() && code.length() <= CodeExecutor.MAX_CODE_LENGTH;
    }

    private static boolean acquireWebJudgePermit(HttpExchange exchange) throws IOException {
        if (WEB_JUDGE_PERMITS.tryAcquire()) {
            return true;
        }
        sendError(exchange, 429, "Code execution is busy; please retry soon");
        return false;
    }

    private static void releaseWebJudgePermit() {
        WEB_JUDGE_PERMITS.release();
    }

    private static void sendMethodNotAllowed(HttpExchange exchange, String allow) throws IOException {
        exchange.getResponseHeaders().set("Allow", allow + ", OPTIONS");
        sendError(exchange, 405, "Method not allowed");
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        sendJson(exchange, status, gson.toJson(error));
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        sendJson(exchange, 200, response);
    }

    private static void sendJson(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void sendRateLimited(HttpExchange exchange) throws IOException {
        byte[] bytes = "{\"error\":\"Too many requests\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Retry-After", "60");
        exchange.sendResponseHeaders(429, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String getFrontendHtml() {
        return """
<!DOCTYPE html>
<html lang="en" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AlgoCraft IDE</title>
    <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAACR0lEQVR4nO3bwUtUQRwH8O/78bsIXQoig0IPYpIeggiiDooIIuJBaetPCAQ9SiB4iALR6GAQdPAP0BVFQkoQ0cLokFCQoOJNQUvxZHQ0eDCHt7zdmXnzeszz9z6XcWZ/M7vyHXff7o5AQbYg6cSrt1rPkQPHO1s1f0eCcEHS5Bdm38Fn/Y+fGu0EgnDsmvzm9h7yjCAcp5X8i7FJ+KTtwd2w/flls2YdQTi+qMmbIgjHuiu8as/yeU9eIQjHuoJ7t1tixxfnpuGD0ddvY8eP9g+M5hOEY9PCz99+4CIiCMemheW59/BJ6VFfKusQhGPbCYd/z1J9AOPlO2H7rPTdqP563SWjuvqbN8L2ZP9XzTqCcJx04p/fJ7Hjb9a7wnaofSXRetr5DWY7wBRBOE5rIdvkq9WrftKdpK4A1XOADkE4dl0gaVI6rjvBFEE4zjqptOtcEYRj3/5ms0peIQjHaS9YLTnTZLNKXiEIx1ndUdbJmiIIx64LdIw+sapfezkT6W9P3Y/0W4a/Oj0e0/cACkE4dv0coDJRW66JuyIIx7YTmhoa4bPi8wBLBOEIwnHW38T8b8V1gK/vBbKmOx2mEIRj2wlLq2uRfm9nR6q3V3pYOo30N8pXrJIvTotrBLpTYuoskK8nRD5+2kiUvEIQjnUFvib/anwqdtw0eYUgXJDX/wVyTV4hCMeVA5frr4Xt2MggfPZ8InpC9HhnK9E6BOEC9UNze+d5XPLzH5Yj/YGebq9ur9wJu+urxauA0w7Iu2IHFAqw8Q8Yg9SwOAMrNwAAAABJRU5ErkJggg==">
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/monaco-editor@0.55.1/min/vs/loader.min.js"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <script>
        // Internationalization (i18n) System - loads from mod lang files via API
        const i18n = {
            currentLang: 'en',
            translations: {},
            loaded: false,
            
            t(key) {
                return this.translations[key] || '';
            },
            
            async loadTranslations(lang) {
                try {
                    const langCode = lang === 'zh' ? 'zh_cn' : 'en_us';
                    const res = await fetch('/api/translations?lang=' + langCode);
                    if (res.ok) {
                        this.translations = await res.json();
                        this.loaded = true;
                    }
                } catch (e) {
                    console.error('Failed to load translations', e);
                }
            },
            
            async setLanguage(lang) {
                this.currentLang = lang;
                localStorage.setItem('algocraft-lang', lang);
                await this.loadTranslations(lang);
                this.updateUI();
                // Re-fetch problems with new language
                if (typeof fetchProblems === 'function') {
                    const repoSelect = document.getElementById('repo-select');
                    if (repoSelect && repoSelect.value) {
                        fetchProblems(repoSelect.value);
                    }
                }
            },
            
            detectLanguage() {
                const saved = localStorage.getItem('algocraft-lang');
                if (['en', 'zh'].includes(saved)) {
                    this.currentLang = saved;
                    return saved;
                }
                const browserLang = navigator.language.split('-')[0];
                if (['en', 'zh'].includes(browserLang)) {
                    this.currentLang = browserLang;
                    return browserLang;
                }
                return 'en';
            },
            
            updateUI() {
                document.querySelectorAll('[data-i18n]').forEach(el => {
                    const key = el.getAttribute('data-i18n');
                    el.textContent = this.t(key);
                });
                document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
                    const key = el.getAttribute('data-i18n-placeholder');
                    el.placeholder = this.t(key);
                });
            },
            
            async init() {
                const lang = this.detectLanguage();
                await this.loadTranslations(lang);
                this.updateUI();
            }
        };
    </script>
    <script>
        tailwind.config = {
            darkMode: 'class',
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Inter', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', 'sans-serif'],
                        mono: ['JetBrains Mono', 'Cascadia Mono', 'Consolas', 'monospace'],
                    },
                    // Same palette as the in-game IDE (IdeTheme.java).
                    colors: {
                        bg: '#0d1117',
                        surface: '#161b22',
                        surface2: '#21262d',
                        border: '#30363d',
                        text: '#e6edf3',
                        textMuted: '#9da7b3',
                        primary: '#238636',
                        primaryHover: '#2ea043',
                        success: '#238636',
                    }
                }
            }
        }
    </script>
    <style>
        body { background-color: #0d1117; color: #e6edf3; font-family: 'Inter', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; }

        /* Clean Scrollbar */
        ::-webkit-scrollbar { width: 10px; height: 10px; }
        ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb { background: #30363d; border-radius: 5px; border: 2px solid #0d1117; }
        ::-webkit-scrollbar-thumb:hover { background: #484f58; }

        /* AlgoCraft visual layer: shared palette with the in-game IDE. */
        nav { background: #161b22 !important; }
        #brand-mark { image-rendering: pixelated; object-fit: contain; }
        #sidebar { background: #11161d; }
        #sidebar h2 { font-size: 11px; letter-spacing: .08em; color: #8b949e; }
        #problem-list > div { border-color: #1f252d; padding-top: 10px; padding-bottom: 10px; }
        #problem-list > div .text-sm { font-size: 13.5px; }
        .diff-easy { color: #3fb950; }
        .diff-medium { color: #d29922; }
        .diff-hard { color: #f85149; }
        #problem-title { letter-spacing: -0.01em; margin-right: 10px; }
        #problem-difficulty { text-transform: uppercase; letter-spacing: .04em; font-weight: 600; }
        #problem-difficulty[data-level="easy"] { color: #3fb950; background: rgba(63,185,80,.12); border-color: rgba(63,185,80,.45); }
        #problem-difficulty[data-level="medium"] { color: #d29922; background: rgba(210,153,34,.12); border-color: rgba(210,153,34,.45); }
        #problem-difficulty[data-level="hard"] { color: #f85149; background: rgba(248,81,73,.12); border-color: rgba(248,81,73,.45); }
        #problem-visuals figure { margin: 4px 0 18px; }
        #problem-visuals img { margin: 0 auto; border: 1px solid #30363d; background: #0d1117; }
        #problem-visuals figcaption { text-align: center; }
        #problem-desc { color: #c9d1d9; font-size: 14px; line-height: 1.75; }
        .statement-heading { color: #e6edf3; font-size: 15px; font-weight: 600; margin: 20px 0 8px; padding-bottom: 6px; border-bottom: 1px solid #21262d; }
        .statement-paragraph { margin: 0 0 12px; }
        .statement-list { margin: 0 0 12px; padding-left: 22px; }
        ul.statement-list { list-style: disc; }
        ol.statement-list { list-style: decimal; }
        .statement-list li { margin: 3px 0; padding-left: 2px; }
        .statement-list li::marker { color: #6e7681; }
        .statement-code { font-family: 'JetBrains Mono', 'Cascadia Mono', Consolas, monospace; font-size: 12.5px; color: #e6edf3; background: #21262d; border: 1px solid #30363d; border-radius: 4px; padding: 1px 5px; overflow-wrap: anywhere; }
        .statement-pre { font-family: 'JetBrains Mono', 'Cascadia Mono', Consolas, monospace; font-size: 12.5px; line-height: 1.6; color: #e6edf3; background: #161b22; border: 1px solid #30363d; border-radius: 6px; padding: 10px 12px; margin: 0 0 12px; white-space: pre-wrap; overflow-wrap: anywhere; }
        #problem-desc strong { color: #e6edf3; font-weight: 600; }
        .example-block { font-family: 'JetBrains Mono', 'Cascadia Mono', Consolas, monospace; font-size: 12.5px; line-height: 1.6; color: #c9d1d9; background: #161b22; border: 1px solid #30363d; border-left: 3px solid #3fb950; border-radius: 6px; padding: 10px 12px; }
        #console-panel { background: #0d1117; }
        #console-panel > div:first-child { background: #161b22; }
        #output-container { color: #c9d1d9; }
        #editor-panel, #editor-panel > div:first-child { background: #0d1117 !important; border-color: #21262d !important; }
        #action-bar { background: #161b22; }
        #run-button { background: #21262d; border-color: #484f58; }
        #run-button:hover:not(:disabled) { background: #30363d; }
        #submit-button { background: #238636; box-shadow: 0 0 0 1px rgba(63,185,80,.5); }
        #submit-button:hover:not(:disabled) { background: #2ea043; }
        #success-content, #history-content { background: #161b22; border-color: #30363d; }

        .animate-fade-in { animation: fadeIn 0.2s ease-out; }
        #workspace, #workspace > div { min-width: 0; min-height: 0; }
        nav { flex-shrink: 0; gap: 12px; flex-wrap: wrap; height: auto !important; min-height: 56px; padding-block: 8px; }
        nav > div { min-width: 0; flex-wrap: wrap; gap: 8px; }
        #repo-select { max-width: 160px; }
        #sidebar { flex: 0 0 220px; }
        #statement-panel { flex: 0 0 35%; }
        #statement-scroll { min-height: 0; padding: 24px; overflow-wrap: anywhere; }
        #problem-heading { flex-wrap: wrap; gap: 8px; }
        #problem-title { min-width: 0; overflow-wrap: anywhere; }
        #problem-tags span, #output-container { overflow-wrap: anywhere; }
        #problem-visuals img { max-width: 100%; height: auto; border-radius: 6px; }
        #problem-visuals figcaption { margin-block: 6px 16px; color: #a1a1aa; font-size: 13px; }
        #examples-container pre { white-space: pre-wrap; overflow-wrap: anywhere; }
        #editor-panel { min-width: 260px; }
        #action-bar { flex-shrink: 0; flex-wrap: wrap; gap: 8px; height: auto; min-height: 64px; padding: 12px; }
        #status-text { max-width: 170px; overflow-wrap: anywhere; }
        #output-container { min-height: 0; }
        #console-panel { flex: 0 0 30%; min-height: 100px; }
        button:disabled { opacity: 0.5; cursor: wait; }
        @media (max-width: 900px) {
            body { height: auto !important; min-height: 100vh; overflow-y: auto !important; }
            nav { padding-inline: 12px !important; }
            #workspace { flex-direction: column; overflow: visible; }
            #sidebar { flex: none; width: 100%; max-height: 190px; }
            #sidebar > div:first-child { padding: 8px 12px; }
            #problem-list { min-height: 60px; }
            #statement-panel { flex: none; width: 100%; height: auto; }
            #statement-scroll { flex: none; max-height: 72vh; }
            #console-panel { flex: none; height: 160px; }
            #statement-scroll { padding: 16px; }
            #editor-panel { flex: none; width: 100%; min-width: 0; height: 520px; }
            #history-modal-content table { table-layout: fixed; overflow-wrap: anywhere; }
        }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
    </style>
</head>
<body class="h-screen flex flex-col overflow-hidden bg-bg selection:bg-primary/30">
    <!-- Top Navigation -->
    <nav class="h-14 border-b border-border bg-bg flex items-center justify-between px-6 z-20">
        <div class="flex items-center space-x-3">
            <img id="brand-mark" class="w-8 h-8" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAACR0lEQVR4nO3bwUtUQRwH8O/78bsIXQoig0IPYpIeggiiDooIIuJBaetPCAQ9SiB4iALR6GAQdPAP0BVFQkoQ0cLokFCQoOJNQUvxZHQ0eDCHt7zdmXnzeszz9z6XcWZ/M7vyHXff7o5AQbYg6cSrt1rPkQPHO1s1f0eCcEHS5Bdm38Fn/Y+fGu0EgnDsmvzm9h7yjCAcp5X8i7FJ+KTtwd2w/flls2YdQTi+qMmbIgjHuiu8as/yeU9eIQjHuoJ7t1tixxfnpuGD0ddvY8eP9g+M5hOEY9PCz99+4CIiCMemheW59/BJ6VFfKusQhGPbCYd/z1J9AOPlO2H7rPTdqP563SWjuvqbN8L2ZP9XzTqCcJx04p/fJ7Hjb9a7wnaofSXRetr5DWY7wBRBOE5rIdvkq9WrftKdpK4A1XOADkE4dl0gaVI6rjvBFEE4zjqptOtcEYRj3/5ms0peIQjHaS9YLTnTZLNKXiEIx1ndUdbJmiIIx64LdIw+sapfezkT6W9P3Y/0W4a/Oj0e0/cACkE4dv0coDJRW66JuyIIx7YTmhoa4bPi8wBLBOEIwnHW38T8b8V1gK/vBbKmOx2mEIRj2wlLq2uRfm9nR6q3V3pYOo30N8pXrJIvTotrBLpTYuoskK8nRD5+2kiUvEIQjnUFvib/anwqdtw0eYUgXJDX/wVyTV4hCMeVA5frr4Xt2MggfPZ8InpC9HhnK9E6BOEC9UNze+d5XPLzH5Yj/YGebq9ur9wJu+urxauA0w7Iu2IHFAqw8Q8Yg9SwOAMrNwAAAABJRU5ErkJggg==" alt="AlgoCraft pixel computer">
            <span class="font-semibold text-lg tracking-tight text-white">AlgoCraft</span>
            <select id="repo-select" onchange="changeRepo()" class="bg-surface border border-border rounded-md px-2 py-1 text-sm text-text focus:outline-none focus:border-primary ml-4">
                <option value="">Loading...</option>
            </select>
        </div>
        
        <div class="flex items-center space-x-4">
            <!-- Language Selector -->
            <select id="lang-select" onchange="i18n.setLanguage(this.value)" class="bg-surface border border-border rounded-md px-2 py-1 text-sm text-text focus:outline-none focus:border-primary">
                <option value="en">EN</option>
                <option value="zh">中文</option>
            </select>
            <button onclick="showHistory()" class="px-3 py-1.5 bg-surface hover:bg-surface2 border border-border rounded-md text-sm font-medium transition-colors" data-i18n="nav.history">History</button>
            <div class="flex bg-surface rounded-md border border-border">
                <button onclick="prevProblem()" class="p-1.5 hover:bg-surface2 rounded-l-md transition-colors text-textMuted hover:text-white border-r border-border">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path></svg>
                </button>
                <span class="px-4 py-1 text-sm font-medium text-text flex items-center min-w-[80px] justify-center" id="problem-counter">1 / 1</span>
                <button onclick="nextProblem()" class="p-1.5 hover:bg-surface2 rounded-r-md transition-colors text-textMuted hover:text-white border-l border-border">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
                </button>
            </div>
        </div>
    </nav>

    <div id="workspace" class="flex-1 flex overflow-hidden">
        <!-- Sidebar: Problem List -->
        <div id="sidebar" class="w-64 flex flex-col border-r border-border bg-surface">
            <div class="p-4 border-b border-border">
                <h2 class="text-sm font-semibold text-textMuted uppercase tracking-wider mb-2" data-i18n="sidebar.problems">Problems</h2>
                <input type="text" id="sidebar-search" data-i18n-placeholder="sidebar.filter" placeholder="Filter..." class="w-full bg-bg border border-border rounded-md px-3 py-1.5 text-sm text-text focus:outline-none focus:border-primary transition-colors" oninput="filterSidebar()">
            </div>
            <div class="flex-1 overflow-y-auto custom-scrollbar" id="problem-list">
                <!-- Problem items injected here -->
            </div>
        </div>

        <!-- Middle Panel: Problem Description -->
        <div id="statement-panel" class="w-[35%] flex flex-col border-r border-border bg-bg">
            <div id="statement-scroll" class="flex-1 overflow-y-auto p-8 custom-scrollbar">
                <div class="animate-fade-in">
                    <div id="problem-heading" class="flex items-center mb-2">
                        <h1 class="text-2xl font-semibold text-white" id="problem-title">Loading...</h1>
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-surface2 text-text border border-border" id="problem-difficulty">Easy</span>
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-900/30 text-green-400 border border-green-900/50 hidden" id="problem-passed" data-i18n="problem.passed">Passed</span>
                    </div>
                    
                    <div class="flex flex-wrap gap-2 mb-6" id="problem-tags"></div>
                    
                    <div id="problem-visuals"></div>
                    <div class="prose prose-invert prose-sm max-w-none text-textMuted leading-relaxed" id="problem-desc">
                        <!-- Description injected here -->
                    </div>

                    <!-- Examples Section (Dynamic) -->
                    <div class="mt-8 space-y-4" id="examples-container">
                        <!-- Examples injected here -->
                    </div>
                </div>
            </div>
            
            <!-- Console / Output Panel -->
            <div id="console-panel" class="h-1/3 border-t border-border bg-surface flex flex-col">
                <div class="flex items-center justify-between px-4 py-2 border-b border-border bg-surface2">
                    <span class="text-xs font-semibold text-textMuted uppercase tracking-wider" data-i18n="console.title">Console</span>
                    <button onclick="clearConsole()" class="text-xs text-textMuted hover:text-white transition-colors" data-i18n="console.clear">Clear</button>
                </div>
                <div class="flex-1 p-4 overflow-y-auto font-mono text-sm" id="output-container">
                    <div class="text-textMuted italic" data-i18n="console.ready">Ready to run...</div>
                </div>
            </div>
        </div>

        <!-- Right Panel: Code Editor -->
        <div id="editor-panel" class="flex-1 flex flex-col bg-[#1e1e1e] relative">
            <!-- Editor Toolbar -->
            <div class="h-10 bg-[#1e1e1e] border-b border-[#2b2b2b] flex items-center px-4 space-x-4">
                <div class="flex items-center space-x-2 text-xs text-textMuted">
                    <span class="w-2 h-2 rounded-full bg-primary"></span>
                    <span data-i18n="editor.language">Java</span>
                </div>
            </div>
            
            <div id="editor-container" class="flex-1"></div>
            
            <!-- Action Bar -->
            <div id="action-bar" class="h-16 border-t border-border bg-surface flex items-center justify-between px-6">
                <div class="text-sm text-textMuted flex items-center space-x-2">
                    <div class="w-2 h-2 rounded-full bg-green-500" id="status-dot"></div>
                    <span id="status-text" data-i18n="editor.ready">Ready</span>
                </div>
                <div class="flex items-center space-x-3">
                    <button id="run-button" onclick="runCode()" class="px-5 py-2 rounded-md bg-surface2 border border-border text-text hover:bg-border hover:text-white transition-all font-medium text-sm flex items-center space-x-2">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span data-i18n="editor.run">Run</span>
                    </button>
                    <button id="submit-button" onclick="submitCode()" class="px-6 py-2 rounded-md bg-success text-white font-medium text-sm hover:bg-green-700 transition-all flex items-center space-x-2 shadow-sm">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        <span data-i18n="editor.submit">Submit</span>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Success Overlay -->
    <div id="success-modal" class="fixed inset-0 bg-black/50 backdrop-blur-sm hidden flex items-center justify-center z-50 transition-opacity duration-200 opacity-0">
        <div class="bg-surface border border-border p-8 rounded-xl shadow-2xl transform scale-95 transition-all duration-200 max-w-sm w-full text-center" id="success-content">
            <div class="w-16 h-16 bg-green-500/10 rounded-full flex items-center justify-center mx-auto mb-6">
                <svg class="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
            </div>
            <h2 class="text-2xl font-semibold text-white mb-2" data-i18n="success.title">Accepted</h2>
            <p class="text-textMuted mb-6 text-sm" data-i18n="success.message">All test cases passed successfully.</p>
            
            <div class="grid grid-cols-2 gap-3 mb-6">
                <div class="bg-bg p-3 rounded-lg border border-border">
                    <div class="text-[10px] text-textMuted uppercase font-bold tracking-wider" data-i18n="success.runtime">Runtime</div>
                    <div class="text-text font-mono font-medium mt-1" id="result-time">0 ms</div>
                </div>
                <div class="bg-bg p-3 rounded-lg border border-border">
                    <div class="text-[10px] text-textMuted uppercase font-bold tracking-wider" data-i18n="success.passed">Passed</div>
                    <div class="text-text font-mono font-medium mt-1" id="result-passed">All</div>
                </div>
            </div>

            <button onclick="closeModal()" class="w-full py-2.5 bg-primary text-white font-medium rounded-lg hover:bg-primaryHover transition-colors text-sm" data-i18n="success.continue">
                Continue
            </button>
        </div>
    </div>

    <!-- History Modal -->
    <div id="history-modal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm hidden opacity-0 transition-opacity duration-300">
        <div id="history-content" class="bg-surface border border-border rounded-lg shadow-2xl w-full max-w-2xl transform scale-95 transition-transform duration-300 flex flex-col max-h-[80vh]">
            <div class="p-6 border-b border-border flex justify-between items-center">
                <h3 class="text-xl font-bold text-white" data-i18n="history.title">Submission History</h3>
                <button onclick="closeHistoryModal()" class="text-textMuted hover:text-white">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
            </div>
            <div class="p-6 overflow-y-auto flex-1">
                <table class="w-full text-left text-sm">
                    <thead>
                        <tr class="text-textMuted border-b border-border">
                            <th class="pb-2" data-i18n="history.time">Time</th>
                            <th class="pb-2" data-i18n="history.problem">Problem</th>
                            <th class="pb-2" data-i18n="history.status">Status</th>
                            <th class="pb-2" data-i18n="history.runtime">Runtime</th>
                        </tr>
                    </thead>
                    <tbody id="history-list">
                        <!-- Items -->
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script>
        let editor;
        let currentProblem = null;
        let problems = [];
        let currentIndex = 0;
        let executionPending = false;
        let problemFetchVersion = 0;
        let draftScope = '';
        let catalogRevision = null;
        const draftViews = new Map();
        const memoryDrafts = new Map();

        function saveCurrentDraft() {
            if (!editor || !currentProblem) return;
            const code = editor.getValue();
            memoryDrafts.set(currentProblem.id, code);
            draftViews.set(currentProblem.id, editor.saveViewState());
            try { localStorage.setItem('algocraft-code:' + draftScope + ':' + currentProblem.id, code); } catch (_) {}
        }

        function initialDraft(problem) {
            if (memoryDrafts.has(problem.id)) return memoryDrafts.get(problem.id);
            try {
                const saved = localStorage.getItem('algocraft-code:' + draftScope + ':' + problem.id);
                if (saved !== null) return saved;
            } catch (_) {}
            return problem.initialCode || '';
        }

        window.addEventListener('beforeunload', saveCurrentDraft);
        function setExecutionPending(pending) {
            executionPending = pending;
            document.getElementById('run-button').disabled = pending || !currentProblem;
            document.getElementById('submit-button').disabled = pending || !currentProblem;
        }

        require.config({ paths: { 'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@0.55.1/min/vs' }});
        require(['vs/editor/editor.main'], async function() {
            // Initialize i18n first
            await i18n.init();
            document.getElementById('lang-select').value = i18n.currentLang;
            
            monaco.editor.defineTheme('algocraft-dark', {
                base: 'vs-dark',
                inherit: true,
                rules: [
                    { token: 'keyword', foreground: 'ff7b72' },
                    { token: 'type', foreground: 'ffa657' },
                    { token: 'number', foreground: '79c0ff' },
                    { token: 'string', foreground: 'a5d6ff' },
                    { token: 'comment', foreground: '8b949e', fontStyle: 'italic' }
                ],
                colors: {
                    'editor.background': '#0d1117',
                    'editor.foreground': '#e6edf3',
                    'editorLineNumber.foreground': '#6e7681',
                    'editorLineNumber.activeForeground': '#e6edf3',
                    'editor.lineHighlightBackground': '#161b22',
                    'editor.selectionBackground': '#1f3a5f',
                    'editorCursor.foreground': '#58a6ff',
                    'editorIndentGuide.background1': '#21262d',
                    'editorGutter.background': '#0d1117'
                }
            });
            editor = monaco.editor.create(document.getElementById('editor-container'), {
                value: '// Loading...',
                language: 'java',
                theme: 'algocraft-dark',
                automaticLayout: true,
                minimap: { enabled: false },
                fontSize: 15,
                fontFamily: "'JetBrains Mono', Consolas, 'Courier New', monospace",
                lineHeight: 24,
                padding: { top: 20 },
                scrollBeyondLastLine: false,
                smoothScrolling: true,
                cursorBlinking: 'blink',
                cursorSmoothCaretAnimation: 'off'
            });
            document.fonts.ready.then(() => monaco.editor.remeasureFonts());
            editor.onDidChangeModelContent(saveCurrentDraft);
            setExecutionPending(false);
            pollCatalog();
            setInterval(pollCatalog, 5000);
        });
        async function pollCatalog() {
            if (executionPending) return;
            try {
                const state = await readJsonResponse(await fetch('/api/catalog'));
                if (state.server !== draftScope) {
                    saveCurrentDraft();
                    draftScope = state.server;
                    memoryDrafts.clear(); draftViews.clear(); currentProblem = null;
                }
                if (state.revision !== catalogRevision) {
                    catalogRevision = state.revision;
                    await fetchRepos();
                }
            } catch (_) {}
        }

        async function fetchRepos() {
            try {
                const res = await fetch('/api/repositories');
                const repos = await res.json();
                const select = document.getElementById('repo-select');
                const previousRepo = select.value;
                select.innerHTML = '';
                repos.forEach(repo => {
                    const option = document.createElement('option');
                    option.value = repo.name;
                    option.innerText = repo.name;
                    select.appendChild(option);
                });
                if (repos.length === 0) await fetchProblems(null);
                if (repos.length > 0) {
                    const selected = repos.find(repo => repo.name === previousRepo) || repos[0];
                    select.value = selected.name;
                    await fetchProblems(selected.name);
                }
            } catch (e) {
                console.error("Failed to fetch repos", e);
            }
        }

        async function fetchProblems(repoName) {
            const fetchVersion = ++problemFetchVersion;
            const selectedId = currentProblem && currentProblem.id;
            saveCurrentDraft();
            try {
                let url = '/api/problems?lang=' + encodeURIComponent(i18n.currentLang);
                if (repoName) {
                    url += '&repo=' + encodeURIComponent(repoName);
                }
                const res = await fetch(url);
                const fetched = await readJsonResponse(res);
                if (fetchVersion !== problemFetchVersion) return;
                problems = fetched;
                renderSidebar();
                if (problems.length > 0) {
                    const selectedIndex = problems.findIndex(p => p.id === selectedId);
                    loadProblem(selectedIndex >= 0 ? selectedIndex : 0);
                } else {
                    currentProblem = null;
                    document.getElementById('problem-title').innerText = i18n.t('problems.empty') || 'No problems found';
                    document.getElementById('problem-desc').innerText = '';
                    editor.setValue('');
                    document.getElementById('problem-counter').innerText = '0 / 0';
                    setExecutionPending(executionPending);
                }
            } catch (e) {
                console.error("Failed to fetch problems", e);
            }
        }
        
        function renderSidebar() {
            const list = document.getElementById('problem-list');
            list.innerHTML = '';
            problems.forEach((p, idx) => {
                const div = document.createElement('div');
                div.className = `px-4 py-3 border-b border-border cursor-pointer hover:bg-surface2 transition-colors ${idx === currentIndex ? 'bg-surface2 border-l-2 border-l-primary' : ''}`;
                div.onclick = () => loadProblem(idx);
                
                const title = document.createElement('div');
                title.className = 'text-sm font-medium text-text truncate';
                title.innerText = p.title;
                
                const meta = document.createElement('div');
                meta.className = 'flex items-center justify-between mt-1';
                
                const diff = document.createElement('span');
                diff.className = `text-xs font-medium ${getDiffColor(p.difficulty)}`;
                diff.innerText = p.difficulty || 'Medium';
                
                meta.appendChild(diff);
                
                if (p.passed) {
                    const passed = document.createElement('span');
                    passed.className = 'text-xs text-green-500';
                    passed.innerText = i18n.t('problem.passed') || 'Passed';
                    meta.appendChild(passed);
                }
                
                div.appendChild(title);
                div.appendChild(meta);
                list.appendChild(div);
            });
        }
        
        function getDiffColor(diff) {
            if (!diff) return 'diff-medium';
            const d = diff.toLowerCase();
            if (d === 'easy') return 'diff-easy';
            if (d === 'hard') return 'diff-hard';
            return 'diff-medium';
        }
        
        function filterSidebar() {
            const query = document.getElementById('sidebar-search').value.toLowerCase();
            const items = document.getElementById('problem-list').children;
            Array.from(items).forEach((item, idx) => {
                const p = problems[idx];
                const match = p.title.toLowerCase().includes(query) || (p.tags && p.tags.some(t => t.toLowerCase().includes(query)));
                item.style.display = match ? 'block' : 'none';
            });
        }
        
        function changeRepo() {
            const repoName = document.getElementById('repo-select').value;
            fetchProblems(repoName);
        }

        function loadProblem(index) {
            if (index < 0 || index >= problems.length) return;
            saveCurrentDraft();
            
            // Update active state in sidebar
            const list = document.getElementById('problem-list');
            if (list.children[currentIndex]) {
                list.children[currentIndex].classList.remove('bg-surface2', 'border-l-2', 'border-l-primary');
            }
            if (list.children[index]) {
                list.children[index].classList.add('bg-surface2', 'border-l-2', 'border-l-primary');
                list.children[index].scrollIntoView({ block: 'nearest' });
            }
            
            currentIndex = index;
            currentProblem = problems[index];
            
            document.getElementById('problem-title').innerText = currentProblem.title;
            document.getElementById('problem-difficulty').innerText = currentProblem.difficulty || 'Medium';
            document.getElementById('problem-difficulty').dataset.level = String(currentProblem.difficulty || 'medium').toLowerCase();
            
            const passedBadge = document.getElementById('problem-passed');
            if (currentProblem.passed) {
                passedBadge.classList.remove('hidden');
            } else {
                passedBadge.classList.add('hidden');
            }

            // Render Tags
            const tagsContainer = document.getElementById('problem-tags');
            tagsContainer.innerHTML = '';
            if (currentProblem.tags) {
                currentProblem.tags.forEach(tag => {
                    const span = document.createElement('span');
                    span.className = 'px-2 py-0.5 rounded text-xs font-medium bg-surface2 text-textMuted border border-border';
                    span.innerText = tag;
                    tagsContainer.appendChild(span);
                });
            }

            renderStatement(document.getElementById('problem-desc'), currentProblem.description || '', currentProblem.title);
            const visuals = document.getElementById('problem-visuals');
            visuals.replaceChildren();
            (currentProblem.visuals || []).forEach(visual => {
                const figure = document.createElement('figure');
                const image = document.createElement('img');
                image.src = '/api/problem-image?problemId=' + encodeURIComponent(currentProblem.id) + '&file=' + encodeURIComponent(visual.file);
                image.alt = visual.caption || currentProblem.title;
                figure.appendChild(image);
                const caption = document.createElement('figcaption');
                caption.textContent = visual.caption || '';
                figure.appendChild(caption);
                visuals.appendChild(figure);
            });
            const examples = document.getElementById('examples-container');
            examples.replaceChildren();
            (currentProblem.examples || []).forEach((example, idx) => {
                const block = document.createElement('pre');
                block.className = 'example-block';
                block.textContent = (idx + 1) + '. ' + (i18n.t('console.input') || 'Input') + ': ' + example.input
                    + '\\n' + (i18n.t('console.expected') || 'Expected') + ': ' + example.output;
                examples.appendChild(block);
            });
            document.getElementById('problem-counter').innerText = `${currentIndex + 1} / ${problems.length}`;
            
            // One stable model avoids Monaco cancelling unhandled background
            // requests on model switches. Per-problem drafts/views live above.
            const draft = initialDraft(currentProblem);
            if (editor.getValue() !== draft) editor.setValue(draft);
            if (draftViews.has(currentProblem.id)) editor.restoreViewState(draftViews.get(currentProblem.id));
            setExecutionPending(executionPending);
            clearConsole();
        }

        // Small Markdown subset built from DOM text nodes only: headings, lists,
        // code fences, inline code and bold. The leading "# Title" is skipped
        // because the panel header already shows the title.
        function renderStatement(container, text, title) {
            container.replaceChildren();
            const normalizedTitle = String(title || '').trim().toLowerCase();
            let paragraph = null;
            let list = null;
            let pre = null;
            let sawContent = false;
            String(text).split('\\n').forEach(raw => {
                const line = raw.replace('\\r', '');
                const trimmed = line.trim();
                if (trimmed.startsWith('```')) {
                    if (pre) {
                        pre = null;
                    } else {
                        pre = document.createElement('pre');
                        pre.className = 'statement-pre';
                        container.appendChild(pre);
                        paragraph = null;
                        list = null;
                    }
                    return;
                }
                if (pre) {
                    pre.appendChild(document.createTextNode((pre.childNodes.length ? '\\n' : '') + line));
                    return;
                }
                if (!trimmed) {
                    paragraph = null;
                    list = null;
                    return;
                }
                let level = 0;
                while (level < trimmed.length && level < 6 && trimmed.charAt(level) === '#') level++;
                if (level > 0 && trimmed.charAt(level) === ' ') {
                    const heading = trimmed.slice(level).trim();
                    paragraph = null;
                    list = null;
                    if (!sawContent && heading.toLowerCase() === normalizedTitle) return;
                    const element = document.createElement('h3');
                    element.className = 'statement-heading';
                    appendInlineMarkdown(element, heading);
                    container.appendChild(element);
                    sawContent = true;
                    return;
                }
                sawContent = true;
                const bullet = trimmed.startsWith('- ') || trimmed.startsWith('* ');
                const number = parseInt(trimmed, 10);
                const ordered = !bullet && !isNaN(number) && /^[0-9]+[.)] /.test(trimmed);
                if (bullet || ordered) {
                    const tag = bullet ? 'UL' : 'OL';
                    if (!list || list.tagName !== tag) {
                        list = document.createElement(tag);
                        list.className = 'statement-list';
                        container.appendChild(list);
                    }
                    paragraph = null;
                    const item = document.createElement('li');
                    if (ordered) item.value = number;
                    appendInlineMarkdown(item, bullet ? trimmed.slice(2) : trimmed.slice(trimmed.indexOf(' ') + 1));
                    list.appendChild(item);
                    return;
                }
                list = null;
                if (!paragraph) {
                    paragraph = document.createElement('p');
                    paragraph.className = 'statement-paragraph';
                    container.appendChild(paragraph);
                } else {
                    paragraph.appendChild(document.createElement('br'));
                }
                appendInlineMarkdown(paragraph, trimmed);
            });
        }

        function appendInlineMarkdown(parent, text) {
            String(text).split('`').forEach((part, index) => {
                if (index % 2 === 1) {
                    const code = document.createElement('code');
                    code.className = 'statement-code';
                    code.textContent = part;
                    parent.appendChild(code);
                    return;
                }
                part.split('**').forEach((piece, pieceIndex) => {
                    if (!piece) return;
                    if (pieceIndex % 2 === 1) {
                        const strong = document.createElement('strong');
                        strong.textContent = piece;
                        parent.appendChild(strong);
                    } else {
                        parent.appendChild(document.createTextNode(piece));
                    }
                });
            });
        }

        function renderPlainTextWithBreaks(container, text) {
            container.replaceChildren();
            String(text).split('\\n').forEach((line, index) => {
                if (index > 0) {
                    container.appendChild(document.createElement('br'));
                }
                container.appendChild(document.createTextNode(line));
            });
        }

        function prevProblem() { loadProblem((currentIndex - 1 + problems.length) % problems.length); }
        function nextProblem() { loadProblem((currentIndex + 1) % problems.length); }
        function clearConsole() { document.getElementById('output-container').innerHTML = '<div class="text-textMuted italic">' + (i18n.t('console.ready') || 'Ready to run...') + '</div>'; }

        function appendOutput(text, type = 'info') {
            const container = document.getElementById('output-container');
            if (container.children.length === 1 && container.children[0].classList.contains('italic')) {
                container.innerHTML = '';
            }
            
            const div = document.createElement('div');
            div.className = 'mb-2 font-mono whitespace-pre-wrap ' + (type === 'error' ? 'text-red-400' : 'text-text');
            div.innerText = text;
            container.appendChild(div);
            container.scrollTop = container.scrollHeight;
        }

        async function readJsonResponse(res) {
            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.error || data.message || ('HTTP ' + res.status));
            }
            return data;
        }

        async function runCode() {
            if (!currentProblem || !editor || executionPending) return;
            const runningProblem = currentProblem;
            setExecutionPending(true);
            const code = editor.getValue();
            document.getElementById('status-text').innerText = i18n.t('editor.running') || 'Running...';
            document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-yellow-500 animate-pulse';
            clearConsole();
            appendOutput(i18n.t('console.compiling') || 'Compiling and running...', 'info');
            
            try {
                const res = await fetch('/api/run', {
                    method: 'POST',
                    body: JSON.stringify({ code, problemId: runningProblem.id })
                });
                const data = await readJsonResponse(res);
                if (!currentProblem || currentProblem.id !== runningProblem.id) return;
                appendOutput(data.output || '');
                document.getElementById('status-text').innerText = i18n.t('editor.ready') || 'Ready';
                document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-green-500';
            } catch (e) {
                if (!currentProblem || currentProblem.id !== runningProblem.id) return;
                appendOutput(e.message || i18n.t('console.network_error') || 'Network Error', 'error');
                document.getElementById('status-text').innerText = i18n.t('editor.error') || 'Error';
                document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-red-500';
            } finally {
                setExecutionPending(false);
            }
        }

        async function submitCode() {
            if (!currentProblem || !editor || executionPending) return;
            const submittedProblem = currentProblem;
            setExecutionPending(true);
            const code = editor.getValue();
            document.getElementById('status-text').innerText = i18n.t('editor.judging') || 'Judging...';
            document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-yellow-500 animate-pulse';
            clearConsole();
            appendOutput(i18n.t('console.submitting') || 'Submitting solution...', 'info');
            
            try {
                const res = await fetch('/api/submit', {
                    method: 'POST',
                    body: JSON.stringify({ code, problemId: submittedProblem.id, version: submittedProblem.version })
                });
                const result = await readJsonResponse(res);
                if (result.isSuccess) submittedProblem.passed = true;
                if (!currentProblem || currentProblem.id !== submittedProblem.id || currentProblem.version !== submittedProblem.version) return;
                
                if (result.isSuccess) {
                    document.getElementById('result-time').innerText = (result.executionTimeMs || 0) + ' ms';
                    document.getElementById('result-passed').innerText = `${result.passedCount}/${result.totalCount}`;
                    showSuccess();
                    document.getElementById('problem-passed').classList.remove('hidden');
                    renderSidebar();
                    appendOutput(i18n.t('console.accepted') || 'Accepted!', 'info');
                } else {
                    let msg = `${i18n.t('console.status') || 'Status'}: ${result.message}\\n`;
                    if (result.details) {
                        const fail = result.details.find(d => !d.passed);
                        if (fail) {
                            msg += `${i18n.t('console.failed_on') || 'Failed on input'}: ${fail.input}\\n${i18n.t('console.expected') || 'Expected'}: ${fail.expected}\\n${i18n.t('console.got') || 'Got'}: ${fail.actual}`;
                            if (fail.error) msg += `\\n${i18n.t('console.error') || 'Error'}: ${fail.error}`;
                        }
                    }
                    appendOutput(msg, 'error');
                }
                document.getElementById('status-text').innerText = result.message;
                document.getElementById('status-dot').className = result.isSuccess ? 'w-2 h-2 rounded-full bg-green-500' : 'w-2 h-2 rounded-full bg-red-500';
            } catch (e) {
                if (!currentProblem || currentProblem.id !== submittedProblem.id || currentProblem.version !== submittedProblem.version) return;
                appendOutput(e.message || i18n.t('console.network_error') || 'Network Error', 'error');
                document.getElementById('status-text').innerText = i18n.t('editor.error') || 'Error';
                document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-red-500';
            } finally {
                setExecutionPending(false);
            }
        }

        function showSuccess() {
            const modal = document.getElementById('success-modal');
            const content = document.getElementById('success-content');
            modal.classList.remove('hidden');
            // Trigger reflow
            void modal.offsetWidth;
            modal.classList.remove('opacity-0');
            content.classList.remove('scale-95');
            content.classList.add('scale-100');
        }

        function closeModal() {
            const modal = document.getElementById('success-modal');
            const content = document.getElementById('success-content');
            modal.classList.add('opacity-0');
            content.classList.remove('scale-100');
            content.classList.add('scale-95');
            setTimeout(() => modal.classList.add('hidden'), 200);
        }

        async function showHistory() {
            const modal = document.getElementById('history-modal');
            const content = document.getElementById('history-content');
            const list = document.getElementById('history-list');
            
            setHistoryMessage(list, i18n.t('history.loading') || 'Loading...', 'py-4 text-center text-textMuted');
            
            modal.classList.remove('hidden');
            // Trigger reflow
            void modal.offsetWidth;
            modal.classList.remove('opacity-0');
            content.classList.remove('scale-95');
            content.classList.add('scale-100');

            try {
                const res = await fetch('/api/history');
                const history = await res.json();
                
                list.replaceChildren();
                if (history.length === 0) {
                    setHistoryMessage(list, i18n.t('history.empty') || 'No submissions yet', 'py-4 text-center text-textMuted');
                    return;
                }

                history.forEach(record => {
                    const date = new Date(record.timestamp).toLocaleString();
                    const statusColor = record.status === 'Accepted' ? 'diff-easy' : 'diff-hard';
                    
                    const tr = document.createElement('tr');
                    tr.className = 'border-b border-border/50 hover:bg-surface2 transition-colors';
                    appendHistoryCell(tr, date, 'py-3 text-textMuted');
                    appendHistoryCell(tr, record.problemTitle || '', 'py-3 font-medium text-white');
                    appendHistoryCell(tr, record.status || '', 'py-3 ' + statusColor);
                    appendHistoryCell(tr, (record.executionTime || 0) + ' ms', 'py-3 text-textMuted');
                    list.appendChild(tr);
                });
            } catch (e) {
                setHistoryMessage(list, i18n.t('history.error') || 'Failed to load history', 'py-4 text-center text-red-500');
            }
        }

        function setHistoryMessage(list, text, className) {
            list.replaceChildren();
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = 4;
            td.className = className;
            td.textContent = text;
            tr.appendChild(td);
            list.appendChild(tr);
        }

        function appendHistoryCell(row, text, className) {
            const td = document.createElement('td');
            td.className = className;
            td.textContent = text;
            row.appendChild(td);
        }

        function closeHistoryModal() {
            const modal = document.getElementById('history-modal');
            const content = document.getElementById('history-content');
            modal.classList.add('opacity-0');
            content.classList.remove('scale-100');
            content.classList.add('scale-95');
            setTimeout(() => modal.classList.add('hidden'), 300);
        }
    </script>
</body>
</html>
        """;
    }

}
