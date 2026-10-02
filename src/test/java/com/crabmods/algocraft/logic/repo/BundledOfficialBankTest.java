package com.crabmods.algocraft.logic.repo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class BundledOfficialBankTest {
    @TempDir Path directory;
    @Test void firstInstallContainsAllQuestionsAndReopeningPreservesCache() throws Exception {
        Path cache = directory.resolve("official");
        assertTrue(BundledOfficialBank.installIfEmpty(cache));
        try (var files = Files.list(cache)) {
            assertEquals(500, files.filter(p -> p.getFileName().toString().matches("p\\d+\\.json")).count());
        }
        assertTrue(Files.isRegularFile(cache.resolve("images/p7_example1.png")));
        Files.writeString(cache.resolve("player-cache-marker.txt"), "preserve");
        assertFalse(BundledOfficialBank.installIfEmpty(cache));
        assertEquals("preserve", Files.readString(cache.resolve("player-cache-marker.txt")));
    }
    @Test void existingPartialCacheIsNotReplaced() throws Exception {
        Path cache = directory.resolve("official");
        Files.createDirectories(cache);
        Files.writeString(cache.resolve("p1.json"), "existing bytes");
        assertFalse(BundledOfficialBank.installIfEmpty(cache));
        assertEquals("existing bytes", Files.readString(cache.resolve("p1.json")));
    }
}
