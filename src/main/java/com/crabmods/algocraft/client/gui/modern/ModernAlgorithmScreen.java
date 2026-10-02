package com.crabmods.algocraft.client.gui.modern;

import com.crabmods.algocraft.client.ClientHooks;
import com.crabmods.algocraft.client.gui.component.CodeEditorWidget;
import com.crabmods.algocraft.client.gui.component.ProblemDisplayText;
import com.crabmods.algocraft.client.gui.component.ProblemStatementWidget;
import com.crabmods.algocraft.client.gui.component.ProblemSelectionList;
import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.CodeDraftStore;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.network.PacketSetSolvingState;
import com.crabmods.algocraft.network.PacketSubmitSolution;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class ModernAlgorithmScreen extends Screen {
    private static final int SIDEBAR_WIDTH = IdeLayoutEngine.SIDEBAR_WIDTH;
    private static final int TOP_BAR_HEIGHT = IdeLayoutEngine.TOP_BAR_HEIGHT;
    private static final int BOTTOM_BAR_HEIGHT = IdeLayoutEngine.BOTTOM_BAR_HEIGHT;
    public static final String SMOKE_SUBMIT_CONSOLE = "ALGOCraft IDE smoke submit graded locally";
    public static final String SMOKE_UPDATE_CONSOLE = "ALGOCraft IDE smoke update intercepted";
    private static volatile Supplier<CompletableFuture<Void>> officialUpdateActionForSmokeTest;
    private static volatile BiFunction<Problem, String, CompletableFuture<SubmissionResult>> submissionJudgeActionForSmokeTest;
    private static volatile BiFunction<String, List<CodeExecutor.TestCase>, CompletableFuture<List<CodeExecutor.TestResult>>> exampleActionForSmokeTest;
    private static final Object SUBMISSION_EXECUTOR_LOCK = new Object();
    private static final AtomicInteger SUBMISSION_THREAD_IDS = new AtomicInteger();
    private static ExecutorService submissionExecutor;

    private CodeEditorWidget codeEditor;
    private ProblemStatementWidget problemViewer;
    private net.minecraft.client.gui.components.EditBox searchBox;
    private ProblemSelectionList problemList;
    private CycleButton<ProblemRepository> repositorySelector;
    private Button updateOfficialButton;
    private Button submitButton;
    private Button runButton;
    private boolean runInProgress;
    private CodeDraftStore drafts;
    private int draftSaveTicks;
    private final java.util.Map<String, String> sessionDrafts = new java.util.HashMap<>();
    private final java.util.Map<String, String> problemConsoles = new java.util.HashMap<>();
    private ProblemRepository currentRepository;

    private String consoleText = "";
    private String currentCode = null;
    private Problem currentProblem;
    private boolean showingDescription = false;
    private long problemStartTime = 0;  // Track when the current problem was loaded
    private int consoleScrollY = 0;  // Console scroll position
    private int maxConsoleLines = 5;  // Max visible lines in console
    private boolean officialUpdateInProgress = false;
    private boolean submissionInProgress = false;

    // State to preserve across window resize (init() is called again)
    private String preservedCode = null;
    private String preservedProblemId = null;
    private Problem preservedProblem = null;
    private String preservedSearchText = null;
    private String preservedRepositoryName = null;
    private String preservedConsoleText = null;
    private SubmissionResult lastSmokeSubmissionResult = null;

    public ModernAlgorithmScreen() {
        super(Component.translatable("algocraft.gui.ide_title"));
        this.consoleText = Component.translatable("algocraft.gui.ready").getString();
    }

    @Override
    protected void init() {
        if (drafts == null) {
            try {
                drafts = new CodeDraftStore(this.minecraft.gameDirectory.toPath().resolve(
                        com.crabmods.algocraft.logic.catalog.ClientCatalog.serverId().isEmpty() ? "config/algocraft/code_drafts.json"
                        : "config/algocraft/server-cache/" + com.crabmods.algocraft.logic.catalog.ClientCatalog.serverId() + "/code_drafts.json"));
            } catch (java.io.IOException e) {
                System.getLogger(ModernAlgorithmScreen.class.getName()).log(System.Logger.Level.WARNING, "Could not load code drafts", e);
            }
        }
        if (this.problemViewer != null) this.problemViewer.releaseImages();
        // Save state before recreating widgets (window resize triggers init() again)
        if (this.codeEditor != null) {
            this.currentCode = this.codeEditor.getValue();
            this.preservedCode = this.currentCode;
        } else if (this.currentCode != null) {
            this.preservedCode = this.currentCode;
        }
        if (this.currentProblem != null) {
            this.preservedProblemId = this.currentProblem.getId();
            this.preservedProblem = this.currentProblem;
        }
        if (this.searchBox != null) {
            this.preservedSearchText = this.searchBox.getValue();
        }
        if (this.currentRepository != null) {
            this.preservedRepositoryName = this.currentRepository.getName();
        }
        if (this.consoleText != null && !this.consoleText.isEmpty()) {
            this.preservedConsoleText = this.consoleText;
        }

        IdeLayoutEngine.PanelLayout layout = calculateLayout();

        // Code Editor
        this.codeEditor = new CodeEditorWidget(this.font, layout.editorX(), layout.editorY(), layout.editorWidth(), layout.editorHeight(), Component.translatable("algocraft.gui.code"));
        this.codeEditor.setResponder(value -> this.currentCode = value);
        this.addRenderableWidget(this.codeEditor);

        // Read-only problem statement panel.
        this.problemViewer = new ProblemStatementWidget(this.font, layout.problemX(), layout.problemY(), layout.problemWidth(), layout.problemHeight(), Component.translatable("algocraft.gui.description"));
        this.problemViewer.visible = layout.problemVisible();
        this.codeEditor.visible = layout.editorVisible();
        this.addRenderableWidget(this.problemViewer);

        // Load Problems and restore state if available
        List<Problem> problems = (isSmokeTestEnabled() ? ProblemManager.getProblems() : com.crabmods.algocraft.logic.catalog.ClientCatalog.getProblems());
        Problem problemToSelect = null;

        // Try to restore previously selected problem
        if (this.preservedProblemId != null) {
            for (Problem p : problems) {
                if (p.getId().equals(this.preservedProblemId)) {
                    problemToSelect = p;
                    break;
                }
            }
            if (problemToSelect == null && (isSmokeTestEnabled() || !com.crabmods.algocraft.logic.catalog.ClientCatalog.connected()) && this.preservedProblem != null
                    && this.preservedProblemId.equals(this.preservedProblem.getId())) {
                problemToSelect = this.preservedProblem;
            }
        }

        // Fall back to first problem if no preserved state
        if (problemToSelect == null && !problems.isEmpty()) {
            problemToSelect = problems.get(0);
        }

        if (problemToSelect == null && com.crabmods.algocraft.logic.catalog.ClientCatalog.connected() && !isSmokeTestEnabled()) {
            this.currentProblem = null;
            this.codeEditor.setValue("");
        }
        if (problemToSelect != null) {
            // Don't reset timer if restoring the same problem
            boolean sameAsPreserved = this.preservedProblemId != null && problemToSelect.getId().equals(this.preservedProblemId);
            if (sameAsPreserved) {
                this.currentProblem = problemToSelect;
                this.problemViewer.setProblem(problemToSelect);
                // Restore preserved code instead of resetting to initial code
                String codeToRestore = this.preservedCode != null ? this.preservedCode : this.currentCode;
                if (codeToRestore != null) {
                    this.currentCode = codeToRestore;
                    this.codeEditor.setValue(codeToRestore);
                } else {
                    this.currentCode = problemToSelect.getInitialCode();
                    this.codeEditor.setValue(this.currentCode);
                }
            } else {
                selectProblem(problemToSelect);
            }
        }

        addTopButtons();

        // Search Box - restore preserved search text
        this.searchBox = new net.minecraft.client.gui.components.EditBox(this.font, 10, 10, SIDEBAR_WIDTH - 20, 20, Component.translatable("algocraft.gui.search"));
        this.searchBox.setHint(Component.translatable("algocraft.gui.search").withColor(IdeTheme.TEXT_DIM));
        // Set responder BEFORE setting value to avoid premature triggers
        this.addRenderableWidget(this.searchBox);

        // Repository Selector - restore preserved repository
        List<ProblemRepository> repos = (isSmokeTestEnabled() ? ProblemManager.getRepositories() : com.crabmods.algocraft.logic.catalog.ClientCatalog.getRepositories());
        if (repos.isEmpty()) {
            this.repositorySelector = null;
            this.currentRepository = null;
        } else {
            ProblemRepository initialRepo = repos.get(0);
            if (this.preservedRepositoryName != null) {
                for (ProblemRepository repo : repos) {
                    if (repo.getName().equals(this.preservedRepositoryName)) {
                        initialRepo = repo;
                        break;
                    }
                }
            }
            final ProblemRepository finalInitialRepo = initialRepo;
            this.repositorySelector = CycleButton.builder(RepositoryDisplayNames::componentFor)
                .displayOnlyValue()
                .withValues(repos)
                .withInitialValue(finalInitialRepo)
                .create(10, 35, SIDEBAR_WIDTH - 20, 20, Component.empty(), (btn, val) -> {
                    this.currentRepository = val;
                    this.updateProblemList(this.searchBox.getValue());
                });
            // Drawn by renderRepositorySelector so it matches the IDE theme; still a normal input widget.
            this.addWidget(this.repositorySelector);
            this.currentRepository = this.repositorySelector.getValue();
        }

        // Now set the responder and value after repository is set
        if (this.preservedSearchText != null) {
            this.searchBox.setValue(this.preservedSearchText);
        }
        this.searchBox.setResponder(this::updateProblemList);

        // Restore console text if available
        if (this.preservedConsoleText != null) {
            this.consoleText = this.preservedConsoleText;
        }

        // Manual official repository update. Normal startup remains offline-safe
        // and only reads the cached repository.
        boolean serverCatalog = com.crabmods.algocraft.logic.catalog.ClientCatalog.connected() && !isSmokeTestEnabled();
        this.updateOfficialButton = IdeButton.of(Component.translatable(serverCatalog ? "algocraft.gui.bank_update_help" : "algocraft.gui.update_official"), button -> beginOfficialUpdate(),
                10, 60, SIDEBAR_WIDTH - 20, 20);
        this.updateOfficialButton.active = !this.officialUpdateInProgress;
        this.updateOfficialButton.visible = !serverCatalog || (this.minecraft.player != null && this.minecraft.player.hasPermissions(2));
        this.updateOfficialButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                serverCatalog ? "algocraft.gui.bank_update_help.tooltip" : "algocraft.gui.update_official.tooltip")));
        this.addRenderableWidget(this.updateOfficialButton);

        // Import Button
        this.addRenderableWidget(IdeButton.of(Component.translatable("algocraft.gui.import"), button -> {
            Minecraft.getInstance().setScreen(new ImportProblemScreen(this));
        }, 10, 82, SIDEBAR_WIDTH - 20, 20));

        // Problem List (constrained to sidebar, not extending into console)
        IdeLayoutEngine.Rect problemListBounds = IdeLayoutEngine.calculateProblemList(this.height);
        this.problemList = new ProblemSelectionList(
                this.minecraft,
                problemListBounds.width(),
                problemListBounds.height(),
                problemListBounds.y(),
                30
        );
        this.addRenderableWidget(this.problemList);

        // Use preserved search text if available, otherwise use current searchBox value
        updateProblemList(this.searchBox.getValue());
    }

    private void addTopButtons() {
        for (IdeLayoutEngine.ButtonLayout buttonLayout : IdeLayoutEngine.calculateTopButtons(this.width)) {
            IdeLayoutEngine.Rect rect = buttonLayout.rect();
            switch (buttonLayout.control()) {
                case RUN -> {
                    this.runButton = IdeButton.of(Component.translatable("algocraft.gui.run"), button -> {
                    if (currentProblem != null) {
                        runExamples();
                    }
                    }, rect.x(), rect.y(), rect.width(), rect.height());
                    this.runButton.active = !this.runInProgress && !this.submissionInProgress;
                    this.runButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("algocraft.gui.run.tooltip")));
                    this.addRenderableWidget(this.runButton);
                }
                case SUBMIT -> {
                    this.submitButton = IdeButton.primary(Component.translatable("algocraft.gui.submit"), button -> {
                        if (currentProblem != null) {
                            submitSolution();
                        }
                    }, rect.x(), rect.y(), rect.width(), rect.height());
                    this.submitButton.active = !this.submissionInProgress && !this.runInProgress;
                    this.submitButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("algocraft.gui.submit.tooltip")));
                    this.addRenderableWidget(this.submitButton);
                }
                case HISTORY -> this.addRenderableWidget(IdeButton.of(Component.translatable("algocraft.gui.history"), button -> {
                    Minecraft.getInstance().setScreen(new SubmissionHistoryScreen(this));
                }, rect.x(), rect.y(), rect.width(), rect.height()));
                case TOGGLE_VIEW -> this.addRenderableWidget(IdeButton.of(Component.translatable("algocraft.gui.view_short"), button -> {
                    showingDescription = !showingDescription;
                    applyLayout();
                }, rect.x(), rect.y(), rect.width(), rect.height()));
                case WEB -> this.addRenderableWidget(IdeButton.of(Component.translatable("algocraft.gui.web"), button -> {
                    ClientHooks.openWebIde();
                }, rect.x(), rect.y(), rect.width(), rect.height()));
                case CLOSE -> this.addRenderableWidget(IdeButton.subtle(Component.translatable("algocraft.gui.close_short"), button -> {
                    this.onClose();
                }, rect.x(), rect.y(), rect.width(), rect.height()));
            }
        }
    }

    private void updateProblemList(String filter) {
        this.problemList.clearProblems();
        if (this.currentRepository == null) return;

        String safeFilter = filter == null ? "" : filter;
        String lowerFilter = safeFilter.toLowerCase(Locale.ROOT);

        for (Problem problem : this.currentRepository.getProblems()) {
            boolean match = safeFilter.isEmpty() ||
                            problem.getId().toLowerCase(Locale.ROOT).contains(lowerFilter) ||
                            ProblemDisplayText.title(problem).toLowerCase(Locale.ROOT).contains(lowerFilter) ||
                            problem.getTags().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(lowerFilter));

            if (match) {
                this.problemList.addProblem(problem, this::selectProblem);
            }
        }
    }

    private void selectProblem(Problem problem) {
        rememberCurrentDraft();
        saveDrafts();
        if (this.currentProblem != null && this.currentProblem.getId().equals(problem.getId())) {
            this.currentProblem = problem;
            this.problemViewer.setProblem(problem);
            return;
        }
        this.problemStartTime = System.currentTimeMillis();  // Start tracking solve time
        this.currentProblem = problem;
        this.preservedProblem = problem;
        sendSolvingState(true);
        this.currentCode = sessionDrafts.getOrDefault(problem.getId(),
                drafts == null ? problem.getInitialCode() : drafts.getOrDefault(problem.getId(), problem.getInitialCode()));
        this.codeEditor.setValue(this.currentCode);
        this.problemViewer.setProblem(problem);
        this.lastSmokeSubmissionResult = null;
        this.consoleText = problemConsoles.getOrDefault(problem.getId(),
                Component.translatable("algocraft.gui.loaded_problem", ProblemDisplayText.title(problem)).getString());
        this.consoleScrollY = 0;
    }

    @Override
    public void onClose() {
        rememberCurrentDraft();
        saveDrafts();
        sendSolvingState(false);
        super.onClose();
    }

    private void rememberCurrentDraft() {
        if (currentProblem == null || codeEditor == null) return;
        String code = codeEditor.getValue();
        sessionDrafts.put(currentProblem.getId(), code);
        if (drafts != null) drafts.put(currentProblem.getId(), code);
        problemConsoles.put(currentProblem.getId(), consoleText);
    }

    private void saveDrafts() {
        if (drafts == null) return;
        try {
            drafts.save();
        } catch (java.io.IOException e) {
            System.getLogger(ModernAlgorithmScreen.class.getName()).log(System.Logger.Level.WARNING, "Could not save code drafts", e);
        }
    }

    private String observedCatalogRevision = "";
    public void refreshServerCatalog() {
        rememberCurrentDraft(); saveDrafts();
        this.drafts = null;
        this.init(this.minecraft, this.width, this.height);
    }
    @Override
    public void tick() {
        String revision = com.crabmods.algocraft.logic.catalog.ClientCatalog.revision();
        if (!isSmokeTestEnabled() && !observedCatalogRevision.equals(revision)) {
            observedCatalogRevision = revision;
            refreshServerCatalog();
        }
        if (++draftSaveTicks >= 40) {
            draftSaveTicks = 0;
            rememberCurrentDraft();
            saveDrafts();
        }
    }

    @Override
    public void removed() {
        rememberCurrentDraft();
        saveDrafts();
        if (problemViewer != null) problemViewer.releaseImages();
    }

    private void sendSolvingState(boolean solving) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            String problemId = currentProblem != null ? currentProblem.getId() : "";
            if (solving && !isSmokeTestEnabled() && (currentProblem == null || !currentProblem.isPublished())) solving = false;
            PacketDistributor.sendToServer(new PacketSetSolvingState(solving, problemId));
        }
    }

    private void beginOfficialUpdate() {
        if (!isSmokeTestEnabled() && com.crabmods.algocraft.logic.catalog.ClientCatalog.connected()) {
            this.consoleText = Component.translatable("algocraft.gui.bank_update_help.instructions").getString();
            this.consoleScrollY = 0;
            return;
        }
        if (officialUpdateInProgress) {
            return;
        }
        Supplier<CompletableFuture<Void>> smokeAction = officialUpdateActionForSmokeTest;
        if (isSmokeTestEnabled() && smokeAction == null) {
            this.consoleText = SMOKE_UPDATE_CONSOLE;
            this.consoleScrollY = 0;
            return;
        }

        officialUpdateInProgress = true;
        if (this.updateOfficialButton != null) {
            this.updateOfficialButton.active = false;
        }
        this.consoleText = Component.translatable("algocraft.gui.update_official.running").getString();
        this.consoleScrollY = 0;

        CompletableFuture<Void> updateFuture;
        try {
            updateFuture = smokeAction != null ? smokeAction.get() : ProblemManager.forceRefreshOfficial();
        } catch (Throwable e) {
            updateFuture = CompletableFuture.failedFuture(e);
        }

        updateFuture
                .thenRun(() -> runOnClientThread(this::finishOfficialUpdateSuccess))
                .exceptionally(e -> {
                    runOnClientThread(() -> finishOfficialUpdateFailure(e));
                    return null;
                });
    }

    private void finishOfficialUpdateSuccess() {
        officialUpdateInProgress = false;
        if (this.updateOfficialButton != null) {
            this.updateOfficialButton.active = true;
        }
        this.consoleText = Component.translatable("algocraft.gui.update_official.success").getString();
        this.consoleScrollY = 0;
        this.updateProblemList(this.searchBox.getValue());
    }

    private void finishOfficialUpdateFailure(Throwable error) {
        officialUpdateInProgress = false;
        if (this.updateOfficialButton != null) {
            this.updateOfficialButton.active = true;
        }
        this.consoleText = Component.translatable(
                "algocraft.gui.update_official.error",
                UiErrorMessages.fromThrowable(error)
        ).getString();
        this.consoleScrollY = 0;
        this.updateProblemList(this.searchBox.getValue());
    }

    private static void runOnClientThread(Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((minecraft != null && minecraft.isSameThread()) || RenderSystem.isOnRenderThread()) {
            action.run();
        } else {
            minecraft.execute(action);
        }
    }

    private void runExamples() {
        if (this.runInProgress || this.submissionInProgress || this.currentProblem == null) return;
        Problem runningProblem = this.currentProblem;
        String runningCode = this.codeEditor.getValue();
        StringBuilder sb = new StringBuilder();
        sb.append(Component.translatable("algocraft.gui.running_examples", ProblemDisplayText.title(currentProblem)).getString()).append("\n");

        List<Problem.TestCase> examples = currentProblem.getExamples();

        if (examples.isEmpty()) {
            sb.append(Component.translatable("algocraft.gui.no_examples").getString());
            this.consoleText = sb.toString();
            this.consoleScrollY = 0;
            return;
        }

        // Use batch execution for better performance (compiles once)
        List<CodeExecutor.TestCase> testCases = new java.util.ArrayList<>();
        String preferredMethodName = CodeExecutor.preferredMethodNameFromInitialCode(currentProblem.getInitialCode());
        for (Problem.TestCase test : examples) {
            testCases.add(new CodeExecutor.TestCase(
                    test.getInput(),
                    test.getOutput(),
                    currentProblem.getId(),
                    preferredMethodName
            ));
        }

        this.consoleText = sb.toString();
        this.consoleScrollY = 0;
        this.runInProgress = true;
        updateExecutionButtons();
        CompletableFuture<List<CodeExecutor.TestResult>> future;
        try {
            var smokeAction = exampleActionForSmokeTest;
            future = smokeAction != null ? smokeAction.apply(runningCode, testCases)
                    : CompletableFuture.supplyAsync(() -> CodeExecutor.executeBatch(runningCode, testCases), submissionExecutor());
        } catch (Throwable e) {
            future = CompletableFuture.failedFuture(e);
        }
        future.whenComplete((results, error) -> runOnClientThread(() -> {
            this.runInProgress = false;
            updateExecutionButtons();
            if (error != null || results == null || results.size() != examples.size()) {
                sb.append(UiErrorMessages.fromThrowable(error == null ? new IllegalStateException("Missing example results") : error));
            } else {
        for (int i = 0; i < examples.size(); i++) {
            Problem.TestCase test = examples.get(i);
            CodeExecutor.TestResult result = results.get(i);
            String resultStr = result.passed ? "PASS" : (result.message != null ? result.message : "FAIL");
            sb.append(Component.translatable("algocraft.gui.input", test.getInput()).getString())
              .append(" | ")
              .append(Component.translatable("algocraft.gui.result", resultStr).getString())
              .append("\n");
        }

            }
            problemConsoles.put(runningProblem.getId(), sb.toString());
            if (currentProblem != null && currentProblem.getId().equals(runningProblem.getId())) {
                this.consoleText = sb.toString();
                this.consoleScrollY = 0;
            }
        }));
    }

    private void submitSolution() {
        if (this.submissionInProgress || this.runInProgress || this.currentProblem == null) {
            return;
        }
        boolean smokeTest = isSmokeTestEnabled();
        this.consoleText = Component.translatable("algocraft.gui.submitting").getString();
        this.consoleScrollY = 0;
        Problem submittedProblem = this.currentProblem;
        String submittedCode = codeEditor.getValue();
        if (!PacketSubmitSolution.canEncodeSubmission(submittedProblem.getId(), submittedCode)) {
            this.consoleText = Component.translatable(
                    "algocraft.msg.submit_code_too_large",
                    PacketSubmitSolution.MAX_CODE_LENGTH
            ).getString();
            this.consoleScrollY = 0;
            if (smokeTest) {
                this.lastSmokeSubmissionResult = null;
            }
            return;
        }

        this.lastSmokeSubmissionResult = null;
        setSubmissionInProgress(true);
        long solveTimeMs = problemStartTime > 0 ? System.currentTimeMillis() - problemStartTime : 0;
        CompletableFuture<SubmissionResult> judgeFuture;
        try {
            BiFunction<Problem, String, CompletableFuture<SubmissionResult>> smokeAction = submissionJudgeActionForSmokeTest;
            judgeFuture = smokeAction != null
                    ? smokeAction.apply(submittedProblem, submittedCode)
                    : !smokeTest && com.crabmods.algocraft.logic.catalog.ClientCatalog.connected()
                    ? com.crabmods.algocraft.client.ServerSubmissionBridge.submit(submittedProblem, submittedCode)
                    : CompletableFuture.supplyAsync(() -> Judge.grade(submittedProblem, submittedCode), submissionExecutor());
        } catch (Throwable e) {
            judgeFuture = CompletableFuture.failedFuture(e);
        }

        judgeFuture.whenComplete((result, error) -> runOnClientThread(() ->
                finishSubmission(submittedProblem, submittedCode, solveTimeMs, smokeTest, result, error)));
    }

    private void finishSubmission(Problem submittedProblem, String submittedCode, long solveTimeMs,
                                  boolean smokeTest, SubmissionResult result, Throwable error) {
        setSubmissionInProgress(false);
        SubmissionResult effectiveResult = result;
        if (error != null) {
            effectiveResult = rejectedClientResult(UiErrorMessages.fromThrowable(error));
        } else if (effectiveResult == null) {
            effectiveResult = rejectedClientResult("Unknown Error");
        }

        if (smokeTest) {
            this.lastSmokeSubmissionResult = effectiveResult;
        }

        // Save submission record
        if (!smokeTest) {
            com.crabmods.algocraft.logic.SubmissionHistoryManager.saveRecord(new com.crabmods.algocraft.logic.SubmissionRecord(
                System.currentTimeMillis(),
                submittedProblem.getId(),
                ProblemDisplayText.title(submittedProblem),
                effectiveResult.isSuccess() ? "Accepted" : effectiveResult.getMessage(),
                effectiveResult.getExecutionTimeMs(),
                effectiveResult.getPassedCount(),
                effectiveResult.getTotalCount()
            ));
        }

        StringBuilder sb = new StringBuilder();
        if (smokeTest) {
            sb.append(SMOKE_SUBMIT_CONSOLE).append("\n");
        }


        if (effectiveResult.isSuccess()) {
            if (!smokeTest && !submittedProblem.isPublished()) com.crabmods.algocraft.logic.ProgressManager.markAsPassed(submittedProblem.getId());
            // Server rewards are applied only after the authoritative submit_solution packet is judged.
        } else {
            sb.append(Component.translatable("algocraft.msg.submission_failed").getString()).append(" ")
                    .append(effectiveResult.getMessage()).append("\n");
            for (SubmissionResult.TestCaseResult detail : effectiveResult.getDetails()) {
                if (!detail.isPassed()) {
                    if (detail.getError() != null) sb.append(UiErrorMessages.bounded(detail.getError())).append("\n");
                    else sb.append(Component.translatable("algocraft.gui.expected", detail.getExpected()).getString())
                            .append("\n").append(Component.translatable("algocraft.gui.actual", detail.getActual()).getString()).append("\n");
                    break;
                }
            }
        }

        sb.append(Component.translatable("algocraft.gui.passed", effectiveResult.getPassedCount(), effectiveResult.getTotalCount()).getString()).append("\n");
        sb.append(Component.translatable("algocraft.gui.time", effectiveResult.getExecutionTimeMs()).getString());

        problemConsoles.put(submittedProblem.getId(), sb.toString());
        if (currentProblem != null && currentProblem.getId().equals(submittedProblem.getId())) {
            this.consoleText = sb.toString();
            this.consoleScrollY = 0;
        }
    }

    private void setSubmissionInProgress(boolean inProgress) {
        this.submissionInProgress = inProgress;
        updateExecutionButtons();
    }

    private void updateExecutionButtons() {
        boolean active = !runInProgress && !submissionInProgress;
        if (submitButton != null) submitButton.active = active;
        if (runButton != null) runButton.active = active;
    }

    private static SubmissionResult rejectedClientResult(String message) {
        SubmissionResult result = new SubmissionResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setTotalCount(0);
        return result;
    }

    private static ExecutorService submissionExecutor() {
        synchronized (SUBMISSION_EXECUTOR_LOCK) {
            if (submissionExecutor == null || submissionExecutor.isShutdown() || submissionExecutor.isTerminated()) {
                submissionExecutor = Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable,
                            "AlgoCraft-ClientJudge-" + SUBMISSION_THREAD_IDS.incrementAndGet());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    return thread;
                });
            }
            return submissionExecutor;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ctrlDown = Screen.hasControlDown()
                || (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        if (ctrlDown) {
            if (keyCode == GLFW.GLFW_KEY_R) {
                if (currentProblem != null) {
                    runExamples();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (currentProblem != null) {
                    submitSolution();
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Check if mouse is in console area
        int consoleHeight = IdeLayoutEngine.consoleHeight(this.height);
        int consoleY = this.height - consoleHeight;
        if (mouseX > SIDEBAR_WIDTH && mouseY > consoleY) {
            // Scroll console
            var allLines = consoleLines();
            int lineHeight = 10;
            int maxVisibleLines = (consoleHeight - 25) / lineHeight;
            int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);

            consoleScrollY -= (int) scrollY;
            consoleScrollY = Math.max(0, Math.min(consoleScrollY, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int consoleHeight = IdeLayoutEngine.consoleHeight(this.height);
        int consoleY = this.height - consoleHeight;

        // Background, panels and widgets. Screen.render draws renderBackground first.
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderRepositorySelector(guiGraphics, mouseX, mouseY, partialTick);
        renderTopBarTitle(guiGraphics);

        // Console header and text.
        guiGraphics.fill(SIDEBAR_WIDTH + 10, consoleY + 7, SIDEBAR_WIDTH + 12, consoleY + 13, consoleStatusColor());
        guiGraphics.drawString(this.font, Component.translatable("algocraft.gui.terminal"), SIDEBAR_WIDTH + 16, consoleY + 6, IdeTheme.TEXT_MUTED, false);

        // Split console text by newlines and render with scroll support
        var allLines = consoleLines();
        int lineHeight = 10;
        int consoleTextY = consoleY + 21;
        int maxVisibleLines = (consoleHeight - 25) / lineHeight;

        // Enable scissor to clip text within console bounds
        guiGraphics.enableScissor(SIDEBAR_WIDTH + 5, consoleTextY, this.width - 5, this.height - 2);

        int startLine = Math.max(0, Math.min(consoleScrollY, allLines.size() - maxVisibleLines));
        for (int i = 0; i < maxVisibleLines && (startLine + i) < allLines.size(); i++) {
            var line = allLines.get(startLine + i);
            guiGraphics.drawString(this.font, line, SIDEBAR_WIDTH + 10, consoleTextY + (i * lineHeight), IdeTheme.TEXT, false);
        }

        guiGraphics.disableScissor();

        // Scroll indicator if more lines available
        if (allLines.size() > maxVisibleLines) {
            int scrollIndicatorColor = IdeTheme.TEXT_DIM;
            if (consoleScrollY > 0) {
                guiGraphics.drawString(this.font, "^", this.width - 15, consoleY + 6, scrollIndicatorColor, false);
            }
            if (consoleScrollY + maxVisibleLines < allLines.size()) {
                guiGraphics.drawString(this.font, "v", this.width - 15, this.height - 12, scrollIndicatorColor, false);
            }
        }

    }

    private int consoleStatusColor() {
        if (this.submissionInProgress || this.runInProgress || this.officialUpdateInProgress) {
            return IdeTheme.WARNING;
        }
        return IdeTheme.ACCENT;
    }

    private void renderRepositorySelector(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        CycleButton<ProblemRepository> selector = this.repositorySelector;
        if (selector == null || !selector.visible) {
            return;
        }
        boolean hot = selector.isMouseOver(mouseX, mouseY);
        int x = selector.getX();
        int y = selector.getY();
        int w = selector.getWidth();
        int h = selector.getHeight();
        int border = selector.isFocused() && !hot ? IdeTheme.INFO : (hot ? 0xFF8B949E : IdeTheme.BORDER_STRONG);
        IdeTheme.frame(guiGraphics, x, y, w, h, hot ? IdeTheme.RAISED_HOVER : IdeTheme.RAISED, border);
        guiGraphics.fill(x + 6, y + 7, x + 9, y + h - 7, IdeTheme.ACCENT);
        String label = IdeTheme.ellipsize(this.font, selector.getMessage().getString(), w - 30);
        guiGraphics.drawString(this.font, label, x + 13, y + (h - 8) / 2, IdeTheme.TEXT, false);
        guiGraphics.drawString(this.font, ">", x + w - 10, y + (h - 8) / 2, hot ? IdeTheme.TEXT : IdeTheme.TEXT_DIM, false);
    }

    private void renderTopBarTitle(GuiGraphics guiGraphics) {
        if (this.currentProblem == null) {
            return;
        }
        List<IdeLayoutEngine.ButtonLayout> buttons = IdeLayoutEngine.calculateTopButtons(this.width);
        int left = SIDEBAR_WIDTH + 10;
        int right = this.width - 30;
        for (IdeLayoutEngine.ButtonLayout button : buttons) {
            if (button.control() == IdeLayoutEngine.Control.CLOSE) {
                right = button.rect().x() - 8;
            } else if (button.rect().y() == 5) {
                left = Math.max(left, button.rect().right() + 12);
            }
        }
        int available = right - left;
        if (available < 60) {
            return;
        }
        String difficulty = this.currentProblem.getDifficulty() == null ? "" : this.currentProblem.getDifficulty();
        int badgeWidth = difficulty.isEmpty() ? 0 : this.font.width(difficulty) + 8;
        String title = IdeTheme.ellipsize(this.font, ProblemDisplayText.title(this.currentProblem), available - badgeWidth - 6);
        int titleX = right - badgeWidth - (badgeWidth > 0 ? 6 : 0) - this.font.width(title);
        guiGraphics.drawString(this.font, title, titleX, 11, IdeTheme.TEXT_MUTED, false);
        if (badgeWidth > 0) {
            IdeTheme.pill(guiGraphics, this.font, difficulty, right - badgeWidth, 9, IdeTheme.difficultyColor(difficulty));
        }
    }

    private List<net.minecraft.util.FormattedCharSequence> consoleLines() {
        return this.font.split(Component.literal(this.consoleText), Math.max(1, this.width - SIDEBAR_WIDTH - 25));
    }

    private void applyLayout() {
        IdeLayoutEngine.PanelLayout layout = calculateLayout();
        if (this.problemViewer != null) {
            this.problemViewer.setBounds(layout.problemX(), layout.problemY(), layout.problemWidth(), layout.problemHeight());
            this.problemViewer.visible = layout.problemVisible();
        }
        if (this.codeEditor != null) {
            this.codeEditor.setBounds(layout.editorX(), layout.editorY(), layout.editorWidth(), layout.editorHeight());
            this.codeEditor.visible = layout.editorVisible();
        }
    }

    private IdeLayoutEngine.PanelLayout calculateLayout() {
        return IdeLayoutEngine.calculatePanels(this.width, this.height, this.showingDescription);
    }

    public void selectProblemForSmokeTest(Problem problem) {
        requireSmokeTest();
        selectProblem(problem);
    }

    public String getConsoleTextForSmokeTest() {
        requireSmokeTest();
        return this.consoleText;
    }

    public String getCodeTextForSmokeTest() {
        requireSmokeTest();
        return this.codeEditor.getValue();
    }

    public SubmissionResult getLastSmokeSubmissionResultForSmokeTest() {
        requireSmokeTest();
        return this.lastSmokeSubmissionResult;
    }

    public Problem getCurrentProblemForSmokeTest() {
        requireSmokeTest();
        return this.currentProblem;
    }

    public boolean isShowingDescriptionForSmokeTest() {
        requireSmokeTest();
        return this.showingDescription;
    }

    public boolean isProblemViewerVisibleForSmokeTest() {
        requireSmokeTest();
        return this.problemViewer != null && this.problemViewer.visible;
    }

    public int getLoadedProblemImageBlockCountForSmokeTest() {
        requireSmokeTest();
        return this.problemViewer == null ? 0 : this.problemViewer.getLoadedImageBlockCountForSmokeTest();
    }

    public int getMissingProblemImageBlockCountForSmokeTest() {
        requireSmokeTest();
        return this.problemViewer == null ? 0 : this.problemViewer.getMissingImageBlockCountForSmokeTest();
    }

    public int getVisibleLoadedProblemImageBlockCountForSmokeTest() {
        requireSmokeTest();
        return this.problemViewer == null ? 0 : this.problemViewer.getVisibleLoadedImageBlockCountForSmokeTest();
    }

    public boolean isCodeEditorVisibleForSmokeTest() {
        requireSmokeTest();
        return this.codeEditor != null && this.codeEditor.visible;
    }

    public boolean isOfficialUpdateInProgressForSmokeTest() {
        requireSmokeTest();
        return this.officialUpdateInProgress;
    }

    public boolean isSubmissionInProgressForSmokeTest() {
        requireSmokeTest();
        return this.submissionInProgress;
    }

    public boolean isRunInProgressForSmokeTest() {
        requireSmokeTest();
        return this.runInProgress;
    }

    public static void setExampleActionForSmokeTest(
            BiFunction<String, List<CodeExecutor.TestCase>, CompletableFuture<List<CodeExecutor.TestResult>>> action) {
        requireSmokeTest();
        exampleActionForSmokeTest = action;
    }

    public static void clearExampleActionForSmokeTest() {
        exampleActionForSmokeTest = null;
    }

    public boolean isSubmitButtonActiveForSmokeTest() {
        requireSmokeTest();
        return this.submitButton != null && this.submitButton.active;
    }

    public boolean isUpdateOfficialButtonActiveForSmokeTest() {
        requireSmokeTest();
        return this.updateOfficialButton != null && this.updateOfficialButton.active;
    }

    public static void setSubmissionJudgeActionForSmokeTest(
            BiFunction<Problem, String, CompletableFuture<SubmissionResult>> action) {
        requireSmokeTest();
        submissionJudgeActionForSmokeTest = action;
    }

    public static void clearSubmissionJudgeActionForSmokeTest() {
        submissionJudgeActionForSmokeTest = null;
    }

    public static void setOfficialUpdateActionForSmokeTest(Supplier<CompletableFuture<Void>> action) {
        requireSmokeTest();
        officialUpdateActionForSmokeTest = action;
    }

    public static void clearOfficialUpdateActionForSmokeTest() {
        officialUpdateActionForSmokeTest = null;
    }

    private static void requireSmokeTest() {
        if (!isSmokeTestEnabled()) {
            throw new IllegalStateException("IDE smoke-test hooks are disabled");
        }
    }

    private static boolean isSmokeTestEnabled() {
        return Boolean.getBoolean("algocraft.ideSmokeTest");
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int topBarHeight = IdeLayoutEngine.topBarHeight(this.width);
        int consoleY = this.height - IdeLayoutEngine.consoleHeight(this.height);
        guiGraphics.fill(0, 0, this.width, this.height, IdeTheme.BACKGROUND);

        // Sidebar: controls on top, problem list below.
        guiGraphics.fill(0, 0, SIDEBAR_WIDTH, this.height, IdeTheme.SIDEBAR);
        guiGraphics.fill(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH + 1, this.height, IdeTheme.BORDER);

        // Toolbar.
        guiGraphics.fill(SIDEBAR_WIDTH + 1, 0, this.width, topBarHeight, IdeTheme.PANEL_HEADER);
        guiGraphics.fill(SIDEBAR_WIDTH + 1, topBarHeight, this.width, topBarHeight + 1, IdeTheme.BORDER);

        // Terminal.
        guiGraphics.fill(SIDEBAR_WIDTH + 1, consoleY, this.width, this.height, IdeTheme.PANEL);
        guiGraphics.fill(SIDEBAR_WIDTH + 1, consoleY, this.width, consoleY + 1, IdeTheme.BORDER);
        guiGraphics.fill(SIDEBAR_WIDTH + 1, consoleY + 1, this.width, consoleY + 18, IdeTheme.PANEL_HEADER);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
