package com.crabmods.algocraft.solutions;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Intelligent Solution Test Runner v2.0
 * 
 * Features:
 * - Automatic problem type detection (standard, design, matrix, encode/decode)
 * - Multiple input format parsing (int[], String[], char[][], List<String>)
 * - Deferred error reporting (all errors shown at end)
 * - Dynamic compilation and testing
 */
public class SolutionTestRunner {

    private static final String QUESTION_BANK_PATH = "question_bank/official";
    
    // Statistics
    private static int totalPassed = 0;
    private static int totalFailed = 0;
    private static int totalSkipped = 0;
    
    // Deferred error reporting
    private static List<ErrorReport> allErrors = new ArrayList<>();
    private static List<String> skippedProblems = new ArrayList<>();

    // Problem type enumeration
    enum ProblemType {
        STANDARD,           // Regular function with input/output
        MATRIX_INPUT,       // 2D array input (like Sudoku)
        ENCODE_DECODE,      // Encode then decode problems
        DESIGN_CLASS,       // Class design problems (multiple methods)
        UNSUPPORTED         // Cannot auto-test
    }

    static class ErrorReport {
        String problemId;
        String problemTitle;
        String solutionName;
        int testNumber;
        String input;
        String expected;
        String actual;
        String errorMessage;

        ErrorReport(String problemId, String problemTitle, String solutionName, 
                   int testNumber, String input, String expected, String actual, String errorMessage) {
            this.problemId = problemId;
            this.problemTitle = problemTitle;
            this.solutionName = solutionName;
            this.testNumber = testNumber;
            this.input = input;
            this.expected = expected;
            this.actual = actual;
            this.errorMessage = errorMessage;
        }
    }

    public static void main(String[] args) throws Exception {
        String workspacePath = System.getProperty("user.dir");
        Path questionBankDir = Paths.get(workspacePath, QUESTION_BANK_PATH);
        
        printHeader();
        System.out.println("📂 Scanning: " + questionBankDir + "\n");

        // Get all problem files sorted by number
        List<Path> problemFiles = Files.list(questionBankDir)
            .filter(p -> p.toString().endsWith(".json"))
            .filter(p -> p.getFileName().toString().matches("p\\d+\\.json"))
            .sorted((a, b) -> {
                int numA = extractProblemNumber(a.getFileName().toString());
                int numB = extractProblemNumber(b.getFileName().toString());
                return Integer.compare(numA, numB);
            })
            .collect(java.util.stream.Collectors.toList());

        int problemsWithSolutions = 0;
        
        for (Path problemFile : problemFiles) {
            try {
                String content = Files.readString(problemFile);
                
                if (!content.contains("\"solutions\"")) {
                    continue;
                }
                
                problemsWithSolutions++;
                testProblem(problemFile, content);
                
            } catch (Exception e) {
                System.out.println("❌ Error processing " + problemFile.getFileName() + ": " + e.getMessage());
            }
        }

        // Print final summary with all errors at the end
        printFinalSummary(problemsWithSolutions);
    }

