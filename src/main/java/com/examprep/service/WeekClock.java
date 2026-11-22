package com.examprep.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Week 1 starts at diagnostic completion. Total weeks is floor(days until grant expiry / 7),
 * at least 1. The last week ends at grant expiry (mixed readiness exam).
 */
public final class WeekClock {

    private WeekClock() {
    }

    public static int totalWeeks(LocalDateTime diagnosticCompletedAt, LocalDateTime expiresAt) {
        if (diagnosticCompletedAt == null || expiresAt == null || !expiresAt.isAfter(diagnosticCompletedAt)) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(diagnosticCompletedAt, expiresAt);
        return Math.max(1, (int) (days / 7));
    }

    public static int weekNumber(LocalDateTime diagnosticCompletedAt, LocalDateTime expiresAt, LocalDateTime now) {
        int total = totalWeeks(diagnosticCompletedAt, expiresAt);
        if (now == null || diagnosticCompletedAt == null || now.isBefore(diagnosticCompletedAt)) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(diagnosticCompletedAt, now);
        int week = (int) (days / 7) + 1;
        return Math.min(total, Math.max(1, week));
    }

    public static LocalDateTime weekStart(LocalDateTime diagnosticCompletedAt, int weekNumber) {
        return diagnosticCompletedAt.plusDays((long) (weekNumber - 1) * 7);
    }

    public static LocalDateTime weekEnd(LocalDateTime diagnosticCompletedAt, LocalDateTime expiresAt, int weekNumber) {
        int total = totalWeeks(diagnosticCompletedAt, expiresAt);
        if (weekNumber >= total) {
            return expiresAt;
        }
        return weekStart(diagnosticCompletedAt, weekNumber).plusDays(7);
    }

    public static boolean isFinalWeek(LocalDateTime diagnosticCompletedAt, LocalDateTime expiresAt, int weekNumber) {
        return weekNumber >= totalWeeks(diagnosticCompletedAt, expiresAt);
    }
}
