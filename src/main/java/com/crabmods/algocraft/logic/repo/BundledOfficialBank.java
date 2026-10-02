package com.crabmods.algocraft.logic.repo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Installs the shipped, validated bank once; existing caches are always preserved. */
public final class BundledOfficialBank {
    private BundledOfficialBank() {}

    public static synchronized boolean installIfEmpty(Path cache) throws IOException {
        Files.createDirectories(cache);
        try (var files = Files.list(cache)) {
            if (files.findAny().isPresent()) return false;
        }
        InputStream resource = BundledOfficialBank.class.getResourceAsStream("/assets/algocraft/official-bank.zip");
        if (resource == null) throw new IOException("Bundled official question bank is missing");
        Path staging = Files.createTempDirectory(cache.toAbsolutePath().getParent(), "official-bundled-");
        try {
            try (ZipInputStream zip = new ZipInputStream(resource)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path target = staging.resolve(entry.getName()).normalize();
                    if (!target.startsWith(staging)) throw new IOException("Invalid bundled bank path");
                    if (entry.isDirectory()) Files.createDirectories(target);
                    else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target);
                    }
                }
            }
            RemoteRepositoryDownloader.validateCachedOfficialRepository(staging);
            // A cache that changed while extraction ran must never be replaced.
            try (var files = Files.list(cache)) {
                if (files.findAny().isPresent()) return false;
            }
            Files.delete(cache); // Empty directory only; validated contents move as one unit.
            try {
                Files.move(staging, cache, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                if (!Files.exists(cache)) Files.move(staging, cache);
                else throw e;
            }
            return true;
        } finally {
            if (Files.exists(staging)) try (var files = Files.walk(staging)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
