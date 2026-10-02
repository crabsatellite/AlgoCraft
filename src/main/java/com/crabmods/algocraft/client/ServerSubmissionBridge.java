package com.crabmods.algocraft.client;

import com.crabmods.algocraft.logic.*;
import com.crabmods.algocraft.logic.catalog.ClientCatalog;
import com.crabmods.algocraft.network.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import java.util.concurrent.*;

/** Request/result correlation belongs to the current Minecraft player connection. */
public final class ServerSubmissionBridge {
    private static final Map<String, CompletableFuture<SubmissionResult>> pending = new HashMap<>();
    public static CompletableFuture<SubmissionResult> submit(Problem problem, String code) {
        if (!problem.isPublished()) return CompletableFuture.supplyAsync(() -> Judge.grade(problem, code)).thenApply(result -> {
            if (result.isSuccess()) ProgressManager.markAsPassed(problem.getId());
            return result;
        });
        CompletableFuture<SubmissionResult> result = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().getConnection() == null || !ClientCatalog.isCurrent(problem)) {
                result.complete(rejected("题库已更新或连接已断开，请重新选择题目 / Bank updated or disconnected")); return;
            }
            if (!PacketSubmitSolution.canEncodeSubmission(problem.getId(), code)) {
                result.complete(rejected("提交代码过大 / Submission exceeds the network limit")); return;
            }
            if (!pending.isEmpty()) { result.complete(rejected("已有提交正在判题 / Submission already running")); return; }
            String id = UUID.randomUUID().toString();
            pending.put(id, result);
            try {
                PacketDistributor.sendToServer(new PacketSubmitSolution(problem.getId(), code, 0,
                        problem.getPublicationRepository(), problem.getPublicationVersion(), id));
                result.orTimeout(120, TimeUnit.SECONDS).whenComplete((value, error) ->
                        Minecraft.getInstance().execute(() -> pending.remove(id)));
            } catch (RuntimeException e) { pending.remove(id); result.completeExceptionally(e); }
        });
        return result;
    }
    public static CompletableFuture<SubmissionResult> sendForTest(PacketSubmitSolution packet) {
        if (!Boolean.getBoolean("algocraft.bankClientTest")) throw new IllegalStateException("Test-only submission");
        CompletableFuture<SubmissionResult> result = new CompletableFuture<>();
        pending.put(packet.requestId(), result);
        PacketDistributor.sendToServer(packet);
        result.orTimeout(120, TimeUnit.SECONDS).whenComplete((value, error) -> Minecraft.getInstance().execute(() -> pending.remove(packet.requestId())));
        return result;
    }
    public static void complete(PacketSubmissionResult packet) {
        CompletableFuture<SubmissionResult> completion = pending.remove(packet.requestId());
        if (completion == null) return;
        SubmissionResult result = new SubmissionResult();
        result.setSuccess(packet.accepted());
        com.crabmods.algocraft.logic.catalog.PublicJudgeFeedback.decode(result, packet.message());
        result.setPassedCount(packet.passed()); result.setTotalCount(packet.total()); result.setExecutionTimeMs(packet.time());
        completion.complete(result);
    }
    public static void disconnect() {
        var previous = new ArrayList<>(pending.values()); pending.clear();
        previous.forEach(f -> f.complete(rejected("连接已断开 / Disconnected")));
    }
    public static SubmissionResult rejected(String message) {
        SubmissionResult result = new SubmissionResult(); result.setMessage(message); result.setSuccess(false); return result;
    }
}
