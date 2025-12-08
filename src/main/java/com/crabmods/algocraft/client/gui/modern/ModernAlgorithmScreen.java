package com.crabmods.algocraft.client.gui.modern;

import com.crabmods.algocraft.client.gui.component.CodeEditorWidget;
import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.ProgressManager;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.network.PacketSolveProblem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ModernAlgorithmScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;
    private static final int TOP_BAR_HEIGHT = 40;
    private static final int BOTTOM_BAR_HEIGHT = 120;
    
    private CodeEditorWidget codeEditor;
    private MultiLineEditBox descriptionViewer;
    private String consoleText = "Ready...";
    private Problem currentProblem;
    private boolean showingDescription = false;

    public ModernAlgorithmScreen() {
        super(Component.literal("AlgoCraft IDE"));
    }

    @Override
    protected void init() {
        int editorWidth = this.width - SIDEBAR_WIDTH - 20;
        int editorHeight = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 20;

        // Code Editor
        this.codeEditor = new CodeEditorWidget(this.font, SIDEBAR_WIDTH + 10, TOP_BAR_HEIGHT + 10, editorWidth, editorHeight, Component.literal("Code"));
        this.addRenderableWidget(this.codeEditor);

        // Description Viewer (Initially hidden)
        this.descriptionViewer = new MultiLineEditBox(this.font, SIDEBAR_WIDTH + 10, TOP_BAR_HEIGHT + 10, editorWidth, editorHeight, Component.literal(""), Component.literal("Description"));
        this.descriptionViewer.visible = false;
        this.addRenderableWidget(this.descriptionViewer);

        // Load Problems
        List<Problem> problems = ProblemManager.getProblems();
        if (!problems.isEmpty()) {
            selectProblem(problems.get(0));
        }

        // Run Button
        this.addRenderableWidget(Button.builder(Component.literal("▶ Run"), button -> {
            if (currentProblem == null) return;
            runExamples();
        }).bounds(SIDEBAR_WIDTH + 10, 10, 60, 20).build());

        // Submit Button
        this.addRenderableWidget(Button.builder(Component.literal("✔ Submit"), button -> {
            if (currentProblem == null) return;
            submitSolution();
        }).bounds(SIDEBAR_WIDTH + 80, 10, 70, 20).build());
        
        // Toggle View Button
        this.addRenderableWidget(Button.builder(Component.literal("Toggle View"), button -> {
            showingDescription = !showingDescription;
            codeEditor.visible = !showingDescription;
            descriptionViewer.visible = showingDescription;
        }).bounds(SIDEBAR_WIDTH + 160, 10, 80, 20).build());

        // Close Button
        this.addRenderableWidget(Button.builder(Component.literal("❌ Close"), button -> {
            this.onClose();
        }).bounds(this.width - 70, 10, 60, 20).build());
        
        // Sidebar Buttons (Problem List)
        int y = 50;
        for (Problem problem : problems) {
            net.minecraft.network.chat.MutableComponent label = Component.literal(problem.title);
            if (ProgressManager.isPassed(problem.id)) {
                label.append(Component.literal(" ✔").withStyle(net.minecraft.ChatFormatting.GREEN));
            }
            
            this.addRenderableWidget(Button.builder(label, button -> {
                selectProblem(problem);
            }).bounds(10, y, SIDEBAR_WIDTH - 20, 20).build());
            y += 25;
        }
    }

    private void selectProblem(Problem problem) {
        this.currentProblem = problem;
        this.codeEditor.setValue(problem.initialCode);
        this.descriptionViewer.setValue(problem.description);
        this.consoleText = "Loaded problem: " + problem.title;
    }

    private void runExamples() {
        StringBuilder sb = new StringBuilder();
        sb.append("Running Examples for ").append(currentProblem.title).append("...\n");
        
        if (currentProblem.examples != null) {
            for (Problem.TestCase test : currentProblem.examples) {
                String result = CodeExecutor.execute(codeEditor.getValue(), test.input, test.output);
                sb.append("Input: ").append(test.input).append(" | Output: ").append(result).append("\n");
            }
        } else {
            sb.append("No examples found.");
        }
        this.consoleText = sb.toString();
    }

    private void submitSolution() {
        this.consoleText = "Submitting...";
        SubmissionResult result = Judge.grade(currentProblem, codeEditor.getValue());
        
        StringBuilder sb = new StringBuilder();
        if (result.isSuccess) {
            sb.append("SUCCESS! All tests passed.\n");
            // Send packet to server to reward player
            PacketDistributor.sendToServer(new PacketSolveProblem(currentProblem.id, currentProblem.difficulty));
        } else {
            sb.append("FAILED. ").append(result.message != null ? result.message : "").append("\n");
        }
        sb.append("Passed: ").append(result.passedCount).append("/").append(result.totalCount).append("\n");
        sb.append("Time: ").append(result.executionTimeMs).append("ms");
        
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
        guiGraphics.drawCenteredString(this.font, "PROBLEMS", SIDEBAR_WIDTH / 2, 15, 0xFFAAAAAA);
        guiGraphics.drawString(this.font, "Available:", 10, 35, 0xFF888888);

        // 6. Console Header & Text
        guiGraphics.drawString(this.font, "TERMINAL", SIDEBAR_WIDTH + 10, consoleY + 5, 0xFFAAAAAA);
        
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
