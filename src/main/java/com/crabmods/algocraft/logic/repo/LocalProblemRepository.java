package com.crabmods.algocraft.logic.repo;

import com.crabmods.algocraft.logic.Problem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * A repository that loads problems from a local directory.
 */
public class LocalProblemRepository implements ProblemRepository {
    private static final System.Logger LOGGER = System.getLogger(LocalProblemRepository.class.getName());
    private static final Gson GSON = new GsonBuilder().create();

    private final String name;
    private final Path directory;
    private final String prefix;
    private final int priority;
    private final AtomicReference<List<Problem>> problems = new AtomicReference<>(List.of());

    public LocalProblemRepository(String name, Path directory) {
        this(name, directory, defaultPrefixForName(name), 50);
    }

    public LocalProblemRepository(String name, Path directory, String prefix) {
        this(name, directory, prefix, 50);
    }

    public LocalProblemRepository(String name, Path directory, String prefix, int priority) {
        this.name = name;
        this.directory = directory;
        this.prefix = prefix;
        this.priority = priority;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public List<Problem> getProblems() {
        return new ArrayList<>(problems.get());
    }

    @Override
    public CompletableFuture<Void> refresh() {
        return CompletableFuture.runAsync(() -> {
            List<Problem> loaded = new ArrayList<>();

            if (!Files.exists(directory)) {
                LOGGER.log(System.Logger.Level.WARNING, "Repository directory does not exist: " + directory);
                if (!problems.get().isEmpty()) {
                    throw new CompletionException(new IOException(
                            "Repository directory is missing after a successful load: " + directory));
                }
                problems.set(List.of());
                return;
            }
            if (!Files.isDirectory(directory)) {
                IOException error = new IOException("Repository path is not a directory: " + directory);
                LOGGER.log(System.Logger.Level.ERROR, error.getMessage(), error);
                throw new CompletionException(error);
            }

            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json"))
                        .filter(p -> p.getFileName().toString().matches("p\\d+\\.json"))
                        .sorted(Comparator.comparingInt(LocalProblemRepository::problemNumber))
                        .toList()) {
                    loaded.add(loadProblem(file));
                }
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to list files in repository: " + directory, e);
                throw new CompletionException(e);
            }

            try {
                com.crabmods.algocraft.logic.ProblemTranslationManager.loadAllTranslations(directory, prefix).join();
            } catch (CompletionException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to load translations from " + directory, e.getCause());
                throw e;
            } catch (Throwable e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to load translations from " + directory, e);
                throw new CompletionException(e);
            }

            problems.set(List.copyOf(loaded));
            LOGGER.log(System.Logger.Level.DEBUG, "Loaded " + loaded.size() + " problems from " + name);
        });
    }

    private Problem loadProblem(Path file) throws IOException {
        try {
            String json = Files.readString(file);
            Problem problem = GSON.fromJson(json, Problem.class);
            if (problem != null && problem.isValid()) {
                problem.setAssetBaseDir(directory);
                // Prefix non-official local/custom IDs so they cannot shadow the
                // official remote problem IDs in the aggregate cache.
                String id = problem.getId();
                if (prefix != null && !prefix.isBlank() && !id.startsWith(prefix + ":")) {
                    problem.setId(prefix + ":" + id);
                }
                return problem;
            }
            throw new IOException("Invalid local problem JSON: " + file);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to load problem from " + file + ": " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            IOException error = new IOException("Failed to load problem from " + file, e);
            LOGGER.log(System.Logger.Level.ERROR, error.getMessage(), error);
            throw error;
        }
    }

    private static int problemNumber(Path path) {
        String name = path.getFileName().toString();
        if (name.matches("p\\d+\\.json")) {
            return Integer.parseInt(name.substring(1, name.length() - 5));
        }
        return Integer.MAX_VALUE;
    }

    public static String defaultPrefixForName(String name) {
        if (name == null) {
            return "repo";
        }
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "_");
        normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
        return normalized.isBlank() ? "repo" : normalized;
    }
}
