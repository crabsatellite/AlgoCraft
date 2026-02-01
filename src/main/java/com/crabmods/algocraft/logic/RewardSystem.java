package com.crabmods.algocraft.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.Holder;

import java.util.*;

/**
 * Dynamic reward system that adapts to player progression.
 * 
 * Features:
 * - Tiered rewards based on player's game stage
 * - Streak bonuses for consecutive daily solves
 * - Milestone rewards for problem count achievements
 * - Random bonus drops for excitement
 * - Experience-based rewards that scale
 */
public class RewardSystem {
    
    // ==================== Reward Tiers ====================
    
    /**
     * Determines player's progression tier based on their inventory/achievements.
     */
    public enum PlayerTier {
        EARLY_GAME(1.0),      // No diamonds yet
        MID_GAME(1.5),        // Has diamonds, no netherite
        LATE_GAME(2.0),       // Has netherite
        END_GAME(3.0);        // Has beaten dragon/wither
        
        public final double multiplier;
        
        PlayerTier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
    
    /**
     * Reward types that can be given.
     */
    public enum RewardType {
        ITEMS,          // Physical items
        EXPERIENCE,     // XP points
        EFFECTS,        // Potion effects
        SPECIAL         // Special rewards (enchanted books, etc.)
    }
    
    // ==================== Reward Calculation ====================
    
    /**
     * Calculate and give rewards for solving a problem.
     * 
     * @param player The player to reward
     * @param difficulty Problem difficulty (EASY, MEDIUM, HARD)
     * @param isFirstTime Whether this is the first time solving
     * @param streakCount Current streak count (consecutive days)
     * @param totalSolved Total problems solved by player
     * @return Description of rewards given
     */
    public static RewardResult giveRewards(ServerPlayer player, String difficulty, 
                                           boolean isFirstTime, int streakCount, int totalSolved) {
        RewardResult result = new RewardResult();
        PlayerTier tier = detectPlayerTier(player);
        Random random = new Random();
        
        // Base rewards
        giveBaseRewards(player, difficulty, isFirstTime, tier, result);
        
        // Streak bonus (3+ days)
        if (streakCount >= 3) {
            giveStreakBonus(player, streakCount, tier, result);
        }
        
        // Milestone rewards
        checkMilestones(player, totalSolved, result);
        
        // Random bonus (10% chance)
        if (random.nextDouble() < 0.10) {
            giveRandomBonus(player, tier, result);
        }
        
        // Always give some XP
        int xpAmount = calculateXpReward(difficulty, isFirstTime, tier, streakCount);
        player.giveExperiencePoints(xpAmount);
        result.addXp(xpAmount);
        
        return result;
    }
    
    /**
     * Detect player's progression tier based on inventory.
     */
    public static PlayerTier detectPlayerTier(ServerPlayer player) {
        boolean hasNetherite = false;
        boolean hasDiamond = false;
        
        // Check armor and tools
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            
            String itemName = stack.getItem().toString().toLowerCase();
            if (itemName.contains("netherite")) {
                hasNetherite = true;
            } else if (itemName.contains("diamond")) {
                hasDiamond = true;
            }
        }
        
