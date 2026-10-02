package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.item.TrophyItem;
import com.crabmods.algocraft.logic.AchievementRegistry.Achievement;
import com.crabmods.algocraft.logic.AchievementRegistry.AchievementType;
import com.crabmods.algocraft.logic.AchievementRegistry.Rarity;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages achievement tracking and trophy awarding for players.
 * Integrates with AlgoCraftSavedData for persistence.
 */
public class AchievementManager {
    
    // In-memory cache of earned achievements per player UUID (thread-safe)
    private final Map<UUID, Set<String>> playerAchievements = new ConcurrentHashMap<>();
    
    private AchievementManager() {}
    
    public static AchievementManager createForWorld() {
        return new AchievementManager();
    }

    public static AchievementManager forPlayer(ServerPlayer player) {
        return AlgoCraftSavedData.get(player.serverLevel()).getAchievementManager();
    }
    
    /**
     * Load achievements for a player from saved data.
     */
    public void loadPlayerAchievements(UUID playerUuid, CompoundTag savedData) {
        Set<String> achievements = ConcurrentHashMap.newKeySet();
        
        if (savedData != null && savedData.contains("EarnedAchievements")) {
            ListTag list = savedData.getList("EarnedAchievements", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                achievements.add(list.getString(i));
            }
        }
        
        playerAchievements.put(playerUuid, achievements);
    }
    
    /**
     * Load achievements directly from a Set (used by AlgoCraftSavedData).
     */
    public void loadPlayerAchievementsFromSet(UUID playerUuid, Set<String> achievements) {
        Set<String> threadSafeSet = ConcurrentHashMap.newKeySet();
        threadSafeSet.addAll(achievements);
        playerAchievements.put(playerUuid, threadSafeSet);
    }
    
    /**
     * Save achievements for a player to CompoundTag.
     */
    public CompoundTag savePlayerAchievements(UUID playerUuid) {
        CompoundTag tag = new CompoundTag();
        Set<String> achievements = playerAchievements.getOrDefault(playerUuid, ConcurrentHashMap.newKeySet());
        
        ListTag list = new ListTag();
        for (String achievementId : achievements) {
            list.add(StringTag.valueOf(achievementId));
        }
        tag.put("EarnedAchievements", list);
        
        return tag;
    }
    
    /**
     * Check if a player has earned a specific achievement.
     */
    public boolean hasAchievement(UUID playerUuid, String achievementId) {
        Set<String> achievements = playerAchievements.get(playerUuid);
        return achievements != null && achievements.contains(achievementId);
    }
    
    /**
     * Get all achievements earned by a player.
     */
    public Set<String> getPlayerAchievements(UUID playerUuid) {
        return Collections.unmodifiableSet(
            playerAchievements.getOrDefault(playerUuid, ConcurrentHashMap.newKeySet())
        );
    }
    
    /**
     * Award an achievement to a player if not already earned.
     * Returns true if the achievement was newly awarded.
     */
    public boolean awardAchievement(ServerPlayer player, String achievementId) {
        UUID uuid = player.getUUID();
        
        // Initialize player if not present (thread-safe)
        playerAchievements.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        
        Set<String> achievements = playerAchievements.get(uuid);
        if (achievements.contains(achievementId)) {
            return false; // Already has this achievement
        }
        
        Achievement achievement = AchievementRegistry.get(achievementId);
        if (achievement == null) {
            AlgoCraft.LOGGER.warn("Attempted to award unknown achievement: {}", achievementId);
            return false;
        }
        
        // Add to earned achievements
        achievements.add(achievementId);
        
        // Award trophy item
        awardTrophy(player, achievement);
        
        // Show achievement notification
        showAchievementNotification(player, achievement);
        
        // Play sound based on rarity
        playAchievementSound(player, achievement.getRarity());
        
        return true;
    }
    
