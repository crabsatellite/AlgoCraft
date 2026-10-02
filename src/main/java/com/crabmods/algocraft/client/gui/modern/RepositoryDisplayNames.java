package com.crabmods.algocraft.client.gui.modern;

import com.crabmods.algocraft.logic.repo.ProblemRepository;
import net.minecraft.network.chat.Component;

final class RepositoryDisplayNames {
    private RepositoryDisplayNames() {
    }

    static Component componentFor(ProblemRepository repository) {
        if (repository == null) {
            return Component.translatable("algocraft.gui.repository.unknown");
        }

        String name = repository.getName();
        if (name.startsWith("Server · ")) return Component.translatable("algocraft.gui.repository.server", name.substring(9));
        if (name.startsWith("Practice · ")) return Component.translatable("algocraft.gui.repository.practice", name.substring(11));
        return switch (name) {
            case "Official" -> Component.translatable("algocraft.gui.repository.official");
            case "User" -> Component.translatable("algocraft.gui.repository.user");
            case "Built-in" -> Component.translatable("algocraft.gui.repository.builtin");
            default -> Component.literal(name == null ? "" : name);
        };
    }
}
