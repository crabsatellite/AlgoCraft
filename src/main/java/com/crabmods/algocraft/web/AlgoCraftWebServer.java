package com.crabmods.algocraft.web;

import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.network.PacketSolveProblem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class AlgoCraftWebServer {
    private static HttpServer server;
    private static final int PORT = 3000;
    private static final Gson gson = new Gson();

    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new StaticHandler());
            server.createContext("/api/problems", new ProblemsHandler());
            server.createContext("/api/run", new RunHandler());
            server.createContext("/api/submit", new SubmitHandler());
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

    static class ProblemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                List<Problem> problems = ProblemManager.getProblems();
                String json = gson.toJson(problems);
                sendResponse(exchange, json);
            }
        }
    }

    static class RunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                JsonObject body = gson.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
                String code = body.get("code").getAsString();
                String problemId = body.get("problemId").getAsString();
                
                Problem problem = ProblemManager.getProblems().stream().filter(p -> p.id.equals(problemId)).findFirst().orElse(null);
                if (problem == null) {
                    sendResponse(exchange, "{\"error\": \"Problem not found\"}");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (Problem.TestCase test : problem.examples) {
                    String res = CodeExecutor.execute(code, test.input, test.output);
                    sb.append("Input: ").append(test.input).append("\n");
                    sb.append("Result: ").append(res).append("\n\n");
                }
                
                JsonObject response = new JsonObject();
                response.addProperty("output", sb.toString());
                sendResponse(exchange, gson.toJson(response));
            }
        }
    }

    static class SubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                JsonObject body = gson.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
                String code = body.get("code").getAsString();
                String problemId = body.get("problemId").getAsString();

                Problem problem = ProblemManager.getProblems().stream().filter(p -> p.id.equals(problemId)).findFirst().orElse(null);
                if (problem == null) {
                    sendResponse(exchange, "{\"error\": \"Problem not found\"}");
                    return;
                }

                SubmissionResult result = Judge.grade(problem, code);
                
                // If success, we need to notify the server game thread
                if (result.isSuccess) {
                    Minecraft.getInstance().execute(() -> {
                        PacketDistributor.sendToServer(new PacketSolveProblem(problem.id, problem.difficulty));
                    });
                }

                sendResponse(exchange, gson.toJson(result));
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
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
                        dark: {
                            bg: '#0f1117',
                            surface: '#1e293b',
                            border: '#334155',
                            accent: '#3b82f6'
                        }
                    }
                }
            }
        }
    </script>
    <style>
        body { background-color: #0f1117; color: #e2e8f0; font-family: 'Inter', sans-serif; }
        .glass-panel {
            background: rgba(30, 41, 59, 0.7);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.05);
        }
        .monaco-editor { padding-top: 12px; }
        
        /* Custom Scrollbar */
        ::-webkit-scrollbar { width: 8px; height: 8px; }
        ::-webkit-scrollbar-track { background: #0f1117; }
        ::-webkit-scrollbar-thumb { background: #334155; border-radius: 4px; }
        ::-webkit-scrollbar-thumb:hover { background: #475569; }

        .animate-fade-in { animation: fadeIn 0.3s ease-out; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        
        .btn-glow:hover {
            box-shadow: 0 0 15px rgba(59, 130, 246, 0.5);
        }
    </style>
</head>
<body class="h-screen flex flex-col overflow-hidden bg-dark-bg selection:bg-blue-500/30">
    <!-- Top Navigation -->
    <nav class="h-14 border-b border-dark-border glass-panel flex items-center justify-between px-6 z-20">
        <div class="flex items-center space-x-3">
            <div class="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center shadow-lg">
                <span class="font-bold text-white font-mono">&lt;/&gt;</span>
            </div>
            <span class="font-bold text-lg tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-400">AlgoCraft</span>
        </div>
        <div class="flex items-center space-x-4">
            <div class="flex bg-dark-surface rounded-lg p-1 border border-dark-border">
                <button onclick="prevProblem()" class="p-1.5 hover:bg-white/10 rounded-md transition-colors text-gray-400 hover:text-white">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path></svg>
                </button>
                <span class="px-3 py-1 text-sm font-medium text-gray-300 flex items-center" id="problem-counter">1 / 1</span>
                <button onclick="nextProblem()" class="p-1.5 hover:bg-white/10 rounded-md transition-colors text-gray-400 hover:text-white">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
                </button>
            </div>
        </div>
    </nav>

    <div class="flex-1 flex overflow-hidden">
        <!-- Left Panel: Problem Description -->
        <div class="w-[40%] flex flex-col border-r border-dark-border bg-[#13161c]">
            <div class="flex-1 overflow-y-auto p-8 custom-scrollbar">
                <div class="animate-fade-in">
                    <div class="flex items-center space-x-3 mb-6">
                        <h1 class="text-2xl font-bold text-white" id="problem-title">Loading...</h1>
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-500/10 text-green-400 border border-green-500/20" id="problem-difficulty">Easy</span>
                    </div>
                    
                    <div class="prose prose-invert prose-sm max-w-none text-gray-300 leading-relaxed" id="problem-desc">
                        <!-- Description injected here -->
                    </div>

                    <!-- Examples Section (Dynamic) -->
                    <div class="mt-8 space-y-4" id="examples-container">
                        <!-- Examples injected here -->
                    </div>
                </div>
            </div>
            
            <!-- Console / Output Panel -->
            <div class="h-1/3 border-t border-dark-border bg-[#0f1117] flex flex-col">
                <div class="flex items-center justify-between px-4 py-2 border-b border-dark-border bg-[#161b22]">
                    <span class="text-xs font-bold text-gray-400 uppercase tracking-wider">Console</span>
                    <button onclick="clearConsole()" class="text-xs text-gray-500 hover:text-white transition-colors">Clear</button>
                </div>
                <div class="flex-1 p-4 overflow-y-auto font-mono text-sm" id="output-container">
                    <div class="text-gray-500 italic">Ready to run...</div>
                </div>
            </div>
        </div>

        <!-- Right Panel: Code Editor -->
        <div class="flex-1 flex flex-col bg-[#1e1e1e] relative">
            <!-- Editor Toolbar -->
            <div class="h-10 bg-[#1e1e1e] border-b border-[#2b2b2b] flex items-center px-4 space-x-4">
                <div class="flex items-center space-x-2 text-xs text-gray-400">
                    <span class="w-2 h-2 rounded-full bg-yellow-500"></span>
                    <span>Java</span>
                </div>
            </div>
            
            <div id="editor-container" class="flex-1"></div>
            
            <!-- Action Bar -->
            <div class="h-16 border-t border-dark-border glass-panel absolute bottom-6 right-6 left-6 rounded-xl flex items-center justify-between px-6 shadow-2xl z-10">
                <div class="text-sm text-gray-400 flex items-center space-x-2">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                    <span id="status-text">Ready</span>
                </div>
                <div class="flex items-center space-x-3">
                    <button onclick="runCode()" class="px-5 py-2 rounded-lg bg-dark-surface border border-dark-border text-gray-300 hover:bg-white/5 hover:text-white transition-all font-medium text-sm flex items-center space-x-2">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span>Run</span>
                    </button>
                    <button onclick="submitCode()" class="px-6 py-2 rounded-lg bg-gradient-to-r from-green-600 to-emerald-600 text-white font-medium text-sm shadow-lg shadow-green-900/20 hover:shadow-green-600/30 hover:-translate-y-0.5 transition-all flex items-center space-x-2 btn-glow">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        <span>Submit</span>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Success Overlay -->
    <div id="success-modal" class="fixed inset-0 bg-black/80 backdrop-blur-sm hidden flex items-center justify-center z-50 transition-opacity duration-300 opacity-0">
        <div class="bg-[#1e293b] border border-white/10 p-10 rounded-2xl shadow-2xl transform scale-90 transition-all duration-300 max-w-md w-full text-center relative overflow-hidden" id="success-content">
            <!-- Background Glow -->
            <div class="absolute top-0 left-1/2 -translate-x-1/2 w-32 h-32 bg-green-500/20 blur-3xl rounded-full pointer-events-none"></div>
            
            <div class="relative z-10">
                <div class="w-20 h-20 bg-green-500/10 rounded-full flex items-center justify-center mx-auto mb-6 ring-1 ring-green-500/30">
                    <svg class="w-10 h-10 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                </div>
                <h2 class="text-3xl font-bold text-white mb-2 tracking-tight">Accepted!</h2>
                <p class="text-gray-400 mb-8">You have successfully solved the problem. Rewards have been sent to your inventory.</p>
                
                <div class="grid grid-cols-2 gap-4 mb-8">
                    <div class="bg-black/20 p-3 rounded-lg">
                        <div class="text-xs text-gray-500 uppercase">Runtime</div>
                        <div class="text-green-400 font-mono font-bold" id="result-time">0 ms</div>
                    </div>
                    <div class="bg-black/20 p-3 rounded-lg">
                        <div class="text-xs text-gray-500 uppercase">Passed</div>
                        <div class="text-green-400 font-mono font-bold" id="result-passed">All</div>
                    </div>
                </div>

                <button onclick="closeModal()" class="w-full py-3 bg-white text-black font-bold rounded-lg hover:bg-gray-200 transition-colors">
                    Continue Coding
                </button>
            </div>
        </div>
    </div>

    <script>
        let editor;
        let currentProblem = null;
        let problems = [];
        let currentIndex = 0;

        require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.36.1/min/vs' }});
        require(['vs/editor/editor.main'], function() {
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
            fetchProblems();
        });

        async function fetchProblems() {
            try {
                const res = await fetch('/api/problems');
                problems = await res.json();
                if (problems.length > 0) {
                    loadProblem(0);
                }
            } catch (e) {
                console.error("Failed to fetch problems", e);
            }
        }

        function loadProblem(index) {
            if (index < 0 || index >= problems.length) return;
            currentIndex = index;
            currentProblem = problems[index];
            
            document.getElementById('problem-title').innerText = currentProblem.title;
            document.getElementById('problem-difficulty').innerText = currentProblem.difficulty || 'Medium';
            document.getElementById('problem-desc').innerHTML = currentProblem.description.replace(/\\n/g, '<br>');
            document.getElementById('problem-counter').innerText = `${currentIndex + 1} / ${problems.length}`;
            
            editor.setValue(currentProblem.initialCode || '');
            clearConsole();
        }

        function prevProblem() { loadProblem((currentIndex - 1 + problems.length) % problems.length); }
        function nextProblem() { loadProblem((currentIndex + 1) % problems.length); }
        function clearConsole() { document.getElementById('output-container').innerHTML = '<div class="text-gray-500 italic">Ready to run...</div>'; }

        function appendOutput(text, type = 'info') {
            const container = document.getElementById('output-container');
            if (container.children.length === 1 && container.children[0].classList.contains('italic')) {
                container.innerHTML = '';
            }
            
            const div = document.createElement('div');
            div.className = 'mb-2 font-mono whitespace-pre-wrap ' + (type === 'error' ? 'text-red-400' : 'text-gray-300');
            div.innerText = text;
            container.appendChild(div);
            container.scrollTop = container.scrollHeight;
        }

        async function runCode() {
            const code = editor.getValue();
            document.getElementById('status-text').innerText = 'Running...';
            clearConsole();
            appendOutput('Compiling and running...', 'info');
            
            try {
                const res = await fetch('/api/run', {
                    method: 'POST',
                    body: JSON.stringify({ code, problemId: currentProblem.id })
                });
                const data = await res.json();
                appendOutput(data.output);
                document.getElementById('status-text').innerText = 'Ready';
            } catch (e) {
                appendOutput('Network Error', 'error');
            }
        }

        async function submitCode() {
            const code = editor.getValue();
            document.getElementById('status-text').innerText = 'Judging...';
            clearConsole();
            appendOutput('Submitting solution...', 'info');
            
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
                    appendOutput('Accepted!', 'info');
                } else {
                    let msg = `Status: ${result.message}\\n`;
                    if (result.details) {
                        const fail = result.details.find(d => !d.passed);
                        if (fail) {
                            msg += `Failed on input: ${fail.input}\\nExpected: ${fail.expected}\\nGot: ${fail.actual}`;
                            if (fail.error) msg += `\\nError: ${fail.error}`;
                        }
                    }
                    appendOutput(msg, 'error');
                }
                document.getElementById('status-text').innerText = result.message;
            } catch (e) {
                appendOutput('Network Error', 'error');
            }
        }

        function showSuccess() {
            const modal = document.getElementById('success-modal');
            const content = document.getElementById('success-content');
            modal.classList.remove('hidden');
            // Trigger reflow
            void modal.offsetWidth;
            modal.classList.remove('opacity-0');
            content.classList.remove('scale-90');
            content.classList.add('scale-100');
        }

        function closeModal() {
            const modal = document.getElementById('success-modal');
            const content = document.getElementById('success-content');
            modal.classList.add('opacity-0');
            content.classList.remove('scale-100');
            content.classList.add('scale-90');
            setTimeout(() => modal.classList.add('hidden'), 300);
        }
    </script>
</body>
</html>
        """;
    }

}
