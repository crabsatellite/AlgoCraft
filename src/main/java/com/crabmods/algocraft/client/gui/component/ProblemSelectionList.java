package com.crabmods.algocraft.client.gui.component;

import com.crabmods.algocraft.client.gui.modern.IdeTheme;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProgressManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

public class ProblemSelectionList extends LegacySelectionList<ProblemSelectionList.Entry> {

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

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), IdeTheme.SIDEBAR);
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
        guiGraphics.fill(this.getX(), this.getY() - 1, this.getRight(), this.getY(), IdeTheme.BORDER);
    }

    @Override
    protected void renderSelection(GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {
        int left = this.getX() + 4;
        int right = this.getRight() - 9;
        guiGraphics.fill(left, top - 2, right, top + height + 2, IdeTheme.RAISED);
        guiGraphics.fill(left, top - 2, left + 2, top + height + 2, IdeTheme.ACCENT);
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Repaint the vanilla scroller sprite in the IDE style.
        int maxScroll = this.getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int x = this.getScrollbarPosition();
        guiGraphics.fill(x, this.getY(), x + 6, this.getBottom(), IdeTheme.SIDEBAR);
        int thumbHeight = Math.max(24, Math.min(this.height - 8, this.height * this.height / Math.max(1, this.getMaxPosition())));
        int thumbTop = this.getY() + (int) this.getScrollAmount() * (this.height - thumbHeight) / maxScroll;
        boolean hot = mouseX >= x && mouseX < x + 6 && mouseY >= this.getY() && mouseY < this.getBottom();
        IdeTheme.scrollbar(guiGraphics, x + 2, this.getY(), this.getBottom(), thumbTop, thumbTop + thumbHeight, hot);
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
            return Component.literal(ProblemDisplayText.title(problem));
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int color = IdeTheme.TEXT;
            boolean passed = ProgressManager.isPassed(problem.getId());
            if (hovering && !ProblemSelectionList.this.isSelectedItem(index)) {
                guiGraphics.fill(left - 1, top - 2, left + width - 5, top + height + 2, IdeTheme.withAlpha(IdeTheme.RAISED, 0xB0));
            }
            
            // Title (Top)
            String title = ProblemDisplayText.title(problem);
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
                    guiGraphics.drawString(Minecraft.getInstance().font, title, left + 5 - scroll, top + 3, color, false);
                    guiGraphics.disableScissor();
                } else {
                    hoverStartTime = -1;
                    // Truncate
                    String truncated = Minecraft.getInstance().font.plainSubstrByWidth(title, maxTitleWidth - 5) + "...";
                    guiGraphics.drawString(Minecraft.getInstance().font, truncated, left + 5, top + 3, color, false);
                }
            } else {
                hoverStartTime = -1;
                guiGraphics.drawString(Minecraft.getInstance().font, title, left + 5, top + 3, color, false);
            }
            
            // Checkmark
            if (passed) {
                net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
                String solved = Component.translatable("algocraft.gui.solved_short").getString();
                IdeTheme.pill(guiGraphics, font, solved, left + width - font.width(solved) - 18, top + 13, IdeTheme.ACCENT);
            }
            
            // Difficulty (Bottom)
            String diff = problem.getDifficulty();
            int diffColor = IdeTheme.difficultyColor(diff);
            guiGraphics.fill(left + 5, top + 17, left + 8, top + 20, diffColor);
            guiGraphics.drawString(Minecraft.getInstance().font, diff == null ? "" : diff, left + 11, top + 15, IdeTheme.TEXT_MUTED, false);
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
