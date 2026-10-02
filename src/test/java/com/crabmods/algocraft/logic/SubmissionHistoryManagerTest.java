package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionHistoryManagerTest {
    @TempDir
    Path tempConfigDir;

    @BeforeEach
    void setUp() {
        System.setProperty(ProgressManager.CONFIG_DIR_PROPERTY, tempConfigDir.toString());
        SubmissionHistoryManager.resetForTest();
    }

    @AfterEach
    void tearDown() {
        SubmissionHistoryManager.resetForTest();
        System.clearProperty(ProgressManager.CONFIG_DIR_PROPERTY);
    }

    @Test
    void corruptedHistoryFileDoesNotCrashHistoryQueries() throws Exception {
        Files.writeString(tempConfigDir.resolve("submission_history.json"), "[{not json", StandardCharsets.UTF_8);

        assertDoesNotThrow(SubmissionHistoryManager::getHistory);
        assertEquals(0, SubmissionHistoryManager.getHistorySize(),
                "corrupted submission history must fail closed instead of crashing the history screen");
    }

    @Test
    void loadedHistoryDropsNullAndInvalidRecordsBeforeUiCanRenderThem() throws Exception {
        Files.writeString(tempConfigDir.resolve("submission_history.json"), """
                [
                  null,
                  {"timestamp":100,"problemId":"1","problemTitle":"Two Sum","status":"Accepted","executionTime":5,"passedCount":2,"totalCount":2},
                  {"timestamp":101,"problemId":"2","problemTitle":"Broken","status":"Wrong Answer","executionTime":-1,"passedCount":0,"totalCount":1},
                  {"timestamp":102,"problemId":"3","problemTitle":"Impossible","status":"Wrong Answer","executionTime":1,"passedCount":2,"totalCount":1}
                ]
                """, StandardCharsets.UTF_8);

        List<SubmissionRecord> history = SubmissionHistoryManager.getHistory();

        assertEquals(1, history.size());
        assertEquals("1", history.get(0).getProblemId());
        assertEquals("Two Sum", history.get(0).getProblemTitle());
        assertEquals("Accepted", history.get(0).getStatus());
    }

    @Test
    void savedHistorySanitizesNullBlankAndOversizedTextBeforePersistence() {
        String oversizedTitle = "A".repeat(1_000);

        SubmissionHistoryManager.saveRecord(new SubmissionRecord(
                123L,
                "   ",
                oversizedTitle,
                "Wrong     Answer\nwith noisy whitespace",
                42L,
                0,
                3
        ));

        List<SubmissionRecord> history = SubmissionHistoryManager.getHistory();

        assertEquals(1, history.size());
        SubmissionRecord record = history.get(0);
        assertEquals("unknown", record.getProblemId());
        assertTrue(record.getProblemTitle().endsWith(" [truncated]"));
        assertTrue(record.getProblemTitle().length() <= 240,
                "history row text should stay bounded before the game UI measures it");
        assertEquals("Wrong Answer with noisy whitespace", record.getStatus());

        SubmissionHistoryManager.resetForTest();
        System.setProperty(ProgressManager.CONFIG_DIR_PROPERTY, tempConfigDir.toString());

        List<SubmissionRecord> reloaded = SubmissionHistoryManager.getHistory();
        assertEquals(1, reloaded.size());
        assertEquals("unknown", reloaded.get(0).getProblemId());
        assertTrue(reloaded.get(0).getProblemTitle().endsWith(" [truncated]"));
    }

    @Test
    void historySizeIsTrimmedAndEveryReturnedRecordIsRenderable() {
        for (int i = 0; i < 130; i++) {
            SubmissionHistoryManager.saveRecord(new SubmissionRecord(
                    i,
                    Integer.toString(i),
                    "Problem " + i,
                    i % 2 == 0 ? "Accepted" : "Wrong Answer",
                    i,
                    i % 2,
                    1
            ));
        }

        List<SubmissionRecord> history = SubmissionHistoryManager.getHistory();

        assertEquals(100, history.size());
        assertEquals("129", history.get(0).getProblemId());
        assertFalse(history.stream().anyMatch(record -> record == null
                || record.getProblemId() == null
                || record.getProblemTitle() == null
                || record.getStatus() == null));
        for (SubmissionRecord record : history) {
            assertNotNull(record.getProblemTitle());
            assertNotNull(record.getStatus());
        }
    }
}
