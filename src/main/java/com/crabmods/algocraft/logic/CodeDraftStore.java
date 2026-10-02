package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Player code keyed by the complete repository-qualified problem id. */
public final class CodeDraftStore {
    private static final Gson GSON = new Gson();
    private final Path file;
    private final Map<String, String> drafts = new LinkedHashMap<>();
    private boolean dirty;

    public CodeDraftStore(Path file) throws IOException {
        this.file = file;
        if (Files.isRegularFile(file)) {
            try {
                Map<String, String> loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() {}.getType());
                if (loaded != null) loaded.forEach((id, code) -> {
                    if (id != null && code != null) drafts.put(id, code);
                });
            } catch (JsonParseException e) {
                throw new IOException("Unable to read code drafts; original file has been preserved", e);
            }
        }
    }

    public String getOrDefault(String problemId, String initialCode) {
        return drafts.getOrDefault(problemId, initialCode);
    }

    public void put(String problemId, String code) {
        if (problemId != null && code != null && !code.equals(drafts.put(problemId, code))) dirty = true;
    }

    public void save() throws IOException {
        if (!dirty) return;
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "code-drafts-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(drafts), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
