package com.crabmods.algocraft.client.gui.modern;

import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.network.PacketSetSolvingState;
import com.crabmods.algocraft.network.PacketSolveProblem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ModernAlgorithmScreen extends Screen {
    private MultiLineEditBox codeEditor;
    private Problem currentProblem;
    private int currentProblemIndex = 0;
    private String outputText = "";
    
    // Animation states
    private boolean showSuccess = false;
    private float successAnimTick = 0;
    
    // Colors
    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int SIDEBAR_COLOR = 0xFF252526;
    private static final int TEXT_COLOR = 0xFFCCCCCC;
    private static final int SUCCESS_COLOR = 0xFF4CAF50;
    private static final int LINE_NUMBER_COLOR = 0xFF858585;

    public ModernAlgorithmScreen() {
        super(Component.literal("AlgoCraft Modern"));
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new PacketSetSolvingState(true));
        
        List<Problem> problems = ProblemManager.getProblems();
        if (!problems.isEmpty()) {
            if (currentProblemIndex >= problems.size()) currentProblemIndex = 0;
            currentProblem = problems.get(currentProblemIndex);
        }

        int editorX = this.width / 3 + 25; // Shift for line numbers
        int editorWidth = this.width * 2 / 3 - 45;
        int editorHeight = this.height - 60;

        // Code Editor
        this.codeEditor = new MultiLineEditBox(this.font, editorX, 40, editorWidth, editorHeight, Component.literal(""), Component.literal("Code"));
        if (currentProblem != null) this.codeEditor.setValue(currentProblem.initialCode);
        
        // Note: Syntax highlighting is disabled in In-Game IDE due to vanilla widget limitations.
        // Use Web IDE for full experience.
        
        this.addRenderableWidget(this.codeEditor);

        // Modern Buttons
        int buttonY = this.height - 30;
        
        this.addRenderableWidget(Button.builder(Component.literal("Run Code"), b -> runCode())
                .bounds(this.width - 220, buttonY, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Submit"), b -> submitCode())
                .bounds(this.width - 110, buttonY, 100, 20)
                .build());
                
        // Navigation
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> prevProblem())
                .bounds(10, this.height - 30, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> nextProblem())
                .bounds(40, this.height - 30, 20, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void prevProblem() {
        List<Problem> problems = ProblemManager.getProblems();
        if (!problems.isEmpty()) {
            currentProblemIndex = (currentProblemIndex - 1 + problems.size()) % problems.size();
            rebuildWidgets();
        }
    }

    private void nextProblem() {
        List<Problem> problems = ProblemManager.getProblems();
        if (!problems.isEmpty()) {
            currentProblemIndex = (currentProblemIndex + 1) % problems.size();
            rebuildWidgets();
        }
    }

    private void runCode() {
        if (currentProblem == null) return;
        String code = this.codeEditor.getValue();
        StringBuilder sb = new StringBuilder();
        for (Problem.TestCase test : currentProblem.examples) {
            String res = CodeExecutor.execute(code, test.input, test.output);
            sb.append("Input: ").append(test.input).append("\n");
            sb.append("Result: ").append(res).append("\n\n");
        }
        outputText = sb.toString();
    }

    private void submitCode() {
        if (currentProblem == null) return;
        String code = this.codeEditor.getValue();
        SubmissionResult result = Judge.grade(currentProblem, code);
        
        if (result.isSuccess) {
            showSuccess = true;
            successAnimTick = 0;
            PacketDistributor.sendToServer(new PacketSolveProblem(currentProblem.id, currentProblem.difficulty));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Status: ").append(result.message).append("\n");
            for (SubmissionResult.TestCaseResult detail : result.details) {
                if (!detail.passed) {
                    sb.append("Failed on: ").append(detail.input).append("\n");
                    sb.append("Expected: ").append(detail.expected).append("\n");
                    sb.append("Got: ").append(detail.actual).append("\n");
                    break;
                }
            }
            outputText = sb.toString();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 1. Draw Backgrounds
        guiGraphics.fill(0, 0, this.width, this.height, BG_COLOR); // Main BG
        guiGraphics.fill(0, 0, this.width / 3 - 10, this.height, SIDEBAR_COLOR); // Sidebar

        // 2. Draw Problem Info (Sidebar)
        if (currentProblem != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
            guiGraphics.drawString(this.font, currentProblem.title, 10, 10, 0xFFFFFF);
            guiGraphics.pose().popPose();
            
            guiGraphics.drawWordWrap(this.font, Component.literal(currentProblem.description), 10, 40, this.width / 3 - 30, TEXT_COLOR);
            
            // Draw Output Area in Sidebar
            int outputY = this.height / 2;
            guiGraphics.fill(10, outputY, this.width / 3 - 20, this.height - 40, 0xFF000000);
            guiGraphics.drawString(this.font, "Output:", 15, outputY + 5, 0xAAAAAA);
            guiGraphics.drawWordWrap(this.font, Component.literal(outputText), 15, outputY + 20, this.width / 3 - 40, 0x00FF00);
        }

        // 3. Render Widgets (Editor, Buttons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 4. Render Line Numbers Gutter
        int editorX = this.codeEditor.getX();
        int editorY = this.codeEditor.getY();
        int editorHeight = this.codeEditor.getHeight();
        
        // Draw Line Numbers Gutter
        guiGraphics.fill(editorX - 25, editorY, editorX, editorY + editorHeight, 0xFF252526);
        
        String[] lines = this.codeEditor.getValue().split("\n", -1);
        int lineHeight = 9; // Default font height
        
        for (int i = 0; i < lines.length; i++) {
            int y = editorY + 4 + (i * lineHeight);
            if (y > editorY + editorHeight) break; // Clip
            
            // Draw Line Number
            String lineNum = String.valueOf(i + 1);
            guiGraphics.drawString(this.font, lineNum, editorX - 5 - this.font.width(lineNum), y, LINE_NUMBER_COLOR);
        }
        
        // 5. Success Animation
        if (showSuccess) {
            renderSuccessAnimation(guiGraphics, partialTick);
        }
    }
    
    private void renderSuccessAnimation(GuiGraphics guiGraphics, float partialTick) {
        float scale = Mth.clamp((successAnimTick + partialTick) / 10.0f, 0.0f, 1.0f);
        scale = (float) Math.sin(scale * Math.PI / 2); // Ease out
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 0);
        guiGraphics.pose().scale(scale * 4.0f, scale * 4.0f, 1.0f);
        
        guiGraphics.drawCenteredString(this.font, "ACCEPTED", 0, -10, SUCCESS_COLOR);
        
        guiGraphics.pose().popPose();
    }
    
    @Override
    public void tick() {
        super.tick();
        if (showSuccess) {
            successAnimTick++;
            if (successAnimTick > 60) {
                showSuccess = false;
            }
        }
    }
    
    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new PacketSetSolvingState(false));
        super.onClose();
    }
}
