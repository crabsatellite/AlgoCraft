package com.crabmods.algocraft.logic.repo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FileBackedOfficialRemote implements RemoteFileClient {
    private static final Gson GSON = new Gson();
    private static final Path OFFICIAL_BANK_ROOT = Path.of("question_bank", "official");

    private final String baseUrl;
    private final Map<String, byte[]> responses = new LinkedHashMap<>();

    private FileBackedOfficialRemote(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    static FileBackedOfficialRemote fromOfficialBank() throws IOException {
        FileBackedOfficialRemote remote =
                new FileBackedOfficialRemote("https://example.test/question_bank/official");
        JsonObject manifest = JsonParser.parseString(
                Files.readString(OFFICIAL_BANK_ROOT.resolve("manifest.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
        for (JsonElement element : manifest.getAsJsonArray("files")) {
            String name = element.getAsJsonObject().get("name").getAsString();
            remote.responses.put(name, Files.readAllBytes(resolveOfficialBankFile(name)));
        }
        remote.publishManifest();
        return remote;
    }

    String baseUrl() {
        return baseUrl;
    }

    void publishProblem(int problemNumber, String json) {
        responses.put("p" + problemNumber + ".json", json.getBytes(StandardCharsets.UTF_8));
        publishManifest();
    }

    private void publishManifest() {
        List<String> names = responses.keySet().stream()
                .filter(name -> !"manifest.json".equals(name))
                .sorted(FileBackedOfficialRemote::compareManifestNames)
                .toList();
        JsonArray files = new JsonArray();
        StringBuilder combinedHashes = new StringBuilder();

        for (String name : names) {
            byte[] body = responses.get(name);
            String hash = sha256(body);
            combinedHashes.append(hash);

            JsonObject file = new JsonObject();
            file.addProperty("name", name);
            file.addProperty("hash", hash);
            file.addProperty("size", body.length);
            files.add(file);
        }

        JsonObject manifest = new JsonObject();
        manifest.addProperty("version", "test");
        manifest.addProperty("lastUpdated", "2026-06-25T00:00:00Z");
        manifest.addProperty("signature", sha256(combinedHashes.toString().getBytes(StandardCharsets.UTF_8)));
        manifest.addProperty("totalProblems", RemoteRepositoryDownloader.OFFICIAL_PROBLEM_COUNT);
        manifest.add("files", files);
        responses.put("manifest.json", GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] get(URI uri) throws IOException {
        String raw = uri.toString();
        String prefix = baseUrl + "/";
        if (!raw.startsWith(prefix)) {
            throw new IOException("Unexpected URI: " + raw);
        }
        String name = raw.substring(prefix.length());
        byte[] body = responses.get(name);
        if (body == null) {
            throw new IOException("Not found: " + name);
        }
        return body;
    }

    private static Path resolveOfficialBankFile(String manifestName) {
        Path path = OFFICIAL_BANK_ROOT;
        for (String part : manifestName.split("/")) {
            path = path.resolve(part);
        }
        return path;
    }

    private static int compareManifestNames(String left, String right) {
        return Comparator
                .comparingInt(FileBackedOfficialRemote::manifestGroup)
                .thenComparingInt(name -> isTopLevelProblem(name) ? problemNumber(name) : 0)
                .thenComparing(Comparator.naturalOrder())
                .compare(left, right);
    }

    private static int manifestGroup(String name) {
        if ("catalog.json".equals(name)) {
            return 0;
        }
        if (isTopLevelProblem(name)) {
            return 1;
        }
        if (name.startsWith("images/")) {
            return 2;
        }
        if (name.startsWith("lang/")) {
            return 3;
        }
        return 4;
    }

    private static boolean isTopLevelProblem(String name) {
        return name.matches("p\\d+\\.json");
    }

    private static int problemNumber(String name) {
        return Integer.parseInt(name.substring(1, name.length() - ".json".length()));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }
}
