package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe manager for submission history.
 * Uses ReadWriteLock for efficient concurrent access.
 */
public class SubmissionHistoryManager {
    private static final System.Logger LOGGER = System.getLogger(SubmissionHistoryManager.class.getName());
    private static final int MAX_HISTORY_SIZE = 100;
    private static final int MAX_TEXT_LENGTH = 240;
    private static final String TRUNCATED_SUFFIX = " [truncated]";
    private static final List<SubmissionRecord> history = new ArrayList<>();
    private static final Gson gson = new Gson();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile File historyFile;
    private static volatile boolean initialized = false;

    /**
     * Initialize the history manager. Thread-safe and idempotent.
     */
    public static void init() {
        if (initialized) return;
        
        lock.writeLock().lock();
        try {
            if (initialized) return; // Double-check after acquiring lock
            
            File configDir = configDirectory();
            if (!configDir.exists() && !configDir.mkdirs()) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to create config directory: " + configDir);
            }
            historyFile = new File(configDir, "submission_history.json");
            loadHistoryInternal();
            initialized = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static File configDirectory() {
        String override = System.getProperty(ProgressManager.CONFIG_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return new File(override);
        }

        try {
            return FMLPaths.GAMEDIR.get().resolve("config/algocraft").toFile();
        } catch (Throwable e) {
            return new File(System.getProperty("java.io.tmpdir"), "algocraft/config/algocraft");
        }
    }

    private static void ensureInit() {
        if (!initialized) {
            init();
        }
    }

    /**
     * Internal method to load history. Must be called with write lock held.
     */
    private static void loadHistoryInternal() {
        if (historyFile == null || !historyFile.exists()) return;
        
        try (FileReader reader = new FileReader(historyFile)) {
            List<SubmissionRecord> loaded = gson.fromJson(reader, 
                new TypeToken<List<SubmissionRecord>>(){}.getType());
            if (loaded != null) {
                history.clear();
                for (SubmissionRecord record : loaded) {
                    SubmissionRecord sanitized = sanitizeRecord(record);
                    if (sanitized != null) {
                        history.add(sanitized);
                    }
                    if (history.size() >= MAX_HISTORY_SIZE) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to load submission history", e);
        } catch (JsonSyntaxException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Corrupted submission history file, starting fresh", e);
            history.clear();
        }
    }

    /**
     * Save a submission record. Thread-safe.
     * 
     * @param record The record to save
     */
    public static void saveRecord(SubmissionRecord record) {
        SubmissionRecord sanitized = sanitizeRecord(record);
        if (sanitized == null) return;
        
        ensureInit();
        
        lock.writeLock().lock();
        try {
            history.add(0, sanitized); // Add to beginning
            // Enforce max history size
            while (history.size() > MAX_HISTORY_SIZE) {
                history.remove(history.size() - 1);
            }
            saveHistoryInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Internal method to save history. Must be called with write lock held.
     */
    private static void saveHistoryInternal() {
        if (historyFile == null) return;
        
        try {
            writeHistoryAtomically(historyFile, new ArrayList<>(history));
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to save submission history", e);
        }
    }

    /**
     * Get a copy of the submission history. Thread-safe.
     * 
     * @return A new list containing all history records
     */
    public static List<SubmissionRecord> getHistory() {
        ensureInit();
        
        lock.readLock().lock();
        try {
            return new ArrayList<>(history);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Clear all submission history. Thread-safe.
     */
    public static void clearHistory() {
        ensureInit();
        
        lock.writeLock().lock();
        try {
            history.clear();
            saveHistoryInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get the number of history records. Thread-safe.
     * 
     * @return The number of records
     */
    public static int getHistorySize() {
        ensureInit();
        
        lock.readLock().lock();
        try {
            return history.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    static void resetForTest() {
        lock.writeLock().lock();
        try {
            history.clear();
            historyFile = null;
            initialized = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static SubmissionRecord sanitizeRecord(SubmissionRecord record) {
        if (record == null) {
            return null;
        }

        long timestamp = record.getTimestamp();
        long executionTime = record.getExecutionTime();
        int passedCount = record.getPassedCount();
        int totalCount = record.getTotalCount();
        if (timestamp < 0 || executionTime < 0 || passedCount < 0 || totalCount < 0 || passedCount > totalCount) {
            return null;
        }

        return new SubmissionRecord(
                timestamp,
                boundedText(record.getProblemId(), "unknown"),
                boundedText(record.getProblemTitle(), "Unknown"),
                boundedText(record.getStatus(), "Unknown"),
                executionTime,
                passedCount,
                totalCount
        );
    }

    private static String boundedText(String value, String fallback) {
        String text = value == null ? fallback : value.replaceAll("\\s+", " ").trim();
        if (text.isBlank()) {
            text = fallback;
        }
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        int prefixLength = Math.max(0, MAX_TEXT_LENGTH - TRUNCATED_SUFFIX.length());
        return text.substring(0, prefixLength) + TRUNCATED_SUFFIX;
    }

    private static void writeHistoryAtomically(File targetFile, List<SubmissionRecord> snapshot) throws IOException {
        Path target = targetFile.toPath();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, gson.toJson(snapshot), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // Best-effort cleanup; the target write path above is the critical operation.
            }
        }
    }
}
