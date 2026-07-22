package com.examprep.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display formatting for dates shown in the UI.
 */
public final class DateFormats {

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", Locale.US);

    private DateFormats() {
    }

    /** e.g. {@code July 23, 2026, 3:45 PM} */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DISPLAY);
    }
}
