package com.crabmods.algocraft.client.gui.modern;

import java.util.ArrayList;
import java.util.List;

public final class IdeLayoutEngine {
    public static final int SIDEBAR_WIDTH = 150;
    public static final int TOP_BAR_HEIGHT = 32;
    public static final int BOTTOM_BAR_HEIGHT = 86;
    public static final int PROBLEM_LIST_TOP = 107;
    private static final int MIN_SPLIT_WIDTH = 760;
    private static final int MIN_EDITOR_WIDTH = 320;

    private IdeLayoutEngine() {
    }

    public static PanelLayout calculatePanels(int screenWidth, int screenHeight, boolean showingDescription) {
        int mainX = SIDEBAR_WIDTH + 10;
        int topBarHeight = topBarHeight(screenWidth);
        int mainY = topBarHeight + 10;
        int mainWidth = Math.max(120, screenWidth - SIDEBAR_WIDTH - 20);
        int mainHeight = Math.max(1, screenHeight - topBarHeight - consoleHeight(screenHeight) - 20);
        boolean split = screenWidth >= MIN_SPLIT_WIDTH && !showingDescription;

        if (!split) {
            return new PanelLayout(
                    new Rect(mainX, mainY, mainWidth, mainHeight),
                    new Rect(mainX, mainY, mainWidth, mainHeight),
                    showingDescription,
                    !showingDescription
            );
        }

        int problemWidth = clamp(mainWidth / 3, 230, 320);
        int editorWidth = mainWidth - problemWidth - 8;
        if (editorWidth < MIN_EDITOR_WIDTH) {
            problemWidth = Math.max(180, mainWidth - MIN_EDITOR_WIDTH - 8);
            editorWidth = mainWidth - problemWidth - 8;
        }

        return new PanelLayout(
                new Rect(mainX, mainY, problemWidth, mainHeight),
                new Rect(mainX + problemWidth + 8, mainY, editorWidth, mainHeight),
                true,
                true
        );
    }

    public static List<ButtonLayout> calculateTopButtons(int screenWidth) {
        int availableWidth = Math.max(1, screenWidth - SIDEBAR_WIDTH - 30);
        int buttonY = 5;
        int buttonHeight = 20;
        int buttonSpacing = 4;
        int closeButtonX = screenWidth - 25;
        int buttonRightLimit = closeButtonX - buttonSpacing;

        int runWidth = Math.max(30, Math.min(64, availableWidth / 7));
        int submitWidth = Math.max(42, Math.min(76, availableWidth / 6));
        int historyWidth = Math.max(46, Math.min(76, availableWidth / 6));
        int toggleWidth = Math.max(38, Math.min(68, availableWidth / 7));
        int webWidth = Math.max(32, Math.min(58, availableWidth / 8));

        List<ButtonLayout> buttons = new ArrayList<>();
        int buttonX = SIDEBAR_WIDTH + 10;
        int[] widths = {runWidth, submitWidth, historyWidth, toggleWidth, webWidth};
        Control[] controls = {Control.RUN, Control.SUBMIT, Control.HISTORY, Control.TOGGLE_VIEW, Control.WEB};
        for (int i = 0; i < controls.length; i++) {
            if (buttonX + widths[i] > buttonRightLimit) {
                buttonX = SIDEBAR_WIDTH + 10;
                buttonY += buttonHeight + buttonSpacing;
            }
            buttons.add(new ButtonLayout(controls[i], new Rect(buttonX, buttonY, widths[i], buttonHeight)));
            buttonX += widths[i] + buttonSpacing;
        }
        buttons.add(new ButtonLayout(Control.CLOSE, new Rect(closeButtonX, 5, 20, buttonHeight)));
        return List.copyOf(buttons);
    }

    public static int topBarHeight(int screenWidth) {
        return calculateTopButtons(screenWidth).stream().mapToInt(button -> button.rect().bottom() + 7)
                .max().orElse(TOP_BAR_HEIGHT);
    }

    public static int consoleHeight(int screenHeight) {
        return Math.min(BOTTOM_BAR_HEIGHT, Math.max(50, screenHeight / 4));
    }

    public static Rect calculateProblemList(int screenHeight) {
        int safeHeight = Math.max(1, screenHeight);
        int top = Math.min(PROBLEM_LIST_TOP, safeHeight - 1);
        return new Rect(0, top, SIDEBAR_WIDTH, Math.max(1, safeHeight - top));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum Control {
        RUN,
        SUBMIT,
        HISTORY,
        TOGGLE_VIEW,
        WEB,
        CLOSE
    }

    public record PanelLayout(Rect problem, Rect editor, boolean problemVisible, boolean editorVisible) {
        public int problemX() {
            return problem.x();
        }

        public int problemY() {
            return problem.y();
        }

        public int problemWidth() {
            return problem.width();
        }

        public int problemHeight() {
            return problem.height();
        }

        public int editorX() {
            return editor.x();
        }

        public int editorY() {
            return editor.y();
        }

        public int editorWidth() {
            return editor.width();
        }

        public int editorHeight() {
            return editor.height();
        }
    }

    public record ButtonLayout(Control control, Rect rect) {
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean hasPositiveSize() {
            return width > 0 && height > 0;
        }

        public boolean overlaps(Rect other) {
            return this.x < other.right()
                    && this.right() > other.x
                    && this.y < other.bottom()
                    && this.bottom() > other.y;
        }
    }
}
