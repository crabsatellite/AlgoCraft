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
        this.historyList = new SubmissionHistoryList(this.minecraft, this.width, this.height, 40, 20);
        this.addRenderableWidget(this.historyList);

        for (SubmissionRecord record : SubmissionHistoryManager.getHistory()) {
            this.historyList.addRecord(record);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.back"), button -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.historyList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         guiGraphics.fill(0, 0, this.width, this.height, 0xFF1E1E1E);
    }
}
