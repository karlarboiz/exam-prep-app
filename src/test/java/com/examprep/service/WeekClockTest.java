package com.examprep.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeekClockTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Test
    void totalWeeksIsFloorOfDaysOverSeven() {
        assertEquals(4, WeekClock.totalWeeks(start, start.plusDays(30)));
        assertEquals(1, WeekClock.totalWeeks(start, start.plusDays(3)));
        assertEquals(1, WeekClock.totalWeeks(start, start.plusHours(2)));
    }

    @Test
    void weekNumberCapsAtTotalAndStartsAtOne() {
        LocalDateTime expires = start.plusDays(21);
        assertEquals(3, WeekClock.totalWeeks(start, expires));
        assertEquals(1, WeekClock.weekNumber(start, expires, start.plusDays(2)));
        assertEquals(2, WeekClock.weekNumber(start, expires, start.plusDays(8)));
        assertEquals(3, WeekClock.weekNumber(start, expires, start.plusDays(20)));
        assertEquals(3, WeekClock.weekNumber(start, expires, start.plusDays(40)));
    }

    @Test
    void lastWeekEndsAtGrantExpiry() {
        LocalDateTime expires = start.plusDays(21);
        assertEquals(start, WeekClock.weekStart(start, 1));
        assertEquals(start.plusDays(7), WeekClock.weekEnd(start, expires, 1));
        assertTrue(WeekClock.isFinalWeek(start, expires, 3));
        assertFalse(WeekClock.isFinalWeek(start, expires, 2));
        assertEquals(expires, WeekClock.weekEnd(start, expires, 3));
    }
}
