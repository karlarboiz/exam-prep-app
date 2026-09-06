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

    public static Optional<Long> longField(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Optional.of(Long.parseLong(m.group(1)));
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

    public static String quoted(String value) {
        return "\"" + escape(value) + "\"";
    }

    /**
     * Builds a JSON object from key / already-encoded value pairs (quoted strings, arrays, or objects).
     */
    public static String rawObject(String... keyAndEncodedValue) {
        if (keyAndEncodedValue.length % 2 != 0) {
            throw new IllegalArgumentException("keyAndEncodedValue must be pairs");
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyAndEncodedValue.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quoted(keyAndEncodedValue[i])).append(':').append(keyAndEncodedValue[i + 1]);
        }
        sb.append('}');
        return sb.toString();
    }

    public static String array(java.util.List<String> encodedItems) {
        StringBuilder sb = new StringBuilder("[");
        if (encodedItems != null) {
            for (int i = 0; i < encodedItems.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(encodedItems.get(i));
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public static String unescapeJsonString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                sb.append(c);
                continue;
            }
            char next = raw.charAt(++i);
            switch (next) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                default -> sb.append(next);
            }
        }
        return sb.toString();
    }
}
