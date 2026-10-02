package com.crabmods.algocraft.questionbank;

import com.crabmods.algocraft.logic.ProblemTranslationManager;
import com.crabmods.algocraft.logic.Problem;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OfficialProblemChineseTranslationTest {
    private static final Path QUESTION_BANK = Paths.get(System.getProperty("user.dir"), "question_bank", "official");
    private static final Path ZH_CN = QUESTION_BANK.resolve("lang").resolve("zh_cn");
    private static final int EXPECTED_PROBLEM_COUNT = 500;
    private static final int EXPECTED_SOLUTION_TRANSLATION_COUNT = 500;
    private static final int STRICT_TRANSLATION_QUALITY_COUNT = 500;
    private static final int TEXTBOOK_REVIEWED_TRANSLATION_FORMAT_COUNT = 44;
    private static final Gson GSON = new Gson();
    private static final Pattern CJK = Pattern.compile("[\\p{IsHan}]");
    private static final Pattern MOJIBAKE = Pattern.compile("[涓缁鍚鐨鏁犲俓鈹]");
    private static final Pattern QUESTION_MARK_PLACEHOLDER = Pattern.compile("\\?{2,}");
    private static final Pattern INLINE_CODE_PLACEHOLDER = Pattern.compile("@@(?:\\p{IsHan}+|code)\\d+@@");
    private static final Pattern SMART_QUOTE_IN_CODE_EXAMPLE =
            Pattern.compile("(?:[\\[,，、]\\s*[“”]|[“”]\\s*[,，、\\]])");
    private static final Pattern ENGLISH_SECTION_HEADER = Pattern.compile("(?m)^##\\s+(Example|Examples|Constraints)\\b");
    private static final Pattern ENGLISH_SOLUTION_HEADER = Pattern.compile("(?m)^##\\s+(Approach|Key Insight|Complexity Analysis)\\b");
    private static final Pattern KNOWN_UNTRANSLATED_ENGLISH = Pattern.compile(
            "(Only one valid answer exists|Initializes?|Returns?|Inserts?|Adds?|At most|There will be|"
                    + "All the|consists? of|For example|The answer|The number|Every two|"
                    + "Methods .* will always|is guaranteed|is a lowercase|are unique|is sorted|"
                    + "contains only|will be made|Change the array|Return `k`|Similarly|"
                    + "Each operand|The division|There will not|The input represents|"
                    + "represents a valid expression|The tests are generated|All the integers|"
                    + "Each row|The first integer|if it was rotated|All values|ascending array|"
                    + "contains \\*\\*distinct\\*\\* values|non-decreasing array|mountain array|"
                    + "Stores the key|Update the value|Otherwise, add|least recently used key|"
                    + "The sum of|pointing to some node|valid index in the linked-list|"
                    + "do not intersect|All `Node\\.val` are|will exist in the BST|"
                    + "Both the left and right subtrees|Each value of|calls in total will be made|"
                    + "word` in `addWord`|lowercase English letter|lower-case letters|"
                    + "The length of each word|does not have leading|Maps a string key|"
                    + "the stream of integers|Appends the integer|when you search for the `kth`|"
                    + "Composes a new tweet|started following|started unfollowing|"
                    + "All the tweets|The median|initializes the|adds the integer|"
                    + "returns the median|Answers within|both are sorted|All elements|"
                    + "only one solution|The frequency of each element|"
                    + "There are no|The Graph is connected|Infinity means an empty room|"
                    + "A Sudoku board|Only the filled cells|according to the mentioned rules|"
                    + "representing an empty cell|representing a fresh orange|"
                    + "representing a rotten orange|All the pairs|Every adjacent pair|"
                    + "All the words|shortest transformation sequences|uppercase English letters|"
                    + "All pairs|self-edges|parallel edges|The graph may not be connected|"
                    + "If `x == y`|is in `wordList`|Node\\.val` is unique|"
                    + "If `graph\\[u\\]` contains|consist of only digits|contains only digits|"
                    + "The product of any prefix|After you sell your stock|English characters|"
                    + "English letters|All the values|A nondecreasing array|"
                    + "Each element in the array appears twice|sorted by `starti`|1-indexed|"
                    + "exact-length|next permutation|Word Break|word break|Base-256|"
                    + "De Bruijn|Token 流|切成 token|Minimax|source 是否|"
                    + "source 和 target|从 source|AND 只要|OR 只要|NOT 只有|"
                    + "deleted 状态|forward 指针|tie-break|first-fit|Fenwick Tree|"
                    + "circular sentence|subsequence|startsWith 扫描|调用 startsWith|"
                    + "startsWith 的判断条件|My Calendar II|"
                    + "opposite 结尾|run 的|less、equal、greater|bitmask|bitCount|"
                    + "proper divisor|reshape|enough 函数|布尔 seen|seenA|seenB|counted)");

    @Test
    void officialProblemsHaveCompleteChineseProblemStatements() throws IOException {
        assertTrue(Files.isDirectory(ZH_CN), "missing zh_cn translation directory");

        for (int id = 1; id <= EXPECTED_PROBLEM_COUNT; id++) {
            Path originalPath = QUESTION_BANK.resolve("p" + id + ".json");
            Path translationPath = ZH_CN.resolve("p" + id + ".json");
            assertTrue(Files.isRegularFile(originalPath), "missing original problem p" + id);
            assertTrue(Files.isRegularFile(translationPath), "missing zh_cn translation p" + id);

            JsonObject original = readObject(originalPath);
            JsonObject translation = readObject(translationPath);
            String title = requireString(translation, "title", translationPath);
            String description = requireString(translation, "description", translationPath);

            assertFalse(title.equals(requireString(original, "title", originalPath)),
                    translationPath + " title should not fall back to English");
            assertTrue(CJK.matcher(title).find(), translationPath + " title should contain Chinese characters");
            assertTrue(CJK.matcher(description).find(), translationPath + " description should contain Chinese characters");
            assertFalse(MOJIBAKE.matcher(title).find(), translationPath + " title contains mojibake");
            assertFalse(MOJIBAKE.matcher(description).find(), translationPath + " description contains mojibake");
            assertNoQuestionMarkPlaceholders(title, translationPath, "title");
            assertNoQuestionMarkPlaceholders(description, translationPath, "description");
            assertNoInlineCodePlaceholders(title, translationPath, "title");
            assertNoInlineCodePlaceholders(description, translationPath, "description");
            assertTrue(description.startsWith("# "), translationPath + " description should keep markdown heading");
            assertTrue(hasLocalizedExampleHeading(description),
                    translationPath + " description should localize example headings");
            assertTrue(hasLocalizedConstraintsHeading(description),
                    translationPath + " description should localize constraints heading");
            assertFalse(ENGLISH_SECTION_HEADER.matcher(description).find(),
                    translationPath + " should not keep English section headings");
            if (id <= STRICT_TRANSLATION_QUALITY_COUNT) {
                assertNoKnownUntranslatedEnglish(title, translationPath, "title");
                assertNoKnownUntranslatedEnglish(description, translationPath, "description");
            }
            if (id <= TEXTBOOK_REVIEWED_TRANSLATION_FORMAT_COUNT) {
                assertNoSmartQuotesInCodeExamples(description, translationPath, "description");
            }

            if (id <= EXPECTED_SOLUTION_TRANSLATION_COUNT) {
                JsonArray originalSolutions = requireArray(original, "solutions", originalPath);
                JsonArray translatedSolutions = requireArray(translation, "solutions", translationPath);
                assertEquals(originalSolutions.size(), translatedSolutions.size(),
                        translationPath + " should translate every reference solution");

                for (int i = 0; i < translatedSolutions.size(); i++) {
                    JsonObject originalSolution = originalSolutions.get(i).getAsJsonObject();
                    JsonObject translatedSolution = translatedSolutions.get(i).getAsJsonObject();
                    String solutionName = requireString(translatedSolution, "name", translationPath);
                    String solutionDescription = requireString(translatedSolution, "description", translationPath);

                    assertFalse(translatedSolution.has("code"),
                            translationPath + " solution[" + i + "] should translate text only, not duplicate code");
                    assertFalse(translatedSolution.has("language"),
                            translationPath + " solution[" + i + "] should translate text only, not duplicate language");

                    String originalSolutionName = requireString(originalSolution, "name", originalPath);
                    if (!CJK.matcher(originalSolutionName).find()) {
                        assertFalse(solutionName.equals(originalSolutionName),
                                translationPath + " solution[" + i + "] name should not fall back to English");
                    }
                    assertTrue(CJK.matcher(solutionName).find(),
                            translationPath + " solution[" + i + "] name should contain Chinese characters");
                    assertTrue(CJK.matcher(solutionDescription).find(),
                            translationPath + " solution[" + i + "] description should contain Chinese characters");
                    assertFalse(MOJIBAKE.matcher(solutionName).find(),
                            translationPath + " solution[" + i + "] name contains mojibake");
                    assertFalse(MOJIBAKE.matcher(solutionDescription).find(),
                            translationPath + " solution[" + i + "] description contains mojibake");
                    assertNoQuestionMarkPlaceholders(solutionName, translationPath, "solutions[" + i + "].name");
                    assertNoQuestionMarkPlaceholders(solutionDescription, translationPath,
                            "solutions[" + i + "].description");
                    assertNoInlineCodePlaceholders(solutionName, translationPath, "solutions[" + i + "].name");
                    assertNoInlineCodePlaceholders(solutionDescription, translationPath,
                            "solutions[" + i + "].description");
                    assertTrue(hasLocalizedApproachSection(solutionDescription),
                            translationPath + " solution[" + i + "] should include a localized approach section");
                    assertFalse(ENGLISH_SOLUTION_HEADER.matcher(solutionDescription).find(),
                            translationPath + " solution[" + i + "] should not keep English solution headings");
                    if (id <= STRICT_TRANSLATION_QUALITY_COUNT) {
                        assertNoKnownUntranslatedEnglish(solutionName, translationPath, "solutions[" + i + "].name");
                        assertNoKnownUntranslatedEnglish(solutionDescription, translationPath,
                                "solutions[" + i + "].description");
                    }
                }
            }

            if (original.has("diagrams")) {
                JsonArray originalDiagrams = requireArray(original, "diagrams", originalPath);
                JsonArray translatedDiagrams = requireArray(translation, "diagrams", translationPath);
                assertEquals(originalDiagrams.size(), translatedDiagrams.size(),
                        translationPath + " should translate every diagram caption");

                for (int i = 0; i < translatedDiagrams.size(); i++) {
                    JsonObject originalDiagram = originalDiagrams.get(i).getAsJsonObject();
                    JsonObject translatedDiagram = translatedDiagrams.get(i).getAsJsonObject();
                    String originalId = requireString(originalDiagram, "id", originalPath);
                    String originalCaption = requireString(originalDiagram, "caption", originalPath);
                    String translatedId = requireString(translatedDiagram, "id", translationPath);
                    String translatedCaption = requireString(translatedDiagram, "caption", translationPath);

                    assertEquals(originalId, translatedId,
                            translationPath + " diagrams[" + i + "] id should stay aligned with the original diagram");
                    assertFalse(translatedDiagram.has("file"),
                            translationPath + " diagrams[" + i + "] should translate text only, not duplicate file paths");
                    assertFalse(translatedCaption.equals(originalCaption),
                            translationPath + " diagrams[" + i + "] caption should not fall back to English");
                    assertTrue(CJK.matcher(translatedCaption).find(),
                            translationPath + " diagrams[" + i + "] caption should contain Chinese characters");
                    assertFalse(MOJIBAKE.matcher(translatedCaption).find(),
                            translationPath + " diagrams[" + i + "] caption contains mojibake");
                    assertNoQuestionMarkPlaceholders(translatedCaption, translationPath,
                            "diagrams[" + i + "].caption");
                    assertNoInlineCodePlaceholders(translatedCaption, translationPath,
                            "diagrams[" + i + "].caption");
                    if (id <= STRICT_TRANSLATION_QUALITY_COUNT) {
                        assertNoKnownUntranslatedEnglish(translatedCaption, translationPath,
                                "diagrams[" + i + "].caption");
                    }
                }
            } else {
                assertFalse(translation.has("diagrams"),
                        translationPath + " should not add translated diagrams when the original problem has none");
            }
        }
    }

    @Test
    void zhCnTranslationsLoadThroughRuntimeTranslationManager() throws IOException {
        ProblemTranslationManager.clearCache();

        Map<String, ProblemTranslationManager.ProblemTranslation> translations =
                ProblemTranslationManager.loadTranslations(QUESTION_BANK, "zh_cn").join();

        assertEquals(EXPECTED_PROBLEM_COUNT, translations.size(), "runtime loader should see every zh_cn problem");
        ProblemTranslationManager.registerTranslations("zh_cn", translations);

        assertEquals("两数之和", translations.get("1").getTitle(), "p1 should load as readable Chinese");
        assertTrue(translations.get("500").getDescription().contains("修剪二叉搜索树"),
                "p500 runtime translation should load readable Chinese");
        assertEquals(3, translations.get("1").getSolutions().size(), "p1 should load translated solution entries");
        assertEquals("哈希表 - 一次遍历", translations.get("1").getSolutions().get(0).getName(),
                "p1 solution name should load as readable Chinese");
        assertEquals(1, translations.get("73").getDiagrams().size(), "p73 should load translated diagram captions");
        assertEquals("示例 1 中 put(1,1)、put(2,2)、get(1) 后的缓存：键 2 最久未使用，键 1 最近使用",
                translations.get("73").getDiagrams().get(0).getCaption(),
                "p73 diagram caption should load as readable Chinese");

        assertEquals("两数之和", ProblemTranslationManager.getTitle("zh_cn", "1", "Two Sum"));
        assertTrue(ProblemTranslationManager.getDescription("zh_cn", "500", "Trim a Binary Search Tree")
                        .contains("修剪二叉搜索树"),
                "Problem.getDescription translation path should not fall back to English");
        assertEquals("哈希表 - 一次遍历",
                ProblemTranslationManager.getSolutionName("zh_cn", "1", 0, "Hash Map - One Pass"));
        assertTrue(ProblemTranslationManager.getSolutionDescription("zh_cn", "1", 0, "English fallback")
                        .contains("补数"),
                "solution description translation path should not fall back to English");
        assertEquals("English fallback",
                ProblemTranslationManager.getSolutionName("zh_cn",
                        String.valueOf(EXPECTED_SOLUTION_TRANSLATION_COUNT + 1), 0, "English fallback"),
                "untranslated solution entries should fall back to the original text");

        Problem p1 = GSON.fromJson(readString(QUESTION_BANK.resolve("p1.json")), Problem.class);
        assertEquals("哈希表 - 一次遍历", p1.getSolutions("zh_cn").get(0).getName(),
                "Problem.getSolutions(lang) should apply translated solution names");
        assertEquals(p1.getSolutions().get(0).getCode(), p1.getSolutions("zh_cn").get(0).getCode(),
                "Problem.getSolutions(lang) should not alter reference solution code");

        Problem p73 = GSON.fromJson(readString(QUESTION_BANK.resolve("p73.json")), Problem.class);
        assertEquals("Example 1 after put(1,1), put(2,2), get(1): key 2 is least recent and key 1 most recent",
                p73.getVisuals().get(0).caption(),
                "default visuals should keep the original English caption");
        assertEquals("示例 1 中 put(1,1)、put(2,2)、get(1) 后的缓存：键 2 最久未使用，键 1 最近使用",
                p73.getVisuals("zh_cn").get(0).caption(),
                "Problem.getVisuals(lang) should apply translated diagram captions");
        assertEquals(p73.getVisuals().get(0).file(), p73.getVisuals("zh_cn").get(0).file(),
                "diagram caption localization must not alter image file paths");

        for (int id = 1; id <= EXPECTED_SOLUTION_TRANSLATION_COUNT; id++) {
            Problem problem = GSON.fromJson(readString(QUESTION_BANK.resolve("p" + id + ".json")), Problem.class);
            List<Problem.Solution> originalSolutions = problem.getSolutions();
            List<Problem.Solution> localizedSolutions = problem.getSolutions("zh_cn");

            assertEquals(originalSolutions.size(), localizedSolutions.size(),
                    "p" + id + " localized runtime solution list should stay aligned with the official problem");
            for (int i = 0; i < originalSolutions.size(); i++) {
                Problem.Solution original = originalSolutions.get(i);
                Problem.Solution localized = localizedSolutions.get(i);

                assertEquals(original.getCode(), localized.getCode(),
                        "p" + id + " solution[" + i + "] localization must not change executable code");
                assertEquals(original.getLanguage(), localized.getLanguage(),
                        "p" + id + " solution[" + i + "] localization must not change language metadata");
                assertTrue(CJK.matcher(localized.getName()).find(),
                        "p" + id + " solution[" + i + "] localized runtime name should be Chinese");
                assertTrue(hasLocalizedApproachSection(localized.getDescription()),
                        "p" + id + " solution[" + i + "] localized runtime description should keep teaching sections");
            }
        }
    }

    private static boolean hasLocalizedExampleHeading(String value) {
        return value.contains("## 示例") || value.contains("## 绀轰緥");
    }

    private static boolean hasLocalizedConstraintsHeading(String value) {
        return value.contains("## 约束") || value.contains("## 绾︽潫");
    }

    private static boolean hasLocalizedApproachSection(String value) {
        return value.contains("## 思路") || value.contains("## 鎬濊矾");
    }

    private static JsonObject readObject(Path path) throws IOException {
        return JsonParser.parseString(readString(path)).getAsJsonObject();
    }

    private static String readString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertNoKnownUntranslatedEnglish(String value, Path path, String field) {
        Matcher matcher = KNOWN_UNTRANSLATED_ENGLISH.matcher(value);
        if (matcher.find()) {
            fail(path + " " + field + " contains untranslated English phrase: " + matcher.group());
        }
    }

    private static void assertNoSmartQuotesInCodeExamples(String value, Path path, String field) {
        Matcher matcher = SMART_QUOTE_IN_CODE_EXAMPLE.matcher(value);
        if (matcher.find()) {
            fail(path + " " + field + " code examples should use ASCII quotes and separators: " + matcher.group());
        }
    }

    private static void assertNoQuestionMarkPlaceholders(String value, Path path, String field) {
        Matcher matcher = QUESTION_MARK_PLACEHOLDER.matcher(value);
        if (matcher.find()) {
            fail(path + " " + field + " contains placeholder question marks: " + matcher.group());
        }
    }

    private static void assertNoInlineCodePlaceholders(String value, Path path, String field) {
        Matcher matcher = INLINE_CODE_PLACEHOLDER.matcher(value);
        if (matcher.find()) {
            fail(path + " " + field + " contains inline code placeholder: " + matcher.group());
        }
    }

    private static String requireString(JsonObject object, String key, Path path) {
        assertTrue(object.has(key), path + " missing " + key);
        String value = object.get(key).getAsString();
        assertFalse(value.isBlank(), path + " " + key + " should not be blank");
        return value;
    }

    private static JsonArray requireArray(JsonObject object, String key, Path path) {
        assertTrue(object.has(key), path + " missing " + key);
        assertTrue(object.get(key).isJsonArray(), path + " " + key + " should be an array");
        JsonArray value = object.getAsJsonArray(key);
        assertFalse(value.isEmpty(), path + " " + key + " should not be empty");
        return value;
    }
}
