package com.crabmods.algocraft.client.gui.component;

import com.crabmods.algocraft.client.gui.modern.IdeTheme;
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

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), IdeTheme.BACKGROUND);
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    @Override
    public int getRowWidth() {
        return Math.min(560, this.width - 24);
    }

    /** Column labels drawn above the list, aligned with each row's columns. */
    public void renderColumnHeaders(GuiGraphics guiGraphics, int y) {
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        int left = this.getRowLeft();
        int width = this.getRowWidth();
        int[] columns = columnStarts(left, width);
        String[] keys = {"algocraft.gui.history.problem", "algocraft.gui.history.status", "algocraft.gui.history.date", "algocraft.gui.history.time"};
        for (int i = 0; i < keys.length; i++) {
            guiGraphics.drawString(font, Component.translatable(keys[i]), columns[i], y, IdeTheme.TEXT_DIM, false);
        }
    }

    private static int[] columnStarts(int left, int width) {
        int col1X = left + 5;
        int col2X = col1X + (int) (width * 0.35);
        int col3X = col2X + (int) (width * 0.25);
        int col4X = col3X + (int) (width * 0.25);
        return new int[] {col1X, col2X, col3X, col4X};
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
            int color = record.getStatus().equals("Accepted") ? IdeTheme.ACCENT : IdeTheme.DANGER;
            if (index % 2 == 1) {
                guiGraphics.fill(left - 2, top - 2, left + width - 2, top + height - 2, IdeTheme.withAlpha(IdeTheme.PANEL, 0xC0));
            }
            
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
            if (font.width(status) > col2Width - 18) {
                status = font.plainSubstrByWidth(status, col2Width - 23) + "...";
            }
            
            guiGraphics.drawString(font, title, col1X, top + 4, IdeTheme.TEXT, false);
            guiGraphics.fill(col2X, top + 6, col2X + 4, top + 10, color);
            guiGraphics.drawString(font, status, col2X + 8, top + 4, color, false);
            guiGraphics.drawString(font, dateFormat.format(new Date(record.getTimestamp())), col3X, top + 4, IdeTheme.TEXT_MUTED, false);
            guiGraphics.drawString(font, record.getExecutionTime() + "ms", col4X, top + 4, IdeTheme.TEXT_MUTED, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }
}
