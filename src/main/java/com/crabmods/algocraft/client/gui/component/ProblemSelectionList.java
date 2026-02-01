package com.crabmods.algocraft.client.gui.component;

import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProgressManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

public class ProblemSelectionList extends ObjectSelectionList<ProblemSelectionList.Entry> {

    public ProblemSelectionList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
        super(minecraft, width, height, top, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRight() - 6;
    }

    public void addProblem(Problem problem, OnSelect onSelect) {
        this.addEntry(new Entry(problem, onSelect));
    }
    
    public void clearProblems() {
        this.clearEntries();
    }

    public interface OnSelect {
        void onSelect(Problem problem);
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {
        private final Problem problem;
        private final OnSelect onSelect;
        private long hoverStartTime = -1;

        public Entry(Problem problem, OnSelect onSelect) {
            this.problem = problem;
            this.onSelect = onSelect;
        }

        @Override
        public Component getNarration() {
            return Component.literal(problem.getTitle());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int color = 0xFFFFFF;
            boolean passed = ProgressManager.isPassed(problem.getId());
            if (passed) {
                color = 0x55FF55;
            }
            
            // Title (Top)
            String title = problem.getTitle();
            int titleWidth = Minecraft.getInstance().font.width(title);
            int maxTitleWidth = width - 20; // Reserve space for checkmark
            
            if (titleWidth > maxTitleWidth) {
                // Simple scrolling effect if hovered, otherwise truncate
                if (hovering) {
                    if (hoverStartTime == -1) {
                        hoverStartTime = System.currentTimeMillis();
                    }
                    long time = System.currentTimeMillis() - hoverStartTime;
                    // Wait 1s, then scroll, then wait 1s at end
                    int scrollDuration = (titleWidth - maxTitleWidth) * 20 + 2000; 
                    int scroll = 0;
                    
                    if (time > 1000) {
                        scroll = (int) ((time - 1000) / 20);
                        if (scroll > titleWidth - maxTitleWidth) {
                            scroll = titleWidth - maxTitleWidth;
                        }
                    }
                    
                    // Clip to bounds
                    guiGraphics.enableScissor(left + 5, top, left + 5 + maxTitleWidth, top + height);
                    guiGraphics.drawString(Minecraft.getInstance().font, title, left + 5 - scroll, top + 2, color);
                    guiGraphics.disableScissor();
                } else {
                    hoverStartTime = -1;
                    // Truncate
                    String truncated = Minecraft.getInstance().font.plainSubstrByWidth(title, maxTitleWidth - 5) + "...";
                    guiGraphics.drawString(Minecraft.getInstance().font, truncated, left + 5, top + 2, color);
                }
            } else {
                hoverStartTime = -1;
                guiGraphics.drawString(Minecraft.getInstance().font, title, left + 5, top + 2, color);
            }
            
            // Checkmark
            if (passed) {
                guiGraphics.drawString(Minecraft.getInstance().font, "✔", left + width - 15, top + 2, 0x55FF55);
            }
            
            // Difficulty (Bottom)
            String diff = problem.getDifficulty();
            int diffColor = 0xAAAAAA;
            if ("EASY".equalsIgnoreCase(diff)) diffColor = 0x55FF55;
            else if ("HARD".equalsIgnoreCase(diff)) diffColor = 0xFF5555;
            else if ("MEDIUM".equalsIgnoreCase(diff)) diffColor = 0xFFFF55;
            
            guiGraphics.drawString(Minecraft.getInstance().font, diff, left + 5, top + 14, diffColor);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                onSelect.onSelect(problem);
                ProblemSelectionList.this.setSelected(this);
                return true;
            }
            return false;
        }
    }
}
