package com.crabmods.algocraft.client.gui;

import com.crabmods.algocraft.client.gui.modern.ModernAlgorithmScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AlgorithmSelectionScreen extends Screen {

    public AlgorithmSelectionScreen() {
        super(Component.literal("Select IDE Mode"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Button 1: Open In-Game IDE
        this.addRenderableWidget(Button.builder(Component.literal("Open In-Game IDE"), button -> {
            Minecraft.getInstance().setScreen(new ModernAlgorithmScreen());
        }).bounds(centerX - 100, centerY - 30, 200, 20).build());

        // Button 2: Open Web IDE
        this.addRenderableWidget(Button.builder(Component.literal("Open Web IDE"), button -> {
            Util.getPlatform().openUri("http://localhost:3000");
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Opened AlgoCraft IDE in your browser!"), true);
            this.onClose();
        }).bounds(centerX - 100, centerY + 10, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
    }
}
