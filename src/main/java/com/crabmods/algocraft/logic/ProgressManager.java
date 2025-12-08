package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProgressManager {
    // ProblemID -> LastSolvedTimestamp
    private static final Map<String, Long> passedProblems = new HashMap<>();
    private static final Gson gson = new Gson();
    private static File progressFile;

    public static void init() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config/algocraft");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        progressFile = new File(configDir, "user_progress.json");
        loadProgress();
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
                e.printStackTrace();
            }
        }
    }

    public static void saveProgress() {
        try (FileWriter writer = new FileWriter(progressFile)) {
            gson.toJson(passedProblems, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void markAsPassed(String problemId) {
        passedProblems.put(problemId, System.currentTimeMillis());
        saveProgress();
    }
    
    public static void updateFromPacket(Map<String, Long> serverProgress) {
        passedProblems.clear();
        passedProblems.putAll(serverProgress);
        saveProgress();
    }

    public static boolean isPassed(String problemId) {
        return passedProblems.containsKey(problemId);
    }
    
    public static int getPassedCount() {
        return passedProblems.size();
    }
}
