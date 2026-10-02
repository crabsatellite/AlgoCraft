package com.crabmods.algocraft.client.gui.modern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdeLayoutEngineTest {
    static Stream<Arguments> representativeWindows() {
        return Stream.of(
                Arguments.of(320, 240),
                Arguments.of(420, 300),
                Arguments.of(640, 480),
                Arguments.of(759, 480),
                Arguments.of(760, 480),
                Arguments.of(1024, 600),
                Arguments.of(1920, 1080)
        );
    }

    @Test
    void wideWindowShowsSplitProblemAndEditorPanels() {
        IdeLayoutEngine.PanelLayout layout = IdeLayoutEngine.calculatePanels(1024, 600, false);

        assertTrue(layout.problemVisible());
        assertTrue(layout.editorVisible());
        assertTrue(layout.problem().hasPositiveSize());
        assertTrue(layout.editor().hasPositiveSize());
        assertFalse(layout.problem().overlaps(layout.editor()));
    }

    @Test
    void compactWindowUsesSingleVisiblePanel() {
        IdeLayoutEngine.PanelLayout codeLayout = IdeLayoutEngine.calculatePanels(640, 480, false);
        assertFalse(codeLayout.problemVisible());
        assertTrue(codeLayout.editorVisible());

        IdeLayoutEngine.PanelLayout descriptionLayout = IdeLayoutEngine.calculatePanels(640, 480, true);
        assertTrue(descriptionLayout.problemVisible());
        assertFalse(descriptionLayout.editorVisible());
    }

    @Test
    void normalToolbarExposesAllIdeControls() {
        List<IdeLayoutEngine.ButtonLayout> buttons = IdeLayoutEngine.calculateTopButtons(640);
        Set<IdeLayoutEngine.Control> controls = EnumSet.noneOf(IdeLayoutEngine.Control.class);
        for (IdeLayoutEngine.ButtonLayout button : buttons) {
            controls.add(button.control());
        }

        assertEquals(EnumSet.allOf(IdeLayoutEngine.Control.class), controls);
        assertButtonsDoNotOverlap(buttons);
    }

    @Test
    void narrowToolbarWrapsEveryControlWithoutOverlap() {
        List<IdeLayoutEngine.ButtonLayout> buttons = IdeLayoutEngine.calculateTopButtons(420);
        assertTrue(buttons.stream().anyMatch(button -> button.control() == IdeLayoutEngine.Control.CLOSE));
        assertTrue(buttons.stream().anyMatch(button -> button.control() == IdeLayoutEngine.Control.RUN));
        assertTrue(buttons.stream().anyMatch(button -> button.control() == IdeLayoutEngine.Control.SUBMIT));
        assertEquals(EnumSet.allOf(IdeLayoutEngine.Control.class),
                buttons.stream().map(IdeLayoutEngine.ButtonLayout::control).collect(java.util.stream.Collectors.toSet()));
        assertButtonsDoNotOverlap(buttons);
    }

    @Test
    void problemListBoundsStayPositiveEvenWhenWindowIsVeryShort() {
        for (int height : List.of(1, 60, 106, 107, 108, 240)) {
            IdeLayoutEngine.Rect bounds = IdeLayoutEngine.calculateProblemList(height);

            assertEquals(IdeLayoutEngine.SIDEBAR_WIDTH, bounds.width());
            assertTrue(bounds.hasPositiveSize(), "problem list should stay constructible at height " + height);
            assertTrue(bounds.y() >= 0, "problem list should stay inside the screen at height " + height);
            assertTrue(bounds.bottom() <= Math.max(1, height),
                    "problem list should not extend below the screen at height " + height);
        }
    }

    @ParameterizedTest(name = "{0}x{1}")
    @MethodSource("representativeWindows")
    void representativeWindowSizesKeepPanelsAndToolbarInsideScreen(int width, int height) {
        assertPanelLayoutFits(width, height, false);
        assertPanelLayoutFits(width, height, true);

        List<IdeLayoutEngine.ButtonLayout> buttons = IdeLayoutEngine.calculateTopButtons(width);
        assertFalse(buttons.isEmpty(), "toolbar should expose at least one control");
        assertTrue(buttons.stream().anyMatch(button -> button.control() == IdeLayoutEngine.Control.CLOSE),
                "close control must remain reachable");
        assertButtonsDoNotOverlap(buttons);
        for (IdeLayoutEngine.ButtonLayout button : buttons) {
            assertTrue(button.rect().x() >= IdeLayoutEngine.SIDEBAR_WIDTH,
                    button.control() + " should stay out of the sidebar at width " + width);
            assertTrue(button.rect().right() <= width,
                    button.control() + " should stay within screen width " + width);
        }

        IdeLayoutEngine.Rect problemList = IdeLayoutEngine.calculateProblemList(height);
        assertEquals(IdeLayoutEngine.SIDEBAR_WIDTH, problemList.width());
        assertTrue(problemList.hasPositiveSize());
        assertTrue(problemList.bottom() <= height);
    }

    @Test
    void densePlayableWindowRangeKeepsCoreIdeControlsReachable() {
        for (int width = 320; width <= 1920; width++) {
            List<IdeLayoutEngine.ButtonLayout> buttons = IdeLayoutEngine.calculateTopButtons(width);
            Set<IdeLayoutEngine.Control> controls = EnumSet.noneOf(IdeLayoutEngine.Control.class);
            for (IdeLayoutEngine.ButtonLayout button : buttons) {
                controls.add(button.control());
                assertTrue(button.rect().x() >= IdeLayoutEngine.SIDEBAR_WIDTH,
                        button.control() + " enters sidebar at width " + width);
                assertTrue(button.rect().right() <= width,
                        button.control() + " overflows screen at width " + width);
            }
            assertButtonsDoNotOverlap(buttons);
            assertTrue(controls.contains(IdeLayoutEngine.Control.RUN),
                    "run control must stay reachable at width " + width);
            assertTrue(controls.contains(IdeLayoutEngine.Control.SUBMIT),
                    "submit control must stay reachable at width " + width);
            assertTrue(controls.contains(IdeLayoutEngine.Control.CLOSE),
                    "close control must stay reachable at width " + width);
            assertEquals(EnumSet.allOf(IdeLayoutEngine.Control.class), controls,
                    "every playable width should expose every IDE control at width " + width);
        }

        for (int width = 320; width <= 1920; width += 7) {
            for (int height = 240; height <= 1080; height += 5) {
                assertPanelLayoutFits(width, height, false);
                assertPanelLayoutFits(width, height, true);

                IdeLayoutEngine.Rect problemList = IdeLayoutEngine.calculateProblemList(height);
                assertEquals(IdeLayoutEngine.SIDEBAR_WIDTH, problemList.width());
                assertTrue(problemList.hasPositiveSize(), "problem list collapsed at " + width + "x" + height);
                assertTrue(problemList.y() >= 0, "problem list starts above screen at " + width + "x" + height);
                assertTrue(problemList.bottom() <= height, "problem list overflows screen at " + width + "x" + height);
            }
        }
    }

    private static void assertPanelLayoutFits(int width, int height, boolean showingDescription) {
        IdeLayoutEngine.PanelLayout layout = IdeLayoutEngine.calculatePanels(width, height, showingDescription);
        if (layout.problemVisible()) {
            assertRectInside(layout.problem(), width, height);
        }
        if (layout.editorVisible()) {
            assertRectInside(layout.editor(), width, height);
        }
        if (layout.problemVisible() && layout.editorVisible()) {
            assertFalse(layout.problem().overlaps(layout.editor()));
        }
    }

    private static void assertRectInside(IdeLayoutEngine.Rect rect, int width, int height) {
        assertTrue(rect.hasPositiveSize(), "visible panel should have positive size");
        assertTrue(rect.x() >= 0 && rect.y() >= 0, "visible panel should start inside screen");
        assertTrue(rect.right() <= width, "visible panel should fit width");
        assertTrue(rect.bottom() <= height, "visible panel should fit height");
    }

    private static void assertButtonsDoNotOverlap(List<IdeLayoutEngine.ButtonLayout> buttons) {
        for (int i = 0; i < buttons.size(); i++) {
            assertTrue(buttons.get(i).rect().hasPositiveSize());
            for (int j = i + 1; j < buttons.size(); j++) {
                assertFalse(buttons.get(i).rect().overlaps(buttons.get(j).rect()),
                        buttons.get(i).control() + " overlaps " + buttons.get(j).control());
            }
        }
    }
}
