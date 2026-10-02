package com.crabmods.algocraft.network.compat;
import com.crabmods.algocraft.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
public final class PacketDistributor {
    private PacketDistributor() {}
    public static void sendToServer(CustomPacketPayload packet) { NetworkHandler.CHANNEL.sendToServer(packet); }
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        NetworkHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
