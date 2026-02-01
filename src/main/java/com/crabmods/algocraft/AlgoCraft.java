package com.crabmods.algocraft;

import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.ModItems;
import com.crabmods.algocraft.logic.ProgressManager;
import com.crabmods.algocraft.logic.repo.OfficialRepositorySync;
import com.crabmods.algocraft.network.NetworkHandler;
import com.crabmods.algocraft.server.SolvingPlayerManager;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import com.crabmods.algocraft.web.AlgoCraftWebServer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(AlgoCraft.MODID)
public class AlgoCraft
{
    public static final String MODID = "algocraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> ALGORITHM_COMPUTER_BLOCK = BLOCKS.register("algorithm_computer", () -> new AlgorithmComputerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> ALGORITHM_COMPUTER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("algorithm_computer", ALGORITHM_COMPUTER_BLOCK);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALGOCRAFT_TAB = CREATIVE_MODE_TABS.register("algocraft_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.algocraft"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ALGORITHM_COMPUTER_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ALGORITHM_COMPUTER_BLOCK_ITEM.get());
                // Trophy items
                output.accept(ModItems.BRONZE_TROPHY.get());
                output.accept(ModItems.SILVER_TROPHY.get());
                output.accept(ModItems.GOLD_TROPHY.get());
                output.accept(ModItems.DIAMOND_TROPHY.get());
                output.accept(ModItems.NETHERITE_TROPHY.get());
            }).build());

    public AlgoCraft(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ClientModEvents::onClientSetup);
        modEventBus.addListener(NetworkHandler::register);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModItems.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(SolvingPlayerManager.class);

        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("AlgoCraft setup complete");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(ALGORITHM_COMPUTER_BLOCK_ITEM);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("AlgoCraft server starting");
        AlgoCraftWebServer.start();
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("AlgoCraft server stopping - cleaning up resources...");
        
        // Stop web server
        AlgoCraftWebServer.stop();
        
        // Shutdown executor services
        CodeExecutor.shutdown();
        OfficialRepositorySync.shutdown();
        
        // Force save any pending progress
        ProgressManager.saveProgressImmediate();
        
        LOGGER.info("AlgoCraft cleanup complete");
    }

    // @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("AlgoCraft client setup");
        }
    }
}
