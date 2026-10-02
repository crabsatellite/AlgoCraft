package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeOfficialAllReferenceTest {
    private static final Gson GSON = new Gson();
    private static final Path QUESTION_BANK = Paths.get(System.getProperty("user.dir"), "question_bank", "official");
    private static final int PROBLEM_START = Integer.getInteger("algocraft.officialProblemStart", 1);
    private static final int PROBLEM_END = Integer.getInteger("algocraft.officialProblemEnd", 500);
    private static final boolean ISOLATED_PROCESS_MODE =
            Boolean.getBoolean("algocraft.officialJudgeIsolatedProcess");
    private static final boolean ALL_TESTS_MODE = Boolean.getBoolean("algocraft.officialJudgeAllTests");

    static Stream<Arguments> officialReferenceSolutions() throws IOException {
        return Files.list(QUESTION_BANK)
                .filter(path -> path.getFileName().toString().matches("p\\d+\\.json"))
                .filter(path -> isProblemInConfiguredRange(problemNumber(path)))
                .sorted(Comparator.comparingInt(JudgeOfficialAllReferenceTest::problemNumber))
                .flatMap(JudgeOfficialAllReferenceTest::solutionArguments);
    }

    @ParameterizedTest(name = "{0} solution[{1}] {2}")
    @MethodSource("officialReferenceSolutions")
    void judgeAcceptsEveryOfficialReferenceSolution(Path problemFile, int solutionIndex, String solutionName) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(problemFile, StandardCharsets.UTF_8)).getAsJsonObject();
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);
        if (!ALL_TESTS_MODE) {
            problem.setTests(List.of());
        }
        String code = json.getAsJsonArray("solutions").get(solutionIndex).getAsJsonObject().get("code").getAsString();

        SubmissionResult result = ISOLATED_PROCESS_MODE
                ? Judge.grade(problem, code)
                : Judge.gradeForTest(problem, code, CodeExecutor::executeBatchInProcess);

        assertTrue(result.isSuccess(), () -> problemFile.getFileName() + " solution[" + solutionIndex + "] "
                + solutionName + " should be accepted through " + judgeMode() + ": "
                + result.getMessage() + " details=" + summarizeDetails(result));
        assertEquals(result.getTotalCount(), result.getPassedCount(),
                () -> problemFile.getFileName() + " solution[" + solutionIndex + "] "
                        + solutionName + " should pass " + judgeScope() + " through " + judgeMode());
    }

    private static String judgeMode() {
        return ISOLATED_PROCESS_MODE ? "Judge.grade isolated process mode" : "Judge.grade in-process batch mode";
    }

    private static String judgeScope() {
        return ALL_TESTS_MODE ? "every official test" : "every visible example";
    }

    private static Stream<Arguments> solutionArguments(Path problemFile) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(problemFile, StandardCharsets.UTF_8)).getAsJsonObject();
            var solutions = json.getAsJsonArray("solutions");
            return IntStream.range(0, solutions.size())
                    .mapToObj(index -> {
                        JsonObject solution = solutions.get(index).getAsJsonObject();
                        return Arguments.of(
                                problemFile,
                                index,
                                solution.has("name") ? solution.get("name").getAsString() : "<unnamed>"
                        );
                    });
        } catch (IOException e) {
            throw new AssertionError("Failed to read official problem " + problemFile, e);
        }
    }

    private static int problemNumber(Path path) {
        String name = path.getFileName().toString();
        return Integer.parseInt(name.substring(1, name.length() - 5));
    }

    private static boolean isProblemInConfiguredRange(int problemNumber) {
        return problemNumber >= PROBLEM_START && problemNumber <= PROBLEM_END;
    }

    private static String summarizeDetails(SubmissionResult result) {
        StringBuilder summary = new StringBuilder();
        for (SubmissionResult.TestCaseResult detail : result.getDetails()) {
            if (!detail.isPassed()) {
                summary.append("{input=").append(detail.getInput())
                        .append(", expected=").append(detail.getExpected())
                        .append(", actual=").append(detail.getActual())
                        .append(", error=").append(detail.getError())
                        .append("}");
            }
        }
        return summary.toString();
    }
}
