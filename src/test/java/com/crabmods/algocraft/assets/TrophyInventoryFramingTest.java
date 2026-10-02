package com.crabmods.algocraft.assets;

import com.google.gson.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrophyInventoryFramingTest {
    @Test void everyTrophyAndAchievementVariantFitsInsideAnInventorySlot() throws Exception {
        Path root = Path.of("src/main/resources/assets/algocraft/models/item");
        List<Path> models = new ArrayList<>();
        try (var paths = Files.list(root)) { models.addAll(paths.filter(p -> p.getFileName().toString().matches("trophy_.*_shape\\.json")).toList()); }
        try (var paths = Files.list(root.resolve("trophy_variants"))) { models.addAll(paths.filter(p -> p.toString().endsWith(".json")).toList()); }
        assertEquals(28, models.size());
        for (Path path : models) {
            JsonObject model = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject gui = model.getAsJsonObject("display").getAsJsonObject("gui");
            Vector3f angles = vector(gui.getAsJsonArray("rotation")).mul((float)Math.PI / 180);
            Quaternionf rotation = new Quaternionf().rotationXYZ(angles.x, angles.y, angles.z);
            Vector3f scale = vector(gui.getAsJsonArray("scale"));
            Vector3f translation = vector(gui.getAsJsonArray("translation"));
            for (JsonElement entry : model.getAsJsonArray("elements")) {
                JsonObject element = entry.getAsJsonObject();
                Vector3f from = vector(element.getAsJsonArray("from")), to = vector(element.getAsJsonArray("to"));
                for (int mask = 0; mask < 8; mask++) {
                    Vector3f v = new Vector3f((mask & 1) == 0 ? from.x : to.x, (mask & 2) == 0 ? from.y : to.y, (mask & 4) == 0 ? from.z : to.z);
                    if (element.has("rotation")) {
                        JsonObject local = element.getAsJsonObject("rotation");
                        Vector3f origin = vector(local.getAsJsonArray("origin"));
                        String axisName = local.get("axis").getAsString();
                        Vector3f axis = new Vector3f(axisName.equals("x") ? 1 : 0, axisName.equals("y") ? 1 : 0, axisName.equals("z") ? 1 : 0);
                        float angle = (float)Math.toRadians(local.get("angle").getAsDouble());
                        v.sub(origin).rotate(new Quaternionf().rotationAxis(angle, axis));
                        if (local.has("rescale") && local.get("rescale").getAsBoolean()) {
                            float factor = 1F / (float)Math.cos(angle);
                            v.mul(axis.x == 0 ? factor : 1, axis.y == 0 ? factor : 1, axis.z == 0 ? factor : 1);
                        }
                        v.add(origin);
                    }
                    v.sub(8, 8, 8).mul(scale).rotate(rotation).add(translation);
                    assertTrue(Math.abs(v.x) <= 7 && Math.abs(v.y) <= 7, path.getFileName()+" projects outside its 14-pixel icon area: "+v);
                }
            }
        }
    }
    private static Vector3f vector(JsonArray values) { return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat()); }
}
