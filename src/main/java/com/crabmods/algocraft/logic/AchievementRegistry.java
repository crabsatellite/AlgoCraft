package com.crabmods.algocraft.logic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Defines all achievements/milestones that can be earned in AlgoCraft.
 * Each achievement can award a trophy item with custom description.
 */
public class AchievementRegistry {
    
    private static final Map<String, Achievement> ACHIEVEMENTS = new LinkedHashMap<>();
    
    // ==================== Achievement Definitions ====================
    
    // Problem Count Milestones
    public static final String FIRST_SOLVE = "first_solve";
    public static final String APPRENTICE = "apprentice";
    public static final String JOURNEYMAN = "journeyman";
    public static final String EXPERT = "expert";
    public static final String MASTER = "master";
    public static final String GRANDMASTER = "grandmaster";
    public static final String LEGEND = "legend";
    
    // Streak Achievement IDs
    public static final String STREAK_3 = "streak_3";
    public static final String STREAK_7 = "streak_7";
    public static final String STREAK_14 = "streak_14";
    public static final String STREAK_30 = "streak_30";
    public static final String STREAK_100 = "streak_100";
    
    // Difficulty Achievement IDs
    public static final String FIRST_EASY = "first_easy";
    public static final String FIRST_MEDIUM = "first_medium";
    public static final String FIRST_HARD = "first_hard";
    public static final String EASY_MASTER = "easy_master";
    public static final String MEDIUM_MASTER = "medium_master";
    public static final String HARD_MASTER = "hard_master";
    public static final String BALANCED = "balanced";
    
    // Special Achievement IDs
    public static final String NIGHT_OWL = "night_owl";
    public static final String SPEED_DEMON = "speed_demon";
    public static final String PERFECTIONIST = "perfectionist";
    public static final String COMPLETIONIST = "completionist";
    
    // Achievement Registrations
    private static final Achievement ACH_FIRST_SOLVE = register(FIRST_SOLVE, 
        AchievementType.MILESTONE, Rarity.COMMON, "trophy_bronze");
    
    private static final Achievement ACH_APPRENTICE = register(APPRENTICE, 
        AchievementType.MILESTONE, Rarity.COMMON, "trophy_bronze");  // 10 problems
    
    private static final Achievement ACH_JOURNEYMAN = register(JOURNEYMAN, 
        AchievementType.MILESTONE, Rarity.UNCOMMON, "trophy_silver");  // 25 problems
    
    private static final Achievement ACH_EXPERT = register(EXPERT, 
        AchievementType.MILESTONE, Rarity.RARE, "trophy_silver");  // 50 problems
    
    private static final Achievement ACH_MASTER = register(MASTER, 
        AchievementType.MILESTONE, Rarity.EPIC, "trophy_gold");  // 100 problems
    
    private static final Achievement ACH_GRANDMASTER = register(GRANDMASTER, 
        AchievementType.MILESTONE, Rarity.LEGENDARY, "trophy_gold");  // 250 problems
    
    private static final Achievement ACH_LEGEND = register(LEGEND, 
        AchievementType.MILESTONE, Rarity.MYTHIC, "trophy_diamond");  // 500 problems
    
    // Streak Achievements
    private static final Achievement ACH_STREAK_3 = register(STREAK_3, 
        AchievementType.STREAK, Rarity.COMMON, "trophy_bronze");  // 3 day streak
    
    private static final Achievement ACH_STREAK_7 = register(STREAK_7, 
        AchievementType.STREAK, Rarity.UNCOMMON, "trophy_bronze");  // 7 day streak
    
    private static final Achievement ACH_STREAK_14 = register(STREAK_14, 
        AchievementType.STREAK, Rarity.RARE, "trophy_silver");  // 14 day streak
    
    private static final Achievement ACH_STREAK_30 = register(STREAK_30, 
        AchievementType.STREAK, Rarity.EPIC, "trophy_gold");  // 30 day streak
    
    private static final Achievement ACH_STREAK_100 = register(STREAK_100, 
        AchievementType.STREAK, Rarity.LEGENDARY, "trophy_diamond");  // 100 day streak
    
    // Difficulty Achievements
    private static final Achievement ACH_FIRST_EASY = register(FIRST_EASY, 
        AchievementType.DIFFICULTY, Rarity.COMMON, "trophy_bronze");
    
    private static final Achievement ACH_FIRST_MEDIUM = register(FIRST_MEDIUM, 
        AchievementType.DIFFICULTY, Rarity.UNCOMMON, "trophy_bronze");
    
    private static final Achievement ACH_FIRST_HARD = register(FIRST_HARD, 
        AchievementType.DIFFICULTY, Rarity.RARE, "trophy_silver");
    
    private static final Achievement ACH_EASY_MASTER = register(EASY_MASTER, 
        AchievementType.DIFFICULTY, Rarity.RARE, "trophy_silver");  // 100 easy
    
    private static final Achievement ACH_MEDIUM_MASTER = register(MEDIUM_MASTER, 
        AchievementType.DIFFICULTY, Rarity.EPIC, "trophy_gold");  // 50 medium
    
    private static final Achievement ACH_HARD_MASTER = register(HARD_MASTER, 
        AchievementType.DIFFICULTY, Rarity.LEGENDARY, "trophy_diamond");  // 25 hard
    
    private static final Achievement ACH_BALANCED = register(BALANCED, 
        AchievementType.DIFFICULTY, Rarity.RARE, "trophy_gold");  // 10+ of each
    
