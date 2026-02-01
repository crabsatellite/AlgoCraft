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
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
            PacketSolveProblem.TYPE,
            PacketSolveProblem.STREAM_CODEC,
            PacketSolveProblem::handle
        );
        registrar.playToServer(
            PacketSetSolvingState.TYPE,
            PacketSetSolvingState.STREAM_CODEC,
            PacketSetSolvingState::handle
        );
        registrar.playToServer(
            PacketSubmissionFailed.TYPE,
            PacketSubmissionFailed.STREAM_CODEC,
            PacketSubmissionFailed::handle
        );
        registrar.playToClient(
            PacketSyncProgress.TYPE,
            PacketSyncProgress.STREAM_CODEC,
            PacketSyncProgress::handle
        );
    }
}
