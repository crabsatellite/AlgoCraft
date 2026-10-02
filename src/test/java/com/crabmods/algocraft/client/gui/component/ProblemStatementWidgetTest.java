package com.crabmods.algocraft.client.gui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemStatementWidgetTest {
    @Test
    void imageScalingPreservesAspectRatioAndBounds() {
        ProblemStatementLayout.ScaledImage wide = ProblemStatementLayout.scaleImage(800, 400, 240, 160);
        assertEquals(240, wide.width());
        assertEquals(120, wide.height());

        ProblemStatementLayout.ScaledImage tall = ProblemStatementLayout.scaleImage(200, 600, 240, 150);
        assertEquals(50, tall.width());
        assertEquals(150, tall.height());
    }

    @Test
    void imageScalingDoesNotUpscaleSmallImages() {
        ProblemStatementLayout.ScaledImage small = ProblemStatementLayout.scaleImage(80, 40, 240, 160);
        assertEquals(80, small.width());
        assertEquals(40, small.height());
    }

    @Test
    void contentWidthNeverExceedsAvailableWidgetInterior() {
        assertEquals(216, ProblemStatementLayout.contentWidth(240, 8, 8));
        assertEquals(1, ProblemStatementLayout.contentWidth(24, 8, 8));
        assertEquals(1, ProblemStatementLayout.contentWidth(16, 8, 8));
    }

    @Test
    void framedImageScalingKeepsRenderedBorderInsideContentBlock() {
        int blockWidth = 20;
        int imageMaxWidth = ProblemStatementLayout.framedImageMaxWidth(blockWidth, 2);

        ProblemStatementLayout.ScaledImage scaled = ProblemStatementLayout.scaleImage(800, 400, imageMaxWidth, 160);

        assertEquals(16, imageMaxWidth);
        assertEquals(16, scaled.width());
        assertEquals(8, scaled.height());
        assertEquals(blockWidth, scaled.width() + 4);
    }

    @Test
    void statementViewportHeightStaysPositiveForTinyWindows() {
        assertEquals(184, ProblemStatementLayout.viewportHeight(240, 8, 40));
        assertEquals(1, ProblemStatementLayout.viewportHeight(56, 8, 40));
        assertEquals(1, ProblemStatementLayout.viewportHeight(0, 8, 40));
    }

    @Test
    void statementScrollIsClampedInsideRenderableContent() {
        assertEquals(0, ProblemStatementLayout.clampScroll(-30, 600, 180));
        assertEquals(120, ProblemStatementLayout.clampScroll(120, 600, 180));
        assertEquals(420, ProblemStatementLayout.clampScroll(9_999, 600, 180));
        assertEquals(0, ProblemStatementLayout.clampScroll(80, 120, 180));
        assertEquals(0, ProblemStatementLayout.clampScroll(80, 0, 180));
    }

    @Test
    void statementImageVisibilityUsesScrollAdjustedViewport() {
        assertTrue(ProblemStatementLayout.blockVisibleAfterScroll(0, 80, 0, 120));
        assertTrue(ProblemStatementLayout.blockVisibleAfterScroll(160, 80, 120, 120));
        assertTrue(ProblemStatementLayout.blockVisibleAfterScroll(240, 80, 200, 120));

        assertFalse(ProblemStatementLayout.blockVisibleAfterScroll(240, 80, 0, 120));
        assertFalse(ProblemStatementLayout.blockVisibleAfterScroll(0, 80, 121, 120));
        assertFalse(ProblemStatementLayout.blockVisibleAfterScroll(40, 0, 0, 120));
    }
}
