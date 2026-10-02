package com.crabmods.algocraft.testengine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolutionTestSuiteProviderContractTest {
    @TempDir
    Path tempDir;

    @Test
    void providerFailsWhenOfficialQuestionBankDirectoryIsMissing() {
        Path missing = tempDir.resolve("missing");

        AssertionError error = assertThrows(AssertionError.class,
                () -> SolutionTestSuite.problemProvider(missing, 1, 1));

        assertTrue(error.getMessage().contains("Official question bank directory is missing"));
    }

    @Test
    void providerFailsWhenConfiguredShardHasNoProblems() throws IOException {
        Files.createDirectories(tempDir);
        writeProblem(tempDir.resolve("p1.json"));

        AssertionError error = assertThrows(AssertionError.class,
                () -> SolutionTestSuite.problemProvider(tempDir, 2, 2));

        assertTrue(error.getMessage().contains("No official problem files found in configured range p2-p2"));
    }

    @Test
    void providerFailsWhenProblemWouldProduceNoSolutionCoverage() throws IOException {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("p1.json"), """
                {
                  "id": "1",
                  "title": "No Solutions",
                  "initialCode": "class Solution { public int solve(int x) { return x; } }",
                  "tests": [{"input": "x = 1", "output": "1"}]
                }
                """, StandardCharsets.UTF_8);

        AssertionError error = assertThrows(AssertionError.class,
                () -> SolutionTestSuite.problemProvider(tempDir, 1, 1));

        assertTrue(error.getMessage().contains("p1.json is missing solutions"));
    }

    @Test
    void providerReturnsOneArgumentPerExecutableSolution() throws IOException {
        Files.createDirectories(tempDir);
        writeProblem(tempDir.resolve("p1.json"));

        List<Arguments> arguments = SolutionTestSuite.problemProvider(tempDir, 1, 1).toList();

        assertEquals(2, arguments.size(),
                "each executable reference solution should produce one compiled solution test argument");
    }

    private static void writeProblem(Path path) throws IOException {
        Files.writeString(path, """
                {
                  "id": "1",
                  "title": "Provider Fixture",
                  "initialCode": "class Solution { public int solve(int x) { return x; } }",
                  "tests": [{"input": "x = 1", "output": "1"}],
                  "solutions": [
                    {
                      "name": "Direct",
                      "code": "class Solution { public int solve(int x) { return x; } }"
                    },
                    {
                      "name": "Still Direct",
                      "code": "class Solution { public int solve(int x) { int y = x; return y; } }"
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);
    }
}
