package com.crabmods.algocraft.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class PlayerSubmissionLimiter {
    private final int maxInFlightPerPlayer;
    private final Map<UUID, Integer> inFlightByPlayer = new ConcurrentHashMap<>();

    PlayerSubmissionLimiter(int maxInFlightPerPlayer) {
        if (maxInFlightPerPlayer < 1) {
            throw new IllegalArgumentException("maxInFlightPerPlayer must be at least 1");
        }
        this.maxInFlightPerPlayer = maxInFlightPerPlayer;
    }

    boolean tryAcquire(UUID playerId) {
        AtomicBoolean acquired = new AtomicBoolean(false);
        inFlightByPlayer.compute(playerId, (uuid, count) -> {
            int current = count == null ? 0 : count;
            if (current >= maxInFlightPerPlayer) {
                return count;
            }
            acquired.set(true);
            return current + 1;
        });
        return acquired.get();
    }

    void release(UUID playerId) {
        inFlightByPlayer.computeIfPresent(playerId, (uuid, count) -> count <= 1 ? null : count - 1);
    }

    int inFlightCount(UUID playerId) {
        return inFlightByPlayer.getOrDefault(playerId, 0);
    }
}
