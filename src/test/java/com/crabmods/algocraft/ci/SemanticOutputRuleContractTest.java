package com.crabmods.algocraft.ci;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SemanticOutputRuleContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Pattern CODE_EXECUTOR_RULE = Pattern.compile(
            "id\\.equals\\(\"(\\d+)\"\\)\\s*&&\\s*\"([^\"]+)\"\\.equals\\(method\\.getName\\(\\)\\)");
    private static final Pattern STANDARD_ADAPTER_SWITCH_RULE = Pattern.compile(
            "case\\s+\"(\\d+)\"\\s*->\\s*\"([^\"]+)\"\\.equals\\(methodName\\)");
    private static final Pattern STANDARD_ADAPTER_BRANCH_RULE = Pattern.compile(
            "isProblem\\(problemId,\\s*\"(\\d+)\"\\)\\s*&&\\s*\"([^\"]+)\"\\.equals\\(method\\.getName\\(\\)\\)");
    private static final Pattern DIRECT_CODE_EXECUTOR_EXECUTE = Pattern.compile(
            "\\bCodeExecutor\\.execute\\s*\\(");

    @Test
    void productionJudgeAndOfficialBankAdapterDeclareTheSameSemanticOutputRules() throws IOException {
        Set<Rule> productionRules = extractRules(
                read("src/main/java/com/crabmods/algocraft/logic/CodeExecutor.java"),
                CODE_EXECUTOR_RULE);
        Set<Rule> officialBankRules = new TreeSet<>();
        String standardAdapter = read("src/test/java/com/crabmods/algocraft/testengine/adapters/StandardAdapter.java");
        officialBankRules.addAll(extractRules(standardAdapter, STANDARD_ADAPTER_SWITCH_RULE));
        officialBankRules.addAll(extractRules(standardAdapter, STANDARD_ADAPTER_BRANCH_RULE));

        assertFalse(productionRules.isEmpty(), "CodeExecutor semantic rule extraction found no rules");
        assertFalse(officialBankRules.isEmpty(), "StandardAdapter semantic rule extraction found no rules");
        assertEquals(productionRules, officialBankRules,
                "Judge.grade and officialQuestionBankTest must keep problem-specific output semantics in lockstep");
    }

    @Test
    void productionCodeExecutorCallSitesDoNotBypassProblemSpecificSemantics() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(PROJECT_ROOT.resolve("src/main/java/com/crabmods/algocraft"))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String relativePath = PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
                String source = Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
                collectTwoArgumentTestCaseConstructors(offenders, relativePath, source);
                collectMatches(offenders, relativePath, source, DIRECT_CODE_EXECUTOR_EXECUTE,
                        "production call sites should use Judge.grade or executeBatch with problemId");
            }
        }

        assertEquals(List.of(), offenders,
                "Problem-specific output validators are bypassed by these production call sites");
    }

    private static Set<Rule> extractRules(String source, Pattern pattern) {
        Set<Rule> rules = new TreeSet<>();
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            rules.add(new Rule(matcher.group(1), matcher.group(2)));
        }
        return rules;
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static void collectMatches(List<String> offenders, String relativePath, String source, Pattern pattern,
                                       String message) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            offenders.add(relativePath + ":" + lineNumber(source, matcher.start()) + " " + message);
        }
    }

    private static void collectTwoArgumentTestCaseConstructors(List<String> offenders, String relativePath,
                                                               String source) {
        String needle = "new CodeExecutor.TestCase(";
        int searchStart = 0;
        while (true) {
            int start = source.indexOf(needle, searchStart);
            if (start < 0) {
                return;
            }

            int depth = 1;
            int topLevelCommas = 0;
            int i = start + needle.length();
            for (; i < source.length(); i++) {
                char current = source.charAt(i);
                if (current == '(') {
                    depth++;
                } else if (current == ')') {
                    depth--;
                    if (depth == 0) {
                        if (topLevelCommas == 1) {
                            offenders.add(relativePath + ":" + lineNumber(source, start)
                                    + " CodeExecutor.TestCase must carry problemId in production call sites");
                        }
                        break;
                    }
                } else if (current == ',' && depth == 1) {
                    topLevelCommas++;
                }
            }
            searchStart = Math.max(i + 1, start + needle.length());
        }
    }

    private static int lineNumber(String source, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private record Rule(String problemId, String methodName) implements Comparable<Rule> {
        @Override
        public int compareTo(Rule other) {
            int byId = Integer.compare(Integer.parseInt(problemId), Integer.parseInt(other.problemId));
            if (byId != 0) {
                return byId;
            }
            return methodName.compareTo(other.methodName);
        }
    }
}
