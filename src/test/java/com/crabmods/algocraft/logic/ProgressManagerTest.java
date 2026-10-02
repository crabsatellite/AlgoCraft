package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressManagerTest {
    @TempDir
    Path tempConfigDir;

    @BeforeEach
    void setUp() {
        System.setProperty(ProgressManager.CONFIG_DIR_PROPERTY, tempConfigDir.toString());
        ProgressManager.resetForTest();
    }

    @AfterEach
    void tearDown() {
        ProgressManager.resetForTest();
        System.clearProperty(ProgressManager.CONFIG_DIR_PROPERTY);
    }

    @Test
    void corruptedProgressFileDoesNotCrashProgressQueries() throws Exception {
        Files.writeString(tempConfigDir.resolve("user_progress.json"), "{not json", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> assertFalse(ProgressManager.isPassed("1")));
        assertEquals(0, ProgressManager.getPassedCount(),
                "corrupted progress should fail closed to an empty in-memory snapshot instead of crashing the IDE");
    }

    @Test
    void legacyProgressSetStillLoadsValidProblemIds() throws Exception {
        Files.writeString(tempConfigDir.resolve("user_progress.json"), "[\"1\",\"2\",\"   \"]", StandardCharsets.UTF_8);

        assertTrue(ProgressManager.isPassed("1"));
        assertTrue(ProgressManager.isPassed("2"));
        assertFalse(ProgressManager.isPassed("   "));
        assertEquals(2, ProgressManager.getPassedCount());
    }

    @Test
    void invalidServerProgressSyncDoesNotClearExistingProgress() {
        ProgressManager.markAsPassed("1");
        ProgressManager.saveProgressImmediate();

        Map<String, Long> hostileProgress = new LinkedHashMap<>();
        hostileProgress.put("2", 123L);
        hostileProgress.put(" ", 456L);

        assertDoesNotThrow(() -> ProgressManager.updateFromPacket(hostileProgress));

        assertTrue(ProgressManager.isPassed("1"),
                "invalid sync packets must not clear the player's previously solved problems");
        assertFalse(ProgressManager.isPassed("2"),
                "invalid sync packets must not partially publish valid-looking entries before failing");
        assertEquals(1, ProgressManager.getPassedCount());
    }

    @Test
    void validServerProgressSyncReplacesSnapshotAndPersists() {
        ProgressManager.markAsPassed("1");
        ProgressManager.saveProgressImmediate();

        ProgressManager.updateFromPacket(Map.of("2", 123L, "3", 456L));
        ProgressManager.saveProgressImmediate();

        ProgressManager.resetForTest();
        System.setProperty(ProgressManager.CONFIG_DIR_PROPERTY, tempConfigDir.toString());

        assertFalse(ProgressManager.isPassed("1"));
        assertTrue(ProgressManager.isPassed("2"));
        assertTrue(ProgressManager.isPassed("3"));
        assertEquals(2, ProgressManager.getPassedCount());
    }

    @Test
    void invalidProblemIdsAreIgnoredInsteadOfCrashingProgressSave() {
        assertDoesNotThrow(() -> ProgressManager.markAsPassed(null));
        assertDoesNotThrow(() -> ProgressManager.markAsPassed("   "));

        assertEquals(0, ProgressManager.getPassedCount());
    }
    @Test
    void serverSessionsDoNotOverwriteLocalPracticeOrLeakAcrossServers() throws Exception {
        ProgressManager.markAsPassed("practice:mine:1");
        ProgressManager.saveProgressImmediate();
        String original = Files.readString(tempConfigDir.resolve("user_progress.json"));
        ProgressManager.beginServerSession();
        ProgressManager.updateFromPacket(Map.of("1",123L));
        assertTrue(ProgressManager.isPassed("1"));
        assertTrue(ProgressManager.isPassed("practice:mine:1"));
        ProgressManager.saveProgressImmediate();
        assertEquals(original,Files.readString(tempConfigDir.resolve("user_progress.json")));
        ProgressManager.endServerSession();
        assertFalse(ProgressManager.isPassed("1"));
        assertTrue(ProgressManager.isPassed("practice:mine:1"));
        ProgressManager.beginServerSession();
        assertFalse(ProgressManager.isPassed("1"));
    }

}
