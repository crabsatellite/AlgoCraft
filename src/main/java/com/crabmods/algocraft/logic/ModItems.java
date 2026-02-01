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
        DeferredRegister.create(Registries.ITEM, AlgoCraft.MOD_ID);
    
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
