package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

/**
 * Manages problem translations loaded from separate language files.
 *
 * <p>Translation files are stored in a separate directory structure:
 * <pre>
 * question_bank/official/
 * ├── p1.json              # Original problem (English)
 * ├── p2.json
 * └── lang/                # Translations directory
 *     ├── zh_cn/
 *     │   ├── p1.json      # Chinese translation for p1
 *     │   └── p2.json
 *     └── ja_jp/
 *         ├── p1.json      # Japanese translation for p1
 *         └── p2.json
 * </pre>
 *
 * <p>Translation JSON format:
 * <pre>
 * {
 *   "title": "两数之和",
 *   "description": "给定一个整数数组..."
 * }
 * </pre>
 */
public class ProblemTranslationManager {

    private static final System.Logger LOGGER = System.getLogger(ProblemTranslationManager.class.getName());
    private static final Gson GSON = new Gson();

    // Cache: language -> (problemId -> translation)
    private static final Map<String, Map<String, ProblemTranslation>> translationCache = new ConcurrentHashMap<>();

    // Supported languages
    public static final String[] SUPPORTED_LANGUAGES = {"zh_cn", "ja_jp", "ko_kr", "de_de", "fr_fr", "es_es"};

    /**
     * Represents a problem translation.
     */
    public static class ProblemTranslation {
        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        @SerializedName("solutions")
        private List<SolutionTranslation> solutions;

        @SerializedName("diagrams")
        private List<DiagramTranslation> diagrams;

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public List<SolutionTranslation> getSolutions() {
            return solutions != null ? Collections.unmodifiableList(solutions) : Collections.emptyList();
        }

        public List<DiagramTranslation> getDiagrams() {
            return diagrams != null ? Collections.unmodifiableList(diagrams) : Collections.emptyList();
        }

        public boolean hasTitle() {
            return title != null && !title.isEmpty();
        }

        public boolean hasDescription() {
            return description != null && !description.isEmpty();
        }

        public boolean hasSolutions() {
            return solutions != null && !solutions.isEmpty();
        }

        public boolean hasDiagrams() {
            return diagrams != null && !diagrams.isEmpty();
        }
    }

    /**
     * Represents a localized reference solution entry. Entries are matched by
     * index to the problem JSON's solutions array.
     */
    public static class SolutionTranslation {
        @SerializedName("name")
        private String name;

        @SerializedName("description")
        private String description;

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean hasName() {
            return name != null && !name.isEmpty();
        }

        public boolean hasDescription() {
            return description != null && !description.isEmpty();
        }
    }

    /**
     * Represents localized diagram text. Entries are matched by diagram id and
     * intentionally do not repeat file names so translations cannot alter the
     * resource that the problem points at.
     */
    public static class DiagramTranslation {
        @SerializedName("id")
        private String id;

        @SerializedName("caption")
        private String caption;

        public String getId() {
            return id;
        }

        public String getCaption() {
            return caption;
        }

        public boolean hasId() {
            return id != null && !id.isEmpty();
        }

        public boolean hasCaption() {
            return caption != null && !caption.isEmpty();
        }
    }

    /**
     * Load translations for a specific language from a repository directory.
     *
     * @param repoPath The root path of the repository (e.g., question_bank/official)
     * @param lang The language code (e.g., "zh_cn")
     * @return A map of problemId -> translation
     */
    public static CompletableFuture<Map<String, ProblemTranslation>> loadTranslations(Path repoPath, String lang) {
        return loadTranslations(repoPath, lang, "");
    }

    /**
     * Load translations for a repository and namespace the loaded problem ids.
     *
     * <p>Official repositories use an empty prefix and keep numeric ids like
     * {@code 1}. Custom repositories use the same prefix as their loaded
     * problems, for example {@code user:1}, so translations from different
     * repositories cannot overwrite each other in the global language cache.</p>
     */
    public static CompletableFuture<Map<String, ProblemTranslation>> loadTranslations(
            Path repoPath, String lang, String problemIdPrefix) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, ProblemTranslation> translations = new ConcurrentHashMap<>();
            Path langDir = repoPath.resolve("lang").resolve(lang);

