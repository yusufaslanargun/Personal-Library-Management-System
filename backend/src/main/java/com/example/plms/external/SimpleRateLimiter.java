package com.example.plms.external;

public class SimpleRateLimiter {
    private final long minIntervalMs;
    private long lastRequestTime;

    public SimpleRateLimiter(long minIntervalMs) {
        this.minIntervalMs = minIntervalMs;
    }

    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long wait = lastRequestTime + minIntervalMs - now;
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
