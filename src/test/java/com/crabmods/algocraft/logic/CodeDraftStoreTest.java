package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class CodeDraftStoreTest {
    @TempDir Path directory;

    @Test void switchingAndReopeningPreservesIndependentDraftsIncludingEmptyCode() throws IOException {
        Path file = directory.resolve("drafts.json");
        CodeDraftStore store = new CodeDraftStore(file);
        store.put("1", "class Solution { // 中文\n}\n");
        store.put("user_1", "");
        store.save();
        CodeDraftStore reopened = new CodeDraftStore(file);
        assertEquals("class Solution { // 中文\n}\n", reopened.getOrDefault("1", "template"));
        assertEquals("", reopened.getOrDefault("user_1", "template"));
        assertEquals("template", reopened.getOrDefault("2", "template"));
    }

    @Test void corruptDraftsAreNeverSilentlyOverwritten() throws IOException {
        Path file = directory.resolve("drafts.json");
        Files.writeString(file, "{broken");
        assertThrows(IOException.class, () -> new CodeDraftStore(file));
        assertEquals("{broken", Files.readString(file));
    }
}