    /**
     * Check and award any achievements based on player stats.
     * 
     * Optimized: Uses early exit checks to avoid unnecessary awardAchievement calls
     * when player already has the achievement.
     */
    public void checkAndAwardAchievements(ServerPlayer player, int totalSolved, int streak,
                                           int easyCount, int mediumCount, int hardCount) {
        UUID uuid = player.getUUID();
        Set<String> earned = playerAchievements.getOrDefault(uuid, ConcurrentHashMap.newKeySet());
        
        // ==================== Milestone Achievements ====================
        
        // First problem
        if (totalSolved >= 1 && !earned.contains(AchievementRegistry.FIRST_SOLVE)) {
            awardAchievement(player, AchievementRegistry.FIRST_SOLVE);
        }
        
        // Problem count milestones (check in descending order for efficiency)
        if (totalSolved >= 500 && !earned.contains(AchievementRegistry.LEGEND)) {
            awardAchievement(player, AchievementRegistry.LEGEND);
        }
        if (totalSolved >= 250 && !earned.contains(AchievementRegistry.GRANDMASTER)) {
            awardAchievement(player, AchievementRegistry.GRANDMASTER);
        }
        if (totalSolved >= 100 && !earned.contains(AchievementRegistry.MASTER)) {
            awardAchievement(player, AchievementRegistry.MASTER);
        }
        if (totalSolved >= 50 && !earned.contains(AchievementRegistry.EXPERT)) {
            awardAchievement(player, AchievementRegistry.EXPERT);
        }
        if (totalSolved >= 25 && !earned.contains(AchievementRegistry.JOURNEYMAN)) {
            awardAchievement(player, AchievementRegistry.JOURNEYMAN);
        }
        if (totalSolved >= 10 && !earned.contains(AchievementRegistry.APPRENTICE)) {
            awardAchievement(player, AchievementRegistry.APPRENTICE);
        }
        
        // ==================== Streak Achievements ====================
        
        if (streak >= 100 && !earned.contains(AchievementRegistry.STREAK_100)) {
            awardAchievement(player, AchievementRegistry.STREAK_100);
        }
        if (streak >= 30 && !earned.contains(AchievementRegistry.STREAK_30)) {
            awardAchievement(player, AchievementRegistry.STREAK_30);
        }
        if (streak >= 14 && !earned.contains(AchievementRegistry.STREAK_14)) {
            awardAchievement(player, AchievementRegistry.STREAK_14);
        }
        if (streak >= 7 && !earned.contains(AchievementRegistry.STREAK_7)) {
            awardAchievement(player, AchievementRegistry.STREAK_7);
        }
        if (streak >= 3 && !earned.contains(AchievementRegistry.STREAK_3)) {
            awardAchievement(player, AchievementRegistry.STREAK_3);
        }
        
        // ==================== Difficulty Achievements ====================
        
        // First of each difficulty
        if (easyCount >= 1 && !earned.contains(AchievementRegistry.FIRST_EASY)) {
            awardAchievement(player, AchievementRegistry.FIRST_EASY);
        }
        if (mediumCount >= 1 && !earned.contains(AchievementRegistry.FIRST_MEDIUM)) {
            awardAchievement(player, AchievementRegistry.FIRST_MEDIUM);
        }
        if (hardCount >= 1 && !earned.contains(AchievementRegistry.FIRST_HARD)) {
            awardAchievement(player, AchievementRegistry.FIRST_HARD);
        }
        
        // Difficulty mastery
        if (easyCount >= 100 && !earned.contains(AchievementRegistry.EASY_MASTER)) {
            awardAchievement(player, AchievementRegistry.EASY_MASTER);
        }
        if (mediumCount >= 50 && !earned.contains(AchievementRegistry.MEDIUM_MASTER)) {
            awardAchievement(player, AchievementRegistry.MEDIUM_MASTER);
        }
        if (hardCount >= 25 && !earned.contains(AchievementRegistry.HARD_MASTER)) {
            awardAchievement(player, AchievementRegistry.HARD_MASTER);
        }
        
        // Balance achievement
        if (easyCount >= 10 && mediumCount >= 10 && hardCount >= 10 
                && !earned.contains(AchievementRegistry.BALANCED)) {
            awardAchievement(player, AchievementRegistry.BALANCED);
        }
        // Perfectionist (checked when 10 consecutive correct)
    }
    
    /* Optimized with early exit checks.
     */
    public void checkSpecialAchievements(ServerPlayer player, long solveTimeMillis, 
                                         int consecutiveCorrect, boolean isNightTime) {
        UUID uuid = player.getUUID();
        Set<String> earned = playerAchievements.getOrDefault(uuid, ConcurrentHashMap.newKeySet());
        
        // Night Owl - solved at night (between 12am and 6am real time)
        if (isNightTime && !earned.contains(AchievementRegistry.NIGHT_OWL)) {
            awardAchievement(player, AchievementRegistry.NIGHT_OWL);
        }
        
        // Speed Demon - solved in under 60 seconds
        if (solveTimeMillis > 0 && solveTimeMillis < 60000 
                && !earned.contains(AchievementRegistry.SPEED_DEMON)) {
            awardAchievement(player, AchievementRegistry.SPEED_DEMON);
        }
        
        // Perfectionist - 10 consecutive correct
        if (consecutiveCorrect >= 10 && !earned.contains(AchievementRegistry.PERFECTIONIST)) {
            awardAchievement(player, AchievementRegistry.PERFECTIONIST);
        }
    }
    
    /**
     * Check if player has completed all problems in a repository.
     * Call this when a problem is solved.
     * 
     * @param player The player
     * @param repositoryName The repository name (prefix before the problem ID)
     * @param totalProblemsInRepo The total number of problems in the repository
     * @param solvedInRepo The number of problems the player has solved in this repository
     */
    public void checkCompletionistAchievement(ServerPlayer player, String repositoryName, 
                                               int totalProblemsInRepo, int solvedInRepo) {
        if (totalProblemsInRepo > 0 && solvedInRepo >= totalProblemsInRepo) {
            awardAchievement(player, AchievementRegistry.COMPLETIONIST);
        }
    }
    
