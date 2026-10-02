package com.crabmods.algocraft.client;
import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.logic.catalog.ClientCatalog;
import com.crabmods.algocraft.web.AlgoCraftWebServer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = AlgoCraft.MODID, value = Dist.CLIENT)
public final class CatalogClientEvents {
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientCatalog.connect(Minecraft.getInstance().gameDirectory.toPath());
        AlgoCraftWebServer.start();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ServerSubmissionBridge.disconnect();
        ClientCatalog.disconnect();
        AlgoCraftWebServer.stop();
    }
}
