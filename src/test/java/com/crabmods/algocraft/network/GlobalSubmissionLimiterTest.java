package com.crabmods.algocraft.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSubmissionLimiterTest {
    @Test
    void capsTotalInFlightSubmissions() {
        GlobalSubmissionLimiter limiter = new GlobalSubmissionLimiter(2);

        assertTrue(limiter.tryAcquire());
        assertEquals(1, limiter.inFlightCount());
        assertTrue(limiter.tryAcquire());
        assertEquals(2, limiter.inFlightCount());
        assertFalse(limiter.tryAcquire(), "global judge capacity should reject the third concurrent submission");

        limiter.release();
        assertEquals(1, limiter.inFlightCount());
        assertTrue(limiter.tryAcquire(), "releasing one slot should admit a new submission");
        assertEquals(2, limiter.inFlightCount());
    }

    @Test
    void extraReleaseDoesNotMakeCapacityNegative() {
        GlobalSubmissionLimiter limiter = new GlobalSubmissionLimiter(1);

        limiter.release();
        assertEquals(0, limiter.inFlightCount());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new GlobalSubmissionLimiter(0));
    }
}
