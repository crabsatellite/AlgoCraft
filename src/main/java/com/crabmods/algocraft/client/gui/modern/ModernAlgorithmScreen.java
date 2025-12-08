package com.crabmods.algocraft.client.gui.modern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModernAlgorithmScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;
    private static final int TOP_BAR_HEIGHT = 40;
    private static final int BOTTOM_BAR_HEIGHT = 120;
    
    private MultiLineEditBox codeEditor;
    private String consoleText = "Ready...";
    private String currentFile = "Solution.java";

    public ModernAlgorithmScreen() {
        super(Component.literal("AlgoCraft IDE"));
    }

    @Override
    protected void init() {
        int editorWidth = this.width - SIDEBAR_WIDTH - 20;
        int editorHeight = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 20;

        // Code Editor
        // Note: MultiLineEditBox might need a specific constructor or builder depending on exact version mappings.
        // Assuming standard 1.21 constructor: Font, x, y, width, height, placeholder, message
        this.codeEditor = new MultiLineEditBox(this.font, SIDEBAR_WIDTH + 10, TOP_BAR_HEIGHT + 10, editorWidth, editorHeight, Component.literal(""), Component.literal("Code"));
        this.codeEditor.setValue("public class Solution {\n    public static void main(String[] args) {\n        System.out.println(\"Hello AlgoCraft!\");\n    }\n}");
        this.addRenderableWidget(this.codeEditor);

        // Run Button
        this.addRenderableWidget(Button.builder(Component.literal("▶ Run Code"), button -> {
            this.consoleText = "Compiling " + currentFile + "...\n> Hello AlgoCraft!\n> Process finished with exit code 0";
        }).bounds(SIDEBAR_WIDTH + 10, 10, 80, 20).build());

        // Save Button
        this.addRenderableWidget(Button.builder(Component.literal("💾 Save"), button -> {
            this.consoleText = "Saved " + currentFile;
        }).bounds(SIDEBAR_WIDTH + 100, 10, 60, 20).build());
        
        // Settings Button
        this.addRenderableWidget(Button.builder(Component.literal("⚙ Settings"), button -> {
            this.consoleText = "Settings opened (Not implemented)";
        }).bounds(SIDEBAR_WIDTH + 170, 10, 80, 20).build());

        // Close Button
        this.addRenderableWidget(Button.builder(Component.literal("❌ Close"), button -> {
            this.onClose();
        }).bounds(this.width - 70, 10, 60, 20).build());
        
        // Sidebar Buttons (Mock Files)
        this.addRenderableWidget(Button.builder(Component.literal("Solution.java"), button -> {
            this.currentFile = "Solution.java";
            this.codeEditor.setValue("public class Solution {\n    public static void main(String[] args) {\n        System.out.println(\"Hello AlgoCraft!\");\n    }\n}");
        }).bounds(10, 50, SIDEBAR_WIDTH - 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Problem.md"), button -> {
            this.currentFile = "Problem.md";
            this.codeEditor.setValue("# Two Sum\n\nGiven an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.");
        }).bounds(10, 75, SIDEBAR_WIDTH - 20, 20).build());
        
        this.addRenderableWidget(Button.builder(Component.literal("TestCases.json"), button -> {
            this.currentFile = "TestCases.json";
            this.codeEditor.setValue("{\n  \"tests\": [\n    { \"input\": [2, 7, 11, 15], \"target\": 9, \"output\": [0, 1] }\n  ]\n}");
        }).bounds(10, 100, SIDEBAR_WIDTH - 20, 20).build());
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
        guiGraphics.drawCenteredString(this.font, "PROJECT EXPLORER", SIDEBAR_WIDTH / 2, 15, 0xFFAAAAAA);
        guiGraphics.drawString(this.font, "Files:", 10, 35, 0xFF888888);

        // 6. Console Header & Text
        guiGraphics.drawString(this.font, "TERMINAL", SIDEBAR_WIDTH + 10, consoleY + 5, 0xFFAAAAAA);
        
        // Split console text by newlines and render
        String[] lines = this.consoleText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            guiGraphics.drawString(this.font, lines[i], SIDEBAR_WIDTH + 10, consoleY + 20 + (i * 10), 0xFFCCCCCC);
        }

        // 7. Render Widgets (Editor, Buttons)
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
