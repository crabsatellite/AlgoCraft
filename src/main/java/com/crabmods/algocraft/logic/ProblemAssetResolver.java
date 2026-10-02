package com.crabmods.algocraft.logic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class ProblemAssetResolver {
    private ProblemAssetResolver() {
    }

    public static Optional<Path> resolveImage(Problem problem, Problem.Visual visual) {
        if (problem == null || visual == null) {
            return Optional.empty();
        }
        return resolveImage(problem.getAssetBaseDir(), visual.file());
    }

    public static Optional<Path> resolveImage(Path repositoryRoot, String file) {
        if (repositoryRoot == null) {
            return Optional.empty();
        }

        Optional<String> normalized = normalizeImagePath(file);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path candidate = root.resolve(normalized.get()).normalize();
        if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
            return Optional.empty();
        }

        return Optional.of(candidate);
    }

    public static Optional<String> normalizeImagePath(String file) {
        if (file == null || file.isBlank()) {
            return Optional.empty();
        }

        String normalized = file.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":")) {
            return Optional.empty();
        }
        if (!normalized.startsWith("images/")) {
            normalized = "images/" + normalized;
        }

        Path relative = Paths.get(normalized).normalize();
        if (relative.isAbsolute()
                || relative.getNameCount() != 2
                || !"images".equals(relative.getName(0).toString())
                || relative.startsWith("..")) {
            return Optional.empty();
        }

        String fileName = relative.getFileName().toString();
        if (!fileName.matches("[A-Za-z0-9._-]+\\.png")) {
            return Optional.empty();
        }

        return Optional.of(relative.toString().replace('\\', '/'));
    }
}
