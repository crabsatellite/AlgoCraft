package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent when a player submits an incorrect solution.
 * This resets the consecutive correct counter.
 */
public record PacketSubmissionFailed(String problemId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSubmissionFailed> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "submission_failed"));
    
    public static final StreamCodec<ByteBuf, PacketSubmissionFailed> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketSubmissionFailed::problemId,
            PacketSubmissionFailed::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSubmissionFailed payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                AlgoCraftSavedData data = AlgoCraftSavedData.get(level);
                
                // Reset consecutive correct count on failure
                data.resetConsecutiveCorrect(player.getUUID());
                
                AlgoCraft.LOGGER.debug("Player {} failed submission for problem {}, consecutive correct reset",
                    player.getName().getString(), payload.problemId());
            }
        });
    }
}
