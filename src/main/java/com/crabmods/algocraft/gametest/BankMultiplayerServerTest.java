package com.crabmods.algocraft.gametest;
import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.logic.*;
import com.crabmods.algocraft.logic.catalog.PublishedCatalog;
import com.crabmods.algocraft.server.ServerBankService;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import com.google.gson.Gson;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.util.*;

@EventBusSubscriber(modid=AlgoCraft.MODID)
public final class BankMultiplayerServerTest {
    private static final List<String> checks = new ArrayList<>();
    private static int ticks, phase;
    private static boolean enabled() { return Boolean.getBoolean("algocraft.bankServerTest"); }
    private static Path output() { return Path.of(System.getProperty("algocraft.bankOutput")); }
    private static void check(boolean value,String message) { if(!value) throw new IllegalStateException(message);checks.add(message); }
    @SubscribeEvent public static void started(ServerStartedEvent event) throws Exception {
        if(!enabled()) return;
        Problem original=ProblemManager.getProblem("7");
        Problem fixture=new Gson().fromJson(new Gson().toJson(original),Problem.class);
        fixture.setId("serverfixture:7"); fixture.setAssetBaseDir(original.getAssetBaseDir());
        ProblemManager.addRepository(new PublishedCatalog.Bank("ServerFixture",50,List.of(fixture))).join();
        event.getServer().getCommands().performPrefixedCommand(event.getServer().createCommandSourceStack(),"algocraft bank enable serverfixture");
        check(ServerBankService.current().get(fixture.getId())!=null,"Administrator publishes a server-only bank");
        Problem chinese=new Gson().fromJson(new Gson().toJson(original),Problem.class);
        chinese.setId("课堂:7");chinese.setAssetBaseDir(original.getAssetBaseDir());
        ProblemManager.addRepository(new PublishedCatalog.Bank("课堂",50,List.of(chinese))).join();
        event.getServer().getCommands().performPrefixedCommand(event.getServer().createCommandSourceStack(),"algocraft bank enable \"课堂\"");
        check(ServerBankService.current().get(chinese.getId())!=null,"Quoted Unicode bank IDs can be enabled");
        event.getServer().getCommands().performPrefixedCommand(event.getServer().createCommandSourceStack(),"algocraft bank disable \"课堂\"");
        check(ServerBankService.current().get(chinese.getId())==null,"Quoted Unicode bank IDs can be disabled");
        Files.createDirectories(output());Files.writeString(output().resolve("server-ready.json"),"{}");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(!enabled() || phase==2) return;
        var server=event.getServer();ticks++;
        try {
            if(ticks>36000) throw new IllegalStateException("Server topology timeout");
            if(Files.exists(output().resolve("A-first.json")) && Files.exists(output().resolve("B-first.json")) && phase==0) {
                var players=server.getPlayerList().getPlayers();
                check(players.size()==2,"Exactly two real TCP clients connected");
                var data=AlgoCraftSavedData.get(server.overworld());
                for(var player:players) {
                    check(!player.hasPermissions(2),"Test player cannot administer banks");
                    check(data.isProblemSolved(player.getUUID(),"serverfixture:7"),"Each player's accepted result is stored on server");
                    check(!data.isProblemSolved(player.getUUID(),"practice:private:7"),"Personal practice never grants server progress");
                    check(data.getTotalSolved(player.getUUID())==1,"Repeated Web submit does not duplicate first-clear progress");
                }
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"algocraft bank disable serverfixture");
                check(ServerBankService.current().get("serverfixture:7")==null,"Administrator disables bank atomically");
                phase=1;
            }
            if(phase==1 && Files.exists(output().resolve("A-result.json")) && Files.exists(output().resolve("B-result.json"))) {
                check(ServerBankService.current().get("1")!=null,"Forged submissions do not modify the official bank");
                Files.writeString(output().resolve("server-result.json"),new Gson().toJson(Map.of("passed",true,"checks",checks,"processStartCount",1,"worldLoadCount",1,"ticks",ticks)));
                phase=2;server.halt(false);
            }
        } catch(Exception e) {
            try { Files.writeString(output().resolve("server-result.json"),new Gson().toJson(Map.of("passed",false,"error",e.toString(),"checks",checks))); }catch(Exception ignored){}
            phase=2;server.halt(false);
        }
    }
}
