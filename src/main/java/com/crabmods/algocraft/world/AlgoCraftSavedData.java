package com.crabmods.algocraft.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlgoCraftSavedData extends SavedData {
    private static final String DATA_NAME = "algocraft_data";
    
    // UUID -> (ProblemID -> LastSolvedTimestamp)
    private final Map<UUID, Map<String, Long>> playerProgress = new HashMap<>();

    public static AlgoCraftSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
            AlgoCraftSavedData::new,
            AlgoCraftSavedData::load,
            null
        ), DATA_NAME);
    }
    
    public AlgoCraftSavedData() {}

    public static AlgoCraftSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        AlgoCraftSavedData data = new AlgoCraftSavedData();
        ListTag playersList = tag.getList("Players", Tag.TAG_COMPOUND);
        
        for (int i = 0; i < playersList.size(); i++) {
            CompoundTag playerTag = playersList.getCompound(i);
            UUID uuid = playerTag.getUUID("UUID");
            
            Map<String, Long> progress = new HashMap<>();
            ListTag problemsList = playerTag.getList("Problems", Tag.TAG_COMPOUND);
            for (int j = 0; j < problemsList.size(); j++) {
                CompoundTag probTag = problemsList.getCompound(j);
                progress.put(probTag.getString("ID"), probTag.getLong("Time"));
            }
            
            data.playerProgress.put(uuid, progress);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag playersList = new ListTag();
        
        for (Map.Entry<UUID, Map<String, Long>> entry : playerProgress.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("UUID", entry.getKey());
            
            ListTag problemsList = new ListTag();
            for (Map.Entry<String, Long> probEntry : entry.getValue().entrySet()) {
                CompoundTag probTag = new CompoundTag();
                probTag.putString("ID", probEntry.getKey());
                probTag.putLong("Time", probEntry.getValue());
                problemsList.add(probTag);
            }
            playerTag.put("Problems", problemsList);
            
            playersList.add(playerTag);
        }
        
        tag.put("Players", playersList);
        return tag;
    }
    
    public boolean isProblemSolved(UUID player, String problemId) {
        return playerProgress.containsKey(player) && playerProgress.get(player).containsKey(problemId);
    }
    
    public long getLastSolvedTime(UUID player, String problemId) {
        if (!playerProgress.containsKey(player)) return 0;
        return playerProgress.get(player).getOrDefault(problemId, 0L);
    }
    
    public void setProblemSolved(UUID player, String problemId, long time) {
        playerProgress.computeIfAbsent(player, k -> new HashMap<>()).put(problemId, time);
        setDirty();
    }
    
    public Map<String, Long> getPlayerProgress(UUID player) {
        return playerProgress.getOrDefault(player, new HashMap<>());
    }
}
