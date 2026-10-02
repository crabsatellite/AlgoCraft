package com.crabmods.algocraft.network;
import com.crabmods.algocraft.AlgoCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record PacketCatalogManifest(String serverId, String revision, int bytes, int chunks) implements CustomPacketPayload {
    public static final Type<PacketCatalogManifest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "catalog_manifest"));
    public static final StreamCodec<ByteBuf, PacketCatalogManifest> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(36), PacketCatalogManifest::serverId,
        ByteBufCodecs.stringUtf8(64), PacketCatalogManifest::revision,
        ByteBufCodecs.VAR_INT, PacketCatalogManifest::bytes,
        ByteBufCodecs.VAR_INT, PacketCatalogManifest::chunks, PacketCatalogManifest::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(PacketCatalogManifest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                com.crabmods.algocraft.logic.catalog.ClientCatalog.manifest(payload.serverId(), payload.revision(), payload.bytes(), payload.chunks())
                    .whenComplete((request, error) -> context.enqueueWork(() -> {
                        if (error != null) context.disconnect(net.minecraft.network.chat.Component.translatable("algocraft.bank.invalid_manifest"));
                        else if (Boolean.TRUE.equals(request)) context.reply(new PacketRequestCatalog(payload.revision()));
                    }));
            } catch (java.io.IOException e) { context.disconnect(net.minecraft.network.chat.Component.translatable("algocraft.bank.invalid_manifest")); }
        });
    }
}
