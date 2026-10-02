package com.crabmods.algocraft.assets;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgorithmComputerAssetTest {
    private static final Path ASSETS = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "assets", "algocraft");
    private static final Set<String> TROPHY_TIER_DETAIL_ELEMENTS = Set.of(
            "front_black_split_lower",
            "front_black_split_upper",
            "left_leaf_rib",
            "right_leaf_rib"
    );

    @Test
    void algorithmComputerBlockstateModelAndTexturesAreWired() throws IOException {
        JsonObject blockstate = readObject(ASSETS.resolve("blockstates").resolve("algorithm_computer.json"));
        JsonObject variants = object(blockstate, "variants", "blockstate");

        Map<String, Integer> expectedRotations = Map.of(
                "facing=north", 0,
                "facing=east", 90,
                "facing=south", 180,
                "facing=west", 270
        );

        assertEquals(expectedRotations.keySet(), variants.keySet(), "computer blockstate should expose all horizontal facings");
        for (Map.Entry<String, Integer> entry : expectedRotations.entrySet()) {
            JsonObject variant = object(variants, entry.getKey(), "blockstate variants");
            assertEquals("algocraft:block/algorithm_computer", string(variant, "model", entry.getKey()));
            if (entry.getValue() == 0) {
                assertTrue(!variant.has("y"), "north-facing model should not need rotation");
            } else {
                assertEquals(entry.getValue(), variant.get("y").getAsInt(), entry.getKey() + " y rotation");
            }
        }

        JsonObject model = readObject(ASSETS.resolve("models").resolve("block").resolve("algorithm_computer.json"));
        Set<String> elementNames = assertComputerModelElementsDoNotOverlap(model);
        assertTrue(model.getAsJsonArray("elements").size() >= 40,
                "computer model should use real cuboid details instead of texture-painted details");
        assertTrue(model.getAsJsonArray("elements").size() <= 64,
                "computer model should stay compact enough for a vanilla block model");

        JsonObject textures = object(model, "textures", "computer model");
        Set<String> requiredTextures = Set.of(
                "body", "trim", "dark", "panel", "metal", "vent", "screen",
                "screen_line", "screen_muted", "key", "key_label", "accent", "particle"
        );
        assertTrue(textures.keySet().containsAll(requiredTextures), "computer model should define all required texture slots");

        Map<String, String> expectedTextures = Map.ofEntries(
                Map.entry("body", "minecraft:block/gray_concrete"),
                Map.entry("trim", "minecraft:block/light_gray_concrete"),
                Map.entry("dark", "minecraft:block/black_concrete"),
                Map.entry("panel", "minecraft:block/black_wool"),
                Map.entry("metal", "minecraft:block/iron_block"),
                Map.entry("vent", "minecraft:block/deepslate_tiles"),
                Map.entry("screen", "minecraft:block/black_stained_glass"),
                Map.entry("screen_line", "minecraft:block/lime_concrete"),
                Map.entry("screen_muted", "minecraft:block/light_blue_concrete"),
                Map.entry("key", "minecraft:block/light_gray_concrete"),
                Map.entry("key_label", "minecraft:block/yellow_concrete"),
                Map.entry("accent", "minecraft:block/lime_concrete"),
                Map.entry("particle", "minecraft:block/gray_concrete")
        );

        for (String textureSlot : requiredTextures) {
            String texture = string(textures, textureSlot, "computer model textures");
            assertEquals(expectedTextures.get(textureSlot), texture, "computer " + textureSlot + " texture");
            assertTrue(texture.startsWith("minecraft:block/"),
                    "computer model should use vanilla block textures, not generated custom textures: " + texture);
            assertFalse(texture.startsWith("algocraft:block/"),
                    "computer model should not depend on old generated computer texture files: " + texture);
        }
        assertComputerFacesUseOnlyTextureSlots(model, requiredTextures);
        for (String requiredElement : Set.of(
                "monitor_screen_panel",
                "screen_code_line_1",
                "screen_cursor",
                "computer_vent_top",
                "computer_power_button",
                "keyboard_key_row_1",
                "keyboard_spacebar",
                "mouse_scroll_wheel"
        )) {
            assertTrue(elementNames.contains(requiredElement),
                    "computer model should express detail as geometry: " + requiredElement);
        }

        JsonObject itemModel = readObject(ASSETS.resolve("models").resolve("item").resolve("algorithm_computer.json"));
        assertEquals("algocraft:block/algorithm_computer", string(itemModel, "parent", "computer item model"));
    }

    @Test
    void trophyItemsUseProgressiveMinecraftStyleModelsAndVanillaMaterials() throws IOException {
        Set<String> requiredTextureSlots = Set.of("base", "shadow", "metal", "accent", "gem", "particle");
        Set<String> allowedFaceTextures = Set.of("#base", "#shadow", "#metal", "#accent", "#gem");
        List<String> tierOrder = List.of("bronze_trophy", "silver_trophy", "gold_trophy", "diamond_trophy", "netherite_trophy");
        Map<String, String> expectedShapes = Map.of(
                "bronze_trophy", "trophy_bronze_shape",
                "silver_trophy", "trophy_silver_shape",
                "gold_trophy", "trophy_gold_shape",
                "diamond_trophy", "trophy_diamond_shape",
                "netherite_trophy", "trophy_netherite_shape"
        );
        Map<String, String> expectedMetals = Map.of(
                "bronze_trophy", "minecraft:block/copper_block",
                "silver_trophy", "minecraft:block/iron_block",
                "gold_trophy", "minecraft:block/gold_block",
                "diamond_trophy", "minecraft:block/diamond_block",
                "netherite_trophy", "minecraft:block/netherite_block"
        );
        Map<String, String> expectedAccents = Map.of(
                "bronze_trophy", "minecraft:block/orange_concrete",
                "silver_trophy", "minecraft:block/light_gray_concrete",
                "gold_trophy", "minecraft:block/yellow_concrete",
                "diamond_trophy", "minecraft:block/cyan_concrete",
                "netherite_trophy", "minecraft:block/gray_concrete"
        );
        Map<String, String> expectedGems = Map.of(
                "bronze_trophy", "minecraft:block/brown_concrete",
                "silver_trophy", "minecraft:block/white_concrete",
                "gold_trophy", "minecraft:block/orange_concrete",
                "diamond_trophy", "minecraft:block/light_blue_concrete",
                "netherite_trophy", "minecraft:block/purple_concrete"
        );

        int previousElementCount = 0;
        for (String itemName : tierOrder) {
            String shapeName = expectedShapes.get(itemName);
            JsonObject shapeModel = readObject(ASSETS.resolve("models").resolve("item").resolve(shapeName + ".json"));
            assertTrophyShapeModel(shapeModel, shapeName, requiredTextureSlots, allowedFaceTextures);
            JsonObject shapeTextures = object(shapeModel, "textures", shapeName + " textures");
            assertEquals(expectedMetals.get(itemName), string(shapeTextures, "metal", shapeName + " fallback metal texture"));
            assertEquals(expectedAccents.get(itemName), string(shapeTextures, "accent", shapeName + " fallback accent texture"));
            assertEquals(expectedGems.get(itemName), string(shapeTextures, "gem", shapeName + " fallback gem texture"));
            assertEquals("minecraft:block/polished_blackstone", string(shapeTextures, "base", shapeName + " fallback base texture"));
            assertEquals("minecraft:block/blackstone", string(shapeTextures, "shadow", shapeName + " fallback shadow texture"));

            int elementCount = shapeModel.getAsJsonArray("elements").size();
            assertTrue(elementCount > previousElementCount,
                    shapeName + " should add visible parts compared with the previous trophy tier");
            assertTrue(elementCount >= 15, shapeName + " should preserve a sculptural trophy silhouette");
            assertTrue(elementCount <= 64, shapeName + " should stay compact enough for a Minecraft item model");
            previousElementCount = elementCount;

            JsonObject itemModel = readObject(ASSETS.resolve("models").resolve("item").resolve(itemName + ".json"));
            assertEquals("algocraft:item/" + shapeName, string(itemModel, "parent", itemName + " item model"));
            assertFalse(itemModel.has("elements"), itemName + " should inherit tier-specific shape geometry");

            JsonObject textures = object(itemModel, "textures", itemName + " textures");
            assertTrue(textures.keySet().containsAll(requiredTextureSlots), itemName + " should override all material slots");
            assertEquals(expectedMetals.get(itemName), string(textures, "metal", itemName + " metal texture"));
            assertEquals(expectedAccents.get(itemName), string(textures, "accent", itemName + " accent texture"));
            assertEquals(expectedGems.get(itemName), string(textures, "gem", itemName + " gem texture"));
            assertEquals("minecraft:block/polished_blackstone", string(textures, "base", itemName + " base texture"));
            assertEquals("minecraft:block/blackstone", string(textures, "shadow", itemName + " shadow texture"));

            for (String slot : requiredTextureSlots) {
                String texture = string(textures, slot, itemName + " texture slot " + slot);
                assertTrue(texture.startsWith("minecraft:block/"),
                        itemName + " should use vanilla block textures, not generated item placeholders: " + texture);
                assertFalse(texture.contains("ingot"),
                        itemName + " should not fall back to old ingot placeholder textures: " + texture);
            }
        }
    }

    private static void assertTrophyShapeModel(
            JsonObject model,
            String context,
            Set<String> requiredTextureSlots,
            Set<String> allowedFaceTextures
    ) {
        assertEquals("minecraft:block/block", string(model, "parent", context));
        assertTrue(model.has("elements") && model.get("elements").isJsonArray(),
                context + " should define real cuboid geometry instead of a generated item icon");
        assertTrue(model.has("display") && model.get("display").isJsonObject(),
                context + " should define item display transforms");

        JsonObject textures = object(model, "textures", context + " textures");
        assertTrue(textures.keySet().containsAll(requiredTextureSlots),
                context + " should expose all tier material slots");

        Set<String> seenNames = new java.util.HashSet<>();
        for (JsonElement element : model.getAsJsonArray("elements")) {
            assertTrue(element.isJsonObject(), context + " trophy element should be an object");
            JsonObject object = element.getAsJsonObject();
            String name = string(object, "name", context + " trophy element");
            assertTrue(seenNames.add(name), context + " should not reuse element name " + name);
            assertModelCoordinatesAreInVanillaBounds(object, context + " trophy element " + name);
            assertModelRotationIsVanillaCompatible(object);

            JsonObject faces = object(object, "faces", context + " trophy element faces");
            assertEquals(Set.of("down", "up", "north", "south", "west", "east"), faces.keySet(),
                    context + " each trophy cuboid should render all faces");
            for (String face : faces.keySet()) {
                JsonObject faceObject = object(faces, face, context + " trophy face " + face);
                String texture = string(faceObject, "texture", context + " trophy face texture");
                assertTrue(allowedFaceTextures.contains(texture),
                        context + " trophy face should use one of the shared material slots");
                if (TROPHY_TIER_DETAIL_ELEMENTS.contains(name)) {
                    assertEquals("#gem", texture,
                            context + " front inlay/rib should use the tier's matching detail color");
                }
            }
        }
    }

    private static Set<String> assertComputerModelElementsDoNotOverlap(JsonObject model) {
        assertTrue(model.has("elements") && model.get("elements").isJsonArray(),
                "computer model should define geometry elements");

        List<ModelBox> boxes = new ArrayList<>();
        Set<String> seenNames = new java.util.HashSet<>();
        for (JsonElement element : model.getAsJsonArray("elements")) {
            assertTrue(element.isJsonObject(), "computer model element should be an object");
            JsonObject object = element.getAsJsonObject();
            String name = string(object, "name", "computer model element");
            assertTrue(seenNames.add(name), "computer model should not reuse element name " + name);
            boxes.add(new ModelBox(
                    name,
                    coordinate(object, "from", 0),
                    coordinate(object, "from", 1),
                    coordinate(object, "from", 2),
                    coordinate(object, "to", 0),
                    coordinate(object, "to", 1),
                    coordinate(object, "to", 2)
            ));
        }

        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                ModelBox left = boxes.get(i);
                ModelBox right = boxes.get(j);
                assertFalse(left.overlaps(right),
                        "model elements should not overlap: " + left.name() + " intersects " + right.name());
            }
        }

        return seenNames;
    }

    private static void assertComputerFacesUseOnlyTextureSlots(JsonObject model, Set<String> allowedSlots) {
        for (JsonElement element : model.getAsJsonArray("elements")) {
            JsonObject object = element.getAsJsonObject();
            JsonObject faces = object(object, "faces", "computer model element faces");
            for (String face : faces.keySet()) {
                JsonObject faceObject = object(faces, face, "computer model face");
                String texture = string(faceObject, "texture", "computer model face texture");
                assertTrue(texture.startsWith("#"), "computer face should reference a texture slot: " + texture);
                assertTrue(allowedSlots.contains(texture.substring(1)),
                        "computer face should use a declared vanilla material slot: " + texture);
            }
        }
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

    private static String string(JsonObject object, String field, String context) {
        assertTrue(object.has(field) && object.get(field).isJsonPrimitive(), context + " should have field " + field);
        return object.get(field).getAsString();
    }

    private static double coordinate(JsonObject object, String field, int index) {
        assertTrue(object.has(field) && object.get(field).isJsonArray(),
                "computer model element should have coordinate array " + field);
        assertTrue(object.getAsJsonArray(field).size() == 3,
                "computer model element coordinate array should have three values: " + field);
        return object.getAsJsonArray(field).get(index).getAsDouble();
    }

    private static void assertModelCoordinatesAreInVanillaBounds(JsonObject object, String context) {
        for (String field : List.of("from", "to")) {
            assertTrue(object.has(field) && object.get(field).isJsonArray(),
                    context + " should have coordinate array " + field);
            assertEquals(3, object.getAsJsonArray(field).size(),
                    context + " coordinate array should have three values: " + field);
            for (JsonElement value : object.getAsJsonArray(field)) {
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
        assertTrue(rotation.has("origin") && rotation.get("origin").isJsonArray(),
                "model rotation should define an origin");
        assertEquals(3, rotation.getAsJsonArray("origin").size(),
                "model rotation origin should have three values");
    }

    private record ModelBox(String name, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private boolean overlaps(ModelBox other) {
            return minX < other.maxX && maxX > other.minX
                    && minY < other.maxY && maxY > other.minY
                    && minZ < other.maxZ && maxZ > other.minZ;
        }
    }
}
