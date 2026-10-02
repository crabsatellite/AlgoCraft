package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialProblemMinimumFallingPathSumIIJudgeTest {
    private static final Gson GSON = new Gson();
    private static final Path QUESTION_BANK = Path.of("question_bank", "official");
    private static final String CODE_EXECUTOR_TIMEOUT_PROPERTY = "algocraft.codeExecutorTimeoutMs";

    @Test
    void judgeAcceptsAllMinimumFallingPathSumIIReferenceSolutions() throws IOException {
        String previousTimeout = System.getProperty(CODE_EXECUTOR_TIMEOUT_PROPERTY);
        System.setProperty(CODE_EXECUTOR_TIMEOUT_PROPERTY, "8000");
        try {
            assertJudgeAcceptsAllMinimumFallingPathSumIIReferenceSolutions();
        } finally {
            if (previousTimeout == null) {
                System.clearProperty(CODE_EXECUTOR_TIMEOUT_PROPERTY);
            } else {
                System.setProperty(CODE_EXECUTOR_TIMEOUT_PROPERTY, previousTimeout);
            }
        }
    }

    private static void assertJudgeAcceptsAllMinimumFallingPathSumIIReferenceSolutions() throws IOException {
        Path problemFile = QUESTION_BANK.resolve("p469.json");
        JsonObject json = JsonParser.parseString(
                Files.readString(problemFile, StandardCharsets.UTF_8)).getAsJsonObject();
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);

        var solutions = json.getAsJsonArray("solutions");
        assertEquals(4, solutions.size(), "P469 should keep the four reviewed teaching routes");
        for (int i = 0; i < solutions.size(); i++) {
            int solutionIndex = i;
            JsonObject solution = solutions.get(i).getAsJsonObject();
            String name = solution.get("name").getAsString();
            String code = solution.get("code").getAsString();

            SubmissionResult result = Judge.grade(problem, code);

            assertTrue(result.isSuccess(), () -> "p469 solution[" + solutionIndex + "] " + name
                    + " should be accepted through Judge.grade: "
                    + result.getMessage() + " details=" + summarizeDetails(result));
            assertEquals(result.getTotalCount(), result.getPassedCount(),
                    () -> "p469 solution[" + solutionIndex + "] " + name
                            + " should pass every official test through Judge.grade");
        }
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
