package com.crabmods.algocraft.client.gui.component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
/** Retains shared viewport geometry and styling on the 1.20 selection-list API. */
public abstract class LegacySelectionList<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
    protected LegacySelectionList(Minecraft minecraft, int width, int viewportHeight, int top, int itemHeight) {
        super(minecraft, width, top + viewportHeight, top, top + viewportHeight, itemHeight);
        this.height = viewportHeight;
        setRenderBackground(false);
        setRenderTopAndBottom(false);
    }
    public int getX() { return x0; }
    public int getY() { return y0; }
    public int getRight() { return x1; }
    public int getBottom() { return y1; }
    protected abstract void renderListBackground(GuiGraphics graphics);
    protected abstract void renderListSeparators(GuiGraphics graphics);
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderListBackground(graphics);
        renderListSeparators(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
