package com.crabmods.algocraft.client.test;

import com.crabmods.algocraft.client.ServerSubmissionBridge;
import com.crabmods.algocraft.logic.*;
import com.crabmods.algocraft.logic.catalog.*;
import com.crabmods.algocraft.network.PacketSubmitSolution;
import com.crabmods.algocraft.web.AlgoCraftWebServer;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

/** A real TCP client; never controls the desktop. Two clients share one dedicated fixture server. */
public final class BankMultiplayerClientTest {
    public static final String CODE = "class Solution { public boolean isValidSudoku(char[][] b) { int[] rows=new int[9],cols=new int[9],boxes=new int[9]; for(int r=0;r<9;r++) for(int c=0;c<9;c++){ char v=b[r][c]; if(v=='.')continue; int bit=1<<(v-'1'),box=(r/3)*3+c/3; if((rows[r]&bit)!=0||(cols[c]&bit)!=0||(boxes[box]&bit)!=0)return false; rows[r]|=bit;cols[c]|=bit;boxes[box]|=bit;}return true;} }";
    private final Path output = Path.of(System.getProperty("algocraft.bankOutput"));
    private final String role = System.getProperty("algocraft.bankRole");
    private final List<String> checks = new ArrayList<>();
    private int ticks, phase;
    private boolean finished;
    private boolean compilerChecked;
    private boolean practiceChecked;
    private Problem oldProblem;
    private String firstRevision;
    private CompletableFuture<SubmissionResult> submission;
    public static void maybeRegister() {
        if (Boolean.getBoolean("algocraft.bankClientTest")) {
            BankMultiplayerClientTest test = new BankMultiplayerClientTest();
            NeoForge.EVENT_BUS.addListener(test::tick);
        }
    }
    private void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        checks.add(message);
    }
    private void tick(ClientTickEvent.Post event) {
        if (finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            UnattendedClientTestMode.verify();
            ticks++;
            if (phase > 0 && mc.screen instanceof DisconnectedScreen) throw new IllegalStateException("Client disconnected during multiplayer scenario");
            if (ticks > 18000) throw new IllegalStateException("Multiplayer timeout at phase " + phase);
            if (phase == 0) {
                if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
                mc.keyboardHandler.setClipboard("isolated bank test");
                check(mc.keyboardHandler.getClipboard().equals("isolated bank test"), "Clipboard uses isolated adapter");
                String address = "127.0.0.1:" + Integer.getInteger("algocraft.bankPort", 25586);
                ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString(address),new ServerData("AlgoCraft fixture",address,ServerData.Type.OTHER),false,null);
                phase = 1;
            } else if (phase == 1) {
                if (mc.player == null || !ClientCatalog.ready()) return;
                oldProblem = ClientCatalog.getProblem("serverfixture:7");
                check(oldProblem != null, "Server-exclusive problem arrived over TCP");
                check(oldProblem.isPublished() && oldProblem.getTests().isEmpty() && oldProblem.getSolutions().isEmpty(), "Client received public data without hidden tests or answers");
                check(ClientCatalog.getProblem("1").getTitle("zh_cn").matches(".*[\\p{IsHan}].*"), "Server Chinese translation is available");
                var pictured = ClientCatalog.getProblems().stream().filter(p -> !p.getVisuals().isEmpty()).findFirst().orElseThrow();
                check(ProblemAssetResolver.resolveImage(pictured,pictured.getVisuals().getFirst()).isPresent(), "Server image installed in client cache");
                check(!ProgressManager.isPassed(oldProblem.getId()), "New player's progress is independent");
                firstRevision = ClientCatalog.revision();
                mc.getConnection().sendCommand("algocraft bank disable official");
                // A privately imported repository must neither replace nor publish a server problem.
                Problem privateProblem = new Gson().fromJson(new Gson().toJson(oldProblem),Problem.class);
                privateProblem.setId("private:7"); privateProblem.setTests(List.of());
                ProblemManager.addRepository(new PublishedCatalog.Bank("Private",50,List.of(privateProblem))).join();
                check(ClientCatalog.getProblem("practice:private:7") != null, "Personal import has a distinct practice identity");
                check(ClientCatalog.getProblem("serverfixture:7").isPublished(), "Personal import cannot replace the server catalog");
                submission = ServerSubmissionBridge.submit(oldProblem,"class Solution { public boolean isValidSudoku(char[][] b){return false;} }");
                phase = 2;
            } else if (phase == 2) {
                if (!submission.isDone()) return;
                if (!compilerChecked) {
                    check(!submission.join().isSuccess(), "Server rejects wrong answer using its hidden tests");
                    submission = ServerSubmissionBridge.submit(oldProblem, "class Solution { broken }");
                    compilerChecked = true;
                    return;
                }
                check(submission.join().getMessage().equals("Compilation Error") && !submission.join().getDetails().isEmpty(),
                        "Server compiler diagnostics arrive without hidden inputs");
                check(!ProgressManager.isPassed(oldProblem.getId()), "Wrong answer awards no progress");
                if (role.equals("B") && !Files.exists(output.resolve("A-first.json"))) return;
                check(!ProgressManager.isPassed(oldProblem.getId()), "Other player's accepted submission does not mark this player passed");
                submission = ServerSubmissionBridge.submit(oldProblem,CODE);
                phase = 3;
            } else if (phase == 3) {
                if (!submission.isDone()) return;
                if (!submission.join().isSuccess()) throw new IllegalStateException("Accepted fixture rejected: " + submission.join().getMessage());
                if (!ProgressManager.isPassed(oldProblem.getId())) return;
                check(submission.join().isSuccess() && submission.join().getTotalCount()>oldProblem.getExamples().size(), "Authoritative accepted result includes server hidden-test counts");
                check(ClientCatalog.getProblem("1") != null && ClientCatalog.revision().equals(firstRevision), "Non-operator bank command did not alter publication");
                // Verify Web POST uses this Minecraft player's server submission path.
                submission = CompletableFuture.supplyAsync(() -> {
                    try {
                        JsonObject body = new JsonObject(); body.addProperty("problemId",oldProblem.getId());body.addProperty("version",oldProblem.getPublicationVersion());body.addProperty("code",CODE);
                        var request = HttpRequest.newBuilder(URI.create(AlgoCraftWebServer.getLocalUrl()+"/api/submit"))
                                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                        var response = HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode()!=200) throw new IllegalStateException(response.body());
                        return new Gson().fromJson(response.body(),SubmissionResult.class);
                    } catch(Exception e) { throw new CompletionException(e); }
                });
                phase=4;
            } else if (phase==4) {
                if (!submission.isDone()) return;
                if (!practiceChecked) {
                    check(submission.join().isSuccess(), "Web submit returns the authoritative server result");
                    submission = CompletableFuture.supplyAsync(() -> {
                        try {
                            JsonObject body = new JsonObject();body.addProperty("problemId","practice:private:7");body.addProperty("code",CODE);
                            var request = HttpRequest.newBuilder(URI.create(AlgoCraftWebServer.getLocalUrl()+"/api/submit"))
                                    .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                            var response = HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
                            if(response.statusCode()!=200)throw new IllegalStateException(response.body());
                            return new Gson().fromJson(response.body(),SubmissionResult.class);
                        }catch(Exception e){throw new CompletionException(e);}
                    });
                    practiceChecked = true;
                    return;
                }
                check(submission.join().isSuccess() && ProgressManager.isPassed("practice:private:7"), "Web private practice saves local completion without server rewards");
                Files.writeString(output.resolve(role+"-first.json"), new Gson().toJson(Map.of("uuid",mc.player.getUUID().toString(),"checks",checks)));
                phase=5;
            } else if (phase==5) {
                if (ClientCatalog.revision().isEmpty() || ClientCatalog.revision().equals(firstRevision)) return;
                check(ClientCatalog.getProblem(oldProblem.getId())==null, "Administrator disabled bank disappears automatically");
                submission = ServerSubmissionBridge.submit(oldProblem,CODE);
                phase=6;
            } else if (phase==6) {
                if(!submission.isDone()) return;
                check(!submission.join().isSuccess(), "Client rejects obsolete selection after bank update");
                submission = ServerSubmissionBridge.sendForTest(new PacketSubmitSolution(oldProblem.getId(),CODE,0,
                        oldProblem.getPublicationRepository(),oldProblem.getPublicationVersion(),UUID.randomUUID().toString()));
                phase=7;
            } else if (phase==7) {
                if(!submission.isDone()) return;
                check(!submission.join().isSuccess(), "Server rejects forged obsolete-version submission");
                submission = ServerSubmissionBridge.sendForTest(new PacketSubmitSolution("1",CODE,0,"official","0".repeat(64),UUID.randomUUID().toString()));
                phase=8;
            } else if (phase==8) {
                if(!submission.isDone()) return;
                check(!submission.join().isSuccess(), "Server rejects mismatched problem version");
                ProgressManager.saveProgressImmediate();
                JsonObject local = JsonParser.parseString(Files.readString(mc.gameDirectory.toPath().resolve("config/algocraft/user_progress.json"))).getAsJsonObject();
                check(local.has("practice:private:7") && !local.has("serverfixture:7"), "Only private practice progress is persisted to the client's local file");
                finish(mc,null);
            }
        } catch(Throwable e) { finish(mc,e); }
    }
    private void finish(Minecraft mc, Throwable error) {
        finished=true;
        try {
            Map<String,Object> receipt=new LinkedHashMap<>();
            receipt.put("passed",error==null);receipt.put("checks",checks);receipt.put("ticks",ticks);receipt.put("phase",phase);
            receipt.put("error",error==null?null:error.toString());receipt.put("processStartCount",1);receipt.put("worldLoadCount",1);
            receipt.put("desktopIsolation",UnattendedClientTestMode.receipt());
            receipt.put("sessionId",System.getenv("ALGOCRAFT_TEST_SESSION_ID"));
            Files.writeString(output.resolve(role+"-result.json"),new Gson().toJson(receipt));
        } catch(Exception e) { e.printStackTrace(); }
        mc.stop();
    }
}
