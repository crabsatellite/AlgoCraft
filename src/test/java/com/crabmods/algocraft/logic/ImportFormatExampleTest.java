package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImportFormatExampleTest {
    @Test void downloadableTemplateIsValidAndItsReferenceSolutionPasses() throws Exception {
        Problem problem = new Gson().fromJson(Files.readString(Path.of("docs/examples/p9001.json")), Problem.class);
        assertTrue(problem.isValid());
        var cases = problem.getTests().stream().map(test -> new CodeExecutor.TestCase(
                test.getInput(), test.getOutput(), problem.getId())).toList();
        var results = CodeExecutor.executeBatch(problem.getSolutions().get(0).getCode(), cases);
        assertEquals(cases.size(), results.size());
        for (var result : results) assertTrue(result.passed, result.message);
    }
}
