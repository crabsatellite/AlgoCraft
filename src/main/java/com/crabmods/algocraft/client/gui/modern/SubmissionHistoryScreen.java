package com.crabmods.algocraft.client.gui.modern;

import com.crabmods.algocraft.client.gui.component.SubmissionHistoryList;
import com.crabmods.algocraft.logic.SubmissionHistoryManager;
import com.crabmods.algocraft.logic.SubmissionRecord;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SubmissionHistoryScreen extends Screen {
    private final Screen parent;
    private SubmissionHistoryList historyList;

    public SubmissionHistoryScreen(Screen parent) {
        super(Component.translatable("algocraft.gui.history"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.historyList = new SubmissionHistoryList(this.minecraft, this.width, Math.max(1, this.height - 80), 40, 20);
        this.addRenderableWidget(this.historyList);

        for (SubmissionRecord record : SubmissionHistoryManager.getHistory()) {
            this.historyList.addRecord(record);
        }

        this.addRenderableWidget(IdeButton.of(Component.translatable("algocraft.gui.back"), button -> {
            this.minecraft.setScreen(this.parent);
        }, this.width / 2 - 100, this.height - 30, 200, 20));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, IdeTheme.TEXT);
        if (this.historyList != null) {
            this.historyList.renderColumnHeaders(guiGraphics, 28);
            if (this.historyList.children().isEmpty()) {
                guiGraphics.drawCenteredString(this.font, Component.translatable("algocraft.gui.history.empty"), this.width / 2, 60, IdeTheme.TEXT_MUTED);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, IdeTheme.BACKGROUND);
        guiGraphics.fill(0, 0, this.width, 40, IdeTheme.PANEL_HEADER);
        guiGraphics.fill(0, 40, this.width, 41, IdeTheme.BORDER);
        guiGraphics.fill(0, this.height - 40, this.width, this.height, IdeTheme.PANEL_HEADER);
        guiGraphics.fill(0, this.height - 40, this.width, this.height - 39, IdeTheme.BORDER);
    }
}
