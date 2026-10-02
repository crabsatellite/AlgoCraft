package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.logic.CodeExecutor;
import com.crabmods.algocraft.logic.Judge;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.SubmissionResult;
import com.crabmods.algocraft.server.SolvingPlayerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public record PacketSubmitSolution(String problemId, String code, long solveTimeMs, String repository, String version, String requestId) implements CustomPacketPayload {
    public PacketSubmitSolution(String problemId, String code, long solveTimeMs) { this(problemId, code, solveTimeMs, "", "", ""); }
    private static final StreamCodec<ByteBuf, String> CODE_CODEC = ByteBufCodecs.byteArray(
            com.crabmods.algocraft.logic.catalog.SubmissionWire.MAX_COMPRESSED_BYTES).map(bytes -> {
        try { return com.crabmods.algocraft.logic.catalog.SubmissionWire.decode(bytes); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("Invalid compressed submission", e); }
    }, code -> {
        try { return com.crabmods.algocraft.logic.catalog.SubmissionWire.encode(code); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("Submission too large", e); }
    });
    public static final CustomPacketPayload.Type<PacketSubmitSolution> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "submit_solution"));
    private static final int MAX_PROBLEM_ID_LENGTH = 256;
    public static final int MAX_CODE_LENGTH = CodeExecutor.MAX_CODE_LENGTH;
    private static final int MAX_IN_FLIGHT_PER_PLAYER = 1;
    private static final int MAX_IN_FLIGHT_GLOBAL = 4;
    private static final PlayerSubmissionLimiter SUBMISSION_LIMITER =
            new PlayerSubmissionLimiter(MAX_IN_FLIGHT_PER_PLAYER);
    private static final GlobalSubmissionLimiter GLOBAL_SUBMISSION_LIMITER =
            new GlobalSubmissionLimiter(MAX_IN_FLIGHT_GLOBAL);
    private static final Object JUDGE_EXECUTOR_LOCK = new Object();
    private static final AtomicInteger JUDGE_THREAD_IDS = new AtomicInteger();
    private static ExecutorService judgeExecutor;

    public static final StreamCodec<ByteBuf, PacketSubmitSolution> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_PROBLEM_ID_LENGTH), PacketSubmitSolution::problemId,
            CODE_CODEC, PacketSubmitSolution::code,
            ByteBufCodecs.VAR_LONG, PacketSubmitSolution::solveTimeMs,
            ByteBufCodecs.stringUtf8(128), PacketSubmitSolution::repository,
            ByteBufCodecs.stringUtf8(64), PacketSubmitSolution::version,
            ByteBufCodecs.stringUtf8(36), PacketSubmitSolution::requestId,
            PacketSubmitSolution::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSubmitSolution payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CompletableFuture<SubmissionResult> judged;
                try { java.util.UUID.fromString(payload.requestId()); }
                catch (RuntimeException e) { return; }
                if (!com.crabmods.algocraft.server.ServerBankService.matches(payload.repository(), payload.problemId(), payload.version())) {
                    judged = CompletableFuture.completedFuture(rejectedResult("题库版本已更新或未发布，请重新选择 / Bank updated or unpublished"));
                    com.crabmods.algocraft.server.ServerBankService.announce(player);
                } else {
                    var entry = com.crabmods.algocraft.server.ServerBankService.current().get(payload.problemId());
                    judged = submitAsyncVerifiedProblem(player, entry.problem(), payload.code(), payload.solveTimeMs());
                }
                judged.whenComplete((result, error) -> player.getServer().execute(() -> {
                    if (player.hasDisconnected()) return;
                    SubmissionResult response = error == null ? result : rejectedResult("Server judge failed");
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, PacketSubmissionResult.of(payload.requestId(), response));
                }));
            }
        });
    }

    public static CompletableFuture<SubmissionResult> submitAsync(ServerPlayer player, String problemId,
                                                                   String code, long solveTimeMs) {
        if (!isProblemIdAcceptable(problemId)) {
            return rejectWithoutJudging(player, "", "Problem id is empty or too large");
        }
        if (!isCodeAcceptable(code)) {
            return rejectWithoutJudging(player, problemId, "Code is empty or too large");
        }

        Problem problem = ProblemManager.getProblem(problemId);
        if (problem == null) {
            return CompletableFuture.completedFuture(rejectMissingProblem(player, problemId));
        }

        return submitAsyncVerifiedProblem(player, problem, code, solveTimeMs);
    }

    public static boolean canEncodeSubmission(String problemId, String code) {
        if (!isProblemIdAcceptable(problemId) || !isCodeAcceptable(code)) return false;
        try { com.crabmods.algocraft.logic.catalog.SubmissionWire.encode(code); return true; }
        catch (java.io.IOException e) { return false; }
    }

    public static boolean hasPlayerSubmissionInFlightForGameTest(ServerPlayer player) {
        return player != null && SUBMISSION_LIMITER.inFlightCount(player.getUUID()) > 0;
    }

    public static CompletableFuture<SubmissionResult> submitAsyncVerifiedProblem(ServerPlayer player, Problem problem,
                                                                                 String code, long solveTimeMs) {
        String submittedCode = code;
        return submitAsyncVerifiedProblem(player, problem, code, solveTimeMs,
                () -> Judge.grade(problem, submittedCode));
    }

    public static CompletableFuture<SubmissionResult> submitAsyncVerifiedProblemWithJudgeForGameTest(
            ServerPlayer player,
            Problem problem,
            String code,
            long solveTimeMs,
            Supplier<SubmissionResult> judgeTask) {
        return submitAsyncVerifiedProblem(player, problem, code, solveTimeMs, judgeTask);
    }

    private static CompletableFuture<SubmissionResult> submitAsyncVerifiedProblem(ServerPlayer player, Problem problem,
                                                                                  String code, long solveTimeMs,
                                                                                  Supplier<SubmissionResult> judgeTask) {
        if (problem == null) {
            return CompletableFuture.completedFuture(rejectMissingProblem(player, ""));
        }
        if (!isProblemIdAcceptable(problem.getId())) {
            return rejectWithoutJudging(player, "", "Problem id is empty or too large");
        }
        if (!isCodeAcceptable(code)) {
            return rejectWithoutJudging(player, problem.getId(), "Code is empty or too large");
        }
        UUID playerId = player.getUUID();
        if (!SUBMISSION_LIMITER.tryAcquire(playerId)) {
            player.sendSystemMessage(Component.translatable("algocraft.msg.server_judge_busy"));
            return CompletableFuture.completedFuture(rejectedResult("Submission already running"));
        }
        if (!GLOBAL_SUBMISSION_LIMITER.tryAcquire()) {
            SUBMISSION_LIMITER.release(playerId);
            player.sendSystemMessage(Component.translatable("algocraft.msg.server_judge_global_busy"));
            return CompletableFuture.completedFuture(rejectedResult("Server judge capacity is full"));
        }

        CompletableFuture<SubmissionResult> completion = new CompletableFuture<>();
        try {
            CompletableFuture
                    .supplyAsync(judgeTask, judgeExecutor())
                    .whenComplete((result, error) -> {
                        boolean scheduled = runOnServerThread(player, () -> {
                            SubmissionResult completionResult = null;
                            Throwable completionError = null;
                            try {
                                if (error != null) {
                                    AlgoCraft.LOGGER.warn("Server judge failed for problem {}", problem.getId(), error);
                                    completionResult = rejectWithoutJudging(
                                            player,
                                            problem.getId(),
                                            error.getClass().getSimpleName()
                                    ).join();
                                } else {
                                    SubmissionResult judgedResult = result != null
                                            ? result
                                            : rejectedResult("Unknown Error");
                                    completionResult = applyJudgedResult(
                                            player,
                                            problem,
                                            judgedResult,
                                            System.currentTimeMillis()
                                    );
                                }
                            } catch (Throwable throwable) {
                                completionError = throwable;
                            } finally {
                                GLOBAL_SUBMISSION_LIMITER.release();
                                SUBMISSION_LIMITER.release(playerId);
                            }
                            if (completionError != null) {
                                completion.completeExceptionally(completionError);
                            } else {
                                completion.complete(completionResult);
                            }
                        });
                        if (!scheduled) {
                            GLOBAL_SUBMISSION_LIMITER.release();
                            SUBMISSION_LIMITER.release(playerId);
                            completion.complete(rejectedResult("Server unavailable"));
                        }
                    });
        } catch (RejectedExecutionException e) {
            GLOBAL_SUBMISSION_LIMITER.release();
            SUBMISSION_LIMITER.release(playerId);
            player.sendSystemMessage(Component.translatable("algocraft.msg.server_judge_rejected", "Server judge unavailable"));
            completion.complete(rejectedResult("Server judge unavailable"));
        }
        return completion;
    }

    public static void shutdownJudgeExecutor() {
        ExecutorService executorToStop;
        synchronized (JUDGE_EXECUTOR_LOCK) {
            executorToStop = judgeExecutor;
            judgeExecutor = null;
        }
        if (executorToStop == null) {
            return;
        }
        executorToStop.shutdown();
        try {
            if (!executorToStop.awaitTermination(5, TimeUnit.SECONDS)) {
                executorToStop.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorToStop.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static SubmissionResult submitVerifiedProblem(ServerPlayer player, Problem problem, String code,
                                                          long solveTimeMs, long now) {
        if (problem == null) {
            return rejectMissingProblem(player, "");
        }
        if (!isProblemIdAcceptable(problem.getId())) {
            rejectWithoutJudging(player, "", "Problem id is empty or too large");
            return rejectedResult("Problem id is empty or too large");
        }
        if (!isCodeAcceptable(code)) {
            rejectWithoutJudging(player, problem.getId(), "Code is empty or too large");
            return rejectedResult("Code is empty or too large");
        }

        SubmissionResult result = Judge.grade(problem, code);
        if (result == null) {
            result = rejectedResult("Unknown Error");
        }
        return applyJudgedResult(player, problem, result, now);
    }

    private static SubmissionResult applyJudgedResult(ServerPlayer player, Problem problem, SubmissionResult result,
                                                      long now) {
        if (problem.isPublished() && (player.hasDisconnected() || !com.crabmods.algocraft.server.ServerBankService.matches(
                problem.getPublicationRepository(), problem.getId(), problem.getPublicationVersion()))) {
            return rejectedResult("题库版本已更新或连接已断开 / Bank updated or disconnected");
        }
        if (isAcceptedJudgeResult(result)) {
            long serverSolveTimeMs = SolvingPlayerManager.getSolvingElapsedMs(player, problem.getId(), now).orElse(0L);
            SolvedProblemRewardService.applySolvedProblem(player, problem.getId(), problem.getDifficulty(), serverSolveTimeMs, now);
            return result;
        }

        SubmissionResult rejectedResult = rejectedJudgeResult(result);
        if (result != null && result.isSuccess()) {
            AlgoCraft.LOGGER.warn(
                    "Rejected malformed successful judge result for problem {}: passed={}, total={}",
                    problem.getId(),
                    result.getPassedCount(),
                    result.getTotalCount()
            );
        }

        PacketSubmissionFailed.applySubmissionFailure(player, problem.getId());
        player.sendSystemMessage(Component.translatable("algocraft.msg.server_judge_rejected", rejectedResult.getMessage()));
        return rejectedResult;
    }

    private static boolean isAcceptedJudgeResult(SubmissionResult result) {
        return result != null
                && result.isSuccess()
                && result.getTotalCount() > 0
                && result.getPassedCount() == result.getTotalCount();
    }

    private static SubmissionResult rejectedJudgeResult(SubmissionResult result) {
        if (result == null) {
            return rejectedResult("Unknown Error");
        }
        if (!result.isSuccess()) {
            return result;
        }
        return rejectedResult("Malformed judge result");
    }

    private static CompletableFuture<SubmissionResult> rejectWithoutJudging(ServerPlayer player, String problemId,
                                                                            String reason) {
        PacketSubmissionFailed.applySubmissionFailure(player, problemId);
        player.sendSystemMessage(Component.translatable("algocraft.msg.server_judge_rejected", reason));
        return CompletableFuture.completedFuture(rejectedResult(reason));
    }

    private static SubmissionResult rejectMissingProblem(ServerPlayer player, String problemId) {
        String safeProblemId = problemId != null ? problemId : "";
        PacketSubmissionFailed.applySubmissionFailure(player, safeProblemId);
        player.sendSystemMessage(Component.translatable("algocraft.msg.server_judge_missing_problem", safeProblemId));
        return rejectedResult("Problem not found on server");
    }

    private static SubmissionResult rejectedResult(String message) {
        SubmissionResult result = new SubmissionResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setTotalCount(0);
        return result;
    }

    private static boolean isProblemIdAcceptable(String problemId) {
        return problemId != null && !problemId.isBlank() && problemId.length() <= MAX_PROBLEM_ID_LENGTH;
    }

    private static boolean isCodeAcceptable(String code) {
        return code != null && !code.isBlank() && code.length() <= MAX_CODE_LENGTH;
    }

    private static ExecutorService judgeExecutor() {
        synchronized (JUDGE_EXECUTOR_LOCK) {
            if (judgeExecutor == null || judgeExecutor.isShutdown() || judgeExecutor.isTerminated()) {
                judgeExecutor = Executors.newFixedThreadPool(MAX_IN_FLIGHT_GLOBAL, runnable -> {
                    Thread thread = new Thread(runnable,
                            "AlgoCraft-ServerJudge-" + JUDGE_THREAD_IDS.incrementAndGet());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    return thread;
                });
            }
            return judgeExecutor;
        }
    }

    private static boolean runOnServerThread(ServerPlayer player, Runnable action) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(action);
            return true;
        }
        return false;
    }
}
