package com.crabmods.algocraft.client.gui.component;

final class CodeEditorViewport {
    static final int MIN_TEXT_AREA_WIDTH = 20;
    static final int TEXT_AREA_RIGHT_PADDING = 4;

    private CodeEditorViewport() {
    }

    static int textAreaWidth(int widgetWidth, int lineNumberWidth) {
        return Math.max(MIN_TEXT_AREA_WIDTH, widgetWidth - lineNumberWidth - 12);
    }

    static int maxHorizontalScroll(int widgetWidth, int lineNumberWidth, int maxLineWidth) {
        return Math.max(0, maxLineWidth - textAreaWidth(widgetWidth, lineNumberWidth) + TEXT_AREA_RIGHT_PADDING);
    }

    static int clampHorizontalScroll(int requestedScrollX, int widgetWidth, int lineNumberWidth, int maxLineWidth) {
        return Math.max(0, Math.min(requestedScrollX, maxHorizontalScroll(widgetWidth, lineNumberWidth, maxLineWidth)));
    }
}
