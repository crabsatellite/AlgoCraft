package com.crabmods.algocraft.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkPayloadContractTest {
    private static final Path NETWORK_DIR = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "network"
    );
    private static final Path MODERN_SCREEN = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "client", "gui", "modern", "ModernAlgorithmScreen.java"
    );
    private static final Path CLIENT_HOOKS = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "client", "ClientHooks.java"
    );
    private static final Path CODE_EXECUTOR = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "logic", "CodeExecutor.java"
    );
    private static final Path MOD_ENTRYPOINT = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "AlgoCraft.java"
    );
    private static final Path SOLVING_MANAGER = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "crabmods", "algocraft", "server", "SolvingPlayerManager.java"
    );

    @Test
    void clientCannotRegisterDirectProgressMutationPackets() throws IOException {
        String networkHandler = Files.readString(NETWORK_DIR.resolve("NetworkHandler.java"), StandardCharsets.UTF_8);
        String rewardService = Files.readString(NETWORK_DIR.resolve("SolvedProblemRewardService.java"), StandardCharsets.UTF_8);
        String failureService = Files.readString(NETWORK_DIR.resolve("PacketSubmissionFailed.java"), StandardCharsets.UTF_8);
        String modernScreen = Files.readString(MODERN_SCREEN, StandardCharsets.UTF_8);
        String modEntrypoint = Files.readString(MOD_ENTRYPOINT, StandardCharsets.UTF_8);

        assertFalse(Files.exists(NETWORK_DIR.resolve("PacketSolveProblem.java")),
                "legacy direct-solve packet code must be removed instead of left around unregistered");
        assertTrue(modEntrypoint.contains("modEventBus.addListener(NetworkHandler::register)"),
                "the mod entrypoint must register payload handlers on the mod event bus");
        assertTrue(networkHandler.contains("PacketSubmitSolution.TYPE"),
                "client submissions must go through the server-judged submit_solution payload");
        assertFalse(networkHandler.contains("solve_problem"),
                "direct solve/reward packet ids must not be registered as client-sendable payloads");
        assertFalse(networkHandler.contains("PacketSubmissionFailed.TYPE"),
                "direct failure/reset packets must not be registered as client-sendable payloads");
        assertFalse(rewardService.contains("implements CustomPacketPayload"),
                "reward application should stay an internal server service, not a network payload");
        assertFalse(rewardService.contains("CustomPacketPayload"),
                "reward application service must not carry network payload imports or declarations");
        assertFalse(rewardService.contains("client"),
                "reward application comments and parameters must not imply client-trusted solve timing");
        assertFalse(failureService.contains("implements CustomPacketPayload"),
                "failure application should stay an internal server service, not a network payload");
        assertFalse(modernScreen.contains("new com.crabmods.algocraft.network.PacketSubmissionFailed"),
                "the IDE must submit code for server judging instead of directly resetting server-side progress");
    }

    @Test
    void submitSolutionPayloadHasExplicitStringAndConcurrencyLimits() throws IOException {
        String submitPayload = Files.readString(NETWORK_DIR.resolve("PacketSubmitSolution.java"), StandardCharsets.UTF_8);
        String codeExecutor = Files.readString(CODE_EXECUTOR, StandardCharsets.UTF_8);
        String modEntrypoint = Files.readString(MOD_ENTRYPOINT, StandardCharsets.UTF_8);

        assertTrue(submitPayload.contains("MAX_PROBLEM_ID_LENGTH = 256"),
                "problem ids sent by clients should have a tight payload limit");
        assertTrue(codeExecutor.contains("public static final int MAX_CODE_LENGTH = 50_000"),
                "the execution engine should expose the authoritative code acceptance limit");
        assertTrue(submitPayload.contains("import com.crabmods.algocraft.logic.CodeExecutor;"),
                "submit_solution should import the execution engine limit instead of drifting separately");
        assertTrue(submitPayload.contains("MAX_CODE_LENGTH = CodeExecutor.MAX_CODE_LENGTH"),
                "code payload limit should reuse the execution-engine code acceptance limit");
        assertTrue(submitPayload.contains("ByteBufCodecs.stringUtf8(MAX_PROBLEM_ID_LENGTH)"),
                "problem id codec should use the explicit problem-id limit");
        assertTrue(submitPayload.contains("CODE_CODEC, PacketSubmitSolution::code"),
                "code codec should use the explicit code limit instead of the default 32767 string limit");
        assertTrue(submitPayload.contains("new PlayerSubmissionLimiter(MAX_IN_FLIGHT_PER_PLAYER)"),
                "async server judging should be protected by a per-player in-flight limiter");
        assertTrue(submitPayload.contains("MAX_IN_FLIGHT_GLOBAL = 4"),
                "async server judging should have a global capacity limit");
        assertTrue(submitPayload.contains("new GlobalSubmissionLimiter(MAX_IN_FLIGHT_GLOBAL)"),
                "async server judging should be protected by a global in-flight limiter");
        assertTrue(submitPayload.contains("newFixedThreadPool(MAX_IN_FLIGHT_GLOBAL"),
                "async server judging should run on a bounded AlgoCraft-owned executor");
        assertTrue(submitPayload.contains("\"AlgoCraft-ServerJudge-\""),
                "server judge worker threads should be named for diagnostics");
        assertTrue(submitPayload.contains("() -> Judge.grade(problem, submittedCode)"),
                "the production async submit entrypoint should still judge real user code");
        assertTrue(submitPayload.contains(".supplyAsync(judgeTask, judgeExecutor())"),
                "server judge submissions must not use the default common pool");
        assertTrue(submitPayload.contains("submitAsyncVerifiedProblemWithJudgeForGameTest"),
                "GameTests should be able to saturate judge capacity without depending on worker wall-clock timing");
        assertTrue(submitPayload.contains("shutdownJudgeExecutor()"),
                "server judge submissions should expose a shutdown hook for server stop cleanup");
        assertTrue(modEntrypoint.contains("PacketSubmitSolution.shutdownJudgeExecutor();"),
                "server stop cleanup should shut down the async judge executor before CodeExecutor shutdown");
        assertTrue(submitPayload.contains("algocraft.msg.server_judge_global_busy"),
                "players should get a translated status when global judge capacity is full");
        assertTrue(submitPayload.contains("canEncodeSubmission"),
                "client UI should be able to check the same payload bounds before sending a submit packet");
        assertTrue(submitPayload.contains("!isProblemIdAcceptable(problem.getId())"),
                "server-internal verified submit paths must still reject blank or oversized problem ids before judging");
        assertTrue(submitPayload.contains("SolvingPlayerManager.getSolvingElapsedMs(player, problem.getId(), now)"),
                "speed-sensitive achievements should use same-problem server-measured solving time");
        assertTrue(submitPayload.contains("isAcceptedJudgeResult(result)"),
                "server rewards should be gated by a single accepted-result consistency check");
        assertTrue(submitPayload.contains("result.getTotalCount() > 0"),
                "server rewards must not accept successful submissions with no judged tests");
        assertTrue(submitPayload.contains("result.getPassedCount() == result.getTotalCount()"),
                "server rewards must require successful submissions to pass every judged test");
        assertTrue(submitPayload.contains("Malformed judge result"),
                "malformed successful judge results should be downgraded before rewards or progress");
        assertFalse(submitPayload.contains("problem.getDifficulty(), solveTimeMs"),
                "client-supplied solveTimeMs must not flow directly into reward or achievement application");
        assertFalse(submitPayload.contains("ByteBufCodecs.STRING_UTF8"),
                "submit_solution must not fall back to the default STRING_UTF8 codec limits");
        assertFalse(submitPayload.contains(".supplyAsync(() -> Judge.grade(problem, submittedCode))"),
                "server judge submissions must not fall back to the JVM common pool");
    }

    @Test
    void ideAndWebSubmittersRejectPayloadsBeforeSilentNetworkDrops() throws IOException {
        String modernScreen = Files.readString(MODERN_SCREEN, StandardCharsets.UTF_8);
        String clientHooks = Files.readString(CLIENT_HOOKS.getParent().resolve("ServerSubmissionBridge.java"), StandardCharsets.UTF_8);

        assertTrue(modernScreen.contains("PacketSubmitSolution.canEncodeSubmission(submittedProblem.getId(), submittedCode)"),
                "the in-game IDE should check payload bounds before local Judge.grade and network send");
        assertTrue(modernScreen.contains("CompletableFuture.supplyAsync(() -> Judge.grade(submittedProblem, submittedCode), submissionExecutor())"),
                "the in-game IDE should run local judging off the render thread");
        assertTrue(modernScreen.contains("\"AlgoCraft-ClientJudge-\""),
                "client judge worker threads should be named for diagnostics");
        assertTrue(modernScreen.contains("setSubmissionInProgress(true)"),
                "the in-game IDE should expose a pending state while local judging is running");
        assertTrue(modernScreen.contains("setSubmissionInProgress(false)"),
                "the in-game IDE should clear its pending state after local judging completes");
        assertTrue(modernScreen.contains("boolean active = !runInProgress && !submissionInProgress")
                        && modernScreen.contains("submitButton.active = active")
                        && modernScreen.contains("runButton.active = active"),
                "Run and Submit should be disabled while either execution is pending");
        assertTrue(modernScreen.contains("algocraft.msg.submit_code_too_large"),
                "the in-game IDE should show a translated rejection instead of silently dropping oversized submits");
        assertTrue(modernScreen.contains("PacketSubmitSolution.MAX_CODE_LENGTH"),
                "the in-game IDE rejection should display the same code limit used by networking and execution");
        assertTrue(clientHooks.contains("PacketSubmitSolution.canEncodeSubmission(problem.getId(), code)"),
                "the web bridge should check payload bounds before sending the same submit_solution packet");
        assertTrue(clientHooks.contains("Submission exceeds the network limit"),
                "the web bridge should notify players when a solved payload cannot be encoded");
        assertTrue(clientHooks.contains("PacketSubmitSolution.canEncodeSubmission"),
                "the web bridge rejection should display the same code limit used by networking and execution");
    }

    @Test
    void ideScreenOpensAndClosesServerSolvingState() throws IOException {
        String modernScreen = Files.readString(MODERN_SCREEN, StandardCharsets.UTF_8);
        String solvingPayload = Files.readString(NETWORK_DIR.resolve("PacketSetSolvingState.java"), StandardCharsets.UTF_8);
        String solvingManager = Files.readString(SOLVING_MANAGER, StandardCharsets.UTF_8);

        assertTrue(modernScreen.contains("sendSolvingState(true);"),
                "selecting a problem in the IDE should tell the server that the player is solving");
        assertTrue(modernScreen.contains("sendSolvingState(false);"),
                "closing the IDE should clear the server-side solving state");
        assertTrue(modernScreen.contains("new PacketSetSolvingState(solving, problemId)"),
                "the solving-state helper should bind the server payload to the current problem");
        assertTrue(modernScreen.contains("minecraft.getConnection() != null"),
                "solving-state sends should be guarded for non-world UI contexts");
        assertTrue(solvingPayload.contains("MAX_PROBLEM_ID_LENGTH = 256"),
                "solving-state problem ids should have a tight payload limit");
        assertTrue(solvingPayload.contains("ByteBufCodecs.stringUtf8(MAX_PROBLEM_ID_LENGTH)"),
                "solving-state should use an explicit problem-id codec limit");
        assertTrue(solvingPayload.contains("ServerBankService.current().get(payload.problemId()) != null"),
                "server should only accept solving-state starts for known problems");
        assertTrue(solvingManager.contains("MAX_SOLVING_SESSION_MS"),
                "server-side solving protection should expire instead of lasting indefinitely");
        assertTrue(solvingManager.contains("session.problemId().equals(problemId)"),
                "server-side solve timers should be bound to the submitted problem");
        assertTrue(solvingManager.contains("existingSession.problemId().equals(problemId)"),
                "duplicate same-problem solve starts should preserve the original server timer");
        assertTrue(solvingManager.contains("solvingSessions.put(playerId, new SolvingSession(problemId, now))"),
                "switching to a different problem should still replace the active solving session");
    }
}
