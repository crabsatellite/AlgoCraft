package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressManager {
    private static final System.Logger LOGGER = System.getLogger(ProgressManager.class.getName());
    static final String CONFIG_DIR_PROPERTY = "algocraft.configDir";
    // ProblemID -> LastSolvedTimestamp (thread-safe)
    private static final Map<String, Long> passedProblems = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static volatile File progressFile;
    private static volatile boolean serverSession;
    private static volatile Map<String, Long> serverProgress = Map.of();
    public static void beginServerSession() { serverProgress = Map.of(); serverSession = true; }
    public static void endServerSession() { serverSession = false; serverProgress = Map.of(); }
    
    // Debounce save operations to avoid excessive IO
    private static final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private static final AtomicLong saveGeneration = new AtomicLong();
    private static final long SAVE_DEBOUNCE_MS = 500;

    public static void init() {
        if (progressFile != null) return;
        File configDir = configDirectory();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        progressFile = new File(configDir, "user_progress.json");
        loadProgress();
    }

    private static File configDirectory() {
        String override = System.getProperty(CONFIG_DIR_PROPERTY);
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
                        mergeValidProgressEntries(loaded);
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
                            if (isValidProblemId(id)) {
                                passedProblems.put(id, 0L);
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    LOGGER.log(System.Logger.Level.ERROR, "Corrupted progress file, starting fresh", e);
                }
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to load progress", e);
            } catch (JsonSyntaxException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Corrupted progress file, starting fresh", e);
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

        long generation = saveGeneration.get();
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(SAVE_DEBOUNCE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (generation != saveGeneration.get()) {
                return;
            }

            saveScheduled.set(false);
            if (generation == saveGeneration.get()) {
                saveProgressSync();
            }
        });
    }

    private static void mergeValidProgressEntries(Map<String, Long> loaded) {
        for (Map.Entry<String, Long> entry : loaded.entrySet()) {
            if (isValidProgressEntry(entry.getKey(), entry.getValue())) {
                passedProblems.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static Map<String, Long> sanitizedProgressSnapshot(Map<String, Long> progress) {
        if (progress == null) {
            return null;
        }
        Map<String, Long> sanitized = new HashMap<>();
        for (Map.Entry<String, Long> entry : progress.entrySet()) {
            if (!isValidProgressEntry(entry.getKey(), entry.getValue())) {
                return null;
            }
            sanitized.put(entry.getKey(), entry.getValue());
        }
        return sanitized;
    }

    private static boolean isValidProgressEntry(String problemId, Long solvedAtMillis) {
        return isValidProblemId(problemId) && solvedAtMillis != null && solvedAtMillis >= 0;
    }

    private static boolean isValidProblemId(String problemId) {
        return problemId != null && !problemId.isBlank() && problemId.length() <= 256;
    }

    private static void writeProgressAtomically(File targetFile, Map<String, Long> snapshot) throws IOException {
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
    
    /**
     * Synchronous save - called by async debouncer or for forced saves.
     */
    private static void saveProgressSync() {
        ensureInit();
        if (progressFile == null) return;

        try {
            writeProgressAtomically(progressFile, new HashMap<>(passedProblems));
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to save progress", e);
        }
    }
    
    /**
     * Force immediate save (for shutdown).
     */
    public static void saveProgressImmediate() {
        saveGeneration.incrementAndGet();
        saveScheduled.set(false); // Cancel pending debounced save
        saveProgressSync();
    }

    public static void markAsPassed(String problemId) {
        if (!isValidProblemId(problemId)) {
            return;
        }
        ensureInit();
        passedProblems.put(problemId, System.currentTimeMillis());
        saveProgress();
    }
    
    public static void updateFromPacket(Map<String, Long> serverProgress) {
        ensureInit();
        Map<String, Long> sanitized = sanitizedProgressSnapshot(serverProgress);
        if (sanitized == null) {
            LOGGER.log(System.Logger.Level.WARNING, "Ignoring invalid progress sync packet");
            return;
        }
        if (serverSession) { ProgressManager.serverProgress = Map.copyOf(sanitized); return; }
        passedProblems.clear();
        passedProblems.putAll(sanitized);
        saveProgress();
    }

    public static boolean isPassed(String problemId) {
        if (problemId == null) return false;
        ensureInit();
        return serverSession && !problemId.startsWith("practice:") ? serverProgress.containsKey(problemId) : passedProblems.containsKey(problemId);
    }
    
    public static int getPassedCount() {
        return serverSession ? serverProgress.size() : passedProblems.size();
    }

    static void resetForTest() {
        endServerSession();
        saveGeneration.incrementAndGet();
        passedProblems.clear();
        progressFile = null;
        saveScheduled.set(false);
    }
}
