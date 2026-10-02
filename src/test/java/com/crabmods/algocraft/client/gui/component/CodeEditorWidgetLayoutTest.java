package com.crabmods.algocraft.client.gui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeEditorWidgetLayoutTest {
    private static final int LINE_NUMBER_WIDTH = 25;

    @Test
    void horizontalScrollClampsToZeroWhenLineFitsTextArea() {
        int widgetWidth = 240;
        int fittingLineWidth = CodeEditorViewport.textAreaWidth(widgetWidth, LINE_NUMBER_WIDTH) - 4;

        assertEquals(0, CodeEditorViewport.maxHorizontalScroll(widgetWidth, LINE_NUMBER_WIDTH, fittingLineWidth));
        assertEquals(0, CodeEditorViewport.clampHorizontalScroll(10_000, widgetWidth, LINE_NUMBER_WIDTH, fittingLineWidth));
    }

    @Test
    void horizontalScrollCannotMovePastLongestLine() {
        int widgetWidth = 120;
        int maxLineWidth = 600;
        int maxScroll = CodeEditorViewport.maxHorizontalScroll(widgetWidth, LINE_NUMBER_WIDTH, maxLineWidth);

        assertEquals(maxScroll, CodeEditorViewport.clampHorizontalScroll(10_000, widgetWidth, LINE_NUMBER_WIDTH, maxLineWidth));
        assertEquals(0, CodeEditorViewport.clampHorizontalScroll(-10_000, widgetWidth, LINE_NUMBER_WIDTH, maxLineWidth));
        assertEquals(80, CodeEditorViewport.clampHorizontalScroll(80, widgetWidth, LINE_NUMBER_WIDTH, maxLineWidth));
    }

    @Test
    void horizontalScrollUsesMinimumTextAreaForExtremelyNarrowEditor() {
        int maxScroll = CodeEditorViewport.maxHorizontalScroll(12, LINE_NUMBER_WIDTH, 200);

        assertEquals(184, maxScroll);
        assertEquals(184, CodeEditorViewport.clampHorizontalScroll(500, 12, LINE_NUMBER_WIDTH, 200));
    }
}
