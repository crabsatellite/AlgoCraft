package com.crabmods.algocraft.client.gui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeEditorTextNormalizerTest {
    @Test
    void nullAndEmptyTextNormalizeToEmptyText() {
        assertEquals("", CodeEditorTextNormalizer.normalizeLineEndings(null));
        assertEquals("", CodeEditorTextNormalizer.normalizeLineEndings(""));
    }

    @Test
    void windowsAndClassicMacLineEndingsNormalizeToUnixNewlines() {
        assertEquals(
                "class Solution {\n  int answer;\n}",
                CodeEditorTextNormalizer.normalizeLineEndings("class Solution {\r\n  int answer;\r}"));
    }

    @Test
    void existingUnixNewlinesArePreserved() {
        assertEquals(
                "return 1;\nreturn 2;\n",
                CodeEditorTextNormalizer.normalizeLineEndings("return 1;\nreturn 2;\n"));
    }

    @Test
    void externalEditorBomAndZeroWidthCharactersAreRemoved() {
        assertEquals(
                "class Solution { return 1; }",
                CodeEditorTextNormalizer.normalizeLineEndings(
                        "\uFEFFclass Solu\u200Btion { ret\u200Curn 1; }\u2060"));
    }

    @Test
    void webCopiedNonBreakingSpacesBecomePlainSpaces() {
        assertEquals(
                "class Solution {\n    return 1;\n}",
                CodeEditorTextNormalizer.normalizeLineEndings(
                        "class\u00A0Solution\u00A0{\r\n\u202F\u202F\u202F\u202Freturn\u00A01;\r}"));
    }
}
