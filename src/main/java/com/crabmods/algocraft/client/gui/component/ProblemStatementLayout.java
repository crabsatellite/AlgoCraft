package com.crabmods.algocraft.client.gui.component;

public final class ProblemStatementLayout {
    private ProblemStatementLayout() {
    }

    public static int contentWidth(int widgetWidth, int horizontalPadding, int scrollbarReserve) {
        if (widgetWidth <= 0 || horizontalPadding < 0 || scrollbarReserve < 0) {
            return 1;
        }
        return Math.max(1, widgetWidth - horizontalPadding * 2 - scrollbarReserve);
    }

    public static int framedImageMaxWidth(int blockWidth, int framePadding) {
        if (blockWidth <= 0 || framePadding < 0) {
            return 1;
        }
        return Math.max(1, blockWidth - framePadding * 2);
    }

    public static int viewportHeight(int widgetHeight, int verticalPadding, int headerHeight) {
        if (widgetHeight <= 0 || verticalPadding < 0 || headerHeight < 0) {
            return 1;
        }
        return Math.max(1, widgetHeight - verticalPadding * 2 - headerHeight);
    }

    public static int clampScroll(int requestedScroll, int contentHeight, int viewportHeight) {
        if (requestedScroll <= 0 || contentHeight <= 0 || viewportHeight <= 0) {
            return 0;
        }
        return Math.min(requestedScroll, Math.max(0, contentHeight - viewportHeight));
    }

    public static boolean blockVisibleAfterScroll(int blockTop, int blockHeight, int scrollY, int viewportHeight) {
        if (blockHeight <= 0 || viewportHeight <= 0) {
            return false;
        }
        int viewportTop = Math.max(0, scrollY);
        int viewportBottom = viewportTop + viewportHeight;
        int blockBottom = blockTop + blockHeight;
        return blockBottom >= viewportTop && blockTop <= viewportBottom;
    }

    public static ScaledImage scaleImage(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
        if (imageWidth <= 0 || imageHeight <= 0 || maxWidth <= 0 || maxHeight <= 0) {
            return new ScaledImage(1, 1);
        }

        double scale = Math.min(1.0D, Math.min(maxWidth / (double) imageWidth, maxHeight / (double) imageHeight));
        return new ScaledImage(Math.max(1, (int) Math.round(imageWidth * scale)), Math.max(1, (int) Math.round(imageHeight * scale)));
    }

    public record ScaledImage(int width, int height) {
    }
}
