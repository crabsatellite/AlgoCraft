package com.crabmods.algocraft.logic;

import com.google.gson.Gson;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Isolated JVM entry point for executing user-submitted code.
 */
public final class CodeExecutorWorker {
    private static final Gson GSON = new Gson();

    private CodeExecutorWorker() {
    }

    public static void main(String[] args) {
        CodeExecutor.WorkerResponse response;
        try {
            String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            CodeExecutor.WorkerRequest request = GSON.fromJson(input, CodeExecutor.WorkerRequest.class);
            response = handle(request);
        } catch (Throwable throwable) {
            response = CodeExecutor.WorkerResponse.error("ERROR: Worker failed: "
                    + throwable.getClass().getSimpleName() + ": " + safeMessage(throwable));
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            GSON.toJson(response, writer);
        } catch (Exception ignored) {
            System.exit(2);
        }
    }

    private static CodeExecutor.WorkerResponse handle(CodeExecutor.WorkerRequest request) {
        if (request == null) {
            return CodeExecutor.WorkerResponse.error("ERROR: Worker received an empty request");
        }
        if (request.timeoutMs > 0) {
            System.setProperty(CodeExecutor.TIMEOUT_PROPERTY, Integer.toString(request.timeoutMs));
        }
        if ("single".equals(request.mode)) {
            return CodeExecutor.WorkerResponse.single(
                    CodeExecutor.executeInProcess(request.code, request.input, request.expectedOutput));
        }
        if ("batch".equals(request.mode)) {
            List<CodeExecutor.TestCase> testCases = request.toTestCases();
            return CodeExecutor.WorkerResponse.batch(CodeExecutor.executeBatchInProcess(request.code, testCases));
        }
        return CodeExecutor.WorkerResponse.error("ERROR: Unknown worker mode: " + request.mode);
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return "";
        }
        return message.length() > 200 ? message.substring(0, 200) + "..." : message;
    }
}
