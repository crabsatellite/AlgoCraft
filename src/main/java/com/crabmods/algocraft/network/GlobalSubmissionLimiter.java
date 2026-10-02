package com.crabmods.algocraft.network;

import java.util.concurrent.atomic.AtomicInteger;

final class GlobalSubmissionLimiter {
    private final int maxInFlight;
    private final AtomicInteger inFlight = new AtomicInteger();

    GlobalSubmissionLimiter(int maxInFlight) {
        if (maxInFlight < 1) {
            throw new IllegalArgumentException("maxInFlight must be at least 1");
        }
        this.maxInFlight = maxInFlight;
    }

    boolean tryAcquire() {
        while (true) {
            int current = inFlight.get();
            if (current >= maxInFlight) {
                return false;
            }
            if (inFlight.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    void release() {
        inFlight.updateAndGet(count -> Math.max(0, count - 1));
    }

    int inFlightCount() {
        return inFlight.get();
    }
}
