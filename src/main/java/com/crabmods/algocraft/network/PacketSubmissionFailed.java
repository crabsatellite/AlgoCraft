package com.crabmods.algocraft.network;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Internal server-side service used when a judged submission fails.
 * This resets the consecutive correct counter.
 */
public final class PacketSubmissionFailed {
    private PacketSubmissionFailed() {
    }

    public static void applySubmissionFailure(ServerPlayer player, String problemId) {
        ServerLevel level = player.serverLevel();
        AlgoCraftSavedData data = AlgoCraftSavedData.get(level);

        // Reset consecutive correct count on failure
        data.resetConsecutiveCorrect(player.getUUID());

        AlgoCraft.LOGGER.debug("Player {} failed submission for problem {}, consecutive correct reset",
            player.getName().getString(), problemId);
    }
}
