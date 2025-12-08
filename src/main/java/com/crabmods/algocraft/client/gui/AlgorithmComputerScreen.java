package com.crabmods.algocraft.client.gui;

import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.network.PacketSetSolvingState;
import com.crabmods.algocraft.network.PacketSolveProblem;
import net.neoforged.neoforge.network.PacketDistributor;

public class AlgorithmComputerScreen extends Screen {
    private MultiLineEditBox codeEditor;
    private Problem currentProblem;
    private int currentProblemIndex = 0;
    private String outputText = "";

    public AlgorithmComputerScreen() {
        super(Component.translatable("algocraft.gui.title"));
    }

    @Override
    protected void init() {
        // Notify server that we are solving
        PacketDistributor.sendToServer(new PacketSetSolvingState(true));

        List<Problem> problems = ProblemManager.getProblems();
        if (!problems.isEmpty()) {
            if (currentProblemIndex >= problems.size()) currentProblemIndex = 0;
            currentProblem = problems.get(currentProblemIndex);
        }

        // Problem Navigation
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            if (!problems.isEmpty()) {
                currentProblemIndex = (currentProblemIndex - 1 + problems.size()) % problems.size();
                rebuildWidgets();
            }
        }).bounds(10, 50, 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            if (!problems.isEmpty()) {
                currentProblemIndex = (currentProblemIndex + 1) % problems.size();
                rebuildWidgets();
            }
        }).bounds(this.width / 4 - 30, 50, 20, 20).build());

        // Code Editor
        this.codeEditor = new MultiLineEditBox(this.font, this.width / 4, 40, this.width / 2, this.height - 80, Component.translatable("algocraft.gui.code"), Component.translatable("algocraft.gui.code"));
        if (currentProblem != null) this.codeEditor.setValue(currentProblem.initialCode);
        this.addRenderableWidget(this.codeEditor);

        // Run Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.run"), button -> {
            runCode();
        }).bounds(this.width - 100, this.height - 40, 80, 20).build());

        // Submit Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.submit"), button -> {
            submitCode();
        }).bounds(this.width - 190, this.height - 40, 80, 20).build());
    }

    private void runCode() {
        if (currentProblem == null) return;
        String code = this.codeEditor.getValue();
        StringBuilder sb = new StringBuilder();
        for (Problem.TestCase test : currentProblem.examples) {
            String res = CodeExecutor.execute(code, test.input, test.output);
            sb.append(Component.translatable("algocraft.gui.input", test.input).getString()).append("\n");
            sb.append(Component.translatable("algocraft.gui.result", res).getString()).append("\n\n");
        }
        outputText = sb.toString();
    }

    private void submitCode() {
        if (currentProblem == null) return;
        String code = this.codeEditor.getValue();
        
        SubmissionResult result = Judge.grade(currentProblem, code);
        
        StringBuilder sb = new StringBuilder();
        
        String statusKey = switch (result.message) {
            case "Accepted" -> "algocraft.msg.accepted";
            case "Wrong Answer" -> "algocraft.msg.wrong_answer";
            case "Runtime Error" -> "algocraft.msg.runtime_error";
            case "Hidden Test Failed" -> "algocraft.msg.hidden_failed";
            default -> "algocraft.msg.wrong_answer";
        };
        
        sb.append(Component.translatable("algocraft.gui.status", Component.translatable(statusKey)).getString()).append("\n");
        sb.append(Component.translatable("algocraft.gui.passed", result.passedCount, result.totalCount).getString()).append("\n");
        sb.append(Component.translatable("algocraft.gui.time", result.executionTimeMs).getString()).append("\n\n");
        
        if (!result.isSuccess) {
            sb.append(Component.translatable("algocraft.msg.submission_failed").getString()).append("\n");
            for (SubmissionResult.TestCaseResult detail : result.details) {
                if (!detail.passed) {
                    sb.append(Component.translatable("algocraft.gui.input", detail.input).getString()).append("\n");
                    sb.append(Component.translatable("algocraft.gui.expected", detail.expected).getString()).append("\n");
                    sb.append(Component.translatable("algocraft.gui.got", detail.actual).getString()).append("\n");
                    if (detail.error != null) sb.append(Component.translatable("algocraft.gui.error", detail.error).getString()).append("\n");
                    break; // Only show first failure
                }
            }
        } else {
            sb.append(Component.translatable("algocraft.msg.rewards_sent").getString());
            PacketDistributor.sendToServer(new PacketSolveProblem(currentProblem.id, currentProblem.difficulty));
        }
        
        outputText = sb.toString();
    }

    @Override
    public void onClose() {
        // Notify server that we stopped solving
        PacketDistributor.sendToServer(new PacketSetSolvingState(false));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw Problem Info
        if (currentProblem != null) {
            guiGraphics.drawString(this.font, currentProblem.title, 10, 10, 0xFFFFFF);
            guiGraphics.drawWordWrap(this.font, Component.literal(currentProblem.description), 10, 30, this.width / 4 - 20, 0xAAAAAA);
        }

        // Draw Output
        guiGraphics.drawWordWrap(this.font, Component.literal(outputText), this.width * 3 / 4 + 10, 40, this.width / 4 - 20, 0x00FF00);
    }
}
