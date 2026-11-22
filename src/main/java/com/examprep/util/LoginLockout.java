package com.examprep.util;

import com.examprep.config.AppConfig;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory failed-login lockout keyed by normalized username.
 */
public final class LoginLockout {

    private final ConcurrentMap<String, FailureWindow> failures = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final long windowMillis;

    public LoginLockout() {
        this(AppConfig.getInt("login.lockout.max.failures", 5),
                AppConfig.getInt("login.lockout.window.minutes", 15) * 60_000L);
    }

    public LoginLockout(int maxFailures, long windowMillis) {
        this.maxFailures = Math.max(1, maxFailures);
        this.windowMillis = Math.max(1_000L, windowMillis);
    }

    public boolean isLocked(String username) {
        FailureWindow window = failures.get(normalize(username));
        return window != null && window.isLocked(System.currentTimeMillis(), maxFailures, windowMillis);
    }

    public void recordFailure(String username) {
        String key = normalize(username);
        failures.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || existing.isExpired(now, windowMillis)) {
                return new FailureWindow(now, 1);
            }
            return existing.increment(now);
        });
    }

    public void recordSuccess(String username) {
        failures.remove(normalize(username));
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class FailureWindow {
        private final long firstFailureAt;
        private final int count;

        private FailureWindow(long firstFailureAt, int count) {
            this.firstFailureAt = firstFailureAt;
            this.count = count;
        }

        private FailureWindow increment(long now) {
            return new FailureWindow(firstFailureAt, count + 1);
        }

        private boolean isExpired(long now, long windowMillis) {
            return now - firstFailureAt > windowMillis;
        }

        private boolean isLocked(long now, int maxFailures, long windowMillis) {
            return !isExpired(now, windowMillis) && count >= maxFailures;
        }
    }
}
