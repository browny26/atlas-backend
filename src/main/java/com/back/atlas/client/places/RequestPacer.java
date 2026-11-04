package com.back.atlas.client.places;

import java.time.Duration;

/**
 * Spaces calls to a service at least {@code interval} apart, across all threads of this
 * instance (callers wait their turn). Enough for one backend instance; several replicas
 * would need a shared limiter, e.g. in Redis.
 */
final class RequestPacer {

    private final long intervalNanos;
    private long nextSlot = System.nanoTime();

    RequestPacer(Duration interval) {
        this.intervalNanos = interval.toNanos();
    }

    void awaitTurn() throws InterruptedException {
        long slot;
        synchronized (this) {
            slot = Math.max(System.nanoTime(), nextSlot);
            nextSlot = slot + intervalNanos;
        }
        long wait = slot - System.nanoTime();
        if (wait > 0) {
            Thread.sleep(Duration.ofNanos(wait));
        }
    }
}
