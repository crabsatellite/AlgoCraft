package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.server.SolvingPlayerManager;
import io.netty.buffer.ByteBuf;
import com.crabmods.algocraft.network.compat.ByteBufCodecs;
import com.crabmods.algocraft.network.compat.StreamCodec;
import com.crabmods.algocraft.network.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.crabmods.algocraft.network.compat.IPayloadContext;

public record PacketSetSolvingState(boolean isSolving, String problemId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSetSolvingState> TYPE = new CustomPacketPayload.Type<>(new ResourceLocation(AlgoCraft.MODID, "set_solving_state"));
    private static final int MAX_PROBLEM_ID_LENGTH = 256;

    public static final StreamCodec<ByteBuf, PacketSetSolvingState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PacketSetSolvingState::isSolving,
            ByteBufCodecs.stringUtf8(MAX_PROBLEM_ID_LENGTH), PacketSetSolvingState::problemId,
            PacketSetSolvingState::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSetSolvingState payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (!payload.isSolving()) {
                    SolvingPlayerManager.setSolving(player, false, "");
                    return;
                }
                if (payload.problemId() != null
                        && !payload.problemId().isBlank()
                        && com.crabmods.algocraft.server.ServerBankService.current() != null
                        && com.crabmods.algocraft.server.ServerBankService.current().get(payload.problemId()) != null) {
                    SolvingPlayerManager.setSolving(player, true, payload.problemId());
                }
            }
        });
    }
}
