package com.crabmods.algocraft.item;

import com.crabmods.algocraft.logic.AchievementRegistry;
import com.crabmods.algocraft.logic.ModTrophyBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Trophy item that represents an earned achievement.
 * Each trophy stores metadata about when and who earned it.
 */
public class TrophyItem extends BlockItem {
    
    public static final String TAG_ACHIEVEMENT_ID = "AchievementId";
    public static final String TAG_PLAYER_NAME = "PlayerName";
    public static final String TAG_PLAYER_UUID = "PlayerUUID";
    public static final String TAG_TIMESTAMP = "Timestamp";
    public static final String TAG_EXTRA_DATA = "ExtraData";
    
    private final TrophyTier tier;
    
    public TrophyItem(TrophyTier tier) {
        super(ModTrophyBlocks.forTier(tier).get(), new Item.Properties()
            .stacksTo(1)
            .rarity(tier.getItemRarity())
            .fireResistant()  // Trophies are precious!
        );
        this.tier = tier;
    }
    
    public TrophyTier getTier() {
        return tier;
    }
    
    @Override
    public Component getName(ItemStack stack) {
        // Get achievement info from stack
        String achievementId = getAchievementId(stack);
        if (achievementId != null) {
            AchievementRegistry.Achievement achievement = AchievementRegistry.get(achievementId);
            if (achievement != null) {
                return achievement.getName();
            }
        }
        
        // Fallback to generic trophy name
        return Component.translatable(tier.getTranslationKey())
            .withStyle(tier.getChatColor());
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Get all data from NBT in one call to avoid repeated copyTag()
        CompoundTag tag = getCustomTag(stack);
        if (tag == null) {
            tooltip.add(Component.translatable("item.algocraft.trophy.generic_desc")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        
        String achievementId = tag.contains(TAG_ACHIEVEMENT_ID) ? tag.getString(TAG_ACHIEVEMENT_ID) : null;
        String playerName = tag.contains(TAG_PLAYER_NAME) ? tag.getString(TAG_PLAYER_NAME) : null;
        long timestamp = tag.contains(TAG_TIMESTAMP) ? tag.getLong(TAG_TIMESTAMP) : 0;
        
        if (achievementId != null) {
            AchievementRegistry.Achievement achievement = AchievementRegistry.get(achievementId);
            if (achievement != null) {
                // Get full tooltip from achievement
                List<Component> achievementTooltip = achievement.getTrophyTooltip(
                    playerName != null ? playerName : "Unknown",
                    timestamp
                );
                
                // Skip the first line (name) since it's already shown as item name
                for (int i = 1; i < achievementTooltip.size(); i++) {
                    tooltip.add(achievementTooltip.get(i));
                }
                return;
            }
        }
        
        // Fallback generic tooltip
        tooltip.add(Component.translatable("item.algocraft.trophy.generic_desc")
            .withStyle(ChatFormatting.GRAY));
        
        if (playerName != null) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("algocraft.trophy.awarded_to", playerName)
                .withStyle(ChatFormatting.GOLD));
        }
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // Legendary and Mythic trophies have enchantment glint
        String achievementId = getAchievementId(stack);
        if (achievementId != null) {
            AchievementRegistry.Achievement achievement = AchievementRegistry.get(achievementId);
            if (achievement != null) {
                return achievement.getRarity() == AchievementRegistry.Rarity.LEGENDARY ||
                       achievement.getRarity() == AchievementRegistry.Rarity.MYTHIC;
            }
        }
        return tier == TrophyTier.DIAMOND || tier == TrophyTier.NETHERITE;
    }
    
    // ==================== Static Helper Methods ====================
    
    /**
     * Create a trophy stack with full achievement data.
     */
    public static ItemStack createTrophy(Item trophyItem, AchievementRegistry.Achievement achievement,
                                         String playerName, String playerUuid, long timestamp) {
        ItemStack stack = new ItemStack(trophyItem);
        
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ACHIEVEMENT_ID, achievement.getId());
        tag.putString(TAG_PLAYER_NAME, playerName);
        tag.putString(TAG_PLAYER_UUID, playerUuid);
        tag.putLong(TAG_TIMESTAMP, timestamp);
        
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(achievement.getTrophyModelData()));
        
        return stack;
    }
    
    /**
     * Create a trophy with extra achievement-specific data.
     */
    public static ItemStack createTrophy(Item trophyItem, AchievementRegistry.Achievement achievement,
                                         String playerName, String playerUuid, long timestamp,
                                         CompoundTag extraData) {
        ItemStack stack = createTrophy(trophyItem, achievement, playerName, playerUuid, timestamp);
        
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            tag.put(TAG_EXTRA_DATA, extraData);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        
        return stack;
    }
    
    public static String getAchievementId(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack);
        return tag != null && tag.contains(TAG_ACHIEVEMENT_ID) ? tag.getString(TAG_ACHIEVEMENT_ID) : null;
    }
    
    public static String getPlayerName(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack);
        return tag != null && tag.contains(TAG_PLAYER_NAME) ? tag.getString(TAG_PLAYER_NAME) : null;
    }
    
    public static String getPlayerUuid(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack);
        return tag != null && tag.contains(TAG_PLAYER_UUID) ? tag.getString(TAG_PLAYER_UUID) : null;
    }
    
    public static long getTimestamp(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack);
        return tag != null && tag.contains(TAG_TIMESTAMP) ? tag.getLong(TAG_TIMESTAMP) : 0;
    }
    
    public static CompoundTag getExtraData(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack);
        return tag != null && tag.contains(TAG_EXTRA_DATA) ? tag.getCompound(TAG_EXTRA_DATA) : new CompoundTag();
    }
    
    /**
     * Helper method to get custom tag only once (avoids repeated copyTag() calls).
     */
    private static CompoundTag getCustomTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : null;
    }
    
    // ==================== Trophy Tiers ====================
    
    public enum TrophyTier {
        BRONZE(Rarity.COMMON, ChatFormatting.GOLD, "bronze_trophy"),
        SILVER(Rarity.UNCOMMON, ChatFormatting.GRAY, "silver_trophy"),
        GOLD(Rarity.RARE, ChatFormatting.YELLOW, "gold_trophy"),
        DIAMOND(Rarity.EPIC, ChatFormatting.AQUA, "diamond_trophy"),
        NETHERITE(Rarity.EPIC, ChatFormatting.DARK_RED, "netherite_trophy");
        
        private final Rarity itemRarity;
        private final ChatFormatting chatColor;
        private final String itemId;
        
        TrophyTier(Rarity itemRarity, ChatFormatting chatColor, String itemId) {
            this.itemRarity = itemRarity;
            this.chatColor = chatColor;
            this.itemId = itemId;
        }
        
        public Rarity getItemRarity() {
            return itemRarity;
        }
        
        public ChatFormatting getChatColor() {
            return chatColor;
        }

        public String getItemId() {
            return itemId;
        }

        public String getTranslationKey() {
            return "item.algocraft." + itemId;
        }
    }
}
