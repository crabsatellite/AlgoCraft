package com.crabmods.algocraft.ci;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationGateContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void gradleCheckIncludesOfficialBankAndGameTestServerGates() throws IOException {
        String build = read("build.gradle");

        assertContains(build, "tasks.named('check')");
        assertContains(build, "dependsOn tasks.named('officialQuestionBankTest')");
        assertContains(build, "dependsOn tasks.named('runGameTestServer')");
        assertContains(build, "tasks.register('officialQuestionBankQualityTest', Test)");
        assertContains(build, "include '**/QuestionBankQualityTest.class'");
        assertContains(build, "include '**/OfficialProblemStatementRenderTest.class'");
        assertContains(build, "include '**/OfficialProblemChineseTranslationTest.class'");
        assertContains(build, "include '**/OfficialDiagramLayoutTest.class'");
        assertContains(build, "tasks.register('officialQuestionBankTest')");
        assertContains(build, "dependsOn officialQuestionBankQualityTask");
        assertContains(build, "include '**/JudgeOfficialAllReferenceTest.class'");
        assertContains(build, "systemProperty 'algocraft.officialProblemStart'");
        assertContains(build, "systemProperty 'algocraft.officialProblemEnd'");
        assertContains(build, "testTask.systemProperty 'algocraft.codeExecutorTimeoutMs'");
        assertContains(build, "System.getProperty('algocraft.codeExecutorTimeoutMs', '8000')");
    }

    @Test
    void realIdeClientRuntimeDirectoriesStayOutOfSourceControl() throws IOException {
        String gitignore = read(".gitignore");
        String build = read("build.gradle");

        assertContains(build, "gameDirectory = project.file('run-ide-smoke')");
        assertContains(build, "gameDirectory = project.file('run-ide-stress')");
        assertContains(gitignore, "run-ide-smoke/");
        assertContains(gitignore, "run-ide-stress/");
    }

    @Test
    void gradleIdeAcceptanceFailsOnMissingRuntimeEvidence() throws IOException {
        String build = read("build.gradle");

        assertContains(build, "tasks.register('verifyIdeClientSmokeResult')");
        assertContains(build, "tasks.register('verifyIdeClientStressResult')");
        assertContains(build, "requireIdeClientCaptureEvidence");
        assertContains(build, "captureFile.toPath().startsWith(outputDir.toPath())");
        assertContains(build, "capture ${captureName} screenshot file is missing");
        assertContains(build, "capture ${captureName} screenshot file is empty");
        assertContains(build, "javax.imageio.ImageIO.read(captureFile)");
        assertContains(build, "screenshot file is not a readable image");
        assertContains(build, "screenshot dimensions");
        assertContains(build, "do not match reported framebuffer");
        assertContains(build, "measureScreenshotPixels");
        assertContains(build, "screenshot.getRGB(x, y)");
        assertContains(build, "screenshot file is visually blank or flat");
        assertContains(build, "screenshot file has too few visible non-dark pixels");
        assertContains(build, "framebufferWidth");
        assertContains(build, "framebufferHeight");
        assertContains(build, "guiWidth");
        assertContains(build, "guiHeight");
        assertContains(build, "throw new GradleException(\"IDE client smoke test did not write");
        assertContains(build, "throw new GradleException(\"IDE client stress test did not write");
        assertContains(build, "requireIdeClientQuestionBankRoot");
        assertContains(build, "ideClientQuestionBankRoot = layout.projectDirectory.dir('question_bank/official').asFile.canonicalFile");
        assertContains(build, "expectedRoot = ideClientQuestionBankRoot");
        assertContains(build, "did not report questionBankRoot");
        assertContains(build, "questionBankRoot does not contain manifest.json");
        assertContains(build, "ideClientRequiredCheckFragments.each");
        assertContains(build, "seenCaptureNames");
        assertContains(build, "reported duplicate screenshot capture");
        assertContains(build, "Smoke problem statement widget loaded at least one diagram image block");
        assertContains(build, "Smoke problem statement widget has no missing-image placeholder blocks");
        assertContains(build, "ide-wide statement widget has a visible loaded diagram image block");
        assertContains(build, "ide-narrow statement widget has a visible loaded diagram image block");
        assertContains(build, "visualMetadataCount");
        assertContains(build, "loadedImageBlocks");
        assertContains(build, "missingImageBlocks");
        assertContains(build, "visibleImageBlocks");
        assertContains(build, "has visual metadata but no loaded image blocks");
        assertContains(build, "reported missing-image placeholders");
        assertContains(build, "has no visible loaded image block");
        assertContains(build, "RUN has exactly one active clickable widget at its center");
        assertContains(build, "SUBMIT has exactly one active clickable widget at its center");
        assertContains(build, "HISTORY has exactly one active clickable widget at its center");
        assertContains(build, "TOGGLE_VIEW has exactly one active clickable widget at its center");
        assertContains(build, "WEB has exactly one active clickable widget at its center");
        assertContains(build, "CLOSE has exactly one active clickable widget at its center");
        assertContains(build, "Update Official has exactly one active clickable widget at its center");
        assertContains(build, "Import has exactly one active clickable widget at its center");
        assertContains(build, "Pasted solution Ctrl+V keybinding pastes through the focused editor");
        assertContains(build, "Pasted solution uses LF newlines in the editor");
        assertContains(build, "Pasted solution keeps the Java class declaration");
        assertContains(build, "Pasted solution preserves the expected line count");
        assertContains(build, "Pasted solution Enter over selected code replaces selected text instead of keeping stale code");
        assertContains(build, "Pasted solution Ctrl+Z restores the pasted code after selection replacement");
        assertContains(build, "Pasted solution Ctrl+Y reapplies the selection replacement");
        assertContains(build, "Pasted solution final Ctrl+Z leaves the runnable pasted code in the editor");
        assertContains(build, "Run console has no failure after the pasted solution");
        assertContains(build, "Statement-only mode shows the problem panel and hides the editor");
        assertContains(build, "View button returns to coding mode");
        assertContains(build, "Submit button runs the local judge without needing a server connection");
        assertContains(build, "Submit button reports every official test case as passed");
        assertContains(build, "Submit console has no failure after the pasted solution");
        assertContains(build, "Submit starts exactly one asynchronous judge action on click");
        assertContains(build, "Submit enters an in-progress state while the judge future is pending");
        assertContains(build, "Submit button is disabled while judge future is pending");
        assertContains(build, "Repeated Submit clicks do not start a second concurrent judge");
        assertContains(build, "View button remains responsive while submit judge is pending");
        assertContains(build, "Submit clears in-progress state after judge success");
        assertContains(build, "Submit button is re-enabled after judge success");
        assertContains(build, "Submit clears in-progress state after judge failure");
        assertContains(build, "Submit button is re-enabled after judge failure");
        assertContains(build, "Pending Submit records a rejected result after judge failure");
        assertContains(build, "Pending Submit writes a translated failure after judge failure");
        assertContains(build, "Pending Submit failure does not mutate the user's code text");
        assertContains(build, "History back button returns to IDE");
        assertContains(build, "Import screen opens in remote repository mode");
        assertContains(build, "Import remote mode reports empty input through translated status text");
        assertContains(build, "Import type toggle switches to local file mode");
        assertContains(build, "Import local mode reports empty input through translated status text");
        assertContains(build, "Import type toggle returns to remote repository mode");
        assertContains(build, "Import remote submit starts exactly one repository install action");
        assertContains(build, "Import remote submit passes the typed name and URL to the repository installer");
        assertContains(build, "Import remote submit enters an in-progress state");
        assertContains(build, "Import button is disabled while remote import is pending");
        assertContains(build, "Import remote submit shows translated downloading status");
        assertContains(build, "Repeated Import clicks do not start a second concurrent remote import");
        assertContains(build, "Import clears in-progress state after success");
        assertContains(build, "Import button is re-enabled after success");
        assertContains(build, "Import shows translated success status after remote import succeeds");
        assertContains(build, "Import can be started again after a successful remote import");
        assertContains(build, "Import clears in-progress state after failure");
        assertContains(build, "Import button is re-enabled after failure");
        assertContains(build, "Import shows translated failure status and the remote error message");
        assertContains(build, "Import failure status marks oversized external error messages as truncated");
        assertContains(build, "Import failure status does not retain the full external error payload");
        assertContains(build, "Import failure status remains short enough for the in-game UI");
        assertContains(build, "Import back button returns to IDE");
        assertContains(build, "Update Official enters an in-progress state while the refresh future is pending");
        assertContains(build, "Update Official button is disabled while a refresh is in progress");
        assertContains(build, "Update Official shows the translated running state in the console");
        assertContains(build, "Repeated Update Official clicks do not start a second concurrent refresh");
        assertContains(build, "Update Official clears in-progress state after success");
        assertContains(build, "Update Official button is re-enabled after success");
        assertContains(build, "Update Official can be started again after a successful refresh");
        assertContains(build, "Update Official enters an in-progress state before a failure");
        assertContains(build, "Update Official clears in-progress state after failure");
        assertContains(build, "Update Official button is re-enabled after failure");
        assertContains(build, "Update Official failure console marks oversized external error messages as truncated");
        assertContains(build, "Update Official failure console does not retain the full external error payload");
        assertContains(build, "Update Official failure console remains short enough for the in-game UI");
        assertContains(build, "Run button reports compilation errors without crashing");
        assertContains(build, "Run console does not retain the oversized actual output in full");
        assertContains(build, "Hostile wrong-answer submit is classified as Wrong Answer, not a crash");
        assertContains(build, "Failed submit detail keeps the actual output bounded");
        assertContains(build, "Failed submit detail does not retain the oversized actual output in full");
        assertContains(build, "Submit button accepts the corrected solution after a failed submission");
        assertContains(build, "Run console does not report PASS for uncompilable code");
        assertContains(build, "Submit button still uses the local judge for uncompilable code");
        assertContains(build, "Submit button rejects uncompilable code through Judge.grade");
        assertContains(build, "Compilation-error submit records an error detail");
        assertContains(build, "Compilation-error submit does not mutate the user's code text");
        assertContains(build, "Submit button accepts the corrected solution after a compilation-error submission");
        assertContains(build, "Submit button rejects oversized code before invoking Judge.grade");
        assertContains(build, "Submit button shows the translated oversized-code rejection before network send");
        assertContains(build, "Oversized submit rejection does not mutate the user's code text");
        assertContains(build, "Submit button accepts corrected solution after oversized-code rejection");
        assertContains(build, "Resize preserves the selected problem");
        assertContains(build, "Resize preserves the pasted solution text");
        assertContains(build, "IDE screen survived modal button round-trips");
        assertContains(build, "Narrow View button switches to statement mode");
        assertContains(build, "Narrow statement mode shows the problem image panel without the editor");
        assertContains(build, "Close button exits the IDE screen");
        assertContains(build, "tasks.register('algocraftCoreAcceptanceTest')");
        assertContains(build, "dependsOn tasks.named('build')");
        assertContains(build, "dependsOn tasks.named('ideClientAcceptanceTest')");
    }

    @Test
    void ideRemoteFailureStatusTextIsBoundedBeforeRendering() throws IOException {
        String formatter = read("src/main/java/com/crabmods/algocraft/client/gui/modern/UiErrorMessages.java");
        String screen = read("src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java");
        String importScreen = read("src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java");
        String smoke = read("src/main/java/com/crabmods/algocraft/client/test/IdeClientSmokeTest.java");
        String formatterTest = read("src/test/java/com/crabmods/algocraft/client/gui/modern/UiErrorMessagesTest.java");

        assertContains(formatter, "MAX_ERROR_MESSAGE_CHARS = 240");
        assertContains(formatter, "\" [truncated]\"");
        assertContains(formatter, "message.replaceAll(\"\\\\s+\", \" \").trim()");
        assertContains(screen, "UiErrorMessages.fromThrowable(error)");
        assertContains(importScreen, "UiErrorMessages.fromThrowable(error)");
        assertContains(importScreen, "UiErrorMessages.fromThrowable(e)");
        assertFalse(screen.contains("cause.getMessage()"),
                "official update failure console must not render raw external exception messages");
        assertFalse(importScreen.contains("e.getMessage()"),
                "import failure status must not render raw external exception messages");
        assertContains(smoke, "HOSTILE_REMOTE_ERROR_TAIL");
        assertContains(smoke, "requireBoundedExternalErrorText(updateFailureConsole, \"Update Official failure console\")");
        assertContains(smoke, "requireBoundedExternalErrorText(importFailureStatus, \"Import failure status\")");
        assertContains(formatterTest, "oversizedExternalErrorMessagesAreTruncatedForGameUi");
        assertContains(formatterTest, "bounded message must not retain the full external error payload");
    }

    @Test
    void inGameIdeUsesLocalizedProblemTextAndDiagramCaptions() throws IOException {
        String problem = read("src/main/java/com/crabmods/algocraft/logic/Problem.java");
        String translations = read("src/main/java/com/crabmods/algocraft/logic/ProblemTranslationManager.java");
        String displayText = read("src/main/java/com/crabmods/algocraft/client/gui/component/ProblemDisplayText.java");
        String statement = read("src/main/java/com/crabmods/algocraft/client/gui/component/ProblemStatementWidget.java");
        String document = read("src/main/java/com/crabmods/algocraft/client/gui/component/ProblemStatementDocument.java");
        String list = read("src/main/java/com/crabmods/algocraft/client/gui/component/ProblemSelectionList.java");
        String screen = read("src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java");
        String translationTest = read("src/test/java/com/crabmods/algocraft/questionbank/OfficialProblemChineseTranslationTest.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(translations, "@SerializedName(\"diagrams\")");
        assertContains(translations, "class DiagramTranslation");
        assertContains(translations, "getDiagramCaption");
        assertContains(problem, "public List<Visual> getVisuals(String lang)");
        assertContains(displayText, "minecraft.getLanguageManager().getSelected()");
        assertContains(displayText, "problem.getTitle(normalizeLanguageCode(lang))");
        assertContains(displayText, "problem.getDescription(normalizeLanguageCode(lang))");
        assertContains(displayText, "problem.getVisuals(normalizeLanguageCode(lang))");
        assertContains(statement, "ProblemDisplayText.visuals(problem)");
        assertContains(statement, "ProblemDisplayText.description(this.problem)");
        assertContains(statement, "ProblemDisplayText.title(problem)");
        assertContains(document, "buildVisualBlocks(Problem problem, List<Problem.Visual> visuals");
        assertContains(list, "ProblemDisplayText.title(problem)");
        assertContains(screen, "ProblemDisplayText.title(problem).toLowerCase(Locale.ROOT)");
        assertContains(screen, "ProblemDisplayText.title(currentProblem)");
        assertContains(screen, "ProblemDisplayText.title(submittedProblem)");
        assertFalse(statement.contains("this.problem.getDescription()"),
                "problem statement panel must use the current Minecraft language, not raw English descriptions");
        assertFalse(statement.contains("problem.getTitle()"),
                "problem statement panel must use the current Minecraft language, not raw English titles");
        assertFalse(list.contains("problem.getTitle()"),
                "problem list must use the current Minecraft language, not raw English titles");
        assertFalse(screen.contains("problem.getTitle().toLowerCase(Locale.ROOT)"),
                "IDE search must match localized problem titles");
        assertContains(translationTest, "should translate every diagram caption");
        assertContains(translationTest, "Problem.getVisuals(lang) should apply translated diagram captions");
        assertContains(repositoryTest, "forceRefreshRejectsMissingDiagramCaptionTranslationsWithoutReplacingCache");
    }

    @Test
    void webIdeCodeExecutionApisUseBoundedBackpressureExecutor() throws IOException {
        String webServer = read("src/main/java/com/crabmods/algocraft/web/AlgoCraftWebServer.java");
        String webServerTest = read("src/test/java/com/crabmods/algocraft/web/AlgoCraftWebServerTest.java");
        String integrationTest = read("src/test/java/com/crabmods/algocraft/logic/ProblemManagerWebApiIntegrationTest.java");

        assertContains(webServer, "WEB_EXECUTOR_MAX_THREADS = 4");
        assertContains(webServer, "WEB_EXECUTOR_QUEUE_CAPACITY = 64");
        assertContains(webServer, "new ThreadPoolExecutor(");
        assertContains(webServer, "new LinkedBlockingQueue<>(WEB_EXECUTOR_QUEUE_CAPACITY)");
        assertContains(webServer, "new ThreadPoolExecutor.CallerRunsPolicy()");
        assertContains(webServer, "thread.setDaemon(true)");
        assertContains(webServer, "WEB_JUDGE_MAX_CONCURRENT = 2");
        assertContains(webServer, "new Semaphore(WEB_JUDGE_MAX_CONCURRENT)");
        assertContains(webServer, "WEB_JUDGE_PERMITS.tryAcquire()");
        assertContains(webServer, "Code execution is busy; please retry soon");
        assertContains(webServer, "releaseWebJudgePermit()");
        assertContains(webServerTest, "webServerUsesBoundedBackpressureExecutorForCodeExecutionApis");
        assertContains(webServerTest, "web IDE run/submit APIs must not use an unbounded cached thread pool");
        assertContains(webServerTest, "web IDE must use a finite queue so request pressure cannot allocate unbounded threads");
        assertContains(integrationTest,
                "webApiSubmitRejectsExtraConcurrentJudgeRequestButKeepsProblemBrowsingResponsive");
        assertContains(integrationTest, "lightweight browsing APIs should remain responsive while judge slots are full");
        assertFalse(webServer.contains("Executors.newCachedThreadPool"),
                "web IDE run/submit APIs must not use an unbounded cached thread pool");
    }

    @Test
    void ideClientAcceptanceKeepsSmokeStressEcjAndRepresentativeProblemGates() throws IOException {
        String build = read("build.gradle");

        assertContains(build, "tasks.named('runIdeClientSmokeTest')");
        assertContains(build, "dependsOn tasks.named('cleanIdeClientSmokeOutput')");
        assertContains(build, "ignoreExitValue = true\n    finalizedBy tasks.named('verifyIdeClientSmokeResult')");
        assertContains(build, "finalizedBy tasks.named('verifyIdeClientSmokeResult')");
        assertContains(build, "tasks.named('runIdeClientStressTest')");
        assertContains(build, "dependsOn tasks.named('cleanIdeClientStressOutput')");
        assertContains(build, "mustRunAfter tasks.named('runIdeClientSmokeTest')");
        assertContains(build, "ignoreExitValue = true\n    finalizedBy tasks.named('verifyIdeClientStressResult')");
        assertContains(build, "finalizedBy tasks.named('verifyIdeClientStressResult')");
        assertTrue(countOccurrences(build, "ignoreExitValue = true") >= 2,
                "both real Minecraft IDE client run tasks must let result verification decide pass/fail");

        assertContains(build, "tasks.register('ideAcceptanceTest')");
        assertContains(build, "dependsOn tasks.named('test')");
        assertContains(build, "dependsOn tasks.named('runIdeClientSmokeTest')");
        assertContains(build, "dependsOn tasks.named('verifyIdeClientSmokeResult')");
        assertContains(build, "tasks.register('ideStressTest')");
        assertContains(build, "mustRunAfter tasks.named('ideAcceptanceTest')");
        assertContains(build, "dependsOn tasks.named('runIdeClientStressTest')");
        assertContains(build, "dependsOn tasks.named('verifyIdeClientStressResult')");
        assertContains(build, "tasks.register('ideClientAcceptanceTest')");
        assertContains(build, "description = 'Runs all compatible IDE acceptance cases in one loaded world with ten interaction passes.'");
        assertContains(build, "dependsOn tasks.named('ideStressTest')");

        assertContains(build, "systemProperty 'algocraft.ideSmokeTest', 'true'");
        assertContains(build, "systemProperty 'algocraft.ideSmokeQuestionBank'");
        assertContains(build, "systemProperty 'algocraft.ideSmokeOutputDir'");
        assertContains(build, "systemProperty 'algocraft.codeExecutor.forceEcjCompiler', 'true'");
        assertContains(build, "systemProperty 'algocraft.ideSmokeProblemId', '7'");
        assertContains(build, "systemProperty 'algocraft.ideSmokeProblemIds', '7,1,73,95,500'");
        assertContains(build, "systemProperty 'algocraft.ideSmokeStressPasses', '10'");

        assertContains(build, "IDE client smoke test only reported");
        assertContains(build, "IDE client stress test only ran");
        assertContains(build, "IDE client stress test only reported");
        assertContains(build, "IDE client stress test did not cover representative problems");
        assertContains(build, "def expectedProblemIds = ['7', '1', '73', '95', '500']");
        assertContains(build, "IDE client stress test did not capture representative problem p${id}");
        assertContains(build, "ideClientStressRequiredCheckFragments.each");
        assertContains(build, "Representative problem p1 selected in the real IDE");
        assertContains(build, "sample-p1 no-diagram problem statement has visible text pixels");
        assertContains(build, "sample-p1 no-diagram problem statement is not a flat background");
        assertContains(build, "Representative problem p73 selected in the real IDE");
        assertContains(build, "sample-p73 statement widget loaded at least one diagram image block");
        assertContains(build, "sample-p73 problem panel has enough color detail to prove an image rendered");
        assertContains(build, "sample-p73 problem panel is not just the dark placeholder/background");
        assertContains(build, "Representative problem p95 selected in the real IDE");
        assertContains(build, "sample-p95 statement widget loaded at least one diagram image block");
        assertContains(build, "sample-p95 problem panel has enough color detail to prove an image rendered");
        assertContains(build, "sample-p95 problem panel is not just the dark placeholder/background");
        assertContains(build, "Representative problem p500 selected in the real IDE");
        assertContains(build, "sample-p500 statement widget loaded at least one diagram image block");
        assertContains(build, "sample-p500 problem panel has enough color detail to prove an image rendered");
        assertContains(build, "sample-p500 problem panel is not just the dark placeholder/background");
        assertContains(build, "Representative sample p1 Run passes examples");
        assertContains(build, "Representative sample p1 Submit passes every official test case");
        assertContains(build, "Representative sample p73 Run passes examples");
        assertContains(build, "Representative sample p73 Submit passes every official test case");
        assertContains(build, "Representative sample p95 Run passes examples");
        assertContains(build, "Representative sample p95 Submit passes every official test case");
        assertContains(build, "Representative sample p500 Run passes examples");
        assertContains(build, "Representative sample p500 Submit passes every official test case");
        assertContains(build, "Import local invalid file reports translated failure status");
        assertContains(build, "Import local invalid file is not left in the user repository");
        assertContains(build, "Import local invalid file does not publish a user-prefixed problem");
        assertContains(build, "Import local invalid file preserves the current ProblemManager cache");
        assertContains(build, "Import local submit refreshes ProblemManager with the user-prefixed problem");
        assertContains(build, "requireIdeClientCaptureEvidence(result, resultPath, 'IDE client smoke test', ideSmokeOutputDir)");
        assertContains(build, "requireIdeClientCaptureEvidence(result, resultPath, 'IDE client stress test', ideStressOutputDir)");
    }

    @Test
    void ideClientSmokeSeedsAndUsesTheRealOfficialProblemManagerCache() throws IOException {
        String smoke = read("src/main/java/com/crabmods/algocraft/client/test/IdeClientSmokeTest.java");

        assertContains(smoke, "seedOfficialRepositoryCache()");
        assertContains(smoke, "copyOfficialManifestFile(sourceRoot, staging, \"manifest.json\")");
        assertContains(smoke, "normalizeJsonForManifestFixture(source)");
        assertContains(smoke, ".replace(\"\\r\\n\", \"\\n\")");
        assertContains(smoke, "cleanSmokeLocalImportFixtures(gameDir)");
        assertContains(smoke, "writeInvalidLocalImportProblemFixture()");
        assertContains(smoke, "Import local invalid file is not left in the user repository");
        assertContains(smoke, "ProblemManager.getProblem(problemId)");
        assertContains(smoke, "loaded through ProblemManager official cache");
        assertContains(smoke, "uses the game official cache asset base, not direct question_bank injection");
        assertContains(smoke, "IDE smoke loaded official cached problems through ProblemManager before screen interaction");
    }

    @Test
    void localFileImportRollsBackBrokenProblemsInsteadOfPoisoningTheUserRepository() throws IOException {
        String importScreen = read("src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java");

        assertContains(importScreen, "Path backup = null");
        assertContains(importScreen, "Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING)");
        assertContains(importScreen, "rollbackLocalImportTarget(target, backup, hadExistingTarget, e)");
        assertContains(importScreen, "Files.deleteIfExists(target)");
        assertContains(importScreen, "ProblemManager.refreshAll()");
        assertContains(importScreen, "original.addSuppressed(rollbackError)");
    }

    @Test
    void buildWorkflowRunsCompileJudgeGameTestAndRealIdeAcceptanceWithoutSoftFailure() throws IOException {
        String workflow = read(".github/workflows/build.yml");

        assertContains(workflow, "java-version: '21'");
        assertContains(workflow, "run: ./mod-build.ps1 build --no-daemon");
        assertContains(workflow, "ide-client-acceptance:");
        assertFalse(workflow.contains("needs: build"),
                "native CI must run independently so a build failure cannot hide client defects");
        assertContains(workflow, "runs-on: windows-latest");
        assertContains(workflow, "run: ./mod-build.ps1 gradle ideStressTest --no-daemon");
        assertContains(workflow, "GALLIUM_DRIVER=llvmpipe");
        assertContains(workflow, "LP_NUM_THREADS=2");
        assertContains(workflow, "mingw-w64-ucrt-x86_64-mesa");
        assertContains(workflow, "RUNNER_ENVIRONMENT");
        assertContains(workflow, "build/test-results/");
        assertContains(workflow, "build/algocraft-ide-smoke/");
        assertContains(workflow, "build/algocraft-ide-stress/");
        assertFalse(workflow.contains("continue-on-error: true"),
                "core AlgoCraft build and IDE acceptance gates must not be soft-failed in CI");
    }

    @Test
    void officialQuestionBankWorkflowRunsFullShardedBankGate() throws IOException {
        String workflow = read(".github/workflows/test-solutions.yml");

        assertContains(workflow, "question_bank/official/**");
        assertContains(workflow, "java-version: '21'");
        assertContains(workflow, "run: ./mod-build.ps1 gradle officialQuestionBankTest --no-build-cache");
        assertContains(workflow, "build/test-results/officialQuestionBankTest*/TEST-*.xml");
        assertFalse(workflow.contains("continue-on-error: true"),
                "official question-bank correctness must not be soft-failed in CI");
    }

    @Test
    void manifestValidationNeverMutatesTheReviewedBank() throws IOException {
        String updateManifestWorkflow = read(".github/workflows/update-manifest.yml");
        String solutionWorkflow = read(".github/workflows/test-solutions.yml");

        assertContains(updateManifestWorkflow, "python generate_manifest.py");
        assertContains(updateManifestWorkflow, "--validate-only");
        assertContains(updateManifestWorkflow, "contents: read");
        assertFalse(updateManifestWorkflow.contains("git push"));
        assertContains(solutionWorkflow, "question_bank/official/**");
        assertContains(solutionWorkflow, "run: ./mod-build.ps1 gradle officialQuestionBankTest --no-build-cache");
    }

    @Test
    void officialJudgeGateRunsEveryReferenceSolutionNotOnlyTheFirstOne() throws IOException {
        String judgeTest = read("src/test/java/com/crabmods/algocraft/logic/JudgeOfficialAllReferenceTest.java");

        assertContains(judgeTest, "judgeAcceptsEveryOfficialReferenceSolution");
        assertContains(judgeTest, "solutionArguments");
        assertContains(judgeTest, "IntStream.range(0, solutions.size())");
        assertContains(judgeTest, "json.getAsJsonArray(\"solutions\").get(solutionIndex)");
        assertContains(judgeTest, "Judge.gradeForTest(problem, code, CodeExecutor::executeBatchInProcess)");
        assertContains(judgeTest, "algocraft.officialJudgeIsolatedProcess");
        assertContains(judgeTest, "algocraft.officialJudgeAllTests");
        assertContains(judgeTest, "problem.setTests(List.of())");
        assertFalse(judgeTest.contains("json.getAsJsonArray(\"solutions\").get(0)"),
                "official Judge gate must not silently collapse back to checking only the first reference solution");
    }

    @Test
    void officialSolutionGateDoesNotKeepManualSkipRunner() throws IOException {
        assertFalse(Files.exists(PROJECT_ROOT.resolve(
                        "src/test/java/com/crabmods/algocraft/solutions/SolutionTestRunner.java")),
                "legacy SolutionTestRunner silently skipped design/unsupported problems; use testengine.SolutionTestSuite");

        String testSources = readAllJavaSourcesUnder("src/test/java/com/crabmods/algocraft");
        String manualTestingMarker = "Design class problems require manual " + "testing";
        assertFalse(testSources.contains(manualTestingMarker),
                "official solution coverage must not defer any problem category to manual review");
        assertFalse(testSources.contains("Unsupported problem " + "type"),
                "official solution coverage must classify unsupported problems as failures instead of skips");
        assertFalse(testSources.contains("skipped" + "Problems"),
                "official solution coverage must not keep a skipped-problem summary");
        assertFalse(testSources.contains("total" + "Skipped"),
                "official solution coverage must not count skipped tests as part of the gate");
    }

    @Test
    void officialSolutionSuiteFailsFastInsteadOfSilentlySkippingBrokenShards() throws IOException {
        String suite = read("src/test/java/com/crabmods/algocraft/testengine/SolutionTestSuite.java");
        String providerContract = read("src/test/java/com/crabmods/algocraft/testengine/SolutionTestSuiteProviderContractTest.java");

        assertContains(suite, "Official question bank directory is missing");
        assertContains(suite, "No official problem files found in configured range");
        assertContains(suite, "produced no executable solution tests");
        assertContains(suite, "has no executable solutions");
        assertContains(suite, "has no executable tests");
        assertContains(providerContract, "providerFailsWhenOfficialQuestionBankDirectoryIsMissing");
        assertContains(providerContract, "providerFailsWhenConfiguredShardHasNoProblems");
        assertContains(providerContract, "providerFailsWhenProblemWouldProduceNoSolutionCoverage");
        assertFalse(suite.contains("return Stream.empty()"),
                "official solution coverage must fail loudly instead of returning an empty parameterized suite");
    }

    @Test
    void playerAndOfficialSolutionEntrypointSelectionIsDeterministicAndInputAware() throws IOException {
        String selector = read("src/main/java/com/crabmods/algocraft/logic/SolutionMethodSelector.java");
        String codeExecutor = read("src/main/java/com/crabmods/algocraft/logic/CodeExecutor.java");
        String modernScreen = read("src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java");
        String webServer = read("src/main/java/com/crabmods/algocraft/web/AlgoCraftWebServer.java");
        String standardAdapter = read("src/test/java/com/crabmods/algocraft/testengine/adapters/StandardAdapter.java");
        String codeExecutorTest = read("src/test/java/com/crabmods/algocraft/logic/CodeExecutorContractTest.java");
        String standardAdapterTest = read("src/test/java/com/crabmods/algocraft/testengine/adapters/StandardAdapterOutputSemanticsTest.java");

        assertContains(selector, "argumentShapeMatches(method, namedArguments)");
        assertContains(selector, "HELPER_METHOD_NAMES");
        assertContains(selector, "preferredMethodNames.contains(method.getName())");
        assertContains(codeExecutor, "findSolutionMethod(clazz, input)");
        assertContains(codeExecutor, "preferredMethodNameFromInitialCode");
        assertContains(codeExecutor, "preferredMethodName(testCases)");
        assertContains(codeExecutor, "\"public\".equals(visibility)");
        assertContains(codeExecutor, "!\"private\".equals(visibility)");
        assertContains(codeExecutor, "String firstTestInput = testCases.isEmpty() ? \"\" : testCases.getFirst().input");
        assertContains(codeExecutor, "findSolutionMethod(clazz, firstTestInput, preferredMethodName(testCases))");
        assertContains(codeExecutor, "SolutionMethodSelector.select(clazz, input");
        assertContains(modernScreen, "CodeExecutor.preferredMethodNameFromInitialCode(currentProblem.getInitialCode())");
        assertContains(webServer, "CodeExecutor.preferredMethodNameFromInitialCode(problem.getInitialCode())");
        assertContains(standardAdapter, "findSolutionMethod(solutionClass, input)");
        assertContains(standardAdapter, "SolutionMethodSelector.select(clazz, input");
        assertContains(codeExecutorTest, "executePrefersInputCompatibleEntrypointOverPublicHelperMethod");
        assertContains(codeExecutorTest, "executeBatchPrefersProblemTemplateEntrypointOverSameShapeHelperMethod");
        assertContains(codeExecutorTest, "preferredEntrypointExtractionSkipsPrivateTemplateHelperMethods");
        assertContains(standardAdapterTest, "prefersInputCompatibleEntrypointOverPublicHelperMethod");
    }

    @Test
    void judgeFailsClosedOnMalformedProblemTestCasesInsteadOfSkippingThem() throws IOException {
        String judge = read("src/main/java/com/crabmods/algocraft/logic/Judge.java");
        String judgeContract = read("src/test/java/com/crabmods/algocraft/logic/JudgeContractTest.java");

        assertContains(judge, "Invalid problem test case");
        assertContains(judge, "invalidProblemTestCase(result, problem");
        assertFalse(judge.contains("Skipping invalid example " + "test case"),
                "Judge.grade must not skip malformed visible tests and then accept on the remaining subset");
        assertFalse(judge.contains("Skipping invalid hidden " + "test case"),
                "Judge.grade must not skip malformed hidden tests and then accept on the remaining subset");
        assertContains(judgeContract, "judgeRejectsInvalidExampleTestCaseInsteadOfSkippingIt");
        assertContains(judgeContract, "judgeRejectsInvalidHiddenTestCaseInsteadOfSkippingIt");
        assertContains(judgeContract, "Judge must not execute code when an example test case is malformed");
        assertContains(judgeContract, "Judge must not execute code when a hidden test case is malformed");
    }

    @Test
    void progressPersistenceFailsClosedOnCorruptDiskAndInvalidSyncPackets() throws IOException {
        String progressManager = read("src/main/java/com/crabmods/algocraft/logic/ProgressManager.java");
        String progressTest = read("src/test/java/com/crabmods/algocraft/logic/ProgressManagerTest.java");

        assertContains(progressManager, "Corrupted progress file, starting fresh");
        assertContains(progressManager, "sanitizedProgressSnapshot(serverProgress)");
        assertContains(progressManager, "Ignoring invalid progress sync packet");
        assertContains(progressManager, "writeProgressAtomically");
        assertContains(progressManager, "StandardCopyOption.ATOMIC_MOVE");
        assertContains(progressManager, "saveGeneration.incrementAndGet()");
        assertContains(progressTest, "corruptedProgressFileDoesNotCrashProgressQueries");
        assertContains(progressTest, "invalidServerProgressSyncDoesNotClearExistingProgress");
        assertContains(progressTest, "validServerProgressSyncReplacesSnapshotAndPersists");
        assertContains(progressTest, "invalid sync packets must not clear the player's previously solved problems");
        assertFalse(progressManager.contains("passedProblems.putAll(serverProgress)"),
                "server progress sync must validate the full payload before mutating local progress");
    }

    @Test
    void submissionHistoryPersistenceOnlyPublishesRenderableAtomicSnapshots() throws IOException {
        String historyManager = read("src/main/java/com/crabmods/algocraft/logic/SubmissionHistoryManager.java");
        String historyTest = read("src/test/java/com/crabmods/algocraft/logic/SubmissionHistoryManagerTest.java");

        assertContains(historyManager, "sanitizeRecord(record)");
        assertContains(historyManager, "boundedText(record.getProblemTitle(), \"Unknown\")");
        assertContains(historyManager, "writeHistoryAtomically");
        assertContains(historyManager, "StandardCopyOption.ATOMIC_MOVE");
        assertContains(historyManager, "Files.writeString(temp, gson.toJson(snapshot), StandardCharsets.UTF_8)");
        assertContains(historyTest, "corruptedHistoryFileDoesNotCrashHistoryQueries");
        assertContains(historyTest, "loadedHistoryDropsNullAndInvalidRecordsBeforeUiCanRenderThem");
        assertContains(historyTest, "savedHistorySanitizesNullBlankAndOversizedTextBeforePersistence");
        assertContains(historyTest, "history row text should stay bounded before the game UI measures it");
        assertFalse(historyManager.contains("history.addAll(loaded)"),
                "submission history must sanitize loaded disk records before the game UI consumes them");
        assertFalse(historyManager.contains("new FileWriter(historyFile)"),
                "submission history saves must not use direct non-atomic file writes");
    }

    @Test
    void randomizedDesignClassJudgingChecksCurrentCollectionMembershipInsteadOfSkipping() throws IOException {
        String codeExecutor = read("src/main/java/com/crabmods/algocraft/logic/CodeExecutor.java");
        String designAdapter = read("src/test/java/com/crabmods/algocraft/testengine/adapters/DesignClassAdapter.java");
        String codeExecutorTest = read("src/test/java/com/crabmods/algocraft/logic/CodeExecutorContractTest.java");
        String adapterTest = read("src/test/java/com/crabmods/algocraft/testengine/adapters/DesignClassAdapterRandomizedTest.java");

        assertContains(codeExecutor, "randomizedDesignOutputsMatch(methodNames, allArgs, actualResults");
        assertContains(codeExecutor, "RandomizedDesignState.fromConstructor(methodNames)");
        assertContains(codeExecutor, "counts.getOrDefault(value, 0) > 0");
        assertContains(designAdapter, "compareRandomizedDesignOutput(actual, expected, methodNames, allArgs");
        assertContains(designAdapter, "RandomizedDesignState.fromConstructor(methodNames)");
        assertContains(designAdapter, "counts.getOrDefault(value, 0) > 0");
        assertFalse(designAdapter.contains("NON_DETERMINISTIC_METHODS"),
                "official design-class adapter must not keep a skip list for random-looking method names");
        assertFalse(designAdapter.contains("contains(methodName)) continue"),
                "official design-class adapter must validate randomized outputs instead of skipping them");
        assertContains(codeExecutorTest, "randomizedSetGetRandomMustReturnCurrentMemberInsteadOfAnyNonNullValue");
        assertContains(adapterTest, "randomizedSetGetRandomRejectsValuesOutsideTheCurrentSet");
        assertContains(adapterTest, "randomizedCollectionGetRandomAcceptsAnyCurrentMultisetMember");
    }

    @Test
    void codeExecutorSandboxClassLoaderRemainsADefenseInDepthGateForRestrictedJdkApis() throws IOException {
        String sandboxLoader = read("src/main/java/com/crabmods/algocraft/logic/SandboxClassLoader.java");
        String securityTest = read("src/test/java/com/crabmods/algocraft/logic/CodeExecutorSecurityTest.java");

        assertContains(sandboxLoader, "java.lang.ProcessHandle");
        assertContains(sandboxLoader, "java.lang.Runtime");
        assertContains(sandboxLoader, "java.lang.StackTraceElement");
        assertContains(sandboxLoader, "java.lang.System$Logger");
        assertContains(sandboxLoader, "java.lang.reflect.Method");
        assertContains(sandboxLoader, "java.lang.invoke.MethodHandle");
        assertContains(sandboxLoader, "java.util.ServiceLoader");
        assertContains(sandboxLoader, "java.util.ResourceBundle");
        assertContains(sandboxLoader, "packageName.startsWith(\"java.security\")");
        assertContains(sandboxLoader, "packageName.startsWith(\"sun.\")");
        assertContains(sandboxLoader, "packageName.startsWith(\"jdk.\")");
        assertContains(sandboxLoader, "packageName.startsWith(\"com.sun.\")");
        assertContains(sandboxLoader, "return false;");
        assertContains(securityTest, "sandboxClassLoaderRejectsNetworkFileProcessManagementAndUiApisEvenWithoutImports");
        assertContains(securityTest, "sandboxClassLoaderRejectsSecurityAndInternalJdkApisEvenWithoutImports");
        assertContains(securityTest, "executeRejectsThrowableStackTraceDiscoveryBeforeCompilation");
        assertContains(securityTest, "executeRejectsThrowablePrintStackTraceBeforeCompilation");
        assertContains(securityTest, "sandboxClassLoaderRejectsStackTraceElementEvenWithoutImports");
        assertContains(securityTest, "executeRejectsSystemLoggerBeforeCompilation");
        assertContains(securityTest, "sandboxClassLoaderRejectsSystemLoggerApisEvenWithoutImports");
        assertContains(securityTest, "java.net.Socket");
        assertContains(securityTest, "java.io.File");
        assertContains(securityTest, "java.nio.file.Path");
        assertContains(securityTest, "java.lang.management.ManagementFactory");
        assertContains(securityTest, "javax.management.MBeanServerFactory");
        assertContains(securityTest, "java.awt.Desktop");
        assertContains(securityTest, "javax.swing.JOptionPane");
        assertContains(securityTest, "java.beans.Expression");
        assertContains(securityTest, "executeRejectsSecurityPackageBeforeCompilation");
        assertContains(securityTest, "executeRejectsBroadJavaxPackageBeforeCompilation");
        assertContains(securityTest, "executeRejectsInternalJdkAndSunPackagesBeforeCompilation");
        assertContains(securityTest, "executeRejectsArchiveAndCalendarPackagesBeforeCompilation");
        assertContains(securityTest, "executeRejectsJavaLangForeignBeforeCompilation");
    }

    @Test
    void problemManagerRefreshAllPropagatesRepositoryFailuresInsteadOfSoftFailing() throws IOException {
        String manager = read("src/main/java/com/crabmods/algocraft/logic/ProblemManager.java");
        String cacheTest = read("src/test/java/com/crabmods/algocraft/logic/ProblemManagerCacheTest.java");

        assertContains(manager, "List<RepositoryRefreshFailure> failures");
        assertContains(manager, "recordRefreshFailure(repo, e, failures)");
        assertContains(manager, "throw new CompletionException(combineRefreshFailures(failures))");
        assertContains(cacheTest, "refreshAllAsyncPropagatesFailuresWithoutPublishingPartialCache");
        assertContains(cacheTest, "a failed aggregate refresh must not publish problems from the broken repository");
        assertFalse(manager.contains("refresh().exceptionally"),
                "ProblemManager.refreshAllAsync must not convert repository refresh failures into successful refreshes");
    }

    @Test
    void problemManagerNewRepositoriesPublishOnlyAfterFirstRefreshSucceeds() throws IOException {
        String manager = read("src/main/java/com/crabmods/algocraft/logic/ProblemManager.java");
        String cacheTest = read("src/test/java/com/crabmods/algocraft/logic/ProblemManagerCacheTest.java");

        assertContains(manager, "refreshNewRepositoryAndPublish");
        assertContains(manager, "return refreshNewRepositoryAndPublish(repo)");
        assertContains(manager, "refreshRepository(repo).thenRun(() ->");
        assertContains(manager, "repositories.add(repo)");
        assertContains(cacheTest, "a new repository must not be selectable before its first refresh is validated");
        assertContains(cacheTest, "a pending addRepository refresh must not expose an unvalidated repository");
        assertContains(cacheTest, "a repository whose first refresh failed must stay out of the visible repository list");
        assertFalse(manager.contains("repositories.add(repo);\n            sortRepositoriesByPriority();\n        }\n        return refreshRepository"),
                "ProblemManager.addRepository must not publish a new repository before the first refresh finishes");
    }

    @Test
    void problemManagerOfficialRefreshFailsClosedWhenOfficialRepositoryIsUnavailable() throws IOException {
        String manager = read("src/main/java/com/crabmods/algocraft/logic/ProblemManager.java");
        String cacheTest = read("src/test/java/com/crabmods/algocraft/logic/ProblemManagerCacheTest.java");

        assertContains(manager, "CompletableFuture.failedFuture(new IllegalStateException(\"ProblemManager is not initialized\"))");
        assertContains(manager, "CompletableFuture.failedFuture(new IllegalStateException(\"Official repository is not initialized\"))");
        assertContains(cacheTest, "forceRefreshOfficialFailsClosedWhenOfficialRepositoryIsMissing");
        assertContains(cacheTest, "Update Official must not report success before ProblemManager is initialized");
        assertContains(cacheTest, "Update Official must not report success when the official repository is unavailable");
        assertFalse(manager.contains("if (officialRepository == null) return CompletableFuture.completedFuture(null)"),
                "Update Official must not silently report success when no official repository is wired");
    }

    @Test
    void localProblemRepositoryPublishesAtomicProblemSnapshots() throws IOException {
        String repository = read("src/main/java/com/crabmods/algocraft/logic/repo/LocalProblemRepository.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/LocalProblemRepositoryTest.java");

        assertContains(repository, "AtomicReference<List<Problem>> problems");
        assertContains(repository, "List<Problem> loaded = new ArrayList<>()");
        assertContains(repository, "problems.set(List.copyOf(loaded))");
        assertContains(repository, "throw new CompletionException(e)");
        assertContains(repository, "Repository directory is missing after a successful load");
        assertContains(repository, "Invalid local problem JSON");
        assertContains(repository, "Failed to load translations from");
        assertContains(repository, "throw e;");
        assertContains(repositoryTest, "refreshFailureKeepsPreviouslyLoadedProblemSnapshot");
        assertContains(repositoryTest, "missingRepositoryDirectoryAfterSuccessfulRefreshKeepsPreviouslyLoadedProblemSnapshot");
        assertContains(repositoryTest, "damagedLocalProblemDoesNotPublishPartialCustomProblemSnapshot");
        assertContains(repositoryTest, "damagedCustomRepositoryTranslationsFailRefreshAndPreservePreviousSnapshots");
        assertContains(repositoryTest, "failed custom refresh should keep the last complete problem snapshot");
        assertContains(repositoryTest, "failed translation refresh should preserve the last complete custom translation snapshot");
        assertFalse(repository.contains("problems.clear()"),
                "local repository refresh must not expose an empty live problem list while loading");
        assertFalse(repository.contains("LOGGER.log(System.Logger.Level.DEBUG, \"Failed to load translations"),
                "local repository refresh must not hide damaged translation loads");
        assertFalse(repository.contains("return null;"),
                "local repository refresh must not silently skip malformed problem files");
    }

    @Test
    void officialRepositoryCacheRefreshFailsLoudlyInsteadOfPublishingPartialSnapshots() throws IOException {
        String repository = read("src/main/java/com/crabmods/algocraft/logic/repo/OfficialRepository.java");
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(repository, "AtomicReference<List<Problem>> problems");
        assertContains(repository, "problems.set(List.copyOf(loaded))");
        assertContains(repository, "RemoteRepositoryDownloader.validateCachedOfficialRepository(cacheDir)");
        assertContains(repository, "Invalid official cached problem JSON");
        assertContains(repository, "throw new CompletionException(e)");
        assertContains(repository, "Failed to load official translations");
        assertContains(downloader, "static void validateCachedOfficialRepository(Path repositoryDir)");
        assertContains(downloader, "validateRepositorySemanticContent(");
        assertContains(downloader, "validateNoExtraCachedRepositoryFiles(normalizedRepositoryDir, manifestNames)");
        assertContains(downloader, "Cached official repository is incomplete: missing manifest.json");
        assertContains(downloader, "Cached official repository contains unmanifested file");
        assertContains(repositoryTest, "productionOfficialRefreshAllowsEmptyCacheBeforeTheFirstRemoteUpdate");
        assertContains(repositoryTest, "productionOfficialRefreshRejectsIncompleteCachedRepositoryInsteadOfPublishingPartialProblems");
        assertContains(repositoryTest, "productionOfficialRefreshRejectsCachedProblemFilesNotListedByManifest");
        assertContains(repositoryTest, "damagedCachedProblemDoesNotPublishPartialOfficialProblemSnapshot");
        assertContains(repositoryTest, "damagedCachedOfficialTranslationsFailRefreshAndPreservePreviousTranslationSnapshot");
        assertContains(repositoryTest, "an incomplete official cache must not be published as a usable problem list");
        assertContains(repositoryTest, "unmanifested cached problem files must not leak into the visible official problem list");
        assertContains(repositoryTest, "failed cache reload should keep the last complete official problem snapshot");
        assertContains(repositoryTest, "failed official updates must keep the last complete official translation snapshot");
        assertContains(repositoryTest, "failed translation refresh should preserve the last complete official translation snapshot");
        assertFalse(repository.contains("problems.clear()"),
                "official cache refresh must not clear the live problem list before a full load succeeds");
        assertFalse(repository.contains("loadProblem(file, loaded)"),
                "official cache refresh must not silently skip malformed cached problem files");
    }

    @Test
    void repositoryManagerDoesNotPublishDownloadedRepositoriesUntilContentValidationPasses() throws IOException {
        String manager = read("src/main/java/com/crabmods/algocraft/logic/repo/RepositoryManager.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/RepositoryManagerTest.java");

        assertContains(manager, "RemoteRepositoryDownloader.update(candidate.url, getRepositoryDir(candidate).toPath(), client)");
        assertContains(manager, "upsertRepositoryLocked(candidate)");
        assertContains(manager, "saveConfigLocked()");
        assertContains(repositoryTest, "failedInstallAfterDownloadedContentValidationDoesNotPersistMetadataOrCache");
        assertContains(repositoryTest, "failedExistingUpdateAfterContentValidationKeepsPreviousMetadataAndCache");
        assertContains(repositoryTest, "repositories.json should not be created after content validation failure");
        assertContains(repositoryTest, "failed content validation must preserve the previous usable repository cache");
        assertContains(repositoryTest, "replaceProblemBody");
    }

    @Test
    void productionOfficialRefreshRequiresExactlyFiveHundredProblemsWithoutConstrainingCustomRepositories()
            throws IOException {
        String officialRepository = read("src/main/java/com/crabmods/algocraft/logic/repo/OfficialRepository.java");
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String manager = read("src/main/java/com/crabmods/algocraft/logic/repo/RepositoryManager.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(downloader, "static final int OFFICIAL_PROBLEM_COUNT = 500");
        assertContains(downloader, "static void updateOfficial");
        assertContains(downloader, "update(repositoryUrl, targetDir, client, OFFICIAL_PROBLEM_COUNT)");
        assertContains(downloader, "validateManifest(manifest, expectedProblemCount)");
        assertContains(downloader, "totalProblems must be exactly");
        assertContains(officialRepository, "this(cacheDir, DEFAULT_REPOSITORY_URL, new HttpRemoteFileClient(), true)");
        assertContains(officialRepository, "RemoteRepositoryDownloader.updateOfficial(repositoryUrl, cacheDir, remoteFileClient)");
        assertContains(officialRepository, "RemoteRepositoryDownloader.update(repositoryUrl, cacheDir, remoteFileClient)");
        assertContains(manager, "new OfficialRepository(officialCacheDir)");
        assertContains(manager, "RemoteRepositoryDownloader.update(candidate.url, getRepositoryDir(candidate).toPath(), client)");
        assertContains(repositoryTest,
                "forceRefreshRejectsOfficialRepositoryManifestThatIsNotExactlyFiveHundredProblemsWithoutReplacingCache");
    }

    @Test
    void remoteOfficialChineseTranslationsRejectKnownEnglishResidualsBeforeCaching() throws IOException {
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(downloader, "KNOWN_UNTRANSLATED_ENGLISH");
        assertContains(downloader, "contains untranslated English phrase");
        assertContains(downloader, "is in `wordList`");
        assertContains(repositoryTest, "forceRefreshRejectsKnownEnglishResidualChineseTranslationWithoutReplacingCache");
        assertContains(repositoryTest, "`s1` is in `wordList`");
        assertContains(repositoryTest, "failure should identify zh_cn English residuals before cache replacement");
    }

    @Test
    void remoteOfficialRepositoryRejectsDuplicateProblemTitlesBeforeCaching() throws IOException {
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(downloader, "validateUniqueProblemTitles(downloadedProblems)");
        assertContains(downloader, "normalizeProblemTitle(title)");
        assertContains(downloader, "duplicate problem title");
        assertContains(repositoryTest, "forceRefreshRejectsDuplicateProblemTitlesWithoutReplacingCache");
        assertContains(repositoryTest, "Duplicate   Remote   Title");
        assertContains(repositoryTest, "failure should identify duplicate remote problem titles before cache replacement");
    }

    @Test
    void remoteOfficialRepositoryRejectsLegacyAndMalformedDiagramMetadataBeforeCaching() throws IOException {
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(downloader, "validateProblemDiagrams(name, problemJson)");
        assertContains(downloader, "legacy images field is not allowed; use diagrams");
        assertContains(downloader, "legacy diagram field is not allowed; use diagrams");
        assertContains(downloader, "diagrams should be omitted instead of empty");
        assertContains(downloader, "diagrams must not repeat id");
        assertContains(downloader, "diagrams must not repeat file");
        assertContains(downloader, "promptImageProblemNumber(file)");
        assertContains(downloader, "prompt image file must belong to p");
        assertContains(repositoryTest, "forceRefreshRejectsLegacyPromptImageFieldsWithoutReplacingCache");
        assertContains(repositoryTest, "forceRefreshRejectsMalformedStructuredDiagramsWithoutReplacingCache");
        assertContains(repositoryTest, "forceRefreshRejectsPromptImageFromDifferentProblemWithoutReplacingCache");
        assertContains(repositoryTest, "failure should reject legacy images fields before cache replacement");
        assertContains(repositoryTest, "failure should reject duplicate diagram files before cache replacement");
        assertContains(repositoryTest, "failure should identify prompt images that belong to a different problem");
    }

    @Test
    void remoteOfficialRepositoryRejectsUnreadablePromptImagesBeforeCaching() throws IOException {
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(downloader, "MIN_PROMPT_IMAGE_WIDTH = 256");
        assertContains(downloader, "MIN_PROMPT_IMAGE_HEIGHT = 144");
        assertContains(downloader, "MIN_PROMPT_IMAGE_UNIQUE_COLORS = 24");
        assertContains(downloader, "MIN_PROMPT_IMAGE_LUMINANCE_RANGE = 80");
        assertContains(downloader, "MIN_PROMPT_IMAGE_OPAQUE_PIXEL_RATIO = 0.95");
        assertContains(downloader, "PromptImageStats.analyze(image)");
        assertContains(downloader, "prompt image must not be blank or flat");
        assertContains(downloader, "prompt image needs enough contrast for Minecraft GUI readability");
        assertContains(repositoryTest, "forceRefreshRejectsFlatPromptImageWithoutReplacingCache");
        assertContains(repositoryTest, "failure should reject visually flat prompt images before cache replacement");
    }

    @Test
    void remoteOfficialCatalogRejectsTagsDuplicatedAcrossTracksBeforeCaching() throws IOException {
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(downloader, "duplicate tag across tracks");
        assertContains(downloader, "if (!tags.add(tag))");
        assertContains(repositoryTest, "forceRefreshRejectsCatalogTagsDuplicatedAcrossTracksWithoutReplacingCache");
        assertContains(repositoryTest, "failure should identify ambiguous catalog tags before cache replacement");
    }

    @Test
    void remoteOfficialCatalogRejectsEmptyTracksUnusedTagsAndRepeatedProblemTagsBeforeCaching() throws IOException {
        String downloader = read("src/main/java/com/crabmods/algocraft/logic/repo/RemoteRepositoryDownloader.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/OfficialRepositoryTest.java");

        assertContains(downloader, "catalog tracks must not be empty");
        assertContains(downloader, "catalog tags must all be used by problems");
        assertContains(downloader, "tags must not repeat catalog tag");
        assertContains(downloader, "trackProblemCounts");
        assertContains(downloader, "tagProblemCounts");
        assertContains(repositoryTest, "forceRefreshRejectsCatalogTagsUnusedByProblemsWithoutReplacingCache");
        assertContains(repositoryTest, "forceRefreshRejectsCatalogTracksWithoutProblemsWithoutReplacingCache");
        assertContains(repositoryTest, "forceRefreshRejectsProblemTagsRepeatedWithinOneProblemWithoutReplacingCache");
    }

    @Test
    void problemTranslationManagerPreservesCacheOnDamagedTranslationLoads() throws IOException {
        String translationManager = read("src/main/java/com/crabmods/algocraft/logic/ProblemTranslationManager.java");
        String repositoryTest = read("src/test/java/com/crabmods/algocraft/logic/repo/LocalProblemRepositoryTest.java");

        assertContains(translationManager, "Translation path is not a directory");
        assertContains(translationManager, "loadTranslationFile(path, problemIdPrefix, translations)");
        assertContains(translationManager, "throw new CompletionException(error)");
        assertContains(repositoryTest, "damagedTranslationDirectoryDoesNotClearPreviouslyLoadedTranslations");
        assertContains(repositoryTest, "failed translation refresh should preserve the last complete translation snapshot");
    }

    @Test
    void ideScreenDoesNotCrashWhenNoRepositoriesAreAvailable() throws IOException {
        String screen = read("src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java");

        assertContains(screen, "if (repos.isEmpty())");
        assertContains(screen, "this.repositorySelector = null");
        assertContains(screen, "this.currentRepository = null");
        assertContains(screen, "String safeFilter = filter == null ? \"\" : filter");
        assertContains(screen, "toLowerCase(Locale.ROOT)");
        assertFalse(screen.contains("filter.toLowerCase()"),
                "IDE search filtering must not assume the responder always passes a non-null filter");
    }

    @Test
    void asyncServerJudgeGameTestsAllowRealWorkerLatencyAndReportState() throws IOException {
        String gameTests = read("src/main/java/com/crabmods/algocraft/gametest/AlgoCraftGameTests.java");

        assertContains(gameTests, "ASYNC_SUBMISSION_TIMEOUT_TICKS = 24_000");
        assertContains(gameTests, "timeoutTicks = ASYNC_SUBMISSION_TIMEOUT_TICKS");
        assertContains(gameTests, "batch = \"async_submission_success\"");
        assertContains(gameTests, "batch = \"async_submission_malformed_success\"");
        assertContains(gameTests, "batch = \"async_submission_duplicate\"");
        assertContains(gameTests, "batch = \"async_submission_global_capacity\"");
        assertContains(gameTests, "batch = \"async_submission_failure_retry\"");
        assertContains(gameTests, "async server judge should complete: \" + asyncState(future, player, startedNanos)");
        assertContains(gameTests, "server_submission_rejects_malformed_success_without_rewards");
        assertContains(gameTests, "malformed accepted judge result must be downgraded before rewards");
        assertContains(gameTests, "malformed accepted judge result must not grant reward items");
        assertContains(gameTests, "server_submission_missing_problem_counts_as_failure_without_rewards");
        assertContains(gameTests, "missing server problem should be rejected before async Judge scheduling");
        assertContains(gameTests, "missing server problem must count as a failed submission and reset the streak");
        assertContains(gameTests, "missing server problem must not grant reward items");
        assertContains(gameTests, "server_submission_async_duplicate_click_is_rejected_while_first_submission_runs");
        assertContains(gameTests, "first async submission should reserve the per-player judge slot before returning");
        assertContains(gameTests, "duplicate async submission should be rejected immediately while the first judge is in flight");
        assertContains(gameTests, "duplicate rejection plus first success should increment total solved exactly once");
        assertContains(gameTests, "server_submission_async_global_capacity_rejects_extra_player_and_recovers");
        assertContains(gameTests, "submitAsyncVerifiedProblemWithJudgeForGameTest");
        assertContains(gameTests, "controlled saturating submissions should release global capacity");
        assertContains(gameTests, "fifth async submission should be rejected immediately while global judge capacity is full");
        assertContains(gameTests, "global-capacity rejection must release the rejected player's per-player judge slot");
        assertContains(gameTests, "normal submission should pass after global capacity is released");
        assertContains(gameTests, "global-capacity rejection plus recovery should increment total solved exactly once");
        assertContains(gameTests, "failed async code should complete: \" + asyncState(first, player, startedNanos)");
        assertContains(gameTests, "elapsedMs=\" + elapsedMs");
        assertContains(gameTests, "playerInFlight=\" + PacketSubmitSolution.hasPlayerSubmissionInFlightForGameTest(player)");
        assertFalse(gameTests.contains("timeoutTicks = 400"),
                "async GameTests must not use a short tick timeout for real Java compile/Judge worker latency");
    }

    @Test
    void solvingStateGameTestCoversDuplicateStartAndExpiry() throws IOException {
        String gameTests = read("src/main/java/com/crabmods/algocraft/gametest/AlgoCraftGameTests.java");

        assertContains(gameTests, "solving_session_is_problem_bound_and_expires");
        assertContains(gameTests, "duplicate same-problem solving start must not reset the server timer");
        assertContains(gameTests, "expired solving session should be cleared from protection state");
    }

    @Test
    void solvingProtectionHandlersAreRegisteredOnTheGameEventBus() throws IOException {
        String modEntrypoint = read("src/main/java/com/crabmods/algocraft/AlgoCraft.java");
        String solvingManager = read("src/main/java/com/crabmods/algocraft/server/SolvingPlayerManager.java");

        assertContains(modEntrypoint, "NeoForge.EVENT_BUS.register(SolvingPlayerManager.class)");
        assertContains(solvingManager, "@SubscribeEvent\n    public static void onLivingAttack");
        assertContains(solvingManager, "@SubscribeEvent\n    public static void onLivingChangeTarget");
        assertContains(solvingManager, "@SubscribeEvent\n    public static void onPlayerLogout");
        assertContains(solvingManager, "@SubscribeEvent\n    public static void onPlayerChangeDimension");
        assertContains(solvingManager, "Config.PROTECT_WHILE_SOLVING.get() && isSolving(event.getEntity())");
        assertContains(solvingManager, "Config.PROTECT_WHILE_SOLVING.get() && isSolving(event.getNewAboutToBeSetTarget())");
        assertContains(solvingManager, "clearSolving(event.getEntity().getUUID())");
    }

    @Test
    void problemImagesResolveOnlyFromTheirOwnRepositoryRoot() throws IOException {
        String resolver = read("src/main/java/com/crabmods/algocraft/logic/ProblemAssetResolver.java");
        String resolverTest = read("src/test/java/com/crabmods/algocraft/logic/ProblemAssetResolverTest.java");

        assertContains(resolver, "return resolveImage(problem.getAssetBaseDir(), visual.file());");
        assertContains(resolverTest, "problemImageResolutionDoesNotFallbackToDeveloperOfficialBank");
        assertContains(resolverTest,
                "a remote/custom problem missing its own image must not silently use a same-named official image");
        assertFalse(resolver.contains("DEV_OFFICIAL_BANK"),
                "problem image resolution must not silently fall back to a developer-local official bank");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static String readAllJavaSourcesUnder(String relativeDirectory) throws IOException {
        StringBuilder sources = new StringBuilder();
        try (var stream = Files.walk(PROJECT_ROOT.resolve(relativeDirectory))) {
            for (Path source : stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                sources.append('\n').append(Files.readString(source, StandardCharsets.UTF_8));
            }
        }
        return sources.toString().replace("\r\n", "\n");
    }

    private static void assertContains(String source, String expected) {
        assertTrue(source.contains(expected), () -> "Expected to find: " + expected);
    }

    private static int countOccurrences(String source, String expected) {
        int count = 0;
        int index = source.indexOf(expected);
        while (index >= 0) {
            count++;
            index = source.indexOf(expected, index + expected.length());
        }
        return count;
    }
}
