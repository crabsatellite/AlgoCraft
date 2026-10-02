package com.crabmods.algocraft.logic.catalog;

import com.crabmods.algocraft.logic.*;
import com.crabmods.algocraft.logic.repo.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.*;

/** An immutable publication. Judge data stays in this snapshot; only public data goes on the wire. */
public final class PublishedCatalog {
    public static final int MAX_ARCHIVE_BYTES = 16 * 1024 * 1024;
    public static final int MAX_EXPANDED_BYTES = 64 * 1024 * 1024;
    public static final int CHUNK_BYTES = 24 * 1024;
    private static final Gson GSON = new Gson();
    public record Entry(String repository, String version, Problem problem) {}
    public record Bank(String name, int priority, List<Problem> problems) implements ProblemRepository {
        public Bank { problems = List.copyOf(problems); }
        public String getName() { return name; }
        public int getPriority() { return priority; }
        public List<Problem> getProblems() { return problems; }
        public CompletableFuture<Void> refresh() { return CompletableFuture.completedFuture(null); }
    }
    private final Map<String, Entry> entries;
    private final byte[] archive;
    private final String revision;
    private PublishedCatalog(Map<String, Entry> entries, byte[] archive) {
        this.entries = Map.copyOf(entries);
        this.archive = archive;
        revision = hash(archive);
    }
    public Entry get(String id) { return entries.get(id); }
    public int size() { return entries.size(); }
    public List<ProblemRepository> judgeRepositories() {
        Map<String, List<Problem>> grouped = new TreeMap<>();
        entries.values().forEach(entry -> grouped.computeIfAbsent(entry.repository(), key -> new ArrayList<>()).add(entry.problem()));
        return grouped.entrySet().stream().<ProblemRepository>map(group -> new Bank(group.getKey(), 0, group.getValue())).toList();
    }
    public String revision() { return revision; }
    public byte[] archive() { return archive.clone(); }
    public int byteSize() { return archive.length; }
    public byte[] chunk(int index) {
        int start = Math.multiplyExact(index, CHUNK_BYTES);
        if (start < 0 || start >= archive.length) throw new IllegalArgumentException("Invalid chunk");
        return Arrays.copyOfRange(archive, start, Math.min(start + CHUNK_BYTES, archive.length));
    }
    public int chunkCount() { return (archive.length + CHUNK_BYTES - 1) / CHUNK_BYTES; }