            if (!Files.exists(langDir)) {
                LOGGER.log(System.Logger.Level.DEBUG,
                        "No translation directory found for language '" + lang + "' at " + langDir);
                return translations;
            }
            if (!Files.isDirectory(langDir)) {
                IOException error = new IOException("Translation path is not a directory: " + langDir);
                LOGGER.log(System.Logger.Level.ERROR, error.getMessage(), error);
                throw new CompletionException(error);
            }

            try (Stream<Path> paths = Files.list(langDir)) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".json"))
                        .filter(p -> p.getFileName().toString().matches("p\\d+\\.json"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .toList()) {
                    loadTranslationFile(path, problemIdPrefix, translations);
                }
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to list translation directory: " + langDir, e);
                throw new CompletionException(e);
            }

            LOGGER.log(System.Logger.Level.INFO,
                    "Loaded " + translations.size() + " translations for language '" + lang + "' from " + repoPath);
            return translations;
        });
    }

    private static void loadTranslationFile(Path path, String problemIdPrefix,
                                            Map<String, ProblemTranslation> translations) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            ProblemTranslation translation = GSON.fromJson(content, ProblemTranslation.class);
            if (translation == null) {
                throw new IOException("Translation file produced no translation object: " + path);
            }
            // Extract problem ID from filename (e.g., "p1.json" -> "1")
            String filename = path.getFileName().toString();
            String problemId = translationProblemId(filename, problemIdPrefix);
            translations.put(problemId, translation);
        } catch (Exception e) {
            IOException error = new IOException("Failed to load translation from " + path, e);
            LOGGER.log(System.Logger.Level.ERROR, error.getMessage(), error);
            throw new CompletionException(error);
        }
    }

    /**
     * Load translations from a resource input stream (for built-in repository).
     */
    public static ProblemTranslation loadFromStream(InputStream stream) {
        if (stream == null) return null;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, ProblemTranslation.class);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to load translation from stream", e);
            return null;
        }
    }

    /**
     * Get cached translation for a problem in a specific language.
     *
     * @param lang The language code
     * @param problemId The problem ID (without prefix)
     * @return The translation, or null if not found
     */
    public static ProblemTranslation getTranslation(String lang, String problemId) {
        Map<String, ProblemTranslation> langCache = translationCache.get(lang);
        return langCache != null ? langCache.get(problemId) : null;
    }

    /**
     * Register loaded translations into the cache.
     */
    public static void registerTranslations(String lang, Map<String, ProblemTranslation> translations) {
        translationCache.computeIfAbsent(lang, k -> new ConcurrentHashMap<>()).putAll(translations);
    }

    private static void replaceTranslations(String lang, String problemIdPrefix,
                                            Map<String, ProblemTranslation> translations) {
        Map<String, ProblemTranslation> langCache =
                translationCache.computeIfAbsent(lang, k -> new ConcurrentHashMap<>());
        synchronized (langCache) {
            langCache.keySet().removeIf(problemId -> isInNamespace(problemId, problemIdPrefix));
            langCache.putAll(translations);
        }
    }

    /**
     * Clear all cached translations.
     */
    public static void clearCache() {
        translationCache.clear();
    }

    /**
     * Get the translated title for a problem, with fallback to default.
     *
     * @param lang The language code
     * @param problemId The problem ID
     * @param defaultTitle The default (English) title
     * @return The translated title, or default if not found
     */
    public static String getTitle(String lang, String problemId, String defaultTitle) {
        if (lang == null || lang.isEmpty() || lang.equals("en_us")) {
            return defaultTitle;
        }

        ProblemTranslation translation = getTranslation(lang, extractNumericId(problemId));
        if (translation != null && translation.hasTitle()) {
            return translation.getTitle();
        }
        return defaultTitle;
    }

    /**
     * Get the translated description for a problem, with fallback to default.
     *
     * @param lang The language code
     * @param problemId The problem ID
     * @param defaultDescription The default (English) description
     * @return The translated description, or default if not found
     */
    public static String getDescription(String lang, String problemId, String defaultDescription) {
        if (lang == null || lang.isEmpty() || lang.equals("en_us")) {
            return defaultDescription;
        }

        ProblemTranslation translation = getTranslation(lang, extractNumericId(problemId));
        if (translation != null && translation.hasDescription()) {
            return translation.getDescription();
        }
        return defaultDescription;
    }

    /**
     * Get the translated reference solution name, with fallback to the original
     * solution entry when no localized entry exists.
     */
    public static String getSolutionName(String lang, String problemId, int solutionIndex, String defaultName) {
        SolutionTranslation translation = getSolutionTranslation(lang, problemId, solutionIndex);
        if (translation != null && translation.hasName()) {
            return translation.getName();
        }
        return defaultName;
    }

    /**
     * Get the translated reference solution explanation, with fallback to the
     * original solution entry when no localized entry exists.
     */
    public static String getSolutionDescription(String lang, String problemId, int solutionIndex, String defaultDescription) {
        SolutionTranslation translation = getSolutionTranslation(lang, problemId, solutionIndex);
        if (translation != null && translation.hasDescription()) {
            return translation.getDescription();
        }
        return defaultDescription;
    }

    /**
     * Get the translated diagram caption, with fallback to the original
     * problem metadata when no localized caption exists.
     */
    public static String getDiagramCaption(String lang, String problemId, String diagramId, String defaultCaption) {
        if (lang == null || lang.isEmpty() || lang.equals("en_us") || diagramId == null || diagramId.isEmpty()) {
            return defaultCaption;
        }

        ProblemTranslation translation = getTranslation(lang, extractNumericId(problemId));
        if (translation == null || !translation.hasDiagrams()) {
            return defaultCaption;
        }
        for (DiagramTranslation diagram : translation.getDiagrams()) {
            if (diagram != null && diagramId.equals(diagram.getId()) && diagram.hasCaption()) {
                return diagram.getCaption();
            }
        }
        return defaultCaption;
    }

    private static SolutionTranslation getSolutionTranslation(String lang, String problemId, int solutionIndex) {
        if (lang == null || lang.isEmpty() || lang.equals("en_us") || solutionIndex < 0) {
            return null;
        }

        ProblemTranslation translation = getTranslation(lang, extractNumericId(problemId));
        if (translation == null || !translation.hasSolutions() || solutionIndex >= translation.getSolutions().size()) {
            return null;
        }
        return translation.getSolutions().get(solutionIndex);
    }

    /**
     * Extract numeric ID from a prefixed problem ID.
     * e.g., "official_1" -> "1", "dev_42" -> "42", "1" -> "1"
     */
    private static String extractNumericId(String problemId) {
        if (problemId == null) return "";
        if (problemId.contains(":")) {
            return problemId;
        }
        int underscoreIdx = problemId.lastIndexOf('_');
        if (underscoreIdx >= 0 && underscoreIdx < problemId.length() - 1) {
            return problemId.substring(underscoreIdx + 1);
        }
        return problemId;
    }

    /**
     * Load translations for all supported languages from a repository.
     */
    public static CompletableFuture<Void> loadAllTranslations(Path repoPath) {
        return loadAllTranslations(repoPath, "");
    }

    public static CompletableFuture<Void> loadAllTranslations(Path repoPath, String problemIdPrefix) {
        Map<String, CompletableFuture<Map<String, ProblemTranslation>>> futures = new LinkedHashMap<>();
        for (String lang : SUPPORTED_LANGUAGES) {
            futures.put(lang, loadTranslations(repoPath, lang, problemIdPrefix));
        }
        return CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new))
                .thenRun(() -> {
                    for (Map.Entry<String, CompletableFuture<Map<String, ProblemTranslation>>> entry : futures.entrySet()) {
                        replaceTranslations(entry.getKey(), problemIdPrefix, entry.getValue().join());
                    }
                });
    }

    private static String translationProblemId(String filename, String problemIdPrefix) {
        String problemId = filename.substring(1, filename.length() - ".json".length());
        if (problemIdPrefix == null || problemIdPrefix.isBlank()) {
            return problemId;
        }
        return problemIdPrefix + ":" + problemId;
    }

    private static boolean isInNamespace(String problemId, String problemIdPrefix) {
        if (problemId == null) {
            return false;
        }
        if (problemIdPrefix == null || problemIdPrefix.isBlank()) {
            return !problemId.contains(":");
        }
        return problemId.startsWith(problemIdPrefix + ":");
    }
}
