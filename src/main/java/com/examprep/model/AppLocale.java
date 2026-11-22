package com.examprep.model;

import java.util.Locale;

/**
 * UI languages. Tagalog is the default so the app is ready for Filipino learners;
 * English remains available as a switch.
 */
public enum AppLocale {
    TL("tl"),
    EN("en");

    public static final AppLocale DEFAULT = TL;

    private final String code;

    AppLocale(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getHtmlLang() {
        return code;
    }

    /** Locale used to load {@code messages_*.properties}. */
    public Locale toBundleLocale() {
        return Locale.forLanguageTag(code);
    }

    /** Locale used for dates; Filipino CLDR data is richer than Tagalog. */
    public Locale toFormatLocale() {
        return this == EN ? Locale.ENGLISH : Locale.forLanguageTag("fil");
    }

    public static AppLocale fromCode(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("en")) {
            return EN;
        }
        if (normalized.startsWith("tl") || normalized.startsWith("fil")) {
            return TL;
        }
        return DEFAULT;
    }

    public static boolean isSupported(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("en") || normalized.startsWith("tl") || normalized.startsWith("fil");
    }
}
