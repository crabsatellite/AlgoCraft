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
    private static final int PROBLEM_START = Integer.getInteger("algocraft.officialProblemStart", 1);
    private static final int PROBLEM_END = Integer.getInteger("algocraft.officialProblemEnd", 500);

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
        return problemProvider(questionBank, PROBLEM_START, PROBLEM_END);
    }

    static Stream<Arguments> problemProvider(Path questionBank, int problemStart, int problemEnd) {
        if (problemStart > problemEnd) {
            throw new AssertionError("Invalid official question bank range: p" + problemStart + "-p" + problemEnd);
        }
        if (!Files.isDirectory(questionBank)) {
            throw new AssertionError("Official question bank directory is missing: " + questionBank);
        }

        List<Path> problemFiles;
        try {
            try (Stream<Path> files = Files.list(questionBank)) {
                problemFiles = files
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> p.getFileName().toString().matches("p\\d+\\.json"))
                    .filter(p -> isProblemInConfiguredRange(extractNumber(p.getFileName().toString()),
                            problemStart, problemEnd))
                    .sorted(Comparator.comparingInt(p -> extractNumber(p.getFileName().toString())))
                    .toList();
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to list official question bank directory: " + questionBank, e);
        }
        if (problemFiles.isEmpty()) {
            throw new AssertionError("No official problem files found in configured range p"
                    + problemStart + "-p" + problemEnd + " under " + questionBank);
        }

        List<Arguments> arguments = new ArrayList<>();
        for (Path problemFile : problemFiles) {
            arguments.addAll(loadProblemTests(problemFile));
        }
        if (arguments.isEmpty()) {
            throw new AssertionError("Official question bank range p" + problemStart + "-p" + problemEnd
                    + " produced no executable solution tests");
        }
        return arguments.stream();
    }

    private static int extractNumber(String filename) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("p(\\d+)\\.json").matcher(filename);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static boolean isProblemInConfiguredRange(int problemNumber, int problemStart, int problemEnd) {
        return problemNumber >= problemStart && problemNumber <= problemEnd;
    }

    /**
     * Load a problem JSON and generate one Argument per (solution, testCase) pair.
     */
    private static List<Arguments> loadProblemTests(Path file) {
        try {
            String content = Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            String id = json.has("id") ? json.get("id").getAsString() : file.getFileName().toString();
            String title = json.has("title") ? json.get("title").getAsString() : "";
            String initialCode = json.has("initialCode") ? json.get("initialCode").getAsString() : "";

            if (!json.has("solutions")) {
                throw new AssertionError(file.getFileName() + " is missing solutions");
            }
            if (!json.has("tests")) {
                throw new AssertionError(file.getFileName() + " is missing tests");
            }

            JsonArray solutionsArr = json.getAsJsonArray("solutions");
            JsonArray testsArr = json.getAsJsonArray("tests");

            List<SolutionData> solutions = new ArrayList<>();
            for (int i = 0; i < solutionsArr.size(); i++) {
                JsonElement sol = solutionsArr.get(i);
                JsonObject s = sol.getAsJsonObject();
                String name = s.has("name") ? s.get("name").getAsString() : "Solution";
                String code = s.has("code") ? s.get("code").getAsString() : "";
                if (code.isBlank()) {
                    throw new AssertionError(file.getFileName() + " solution[" + i + "] has blank code");
                }
                solutions.add(new SolutionData(name, code));
            }

            List<TestCaseData> tests = new ArrayList<>();
            for (int i = 0; i < testsArr.size(); i++) {
                JsonElement test = testsArr.get(i);
                JsonObject t = test.getAsJsonObject();
                String input = t.has("input") ? t.get("input").getAsString() : "";
                String output = t.has("output") ? t.get("output").getAsString() : "";
                if (input.isBlank()) {
                    throw new AssertionError(file.getFileName() + " tests[" + i + "] has blank input");
                }
                if (output.isBlank()) {
                    throw new AssertionError(file.getFileName() + " tests[" + i + "] has blank output");
                }
                tests.add(new TestCaseData(input, output));
            }

            if (solutions.isEmpty()) {
                throw new AssertionError(file.getFileName() + " has no executable solutions");
            }
            if (tests.isEmpty()) {
                throw new AssertionError(file.getFileName() + " has no executable tests");
            }

            ProblemType type = detectType(initialCode);
            String className = extractClassName(initialCode);

            // Generate one argument per solution. Recompiling for every test case
            // makes the 500-problem bank slow enough to hide real hangs.
            List<Arguments> args = new ArrayList<>();
            for (int si = 0; si < solutions.size(); si++) {
                SolutionData sol = solutions.get(si);
                String displayName = String.format("P%s [%s] %s",
                        id, type.name().substring(0, 3), sol.name());
                args.add(Arguments.of(displayName, id, type.name(), className, sol.code(), List.copyOf(tests)));
            }
            return args;
        } catch (Exception e) {
            throw new AssertionError("Failed to load official solution tests from " + file.getFileName()
                    + ": " + e.getMessage(), e);
        }
    }

    // ─── The parameterized test ─────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("problemProvider")
    void testSolution(String displayName, String problemId, String problemType, String className,
                      String solutionCode, List<TestCaseData> tests) {
        ProblemType type = ProblemType.valueOf(problemType);

        try (CompilerService compiler = new CompilerService()) {
            Class<?> compiledClass = compiler.compile(className, solutionCode);

            for (int i = 0; i < tests.size(); i++) {
                TestCaseData test = tests.get(i);
                TestResult result;
                if (type == ProblemType.DESIGN_CLASS) {
                    result = DesignClassAdapter.run(compiledClass, test.input(), test.output());
                } else if (type == ProblemType.CODEC) {
                    result = CodecAdapter.run(compiledClass, test.input(), test.output());
                } else {
                    result = StandardAdapter.run(compiledClass, test.input(), test.output(), problemId);
                }

                if (!result.passed) {
                    String testLabel = displayName + " | Test " + (i + 1);
                    if (result.error != null) {
                        fail(testLabel + System.lineSeparator() + result.error);
                    } else {
                        fail(String.format("%s%nExpected: %s%nActual: %s%nInput: %s",
                                testLabel, result.expected, result.actual, result.input));
                    }
                }
            }
        } catch (Exception e) {
            fail(displayName + System.lineSeparator() + "Compilation/execution error: " + e.getMessage());
        }
    }
}
