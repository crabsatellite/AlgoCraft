package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.logic.ProgressManager;
import io.netty.buffer.ByteBuf;
import com.crabmods.algocraft.network.compat.ByteBufCodecs;
import com.crabmods.algocraft.network.compat.StreamCodec;
import com.crabmods.algocraft.network.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.crabmods.algocraft.network.compat.PacketDistributor;
import com.crabmods.algocraft.network.compat.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record PacketSyncProgress(Map<String, Long> progress) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncProgress> TYPE = new CustomPacketPayload.Type<>(new ResourceLocation(AlgoCraft.MODID, "sync_progress"));
    
    public static final StreamCodec<ByteBuf, PacketSyncProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), PacketSyncProgress::progress,
            PacketSyncProgress::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSyncProgress payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update client side progress
            ProgressManager.updateFromPacket(payload.progress());
        });
    }

    public static void sendSafely(ServerPlayer player, Map<String, Long> progress) {
        try {
            PacketDistributor.sendToPlayer(player, new PacketSyncProgress(progress));
        } catch (UnsupportedOperationException e) {
            AlgoCraft.LOGGER.debug("Skipping progress sync for client without algocraft progress payload support: {}",
                    e.getMessage());
        }
    }
}
