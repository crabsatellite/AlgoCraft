package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemAssetResolverTest {
    private static final Gson GSON = new Gson();

    @TempDir
    Path repositoryRoot;

    @Test
    void problemVisualsComeOnlyFromStructuredDiagrams() {
        Problem problem = GSON.fromJson("""
                {
                  "id": "7",
                  "title": "Visual Problem",
                  "description": "Solve it",
                  "difficulty": "MEDIUM",
                  "initialCode": "class Solution {}",
                  "examples": [{"input": "", "output": ""}],
                  "diagrams": [
                    {"id": "prompt", "file": "p7_example1.png", "caption": "Prompt board"}
                  ]
                }
                """, Problem.class);

        assertEquals(1, problem.getVisuals().size());
        assertEquals("p7_example1.png", problem.getVisuals().get(0).file());
        assertEquals("Prompt board", problem.getVisuals().get(0).caption());
    }

    @Test
    void resolvesImagesInsideRepositoryRoot() throws IOException {
        Path image = repositoryRoot.resolve("images").resolve("p1_example.png");
        Files.createDirectories(image.getParent());
        Files.writeString(image, "not actually decoded here");

        assertTrue(ProblemAssetResolver.resolveImage(repositoryRoot, "p1_example.png").isPresent());
        assertTrue(ProblemAssetResolver.resolveImage(repositoryRoot, "images/p1_example.png").isPresent());
    }

    @Test
    void problemImageResolutionDoesNotFallbackToDeveloperOfficialBank() throws IOException {
        Path officialImage = firstOfficialImageName();
        Problem problem = new Problem();
        problem.setAssetBaseDir(repositoryRoot);
        Problem.Visual visual = new Problem.Visual("foreign", officialImage.getFileName().toString(), "Foreign prompt art");

        assertFalse(ProblemAssetResolver.resolveImage(problem, visual).isPresent(),
                "a remote/custom problem missing its own image must not silently use a same-named official image");
    }

    @Test
    void rejectsUnsafeImagePaths() {
        assertTrue(ProblemAssetResolver.normalizeImagePath("../secret.png").isEmpty());
        assertTrue(ProblemAssetResolver.normalizeImagePath("images/../../secret.png").isEmpty());
        assertTrue(ProblemAssetResolver.normalizeImagePath("images/nested/p1_example.png").isEmpty());
        assertTrue(ProblemAssetResolver.normalizeImagePath("C:/temp/secret.png").isEmpty());
        assertTrue(ProblemAssetResolver.normalizeImagePath("images/not_png.txt").isEmpty());
    }

    private static Path firstOfficialImageName() throws IOException {
        Path officialImages = Paths.get(System.getProperty("user.dir"), "question_bank", "official", "images");
        try (var files = Files.list(officialImages)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("official image fixture directory has no png images"));
        }
    }
}
