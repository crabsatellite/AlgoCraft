package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.TrophyBlock;
import com.crabmods.algocraft.TrophyBlockEntity;
import com.crabmods.algocraft.item.TrophyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTrophyBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AlgoCraft.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AlgoCraft.MODID);
    public static final DeferredBlock<TrophyBlock> BRONZE = trophy("bronze_trophy");
    public static final DeferredBlock<TrophyBlock> SILVER = trophy("silver_trophy");
    public static final DeferredBlock<TrophyBlock> GOLD = trophy("gold_trophy");
    public static final DeferredBlock<TrophyBlock> DIAMOND = trophy("diamond_trophy");
    public static final DeferredBlock<TrophyBlock> NETHERITE = trophy("netherite_trophy");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrophyBlockEntity>> TROPHY_ENTITY = ENTITIES.register("trophy",
            () -> BlockEntityType.Builder.of(TrophyBlockEntity::new, BRONZE.get(), SILVER.get(), GOLD.get(), DIAMOND.get(), NETHERITE.get()).build(null));

    private ModTrophyBlocks() {}
    private static DeferredBlock<TrophyBlock> trophy(String name) {
        return BLOCKS.register(name, () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion()));
    }
    public static DeferredBlock<TrophyBlock> forTier(TrophyItem.TrophyTier tier) {
        return switch (tier) {
            case BRONZE -> BRONZE;
            case SILVER -> SILVER;
            case GOLD -> GOLD;
            case DIAMOND -> DIAMOND;
            case NETHERITE -> NETHERITE;
        };
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
    }
}
