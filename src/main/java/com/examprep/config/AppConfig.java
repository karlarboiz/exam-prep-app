package com.examprep.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load app.properties", e);
        }
        overrideFromEnv("DB_URL", "db.url");
        overrideFromEnv("DB_USERNAME", "db.username");
        overrideFromEnv("DB_PASSWORD", "db.password");
        overrideFromEnv("JWT_SECRET", "jwt.secret");
        overrideFromEnv("FUNNEL_API_KEY", "funnel.api.key");
    }

    private AppConfig() {
    }

    private static void overrideFromEnv(String envKey, String propKey) {
        String value = System.getenv(envKey);
        if (value != null && !value.isBlank()) {
            PROPS.setProperty(propKey, value);
        }
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
