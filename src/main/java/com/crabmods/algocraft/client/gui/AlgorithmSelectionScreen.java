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
        super(Component.translatable("algocraft.gui.select_mode"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        
        // Ensure buttons don't overflow on narrow screens
        if (buttonWidth > this.width - 40) {
            buttonWidth = this.width - 40;
        }

        // Button 1: Open In-Game IDE
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.open_ingame_ide"), button -> {
            Minecraft.getInstance().setScreen(new ModernAlgorithmScreen());
        }).bounds(centerX - buttonWidth / 2, centerY - 30, buttonWidth, 20).build());

        // Button 2: Open Web IDE
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.open_web_ide"), button -> {
            Util.getPlatform().openUri("http://localhost:3000");
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("algocraft.msg.opened_web_ide"), true);
            }
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, centerY + 10, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
    }
}