    // Special Achievements
    private static final Achievement ACH_SPEED_DEMON = register(SPEED_DEMON, 
        AchievementType.SPECIAL, Rarity.RARE, "trophy_silver");  // Solve in < 1 min
    
    private static final Achievement ACH_PERFECTIONIST = register(PERFECTIONIST, 
        AchievementType.SPECIAL, Rarity.EPIC, "trophy_gold");  // 10 consecutive correct
    
    private static final Achievement ACH_NIGHT_OWL = register(NIGHT_OWL, 
        AchievementType.SPECIAL, Rarity.UNCOMMON, "trophy_bronze");  // Solve at midnight
    
    private static final Achievement ACH_COMPLETIONIST = register(COMPLETIONIST, 
        AchievementType.SPECIAL, Rarity.MYTHIC, "trophy_netherite");  // All problems solved
    
    // ==================== Achievement Class ====================
    
    public static class Achievement {
        private final String id;
        private final AchievementType type;
        private final Rarity rarity;
        private final String trophyModel;
        
        public Achievement(String id, AchievementType type, Rarity rarity, String trophyModel) {
            this.id = id;
            this.type = type;
            this.rarity = rarity;
            this.trophyModel = trophyModel;
        }
        
        public String getId() {
            return id;
        }
        
        public AchievementType getType() {
            return type;
        }
        
        public Rarity getRarity() {
            return rarity;
        }
        
        public String getTrophyModel() {
            return trophyModel;
        }
        
        /**
         * Get the localized name of this achievement.
         */
        public Component getName() {
            return Component.translatable("algocraft.achievement." + id + ".name")
                .withStyle(rarity.getColor());
        }
        
        /**
         * Get the localized description of this achievement.
         */
        public Component getDescription() {
            return Component.translatable("algocraft.achievement." + id + ".desc");
        }
        
        /**
         * Get the full tooltip for this achievement trophy.
         */
        public List<Component> getTrophyTooltip(String playerName, long timestamp) {
            List<Component> tooltip = new ArrayList<>();
            
            // Trophy name with rarity color
            tooltip.add(getName());
            
            // Empty line
            tooltip.add(Component.empty());
            
            // Achievement description
            tooltip.add(getDescription().copy().withStyle(ChatFormatting.GRAY));
            
            // Empty line
            tooltip.add(Component.empty());
            
            // Rarity indicator
            tooltip.add(Component.translatable("algocraft.trophy.rarity", rarity.getDisplayName())
                .withStyle(ChatFormatting.DARK_GRAY));
            
            // Type indicator
            tooltip.add(Component.translatable("algocraft.trophy.type", type.getDisplayName())
                .withStyle(ChatFormatting.DARK_GRAY));
            
            // Empty line
            tooltip.add(Component.empty());
            
            // Awarded to
            tooltip.add(Component.translatable("algocraft.trophy.awarded_to", playerName)
                .withStyle(ChatFormatting.GOLD));
            
            // Date achieved
            String dateStr = formatTimestamp(timestamp);
            tooltip.add(Component.translatable("algocraft.trophy.date", dateStr)
                .withStyle(ChatFormatting.GRAY));
            
            return tooltip;
        }
        
        private String formatTimestamp(long timestamp) {
            return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }
    
    // ==================== Enums ====================
    
    public enum AchievementType {
        MILESTONE("algocraft.achievement.type.milestone"),
        STREAK("algocraft.achievement.type.streak"),
        DIFFICULTY("algocraft.achievement.type.difficulty"),
        SPECIAL("algocraft.achievement.type.special");
        
        private final String translationKey;
        
        AchievementType(String translationKey) {
            this.translationKey = translationKey;
        }
        
        public Component getDisplayName() {
            return Component.translatable(translationKey);
        }
    }
    
    public enum Rarity {
        COMMON(ChatFormatting.WHITE, "algocraft.rarity.common"),
        UNCOMMON(ChatFormatting.GREEN, "algocraft.rarity.uncommon"),
        RARE(ChatFormatting.BLUE, "algocraft.rarity.rare"),
        EPIC(ChatFormatting.DARK_PURPLE, "algocraft.rarity.epic"),
        LEGENDARY(ChatFormatting.GOLD, "algocraft.rarity.legendary"),
        MYTHIC(ChatFormatting.LIGHT_PURPLE, "algocraft.rarity.mythic");
        
        private final ChatFormatting color;
        private final String translationKey;
        
        Rarity(ChatFormatting color, String translationKey) {
            this.color = color;
            this.translationKey = translationKey;
        }
        
        public ChatFormatting getColor() {
            return color;
        }
        
        public Component getDisplayName() {
            return Component.translatable(translationKey).withStyle(color);
        }
    }
    
    // ==================== Registry Methods ====================
    
    private static Achievement register(String id, AchievementType type, Rarity rarity, String trophyModel) {
        Achievement achievement = new Achievement(id, type, rarity, trophyModel);
        ACHIEVEMENTS.put(id, achievement);
        return achievement;
    }
    
    public static Achievement get(String id) {
        return ACHIEVEMENTS.get(id);
    }
    
    public static Collection<Achievement> getAll() {
        return Collections.unmodifiableCollection(ACHIEVEMENTS.values());
    }
    
    public static List<Achievement> getByType(AchievementType type) {
        return ACHIEVEMENTS.values().stream()
            .filter(a -> a.getType() == type)
            .toList();
    }
    
    public static List<Achievement> getByRarity(Rarity rarity) {
        return ACHIEVEMENTS.values().stream()
            .filter(a -> a.getRarity() == rarity)
            .toList();
    }
}
