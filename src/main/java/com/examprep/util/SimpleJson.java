package com.examprep.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON helpers for the funnel API (avoids a compile-time Jackson dependency).
 */
public final class SimpleJson {

    private SimpleJson() {
    }

    public static Optional<String> stringField(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    public static Optional<Integer> intField(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Optional.of(Integer.parseInt(m.group(1)));
        }
        return Optional.empty();
    }

    public static Optional<LocalDateTime> dateTimeField(String json, String field) {
        return stringField(json, field).flatMap(value -> {
            try {
                if (value.endsWith("Z")) {
                    return Optional.of(LocalDateTime.parse(value.substring(0, value.length() - 1)));
                }
                return Optional.of(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) {
                try {
                    return Optional.of(LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME));
                } catch (DateTimeParseException e2) {
                    return Optional.empty();
                }
            }
        });
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static String object(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must be pairs");
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(escape(keyValues[i])).append("\":\"")
                    .append(escape(keyValues[i + 1])).append('"');
        }
        sb.append('}');
        return sb.toString();
    }
}
