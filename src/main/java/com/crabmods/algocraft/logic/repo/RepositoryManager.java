package com.crabmods.algocraft.logic.repo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Manages custom problem repositories.
 */
public class RepositoryManager {
    private static final System.Logger LOGGER = System.getLogger(RepositoryManager.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    private static final Set<String> RESERVED_REPOSITORY_SLUGS = Set.of("official", "user", "built_in");

    private static Path configDir;
    private static Path reposDir;
    private static Path configFile;
    private static List<RepositoryMetadata> repositories = new ArrayList<>();
    private static OfficialRepository officialRepository;
    private static boolean initialized = false;

    public static void init() {
        init(FMLPaths.GAMEDIR.get());
    }

    static void init(Path gameDir) {
        synchronized (LOCK) {
            if (initialized) return;

            configDir = gameDir.resolve("config").resolve("algocraft");
            reposDir = gameDir.resolve("algorithm_challenges").resolve("repos");
            configFile = configDir.resolve("repositories.json");

            try {
                Files.createDirectories(configDir);
                Files.createDirectories(reposDir);
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to create repository directories", e);
            }

            loadConfigLocked();

            // Initialize official repository
            Path officialCacheDir = reposDir.resolve("official");
            try {
                Files.createDirectories(officialCacheDir);
                BundledOfficialBank.installIfEmpty(officialCacheDir);
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to create official cache dir", e);
            }
            officialRepository = new OfficialRepository(officialCacheDir);

            initialized = true;
            LOGGER.log(System.Logger.Level.INFO,
                    "RepositoryManager initialized with " + repositories.size() + " custom repositories");
        }
    }

    private static void loadConfigLocked() {
        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                List<RepositoryMetadata> loaded = GSON.fromJson(json,
                    new TypeToken<List<RepositoryMetadata>>(){}.getType());
                if (loaded != null) {
                    repositories = canonicalizeRepositories(loaded);
                    if (repositories.size() != loaded.size()) {
                        saveConfigBestEffortLocked();
                    }
                }
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to load repository config", e);
            }
        }
    }

    private static void saveConfigLocked() throws IOException {
        Files.createDirectories(configDir);
        String json = GSON.toJson(repositories);
        Path tempFile = Files.createTempFile(configDir, "repositories", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(tempFile, json);
            try {
                Files.move(tempFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private static void saveConfigBestEffortLocked() {
        try {
            saveConfigLocked();
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to save repository config", e);
        }
    }

    public static OfficialRepository getOfficialRepository() {
        return officialRepository;
    }

    public static List<RepositoryMetadata> getRepositories() {
        synchronized (LOCK) {
            return copyRepositories(repositories);
        }
    }

    public static void removeRepository(String name) {
        synchronized (LOCK) {
            repositories.removeIf(r -> r.name.equals(name));
            saveConfigBestEffortLocked();
        }
    }

    public static File getRepositoryDir(RepositoryMetadata meta) {
        if (!hasRepositorySlugCharacters(meta.name)) {
            throw new IllegalArgumentException("Repository name must include at least one letter or digit");
        }
        String slug = repositorySlug(meta.name);
        if (isReservedRepositorySlug(slug)) {
            throw new IllegalArgumentException("Repository name '" + meta.name
                    + "' is reserved after normalization to slug '" + slug + "'");
        }
        return reposDir.resolve(slug).toFile();
    }

    public static CompletableFuture<Void> updateRepository(RepositoryMetadata meta) {
        return installOrUpdateRepository(meta.name, meta.url).thenApply(updated -> null);
    }

    public static CompletableFuture<RepositoryMetadata> installOrUpdateRepository(String name, String url) {
        return installOrUpdateRepository(name, url, new HttpRemoteFileClient(), System.currentTimeMillis());
    }

    static CompletableFuture<RepositoryMetadata> installOrUpdateRepository(
            String name, String url, RemoteFileClient client, long nowMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RepositoryMetadata candidate = new RepositoryMetadata(name, url);
                synchronized (LOCK) {
                    requireNoSlugConflictLocked(candidate);
                }

                RemoteRepositoryDownloader.update(candidate.url, getRepositoryDir(candidate).toPath(), client);
                candidate.lastUpdated = nowMs;
                synchronized (LOCK) {
                    requireNoSlugConflictLocked(candidate);
                    upsertRepositoryLocked(candidate);
                    saveConfigLocked();
                }
                return new RepositoryMetadata(candidate);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    static void resetForTest() {
        synchronized (LOCK) {
            configDir = null;
            reposDir = null;
            configFile = null;
            repositories = new ArrayList<>();
            officialRepository = null;
            initialized = false;
        }
    }

    private static void upsertRepositoryLocked(RepositoryMetadata candidate) {
        String candidateSlug = repositorySlug(candidate.name);
        for (int i = 0; i < repositories.size(); i++) {
            if (repositorySlug(repositories.get(i).name).equals(candidateSlug)) {
                repositories.set(i, new RepositoryMetadata(candidate));
                return;
            }
        }
        repositories.add(new RepositoryMetadata(candidate));
    }

    private static List<RepositoryMetadata> copyRepositories(List<RepositoryMetadata> source) {
        List<RepositoryMetadata> copy = new ArrayList<>();
        for (RepositoryMetadata meta : source) {
            copy.add(new RepositoryMetadata(meta));
        }
        return copy;
    }

    private static List<RepositoryMetadata> canonicalizeRepositories(List<RepositoryMetadata> loaded) {
        List<RepositoryMetadata> copy = new ArrayList<>();
        Set<String> slugs = new HashSet<>();
        for (RepositoryMetadata meta : loaded) {
            if (meta == null || meta.name == null || meta.name.isBlank()) {
                continue;
            }
            if (!hasRepositorySlugCharacters(meta.name)) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Ignoring repository metadata without a stable normalized slug: " + meta.name);
                continue;
            }
            String slug = repositorySlug(meta.name);
            if (isReservedRepositorySlug(slug)) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Ignoring repository metadata with reserved normalized slug " + slug + ": " + meta.name);
                continue;
            }
            if (slugs.add(slug)) {
                copy.add(new RepositoryMetadata(meta));
            } else {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Ignoring repository metadata with duplicate normalized slug " + slug + ": " + meta.name);
            }
        }
        return copy;
    }

    private static void requireNoSlugConflictLocked(RepositoryMetadata candidate) throws IOException {
        if (!hasRepositorySlugCharacters(candidate.name)) {
            throw new IOException("Repository name must include at least one letter or digit");
        }
        String candidateSlug = repositorySlug(candidate.name);
        if (isReservedRepositorySlug(candidateSlug)) {
            throw new IOException("Repository name '" + candidate.name
                    + "' is reserved after normalization to slug '" + candidateSlug + "'");
        }
        for (RepositoryMetadata existing : repositories) {
            if (repositorySlug(existing.name).equals(candidateSlug) && !existing.name.equals(candidate.name)) {
                throw new IOException("Repository name '" + candidate.name
                        + "' conflicts with existing repository '" + existing.name
                        + "' after normalization to slug '" + candidateSlug + "'");
            }
        }
    }

    private static String repositorySlug(String name) {
        return LocalProblemRepository.defaultPrefixForName(name);
    }

    private static boolean hasRepositorySlugCharacters(String name) {
        return name != null && name.codePoints().anyMatch(Character::isLetterOrDigit);
    }

    private static boolean isReservedRepositorySlug(String slug) {
        return RESERVED_REPOSITORY_SLUGS.contains(slug);
    }
}