    public static PublishedCatalog build(List<ProblemRepository> repositories, Set<String> enabled) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        Map<String, byte[]> files = new TreeMap<>();
        JsonArray banks = new JsonArray();
        for (ProblemRepository repository : repositories) {
            String bankId = repositoryId(repository.getName());
            if (!enabled.contains(bankId)) continue;
            if (!bankId.matches("[\\p{L}\\p{N}_-]{1,128}")) throw new IOException("Invalid bank identity");
            JsonObject bank = new JsonObject();
            bank.addProperty("id", bankId);
            bank.addProperty("name", repository.getName());
            bank.addProperty("priority", repository.getPriority());
            JsonArray problems = new JsonArray();
            for (Problem original : repository.getProblems()) {
                if (!original.isValid() || original.getId().length() > 256 || entries.size() >= 5000 || entries.containsKey(original.getId()))
                    throw new IOException("Invalid or duplicate published problem: " + original.getId());
                if (original.getExamples().stream().anyMatch(test -> test == null || !test.isValid())
                        || original.getTests().stream().anyMatch(test -> test == null || !test.isValid()))
                    throw new IOException("Invalid test case in published problem: " + original.getId());
                JsonObject full = GSON.toJsonTree(original).getAsJsonObject();
                Problem judge = GSON.fromJson(full, Problem.class);
                judge.setAssetBaseDir(original.getAssetBaseDir());
                JsonObject translations = new JsonObject();
                for (String lang : ProblemTranslationManager.SUPPORTED_LANGUAGES) {
                    var translation = ProblemTranslationManager.getTranslation(lang, original.getId());
                    if (translation != null) {
                        JsonObject text = GSON.toJsonTree(translation).getAsJsonObject();
                        text.remove("solutions");
                        translations.add(lang, text);
                    }
                }
                StringBuilder identity = new StringBuilder(bankId).append('\n').append(full).append('\n').append(translations);
                for (Problem.Visual visual : original.getVisuals()) {
                    Path image = ProblemAssetResolver.resolveImage(original, visual)
                            .orElseThrow(() -> new IOException("Missing published image: " + visual.file()));
                    if (Files.size(image) > 2 * 1024 * 1024) throw new IOException("Published image too large");
                    byte[] bytes = Files.readAllBytes(image);
                    String path = bankId + "/" + ProblemAssetResolver.normalizeImagePath(visual.file()).orElseThrow();
                    byte[] previous = files.putIfAbsent(path, bytes);
                    if (previous != null && !Arrays.equals(previous, bytes)) throw new IOException("Conflicting image: " + path);
                    identity.append('\n').append(path).append(':').append(hash(bytes));
                }
                String version = hash(identity.toString().getBytes(StandardCharsets.UTF_8));
                judge.setPublication(bankId, version);
                entries.put(judge.getId(), new Entry(bankId, version, judge));
                JsonObject publicProblem = full.deepCopy();
                publicProblem.remove("tests");
                publicProblem.remove("solutions");
                JsonObject item = new JsonObject();
                item.add("problem", publicProblem);
                item.add("translations", translations);
                item.addProperty("version", version);
                problems.add(item);
            }
            bank.add("problems", problems);
            banks.add(bank);
        }
        JsonObject manifest = new JsonObject();
        manifest.addProperty("schema", 1);
        manifest.add("banks", banks);
        files.put("catalog.json", GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
        if (files.get("catalog.json").length > 32 * 1024 * 1024) throw new IOException("Catalog manifest too large");
        long expanded = files.values().stream().mapToLong(a -> a.length).sum();
        if (expanded > MAX_EXPANDED_BYTES) throw new IOException("Published catalog too large");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (var file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(0);
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        if (output.size() > MAX_ARCHIVE_BYTES) throw new IOException("Published archive too large");
        return new PublishedCatalog(entries, output.toByteArray());
    }

    /** Validate the entire archive before writing any content. No arbitrary zip paths are accepted. */
    public static List<Bank> readPublic(byte[] archive, Path root) throws IOException {
        if (archive.length == 0 || archive.length > MAX_ARCHIVE_BYTES) throw new IOException("Invalid catalog size");
        Map<String, byte[]> files = new LinkedHashMap<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.equals("catalog.json") && !name.matches("[\\p{L}\\p{N}_-]+/images/[A-Za-z0-9._-]+\\.png"))
                    throw new IOException("Invalid catalog path");
                if (files.containsKey(name) || files.size() > 10000) throw new IOException("Duplicate or excessive catalog files");
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = zip.read(buffer)) != -1) {
                    total += n;
                    if (total > MAX_EXPANDED_BYTES || data.size() + n > (name.equals("catalog.json") ? 32 * 1024 * 1024 : 2 * 1024 * 1024))
                        throw new IOException("Expanded catalog too large");
                    data.write(buffer, 0, n);
                }
                files.put(name, data.toByteArray());
            }
        }
        try {
            byte[] json = Objects.requireNonNull(files.get("catalog.json"), "Missing manifest");
            JsonObject manifest = JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject();
            if (manifest.get("schema").getAsInt() != 1) throw new IOException("Unknown catalog schema");
            List<Bank> banks = new ArrayList<>();
            Set<String> ids = new HashSet<>(), bankIds = new HashSet<>();
            for (JsonElement bankValue : manifest.getAsJsonArray("banks")) {
                JsonObject bank = bankValue.getAsJsonObject();
                String bankId = bank.get("id").getAsString();
                if (!bankId.matches("[\\p{L}\\p{N}_-]{1,128}") || !bankIds.add(bankId)) throw new IOException("Invalid bank id");
                List<Problem> problems = new ArrayList<>();
                for (JsonElement itemValue : bank.getAsJsonArray("problems")) {
                    JsonObject item = itemValue.getAsJsonObject();
                    Problem problem = GSON.fromJson(item.get("problem"), Problem.class);
                    String version = item.get("version").getAsString();
                    if (problem.getId().isBlank() || problem.getId().length() > 256 || !ids.add(problem.getId())
                            || ids.size() > 5000 || !version.matches("[a-f0-9]{64}")
                            || !problem.getTests().isEmpty() || !problem.getSolutions().isEmpty()) throw new IOException("Invalid public problem");
                    problem.setPublication(bankId, version);
                    problem.setAssetBaseDir(root.resolve(bankId));
                    Map<String, ProblemTranslationManager.ProblemTranslation> translations = new HashMap<>();
                    for (var translated : item.getAsJsonObject("translations").entrySet())
                        translations.put(translated.getKey(), GSON.fromJson(translated.getValue(), ProblemTranslationManager.ProblemTranslation.class));
                    problem.setPublishedTranslations(translations);
                    for (Problem.Visual visual : problem.getVisuals()) {
                        String path = bankId + "/" + ProblemAssetResolver.normalizeImagePath(visual.file()).orElseThrow();
                        if (!files.containsKey(path)) throw new IOException("Missing catalog image");
                    }
                    problems.add(problem);
                }
                banks.add(new Bank("Server · " + bank.get("name").getAsString(), bank.get("priority").getAsInt(), problems));
            }
            for (var file : files.entrySet()) {
                Path target = root.resolve(file.getKey());
                Files.createDirectories(target.getParent());
                Files.write(target, file.getValue());
            }
            return List.copyOf(banks);
        } catch (RuntimeException e) { throw new IOException("Invalid catalog manifest", e); }
    }

    public static String repositoryId(String name) { return LocalProblemRepository.defaultPrefixForName(name); }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
