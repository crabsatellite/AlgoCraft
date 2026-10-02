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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;

public final class ModTrophyBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AlgoCraft.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AlgoCraft.MODID);
    public static final RegistryObject<TrophyBlock> BRONZE = trophy("bronze_trophy");
    public static final RegistryObject<TrophyBlock> SILVER = trophy("silver_trophy");
    public static final RegistryObject<TrophyBlock> GOLD = trophy("gold_trophy");
    public static final RegistryObject<TrophyBlock> DIAMOND = trophy("diamond_trophy");
    public static final RegistryObject<TrophyBlock> NETHERITE = trophy("netherite_trophy");
    public static final RegistryObject<BlockEntityType<TrophyBlockEntity>> TROPHY_ENTITY = ENTITIES.register("trophy",
            () -> BlockEntityType.Builder.of(TrophyBlockEntity::new, BRONZE.get(), SILVER.get(), GOLD.get(), DIAMOND.get(), NETHERITE.get()).build(null));

    private ModTrophyBlocks() {}
    private static RegistryObject<TrophyBlock> trophy(String name) {
        return BLOCKS.register(name, () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion()));
    }
    public static RegistryObject<TrophyBlock> forTier(TrophyItem.TrophyTier tier) {
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
