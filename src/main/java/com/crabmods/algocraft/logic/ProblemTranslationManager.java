package com.crabmods.algocraft.logic;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
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
    
    private static final Logger LOGGER = LogUtils.getLogger();
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
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean hasTitle() {
            return title != null && !title.isEmpty();
        }
        
        public boolean hasDescription() {
            return description != null && !description.isEmpty();
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
        return CompletableFuture.supplyAsync(() -> {
            Map<String, ProblemTranslation> translations = new ConcurrentHashMap<>();
            Path langDir = repoPath.resolve("lang").resolve(lang);
            
            if (!Files.exists(langDir) || !Files.isDirectory(langDir)) {
                LOGGER.debug("No translation directory found for language '{}' at {}", lang, langDir);
                return translations;
            }
            
            try (Stream<Path> paths = Files.list(langDir)) {
                paths.filter(p -> p.toString().endsWith(".json"))
                     .parallel()
                     .forEach(path -> {
                         try {
                             String content = Files.readString(path, StandardCharsets.UTF_8);
                             ProblemTranslation translation = GSON.fromJson(content, ProblemTranslation.class);
                             if (translation != null) {
                                 // Extract problem ID from filename (e.g., "p1.json" -> "1")
                                 String filename = path.getFileName().toString();
                                 String problemId = filename.replace(".json", "").replace("p", "");
                                 translations.put(problemId, translation);
                             }
                         } catch (Exception e) {
                             LOGGER.error("Failed to load translation from {}", path, e);
                         }
                     });
            } catch (IOException e) {
                LOGGER.error("Failed to list translation directory: {}", langDir, e);
            }
            
            LOGGER.info("Loaded {} translations for language '{}' from {}", translations.size(), lang, repoPath);
            return translations;
        });
    }
    
    /**
     * Load translations from a resource input stream (for built-in repository).
     */
    public static ProblemTranslation loadFromStream(InputStream stream) {
        if (stream == null) return null;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, ProblemTranslation.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load translation from stream", e);
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
     * Extract numeric ID from a prefixed problem ID.
     * e.g., "official_1" -> "1", "dev_42" -> "42", "1" -> "1"
     */
    private static String extractNumericId(String problemId) {
        if (problemId == null) return "";
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
        CompletableFuture<?>[] futures = new CompletableFuture[SUPPORTED_LANGUAGES.length];
        for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
            final String lang = SUPPORTED_LANGUAGES[i];
            futures[i] = loadTranslations(repoPath, lang).thenAccept(translations -> {
                if (!translations.isEmpty()) {
                    registerTranslations(lang, translations);
                }
            });
        }
        return CompletableFuture.allOf(futures);
    }
}
