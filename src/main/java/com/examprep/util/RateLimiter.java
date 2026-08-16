package com.examprep.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple sliding window rate limiter.
 * Tracks requests per key (typically IP address) and enforces rate limits.
 */
public final class RateLimiter {

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public RateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * Attempts to acquire a token for the given key.
     * 
     * @param key the rate limit key (typically IP address)
     * @return true if the request is allowed, false if rate limited
     */
    public boolean tryAcquire(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxRequests, windowMillis));
        return bucket.tryConsume();
    }

    /**
     * Gets the number of remaining requests for the given key.
     * 
     * @param key the rate limit key
     * @return the number of remaining requests, or -1 if key not found
     */
    public int getRemainingRequests(String key) {
        TokenBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getRemaining() : maxRequests;
    }

    /**
     * Cleans up expired buckets to prevent memory leaks.
     * Should be called periodically.
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private static class TokenBucket {
        private final int capacity;
        private final long windowMillis;
        private final AtomicLong lastRefillTime;
        private final AtomicLong tokens;

        TokenBucket(int capacity, long windowMillis) {
            this.capacity = capacity;
            this.windowMillis = windowMillis;
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.tokens = new AtomicLong(capacity);
        }

        boolean tryConsume() {
            refill();
            long current = tokens.get();
            while (current > 0) {
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
                current = tokens.get();
            }
            return false;
        }

        int getRemaining() {
            refill();
            return (int) Math.max(0, tokens.get());
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            long elapsed = now - lastRefill;

            if (elapsed > windowMillis) {
                if (lastRefillTime.compareAndSet(lastRefill, now)) {
                    tokens.set(capacity);
                }
            }
        }

        boolean isExpired(long now) {
            return (now - lastRefillTime.get()) > (windowMillis * 2);
        }
    }
}
