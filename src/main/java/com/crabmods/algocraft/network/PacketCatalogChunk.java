package com.crabmods.algocraft.network;
import com.crabmods.algocraft.AlgoCraft;
import io.netty.buffer.ByteBuf;
import com.crabmods.algocraft.network.compat.*;
import com.crabmods.algocraft.network.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.crabmods.algocraft.network.compat.IPayloadContext;
public record PacketCatalogChunk(String revision, int index, byte[] data) implements CustomPacketPayload {
    public static final Type<PacketCatalogChunk> TYPE = new Type<>(new ResourceLocation(AlgoCraft.MODID, "catalog_chunk"));
    public static final StreamCodec<ByteBuf, PacketCatalogChunk> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(64), PacketCatalogChunk::revision,
        ByteBufCodecs.VAR_INT, PacketCatalogChunk::index,
        ByteBufCodecs.byteArray(com.crabmods.algocraft.logic.catalog.PublishedCatalog.CHUNK_BYTES), PacketCatalogChunk::data, PacketCatalogChunk::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(PacketCatalogChunk payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try { com.crabmods.algocraft.logic.catalog.ClientCatalog.chunk(payload.revision(), payload.index(), payload.data()); }
            catch (java.io.IOException e) { context.disconnect(net.minecraft.network.chat.Component.translatable("algocraft.bank.invalid_chunk")); }
        });
    }
}
