package com.crabmods.algocraft.network.compat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
/** Main-thread work and connection replies use the Forge network context. */
public final class IPayloadContext {
    private final NetworkEvent.Context context;
    public IPayloadContext(NetworkEvent.Context context) { this.context = context; }
    public Player player() { return context.getSender(); }
    public void enqueueWork(Runnable work) { context.enqueueWork(work); }
    public void disconnect(Component reason) { context.getNetworkManager().disconnect(reason); }
    public void reply(CustomPacketPayload packet) {
        if (context.getSender() == null) PacketDistributor.sendToServer(packet);
        else PacketDistributor.sendToPlayer(context.getSender(), packet);
    }
}
