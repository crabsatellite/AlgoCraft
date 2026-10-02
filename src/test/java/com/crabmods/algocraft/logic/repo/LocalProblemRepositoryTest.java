package com.crabmods.algocraft.logic.repo;

import com.crabmods.algocraft.logic.ProblemTranslationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalProblemRepositoryTest {
    @TempDir
    Path repositoryDir;

    @Test
    void customRepositoryPrefixesProblemIdsSoItCannotShadowOfficialProblems() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Custom One"), StandardCharsets.UTF_8);

        LocalProblemRepository repository = new LocalProblemRepository("User", repositoryDir, "user");
        repository.refresh().join();

        assertEquals(1, repository.getProblems().size());
        assertEquals("user:1", repository.getProblems().get(0).getId());
    }

    @Test
    void blankPrefixLeavesOfficialCacheIdsUntouched() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Official One"), StandardCharsets.UTF_8);
        Files.writeString(repositoryDir.resolve("catalog.json"), "{\"tracks\":[]}", StandardCharsets.UTF_8);

        LocalProblemRepository repository = new LocalProblemRepository("Official", repositoryDir, "");
        repository.refresh().join();

        assertEquals(1, repository.getProblems().size());
        assertEquals("1", repository.getProblems().get(0).getId());
    }

    @Test
    void customRepositoryDefaultPrefixIsCanonical() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Custom One"), StandardCharsets.UTF_8);

        LocalProblemRepository repository = new LocalProblemRepository("Smoke Remote!", repositoryDir);
        repository.refresh().join();

        assertEquals(1, repository.getProblems().size());
        assertEquals("smoke_remote:1", repository.getProblems().get(0).getId());
    }

    @Test
    void customRepositoryDefaultPrefixPreservesUnicodeLettersForReadableNames() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Custom One"), StandardCharsets.UTF_8);

        LocalProblemRepository repository = new LocalProblemRepository("\u6570\u636e\u7ed3\u6784", repositoryDir);
        repository.refresh().join();

        assertEquals(1, repository.getProblems().size());
        assertEquals("\u6570\u636e\u7ed3\u6784:1", repository.getProblems().get(0).getId());
    }

    @Test
    void customRepositoryTranslationsAreNamespacedAndDoNotOverwriteOfficialTranslations() throws IOException {
        Path officialDir = repositoryDir.resolve("official");
        Path customDir = repositoryDir.resolve("custom");
        Files.createDirectories(officialDir);
        Files.createDirectories(customDir);
        Files.writeString(officialDir.resolve("p1.json"), problemJson("1", "Official One"), StandardCharsets.UTF_8);
        Files.writeString(customDir.resolve("p1.json"), problemJson("1", "Custom One"), StandardCharsets.UTF_8);
        writeChineseTranslation(officialDir, "\u5b98\u65b9\u4e00", "\u5b98\u65b9\u63cf\u8ff0");
        writeChineseTranslation(customDir, "\u7528\u6237\u4e00", "\u7528\u6237\u63cf\u8ff0");

        ProblemTranslationManager.clearCache();
        try {
            ProblemTranslationManager.loadAllTranslations(officialDir).join();
            assertEquals("\u5b98\u65b9\u4e00",
                    ProblemTranslationManager.getTitle("zh_cn", "1", "Official fallback"));

            LocalProblemRepository customRepository = new LocalProblemRepository("User", customDir, "user");
            customRepository.refresh().join();

            assertEquals("user:1", customRepository.getProblems().get(0).getId());
            assertEquals("\u7528\u6237\u4e00", customRepository.getProblems().get(0).getTitle("zh_cn"));
            assertEquals("\u5b98\u65b9\u4e00",
                    ProblemTranslationManager.getTitle("zh_cn", "1", "Official fallback"));
        } finally {
            ProblemTranslationManager.clearCache();
        }
    }

    @Test
    void customRepositoryTranslationsSurviveCanonicalPrefixesWithUnderscores() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Custom One"), StandardCharsets.UTF_8);
        writeChineseTranslation(repositoryDir, "\u4e0b\u5212\u7ebf\u4ed3\u5e93", "\u4e0b\u5212\u7ebf\u4ed3\u5e93\u63cf\u8ff0");

        ProblemTranslationManager.clearCache();
        try {
            LocalProblemRepository repository = new LocalProblemRepository("Smoke Remote", repositoryDir);
            repository.refresh().join();

            assertEquals(1, repository.getProblems().size());
            assertEquals("smoke_remote:1", repository.getProblems().get(0).getId());
            assertEquals("\u4e0b\u5212\u7ebf\u4ed3\u5e93", repository.getProblems().get(0).getTitle("zh_cn"));
            assertEquals("\u4e0b\u5212\u7ebf\u4ed3\u5e93\u63cf\u8ff0",
                    repository.getProblems().get(0).getDescription("zh_cn"));
            assertEquals("Official fallback",
                    ProblemTranslationManager.getTitle("zh_cn", "1", "Official fallback"),
                    "custom repository translations must not populate the official numeric id");
        } finally {
            ProblemTranslationManager.clearCache();
        }
    }

    @Test
    void removedCustomRepositoryTranslationsDoNotSurviveRefresh() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Custom One"), StandardCharsets.UTF_8);
        Path translation = writeChineseTranslation(repositoryDir, "\u65e7\u7ffb\u8bd1", "\u65e7\u63cf\u8ff0");

        ProblemTranslationManager.clearCache();
        try {
            LocalProblemRepository repository = new LocalProblemRepository("User", repositoryDir, "user");
            repository.refresh().join();
            assertEquals("\u65e7\u7ffb\u8bd1", repository.getProblems().get(0).getTitle("zh_cn"));

            Files.delete(translation);
            repository.refresh().join();

            assertEquals("Custom One", repository.getProblems().get(0).getTitle("zh_cn"),
                    "removed custom translation should not survive a repository refresh");
            assertEquals("Custom One",
                    ProblemTranslationManager.getTitle("zh_cn", "user:1", "Custom One"),
                    "translation cache should clear stale entries for the custom repository namespace");
        } finally {
            ProblemTranslationManager.clearCache();
        }
    }

    @Test
    void damagedTranslationDirectoryDoesNotClearPreviouslyLoadedTranslations() throws IOException {
        Path translation = writeChineseTranslation(repositoryDir, "\u7a33\u5b9a\u7ffb\u8bd1", "\u7a33\u5b9a\u63cf\u8ff0");

        ProblemTranslationManager.clearCache();
        try {
            ProblemTranslationManager.loadAllTranslations(repositoryDir, "user").join();
            assertEquals("\u7a33\u5b9a\u7ffb\u8bd1",
                    ProblemTranslationManager.getTitle("zh_cn", "user:1", "English fallback"));

            Path langDir = translation.getParent();
            Files.delete(translation);
            Files.delete(langDir);
            Files.writeString(langDir, "not a translation directory", StandardCharsets.UTF_8);

            assertThrows(CompletionException.class,
                    () -> ProblemTranslationManager.loadAllTranslations(repositoryDir, "user").join(),
                    "a damaged translation directory should fail instead of publishing an empty translation cache");
            assertEquals("\u7a33\u5b9a\u7ffb\u8bd1",
                    ProblemTranslationManager.getTitle("zh_cn", "user:1", "English fallback"),
                    "failed translation refresh should preserve the last complete translation snapshot");
        } finally {
            ProblemTranslationManager.clearCache();
        }
    }

    @Test
    void failedMultilingualTranslationRefreshDoesNotPartiallyClearOtherLanguages() throws IOException {
        Path chineseTranslation = writeTranslation(
                repositoryDir,
                "zh_cn",
                "\u7a33\u5b9a\u4e2d\u6587",
                "\u7a33\u5b9a\u4e2d\u6587\u63cf\u8ff0"
        );
        Path japaneseTranslation = writeTranslation(
                repositoryDir,
                "ja_jp",
                "\u5b89\u5b9a\u3057\u305f\u65e5\u672c\u8a9e",
                "\u5b89\u5b9a\u3057\u305f\u65e5\u672c\u8a9e\u306e\u8aac\u660e"
        );

        ProblemTranslationManager.clearCache();
        try {
            ProblemTranslationManager.loadAllTranslations(repositoryDir, "user").join();
            assertEquals("\u7a33\u5b9a\u4e2d\u6587",
                    ProblemTranslationManager.getTitle("zh_cn", "user:1", "English fallback"));
            assertEquals("\u5b89\u5b9a\u3057\u305f\u65e5\u672c\u8a9e",
                    ProblemTranslationManager.getTitle("ja_jp", "user:1", "English fallback"));

            Path chineseLangDir = chineseTranslation.getParent();
            Files.delete(chineseTranslation);
            Files.delete(chineseLangDir);
            Files.writeString(chineseLangDir, "not a translation directory", StandardCharsets.UTF_8);
            Files.delete(japaneseTranslation);
            Files.delete(japaneseTranslation.getParent());

            assertThrows(CompletionException.class,
                    () -> ProblemTranslationManager.loadAllTranslations(repositoryDir, "user").join(),
                    "a failed multilingual translation refresh should not publish partial per-language results");
            assertEquals("\u7a33\u5b9a\u4e2d\u6587",
                    ProblemTranslationManager.getTitle("zh_cn", "user:1", "English fallback"),
                    "the damaged language should preserve its last complete translation snapshot");
            assertEquals("\u5b89\u5b9a\u3057\u305f\u65e5\u672c\u8a9e",
                    ProblemTranslationManager.getTitle("ja_jp", "user:1", "English fallback"),
                    "other languages must not be cleared by a failed multilingual refresh");
        } finally {
            ProblemTranslationManager.clearCache();
        }
    }

    @Test
    void damagedCustomRepositoryTranslationsFailRefreshAndPreservePreviousSnapshots() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Custom One"), StandardCharsets.UTF_8);
        Path translation = writeChineseTranslation(repositoryDir, "\u7a33\u5b9a\u7ffb\u8bd1", "\u7a33\u5b9a\u63cf\u8ff0");

        ProblemTranslationManager.clearCache();
        try {
            LocalProblemRepository repository = new LocalProblemRepository("User", repositoryDir, "user");
            repository.refresh().join();
            assertEquals("\u7a33\u5b9a\u7ffb\u8bd1", repository.getProblems().get(0).getTitle("zh_cn"));

            Path langDir = translation.getParent();
            Files.delete(translation);
            Files.delete(langDir);
            Files.writeString(langDir, "not a translation directory", StandardCharsets.UTF_8);

            CompletionException error = assertThrows(CompletionException.class, () -> repository.refresh().join(),
                    "damaged custom repository translations must fail the refresh instead of hiding stale data risk");
            assertTrue(error.toString().contains("Translation path is not a directory")
                            || (error.getCause() != null
                            && error.getCause().getMessage().contains("Translation path is not a directory")),
                    "failure should identify the damaged custom translation directory");
            assertEquals(1, repository.getProblems().size(),
                    "failed translation refresh should preserve the last complete custom problem snapshot");
            assertEquals("\u7a33\u5b9a\u7ffb\u8bd1", repository.getProblems().get(0).getTitle("zh_cn"),
                    "failed translation refresh should preserve the last complete custom translation snapshot");
        } finally {
            ProblemTranslationManager.clearCache();
        }
    }

    @Test
    void damagedLocalProblemDoesNotPublishPartialCustomProblemSnapshot() throws IOException {
        Files.writeString(repositoryDir.resolve("p1.json"), problemJson("1", "Stable One"), StandardCharsets.UTF_8);
        Files.writeString(repositoryDir.resolve("p2.json"), problemJson("2", "Stable Two"), StandardCharsets.UTF_8);

        LocalProblemRepository repository = new LocalProblemRepository("User", repositoryDir, "user");
        repository.refresh().join();
        assertEquals(2, repository.getProblems().size());

        Files.writeString(repositoryDir.resolve("p2.json"), "{not valid json", StandardCharsets.UTF_8);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.refresh().join(),
                "damaged custom problem files must fail the refresh instead of silently skipping a problem");
        assertTrue(error.toString().contains("p2.json")
                        || (error.getCause() != null && error.getCause().getMessage().contains("p2.json")),
                "failure should identify the damaged custom problem file");
        assertEquals(2, repository.getProblems().size(),
                "failed custom refresh should keep the last complete problem snapshot");
        assertEquals("user:1", repository.getProblems().get(0).getId());
        assertEquals("user:2", repository.getProblems().get(1).getId());
    }

    @Test
    void refreshFailureKeepsPreviouslyLoadedProblemSnapshot() throws IOException {
        Path repoPath = repositoryDir.resolve("repo");
        Files.createDirectories(repoPath);
        Files.writeString(repoPath.resolve("p1.json"), problemJson("1", "Stable One"), StandardCharsets.UTF_8);

        LocalProblemRepository repository = new LocalProblemRepository("User", repoPath, "user");
        repository.refresh().join();
        assertEquals(1, repository.getProblems().size());
        assertEquals("user:1", repository.getProblems().get(0).getId());

        Files.delete(repoPath.resolve("p1.json"));
        Files.delete(repoPath);
        Files.writeString(repoPath, "not a repository directory", StandardCharsets.UTF_8);

        assertThrows(CompletionException.class, () -> repository.refresh().join(),
                "a repository listing failure should be visible to callers instead of publishing an empty success");
        assertEquals(1, repository.getProblems().size(),
                "a failed refresh must preserve the last complete problem snapshot");
        assertEquals("user:1", repository.getProblems().get(0).getId());
    }

    @Test
    void missingRepositoryDirectoryAfterSuccessfulRefreshKeepsPreviouslyLoadedProblemSnapshot() throws IOException {
        Path repoPath = repositoryDir.resolve("repo");
        Files.createDirectories(repoPath);
        Files.writeString(repoPath.resolve("p1.json"), problemJson("1", "Stable One"), StandardCharsets.UTF_8);

        LocalProblemRepository repository = new LocalProblemRepository("User", repoPath, "user");
        repository.refresh().join();
        assertEquals(1, repository.getProblems().size());

        Files.delete(repoPath.resolve("p1.json"));
        Files.delete(repoPath);

        CompletionException error = assertThrows(CompletionException.class, () -> repository.refresh().join(),
                "a missing repository directory after a successful load should be visible to callers");
        assertTrue(error.toString().contains("Repository directory is missing")
                        || (error.getCause() != null
                        && error.getCause().getMessage().contains("Repository directory is missing")),
                "failure should identify the missing repository directory");
        assertEquals(1, repository.getProblems().size(),
                "a failed refresh must preserve the last complete problem snapshot");
        assertEquals("user:1", repository.getProblems().get(0).getId());
    }

    private static String problemJson(String id, String title) {
        return """
                {
                  "id": "%s",
                  "title": "%s",
                  "description": "Local problem",
                  "difficulty": "EASY",
                  "initialCode": "class Solution { public int solve() { return 1; } }",
                  "examples": [{"input": "", "output": "1"}],
                  "tests": [{"input": "", "output": "1"}]
                }
                """.formatted(id, title);
    }

    private static Path writeChineseTranslation(Path repositoryDir, String title, String description) throws IOException {
        return writeTranslation(repositoryDir, "zh_cn", title, description);
    }

    private static Path writeTranslation(Path repositoryDir, String language, String title, String description)
            throws IOException {
        Path langDir = repositoryDir.resolve("lang").resolve(language);
        Files.createDirectories(langDir);
        Path translationFile = langDir.resolve("p1.json");
        Files.writeString(translationFile, """
                {
                  "title": "%s",
                  "description": "%s"
                }
                """.formatted(title, description), StandardCharsets.UTF_8);
        return translationFile;
    }
}
