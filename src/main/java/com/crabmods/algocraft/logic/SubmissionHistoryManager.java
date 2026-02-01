package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe manager for submission history.
 * Uses ReadWriteLock for efficient concurrent access.
 */
public class SubmissionHistoryManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_HISTORY_SIZE = 100;
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
            
            File configDir = new File(Minecraft.getInstance().gameDirectory, "config/algocraft");
            if (!configDir.exists() && !configDir.mkdirs()) {
                LOGGER.warn("Failed to create config directory: {}", configDir);
            }
            historyFile = new File(configDir, "submission_history.json");
            loadHistoryInternal();
            initialized = true;
        } finally {
            lock.writeLock().unlock();
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
                history.addAll(loaded);
                // Trim to max size if needed
                while (history.size() > MAX_HISTORY_SIZE) {
                    history.remove(history.size() - 1);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load submission history", e);
        } catch (JsonSyntaxException e) {
            LOGGER.error("Corrupted submission history file, starting fresh", e);
            history.clear();
        }
    }

    /**
     * Save a submission record. Thread-safe.
     * 
     * @param record The record to save
     */
    public static void saveRecord(SubmissionRecord record) {
        if (record == null) return;
        
        ensureInit();
        
        lock.writeLock().lock();
        try {
            history.add(0, record); // Add to beginning
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
        
        try (FileWriter writer = new FileWriter(historyFile)) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save submission history", e);
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
}
