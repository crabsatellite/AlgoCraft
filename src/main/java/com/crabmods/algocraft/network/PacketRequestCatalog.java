package com.crabmods.algocraft.network;
import com.crabmods.algocraft.AlgoCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record PacketRequestCatalog(String revision) implements CustomPacketPayload {
    public static final Type<PacketRequestCatalog> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "request_catalog"));
    public static final StreamCodec<ByteBuf, PacketRequestCatalog> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(64), PacketRequestCatalog::revision, PacketRequestCatalog::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(PacketRequestCatalog payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
                com.crabmods.algocraft.server.ServerBankService.request(player, payload.revision());
        });
    }
}
