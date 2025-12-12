package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubmissionHistoryManager {
    private static final List<SubmissionRecord> history = new ArrayList<>();
    private static final Gson gson = new Gson();
    private static File historyFile;

    public static void init() {
        if (historyFile != null) return;
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config/algocraft");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        historyFile = new File(configDir, "submission_history.json");
        loadHistory();
    }

    private static void ensureInit() {
        if (historyFile == null) {
            init();
        }
    }

    private static void loadHistory() {
        if (historyFile.exists()) {
            try (FileReader reader = new FileReader(historyFile)) {
                List<SubmissionRecord> loaded = gson.fromJson(reader, new TypeToken<List<SubmissionRecord>>(){}.getType());
                if (loaded != null) {
                    history.clear();
                    history.addAll(loaded);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveRecord(SubmissionRecord record) {
        ensureInit();
        history.add(0, record); // Add to beginning
        saveHistory();
    }

    private static void saveHistory() {
        try (FileWriter writer = new FileWriter(historyFile)) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<SubmissionRecord> getHistory() {
        ensureInit();
        return new ArrayList<>(history);
    }
}
