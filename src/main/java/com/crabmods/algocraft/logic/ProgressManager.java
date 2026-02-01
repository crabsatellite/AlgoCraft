package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    // ProblemID -> LastSolvedTimestamp (thread-safe)
    private static final Map<String, Long> passedProblems = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static volatile File progressFile;
    
    // Debounce save operations to avoid excessive IO
    private static final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private static final long SAVE_DEBOUNCE_MS = 500;

    public static void init() {
        if (progressFile != null) return;
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config/algocraft");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        progressFile = new File(configDir, "user_progress.json");
        loadProgress();
    }

    private static void ensureInit() {
        if (progressFile == null) {
            init();
        }
    }

    private static void loadProgress() {
        if (progressFile.exists()) {
            try (FileReader reader = new FileReader(progressFile)) {
                // Try loading as Map first
                try {
                    Map<String, Long> loaded = gson.fromJson(reader, new TypeToken<Map<String, Long>>(){}.getType());
                    if (loaded != null) {
                        passedProblems.putAll(loaded);
                        return;
                    }
                } catch (Exception e) {
                    // Fallback for old format (Set<String>)
                }
                
                // Re-read for old format
                try (FileReader reader2 = new FileReader(progressFile)) {
                    Set<String> loadedOld = gson.fromJson(reader2, new TypeToken<Set<String>>(){}.getType());
                    if (loadedOld != null) {
                        for (String id : loadedOld) {
                            passedProblems.put(id, 0L);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load progress", e);
            }
        }
    }

    /**
     * Save progress asynchronously with debouncing.
     * This prevents excessive file IO when multiple problems are solved quickly.
     */
    public static void saveProgress() {
        // If a save is already scheduled, skip (debounce)
        if (!saveScheduled.compareAndSet(false, true)) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(SAVE_DEBOUNCE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            saveScheduled.set(false);
            saveProgressSync();
        });
    }
    
    /**
     * Synchronous save - called by async debouncer or for forced saves.
     */
    private static void saveProgressSync() {
        ensureInit();
        if (progressFile == null) return;
        
        try (FileWriter writer = new FileWriter(progressFile)) {
            gson.toJson(new HashMap<>(passedProblems), writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save progress", e);
        }
    }
    
    /**
     * Force immediate save (for shutdown).
     */
    public static void saveProgressImmediate() {
        saveScheduled.set(false); // Cancel pending debounced save
        saveProgressSync();
    }

    public static void markAsPassed(String problemId) {
        ensureInit();
        passedProblems.put(problemId, System.currentTimeMillis());
        saveProgress();
    }
    
    public static void updateFromPacket(Map<String, Long> serverProgress) {
        ensureInit();
        passedProblems.clear();
        passedProblems.putAll(serverProgress);
        saveProgress();
    }

    public static boolean isPassed(String problemId) {
        ensureInit();
        return passedProblems.containsKey(problemId);
    }
    
    public static int getPassedCount() {
        return passedProblems.size();
    }
}
