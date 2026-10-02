package com.crabmods.algocraft.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementTrophyContractTest {
    private static final Path ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path MAIN = ROOT.resolve("src").resolve("main");
    private static final Path JAVA = MAIN.resolve("java").resolve("com").resolve("crabmods").resolve("algocraft");
    private static final Path ITEM_MODELS = MAIN.resolve("resources").resolve("assets").resolve("algocraft")
            .resolve("models").resolve("item");

    private static final Set<String> TROPHY_ITEM_IDS = Set.of(
            "bronze_trophy",
            "silver_trophy",
            "gold_trophy",
            "diamond_trophy",
            "netherite_trophy"
    );
    private static final Set<String> TROPHY_TIER_DETAIL_ELEMENTS = Set.of(
            "front_black_split_lower",
            "front_black_split_upper",
            "left_leaf_rib",
            "right_leaf_rib"
    );

    private static final Pattern CONSTANT_PATTERN = Pattern.compile(
            "public static final String ([A-Z0-9_]+) = \"([a-z0-9_]+)\";"
    );
    private static final Pattern REGISTER_PATTERN = Pattern.compile(
            "register\\(\\s*([A-Z0-9_]+)\\s*,\\s*"
                    + "AchievementType\\.([A-Z_]+)\\s*,\\s*"
                    + "Rarity\\.([A-Z]+)\\s*,\\s*"
                    + "\"([a-z_]+)\"\\s*,\\s*"
                    + "(\\d+)\\s*\\)",
            Pattern.MULTILINE
    );

    @Test
    void everyAchievementDeclaresARealTrophyItemModelAndUniqueVariant() throws IOException {
        String registry = readJava("logic/AchievementRegistry.java");
        List<AchievementSpec> achievements = achievements(registry);
        Set<String> usedTrophyIds = new HashSet<>();
        Set<Integer> modelDataValues = new HashSet<>();

        for (AchievementSpec achievement : achievements) {
            assertTrue(TROPHY_ITEM_IDS.contains(achievement.trophyItemId()),
                    achievement.constant() + " should declare a real trophy item id");
            assertTrue(Files.isRegularFile(ITEM_MODELS.resolve(achievement.trophyItemId() + ".json")),
                    achievement.constant() + " trophy item model is missing: " + achievement.trophyItemId());
            assertTrue(modelDataValues.add(achievement.modelData()),
                    achievement.constant() + " should have a unique custom model data value");
            assertTrue(Files.isRegularFile(ITEM_MODELS.resolve("trophy_variants").resolve(achievement.id() + ".json")),
                    achievement.constant() + " should have an achievement-specific trophy variant model");
            usedTrophyIds.add(achievement.trophyItemId());
        }

        assertEquals(23, achievements.size(), "all registered achievements should be covered by this contract");
        assertEquals(TROPHY_ITEM_IDS, usedTrophyIds, "the achievement set should exercise every trophy tier");
        assertFalse(registry.contains("\"trophy_bronze\""), "old model-style trophy ids should not remain");
        assertFalse(registry.contains("\"trophy_silver\""), "old model-style trophy ids should not remain");
        assertFalse(registry.contains("\"trophy_gold\""), "old model-style trophy ids should not remain");
        assertFalse(registry.contains("\"trophy_diamond\""), "old model-style trophy ids should not remain");
        assertFalse(registry.contains("\"trophy_netherite\""), "old model-style trophy ids should not remain");
    }

    @Test
    void trophyItemModelsOverrideToAchievementSpecificVariants() throws IOException {
        Map<String, List<AchievementSpec>> byTrophy = new HashMap<>();
        for (AchievementSpec achievement : achievements(readJava("logic/AchievementRegistry.java"))) {
            byTrophy.computeIfAbsent(achievement.trophyItemId(), ignored -> new ArrayList<>()).add(achievement);
        }

        for (Map.Entry<String, List<AchievementSpec>> entry : byTrophy.entrySet()) {
            JsonObject itemModel = readObject(ITEM_MODELS.resolve(entry.getKey() + ".json"));
            JsonArray overrides = array(itemModel, "overrides", entry.getKey() + " item model");
            assertEquals(entry.getValue().size(), overrides.size(), entry.getKey() + " should override every achievement variant");

            Map<Integer, String> actualOverrides = new HashMap<>();
            for (JsonElement overrideElement : overrides) {
                JsonObject override = overrideElement.getAsJsonObject();
                JsonObject predicate = object(override, "predicate", entry.getKey() + " override");
                int modelData = predicate.get("custom_model_data").getAsInt();
                actualOverrides.put(modelData, string(override, "model", entry.getKey() + " override"));
            }

            for (AchievementSpec achievement : entry.getValue()) {
                assertEquals("algocraft:item/trophy_variants/" + achievement.id(),
                        actualOverrides.get(achievement.modelData()),
                        achievement.constant() + " should resolve to its own trophy variant model");
            }
        }
    }

    @Test
    void trophyVariantModelsShareTierSilhouetteAndAddAchievementMarks() throws IOException {
        for (AchievementSpec achievement : achievements(readJava("logic/AchievementRegistry.java"))) {
            JsonObject itemModel = readObject(ITEM_MODELS.resolve(achievement.trophyItemId() + ".json"));
            String parent = string(itemModel, "parent", achievement.trophyItemId() + " model");
            String shapeName = parent.substring("algocraft:item/".length());
            JsonObject baseShape = readObject(ITEM_MODELS.resolve(shapeName + ".json"));
            JsonObject variant = readObject(ITEM_MODELS.resolve("trophy_variants").resolve(achievement.id() + ".json"));
            assertValidTrophyVariantModel(variant, achievement.id());

            int baseElements = array(baseShape, "elements", shapeName).size();
            int variantElements = array(variant, "elements", achievement.id()).size();
            assertTrue(variantElements > baseElements,
                    achievement.constant() + " variant should add visible achievement-specific geometry");
            assertTrue(variantElements <= 64,
                    achievement.constant() + " variant should stay compact enough for a Minecraft item model");
            assertEquals("minecraft:block/block", string(variant, "parent", achievement.id()));
            JsonObject itemTextures = object(itemModel, "textures", achievement.trophyItemId());
            JsonObject variantTextures = object(variant, "textures", achievement.id());
            assertEquals(itemTextures.keySet(),
                    variantTextures.keySet(),
                    achievement.constant() + " variant should use the same tier material slots");
            for (String slot : itemTextures.keySet()) {
                assertEquals(string(itemTextures, slot, achievement.trophyItemId() + " texture " + slot),
                        string(variantTextures, slot, achievement.id() + " texture " + slot),
                        achievement.constant() + " variant should use the same tier material for " + slot);
            }
        }
    }

    private static void assertValidTrophyVariantModel(JsonObject model, String context) {
        Set<String> requiredTextureSlots = Set.of("base", "shadow", "metal", "accent", "gem", "particle");
        Set<String> allowedFaceTextures = Set.of("#base", "#shadow", "#metal", "#accent", "#gem");
        Set<String> seenNames = new HashSet<>();

        assertEquals("minecraft:block/block", string(model, "parent", context));
        assertTrue(object(model, "textures", context + " textures").keySet().containsAll(requiredTextureSlots),
                context + " should expose all tier material slots");
        assertTrue(model.has("display") && model.get("display").isJsonObject(),
                context + " should define item display transforms");

        for (JsonElement element : array(model, "elements", context)) {
            assertTrue(element.isJsonObject(), context + " element should be an object");
            JsonObject object = element.getAsJsonObject();
            String name = string(object, "name", context + " element");
            assertTrue(seenNames.add(name), context + " should not reuse element name " + name);
            assertModelCoordinatesAreInVanillaBounds(object, context + " element " + name);
            assertModelRotationIsVanillaCompatible(object);

            JsonObject faces = object(object, "faces", context + " element faces");
            assertEquals(Set.of("down", "up", "north", "south", "west", "east"), faces.keySet(),
                    context + " each trophy cuboid should render all faces");
            for (String face : faces.keySet()) {
                JsonObject faceObject = object(faces, face, context + " face " + face);
                String texture = string(faceObject, "texture", context + " face texture");
                assertTrue(allowedFaceTextures.contains(texture),
                        context + " trophy face should use a shared material slot");
                if (TROPHY_TIER_DETAIL_ELEMENTS.contains(name)) {
                    assertEquals("#gem", texture,
                            context + " front inlay/rib should use the tier's matching detail color");
                }
            }
        }
    }

    @Test
    void achievementAwardingUsesTheDeclaredTrophyItemInsteadOfRarityFallback() throws IOException {
        String modItems = readJava("logic/ModItems.java");
        String manager = readJava("logic/AchievementManager.java");

        for (String trophyItemId : TROPHY_ITEM_IDS) {
            assertTrue(modItems.contains("case \"" + trophyItemId + "\""),
                    "ModItems should resolve " + trophyItemId + " explicitly");
        }

        assertTrue(manager.contains("ModItems.getTrophyForItemId(achievement.getTrophyItemId())"),
                "AchievementManager should award the exact trophy declared by the achievement");
        assertFalse(manager.contains("getTrophyForRarity(achievement.getRarity())"),
                "AchievementManager should not guess trophy items from rarity");
    }

    @Test
    void trophyTooltipContractStoresAwardTimeAndModelVariant() throws IOException {
        String trophyItem = readJava("item/TrophyItem.java");
        String registry = readJava("logic/AchievementRegistry.java");

        assertTrue(trophyItem.contains("public static final String TAG_TIMESTAMP = \"Timestamp\""),
                "trophy stacks should have a stable timestamp tag");
        assertTrue(trophyItem.contains("tag.putLong(TAG_TIMESTAMP, timestamp)"),
                "created trophy stacks should store the award timestamp");
        assertTrue(trophyItem.contains("tag.getLong(TAG_TIMESTAMP)"),
                "trophy tooltip path should read the stored award timestamp");
        assertTrue(trophyItem.contains("DataComponents.CUSTOM_MODEL_DATA"),
                "created trophy stacks should store the achievement-specific model selector");
        assertTrue(trophyItem.contains("new CustomModelData(achievement.getTrophyModelData())"),
                "created trophy stacks should use the achievement-specific custom model data value");
        assertTrue(trophyItem.contains("achievement.getTrophyTooltip("),
                "trophy item tooltip should delegate to achievement-specific descriptions");
        assertTrue(trophyItem.contains("timestamp"),
                "trophy tooltip should pass the stored timestamp into the description path");

        assertTrue(registry.contains("\"algocraft.trophy.awarded_on\""),
                "achievement trophy tooltip should use the obtained-time translation key");
        assertTrue(registry.contains("formatTimestamp(timestamp)"),
                "achievement trophy tooltip should format the exact award timestamp");
        assertTrue(registry.contains("DateTimeFormatter.ofPattern(\"yyyy-MM-dd HH:mm\")"),
                "award time should include both date and minute-level time");
    }

    @Test
    void trophyItemsHaveDistinctFallbackNamesAndVisualEffects() throws IOException {
        String trophyItem = readJava("item/TrophyItem.java");

        for (String trophyItemId : TROPHY_ITEM_IDS) {
            assertTrue(trophyItem.contains("\"" + trophyItemId + "\""),
                    "TrophyTier should know its item id " + trophyItemId);
            assertTrue(Files.isRegularFile(ITEM_MODELS.resolve(trophyItemId + ".json")),
                    "missing trophy item model " + trophyItemId);
        }

        assertTrue(trophyItem.contains("return \"item.algocraft.\" + itemId"),
                "fallback trophy names should use real item translation keys");
        assertFalse(trophyItem.contains("\"item.algocraft.trophy.\""),
                "old fallback trophy translation key shape should not remain");
        assertTrue(trophyItem.contains("achievement.getRarity() == AchievementRegistry.Rarity.LEGENDARY"),
                "legendary achievement trophies should have glint");
        assertTrue(trophyItem.contains("achievement.getRarity() == AchievementRegistry.Rarity.MYTHIC"),
                "mythic achievement trophies should have glint");
        assertTrue(trophyItem.contains("tier == TrophyTier.DIAMOND || tier == TrophyTier.NETHERITE"),
                "unbound high-tier trophy items should still have a visual glint");
    }

    @Test
    void achievementRaritiesHaveTieredSoundEffects() throws IOException {
        String manager = readJava("logic/AchievementManager.java");

        assertTrue(manager.contains("case COMMON, UNCOMMON -> SoundEvents.PLAYER_LEVELUP"),
                "common and uncommon achievements should use the level-up sound");
        assertTrue(manager.contains("case RARE -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE"),
                "rare achievements should use the challenge toast sound");
        assertTrue(manager.contains("case EPIC -> SoundEvents.ENDER_DRAGON_GROWL"),
                "epic achievements should use a stronger sound");
        assertTrue(manager.contains("case LEGENDARY -> SoundEvents.END_PORTAL_SPAWN"),
                "legendary achievements should use an end-portal sound");
        assertTrue(manager.contains("case MYTHIC -> SoundEvents.TOTEM_USE"),
                "mythic achievements should use a top-tier sound");
        assertTrue(manager.contains("rarity.ordinal() >= Rarity.EPIC.ordinal() ? 1.0f : 0.7f"),
                "high-rarity achievements should have stronger volume");
        assertTrue(manager.contains("1.0f + (rarity.ordinal() * 0.1f)"),
                "rarer achievements should have higher pitch feedback");
    }

    private static List<AchievementSpec> achievements(String registry) {
        Map<String, String> constants = new HashMap<>();
        Matcher constantMatcher = CONSTANT_PATTERN.matcher(registry);
        while (constantMatcher.find()) {
            constants.put(constantMatcher.group(1), constantMatcher.group(2));
        }

        Matcher matcher = REGISTER_PATTERN.matcher(registry);
        List<AchievementSpec> achievements = new ArrayList<>();
        while (matcher.find()) {
            String constant = matcher.group(1);
            achievements.add(new AchievementSpec(
                    constant,
                    constants.get(constant),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    Integer.parseInt(matcher.group(5))
            ));
        }
        return achievements;
    }

    private static JsonObject readObject(Path path) throws IOException {
        JsonElement element = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
        assertTrue(element.isJsonObject(), path + " should be a JSON object");
        return element.getAsJsonObject();
    }

    private static JsonObject object(JsonObject object, String field, String context) {
        assertTrue(object.has(field) && object.get(field).isJsonObject(), context + " should have object field " + field);
        return object.getAsJsonObject(field);
    }

    private static JsonArray array(JsonObject object, String field, String context) {
        assertTrue(object.has(field) && object.get(field).isJsonArray(), context + " should have array field " + field);
        return object.getAsJsonArray(field);
    }

    private static String string(JsonObject object, String field, String context) {
        assertTrue(object.has(field) && object.get(field).isJsonPrimitive(), context + " should have field " + field);
        return object.get(field).getAsString();
    }

    private static void assertModelCoordinatesAreInVanillaBounds(JsonObject object, String context) {
        for (String field : List.of("from", "to")) {
            JsonArray coordinates = array(object, field, context);
            assertEquals(3, coordinates.size(), context + " coordinate array should have three values: " + field);
            for (JsonElement value : coordinates) {
                double coordinate = value.getAsDouble();
                assertTrue(coordinate >= -16.0 && coordinate <= 32.0,
                        context + " coordinate should stay inside vanilla model bounds: " + coordinate);
            }
        }
    }

    private static void assertModelRotationIsVanillaCompatible(JsonObject object) {
        if (!object.has("rotation")) {
            return;
        }
        JsonObject rotation = object(object, "rotation", "model element rotation");
        assertTrue(Set.of("x", "y", "z").contains(string(rotation, "axis", "model element rotation")),
                "model rotation axis should be vanilla-compatible");
        double angle = rotation.get("angle").getAsDouble();
        assertTrue(Set.of(-45.0, -22.5, 0.0, 22.5, 45.0).contains(angle),
                "model rotation angle should be one of the vanilla-supported angles: " + angle);
        JsonArray origin = array(rotation, "origin", "model element rotation");
        assertEquals(3, origin.size(), "model rotation origin should have three values");
    }

    private static String readJava(String path) throws IOException {
        return Files.readString(JAVA.resolve(path), StandardCharsets.UTF_8);
    }

    private record AchievementSpec(
            String constant,
            String id,
            String type,
            String rarity,
            String trophyItemId,
            int modelData
    ) {
    }
}