    private static void printHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║        AlgoCraft Intelligent Solution Test Runner v2.0          ║");
        System.out.println("║    ✦ Auto-detects problem types (standard/matrix/encode)        ║");
        System.out.println("║    ✦ Supports multiple input formats                            ║");
        System.out.println("║    ✦ Deferred error reporting (errors at end)                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");
    }

    private static void printFinalSummary(int problemsWithSolutions) {
        System.out.println("\n");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("                        FINAL SUMMARY                              ");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("Problems with solutions: " + problemsWithSolutions);
        System.out.println("Total tests passed: ✅ " + totalPassed);
        System.out.println("Total tests failed: ❌ " + totalFailed);
        System.out.println("Total tests skipped: ⏭️  " + totalSkipped);
        
        int total = totalPassed + totalFailed;
        if (total > 0) {
            double passRate = (totalPassed * 100.0) / total;
            System.out.printf("Pass rate: %.1f%%\n", passRate);
        }
        
        // Print skipped problems
        if (!skippedProblems.isEmpty()) {
            System.out.println("\n⏭️  Skipped problems (require special handling):");
            for (String p : skippedProblems) {
                System.out.println("   - " + p);
            }
        }
        
        // Print all errors at the end
        if (!allErrors.isEmpty()) {
            System.out.println("\n");
            System.out.println("════════════════════════════════════════════════════════════════════");
            System.out.println("                     DETAILED ERROR REPORT                          ");
            System.out.println("════════════════════════════════════════════════════════════════════");
            
            String currentProblem = "";
            for (ErrorReport err : allErrors) {
                if (!currentProblem.equals(err.problemId)) {
                    currentProblem = err.problemId;
                    System.out.println("\n🔴 Problem " + err.problemId + ": " + err.problemTitle);
                    System.out.println("────────────────────────────────────────────────────────────────────");
                }
                
                System.out.println("   Solution: " + err.solutionName + " | Test " + err.testNumber);
                if (err.errorMessage != null && !err.errorMessage.isEmpty()) {
                    System.out.println("   Error: " + truncate(err.errorMessage, 80));
                } else {
                    System.out.println("   Input: " + truncate(err.input, 70));
                    System.out.println("   Expected: " + truncate(err.expected, 70));
                    System.out.println("   Got: " + truncate(err.actual, 70));
                }
                System.out.println();
            }
        } else if (totalFailed == 0 && totalSkipped == 0 && problemsWithSolutions > 0) {
            System.out.println("\n🎉 All solutions passed all tests! Excellent work!");
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    private static int extractProblemNumber(String filename) {
        Matcher m = Pattern.compile("p(\\d+)\\.json").matcher(filename);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    // ==================== Problem Type Detection ====================

    private static ProblemType detectProblemType(String content, String code) {
        if (code == null) code = "";
        
        // Check for encode/decode pattern
        if (code.contains("encode") && code.contains("decode") && 
            code.contains("List<String>")) {
            return ProblemType.ENCODE_DECODE;
        }
        
        // Check method signature for char[][] parameter
        if (code.contains("char[][]") || code.contains("char [][]")) {
            return ProblemType.MATRIX_INPUT;
        }
        
        // Check method signature for int[][] parameter
        if (code.contains("int[][]") || code.contains("int [][]")) {
            return ProblemType.MATRIX_INPUT;
        }
        
        // Check for 2D matrix input in test cases
        String firstTest = extractFirstTestInput(content);
        if (firstTest != null) {
            // Check for board/matrix 2D array pattern
            if (firstTest.contains("[[\\\"") || firstTest.contains("[[\"") ||
                (firstTest.contains("board") && firstTest.contains("[["))) {
                return ProblemType.MATRIX_INPUT;
            }
            // Check for int[][] matrix pattern
            if (firstTest.contains("matrix") && firstTest.contains("[[")) {
                return ProblemType.MATRIX_INPUT;
            }
            // Check for design class pattern (operations array)
            if (firstTest.startsWith("[\"") && !firstTest.startsWith("[\"[")) {
                return ProblemType.DESIGN_CLASS;
            }
        }
        
        return ProblemType.STANDARD;
    }

    private static String extractFirstTestInput(String content) {
        int testsStart = content.indexOf("\"tests\"");
        if (testsStart == -1) return null;
        
        int inputStart = content.indexOf("\"input\"", testsStart);
        if (inputStart == -1) return null;
        
        int valueStart = content.indexOf(":", inputStart) + 1;
        
        // Find the next "output" key to delimit input value
        int outputStart = content.indexOf("\"output\"", valueStart);
        if (outputStart == -1) return null;
        
        String value = content.substring(valueStart, outputStart).trim();
        // Remove trailing comma
        if (value.endsWith(",")) value = value.substring(0, value.length() - 1);
        // Remove surrounding quotes if it's a quoted string
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    // ==================== Main Test Dispatcher ====================

    private static void testProblem(Path problemFile, String content) throws Exception {
        String id = extractJsonString(content, "id");
        String title = extractJsonString(content, "title");
        
        List<Solution> solutions = extractSolutions(content);
        if (solutions.isEmpty()) {
            return;
        }
        
        String solutionCode = solutions.get(0).code;
        ProblemType problemType = detectProblemType(content, solutionCode);
        
        String typeTag;
        switch (problemType) {
            case STANDARD: typeTag = "Standard"; break;
            case MATRIX_INPUT: typeTag = "Matrix Input"; break;
            case ENCODE_DECODE: typeTag = "Encode/Decode"; break;
            case DESIGN_CLASS: typeTag = "Design Class"; break;
            default: typeTag = "Unknown"; break;
        }
        
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📋 P" + id + ": " + title + " [" + typeTag + "]");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        switch (problemType) {
            case MATRIX_INPUT:
                testMatrixProblem(id, title, content, solutions);
                break;
            case ENCODE_DECODE:
                testEncodeDecodeProblem(id, title, content, solutions);
                break;
            case DESIGN_CLASS:
                System.out.println("   ⏭️  Design class problems require manual testing");
                skippedProblems.add("P" + id + ": " + title);
                countSkippedTests(content, solutions.size());
                break;
            case STANDARD:
            default:
                testStandardProblem(id, title, content, solutions);
                break;
        }
    }

    private static void countSkippedTests(String content, int numSolutions) {
        List<TestCase> testCases = extractTestCases(content);
        totalSkipped += testCases.size() * numSolutions;
    }

    // ==================== Standard Problem Testing ====================

    private static void testStandardProblem(String id, String title, String content, 
                                           List<Solution> solutions) throws Exception {
        List<TestCase> testCases = extractTestCases(content);
        if (testCases.isEmpty()) {
            System.out.println("   ⚠️  No test cases found");
            return;
        }

        for (int solIdx = 0; solIdx < solutions.size(); solIdx++) {
            Solution sol = solutions.get(solIdx);
            System.out.print("   📝 " + sol.name + ": ");
            
            try {
                String className = "Solution_P" + id + "_" + System.currentTimeMillis() + "_" + solIdx;
                String fullCode = wrapSolutionCode(className, sol.code);
                Class<?> solutionClass = compileClass(className, fullCode);
                
                if (solutionClass == null) {
                    System.out.println("❌ Compilation failed");
                    recordErrors(id, title, sol.name, testCases.size(), "Compilation failed");
                    continue;
                }
                
                Object instance = solutionClass.getDeclaredConstructor().newInstance();
                Method method = findMainMethod(solutionClass);
                
                if (method == null) {
                    System.out.println("❌ Method not found");
                    recordErrors(id, title, sol.name, testCases.size(), "Method not found");
                    continue;
                }
                
                int passed = 0;
                int failed = 0;
                
                for (int i = 0; i < testCases.size(); i++) {
                    TestCase tc = testCases.get(i);
                    try {
                        Object[] args = parseInputArgs(tc.input, method);
                        Object result = method.invoke(instance, args);
                        String resultStr = formatResult(result);
                        
                        if (compareResults(resultStr, tc.output, method.getReturnType())) {
                            passed++;
                            totalPassed++;
                        } else {
                            failed++;
                            totalFailed++;
                            allErrors.add(new ErrorReport(id, title, sol.name, i + 1,
                                tc.input, tc.output, resultStr, null));
                        }
                    } catch (Exception e) {
                        failed++;
                        totalFailed++;
                        String errMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        allErrors.add(new ErrorReport(id, title, sol.name, i + 1,
                            tc.input, tc.output, "", errMsg != null ? errMsg : "Unknown error"));
                    }
                }
                
                printTestResult(passed, failed);
                
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
                recordErrors(id, title, sol.name, testCases.size(), e.getMessage());
            }
        }
    }

    // ==================== Matrix Problem Testing ====================

    private static void testMatrixProblem(String id, String title, String content,
                                          List<Solution> solutions) throws Exception {
        List<TestCase> testCases = extractTestCases(content);
        if (testCases.isEmpty()) {
            System.out.println("   ⚠️  No test cases found");
            return;
        }

        for (int solIdx = 0; solIdx < solutions.size(); solIdx++) {
            Solution sol = solutions.get(solIdx);
            System.out.print("   📝 " + sol.name + ": ");
            
            try {
                String className = "Solution_P" + id + "_" + System.currentTimeMillis() + "_" + solIdx;
                String fullCode = wrapSolutionCode(className, sol.code);
                Class<?> solutionClass = compileClass(className, fullCode);
                
                if (solutionClass == null) {
                    System.out.println("❌ Compilation failed");
                    recordErrors(id, title, sol.name, testCases.size(), "Compilation failed");
                    continue;
                }
                
                Object instance = solutionClass.getDeclaredConstructor().newInstance();
                Method method = findMainMethod(solutionClass);
                
                if (method == null) {
                    System.out.println("❌ Method not found");
                    recordErrors(id, title, sol.name, testCases.size(), "Method not found");
                    continue;
                }
                
                int passed = 0;
                int failed = 0;
                
                for (int i = 0; i < testCases.size(); i++) {
                    TestCase tc = testCases.get(i);
                    try {
                        Object[] args = parseMatrixInputArgs(tc.input, method);
                        Object result = method.invoke(instance, args);
                        
                        // For void methods, the result is the modified input matrix
                        String resultStr;
                        if (method.getReturnType() == void.class) {
                            // Return the modified matrix (first argument)
                            resultStr = format2DArray(args[0]);
                        } else if (result != null && result.getClass().isArray() && result.getClass().getComponentType().isArray()) {
                            resultStr = format2DArray(result);
                        } else {
                            resultStr = formatResult(result);
                        }
                        
                        if (compareResults(resultStr, tc.output, args[0].getClass())) {
                            passed++;
                            totalPassed++;
                        } else {
                            failed++;
                            totalFailed++;
                            allErrors.add(new ErrorReport(id, title, sol.name, i + 1,
                                truncate(tc.input, 50), tc.output, resultStr, null));
                        }
                    } catch (Exception e) {
                        failed++;
                        totalFailed++;
                        String errMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        allErrors.add(new ErrorReport(id, title, sol.name, i + 1,
                            truncate(tc.input, 50), tc.output, "", errMsg != null ? errMsg : "Unknown error"));
                    }
                }
                
                printTestResult(passed, failed);
                
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
                recordErrors(id, title, sol.name, testCases.size(), e.getMessage());
            }
        }
    }

    // ==================== Encode/Decode Problem Testing ====================

    private static void testEncodeDecodeProblem(String id, String title, String content,
                                                List<Solution> solutions) throws Exception {
        List<TestCase> testCases = extractTestCases(content);
        if (testCases.isEmpty()) {
            System.out.println("   ⚠️  No test cases found");
            return;
        }

        for (int solIdx = 0; solIdx < solutions.size(); solIdx++) {
            Solution sol = solutions.get(solIdx);
            System.out.print("   📝 " + sol.name + ": ");
            
            try {
                String className = "Solution_P" + id + "_" + System.currentTimeMillis() + "_" + solIdx;
                String fullCode = wrapSolutionCode(className, sol.code);
                Class<?> solutionClass = compileClass(className, fullCode);
                
                if (solutionClass == null) {
                    System.out.println("❌ Compilation failed");
                    recordErrors(id, title, sol.name, testCases.size(), "Compilation failed");
                    continue;
                }
                
                Object instance = solutionClass.getDeclaredConstructor().newInstance();
                
                // Find encode and decode methods
                Method encodeMethod = null;
                Method decodeMethod = null;
                for (Method m : solutionClass.getDeclaredMethods()) {
                    if (m.getName().equals("encode")) encodeMethod = m;
                    if (m.getName().equals("decode")) decodeMethod = m;
                }
                
                if (encodeMethod == null || decodeMethod == null) {
                    System.out.println("❌ encode/decode methods not found");
                    recordErrors(id, title, sol.name, testCases.size(), "encode/decode methods not found");
                    continue;
                }
                
                int passed = 0;
                int failed = 0;
                
                for (int i = 0; i < testCases.size(); i++) {
                    TestCase tc = testCases.get(i);
                    try {
                        // Parse input as List<String>
                        List<String> inputList = parseStringList(tc.input);
                        
                        // Encode then decode
                        String encoded = (String) encodeMethod.invoke(instance, inputList);
                        @SuppressWarnings("unchecked")
                        List<String> decoded = (List<String>) decodeMethod.invoke(instance, encoded);
                        
                        // Compare decoded with original input
                        if (inputList.equals(decoded)) {
                            passed++;
                            totalPassed++;
                        } else {
                            failed++;
                            totalFailed++;
                            allErrors.add(new ErrorReport(id, title, sol.name, i + 1,
                                tc.input, "decoded == original", decoded.toString(), null));
                        }
                    } catch (Exception e) {
                        failed++;
                        totalFailed++;
                        String errMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        allErrors.add(new ErrorReport(id, title, sol.name, i + 1,
                            tc.input, tc.output, "", errMsg != null ? errMsg : "Unknown error"));
                    }
                }
                
                printTestResult(passed, failed);
                
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
                recordErrors(id, title, sol.name, testCases.size(), e.getMessage());
            }
        }
    }

    // ==================== Helper Methods ====================

    private static void printTestResult(int passed, int failed) {
        if (failed == 0) {
            System.out.println("✅ " + passed + "/" + passed + " passed");
        } else {
            System.out.println("❌ " + passed + "/" + (passed + failed) + " passed");
        }
    }

    private static void recordErrors(String id, String title, String solName, int count, String message) {
        for (int i = 0; i < count; i++) {
            totalFailed++;
            allErrors.add(new ErrorReport(id, title, solName, i + 1, "", "", "", message));
        }
    }

    private static Method findMainMethod(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            String name = m.getName();
            // Skip utility methods
            if (name.equals("main") || name.startsWith("lambda") || name.contains("$") ||
                name.equals("toString") || name.equals("hashCode") || name.equals("equals")) {
                continue;
            }
            // Return first public non-static method (most likely the solution method)
            if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
                return m;
            }
        }
        // Fallback: return any public method
        for (Method m : clazz.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && 
                !m.getName().equals("main") && !m.getName().startsWith("lambda")) {
                return m;
            }
        }
        return null;
    }

    private static String wrapSolutionCode(String className, String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.*;\n");
        sb.append("import java.util.stream.*;\n\n");
        
        String modifiedCode = code.replaceFirst("class\\s+Solution", "public class " + className);
        sb.append(modifiedCode);
        
        return sb.toString();
    }

    private static Class<?> compileClass(String className, String sourceCode) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return null;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
            InMemoryFileManager fileManager = new InMemoryFileManager(standardFileManager);
            JavaFileObject sourceFile = new InMemoryJavaFileObject(className, sourceCode);
            
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, null, null, Arrays.asList(sourceFile)
            );
            
            boolean success = task.call();
            if (!success) {
                return null;
            }
            
            return fileManager.getClassLoader(null).loadClass(className);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Input Parsing ====================

    private static Object[] parseInputArgs(String input, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        
        // Smart parsing that handles commas inside quotes and brackets
        List<String> values = new ArrayList<>();
        int depth = 0;  // Track bracket depth
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == '"' && (i == 0 || input.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (!inQuotes && (c == '[' || c == '{')) {
                depth++;
                current.append(c);
            } else if (!inQuotes && (c == ']' || c == '}')) {
                depth--;
                current.append(c);
            } else if (!inQuotes && depth == 0 && c == ',') {
                // This is a parameter separator
                String part = current.toString().trim();
                if (part.contains("=")) {
                    String value = part.substring(part.indexOf('=') + 1).trim();
                    values.add(value);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        // Handle the last part
        String lastPart = current.toString().trim();
        if (lastPart.contains("=")) {
            String value = lastPart.substring(lastPart.indexOf('=') + 1).trim();
            values.add(value);
        }
        
        for (int i = 0; i < paramTypes.length && i < values.size(); i++) {
            args[i] = parseValue(values.get(i), paramTypes[i]);
        }
        
        return args;
    }

    private static Object[] parseMatrixInputArgs(String input, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] == char[][].class) {
                args[i] = parse2DCharArray(input);
            } else if (paramTypes[i] == int[][].class) {
                args[i] = parse2DIntArray(input);
            } else {
                // Fallback to standard parsing
                Object[] stdArgs = parseInputArgs(input, method);
                if (i < stdArgs.length) {
                    args[i] = stdArgs[i];
                }
            }
        }
        
        return args;
    }

    private static Object parseValue(String value, Class<?> type) {
        value = value.trim();
        
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == String.class) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        } else if (type == int[].class) {
            return parseIntArray(value);
        } else if (type == String[].class) {
            return parseStringArray(value);
        } else if (type == char[].class) {
            return parseCharArray(value);
        } else if (type == char[][].class) {
            return parse2DCharArray(value);
        } else if (type == List.class || type.getName().contains("List")) {
            return parseStringList("strs = " + value);
        }
        
        return null;
    }

    private static int[] parseIntArray(String value) {
        value = value.trim();
        if (value.equals("[]")) return new int[0];
        
        value = value.replaceAll("[\\[\\]]", "");
        if (value.isEmpty()) return new int[0];
        
        String[] parts = value.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    private static String[] parseStringArray(String value) {
        value = value.trim();
        if (value.equals("[]")) return new String[0];
        
        value = value.substring(1, value.length() - 1);
        if (value.isEmpty()) return new String[0];
        
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(value);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result.toArray(new String[0]);
    }

    private static char[] parseCharArray(String value) {
        String[] strArr = parseStringArray(value);
        char[] result = new char[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].length() > 0) {
                result[i] = strArr[i].charAt(0);
            }
        }
        return result;
    }

    private static char[][] parse2DCharArray(String input) {
        // Find [[...]] pattern
        int start = input.indexOf("[[");
        int end = input.lastIndexOf("]]");
        if (start == -1 || end == -1) return new char[0][0];
        
        String content = input.substring(start + 1, end + 1);
        
        List<char[]> rows = new ArrayList<>();
        int depth = 0;
        StringBuilder currentRow = new StringBuilder();
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') {
                if (depth == 0) currentRow = new StringBuilder();
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0 && currentRow.length() > 0) {
                    rows.add(parseCharRow(currentRow.toString()));
                }
            } else if (depth > 0) {
                currentRow.append(c);
            }
        }
        
        return rows.toArray(new char[0][]);
    }

    private static int[][] parse2DIntArray(String input) {
        int start = input.indexOf("[[");
        int end = input.lastIndexOf("]]");
        if (start == -1 || end == -1) return new int[0][0];
        
        String content = input.substring(start + 1, end + 1);
        
        List<int[]> rows = new ArrayList<>();
        int depth = 0;
        StringBuilder currentRow = new StringBuilder();
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') {
                if (depth == 0) currentRow = new StringBuilder();
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0 && currentRow.length() > 0) {
                    rows.add(parseIntArray("[" + currentRow + "]"));
                }
            } else if (depth > 0) {
                currentRow.append(c);
            }
        }
        
        return rows.toArray(new int[0][]);
    }

    private static char[] parseCharRow(String row) {
        List<Character> chars = new ArrayList<>();
        Matcher m = Pattern.compile("\"(.)\"").matcher(row);
        while (m.find()) {
            chars.add(m.group(1).charAt(0));
        }
        char[] result = new char[chars.size()];
        for (int i = 0; i < chars.size(); i++) {
            result[i] = chars.get(i);
        }
        return result;
    }

    private static List<String> parseStringList(String input) {
        List<String> result = new ArrayList<>();
        
        int start = input.indexOf('[');
        int end = input.lastIndexOf(']');
        if (start == -1 || end == -1) return result;
        
        String arrayPart = input.substring(start + 1, end);
        if (arrayPart.trim().isEmpty()) return result;
        
        Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(arrayPart);
        while (m.find()) {
            result.add(m.group(1));
        }
        
        return result;
    }

    // ==================== Result Formatting and Comparison ====================

    private static String format2DArray(Object arr) {
        if (arr == null) return "null";
        
        if (arr instanceof int[][]) {
            int[][] matrix = (int[][]) arr;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < matrix.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(Arrays.toString(matrix[i]));
            }
            sb.append("]");
            return sb.toString();
        } else if (arr instanceof char[][]) {
            char[][] matrix = (char[][]) arr;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < matrix.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(Arrays.toString(matrix[i]));
            }
            sb.append("]");
            return sb.toString();
        }
        
        return arr.toString();
    }

    private static String formatResult(Object result) {
        if (result == null) return "null";
        
        if (result instanceof int[]) {
            return Arrays.toString((int[]) result);
        } else if (result instanceof String[]) {
            return Arrays.toString((String[]) result);
        } else if (result instanceof boolean[]) {
            return Arrays.toString((boolean[]) result);
        } else if (result instanceof List) {
            List<?> list = (List<?>) result;
            if (list.isEmpty()) return "[]";
            
            Object first = list.get(0);
            if (first instanceof List) {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(formatResult(list.get(i)));
                }
                sb.append("]");
                return sb.toString();
            }
            return list.toString();
        }
        
        return result.toString();
    }

    private static boolean compareResults(String actual, String expected, Class<?> returnType) {
        String actualNorm = normalizeResult(actual);
        String expectedNorm = normalizeResult(expected);
        
        if (actualNorm.equals(expectedNorm)) return true;
        
        // Unordered array comparison
        if (returnType == int[].class) {
            return compareUnorderedIntArrays(actual, expected);
        }
        
        // Unordered nested list comparison
        if (returnType.getName().contains("List")) {
            return compareUnorderedNestedLists(actual, expected);
        }
        
        return false;
    }

    private static String normalizeResult(String s) {
        return s.replaceAll("\\s+", "").replace("\"", "").toLowerCase();
    }

    private static boolean compareUnorderedIntArrays(String actual, String expected) {
        int[] a = parseIntArray(actual);
        int[] b = parseIntArray(expected);
        
        if (a.length != b.length) return false;
        
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    private static boolean compareUnorderedNestedLists(String actual, String expected) {
        Set<Set<String>> actualSets = parseAsSetOfSets(actual);
        Set<Set<String>> expectedSets = parseAsSetOfSets(expected);
        return actualSets.equals(expectedSets);
    }

    private static Set<Set<String>> parseAsSetOfSets(String s) {
        Set<Set<String>> result = new HashSet<>();
        s = s.trim();
        
        if (s.startsWith("[[") || s.startsWith("[ [")) {
            int depth = 0;
            StringBuilder current = new StringBuilder();
            
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '[') {
                    if (depth == 1) current = new StringBuilder();
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 1) {
                        result.add(parseAsSet(current.toString()));
                    }
                } else if (depth >= 2) {
                    current.append(c);
                }
            }
        }
        
        return result;
    }

    private static Set<String> parseAsSet(String s) {
        Set<String> result = new HashSet<>();
        String[] parts = s.split(",");
        for (String part : parts) {
            String cleaned = part.trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "").trim();
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    // ==================== JSON Parsing Helpers ====================

    private static String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"").replace("\\n", "\n");
        }
        return null;
    }

    private static List<TestCase> extractTestCases(String json) {
        List<TestCase> testCases = new ArrayList<>();
        
        int testsStart = json.indexOf("\"tests\"");
        if (testsStart == -1) return testCases;
        
        int arrayStart = json.indexOf('[', testsStart);
        int arrayEnd = findMatchingBracket(json, arrayStart);
        
        String testsArray = json.substring(arrayStart, arrayEnd + 1);
        
        Pattern objPattern = Pattern.compile("\\{[^{}]*\\}");
        Matcher objMatcher = objPattern.matcher(testsArray);
        
        while (objMatcher.find()) {
            String testObj = objMatcher.group();
            String input = extractJsonString(testObj, "input");
            String output = extractJsonString(testObj, "output");
            
            if (input != null && output != null) {
                testCases.add(new TestCase(input, output));
            }
        }
        
        return testCases;
    }

    private static List<Solution> extractSolutions(String json) {
        List<Solution> solutions = new ArrayList<>();
        
        int solStart = json.indexOf("\"solutions\"");
        if (solStart == -1) return solutions;
        
        int arrayStart = json.indexOf('[', solStart);
        int arrayEnd = findMatchingBracket(json, arrayStart);
        
        String solArray = json.substring(arrayStart, arrayEnd + 1);
        
        int pos = 0;
        while (true) {
            int objStart = solArray.indexOf('{', pos);
            if (objStart == -1) break;
            
            int objEnd = findMatchingBrace(solArray, objStart);
            if (objEnd == -1) break;
            
            String solObj = solArray.substring(objStart, objEnd + 1);
            
            String name = extractJsonString(solObj, "name");
            String code = extractJsonString(solObj, "code");
            
            if (name != null && code != null) {
                code = code.replace("\\n", "\n").replace("\\t", "\t")
                          .replace("\\\"", "\"").replace("\\\\", "\\");
                solutions.add(new Solution(name, code));
            }
            
            pos = objEnd + 1;
        }
        
        return solutions;
    }

    private static int findMatchingBracket(String s, int start) {
        int count = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '[') count++;
            else if (s.charAt(i) == ']') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    private static int findMatchingBrace(String s, int start) {
        int count = 0;
        boolean inString = false;
        boolean escape = false;
        
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            
            if (!inString) {
                if (c == '{') count++;
                else if (c == '}') {
                    count--;
                    if (count == 0) return i;
                }
            }
        }
        return -1;
    }

    // ==================== Helper Classes ====================

    static class TestCase {
        String input, output;
        TestCase(String input, String output) { this.input = input; this.output = output; }
    }

    static class Solution {
        String name, code;
        Solution(String name, String code) { this.name = name; this.code = code; }
    }

    static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;
        InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return code; }
    }

    static class InMemoryClassFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InMemoryClassFileObject(String className) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }
        @Override public OutputStream openOutputStream() { return outputStream; }
        byte[] getBytes() { return outputStream.toByteArray(); }
    }

    static class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, InMemoryClassFileObject> classFiles = new HashMap<>();
        InMemoryFileManager(StandardJavaFileManager fileManager) { super(fileManager); }
        
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, 
                JavaFileObject.Kind kind, FileObject sibling) {
            InMemoryClassFileObject classFile = new InMemoryClassFileObject(className);
            classFiles.put(className, classFile);
            return classFile;
        }
        
        @Override
        public ClassLoader getClassLoader(Location location) {
            return new ClassLoader() {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    InMemoryClassFileObject classFile = classFiles.get(name);
                    if (classFile == null) throw new ClassNotFoundException(name);
                    byte[] bytes = classFile.getBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                }
            };
        }
    }
}
