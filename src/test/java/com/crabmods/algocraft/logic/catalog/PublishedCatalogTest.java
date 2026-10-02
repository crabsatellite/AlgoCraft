package com.crabmods.algocraft.logic.catalog;
import com.crabmods.algocraft.logic.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class PublishedCatalogTest {
    @TempDir Path dir;
    private Problem problem(String id, String expected) {
        return new Gson().fromJson("""
                {"id":"%s","title":"Addition","description":"Add a and b.","initialCode":"class Solution {}",
                 "examples":[{"input":"[1,2]","output":"3"}],"tests":[{"input":"[5,6]","output":"%s"}],
                 "solutions":[{"code":"SECRET_REFERENCE","name":"secret"}]}
                """.formatted(id, expected), Problem.class);
    }
    @Test void publicationStripsJudgeDataAndRetainsExamples() throws Exception {
        Problem original = problem("1", "11");
        var published = PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official", 100, List.of(original))), Set.of("official"));
        assertEquals("11", published.get("1").problem().getTests().get(0).getOutput());
        var client = PublishedCatalog.readPublic(published.archive(), dir).get(0).problems().get(0);
        assertTrue(client.getTests().isEmpty()); assertTrue(client.getSolutions().isEmpty());
        assertEquals("3", client.getExamples().get(0).getOutput()); assertTrue(client.isPublished());
        assertEquals(published.get("1").version(), client.getPublicationVersion());
        assertEquals("official", client.getPublicationRepository());
        original.setTests(List.of()); // mutation of the source cannot change the published judge
        assertEquals(1, published.get("1").problem().getTests().size());
    }
    @Test void hiddenTestChangeInvalidatesVersionAndPublication() throws Exception {
        var first = PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official", 100, List.of(problem("1", "11")))), Set.of("official"));
        var same = PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official", 100, List.of(problem("1", "11")))), Set.of("official"));
        var updated = PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official", 100, List.of(problem("1", "12")))), Set.of("official"));
        assertEquals(first.revision(), same.revision());
        assertNotEquals(first.get("1").version(), updated.get("1").version());
        assertNotEquals(first.revision(), updated.revision());
    }
    @Test void onlyEnabledBanksArePublishedAndDuplicatesFailClosed() throws Exception {
        var official = new PublishedCatalog.Bank("Official",100,List.of(problem("1","11")));
        var personal = new PublishedCatalog.Bank("Private",50,List.of(problem("private:1","11")));
        var catalog = PublishedCatalog.build(List.of(official,personal),Set.of("official"));
        assertEquals(1,catalog.size()); assertNull(catalog.get("private:1"));
        assertThrows(IOException.class, () -> PublishedCatalog.build(List.of(official, new PublishedCatalog.Bank("Collision",50,List.of(problem("1","22")))),Set.of("official","collision")));
    }
    @Test void imagesAndTranslationsComeFromPublication() throws Exception {
        Path image = dir.resolve("images/one.png"); Files.createDirectories(image.getParent()); Files.write(image,new byte[]{1,2,3});
        Problem p = new Gson().fromJson("""
            {"id":"img:1","title":"Image","examples":[{"input":"1","output":"1"}],"diagrams":[{"id":"example","file":"one.png","caption":"English"}]}
            """,Problem.class); p.setAssetBaseDir(dir);
        var translation = new Gson().fromJson("{\"title\":\"中文题目\",\"description\":\"说明\",\"diagrams\":[{\"id\":\"example\",\"caption\":\"中文图\"}]}",ProblemTranslationManager.ProblemTranslation.class);
        ProblemTranslationManager.registerTranslations("zh_cn",Map.of("img:1",translation));
        var published = PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Image",50,List.of(p))),Set.of("image"));
        Problem client = PublishedCatalog.readPublic(published.archive(),dir.resolve("client")).get(0).problems().get(0);
        ProblemTranslationManager.clearCache();
        assertEquals("中文题目",client.getTitle("zh_cn")); assertEquals("中文图",client.getVisuals("zh_cn").get(0).caption());
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(ProblemAssetResolver.resolveImage(client,client.getVisuals().get(0)).orElseThrow()));
    }
    @Test void missingImageBlocksPublication() {
        Problem p = new Gson().fromJson("{\"id\":\"1\",\"title\":\"Missing\",\"examples\":[{\"input\":\"1\",\"output\":\"1\"}],\"diagrams\":[{\"file\":\"missing.png\"}]}",Problem.class);
        p.setAssetBaseDir(dir);
        assertThrows(IOException.class, () -> PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official",100,List.of(p))),Set.of("official")));
    }
    @Test void malformedTestCasesCannotBePublished() {
        Problem p=problem("1","11");p.setTests(Arrays.asList((Problem.TestCase)null));
        assertThrows(IOException.class, () -> PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official",100,List.of(p))),Set.of("official")));
        p.setTests(List.of(new Problem.TestCase()));
        assertThrows(IOException.class, () -> PublishedCatalog.build(List.of(new PublishedCatalog.Bank("Official",100,List.of(p))),Set.of("official")));
    }
    @Test void zipTraversalAndOversizedManifestAreRejected() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(var zip = new ZipOutputStream(bytes)) {zip.putNextEntry(new ZipEntry("../escaped.txt"));zip.write(1);zip.closeEntry();}
        assertThrows(IOException.class, () -> PublishedCatalog.readPublic(bytes.toByteArray(),dir));
        assertFalse(Files.exists(dir.resolveSibling("escaped.txt")));
        CatalogTransfer transfer = new CatalogTransfer();
        assertThrows(IOException.class, () -> transfer.begin(UUID.randomUUID().toString(),"a".repeat(64),Integer.MAX_VALUE,1));
    }
    @Test void chunkAssemblyChecksOrderHashAndSupersededVersions() throws Exception {
        byte[] archive = new byte[PublishedCatalog.CHUNK_BYTES + 3]; new Random(7).nextBytes(archive);
        String hash = PublishedCatalog.hash(archive), server = UUID.randomUUID().toString();
        CatalogTransfer transfer = new CatalogTransfer(); transfer.begin(server,hash,archive.length,2);
        assertNull(transfer.accept("b".repeat(64),0,new byte[3]));
        assertNull(transfer.accept(hash,0,Arrays.copyOf(archive,PublishedCatalog.CHUNK_BYTES)));
        assertArrayEquals(archive,transfer.accept(hash,1,Arrays.copyOfRange(archive,PublishedCatalog.CHUNK_BYTES,archive.length)));
        transfer.begin(server,hash,archive.length,2);
        assertThrows(IOException.class, () -> transfer.accept(hash,1,new byte[3]));
        transfer.begin(server,hash,archive.length,2);
        transfer.accept(hash,0,new byte[PublishedCatalog.CHUNK_BYTES]);
        assertThrows(IOException.class, () -> transfer.accept(hash,1,new byte[3]));
    }
    @Test void compressedCodeRoundTripsAndZipBombIsBounded() throws Exception {
        String code = "// 中文\n".repeat(5000);
        assertEquals(code,SubmissionWire.decode(SubmissionWire.encode(code)));
        assertThrows(IOException.class, () -> SubmissionWire.decode(SubmissionWire.encode("x".repeat(200001))));
        byte[] random = new byte[100000]; new Random(123).nextBytes(random);
        assertThrows(IOException.class, () -> SubmissionWire.encode(Base64.getEncoder().encodeToString(random)));
    }
}
