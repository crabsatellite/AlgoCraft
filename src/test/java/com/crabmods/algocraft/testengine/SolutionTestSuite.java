package com.crabmods.algocraft.testengine;

import com.crabmods.algocraft.testengine.adapters.*;
import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 parameterized test suite for all 500 AlgoCraft problems.
 * <p>
 * Each problem file is a separate test, and each solution × test case is a dynamic test.
 * Automatically detects problem type and dispatches to the appropriate adapter.
 */
@Execution(ExecutionMode.CONCURRENT)
class SolutionTestSuite {

    private static final String QUESTION_BANK_PATH = "question_bank/official";
    private static final Gson GSON = new Gson();

    // ─── Problem type detection ─────────────────────────────────────

    enum ProblemType {
        STANDARD, DESIGN_CLASS, CODEC, UNSUPPORTED
    }

    static ProblemType detectType(String initialCode) {
        if (initialCode == null) return ProblemType.UNSUPPORTED;

        // Strip comments before detecting class names
        String stripped = initialCode
                .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                .replaceAll("//[^\n]*", "");

        // Codec: serialize/deserialize or encode/decode pattern
        if (stripped.contains("class Codec")) {
            return ProblemType.CODEC;
        }
        // Also detect encode/decode or serialize/deserialize method pairs
        // Use method declaration pattern to avoid false positives (e.g., "decode" in "Decode XORed Array")
        if (hasMethodPair(stripped, "encode", "decode") || hasMethodPair(stripped, "serialize", "deserialize")) {
            return ProblemType.CODEC;
        }

        // Design class: class name is NOT "Solution"
        if (stripped.contains("class ") && !stripped.contains("class Solution")) {
            return ProblemType.DESIGN_CLASS;
        }
        return ProblemType.STANDARD;
    }

    /** Check if the code declares two methods forming a codec pair (e.g., encode/decode). */
    private static boolean hasMethodPair(String code, String method1, String method2) {
        // Look for method declaration patterns: "ReturnType methodName("
        String pattern1 = "\\b" + method1 + "\\s*\\(";
        String pattern2 = "\\b" + method2 + "\\s*\\(";
        return code.matches("(?s).*" + pattern1 + ".*") && code.matches("(?s).*" + pattern2 + ".*");
    }

    /**
     * Extract the class name from initialCode, ignoring comments.
     */
    static String extractClassName(String initialCode) {
        // Strip block comments (/* ... */) and line comments (// ...)
        String stripped = initialCode
                .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                .replaceAll("//[^\n]*", "");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("class\\s+(\\w+)").matcher(stripped);
        if (m.find()) return m.group(1);
        return "Solution";
    }

    // ─── Data model ─────────────────────────────────────────────────

    record ProblemData(String id, String title, String initialCode,
                       List<SolutionData> solutions, List<TestCaseData> tests) {}

    record SolutionData(String name, String code) {}

    record TestCaseData(String input, String output) {}

    // ─── Test argument provider ─────────────────────────────────────

    static Stream<Arguments> problemProvider() {
        Path questionBank = Paths.get(System.getProperty("user.dir"), QUESTION_BANK_PATH);
        if (!Files.isDirectory(questionBank)) {
            return Stream.empty();
        }

        try {
            return Files.list(questionBank)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> p.getFileName().toString().matches("p\\d+\\.json"))
                    .sorted(Comparator.comparingInt(p -> extractNumber(p.getFileName().toString())))
                    .flatMap(SolutionTestSuite::loadProblemTests);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static int extractNumber(String filename) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("p(\\d+)\\.json").matcher(filename);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /**
     * Load a problem JSON and generate one Argument per (solution, testCase) pair.
     */
    private static Stream<Arguments> loadProblemTests(Path file) {
        try {
            String content = Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            String id = json.has("id") ? json.get("id").getAsString() : file.getFileName().toString();
            String title = json.has("title") ? json.get("title").getAsString() : "";
            String initialCode = json.has("initialCode") ? json.get("initialCode").getAsString() : "";

            if (!json.has("solutions") || !json.has("tests")) return Stream.empty();

            JsonArray solutionsArr = json.getAsJsonArray("solutions");
            JsonArray testsArr = json.getAsJsonArray("tests");

            List<SolutionData> solutions = new ArrayList<>();
            for (JsonElement sol : solutionsArr) {
                JsonObject s = sol.getAsJsonObject();
                String name = s.has("name") ? s.get("name").getAsString() : "Solution";
                String code = s.has("code") ? s.get("code").getAsString() : "";
                if (!code.isEmpty()) solutions.add(new SolutionData(name, code));
            }

            List<TestCaseData> tests = new ArrayList<>();
            for (JsonElement test : testsArr) {
                JsonObject t = test.getAsJsonObject();
                String input = t.has("input") ? t.get("input").getAsString() : "";
                String output = t.has("output") ? t.get("output").getAsString() : "";
                if (!input.isEmpty() && !output.isEmpty()) tests.add(new TestCaseData(input, output));
            }

            if (solutions.isEmpty() || tests.isEmpty()) return Stream.empty();

            ProblemType type = detectType(initialCode);
            String className = extractClassName(initialCode);

            // Generate one argument per (solution, test) pair
            List<Arguments> args = new ArrayList<>();
            for (int si = 0; si < solutions.size(); si++) {
                SolutionData sol = solutions.get(si);
                for (int ti = 0; ti < tests.size(); ti++) {
                    TestCaseData tc = tests.get(ti);
                    String displayName = String.format("P%s [%s] %s | Test %d",
                            id, type.name().substring(0, 3), sol.name(), ti + 1);
                    args.add(Arguments.of(displayName, type.name(), className, sol.code(), tc.input(), tc.output()));
                }
            }
            return args.stream();
        } catch (Exception e) {
            System.err.println("Error loading " + file.getFileName() + ": " + e.getMessage());
            return Stream.empty();
        }
    }

    // ─── The parameterized test ─────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("problemProvider")
    void testSolution(String displayName, String problemType, String className,
                      String solutionCode, String input, String expected) {
        ProblemType type = ProblemType.valueOf(problemType);

        try (CompilerService compiler = new CompilerService()) {
            Class<?> compiledClass = compiler.compile(className, solutionCode);

            TestResult result;
            if (type == ProblemType.DESIGN_CLASS) {
                result = DesignClassAdapter.run(compiledClass, input, expected);
            } else if (type == ProblemType.CODEC) {
                result = CodecAdapter.run(compiledClass, input, expected);
            } else {
                result = StandardAdapter.run(compiledClass, input, expected);
            }

            if (!result.passed) {
                if (result.error != null) {
                    fail(result.error);
                } else {
                    fail(String.format("Expected: %s%nActual: %s%nInput: %s",
                            result.expected, result.actual, result.input));
                }
            }
        } catch (Exception e) {
            fail("Compilation/execution error: " + e.getMessage());
        }
    }
}
