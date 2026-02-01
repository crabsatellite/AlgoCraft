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
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm");

        public Entry(SubmissionRecord record) {
            this.record = record;
        }

        @Override
        public Component getNarration() {
            return Component.literal(record.getProblemTitle());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            int color = record.getStatus().equals("Accepted") ? 0xFF55FF55 : 0xFFFF5555;
            
            // Responsive column widths based on available width
            int col1Width = (int)(width * 0.35);  // Problem title
            int col2Width = (int)(width * 0.25);  // Status
            int col3Width = (int)(width * 0.25);  // Date
            int col4Width = (int)(width * 0.15);  // Time
            
            int col1X = left + 5;
            int col2X = col1X + col1Width;
            int col3X = col2X + col2Width;
            int col4X = col3X + col3Width;
            
            // Truncate problem title if too long
            String title = record.getProblemTitle();
            if (font.width(title) > col1Width - 10) {
                title = font.plainSubstrByWidth(title, col1Width - 15) + "...";
            }
            
            // Truncate status if needed
            String status = record.getStatus();
            if (font.width(status) > col2Width - 10) {
                status = font.plainSubstrByWidth(status, col2Width - 15) + "...";
            }
            
            guiGraphics.drawString(font, title, col1X, top + 2, 0xFFFFFFFF);
            guiGraphics.drawString(font, status, col2X, top + 2, color);
            guiGraphics.drawString(font, dateFormat.format(new Date(record.getTimestamp())), col3X, top + 2, 0xFFAAAAAA);
            guiGraphics.drawString(font, record.getExecutionTime() + "ms", col4X, top + 2, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }
}
