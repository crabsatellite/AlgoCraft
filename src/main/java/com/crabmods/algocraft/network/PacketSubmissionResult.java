package com.crabmods.algocraft.network;
import com.crabmods.algocraft.AlgoCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record PacketSubmissionResult(String requestId, boolean accepted, String message, int passed, int total, long time) implements CustomPacketPayload {
    public static final Type<PacketSubmissionResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "submission_result"));
    public static final StreamCodec<ByteBuf, PacketSubmissionResult> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(36), PacketSubmissionResult::requestId,
        ByteBufCodecs.BOOL, PacketSubmissionResult::accepted,
        ByteBufCodecs.stringUtf8(4096), PacketSubmissionResult::message,
        ByteBufCodecs.VAR_INT, PacketSubmissionResult::passed,
        ByteBufCodecs.VAR_INT, PacketSubmissionResult::total,
        ByteBufCodecs.VAR_LONG, PacketSubmissionResult::time, PacketSubmissionResult::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static PacketSubmissionResult of(String requestId, com.crabmods.algocraft.logic.SubmissionResult result) {
        String message = com.crabmods.algocraft.logic.catalog.PublicJudgeFeedback.encode(result);
        return new PacketSubmissionResult(requestId, result.isSuccess(), message.substring(0, Math.min(message.length(), 4096)),
            result.getPassedCount(), result.getTotalCount(), result.getExecutionTimeMs());
    }
    public static void handle(PacketSubmissionResult payload, IPayloadContext context) {
        context.enqueueWork(() -> com.crabmods.algocraft.client.ServerSubmissionBridge.complete(payload));
    }
}
