package com.examprep.util;

import com.examprep.i18n.LocaleSupport;
import com.examprep.model.AppLocale;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display formatting for dates shown in the UI.
 */
public final class DateFormats {

    private static final DateTimeFormatter DISPLAY_EN =
            DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", Locale.ENGLISH);

    private DateFormats() {
    }

    /** e.g. {@code July 23, 2026, 3:45 PM} or Filipino month names when Tagalog is active. */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        AppLocale locale = LocaleSupport.current();
        if (locale == AppLocale.EN) {
            return dateTime.format(DISPLAY_EN);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", locale.toFormatLocale());
        return dateTime.format(formatter);
    }
}
