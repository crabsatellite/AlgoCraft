package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialProblemDota2SenateJudgeTest {
    private static final Gson GSON = new Gson();
    private static final Path QUESTION_BANK = Path.of("question_bank", "official");
    private static final String WORKER_MAX_TIMEOUT_PROPERTY = "algocraft.codeExecutorWorkerMaxTimeoutMs";

    @BeforeEach
    void allowReviewedProblemBatchWorkerBudget() {
        System.setProperty(WORKER_MAX_TIMEOUT_PROPERTY, "120000");
    }

    @AfterEach
    void clearReviewedProblemBatchWorkerBudget() {
        System.clearProperty(WORKER_MAX_TIMEOUT_PROPERTY);
    }

    @Test
    void judgeAcceptsAllDota2SenateReferenceSolutions() throws IOException {
        Path problemFile = QUESTION_BANK.resolve("p485.json");
        JsonObject json = JsonParser.parseString(
                Files.readString(problemFile, StandardCharsets.UTF_8)).getAsJsonObject();
        Problem problem = GSON.fromJson(json, Problem.class);
        problem.setAssetBaseDir(QUESTION_BANK);

        var solutions = json.getAsJsonArray("solutions");
        assertEquals(4, solutions.size(), "P485 should keep the four reviewed teaching routes");
        for (int i = 0; i < solutions.size(); i++) {
            int solutionIndex = i;
            JsonObject solution = solutions.get(i).getAsJsonObject();
            String name = solution.get("name").getAsString();
            String code = solution.get("code").getAsString();

            SubmissionResult result = Judge.grade(problem, code);

            assertTrue(result.isSuccess(), () -> "p485 solution[" + solutionIndex + "] " + name
                    + " should be accepted through Judge.grade: "
                    + result.getMessage() + " details=" + summarizeDetails(result));
            assertEquals(result.getTotalCount(), result.getPassedCount(),
                    () -> "p485 solution[" + solutionIndex + "] " + name
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
