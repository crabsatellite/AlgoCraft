package com.crabmods.algocraft.client.gui.component;

import com.crabmods.algocraft.logic.SubmissionRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SubmissionHistoryList extends ObjectSelectionList<SubmissionHistoryList.Entry> {

    public SubmissionHistoryList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
        super(minecraft, width, height, top, itemHeight);
    }

    public void addRecord(SubmissionRecord record) {
        this.addEntry(new Entry(record));
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {
        private final SubmissionRecord record;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public Entry(SubmissionRecord record) {
            this.record = record;
        }

        @Override
        public Component getNarration() {
            return Component.literal(record.problemTitle);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int color = record.status.equals("Accepted") ? 0xFF55FF55 : 0xFFFF5555;
            
            guiGraphics.drawString(Minecraft.getInstance().font, record.problemTitle, left + 5, top + 2, 0xFFFFFFFF);
            guiGraphics.drawString(Minecraft.getInstance().font, record.status, left + 150, top + 2, color);
            guiGraphics.drawString(Minecraft.getInstance().font, dateFormat.format(new Date(record.timestamp)), left + 250, top + 2, 0xFFAAAAAA);
            guiGraphics.drawString(Minecraft.getInstance().font, record.executionTime + "ms", left + 400, top + 2, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }
}
