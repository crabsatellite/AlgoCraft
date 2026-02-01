package com.crabmods.algocraft.client.gui.modern;

import com.crabmods.algocraft.client.gui.component.CodeEditorWidget;
import com.crabmods.algocraft.client.gui.component.ProblemSelectionList;
import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.ProgressManager;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.network.PacketSolveProblem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ModernAlgorithmScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;
    private static final int TOP_BAR_HEIGHT = 30;
    private static final int BOTTOM_BAR_HEIGHT = 80;
    
    private CodeEditorWidget codeEditor;
    private MultiLineEditBox descriptionViewer;
    private net.minecraft.client.gui.components.EditBox searchBox;
    private ProblemSelectionList problemList;
    private CycleButton<ProblemRepository> repositorySelector;
    private ProblemRepository currentRepository;
    
    private String consoleText = "";
    private Problem currentProblem;
    private boolean showingDescription = false;
    private long problemStartTime = 0;  // Track when the current problem was loaded
    private int consoleScrollY = 0;  // Console scroll position
    private int maxConsoleLines = 5;  // Max visible lines in console
    
    // State to preserve across window resize (init() is called again)
    private String preservedCode = null;
    private String preservedProblemId = null;
    private String preservedSearchText = null;
    private String preservedRepositoryName = null;
    private String preservedConsoleText = null;

    public ModernAlgorithmScreen() {
        super(Component.translatable("algocraft.gui.ide_title"));
        this.consoleText = Component.translatable("algocraft.gui.ready").getString();
    }

    @Override
    protected void init() {
        // Save state before recreating widgets (window resize triggers init() again)
        if (this.codeEditor != null) {
            this.preservedCode = this.codeEditor.getValue();
        }
        if (this.currentProblem != null) {
            this.preservedProblemId = this.currentProblem.getId();
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
        
        int editorWidth = this.width - SIDEBAR_WIDTH - 20;
        int editorHeight = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 20;

        // Code Editor
        this.codeEditor = new CodeEditorWidget(this.font, SIDEBAR_WIDTH + 10, TOP_BAR_HEIGHT + 10, editorWidth, editorHeight, Component.translatable("algocraft.gui.code"));
        this.addRenderableWidget(this.codeEditor);

        // Description Viewer (Initially hidden, preserving visibility state)
        this.descriptionViewer = new MultiLineEditBox(this.font, SIDEBAR_WIDTH + 10, TOP_BAR_HEIGHT + 10, editorWidth, editorHeight, Component.literal(""), Component.translatable("algocraft.gui.description"));
        this.descriptionViewer.visible = this.showingDescription;
        this.codeEditor.visible = !this.showingDescription;
        this.addRenderableWidget(this.descriptionViewer);

        // Load Problems and restore state if available
        List<Problem> problems = ProblemManager.getProblems();
        Problem problemToSelect = null;
        
        // Try to restore previously selected problem
        if (this.preservedProblemId != null) {
            for (Problem p : problems) {
                if (p.getId().equals(this.preservedProblemId)) {
                    problemToSelect = p;
                    break;
                }
            }
        }
        
        // Fall back to first problem if no preserved state
        if (problemToSelect == null && !problems.isEmpty()) {
            problemToSelect = problems.get(0);
        }
        
        if (problemToSelect != null) {
            // Don't reset timer if restoring the same problem
            boolean sameAsPreserved = this.preservedProblemId != null && problemToSelect.getId().equals(this.preservedProblemId);
            if (sameAsPreserved) {
                this.currentProblem = problemToSelect;
                this.descriptionViewer.setValue(problemToSelect.getDescription());
                // Restore preserved code instead of resetting to initial code
                if (this.preservedCode != null) {
                    this.codeEditor.setValue(this.preservedCode);
                } else {
                    this.codeEditor.setValue(problemToSelect.getInitialCode());
                }
            } else {
                selectProblem(problemToSelect);
            }
        }

        // Calculate responsive button layout
        int availableWidth = this.width - SIDEBAR_WIDTH - 30; // 30 for close button + padding
        int buttonY = 5;
        int buttonHeight = 20;
        int buttonSpacing = 5;
        
        // Minimum button widths
        int runWidth = Math.max(50, Math.min(60, availableWidth / 6));
        int submitWidth = Math.max(55, Math.min(70, availableWidth / 5));
        int historyWidth = Math.max(55, Math.min(70, availableWidth / 5));
        int toggleWidth = Math.max(60, Math.min(80, availableWidth / 5));
        
        int buttonX = SIDEBAR_WIDTH + 10;
        
        // Run Button
        this.addRenderableWidget(Button.builder(Component.literal("▶ ").append(Component.translatable("algocraft.gui.run")), button -> {
            if (currentProblem == null) return;
            runExamples();
        }).bounds(buttonX, buttonY, runWidth, buttonHeight).build());
        buttonX += runWidth + buttonSpacing;

        // Submit Button
        this.addRenderableWidget(Button.builder(Component.literal("✔ ").append(Component.translatable("algocraft.gui.submit")), button -> {
            if (currentProblem == null) return;
            submitSolution();
        }).bounds(buttonX, buttonY, submitWidth, buttonHeight).build());
        buttonX += submitWidth + buttonSpacing;
        
        // History Button
        this.addRenderableWidget(Button.builder(Component.literal("🕒 ").append(Component.translatable("algocraft.gui.history")), button -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(new SubmissionHistoryScreen(this));
        }).bounds(buttonX, buttonY, historyWidth, buttonHeight).build());
        buttonX += historyWidth + buttonSpacing;

        // Toggle View Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.toggle_view"), button -> {
            showingDescription = !showingDescription;
            codeEditor.visible = !showingDescription;
            descriptionViewer.visible = showingDescription;
        }).bounds(buttonX, buttonY, toggleWidth, buttonHeight).build());

        // Close Button (always at right edge)
        this.addRenderableWidget(Button.builder(Component.literal("X"), button -> {
            this.onClose();
        }).bounds(this.width - 25, buttonY, 20, buttonHeight).build());
        
        // Search Box - restore preserved search text
        this.searchBox = new net.minecraft.client.gui.components.EditBox(this.font, 10, 10, SIDEBAR_WIDTH - 20, 20, Component.translatable("algocraft.gui.search"));
        // Set responder BEFORE setting value to avoid premature triggers
        this.addRenderableWidget(this.searchBox);

        // Repository Selector - restore preserved repository
        List<ProblemRepository> repos = ProblemManager.getRepositories();
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
        this.repositorySelector = CycleButton.builder((ProblemRepository repo) -> Component.literal(repo.getName()))
            .withValues(repos)
            .withInitialValue(finalInitialRepo)
            .create(10, 35, SIDEBAR_WIDTH - 20, 20, Component.empty(), (btn, val) -> {
                this.currentRepository = val;
                this.updateProblemList(this.searchBox.getValue());
            });
        this.addRenderableWidget(this.repositorySelector);
        this.currentRepository = this.repositorySelector.getValue();
        
        // Now set the responder and value after repository is set
        if (this.preservedSearchText != null) {
            this.searchBox.setValue(this.preservedSearchText);
        }
        this.searchBox.setResponder(this::updateProblemList);
        
        // Restore console text if available
        if (this.preservedConsoleText != null) {
            this.consoleText = this.preservedConsoleText;
        }

        // Import Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.import"), button -> {
            Minecraft.getInstance().setScreen(new ImportProblemScreen(this));
        }).bounds(10, 60, SIDEBAR_WIDTH - 20, 20).build());
        
        // Problem List (constrained to sidebar, not extending into console)
        int problemListHeight = this.height - 85;  // From top 85 to bottom
        this.problemList = new ProblemSelectionList(this.minecraft, SIDEBAR_WIDTH, problemListHeight, 85, 30);
        this.addRenderableWidget(this.problemList);
        
        // Use preserved search text if available, otherwise use current searchBox value
        updateProblemList(this.searchBox.getValue());
    }

    private void updateProblemList(String filter) {
        this.problemList.clearProblems();
        if (this.currentRepository == null) return;
        
        String lowerFilter = filter.toLowerCase();
        
        for (Problem problem : this.currentRepository.getProblems()) {
            boolean match = filter.isEmpty() || 
                            problem.getTitle().toLowerCase().contains(lowerFilter) ||
                            problem.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerFilter));
            
            if (match) {
                this.problemList.addProblem(problem, this::selectProblem);
            }
        }
    }

    private void selectProblem(Problem problem) {
        this.problemStartTime = System.currentTimeMillis();  // Start tracking solve time
        this.currentProblem = problem;
        this.codeEditor.setValue(problem.getInitialCode());
        this.descriptionViewer.setValue(problem.getDescription());
        this.consoleText = Component.translatable("algocraft.gui.loaded_problem", problem.getTitle()).getString();
    }

    private void runExamples() {
        StringBuilder sb = new StringBuilder();
        sb.append(Component.translatable("algocraft.gui.running_examples", currentProblem.getTitle()).getString()).append("\n");
        
        List<Problem.TestCase> examples = currentProblem.getExamples();
        
        if (examples.isEmpty()) {
            sb.append(Component.translatable("algocraft.gui.no_examples").getString());
            this.consoleText = sb.toString();
            return;
        }
        
        // Use batch execution for better performance (compiles once)
        List<CodeExecutor.TestCase> testCases = new java.util.ArrayList<>();
        for (Problem.TestCase test : examples) {
            testCases.add(new CodeExecutor.TestCase(test.getInput(), test.getOutput()));
        }
        
        List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch(codeEditor.getValue(), testCases);
        
        for (int i = 0; i < examples.size(); i++) {
            Problem.TestCase test = examples.get(i);
            CodeExecutor.TestResult result = results.get(i);
            String resultStr = result.passed ? "PASS" : (result.message != null ? result.message : "FAIL");
            sb.append(Component.translatable("algocraft.gui.input", test.getInput()).getString())
              .append(" | ")
              .append(Component.translatable("algocraft.gui.result", resultStr).getString())
              .append("\n");
        }
        
        this.consoleText = sb.toString();
    }

    private void submitSolution() {
        this.consoleText = Component.translatable("algocraft.gui.submitting").getString();
        SubmissionResult result = Judge.grade(currentProblem, codeEditor.getValue());
        
        // Save submission record
        com.crabmods.algocraft.logic.SubmissionHistoryManager.saveRecord(new com.crabmods.algocraft.logic.SubmissionRecord(
            System.currentTimeMillis(),
            currentProblem.getId(),
            currentProblem.getTitle(),
            result.isSuccess() ? "Accepted" : result.getMessage(),
            result.getExecutionTimeMs(),
            result.getPassedCount(),
            result.getTotalCount()
        ));

        StringBuilder sb = new StringBuilder();
        
        if (result.isSuccess()) {
            // Calculate solve time
            long solveTimeMs = problemStartTime > 0 ? System.currentTimeMillis() - problemStartTime : 0;
            // Send packet to server to reward player (with solve time for Speed Demon achievement)
            PacketDistributor.sendToServer(new PacketSolveProblem(currentProblem.getId(), currentProblem.getDifficulty(), solveTimeMs));
        } else {
            sb.append(Component.translatable("algocraft.msg.submission_failed").getString()).append(" ").append(result.getMessage()).append("\n");
            // Send packet to server to reset consecutive correct count
            PacketDistributor.sendToServer(new com.crabmods.algocraft.network.PacketSubmissionFailed(currentProblem.getId()));
        }
        
        sb.append(Component.translatable("algocraft.gui.passed", result.getPassedCount(), result.getTotalCount()).getString()).append("\n");
        sb.append(Component.translatable("algocraft.gui.time", result.getExecutionTimeMs()).getString());
        
        this.consoleText = sb.toString();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // CodeEditorWidget handles its own keys, but we can intercept global shortcuts here if needed
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Check if mouse is in console area
        int consoleY = this.height - BOTTOM_BAR_HEIGHT;
        if (mouseX > SIDEBAR_WIDTH && mouseY > consoleY) {
            // Scroll console
            String[] allLines = this.consoleText.split("\n");
            int lineHeight = 10;
            int maxVisibleLines = (BOTTOM_BAR_HEIGHT - 25) / lineHeight;
            int maxScroll = Math.max(0, allLines.length - maxVisibleLines);
            
            consoleScrollY -= (int) scrollY;
            consoleScrollY = Math.max(0, Math.min(consoleScrollY, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 2. Sidebar Area
        guiGraphics.fill(0, 0, SIDEBAR_WIDTH, this.height, 0xFF252526); // Dark Grey
        guiGraphics.vLine(SIDEBAR_WIDTH, 0, this.height, 0xFF3E3E42); // Border
        
        // 3. Top Bar Area
        guiGraphics.fill(SIDEBAR_WIDTH, 0, this.width, TOP_BAR_HEIGHT, 0xFF333333); // Lighter Grey
        guiGraphics.hLine(SIDEBAR_WIDTH, this.width, TOP_BAR_HEIGHT, 0xFF3E3E42); // Border

        // 4. Console Area
        int consoleY = this.height - BOTTOM_BAR_HEIGHT;
        guiGraphics.fill(SIDEBAR_WIDTH, consoleY, this.width, this.height, 0xFF1E1E1E); // Very Dark Grey
        guiGraphics.hLine(SIDEBAR_WIDTH, this.width, consoleY, 0xFF3E3E42); // Border

        // 5. Sidebar Header
        guiGraphics.drawCenteredString(this.font, Component.translatable("algocraft.gui.problems"), SIDEBAR_WIDTH / 2, 15, 0xFFAAAAAA);
        // guiGraphics.drawString(this.font, Component.translatable("algocraft.gui.available"), 10, 35, 0xFF888888);

        // 6. Console Header & Text
        guiGraphics.drawString(this.font, Component.translatable("algocraft.gui.terminal"), SIDEBAR_WIDTH + 10, consoleY + 5, 0xFFAAAAAA);
        
        // Split console text by newlines and render with scroll support
        String[] allLines = this.consoleText.split("\n");
        int lineHeight = 10;
        int consoleTextY = consoleY + 18;
        int maxVisibleLines = (BOTTOM_BAR_HEIGHT - 25) / lineHeight;
        
        // Enable scissor to clip text within console bounds
        guiGraphics.enableScissor(SIDEBAR_WIDTH + 5, consoleTextY, this.width - 5, this.height - 5);
        
        int startLine = Math.max(0, Math.min(consoleScrollY, allLines.length - maxVisibleLines));
        for (int i = 0; i < maxVisibleLines && (startLine + i) < allLines.length; i++) {
            String line = allLines[startLine + i];
            // Truncate line if too long
            int maxWidth = this.width - SIDEBAR_WIDTH - 20;
            if (this.font.width(line) > maxWidth) {
                line = this.font.plainSubstrByWidth(line, maxWidth - 10) + "...";
            }
            guiGraphics.drawString(this.font, line, SIDEBAR_WIDTH + 10, consoleTextY + (i * lineHeight), 0xFFCCCCCC);
        }
        
        guiGraphics.disableScissor();
        
        // Scroll indicator if more lines available
        if (allLines.length > maxVisibleLines) {
            int scrollIndicatorColor = 0xFF666666;
            if (consoleScrollY > 0) {
                guiGraphics.drawString(this.font, "▲", this.width - 15, consoleY + 5, scrollIndicatorColor);
            }
            if (consoleScrollY + maxVisibleLines < allLines.length) {
                guiGraphics.drawString(this.font, "▼", this.width - 15, this.height - 12, scrollIndicatorColor);
            }
        }
        
        // 7. Title in Top Bar - REMOVED as requested
        // if (currentProblem != null) {
        //    guiGraphics.drawString(this.font, currentProblem.title + (showingDescription ? " (Description)" : " (Code)"), SIDEBAR_WIDTH + 260, 15, 0xFFFFFFFF);
        // }

        // 8. Render Widgets (Editor, Buttons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         // Main background color (Editor area)
         guiGraphics.fill(0, 0, this.width, this.height, 0xFF1E1E1E);
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
