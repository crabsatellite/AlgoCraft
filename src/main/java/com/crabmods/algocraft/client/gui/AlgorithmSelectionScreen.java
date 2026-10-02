package com.crabmods.algocraft.client.gui;

import com.crabmods.algocraft.client.ClientHooks;
import com.crabmods.algocraft.client.gui.modern.IdeButton;
import com.crabmods.algocraft.client.gui.modern.IdeTheme;
import com.crabmods.algocraft.client.gui.modern.ModernAlgorithmScreen;
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

        // Button 1: Open the immersive in-game IDE.
        this.addRenderableWidget(IdeButton.primary(Component.translatable("algocraft.gui.open_ingame_ide"), button -> {
            Minecraft.getInstance().setScreen(new ModernAlgorithmScreen());
        }, centerX - buttonWidth / 2, centerY - 10, buttonWidth, 20));

        // Button 2: Open Web IDE backed by Monaco Editor.
        Button web = IdeButton.of(Component.translatable("algocraft.gui.open_web_ide"), button -> {
            ClientHooks.openWebIde();
            this.onClose();
        }, centerX - buttonWidth / 2, centerY + 20, buttonWidth, 20);
        web.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("algocraft.gui.web.tooltip")));
        this.addRenderableWidget(web);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, Component.translatable("itemGroup.algocraft"), this.width / 2, this.height / 2 - 48, IdeTheme.ACCENT);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 34, IdeTheme.TEXT);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, IdeTheme.withAlpha(IdeTheme.BACKGROUND, 0xE0));
        int cardWidth = Math.min(240, this.width - 16);
        int top = Math.max(0, this.height / 2 - 60);
        IdeTheme.frame(guiGraphics, this.width / 2 - cardWidth / 2, top, cardWidth, Math.min(this.height - top, 118), IdeTheme.PANEL, IdeTheme.BORDER);
        guiGraphics.fill(this.width / 2 - cardWidth / 2 + 1, top + 1, this.width / 2 + cardWidth / 2 - 1, top + 3, IdeTheme.ACCENT);
    }
}
