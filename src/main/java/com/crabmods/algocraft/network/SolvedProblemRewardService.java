package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.Config;
import com.crabmods.algocraft.logic.AchievementManager;
import com.crabmods.algocraft.logic.RewardSystem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Internal server-side reward application after a submission has already been judged accepted.
 */
public final class SolvedProblemRewardService {
    private SolvedProblemRewardService() {
    }

    public static void applySolvedProblem(ServerPlayer player, String problemId, String difficulty,
                                          long serverSolveTimeMs, long now) {
        ServerLevel level = player.serverLevel();
        AlgoCraftSavedData data = AlgoCraftSavedData.get(level);

        boolean isFirstTime = !data.isProblemSolved(player.getUUID(), problemId);
        long lastSolved = data.getLastSolvedTime(player.getUUID(), problemId);

        boolean giveReward = false;

        if (isFirstTime) {
            giveReward = true;
            data.incrementTotalSolved(player.getUUID());
        } else {
            LocalDate lastDate = Instant.ofEpochMilli(lastSolved).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();

            if (today.isAfter(lastDate)) {
                giveReward = true;
            }
        }

        if (giveReward) {
            long lastSolveDay = data.getPlayerStats(player.getUUID()).lastSolveDay;
            LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
            boolean firstSolveToday = lastSolveDay == 0 || today.isAfter(
                    Instant.ofEpochMilli(lastSolveDay).atZone(ZoneId.systemDefault()).toLocalDate());
            int streak = data.updateStreak(player.getUUID(), now);

            if (isFirstTime) {
                data.incrementDifficultyCount(player.getUUID(), difficulty.toLowerCase());
            }

            int totalSolved = data.getTotalSolved(player.getUUID());
            int easyCount = data.getDifficultyCount(player.getUUID(), "easy");
            int mediumCount = data.getDifficultyCount(player.getUUID(), "medium");
            int hardCount = data.getDifficultyCount(player.getUUID(), "hard");

            int consecutiveCorrect = data.incrementConsecutiveCorrect(player.getUUID());

            data.setProblemSolved(player.getUUID(), problemId, now);
            if (!Config.ENABLE_REWARDS.get()) {
                player.sendSystemMessage(Component.translatable("algocraft.msg.rewards_disabled"));
                PacketSyncProgress.sendSafely(player, data.getPlayerProgress(player.getUUID()));
                return;
            }

            RewardSystem.RewardResult rewards = RewardSystem.giveRewards(
                player,
                difficulty,
                isFirstTime,
                firstSolveToday,
                streak,
                totalSolved
            );

            Component itemSummary = Component.empty();
            for (var stack : rewards.getItems()) {
                if (!itemSummary.getString().isEmpty()) {
                    itemSummary = itemSummary.copy().append(Component.translatable("algocraft.format.list_separator"));
                }
                itemSummary = itemSummary.copy().append(Component.translatable("algocraft.msg.reward_item",
                        stack.getCount(), stack.getHoverName()));
            }
            player.sendSystemMessage(Component.translatable("algocraft.msg.reward_summary",
                    itemSummary, rewards.getTotalXp()));

            if (isFirstTime) {
                player.sendSystemMessage(Component.translatable("algocraft.msg.first_clear"));
            } else {
                player.sendSystemMessage(Component.translatable("algocraft.msg.daily_clear"));
            }

            if (streak >= 3) {
                player.sendSystemMessage(Component.translatable("algocraft.msg.streak", streak));
            }

            if (rewards.hasMilestone()) {
                player.sendSystemMessage(Component.translatable("algocraft.msg.milestone", rewards.getMilestone()));
            }

            if (rewards.hasRandomBonus()) {
                player.sendSystemMessage(Component.translatable("algocraft.msg.lucky_drop"));
            }

            AchievementManager achievements = data.getAchievementManager();
            achievements.checkAndAwardAchievements(
                player, totalSolved, streak, easyCount, mediumCount, hardCount
            );

            LocalTime time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime();
            boolean isNightTime = time.isAfter(LocalTime.MIDNIGHT) && time.isBefore(LocalTime.of(6, 0));

            achievements.checkSpecialAchievements(
                player, serverSolveTimeMs, consecutiveCorrect, isNightTime
            );

            Set<String> solvedIds = data.getPlayerProgress(player.getUUID()).keySet();
            var publication = com.crabmods.algocraft.server.ServerBankService.current();
            for (ProblemRepository repository : publication == null ? ProblemManager.getRepositories() : publication.judgeRepositories()) {
                Set<String> repositoryIds = repository.getProblems().stream()
                        .map(problem -> problem.getId()).collect(Collectors.toSet());
                if (repositoryIds.contains(problemId)) {
                    int solvedInRepository = (int) repositoryIds.stream().filter(solvedIds::contains).count();
                    achievements.checkCompletionistAchievement(player, repository.getName(),
                            repositoryIds.size(), solvedInRepository);
                }
            }

            PacketSyncProgress.sendSafely(player, data.getPlayerProgress(player.getUUID()));
        } else {
            player.sendSystemMessage(Component.translatable("algocraft.msg.already_cleared"));
        }
    }
}
