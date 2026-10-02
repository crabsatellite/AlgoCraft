package com.crabmods.algocraft.questionbank;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialDiagramLayoutTest {
    private static final Path ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path QUESTION_BANK = ROOT.resolve("question_bank").resolve("official");
    private static final Path IMAGES = QUESTION_BANK.resolve("images");
    private static final Path LAYOUT_REPORT = ROOT.resolve("scripts").resolve("diagrams").resolve("minecraft_style_layout.json");
    private static final Pattern PROBLEM_FILE = Pattern.compile("p\\d+\\.json");
    private static final Pattern LATIN_TEXT = Pattern.compile(".*[A-Za-z].*");
    private static final int MIN_LATIN_TEXT_PADDING = 4;
    private static final Set<String> REFERENCED_WITHOUT_BOX_TEXT = Set.of("p336_example1.png");

    @Test
    void minecraftStyleDiagramTextFitsItsBoxesAndMatchesCurrentPngs() throws IOException {
        assertTrue(Files.isRegularFile(LAYOUT_REPORT), "Minecraft-style diagram layout report is missing");

        JsonObject report = readObject(LAYOUT_REPORT);
        assertEquals(1, report.get("version").getAsInt(), "layout report version should be explicit");
        JsonArray images = report.getAsJsonArray("images");
        assertFalse(images.isEmpty(), "layout report should cover generated official diagrams");

        Set<String> referencedFiles = referencedDiagramFiles();
        Map<String, JsonObject> layoutByFile = new HashMap<>();
        int checkedTextBoxes = 0;
        boolean sawP385Mouse = false;

        for (JsonElement element : images) {
            JsonObject imageEntry = element.getAsJsonObject();
            String file = requireString(imageEntry, "file", "layout image");
            assertTrue(layoutByFile.put(file, imageEntry) == null, "layout report repeats image " + file);

            Path imagePath = IMAGES.resolve(file);
            assertTrue(Files.isRegularFile(imagePath), "layout report image is missing: " + file);
            assertEquals(requireString(imageEntry, "sha256", file), sha256(Files.readAllBytes(imagePath)),
                    "layout report hash is stale for " + file);

            BufferedImage image = ImageIO.read(imagePath.toFile());
            assertNotNull(image, "layout report image should decode: " + file);
            assertEquals(requireInt(imageEntry, "width", file), image.getWidth(), "layout width mismatch for " + file);
            assertEquals(requireInt(imageEntry, "height", file), image.getHeight(), "layout height mismatch for " + file);

            JsonArray checks = imageEntry.getAsJsonArray("checks");
            assertNotNull(checks, "layout image should include text fit checks: " + file);
            if (referencedFiles.contains(file) && !REFERENCED_WITHOUT_BOX_TEXT.contains(file)) {
                assertFalse(checks.isEmpty(), "official prompt diagram should include text fit checks: " + file);
            }

            for (JsonElement checkElement : checks) {
                JsonObject check = checkElement.getAsJsonObject();
                String text = requireString(check, "text", file + " check");
                assertFalse(text.isBlank(), file + " check text should not be blank");
                int[] container = requireBox(check, "container", file + " " + text);
                int[] textBounds = requireBox(check, "textBounds", file + " " + text);
                assertInside(new int[]{0, 0, image.getWidth(), image.getHeight()}, container,
                        file + " text '" + text + "' container should fit the PNG canvas");
                assertInside(container, textBounds, file + " text '" + text + "' should fit its drawn box");
                if (LATIN_TEXT.matcher(text).matches()) {
                    assertPadding(container, textBounds, MIN_LATIN_TEXT_PADDING,
                            file + " Latin text '" + text + "' should keep readable padding inside its box");
                }
                assertTrue(textBounds[2] > textBounds[0], file + " text '" + text + "' should have positive width");
                checkedTextBoxes++;

                if (file.equals("p385_example1.png") && text.equals("1 mouse")) {
                    sawP385Mouse = true;
                    assertTrue(container[2] - container[0] > 54,
                            "p385 mouse node should be widened beyond the old fixed node width");
                }
            }
        }

        assertTrue(checkedTextBoxes > 1000, "layout report should cover all generated diagram text boxes");
        assertTrue(sawP385Mouse, "layout report should guard the p385 mouse label regression");

        Set<String> missingLayoutChecks = new HashSet<>(referencedFiles);
        missingLayoutChecks.removeAll(layoutByFile.keySet());
        assertTrue(missingLayoutChecks.isEmpty(),
                "every official prompt diagram should have layout fit checks; missing " + missingLayoutChecks);

        Set<String> staleLayoutChecks = new HashSet<>(layoutByFile.keySet());
        staleLayoutChecks.removeAll(referencedFiles);
        assertTrue(staleLayoutChecks.isEmpty(),
                "layout report should not keep checks for unreferenced official diagrams; stale " + staleLayoutChecks);
    }

    private static Set<String> referencedDiagramFiles() throws IOException {
        Set<String> files = new HashSet<>();
        try (Stream<Path> stream = Files.list(QUESTION_BANK)) {
            for (Path problemFile : stream
                    .filter(path -> PROBLEM_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(Path::toString))
                    .toList()) {
                JsonObject problem = readObject(problemFile);
                if (!problem.has("diagrams")) {
                    continue;
                }
                for (JsonElement element : problem.getAsJsonArray("diagrams")) {
                    JsonObject diagram = element.getAsJsonObject();
                    files.add(requireString(diagram, "file", problemFile.getFileName().toString() + " diagram"));
                }
            }
        }
        return files;
    }

    private static JsonObject readObject(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String requireString(JsonObject object, String field, String context) {
        assertTrue(object.has(field), context + " missing " + field);
        return object.get(field).getAsString();
    }

    private static int requireInt(JsonObject object, String field, String context) {
        assertTrue(object.has(field), context + " missing " + field);
        return object.get(field).getAsInt();
    }

    private static int[] requireBox(JsonObject object, String field, String context) {
        assertTrue(object.has(field), context + " missing " + field);
        JsonArray values = object.getAsJsonArray(field);
        assertEquals(4, values.size(), context + " " + field + " should be [left, top, right, bottom]");
        return new int[]{
                values.get(0).getAsInt(),
                values.get(1).getAsInt(),
                values.get(2).getAsInt(),
                values.get(3).getAsInt()
        };
    }

    private static void assertInside(int[] container, int[] textBounds, String message) {
        assertTrue(textBounds[0] >= container[0], message + " left");
        assertTrue(textBounds[1] >= container[1], message + " top");
        assertTrue(textBounds[2] <= container[2], message + " right");
        assertTrue(textBounds[3] <= container[3], message + " bottom");
    }

    private static void assertPadding(int[] container, int[] textBounds, int padding, String message) {
        assertTrue(textBounds[0] - container[0] >= padding, message + " left padding");
        assertTrue(textBounds[1] - container[1] >= padding, message + " top padding");
        assertTrue(container[2] - textBounds[2] >= padding, message + " right padding");
        assertTrue(container[3] - textBounds[3] >= padding, message + " bottom padding");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
