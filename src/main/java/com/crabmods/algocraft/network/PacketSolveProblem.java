package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

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
                ServerLevel level = player.serverLevel();
                AlgoCraftSavedData data = AlgoCraftSavedData.get(level);
                
                long now = System.currentTimeMillis();
                boolean isFirstTime = !data.isProblemSolved(player.getUUID(), payload.problemId());
                long lastSolved = data.getLastSolvedTime(player.getUUID(), payload.problemId());
                
                boolean giveReward = false;
                boolean isDaily = false;
                
                if (isFirstTime) {
                    giveReward = true;
                } else {
                    // Check if it's a new day
                    LocalDate lastDate = Instant.ofEpochMilli(lastSolved).atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
                    
                    if (today.isAfter(lastDate)) {
                        giveReward = true;
                        isDaily = true;
                    }
                }
                
                if (giveReward) {
                    if (isFirstTime) {
                        // First time rewards (Rich)
                        if (payload.difficulty().equalsIgnoreCase("EASY")) {
                            player.getInventory().add(new ItemStack(Items.IRON_INGOT, 5));
                            player.getInventory().add(new ItemStack(Items.EXPERIENCE_BOTTLE, 5));
                        } else if (payload.difficulty().equalsIgnoreCase("MEDIUM")) {
                            player.getInventory().add(new ItemStack(Items.DIAMOND, 3));
                            player.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 1));
                        } else if (payload.difficulty().equalsIgnoreCase("HARD")) {
                            player.getInventory().add(new ItemStack(Items.NETHERITE_SCRAP, 2));
                            player.getInventory().add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
                        } else {
                            player.getInventory().add(new ItemStack(Items.EMERALD, 10));
                        }
                        player.sendSystemMessage(Component.literal("§aFirst clear! You received a rich reward!"));
                    } else {
                        // Daily rewards (Guaranteed small)
                        player.getInventory().add(new ItemStack(Items.GOLD_NUGGET, 3));
                        player.getInventory().add(new ItemStack(Items.EXPERIENCE_BOTTLE, 1));
                        player.sendSystemMessage(Component.literal("§eDaily clear! You received a small reward."));
                    }
                    
                    // Update progress
                    data.setProblemSolved(player.getUUID(), payload.problemId(), now);
                    
                    // Sync to client
                    PacketDistributor.sendToPlayer(player, new PacketSyncProgress(data.getPlayerProgress(player.getUUID())));
                } else {
                    player.sendSystemMessage(Component.literal("§7You have already cleared this problem today. Come back tomorrow!"));
                }
            }
        });
    }
}
