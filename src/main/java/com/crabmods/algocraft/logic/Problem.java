package com.crabmods.algocraft.logic;

import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Represents an algorithm problem with test cases.
 * This class is designed to be safely deserialized from JSON.
 * Uses @SerializedName for JSON field mapping while keeping fields private.
 * Translations are loaded from separate files via ProblemTranslationManager.
 */
public class Problem {
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description; // Markdown content
    
    @SerializedName("difficulty")
    private String difficulty; // "EASY", "MEDIUM", "HARD"
    
    @SerializedName("initialCode")
    private String initialCode;
    
    @SerializedName("tags")
    private List<String> tags = new ArrayList<>();
    
    @SerializedName("examples")
    private List<TestCase> examples = new ArrayList<>();
    
    @SerializedName("tests")
    private List<TestCase> tests = new ArrayList<>();

    @SerializedName("diagrams")
    private List<Diagram> diagrams = new ArrayList<>();

    @SerializedName("solutions")
    private List<Solution> solutions = new ArrayList<>();
    
    // Metadata
    @SerializedName("author")
    private String author;
    
    @SerializedName("version")
    private String version;
    
    @SerializedName("lastModified")
    private long lastModified;

    private transient Path assetBaseDir;
    private transient String publicationRepository = "";
    private transient String publicationVersion = "";
    private transient java.util.Map<String, ProblemTranslationManager.ProblemTranslation> publishedTranslations;

    public void setPublication(String repository, String version) { publicationRepository = repository; publicationVersion = version; }
    public String getPublicationRepository() { return publicationRepository == null ? "" : publicationRepository; }
    public String getPublicationVersion() { return publicationVersion == null ? "" : publicationVersion; }
    public boolean isPublished() { return !getPublicationVersion().isEmpty(); }
    public void setPublishedTranslations(java.util.Map<String, ProblemTranslationManager.ProblemTranslation> translations) {
        publishedTranslations = java.util.Map.copyOf(translations);
    }
    private ProblemTranslationManager.ProblemTranslation publishedTranslation(String lang) {
        return publishedTranslations == null || lang == null ? null : publishedTranslations.get(lang);
    }
    
    /**
     * Get the problem ID, never null.
     */
    public String getId() {
        return id != null ? id : "";
    }
    
    /**
     * Set the problem ID. Used for prefixing IDs in repositories.
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the problem title in default language (English), never null.
     */
    public String getTitle() {
        return title != null ? title : "Untitled";
    }
    
    /**
     * Set the problem title.
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Get the problem title in specified language, with fallback to English.
     * Translations are loaded from separate files in lang/{language}/p{id}.json
     * @param lang Language code (e.g., "zh_cn", "ja_jp")
     */
    public String getTitle(String lang) {
        if (publishedTranslations != null) {
            var text = publishedTranslation(lang);
            return text != null && text.hasTitle() ? text.getTitle() : getTitle();
        }
        return ProblemTranslationManager.getTitle(lang, getId(), getTitle());
    }
    
    /**
     * Get the problem description in default language (English), never null.
     */
    public String getDescription() {
        return description != null ? description : "";
    }
    
    /**
     * Set the problem description.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the problem description in specified language, with fallback to English.
     * Translations are loaded from separate files in lang/{language}/p{id}.json
     * @param lang Language code (e.g., "zh_cn", "ja_jp")
     */
    public String getDescription(String lang) {
        if (publishedTranslations != null) {
            var text = publishedTranslation(lang);
            return text != null && text.hasDescription() ? text.getDescription() : getDescription();
        }
        return ProblemTranslationManager.getDescription(lang, getId(), getDescription());
    }
    
    /**
     * Get the difficulty level, normalized to uppercase.
     */
    public String getDifficulty() {
        if (difficulty == null) return "EASY";
        return difficulty.toUpperCase();
    }
    
    /**
     * Set the difficulty level.
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * Get the initial code template, never null.
     */
    public String getInitialCode() {
        return initialCode != null ? initialCode : "class Solution {\n    // Write your code here\n}";
    }
    
    /**
     * Set the initial code template.
     */
    public void setInitialCode(String initialCode) {
        this.initialCode = initialCode;
    }
    
    /**
     * Get tags as an unmodifiable list.
     */
    public List<String> getTags() {
        return tags != null ? Collections.unmodifiableList(tags) : Collections.emptyList();
    }
    
    /**
     * Get examples as an unmodifiable list.
     */
    public List<TestCase> getExamples() {
        return examples != null ? Collections.unmodifiableList(examples) : Collections.emptyList();
    }
    
    /**
     * Set examples list.
     */
    public void setExamples(List<TestCase> examples) {
        this.examples = examples;
    }
    
    /**
     * Get hidden tests as an unmodifiable list.
     */
    public List<TestCase> getTests() {
        return tests != null ? Collections.unmodifiableList(tests) : Collections.emptyList();
    }
    
    /**
     * Set tests list.
     */
    public void setTests(List<TestCase> tests) {
        this.tests = tests;
    }

    /**
     * Get reference solutions as an unmodifiable list.
     */
    public List<Solution> getSolutions() {
        return solutions != null ? Collections.unmodifiableList(solutions) : Collections.emptyList();
    }

