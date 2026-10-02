package com.crabmods.algocraft.logic;
import com.crabmods.algocraft.logic.catalog.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientCatalogTest {
    @TempDir Path dir;
    @AfterEach void reset() { ClientCatalog.disconnect(); ProblemManager.resetForTest(); ProgressManager.resetForTest(); System.clearProperty(ProgressManager.CONFIG_DIR_PROPERTY); }
    private PublishedCatalog make(String title) throws Exception {
        Problem problem = new Gson().fromJson("{\"id\":\"1\",\"title\":\""+title+"\",\"examples\":[{\"input\":\"1\",\"output\":\"1\"}],\"tests\":[{\"input\":\"2\",\"output\":\"2\"}]}",Problem.class);
        return PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official",100,List.of(problem))),Set.of("official"));
    }
    private void connect() {
        ProblemManager.replaceRepositoriesForTest(List.of());
        System.setProperty(ProgressManager.CONFIG_DIR_PROPERTY,dir.resolve("local-progress").toString());
        ClientCatalog.connect(dir);
    }
    private void await(String revision) throws Exception {
        long deadline=System.nanoTime()+10_000_000_000L;
        while(!ClientCatalog.revision().equals(revision)&&System.nanoTime()<deadline) Thread.sleep(10);
        assertEquals(revision,ClientCatalog.revision());
    }
    private void transfer(String server,PublishedCatalog catalog) throws Exception {
        assertTrue(ClientCatalog.manifest(server,catalog.revision(),catalog.byteSize(),catalog.chunkCount()).join());
        for(int i=0;i<catalog.chunkCount();i++) ClientCatalog.chunk(catalog.revision(),i,catalog.chunk(i));
        await(catalog.revision());
    }
    @Test void reconnectReusesVerifiedCacheAndCorruptCacheIsDownloadedAgain() throws Exception {
        connect(); String server=UUID.randomUUID().toString();var catalog=make("Server title");
        transfer(server,catalog);
        assertEquals("Server title",ClientCatalog.getProblem("1").getTitle());
        ClientCatalog.disconnect();ClientCatalog.connect(dir);
        assertFalse(ClientCatalog.manifest(server,catalog.revision(),catalog.byteSize(),catalog.chunkCount()).join());
        assertTrue(ClientCatalog.ready());
        ClientCatalog.disconnect();
        Files.writeString(dir.resolve("config/algocraft/server-cache/"+server+"/"+catalog.revision()+".zip"),"damaged");
        ClientCatalog.connect(dir);transfer(server,catalog);
        assertTrue(ClientCatalog.ready());
    }
    @Test void switchingServerOrManifestNeverExposesPreviousCatalogOrProgress() throws Exception {
        connect();var first=make("First server");var second=make("Second server");
        transfer(UUID.randomUUID().toString(),first);
        ProgressManager.updateFromPacket(Map.of("1",123L));assertTrue(ProgressManager.isPassed("1"));
        ClientCatalog.disconnect();ClientCatalog.connect(dir);
        assertNull(ClientCatalog.getProblem("1"));assertFalse(ProgressManager.isPassed("1"));
        String secondServer=UUID.randomUUID().toString();
        assertTrue(ClientCatalog.manifest(secondServer,second.revision(),second.byteSize(),second.chunkCount()).join());
        ClientCatalog.chunk(first.revision(),0,first.chunk(0));assertNull(ClientCatalog.getProblem("1"));
        for(int i=0;i<second.chunkCount();i++)ClientCatalog.chunk(second.revision(),i,second.chunk(i));
        await(second.revision());assertEquals("Second server",ClientCatalog.getProblem("1").getTitle());
    }
}
