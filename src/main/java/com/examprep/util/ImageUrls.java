    package com.examprep.util;

import java.util.Locale;

public final class ImageUrls {

    public static final int MAX_LENGTH = 500;

    private ImageUrls() {
    }

    /**
     * Accepts blank (no image), http(s) URLs, or app-relative paths starting with {@code /}.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String url = raw.trim();
        if (url.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Image URL must be at most " + MAX_LENGTH + " characters");
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new IllegalArgumentException("Image URL is not allowed");
        }
        if (url.startsWith("https://") || url.startsWith("http://") || url.startsWith("/")) {
            return url;
        }
        throw new IllegalArgumentException("Image URL must be http(s) or a path starting with /");
    }

    public static String resolve(String stored, String contextPath) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String url = stored.trim();
        if (url.startsWith("/")) {
            return (contextPath == null ? "" : contextPath) + url;
        }
        return url;
    }
}
