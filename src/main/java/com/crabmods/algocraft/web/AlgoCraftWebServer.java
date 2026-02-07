package com.crabmods.algocraft.web;

import com.crabmods.algocraft.Config;
import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.ProgressManager;
import com.crabmods.algocraft.logic.SubmissionHistoryManager;
import com.crabmods.algocraft.logic.SubmissionRecord;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.network.PacketSolveProblem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AlgoCraftWebServer {
    private static HttpServer server;
    private static final int PORT = 3000;
    private static final Gson gson = new Gson();

    // Rate limiting: per-IP request tracking
    private static int getMaxRequestsPerMinute() {
        try { return Config.RATE_LIMIT_PER_MINUTE.get(); }
        catch (Exception e) { return 60; }
    }
    private static final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    // Maximum request body size (from config, default 100KB)
    private static int getMaxRequestBodySize() {
        try { return Config.MAX_REQUEST_SIZE.get(); }
        catch (Exception e) { return 100_000; }
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

    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new StaticHandler());
            server.createContext("/api/repositories", new RepositoriesHandler());
            server.createContext("/api/problems", new ProblemsHandler());
            server.createContext("/api/run", new RunHandler());
            server.createContext("/api/submit", new SubmitHandler());
            server.createContext("/api/history", new HistoryHandler());
            server.createContext("/api/translations", new TranslationsHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("AlgoCraft Web Server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            
            // In a real mod, you would load this from resources
            // For now, we will serve a simple HTML string
            String response = getFrontendHtml();
            
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
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
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            if ("GET".equals(exchange.getRequestMethod())) {
                List<ProblemRepository> repos = ProblemManager.getRepositories();
                com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
                for (ProblemRepository repo : repos) {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                    obj.addProperty("name", repo.getName());
                    jsonArray.add(obj);
                }
                sendResponse(exchange, gson.toJson(jsonArray));
            }
        }
    }

    static class ProblemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            if ("GET".equals(exchange.getRequestMethod())) {
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
                    ProblemRepository repo = ProblemManager.getRepositories().stream()
                        .filter(r -> r.getName().equals(finalRepoName))
                        .findFirst()
                        .orElse(null);
                    problems = repo != null ? repo.getProblems() : java.util.Collections.emptyList();
                } else {
                    problems = ProblemManager.getProblems();
                }

                final String finalLang = lang;
                com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
                for (Problem p : problems) {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                    obj.addProperty("id", p.getId());
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
                    
                    jsonArray.add(obj);
                }
                sendResponse(exchange, gson.toJson(jsonArray));
            }
        }
    }

    static class RunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            if ("POST".equals(exchange.getRequestMethod())) {
                byte[] rawBody = readRequestBody(exchange);
                if (rawBody == null) return;
                JsonObject body = gson.fromJson(new String(rawBody, StandardCharsets.UTF_8), JsonObject.class);
                String code = body.get("code").getAsString();
                String problemId = body.get("problemId").getAsString();
                
                Problem problem = ProblemManager.getProblems().stream().filter(p -> p.getId().equals(problemId)).findFirst().orElse(null);
                if (problem == null) {
                    sendResponse(exchange, "{\"error\": \"Problem not found\"}");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (Problem.TestCase test : problem.getExamples()) {
                    String res = CodeExecutor.execute(code, test.getInput(), test.getOutput());
                    sb.append("Input: ").append(test.getInput()).append("\n");
                    sb.append("Result: ").append(res).append("\n\n");
                }
                
                JsonObject response = new JsonObject();
                response.addProperty("output", sb.toString());
                sendResponse(exchange, gson.toJson(response));
            }
        }
    }

    static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            if ("GET".equals(exchange.getRequestMethod())) {
                List<SubmissionRecord> history = SubmissionHistoryManager.getHistory();
                sendResponse(exchange, gson.toJson(history));
            }
        }
    }

    static class TranslationsHandler implements HttpHandler {
        private static final String[] SUPPORTED_LANGS = {"en_us", "zh_cn"};

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflightIfNeeded(exchange)) return;
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
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
                exchange.sendResponseHeaders(500, -1);
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
            if (!checkRateLimit(exchange)) { sendRateLimited(exchange); return; }
            if ("POST".equals(exchange.getRequestMethod())) {
                byte[] rawBody = readRequestBody(exchange);
                if (rawBody == null) return;
                JsonObject body = gson.fromJson(new String(rawBody, StandardCharsets.UTF_8), JsonObject.class);
                String code = body.get("code").getAsString();
                String problemId = body.get("problemId").getAsString();

                Problem problem = ProblemManager.getProblems().stream().filter(p -> p.getId().equals(problemId)).findFirst().orElse(null);
                if (problem == null) {
                    sendResponse(exchange, "{\"error\": \"Problem not found\"}");
                    return;
                }

                SubmissionResult result = Judge.grade(problem, code);
                
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

                // If success, we need to notify the server game thread
                if (result.isSuccess()) {
                    Minecraft.getInstance().execute(() -> {
                        PacketDistributor.sendToServer(new PacketSolveProblem(problem.getId(), problem.getDifficulty()));
                    });
                }

                sendResponse(exchange, gson.toJson(result));
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
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length > getMaxRequestBodySize()) {
            byte[] err = "{\"error\":\"Request body too large\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(413, err.length);
            exchange.getResponseBody().write(err);
            exchange.close();
            return null;
        }
        return body;
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void sendRateLimited(HttpExchange exchange) throws IOException {
        byte[] bytes = "{\"error\":\"Too many requests\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
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
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.36.1/min/vs/loader.min.js"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <script>
        // Internationalization (i18n) System - loads from mod lang files via API
        const i18n = {
            currentLang: 'en',
            translations: {},
            loaded: false,
            
            t(key) {
                return this.translations[key] || key;
            },
            
            async loadTranslations(lang) {
                try {
                    const langCode = lang === 'zh' ? 'zh_cn' : (lang === 'ja' ? 'ja_jp' : 'en_us');
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
                if (saved) {
                    this.currentLang = saved;
                    return saved;
                }
                const browserLang = navigator.language.split('-')[0];
                if (['en', 'zh', 'ja'].includes(browserLang)) {
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
                        sans: ['Inter', 'sans-serif'],
                        mono: ['JetBrains Mono', 'monospace'],
                    },
                    colors: {
                        bg: '#09090b',       // Zinc 950
                        surface: '#18181b',  // Zinc 900
                        surface2: '#27272a', // Zinc 800
                        border: '#3f3f46',   // Zinc 700
                        text: '#e4e4e7',     // Zinc 200
                        textMuted: '#a1a1aa',// Zinc 400
                        primary: '#2563eb',  // Blue 600
                        primaryHover: '#1d4ed8', // Blue 700
                        success: '#16a34a',  // Green 600
                    }
                }
            }
        }
    </script>
    <style>
        body { background-color: #09090b; color: #e4e4e7; font-family: 'Inter', sans-serif; }
        
        /* Clean Scrollbar */
        ::-webkit-scrollbar { width: 10px; height: 10px; }
        ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb { background: #3f3f46; border-radius: 5px; border: 2px solid #09090b; }
        ::-webkit-scrollbar-thumb:hover { background: #52525b; }

        .animate-fade-in { animation: fadeIn 0.2s ease-out; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
    </style>
</head>
<body class="h-screen flex flex-col overflow-hidden bg-bg selection:bg-primary/30">
    <!-- Top Navigation -->
    <nav class="h-14 border-b border-border bg-bg flex items-center justify-between px-6 z-20">
        <div class="flex items-center space-x-3">
            <div class="w-8 h-8 bg-primary rounded-md flex items-center justify-center shadow-sm">
                <span class="font-bold text-white font-mono">&lt;/&gt;</span>
            </div>
            <span class="font-semibold text-lg tracking-tight text-white">AlgoCraft</span>
            <select id="repo-select" onchange="changeRepo()" class="bg-surface border border-border rounded-md px-2 py-1 text-sm text-text focus:outline-none focus:border-primary ml-4">
                <option value="">Loading...</option>
            </select>
        </div>
        
        <!-- Search Bar -->
        <!-- <div class="relative mx-4 flex-1 max-w-md">
            <input type="text" id="search-input" placeholder="Search problems or tags..." class="w-full bg-surface border border-border rounded-md px-3 py-1.5 text-sm text-text focus:outline-none focus:border-primary transition-colors" oninput="handleSearch()">
            <div id="search-results" class="absolute top-full left-0 right-0 mt-1 bg-surface border border-border rounded-md shadow-lg hidden max-h-60 overflow-y-auto z-50"></div>
        </div> -->

        <div class="flex items-center space-x-4">
            <!-- Language Selector -->
            <select id="lang-select" onchange="i18n.setLanguage(this.value)" class="bg-surface border border-border rounded-md px-2 py-1 text-sm text-text focus:outline-none focus:border-primary">
                <option value="en">🇬🇧 EN</option>
                <option value="zh">🇨🇳 中文</option>
                <option value="ja">🇯🇵 日本語</option>
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

    <div class="flex-1 flex overflow-hidden">
        <!-- Sidebar: Problem List -->
        <div class="w-64 flex flex-col border-r border-border bg-surface">
            <div class="p-4 border-b border-border">
                <h2 class="text-sm font-semibold text-textMuted uppercase tracking-wider mb-2" data-i18n="sidebar.problems">Problems</h2>
                <input type="text" id="sidebar-search" data-i18n-placeholder="sidebar.filter" placeholder="Filter..." class="w-full bg-bg border border-border rounded-md px-3 py-1.5 text-sm text-text focus:outline-none focus:border-primary transition-colors" oninput="filterSidebar()">
            </div>
            <div class="flex-1 overflow-y-auto custom-scrollbar" id="problem-list">
                <!-- Problem items injected here -->
            </div>
        </div>

        <!-- Middle Panel: Problem Description -->
        <div class="w-[35%] flex flex-col border-r border-border bg-bg">
            <div class="flex-1 overflow-y-auto p-8 custom-scrollbar">
                <div class="animate-fade-in">
                    <div class="flex items-center space-x-3 mb-2">
                        <h1 class="text-2xl font-semibold text-white" id="problem-title">Loading...</h1>
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-surface2 text-text border border-border" id="problem-difficulty">Easy</span>
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-900/30 text-green-400 border border-green-900/50 hidden" id="problem-passed">✔ Passed</span>
                    </div>
                    
                    <div class="flex flex-wrap gap-2 mb-6" id="problem-tags"></div>
                    
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
            <div class="h-1/3 border-t border-border bg-surface flex flex-col">
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
        <div class="flex-1 flex flex-col bg-[#1e1e1e] relative">
            <!-- Editor Toolbar -->
            <div class="h-10 bg-[#1e1e1e] border-b border-[#2b2b2b] flex items-center px-4 space-x-4">
                <div class="flex items-center space-x-2 text-xs text-textMuted">
                    <span class="w-2 h-2 rounded-full bg-primary"></span>
                    <span>Java</span>
                </div>
            </div>
            
            <div id="editor-container" class="flex-1"></div>
            
            <!-- Action Bar -->
            <div class="h-16 border-t border-border bg-surface flex items-center justify-between px-6">
                <div class="text-sm text-textMuted flex items-center space-x-2">
                    <div class="w-2 h-2 rounded-full bg-green-500" id="status-dot"></div>
                    <span id="status-text" data-i18n="editor.ready">Ready</span>
                </div>
                <div class="flex items-center space-x-3">
                    <button onclick="runCode()" class="px-5 py-2 rounded-md bg-surface2 border border-border text-text hover:bg-border hover:text-white transition-all font-medium text-sm flex items-center space-x-2">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span data-i18n="editor.run">Run</span>
                    </button>
                    <button onclick="submitCode()" class="px-6 py-2 rounded-md bg-success text-white font-medium text-sm hover:bg-green-700 transition-all flex items-center space-x-2 shadow-sm">
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

        require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.36.1/min/vs' }});
        require(['vs/editor/editor.main'], async function() {
            // Initialize i18n first
            await i18n.init();
            document.getElementById('lang-select').value = i18n.currentLang;
            
            editor = monaco.editor.create(document.getElementById('editor-container'), {
                value: '// Loading...',
                language: 'java',
                theme: 'vs-dark',
                automaticLayout: true,
                minimap: { enabled: false },
                fontSize: 15,
                fontFamily: 'JetBrains Mono',
                lineHeight: 24,
                padding: { top: 20 },
                scrollBeyondLastLine: false,
                smoothScrolling: true,
                cursorBlinking: 'smooth',
                cursorSmoothCaretAnimation: true
            });
            fetchRepos();
        });

        async function fetchRepos() {
            try {
                const res = await fetch('/api/repositories');
                const repos = await res.json();
                const select = document.getElementById('repo-select');
                select.innerHTML = '';
                repos.forEach(repo => {
                    const option = document.createElement('option');
                    option.value = repo.name;
                    option.innerText = repo.name;
                    select.appendChild(option);
                });
                if (repos.length > 0) {
                    fetchProblems(repos[0].name);
                }
            } catch (e) {
                console.error("Failed to fetch repos", e);
            }
        }

        async function fetchProblems(repoName) {
            try {
                let url = '/api/problems?lang=' + encodeURIComponent(i18n.currentLang);
                if (repoName) {
                    url += '&repo=' + encodeURIComponent(repoName);
                }
                const res = await fetch(url);
                problems = await res.json();
                renderSidebar();
                if (problems.length > 0) {
                    loadProblem(0);
                } else {
                    document.getElementById('problem-title').innerText = i18n.t('problems.empty') || 'No problems found';
                    document.getElementById('problem-desc').innerText = '';
                    editor.setValue('');
                    document.getElementById('problem-counter').innerText = '0 / 0';
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
                    passed.innerText = '✔';
                    meta.appendChild(passed);
                }
                
                div.appendChild(title);
                div.appendChild(meta);
                list.appendChild(div);
            });
        }
        
        function getDiffColor(diff) {
            if (!diff) return 'text-yellow-500';
            const d = diff.toLowerCase();
            if (d === 'easy') return 'text-green-500';
            if (d === 'hard') return 'text-red-500';
            return 'text-yellow-500';
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

            document.getElementById('problem-desc').innerHTML = currentProblem.description.replace(/\\n/g, '<br>');
            document.getElementById('problem-counter').innerText = `${currentIndex + 1} / ${problems.length}`;
            
            editor.setValue(currentProblem.initialCode || '');
            clearConsole();
        }

        function handleSearch() {
            const query = document.getElementById('search-input').value.toLowerCase();
            const resultsContainer = document.getElementById('search-results');
            
            if (!query) {
                resultsContainer.classList.add('hidden');
                return;
            }
            
            const matches = problems.filter(p => 
                p.title.toLowerCase().includes(query) || 
                (p.tags && p.tags.some(t => t.toLowerCase().includes(query)))
            );
            
            resultsContainer.innerHTML = '';
            if (matches.length > 0) {
                matches.forEach(p => {
                    const div = document.createElement('div');
                    div.className = 'px-3 py-2 hover:bg-surface2 cursor-pointer text-sm text-text border-b border-border last:border-0';
                    div.innerText = p.title;
                    div.onclick = () => {
                        const idx = problems.indexOf(p);
                        loadProblem(idx);
                        resultsContainer.classList.add('hidden');
                        document.getElementById('search-input').value = '';
                    };
                    resultsContainer.appendChild(div);
                });
                resultsContainer.classList.remove('hidden');
            } else {
                resultsContainer.innerHTML = '<div class="px-3 py-2 text-sm text-textMuted">' + (i18n.t('search.no_results') || 'No results found') + '</div>';
                resultsContainer.classList.remove('hidden');
            }
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

        async function runCode() {
            const code = editor.getValue();
            document.getElementById('status-text').innerText = i18n.t('editor.running') || 'Running...';
            document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-yellow-500 animate-pulse';
            clearConsole();
            appendOutput(i18n.t('console.compiling') || 'Compiling and running...', 'info');
            
            try {
                const res = await fetch('/api/run', {
                    method: 'POST',
                    body: JSON.stringify({ code, problemId: currentProblem.id })
                });
                const data = await res.json();
                appendOutput(data.output);
                document.getElementById('status-text').innerText = i18n.t('editor.ready') || 'Ready';
                document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-green-500';
            } catch (e) {
                appendOutput(i18n.t('console.network_error') || 'Network Error', 'error');
                document.getElementById('status-text').innerText = i18n.t('editor.error') || 'Error';
                document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-red-500';
            }
        }

        async function submitCode() {
            const code = editor.getValue();
            document.getElementById('status-text').innerText = i18n.t('editor.judging') || 'Judging...';
            document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-yellow-500 animate-pulse';
            clearConsole();
            appendOutput(i18n.t('console.submitting') || 'Submitting solution...', 'info');
            
            try {
                const res = await fetch('/api/submit', {
                    method: 'POST',
                    body: JSON.stringify({ code, problemId: currentProblem.id })
                });
                const result = await res.json();
                
                if (result.isSuccess) {
                    document.getElementById('result-time').innerText = (result.executionTimeMs || 0) + ' ms';
                    document.getElementById('result-passed').innerText = `${result.passedCount}/${result.totalCount}`;
                    showSuccess();
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
                appendOutput(i18n.t('console.network_error') || 'Network Error', 'error');
                document.getElementById('status-text').innerText = i18n.t('editor.error') || 'Error';
                document.getElementById('status-dot').className = 'w-2 h-2 rounded-full bg-red-500';
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
            
            list.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-textMuted">' + (i18n.t('history.loading') || 'Loading...') + '</td></tr>';
            
            modal.classList.remove('hidden');
            // Trigger reflow
            void modal.offsetWidth;
            modal.classList.remove('opacity-0');
            content.classList.remove('scale-95');
            content.classList.add('scale-100');

            try {
                const res = await fetch('/api/history');
                const history = await res.json();
                
                list.innerHTML = '';
                if (history.length === 0) {
                    list.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-textMuted">' + (i18n.t('history.empty') || 'No submissions yet') + '</td></tr>';
                    return;
                }

                history.forEach(record => {
                    const date = new Date(record.timestamp).toLocaleString();
                    const statusColor = record.status === 'Accepted' ? 'text-green-500' : 'text-red-500';
                    
                    const tr = document.createElement('tr');
                    tr.className = 'border-b border-border/50 hover:bg-surface2 transition-colors';
                    tr.innerHTML = `
                        <td class="py-3 text-textMuted">${date}</td>
                        <td class="py-3 font-medium text-white">${record.problemTitle}</td>
                        <td class="py-3 ${statusColor}">${record.status}</td>
                        <td class="py-3 text-textMuted">${record.executionTime} ms</td>
                    `;
                    list.appendChild(tr);
                });
            } catch (e) {
                list.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-red-500">' + (i18n.t('history.load_error') || 'Failed to load history') + '</td></tr>';
            }
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
