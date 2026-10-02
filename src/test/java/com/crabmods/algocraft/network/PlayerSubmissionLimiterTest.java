package com.crabmods.algocraft.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSubmissionLimiterTest {
    @Test
    void allowsOnlyOneInFlightSubmissionPerPlayer() {
        PlayerSubmissionLimiter limiter = new PlayerSubmissionLimiter(1);
        UUID playerId = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(playerId));
        assertEquals(1, limiter.inFlightCount(playerId));
        try {
            assertFalse(limiter.tryAcquire(playerId),
                    "a second submission for the same player must be rejected while the first is running");
        } finally {
            limiter.release(playerId);
        }
        assertEquals(0, limiter.inFlightCount(playerId));

        assertTrue(limiter.tryAcquire(playerId),
                "the player should be able to submit again after the running judge releases");
        limiter.release(playerId);
        assertEquals(0, limiter.inFlightCount(playerId));
    }

    @Test
    void limiterIsScopedPerPlayer() {
        PlayerSubmissionLimiter limiter = new PlayerSubmissionLimiter(1);
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(firstPlayer));
        try {
            assertTrue(limiter.tryAcquire(secondPlayer),
                    "one player's running judge must not block another player");
        } finally {
            limiter.release(firstPlayer);
            limiter.release(secondPlayer);
        }
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerSubmissionLimiter(0));
    }
}
