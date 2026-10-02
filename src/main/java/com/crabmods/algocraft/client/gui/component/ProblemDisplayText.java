package com.crabmods.algocraft.client.gui.component;

import com.crabmods.algocraft.logic.Problem;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;

public final class ProblemDisplayText {
    private ProblemDisplayText() {
    }

    public static String currentLanguageCode() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getLanguageManager() == null) {
                return "en_us";
            }
            String selected = minecraft.getLanguageManager().getSelected();
            return normalizeLanguageCode(selected);
        } catch (RuntimeException e) {
            return "en_us";
        }
    }

    public static String title(Problem problem) {
        return title(problem, currentLanguageCode());
    }

    public static String title(Problem problem, String lang) {
        return problem == null ? "" : problem.getTitle(normalizeLanguageCode(lang));
    }

    public static String description(Problem problem) {
        return description(problem, currentLanguageCode());
    }

    public static String description(Problem problem, String lang) {
        return problem == null ? "" : problem.getDescription(normalizeLanguageCode(lang));
    }

    public static List<Problem.Visual> visuals(Problem problem) {
        return visuals(problem, currentLanguageCode());
    }

    public static List<Problem.Visual> visuals(Problem problem, String lang) {
        return problem == null ? List.of() : problem.getVisuals(normalizeLanguageCode(lang));
    }

    static String normalizeLanguageCode(String lang) {
        if (lang == null || lang.isBlank()) {
            return "en_us";
        }
        return lang.toLowerCase(Locale.ROOT);
    }
}
