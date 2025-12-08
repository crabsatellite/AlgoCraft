package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.logic.ProgressManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record PacketSyncProgress(Map<String, Long> progress) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncProgress> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "sync_progress"));
    
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
}
