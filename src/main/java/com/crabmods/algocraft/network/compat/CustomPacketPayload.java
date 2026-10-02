package com.crabmods.algocraft.network.compat;
import net.minecraft.resources.ResourceLocation;
/** Shared packet identity for the Forge SimpleChannel transport. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();
    record Type<T extends CustomPacketPayload>(ResourceLocation id) {}
}
