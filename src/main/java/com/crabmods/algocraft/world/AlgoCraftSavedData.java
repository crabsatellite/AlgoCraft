package com.crabmods.algocraft.world;

import com.crabmods.algocraft.logic.AchievementManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlgoCraftSavedData extends SavedData {
    private static final String DATA_NAME = "algocraft_data";
    
    // UUID -> (ProblemID -> LastSolvedTimestamp)
    // Using ConcurrentHashMap for thread-safety
    private final Map<UUID, Map<String, Long>> playerProgress = new ConcurrentHashMap<>();
    
    // UUID -> PlayerStats (streak, total solved, etc.)
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();

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
            
            // Load player stats
            if (playerTag.contains("Stats")) {
                CompoundTag statsTag = playerTag.getCompound("Stats");
                PlayerStats stats = new PlayerStats();
                stats.currentStreak = statsTag.getInt("CurrentStreak");
                stats.bestStreak = statsTag.getInt("BestStreak");
                stats.totalSolved = statsTag.getInt("TotalSolved");
                stats.lastSolveDay = statsTag.getLong("LastSolveDay");
                stats.easyCount = statsTag.getInt("EasyCount");
                stats.mediumCount = statsTag.getInt("MediumCount");
                stats.hardCount = statsTag.getInt("HardCount");
                stats.consecutiveCorrect = statsTag.getInt("ConsecutiveCorrect");
                data.playerStats.put(uuid, stats);
            }
            
            // Load player achievements into AchievementManager
            if (playerTag.contains("Achievements")) {
                ListTag achievementsList = playerTag.getList("Achievements", Tag.TAG_STRING);
                Set<String> achievements = new HashSet<>();
                for (int j = 0; j < achievementsList.size(); j++) {
                    achievements.add(achievementsList.getString(j));
                }
                AchievementManager.getInstance().loadPlayerAchievementsFromSet(uuid, achievements);
            }
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
            
            // Save player stats
            PlayerStats stats = playerStats.get(entry.getKey());
            if (stats != null) {
                CompoundTag statsTag = new CompoundTag();
                statsTag.putInt("CurrentStreak", stats.currentStreak);
                statsTag.putInt("BestStreak", stats.bestStreak);
                statsTag.putInt("TotalSolved", stats.totalSolved);
                statsTag.putLong("LastSolveDay", stats.lastSolveDay);
                statsTag.putInt("EasyCount", stats.easyCount);
                statsTag.putInt("MediumCount", stats.mediumCount);
                statsTag.putInt("HardCount", stats.hardCount);
                statsTag.putInt("ConsecutiveCorrect", stats.consecutiveCorrect);
                playerTag.put("Stats", statsTag);
            }
            
            // Save player achievements from AchievementManager
            Set<String> achievements = AchievementManager.getInstance().getPlayerAchievements(entry.getKey());
            if (!achievements.isEmpty()) {
                ListTag achievementsList = new ListTag();
                for (String achievementId : achievements) {
                    achievementsList.add(StringTag.valueOf(achievementId));
                }
                playerTag.put("Achievements", achievementsList);
            }
            
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
        playerProgress.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(problemId, time);
        setDirty();
    }
    
    /**
     * Get a copy of player's progress (defensive copy to prevent external modification).
     */
    public Map<String, Long> getPlayerProgress(UUID player) {
        Map<String, Long> progress = playerProgress.get(player);
        return progress != null ? new HashMap<>(progress) : new HashMap<>();
    }
    
    // ==================== New Stats Methods ====================
    
    /**
     * Get or create player stats.
     */
    public PlayerStats getPlayerStats(UUID player) {
        return playerStats.computeIfAbsent(player, k -> new PlayerStats());
    }
    
    /**
     * Update streak when player solves a problem.
     * Should be called once per day when first problem is solved.
     * 
     * @return Updated streak count
     */
    public int updateStreak(UUID player, long solveTime) {
        PlayerStats stats = getPlayerStats(player);
        LocalDate today = Instant.ofEpochMilli(solveTime).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate lastSolveDate = stats.lastSolveDay > 0 
            ? Instant.ofEpochMilli(stats.lastSolveDay).atZone(ZoneId.systemDefault()).toLocalDate()
            : null;
        
        if (lastSolveDate == null) {
            // First time solving
            stats.currentStreak = 1;
        } else {
            long daysBetween = ChronoUnit.DAYS.between(lastSolveDate, today);
            
            if (daysBetween == 0) {
                // Same day, streak unchanged
            } else if (daysBetween == 1) {
                // Consecutive day, increment streak
                stats.currentStreak++;
            } else {
                // Streak broken, reset to 1
                stats.currentStreak = 1;
            }
        }
        
        // Update best streak
        if (stats.currentStreak > stats.bestStreak) {
            stats.bestStreak = stats.currentStreak;
        }
        
        stats.lastSolveDay = solveTime;
        setDirty();
        
        return stats.currentStreak;
    }
    
    /**
     * Increment total solved count.
     * 
     * @return New total count
     */
    public int incrementTotalSolved(UUID player) {
        PlayerStats stats = getPlayerStats(player);
        stats.totalSolved++;
        setDirty();
        return stats.totalSolved;
    }
    
    /**
     * Get current streak for player.
     */
    public int getCurrentStreak(UUID player) {
        return getPlayerStats(player).currentStreak;
    }
    
    /**
     * Get total problems solved by player.
     */
    public int getTotalSolved(UUID player) {
        return getPlayerStats(player).totalSolved;
    }
    
    /**
     * Player statistics holder.
     */
    public static class PlayerStats {
        public int currentStreak = 0;
        public int bestStreak = 0;
        public int totalSolved = 0;
        public long lastSolveDay = 0;
        public int easyCount = 0;
        public int mediumCount = 0;
        public int hardCount = 0;
        public int consecutiveCorrect = 0;  // For perfectionist achievement
    }
    
    /**
     * Get count of problems solved for a specific difficulty.
     */
    public int getDifficultyCount(UUID player, String difficulty) {
        PlayerStats stats = getPlayerStats(player);
        return switch (difficulty.toLowerCase()) {
            case "easy" -> stats.easyCount;
            case "medium" -> stats.mediumCount;
            case "hard" -> stats.hardCount;
            default -> 0;
        };
    }
    
    /**
     * Increment the count for a specific difficulty.
     */
    public void incrementDifficultyCount(UUID player, String difficulty) {
        PlayerStats stats = getPlayerStats(player);
        switch (difficulty.toLowerCase()) {
            case "easy" -> stats.easyCount++;
            case "medium" -> stats.mediumCount++;
            case "hard" -> stats.hardCount++;
        }
        setDirty();
    }
    
    /**
     * Increment consecutive correct submissions.
     * @return New consecutive count
     */
    public int incrementConsecutiveCorrect(UUID player) {
        PlayerStats stats = getPlayerStats(player);
        stats.consecutiveCorrect++;
        setDirty();
        return stats.consecutiveCorrect;
    }
    
    /**
     * Reset consecutive correct on wrong submission.
     */
    public void resetConsecutiveCorrect(UUID player) {
        PlayerStats stats = getPlayerStats(player);
        stats.consecutiveCorrect = 0;
        setDirty();
    }
    
    /**
     * Get consecutive correct count.
     */
    public int getConsecutiveCorrect(UUID player) {
        return getPlayerStats(player).consecutiveCorrect;
    }
}
