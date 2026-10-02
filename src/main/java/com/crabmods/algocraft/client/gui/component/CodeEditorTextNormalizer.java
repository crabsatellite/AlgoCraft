package com.crabmods.algocraft.client.gui.component;

final class CodeEditorTextNormalizer {
    private CodeEditorTextNormalizer() {
    }

    static String normalizeLineEndings(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\uFEFF", "")
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u2060", "");
    }
}
