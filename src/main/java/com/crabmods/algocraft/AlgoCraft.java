package com.crabmods.algocraft;

import com.crabmods.algocraft.client.ClientHooks;
import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.ModItems;
import com.crabmods.algocraft.logic.ModTrophyBlocks;
import com.crabmods.algocraft.logic.ProgressManager;
import com.crabmods.algocraft.network.NetworkHandler;
import com.crabmods.algocraft.network.PacketSubmitSolution;
import com.crabmods.algocraft.client.test.IdeClientSmokeTest;
import com.crabmods.algocraft.server.SolvingPlayerManager;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import com.crabmods.algocraft.web.AlgoCraftWebServer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import net.minecraftforge.registries.RegistryObject;

import net.minecraftforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(AlgoCraft.MODID)
public class AlgoCraft
{
    public static final String MODID = "algocraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> ALGORITHM_COMPUTER_BLOCK = BLOCKS.register("algorithm_computer", () -> new AlgorithmComputerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<BlockItem> ALGORITHM_COMPUTER_BLOCK_ITEM = ITEMS.register("algorithm_computer", () -> new BlockItem(ALGORITHM_COMPUTER_BLOCK.get(), new net.minecraft.world.item.Item.Properties()));

    public static final RegistryObject<CreativeModeTab> ALGOCRAFT_TAB = CREATIVE_MODE_TABS.register("algocraft_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.algocraft"))
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

    public AlgoCraft()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ClientModEvents::onClientSetup);
        

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModTrophyBlocks.register(modEventBus);
        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(SolvingPlayerManager.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(NetworkHandler::register);
        LOGGER.info("AlgoCraft setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("AlgoCraft server starting");
        com.crabmods.algocraft.server.ServerBankService.start(event.getServer());
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("AlgoCraft server stopping - cleaning up resources...");
        
        com.crabmods.algocraft.server.ServerBankService.stop();

        // Stop server-side judge submissions before shutting down the execution engine.
        PacketSubmitSolution.shutdownJudgeExecutor();
        
        // Shutdown executor services
        CodeExecutor.shutdown();

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
            ClientHooks.registerWebServerClientBridge();
            IdeClientSmokeTest.maybeRegister();
            com.crabmods.algocraft.client.test.BankMultiplayerClientTest.maybeRegister();
        }
    }
}
