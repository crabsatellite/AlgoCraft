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

    public ModernAlgorithmScreen() {
        super(Component.translatable("algocraft.gui.ide_title"));
        this.consoleText = Component.translatable("algocraft.gui.ready").getString();
    }

    @Override
    protected void init() {
        int editorWidth = this.width - SIDEBAR_WIDTH - 20;
        int editorHeight = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 20;

        // Code Editor
        this.codeEditor = new CodeEditorWidget(this.font, SIDEBAR_WIDTH + 10, TOP_BAR_HEIGHT + 10, editorWidth, editorHeight, Component.translatable("algocraft.gui.code"));
        this.addRenderableWidget(this.codeEditor);

        // Description Viewer (Initially hidden)
        this.descriptionViewer = new MultiLineEditBox(this.font, SIDEBAR_WIDTH + 10, TOP_BAR_HEIGHT + 10, editorWidth, editorHeight, Component.literal(""), Component.translatable("algocraft.gui.description"));
        this.descriptionViewer.visible = false;
        this.addRenderableWidget(this.descriptionViewer);

        // Load Problems
        List<Problem> problems = ProblemManager.getProblems();
        if (!problems.isEmpty()) {
            selectProblem(problems.get(0));
        }

        // Run Button
        this.addRenderableWidget(Button.builder(Component.literal("▶ ").append(Component.translatable("algocraft.gui.run")), button -> {
            if (currentProblem == null) return;
            runExamples();
        }).bounds(SIDEBAR_WIDTH + 10, 5, 60, 20).build());

        // Submit Button
        this.addRenderableWidget(Button.builder(Component.literal("✔ ").append(Component.translatable("algocraft.gui.submit")), button -> {
            if (currentProblem == null) return;
            submitSolution();
        }).bounds(SIDEBAR_WIDTH + 75, 5, 70, 20).build());
        
        // History Button
        this.addRenderableWidget(Button.builder(Component.literal("🕒 ").append(Component.translatable("algocraft.gui.history")), button -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(new SubmissionHistoryScreen(this));
        }).bounds(SIDEBAR_WIDTH + 150, 5, 70, 20).build());

        // Toggle View Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.toggle_view"), button -> {
            showingDescription = !showingDescription;
            codeEditor.visible = !showingDescription;
            descriptionViewer.visible = showingDescription;
        }).bounds(SIDEBAR_WIDTH + 225, 5, 80, 20).build());

        // Close Button
        this.addRenderableWidget(Button.builder(Component.literal("X"), button -> {
            this.onClose();
        }).bounds(this.width - 25, 5, 20, 20).build());
        
        // Search Box
        this.searchBox = new net.minecraft.client.gui.components.EditBox(this.font, 10, 10, SIDEBAR_WIDTH - 20, 20, Component.translatable("algocraft.gui.search"));
        this.searchBox.setResponder(this::updateProblemList);
        this.addRenderableWidget(this.searchBox);

        // Repository Selector
        List<ProblemRepository> repos = ProblemManager.getRepositories();
        this.repositorySelector = CycleButton.builder((ProblemRepository repo) -> Component.literal(repo.getName()))
            .withValues(repos)
            .withInitialValue(repos.get(0))
            .create(10, 35, SIDEBAR_WIDTH - 20, 20, Component.empty(), (btn, val) -> {
                this.currentRepository = val;
                this.updateProblemList(this.searchBox.getValue());
            });
        this.addRenderableWidget(this.repositorySelector);
        this.currentRepository = this.repositorySelector.getValue();

        // Import Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.import"), button -> {
            Minecraft.getInstance().setScreen(new ImportProblemScreen(this));
        }).bounds(10, 60, SIDEBAR_WIDTH - 20, 20).build());
        
        // Problem List
        this.problemList = new ProblemSelectionList(this.minecraft, SIDEBAR_WIDTH, this.height, 85, 30);
        this.addRenderableWidget(this.problemList);
        
        updateProblemList("");
    }

    private void updateProblemList(String filter) {
        this.problemList.clearProblems();
        if (this.currentRepository == null) return;
        
        String lowerFilter = filter.toLowerCase();
        
        for (Problem problem : this.currentRepository.getProblems()) {
            boolean match = filter.isEmpty() || 
                            problem.title.toLowerCase().contains(lowerFilter) ||
                            (problem.tags != null && problem.tags.stream().anyMatch(t -> t.toLowerCase().contains(lowerFilter)));
            
            if (match) {
                this.problemList.addProblem(problem, this::selectProblem);
            }
        }
    }

    private void selectProblem(Problem problem) {
        this.currentProblem = problem;
        this.codeEditor.setValue(problem.initialCode);
        this.descriptionViewer.setValue(problem.description);
        this.consoleText = Component.translatable("algocraft.gui.loaded_problem", problem.title).getString();
    }

    private void runExamples() {
        StringBuilder sb = new StringBuilder();
        sb.append(Component.translatable("algocraft.gui.running_examples", currentProblem.title).getString()).append("\n");
        
        if (currentProblem.examples != null) {
            for (Problem.TestCase test : currentProblem.examples) {
                String result = CodeExecutor.execute(codeEditor.getValue(), test.input, test.output);
                sb.append(Component.translatable("algocraft.gui.input", test.input).getString())
                  .append(" | ")
                  .append(Component.translatable("algocraft.gui.result", result).getString())
                  .append("\n");
            }
        } else {
            sb.append(Component.translatable("algocraft.gui.no_examples").getString());
        }
        this.consoleText = sb.toString();
    }

    private void submitSolution() {
        this.consoleText = Component.translatable("algocraft.gui.submitting").getString();
        SubmissionResult result = Judge.grade(currentProblem, codeEditor.getValue());
        
        // Save submission record
        com.crabmods.algocraft.logic.SubmissionHistoryManager.saveRecord(new com.crabmods.algocraft.logic.SubmissionRecord(
            System.currentTimeMillis(),
            currentProblem.id,
            currentProblem.title,
            result.isSuccess ? "Accepted" : (result.message != null ? result.message : "Wrong Answer"),
            result.executionTimeMs,
            result.passedCount,
            result.totalCount
        ));

        StringBuilder sb = new StringBuilder();
        if (result.isSuccess) {
            sb.append(Component.translatable("algocraft.gui.success_all_passed").getString()).append("\n");
            // Send packet to server to reward player
            PacketDistributor.sendToServer(new PacketSolveProblem(currentProblem.id, currentProblem.difficulty));
        } else {
            sb.append(Component.translatable("algocraft.msg.submission_failed").getString()).append(" ").append(result.message != null ? result.message : "").append("\n");
        }
        sb.append(Component.translatable("algocraft.gui.passed", result.passedCount, result.totalCount).getString()).append("\n");
        sb.append(Component.translatable("algocraft.gui.time", result.executionTimeMs).getString());
        
        this.consoleText = sb.toString();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // CodeEditorWidget handles its own keys, but we can intercept global shortcuts here if needed
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        
        // Split console text by newlines and render
        String[] lines = this.consoleText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (consoleY + 20 + (i * 10) > this.height - 5) break; // Clip
            guiGraphics.drawString(this.font, lines[i], SIDEBAR_WIDTH + 10, consoleY + 20 + (i * 10), 0xFFCCCCCC);
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