    /**
     * Award the trophy item to the player.
     */
    private void awardTrophy(ServerPlayer player, Achievement achievement) {
        Item trophyItem = getTrophyItemForAchievement(achievement);
        if (trophyItem == null) {
            AlgoCraft.LOGGER.warn("No trophy item registered for achievement {} trophy id {}",
                achievement.getId(), achievement.getTrophyItemId());
            return;
        }
        
        ItemStack trophy = TrophyItem.createTrophy(
            trophyItem,
            achievement,
            player.getName().getString(),
            player.getUUID().toString(),
            Instant.now().toEpochMilli()
        );
        
        // Give to player (drop if inventory full)
        if (!player.getInventory().add(trophy)) {
            player.drop(trophy, false);
        }
    }
    
    /**
     * Get the exact trophy item declared by the achievement.
     */
    private Item getTrophyItemForAchievement(Achievement achievement) {
        return ModItems.getTrophyForItemId(achievement.getTrophyItemId());
    }
    
    /**
     * Show achievement unlock notification to player.
     */
    private void showAchievementNotification(ServerPlayer player, Achievement achievement) {
        // Title message
        Component title = Component.translatable("algocraft.achievement.unlocked")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        
        // Subtitle with achievement name
        Component subtitle = achievement.getName()
            .copy()
            .withStyle(achievement.getRarity().getColor());
        
        // Send as title and subtitle
        player.sendSystemMessage(
            Component.empty()
                .append(title)
                .append(Component.literal(" "))
                .append(subtitle)
        );
        
        // Also send detailed chat message
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(
            Component.translatable("algocraft.achievement.trophy_prefix")
                .withStyle(ChatFormatting.GOLD)
                .append(achievement.getName().copy().withStyle(achievement.getRarity().getColor()))
        );
        player.sendSystemMessage(
            Component.literal("  ")
                .append(achievement.getDescription().copy().withStyle(ChatFormatting.GRAY))
        );
        player.sendSystemMessage(
            Component.literal("  ")
                .append(Component.translatable("algocraft.achievement.rarity",
                    achievement.getRarity().getDisplayName()))
                .withStyle(ChatFormatting.DARK_GRAY)
        );
        player.sendSystemMessage(Component.empty());
    }
    
    /**
     * Play sound effect based on achievement rarity.
     */
    private void playAchievementSound(ServerPlayer player, Rarity rarity) {
        player.level().playSound(null, player.blockPosition(), soundForRarity(rarity),
            SoundSource.PLAYERS, volumeForRarity(rarity), pitchForRarity(rarity));
    }

    static SoundEvent soundForRarity(Rarity rarity) {
        return switch (rarity) {
            case COMMON, UNCOMMON -> SoundEvents.PLAYER_LEVELUP;
            case RARE -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            case EPIC -> SoundEvents.ENDER_DRAGON_GROWL;
            case LEGENDARY -> SoundEvents.END_PORTAL_SPAWN;
            case MYTHIC -> SoundEvents.TOTEM_USE;
        };
    }

    static float volumeForRarity(Rarity rarity) {
        return rarity.ordinal() >= Rarity.EPIC.ordinal() ? 1.0f : 0.7f;
    }

    static float pitchForRarity(Rarity rarity) {
        return 1.0f + (rarity.ordinal() * 0.1f);
    }
    
    /**
     * Get achievement statistics for a player.
     */
    public AchievementStats getStats(UUID playerUuid) {
        Set<String> earned = playerAchievements.getOrDefault(playerUuid, ConcurrentHashMap.newKeySet());
        
        int totalAvailable = AchievementRegistry.getAll().size();
        int totalEarned = earned.size();
        
        Map<AchievementType, Integer> byType = new EnumMap<>(AchievementType.class);
        Map<Rarity, Integer> byRarity = new EnumMap<>(Rarity.class);
        
        for (String achievementId : earned) {
            Achievement achievement = AchievementRegistry.get(achievementId);
            if (achievement != null) {
                byType.merge(achievement.getType(), 1, Integer::sum);
                byRarity.merge(achievement.getRarity(), 1, Integer::sum);
            }
        }
        
        return new AchievementStats(totalEarned, totalAvailable, byType, byRarity);
    }
    
    /**
     * Statistics about a player's achievements.
     */
    public record AchievementStats(
        int earned,
        int total,
        Map<AchievementType, Integer> byType,
        Map<Rarity, Integer> byRarity
    ) {
        public double getCompletionPercentage() {
            return total > 0 ? (earned * 100.0 / total) : 0;
        }
    }
}
