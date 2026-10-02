package com.crabmods.algocraft.client.gui.modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Flat IDE-styled button. Behaviour is identical to a vanilla {@link Button}. */
public class IdeButton extends Button {
    public enum Style {
        NEUTRAL,
        PRIMARY,
        SUBTLE
    }

    private final Style style;

    public IdeButton(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public static IdeButton of(Component message, OnPress onPress, int x, int y, int width, int height) {
        return new IdeButton(x, y, width, height, message, onPress, Style.NEUTRAL);
    }

    public static IdeButton primary(Component message, OnPress onPress, int x, int y, int width, int height) {
        return new IdeButton(x, y, width, height, message, onPress, Style.PRIMARY);
    }

    public static IdeButton subtle(Component message, OnPress onPress, int x, int y, int width, int height) {
        return new IdeButton(x, y, width, height, message, onPress, Style.SUBTLE);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean hot = this.active && this.isHovered();
        int fill;
        int border;
        int text;
        if (!this.active) {
            fill = IdeTheme.PANEL;
            border = IdeTheme.BORDER;
            text = IdeTheme.TEXT_DIM;
        } else if (this.style == Style.PRIMARY) {
            fill = hot ? IdeTheme.ACCENT_FILL_HOVER : IdeTheme.ACCENT_FILL;
            border = hot ? 0xFF56D364 : IdeTheme.ACCENT_BORDER;
            text = 0xFFFFFFFF;
        } else if (this.style == Style.SUBTLE) {
            fill = hot ? IdeTheme.RAISED : IdeTheme.PANEL;
            border = hot ? IdeTheme.BORDER_STRONG : IdeTheme.BORDER;
            text = hot ? IdeTheme.TEXT : IdeTheme.TEXT_MUTED;
        } else {
            fill = hot ? IdeTheme.RAISED_HOVER : IdeTheme.RAISED;
            border = hot ? 0xFF8B949E : IdeTheme.BORDER_STRONG;
            text = IdeTheme.TEXT;
        }
        if (this.active && this.isFocused() && !hot) {
            border = IdeTheme.INFO;
        }
        IdeTheme.frame(graphics, getX(), getY(), this.width, this.height, fill, border);
        String label = IdeTheme.ellipsize(font, getMessage().getString(), this.width - 6);
        int textX = getX() + (this.width - font.width(label) + 1) / 2;
        int textY = getY() + (this.height - 8) / 2;
        graphics.drawString(font, label, textX, textY, text, false);
    }
}
