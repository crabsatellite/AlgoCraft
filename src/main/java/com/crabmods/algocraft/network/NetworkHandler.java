package com.crabmods.algocraft.network;
import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.network.compat.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.BiConsumer;
public final class NetworkHandler {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AlgoCraft.MODID, "main"), () -> "2", "2"::equals, "2"::equals);
    private static boolean registered;
    public static synchronized void register() {
        if (registered) return;
        register(0, PacketCatalogManifest.class, PacketCatalogManifest.STREAM_CODEC, PacketCatalogManifest::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(1, PacketCatalogChunk.class, PacketCatalogChunk.STREAM_CODEC, PacketCatalogChunk::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(2, PacketSubmissionResult.class, PacketSubmissionResult.STREAM_CODEC, PacketSubmissionResult::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(3, PacketRequestCatalog.class, PacketRequestCatalog.STREAM_CODEC, PacketRequestCatalog::handle, NetworkDirection.PLAY_TO_SERVER);
        register(4, PacketSubmitSolution.class, PacketSubmitSolution.STREAM_CODEC, PacketSubmitSolution::handle, NetworkDirection.PLAY_TO_SERVER);
        register(5, PacketSetSolvingState.class, PacketSetSolvingState.STREAM_CODEC, PacketSetSolvingState::handle, NetworkDirection.PLAY_TO_SERVER);
        register(6, PacketSyncProgress.class, PacketSyncProgress.STREAM_CODEC, PacketSyncProgress::handle, NetworkDirection.PLAY_TO_CLIENT);
        registered = true;
    }
    private static <T extends CustomPacketPayload> void register(int id, Class<T> type, StreamCodec<ByteBuf,T> codec,
            BiConsumer<T,IPayloadContext> handler, NetworkDirection direction) {
        CHANNEL.messageBuilder(type,id,direction).encoder((packet, buffer) -> codec.encode(buffer,packet)).decoder(codec::decode)
                .consumerNetworkThread((packet,supplied) -> {
                    var context = supplied.get();
                    handler.accept(packet,new IPayloadContext(context));
                    context.setPacketHandled(true);
                }).add();
    }
}
