package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// @EventBusSubscriber(modid = AlgoCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(PacketCatalogManifest.TYPE, PacketCatalogManifest.STREAM_CODEC, PacketCatalogManifest::handle);
        registrar.playToClient(PacketCatalogChunk.TYPE, PacketCatalogChunk.STREAM_CODEC, PacketCatalogChunk::handle);
        registrar.playToClient(PacketSubmissionResult.TYPE, PacketSubmissionResult.STREAM_CODEC, PacketSubmissionResult::handle);
        registrar.playToServer(PacketRequestCatalog.TYPE, PacketRequestCatalog.STREAM_CODEC, PacketRequestCatalog::handle);
        registrar.playToServer(
            PacketSubmitSolution.TYPE,
            PacketSubmitSolution.STREAM_CODEC,
            PacketSubmitSolution::handle
        );
        registrar.playToServer(
            PacketSetSolvingState.TYPE,
            PacketSetSolvingState.STREAM_CODEC,
            PacketSetSolvingState::handle
        );
        registrar.playToClient(
            PacketSyncProgress.TYPE,
            PacketSyncProgress.STREAM_CODEC,
            PacketSyncProgress::handle
        );
    }
}
