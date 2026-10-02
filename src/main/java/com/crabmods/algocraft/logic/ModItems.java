package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.item.TrophyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for all AlgoCraft items, including trophy items.
 */
public class ModItems {
    
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(Registries.ITEM, AlgoCraft.MODID);
    
    // Trophy items for different achievement rarities
    public static final DeferredHolder<Item, TrophyItem> BRONZE_TROPHY = ITEMS.register(
        "bronze_trophy", 
        () -> new TrophyItem(TrophyItem.TrophyTier.BRONZE)
    );
    
    public static final DeferredHolder<Item, TrophyItem> SILVER_TROPHY = ITEMS.register(
        "silver_trophy", 
        () -> new TrophyItem(TrophyItem.TrophyTier.SILVER)
    );
    
    public static final DeferredHolder<Item, TrophyItem> GOLD_TROPHY = ITEMS.register(
        "gold_trophy", 
        () -> new TrophyItem(TrophyItem.TrophyTier.GOLD)
    );
    
    public static final DeferredHolder<Item, TrophyItem> DIAMOND_TROPHY = ITEMS.register(
        "diamond_trophy", 
        () -> new TrophyItem(TrophyItem.TrophyTier.DIAMOND)
    );
    
    public static final DeferredHolder<Item, TrophyItem> NETHERITE_TROPHY = ITEMS.register(
        "netherite_trophy", 
        () -> new TrophyItem(TrophyItem.TrophyTier.NETHERITE)
    );
    
    /**
     * Register all items with the mod event bus.
     * Call this in AlgoCraft's constructor.
     */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
    
    /**
     * Check whether an achievement trophy id is backed by a registered trophy item.
     */
    public static boolean isKnownTrophyItemId(String itemId) {
        return switch (itemId) {
            case "bronze_trophy", "silver_trophy", "gold_trophy", "diamond_trophy", "netherite_trophy" -> true;
            default -> false;
        };
    }

    /**
     * Get the display/effect tier represented by a trophy item id.
     */
    public static TrophyItem.TrophyTier getTrophyTierForItemId(String itemId) {
        return switch (itemId) {
            case "bronze_trophy" -> TrophyItem.TrophyTier.BRONZE;
            case "silver_trophy" -> TrophyItem.TrophyTier.SILVER;
            case "gold_trophy" -> TrophyItem.TrophyTier.GOLD;
            case "diamond_trophy" -> TrophyItem.TrophyTier.DIAMOND;
            case "netherite_trophy" -> TrophyItem.TrophyTier.NETHERITE;
            default -> throw new IllegalArgumentException("Unknown trophy item id: " + itemId);
        };
    }

    /**
     * Get the trophy item explicitly declared by an achievement.
     */
    public static TrophyItem getTrophyForItemId(String itemId) {
        return switch (itemId) {
            case "bronze_trophy" -> BRONZE_TROPHY.get();
            case "silver_trophy" -> SILVER_TROPHY.get();
            case "gold_trophy" -> GOLD_TROPHY.get();
            case "diamond_trophy" -> DIAMOND_TROPHY.get();
            case "netherite_trophy" -> NETHERITE_TROPHY.get();
            default -> null;
        };
    }

    /**
     * Get the appropriate trophy item for an achievement rarity.
     */
    public static TrophyItem getTrophyForRarity(AchievementRegistry.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> BRONZE_TROPHY.get();
            case UNCOMMON -> SILVER_TROPHY.get();
            case RARE -> GOLD_TROPHY.get();
            case EPIC, LEGENDARY -> DIAMOND_TROPHY.get();
            case MYTHIC -> NETHERITE_TROPHY.get();
        };
    }
}
