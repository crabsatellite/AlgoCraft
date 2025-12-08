package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.server.SolvingPlayerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSetSolvingState(boolean isSolving) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSetSolvingState> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "set_solving_state"));
    
    public static final StreamCodec<ByteBuf, PacketSetSolvingState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PacketSetSolvingState::isSolving,
            PacketSetSolvingState::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSetSolvingState payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SolvingPlayerManager.setSolving(player, payload.isSolving());
            }
        });
    }
}
