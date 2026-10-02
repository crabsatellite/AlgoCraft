package com.crabmods.algocraft.logic.catalog;

import com.crabmods.algocraft.logic.*;
import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Client-only state, deliberately separate from the integrated server's ProblemManager. */
public final class ClientCatalog {
    private static final Gson GSON = new Gson();
    private static final CatalogTransfer TRANSFER = new CatalogTransfer();
    private static volatile List<PublishedCatalog.Bank> banks = List.of();
    private static volatile boolean connected;
    private static volatile String serverId = "", revision = "";
    private static long generation;
    private static Path cacheRoot;
    private static Runnable listener = () -> {};
    public static void setListener(Runnable action) { listener = action; }
    public static synchronized void connect(Path gameDir) {
        generation++; connected = true; serverId = ""; revision = ""; banks = List.of(); TRANSFER.clear();
        cacheRoot = gameDir.resolve("config/algocraft/server-cache");
        ProgressManager.beginServerSession();
    }
    public static synchronized void disconnect() {
        generation++; connected = false; serverId = ""; revision = ""; banks = List.of(); TRANSFER.clear();
        ProgressManager.endServerSession();
    }
    public static boolean connected() { return connected; }
    public static String serverId() { return serverId; }
    public static String revision() { return revision; }
    public static boolean ready() { return connected && !revision.isEmpty(); }
    public static List<ProblemRepository> getRepositories() {
        if (!connected) return ProblemManager.getRepositories();
        List<ProblemRepository> result = new ArrayList<>(banks);
        // Local imports are explicitly private, with IDs that cannot alias server problems.
        for (ProblemRepository repo : ProblemManager.getRepositories()) {
            if (repo.getName().equals("Official")) continue;
            List<Problem> privateProblems = new ArrayList<>();
            for (Problem source : repo.getProblems()) {
                Problem copy = GSON.fromJson(GSON.toJson(source), Problem.class);
                copy.setAssetBaseDir(source.getAssetBaseDir());
                Map<String, ProblemTranslationManager.ProblemTranslation> translations = new HashMap<>();
                for (String lang : ProblemTranslationManager.SUPPORTED_LANGUAGES) {
                    var text = ProblemTranslationManager.getTranslation(lang, source.getId());
                    if (text != null) translations.put(lang, text);
                }
                copy.setPublishedTranslations(translations);
                copy.setId("practice:" + source.getId());
                privateProblems.add(copy);
            }
            if (!privateProblems.isEmpty()) result.add(new PublishedCatalog.Bank("Practice · " + repo.getName(), repo.getPriority(), privateProblems));
        }
        return List.copyOf(result);
    }
    public static List<Problem> getProblems() { return getRepositories().stream().flatMap(r -> r.getProblems().stream()).toList(); }
    public static Problem getProblem(String id) { return getProblems().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null); }
    public static boolean isCurrent(Problem problem) {
        Problem current = banks.stream().flatMap(b -> b.problems().stream()).filter(p -> p.getId().equals(problem.getId())).findFirst().orElse(null);
        return current != null && current.getPublicationVersion().equals(problem.getPublicationVersion());
    }
    public static synchronized CompletableFuture<Boolean> manifest(String server, String hash, int size, int chunks) throws IOException {
        if (!connected) throw new IOException("Catalog received outside a game session");
        TRANSFER.begin(server, hash, size, chunks);
        serverId = server; revision = ""; banks = List.of();
        long token = ++generation;
        listener.run();
        Path archive = cacheRoot.resolve(server).resolve(hash + ".zip");
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.isRegularFile(archive) && Files.size(archive) == size) {
                    byte[] data = Files.readAllBytes(archive);
                    if (PublishedCatalog.hash(data).equals(hash)) {
                        install(token, server, hash, data);
                        return false;
                    }
                }
            } catch (IOException ignored) { /* damaged cache is downloaded again */ }
            synchronized (ClientCatalog.class) { return connected && token == generation; }
        });
    }
    public static synchronized void chunk(String hash, int index, byte[] data) throws IOException {
        byte[] complete = TRANSFER.accept(hash, index, data);
        if (complete == null) return;
        long token = generation;
        String server = TRANSFER.server();
        CompletableFuture.runAsync(() -> {
            try { install(token, server, hash, complete); }
            catch (IOException e) { System.getLogger(ClientCatalog.class.getName()).log(System.Logger.Level.ERROR, "Catalog installation failed", e); }
        });
    }
    private static void install(long token, String server, String hash, byte[] data) throws IOException {
        Path root;
        synchronized (ClientCatalog.class) {
            if (!connected || token != generation) return;
            root = cacheRoot.resolve(server).resolve(hash);
        }
        List<PublishedCatalog.Bank> installed = PublishedCatalog.readPublic(data, root);
        Path archive = root.resolveSibling(hash + ".zip");
        Path temp = Files.createTempFile(root.getParent(), "catalog-", ".tmp");
        try {
            Files.write(temp, data);
            Files.move(temp, archive, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
        synchronized (ClientCatalog.class) {
            if (!connected || token != generation) return;
            banks = installed; revision = hash;
        }
        listener.run();
    }
}