    /**
     * Get reference solutions with localized names and explanations when
     * translations are available. Code and language stay unchanged.
     */
    public List<Solution> getSolutions(String lang) {
        List<Solution> baseSolutions = getSolutions();
        if (lang == null || lang.isEmpty() || "en_us".equals(lang)) {
            return baseSolutions;
        }

        List<Solution> localized = new ArrayList<>(baseSolutions.size());
        for (int i = 0; i < baseSolutions.size(); i++) {
            Solution solution = baseSolutions.get(i);
            localized.add(new Solution(
                    ProblemTranslationManager.getSolutionName(lang, getId(), i, solution.getName()),
                    ProblemTranslationManager.getSolutionDescription(lang, getId(), i, solution.getDescription()),
                    solution.getCode(),
                    solution.getLanguage()
            ));
        }
        return Collections.unmodifiableList(localized);
    }

    /**
     * Get structured diagram metadata from current problem files.
     */
    public List<Diagram> getDiagrams() {
        return diagrams != null ? Collections.unmodifiableList(diagrams) : Collections.emptyList();
    }

    /**
     * Return all prompt visuals from structured diagram metadata.
     */
    public List<Visual> getVisuals() {
        return getVisuals(null);
    }

    /**
     * Return all prompt visuals with localized captions when available.
     */
    public List<Visual> getVisuals(String lang) {
        List<Visual> visuals = new ArrayList<>();
        for (Diagram diagram : getDiagrams()) {
            String file = diagram.getFile();
            if (!file.isBlank()) {
                String id = diagram.getId();
                String caption = diagram.getCaption();
                if (publishedTranslations == null) caption = ProblemTranslationManager.getDiagramCaption(lang, getId(), id, caption);
                else {
                    var text = publishedTranslation(lang);
                    if (text != null) for (var translated : text.getDiagrams()) {
                        if (id.equals(translated.getId()) && translated.hasCaption()) caption = translated.getCaption();
                    }
                }
                visuals.add(new Visual(id, file, caption));
            }
        }
        return Collections.unmodifiableList(visuals);
    }

    public Path getAssetBaseDir() {
        return assetBaseDir;
    }

    public void setAssetBaseDir(Path assetBaseDir) {
        this.assetBaseDir = assetBaseDir;
    }
    
    /**
     * Get the author of this problem.
     */
    public String getAuthor() {
        return author != null ? author : "";
    }
    
    /**
     * Get the version of this problem.
     */
    public String getVersion() {
        return version != null ? version : "1.0";
    }
    
    /**
     * Get the last modified timestamp.
     */
    public long getLastModified() {
        return lastModified;
    }
    
    /**
     * Get total number of test cases (examples + hidden tests).
     */
    public int getTotalTestCount() {
        int count = 0;
        if (examples != null) count += examples.size();
        if (tests != null) count += tests.size();
        return count;
    }
    
    /**
     * Validate the problem has minimum required data.
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() 
            && title != null && !title.isEmpty()
            && ((examples != null && !examples.isEmpty()) || (tests != null && !tests.isEmpty()));
    }

    public static class Diagram {
        @SerializedName("id")
        private String id;

        @SerializedName("file")
        private String file;

        @SerializedName("caption")
        private String caption;

        public String getId() {
            return id != null ? id : "";
        }

        public String getFile() {
            return file != null ? file : "";
        }

        public String getCaption() {
            return caption != null ? caption : "";
        }
    }

    public record Visual(String id, String file, String caption) {
        public Visual {
            id = id != null ? id : "";
            file = file != null ? file : "";
            caption = caption != null ? caption : "";
        }
    }

    /**
     * Represents a reference solution bundled with a problem.
     */
    public static class Solution {
        @SerializedName("name")
        private String name;

        @SerializedName("description")
        private String description;

        @SerializedName("code")
        private String code;

        @SerializedName("language")
        private String language;

        public Solution() {
        }

        private Solution(String name, String description, String code, String language) {
            this.name = name;
            this.description = description;
            this.code = code;
            this.language = language;
        }

        public String getName() {
            return name != null ? name : "";
        }

        public String getDescription() {
            return description != null ? description : "";
        }

        public String getCode() {
            return code != null ? code : "";
        }

        public String getLanguage() {
            return language != null ? language : "";
        }
    }

    /**
     * Represents a single test case.
     */
    public static class TestCase {
        @SerializedName("input")
        private String input;
        
        @SerializedName("output")
        private String output;
        
        @SerializedName("hidden")
        private boolean hidden;
        
        /**
         * Get input, never null.
         */
        public String getInput() {
            return input != null ? input : "";
        }
        
        /**
         * Set input.
         */
        public void setInput(String input) {
            this.input = input;
        }
        
        /**
         * Get expected output, never null.
         */
        public String getOutput() {
            return output != null ? output : "";
        }
        
        /**
         * Set expected output.
         */
        public void setOutput(String output) {
            this.output = output;
        }
        
        /**
         * Check if this is a hidden test case.
         */
        public boolean isHidden() {
            return hidden;
        }
        
        /**
         * Check if this is a valid test case.
         */
        public boolean isValid() {
            return input != null && output != null;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Problem problem = (Problem) o;
        return Objects.equals(id, problem.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Problem{id='" + id + "', title='" + title + "', difficulty='" + difficulty + "'}";
    }
}