        // Check armor slots
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty()) continue;
            String itemName = stack.getItem().toString().toLowerCase();
            if (itemName.contains("netherite")) {
                hasNetherite = true;
            } else if (itemName.contains("diamond")) {
                hasDiamond = true;
            }
        }
        
        // Check for end game (simplified - could check advancements)
        // For now, having netherite tools is considered late game
        if (hasNetherite) {
            return PlayerTier.LATE_GAME;
        } else if (hasDiamond) {
            return PlayerTier.MID_GAME;
        } else {
            return PlayerTier.EARLY_GAME;
        }
    }
    
    /**
     * Give base rewards based on difficulty and tier.
     */
    private static void giveBaseRewards(ServerPlayer player, String difficulty, 
                                        boolean isFirstTime, PlayerTier tier, RewardResult result) {
        List<ItemStack> rewards = new ArrayList<>();
        
        if (isFirstTime) {
            // First time rewards - scaled by tier
            switch (difficulty.toUpperCase()) {
                case "EASY":
                    rewards.addAll(getEasyFirstTimeRewards(tier));
                    break;
                case "MEDIUM":
                    rewards.addAll(getMediumFirstTimeRewards(tier));
                    break;
                case "HARD":
                    rewards.addAll(getHardFirstTimeRewards(tier));
                    break;
                default:
                    rewards.add(new ItemStack(Items.EMERALD, (int)(5 * tier.multiplier)));
            }
        } else {
            // Daily rewards - smaller but still meaningful
            rewards.addAll(getDailyRewards(tier, difficulty));
        }
        
        // Give all rewards
        for (ItemStack stack : rewards) {
            if (!player.getInventory().add(stack)) {
                // If inventory full, drop on ground
                player.drop(stack, false);
            }
            result.addItem(stack);
        }
    }
    
    private static List<ItemStack> getEasyFirstTimeRewards(PlayerTier tier) {
        List<ItemStack> rewards = new ArrayList<>();
        
        switch (tier) {
            case EARLY_GAME:
                rewards.add(new ItemStack(Items.IRON_INGOT, 8));
                rewards.add(new ItemStack(Items.COAL, 16));
                rewards.add(new ItemStack(Items.BREAD, 8));
                break;
            case MID_GAME:
                rewards.add(new ItemStack(Items.IRON_INGOT, 16));
                rewards.add(new ItemStack(Items.GOLD_INGOT, 8));
                rewards.add(new ItemStack(Items.LAPIS_LAZULI, 16));
                break;
            case LATE_GAME:
            case END_GAME:
                rewards.add(new ItemStack(Items.DIAMOND, 2));
                rewards.add(new ItemStack(Items.EMERALD, 8));
                rewards.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 8));
                break;
        }
        
        return rewards;
    }
    
    private static List<ItemStack> getMediumFirstTimeRewards(PlayerTier tier) {
        List<ItemStack> rewards = new ArrayList<>();
        
        switch (tier) {
            case EARLY_GAME:
                rewards.add(new ItemStack(Items.DIAMOND, 2));
                rewards.add(new ItemStack(Items.IRON_INGOT, 16));
                rewards.add(new ItemStack(Items.GOLDEN_APPLE, 2));
                break;
            case MID_GAME:
                rewards.add(new ItemStack(Items.DIAMOND, 5));
                rewards.add(new ItemStack(Items.EMERALD, 16));
                rewards.add(new ItemStack(Items.GOLDEN_APPLE, 3));
                break;
            case LATE_GAME:
            case END_GAME:
                rewards.add(new ItemStack(Items.DIAMOND, 8));
                rewards.add(new ItemStack(Items.NETHERITE_SCRAP, 1));
                rewards.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
                break;
        }
        
        return rewards;
    }
    
    private static List<ItemStack> getHardFirstTimeRewards(PlayerTier tier) {
        List<ItemStack> rewards = new ArrayList<>();
        
        switch (tier) {
            case EARLY_GAME:
                rewards.add(new ItemStack(Items.DIAMOND, 5));
                rewards.add(new ItemStack(Items.GOLDEN_APPLE, 3));
                rewards.add(new ItemStack(Items.IRON_BLOCK, 4));
                break;
            case MID_GAME:
                rewards.add(new ItemStack(Items.DIAMOND, 10));
                rewards.add(new ItemStack(Items.NETHERITE_SCRAP, 2));
                rewards.add(new ItemStack(Items.TOTEM_OF_UNDYING, 1));
                break;
            case LATE_GAME:
            case END_GAME:
                rewards.add(new ItemStack(Items.NETHERITE_INGOT, 1));
                rewards.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2));
                rewards.add(new ItemStack(Items.NETHER_STAR, 1));
                break;
        }
        
        return rewards;
    }
    
    private static List<ItemStack> getDailyRewards(PlayerTier tier, String difficulty) {
        List<ItemStack> rewards = new ArrayList<>();
        
        int baseAmount = switch (difficulty.toUpperCase()) {
            case "EASY" -> 1;
            case "MEDIUM" -> 2;
            case "HARD" -> 3;
            default -> 1;
        };
        
        switch (tier) {
            case EARLY_GAME:
                rewards.add(new ItemStack(Items.IRON_INGOT, baseAmount * 2));
                rewards.add(new ItemStack(Items.EXPERIENCE_BOTTLE, baseAmount));
                break;
            case MID_GAME:
                rewards.add(new ItemStack(Items.GOLD_INGOT, baseAmount * 2));
                rewards.add(new ItemStack(Items.EXPERIENCE_BOTTLE, baseAmount * 2));
                break;
            case LATE_GAME:
            case END_GAME:
                rewards.add(new ItemStack(Items.EMERALD, baseAmount * 3));
                rewards.add(new ItemStack(Items.EXPERIENCE_BOTTLE, baseAmount * 3));
                break;
        }
        
        return rewards;
    }
    
    /**
     * Give streak bonus for consecutive days of solving.
     */
    private static void giveStreakBonus(ServerPlayer player, int streak, PlayerTier tier, RewardResult result) {
        // Cap streak bonus at 30 days
        int effectiveStreak = Math.min(streak, 30);
        
        // Bonus XP based on streak
        int bonusXp = effectiveStreak * 10;
        player.giveExperiencePoints(bonusXp);
        result.addXp(bonusXp);
        result.setStreakBonus(true);
        
        // Every 7 days, give special item
        if (streak % 7 == 0) {
            ItemStack weeklyBonus = getWeeklyStreakReward(streak / 7, tier);
            if (!player.getInventory().add(weeklyBonus)) {
                player.drop(weeklyBonus, false);
            }
            result.addItem(weeklyBonus);
        }
    }
    
    private static ItemStack getWeeklyStreakReward(int weeks, PlayerTier tier) {
        return switch (weeks) {
            case 1 -> new ItemStack(Items.DIAMOND, (int)(3 * tier.multiplier));
            case 2 -> new ItemStack(Items.EMERALD_BLOCK, (int)(1 * tier.multiplier));
            case 3 -> new ItemStack(Items.GOLDEN_APPLE, (int)(3 * tier.multiplier));
            case 4 -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
            default -> new ItemStack(Items.NETHERITE_SCRAP, Math.min(weeks - 3, 4));
        };
    }
    
    /**
     * Check and give milestone rewards.
     */
    private static void checkMilestones(ServerPlayer player, int totalSolved, RewardResult result) {
        // Milestone rewards at specific counts
        ItemStack milestone = switch (totalSolved) {
            case 10 -> new ItemStack(Items.DIAMOND, 10);
            case 25 -> new ItemStack(Items.EMERALD_BLOCK, 5);
            case 50 -> new ItemStack(Items.NETHERITE_SCRAP, 4);
            case 100 -> new ItemStack(Items.NETHERITE_INGOT, 2);
            case 200 -> new ItemStack(Items.NETHER_STAR, 1);
            case 500 -> new ItemStack(Items.DRAGON_EGG, 1); // Super rare!
            default -> null;
        };
        
        if (milestone != null) {
            if (!player.getInventory().add(milestone)) {
                player.drop(milestone, false);
            }
            result.addItem(milestone);
            result.setMilestone(totalSolved);
        }
    }
    
    /**
     * Give random bonus drop for excitement.
     */
    private static void giveRandomBonus(ServerPlayer player, PlayerTier tier, RewardResult result) {
        Random random = new Random();
        
        ItemStack bonus = switch (random.nextInt(10)) {
            case 0, 1, 2 -> new ItemStack(Items.EMERALD, random.nextInt(5) + 1);
            case 3, 4 -> new ItemStack(Items.EXPERIENCE_BOTTLE, random.nextInt(10) + 5);
            case 5, 6 -> new ItemStack(Items.GOLDEN_APPLE, 1);
            case 7 -> new ItemStack(Items.ENCHANTED_BOOK, 1); // Could add random enchant
            case 8 -> new ItemStack(Items.NAME_TAG, 1);
            case 9 -> new ItemStack(Items.MUSIC_DISC_CAT, 1); // Rare!
            default -> new ItemStack(Items.EMERALD, 1);
        };
        
        if (!player.getInventory().add(bonus)) {
            player.drop(bonus, false);
        }
        result.addItem(bonus);
        result.setRandomBonus(true);
    }
    
    /**
     * Calculate XP reward based on various factors.
     */
    private static int calculateXpReward(String difficulty, boolean isFirstTime, 
                                         PlayerTier tier, int streak) {
        int base = switch (difficulty.toUpperCase()) {
            case "EASY" -> 50;
            case "MEDIUM" -> 100;
            case "HARD" -> 200;
            default -> 75;
        };
        
        // First time bonus
        if (isFirstTime) {
            base *= 2;
        }
        
        // Streak bonus (up to +50%)
        double streakMultiplier = 1.0 + Math.min(streak * 0.05, 0.5);
        
        // Tier multiplier
        base = (int)(base * tier.multiplier * streakMultiplier);
        
        return base;
    }
    
    // ==================== Result Class ====================
    
    /**
     * Result object containing all reward information.
     */
    public static class RewardResult {
        private final List<ItemStack> items = new ArrayList<>();
        private int totalXp = 0;
        private boolean streakBonus = false;
        private boolean randomBonus = false;
        private int milestone = 0;
        
        public void addItem(ItemStack stack) {
            items.add(stack.copy());
        }
        
        public void addXp(int xp) {
            totalXp += xp;
        }
        
        public void setStreakBonus(boolean value) {
            streakBonus = value;
        }
        
        public void setRandomBonus(boolean value) {
            randomBonus = value;
        }
        
        public void setMilestone(int count) {
            milestone = count;
        }
        
        public List<ItemStack> getItems() {
            return Collections.unmodifiableList(items);
        }
        
        public int getTotalXp() {
            return totalXp;
        }
        
        public boolean hasStreakBonus() {
            return streakBonus;
        }
        
        public boolean hasRandomBonus() {
            return randomBonus;
        }
        
        public int getMilestone() {
            return milestone;
        }
        
        public boolean hasMilestone() {
            return milestone > 0;
        }
        
        /**
         * Generate a summary message for the player.
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Rewards: ");
            
            for (ItemStack stack : items) {
                sb.append(stack.getCount()).append("x ")
                  .append(stack.getHoverName().getString()).append(", ");
            }
            
            sb.append("+").append(totalXp).append(" XP");
            
            if (streakBonus) sb.append(" [Streak Bonus!]");
            if (randomBonus) sb.append(" [Lucky Drop!]");
            if (milestone > 0) sb.append(" [Milestone: ").append(milestone).append(" problems!]");
            
            return sb.toString();
        }
    }
}
