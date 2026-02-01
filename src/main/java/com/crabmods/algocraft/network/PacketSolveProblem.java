package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.Config;
import com.crabmods.algocraft.logic.AchievementManager;
import com.crabmods.algocraft.logic.RewardSystem;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public record PacketSolveProblem(String problemId, String difficulty, long solveTimeMs) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSolveProblem> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "solve_problem"));
    
    public static final StreamCodec<ByteBuf, PacketSolveProblem> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketSolveProblem::problemId,
            ByteBufCodecs.STRING_UTF8, PacketSolveProblem::difficulty,
            ByteBufCodecs.VAR_LONG, PacketSolveProblem::solveTimeMs,
            PacketSolveProblem::new
    );
    
    /**
     * Constructor for backward compatibility (no solve time tracking).
     */
    public PacketSolveProblem(String problemId, String difficulty) {
        this(problemId, difficulty, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSolveProblem payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Check if rewards are enabled
                if (!Config.ENABLE_REWARDS.get()) {
                    player.sendSystemMessage(Component.translatable("algocraft.msg.rewards_disabled"));
                    return;
                }
                
                ServerLevel level = player.serverLevel();
                AlgoCraftSavedData data = AlgoCraftSavedData.get(level);
                
                long now = System.currentTimeMillis();
                boolean isFirstTime = !data.isProblemSolved(player.getUUID(), payload.problemId());
                long lastSolved = data.getLastSolvedTime(player.getUUID(), payload.problemId());
                
                boolean giveReward = false;
                
                if (isFirstTime) {
                    giveReward = true;
                    // Increment total solved count
                    data.incrementTotalSolved(player.getUUID());
                } else {
                    // Check if it's a new day (for daily rewards)
                    LocalDate lastDate = Instant.ofEpochMilli(lastSolved).atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
                    
                    if (today.isAfter(lastDate)) {
                        giveReward = true;
                    }
                }
                
                if (giveReward) {
                    // Update streak
                    int streak = data.updateStreak(player.getUUID(), now);
                    
                    // Increment difficulty count FIRST (before getting counts)
                    if (isFirstTime) {
                        data.incrementDifficultyCount(player.getUUID(), payload.difficulty().toLowerCase());
                    }
                    
                    // Get updated counts AFTER incrementing
                    int totalSolved = data.getTotalSolved(player.getUUID());
                    int easyCount = data.getDifficultyCount(player.getUUID(), "easy");
                    int mediumCount = data.getDifficultyCount(player.getUUID(), "medium");
                    int hardCount = data.getDifficultyCount(player.getUUID(), "hard");
                    
                    // Increment consecutive correct submissions
                    int consecutiveCorrect = data.incrementConsecutiveCorrect(player.getUUID());
                    
                    // Give rewards using the new system
                    RewardSystem.RewardResult rewards = RewardSystem.giveRewards(
                        player,
                        payload.difficulty(),
                        isFirstTime,
                        streak,
                        totalSolved
                    );
                    
                    // Update progress
                    data.setProblemSolved(player.getUUID(), payload.problemId(), now);
                    
                    // Send messages to player
                    if (isFirstTime) {
                        player.sendSystemMessage(Component.translatable("algocraft.msg.first_clear"));
                    } else {
                        player.sendSystemMessage(Component.translatable("algocraft.msg.daily_clear"));
                    }
                    
                    // Show streak info
                    if (streak >= 3) {
                        player.sendSystemMessage(Component.translatable("algocraft.msg.streak", streak));
                    }
                    
                    // Show milestone
                    if (rewards.hasMilestone()) {
                        player.sendSystemMessage(Component.translatable("algocraft.msg.milestone", rewards.getMilestone()));
                    }
                    
                    // Show random bonus
                    if (rewards.hasRandomBonus()) {
                        player.sendSystemMessage(Component.translatable("algocraft.msg.lucky_drop"));
                    }
                    
                    // Check and award achievements with correct counts
                    AchievementManager.getInstance().checkAndAwardAchievements(
                        player, totalSolved, streak, easyCount, mediumCount, hardCount
                    );
                    
                    // Check for special achievements
                    LocalTime time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime();
                    boolean isNightTime = time.isAfter(LocalTime.MIDNIGHT) && time.isBefore(LocalTime.of(6, 0));
                    
                    // Use solve time from client for Speed Demon achievement
                    AchievementManager.getInstance().checkSpecialAchievements(
                        player, payload.solveTimeMs(), consecutiveCorrect, isNightTime
                    );
                    
                    // Sync progress to client
                    PacketDistributor.sendToPlayer(player, new PacketSyncProgress(data.getPlayerProgress(player.getUUID())));
                } else {
                    player.sendSystemMessage(Component.translatable("algocraft.msg.already_cleared"));
                }
            }
        });
    }
}
