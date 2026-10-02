package com.crabmods.algocraft.client.gui.modern;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared colours and drawing helpers for the AlgoCraft IDE screens. The same
 * palette is used by the web IDE so both editors feel like one product.
 */
public final class IdeTheme {
    public static final int BACKGROUND = 0xFF0D1117;
    public static final int SIDEBAR = 0xFF11161D;
    public static final int PANEL = 0xFF161B22;
    public static final int PANEL_HEADER = 0xFF1A2029;
    public static final int RAISED = 0xFF21262D;
    public static final int RAISED_HOVER = 0xFF2B323B;
    public static final int BORDER = 0xFF30363D;
    public static final int BORDER_STRONG = 0xFF484F58;

    public static final int TEXT = 0xFFE6EDF3;
    public static final int TEXT_MUTED = 0xFF9DA7B3;
    public static final int TEXT_DIM = 0xFF6E7681;

    public static final int ACCENT = 0xFF3FB950;
    public static final int ACCENT_FILL = 0xFF238636;
    public static final int ACCENT_FILL_HOVER = 0xFF2EA043;
    public static final int ACCENT_BORDER = 0xFF3FB950;
    public static final int INFO = 0xFF58A6FF;
    public static final int WARNING = 0xFFD29922;
    public static final int DANGER = 0xFFF85149;

    public static final int EASY = 0xFF3FB950;
    public static final int MEDIUM = 0xFFD29922;
    public static final int HARD = 0xFFF85149;

    private IdeTheme() {
    }

    /** Filled rectangle with a 1px border and cut corners, which reads as slightly rounded. */
    public static void frame(GuiGraphics graphics, int x, int y, int width, int height, int fill, int border) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y, x + width - 1, y + 1, border);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, border);
        graphics.fill(x, y + 1, x + 1, y + height - 1, border);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, border);
    }

    /** Small tinted label such as a difficulty badge. Returns the badge width. */
    public static int pill(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        int width = font.width(text) + 8;
        frame(graphics, x, y, width, 11, withAlpha(color, 0x26), withAlpha(color, 0x8C));
        graphics.drawString(font, text, x + 4, y + 2, color, false);
        return width;
    }

    public static int difficultyColor(String difficulty) {
        if ("HARD".equalsIgnoreCase(difficulty)) {
            return HARD;
        }
        if ("MEDIUM".equalsIgnoreCase(difficulty)) {
            return MEDIUM;
        }
        if ("EASY".equalsIgnoreCase(difficulty)) {
            return EASY;
        }
        return TEXT_MUTED;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
    }

    public static String ellipsize(Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int available = maxWidth - font.width(ellipsis);
        if (available <= 0) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, available) + ellipsis;
    }

    /** Thin vertical scrollbar thumb on a track. */
    public static void scrollbar(GuiGraphics graphics, int x, int top, int bottom, int thumbTop, int thumbBottom, boolean hot) {
        graphics.fill(x, top, x + 3, bottom, 0x1FFFFFFF);
        graphics.fill(x, thumbTop, x + 3, thumbBottom, hot ? 0xFF8B949E : BORDER_STRONG);
    }
}
