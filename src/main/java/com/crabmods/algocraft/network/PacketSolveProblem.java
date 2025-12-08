package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSolveProblem(String problemId, String difficulty) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSolveProblem> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "solve_problem"));
    
    public static final StreamCodec<ByteBuf, PacketSolveProblem> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketSolveProblem::problemId,
            ByteBufCodecs.STRING_UTF8, PacketSolveProblem::difficulty,
            PacketSolveProblem::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSolveProblem payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Give rewards
                if (payload.difficulty().equals("easy")) {
                    player.getInventory().add(new ItemStack(Items.IRON_INGOT, 3));
                } else if (payload.difficulty().equals("medium")) {
                    player.getInventory().add(new ItemStack(Items.DIAMOND, 1));
                } else if (payload.difficulty().equals("hard")) {
                    player.getInventory().add(new ItemStack(Items.NETHERITE_SCRAP, 1));
                } else if (payload.difficulty().equals("insane")) {
                    player.getInventory().add(new ItemStack(Items.NETHERITE_INGOT, 1));
                }
            }
        });
    }
}
