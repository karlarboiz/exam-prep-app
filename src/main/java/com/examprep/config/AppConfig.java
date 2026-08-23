package com.examprep.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = new Properties();
    private static final List<String> INSECURE_PATTERNS = List.of("change-me", "default", "example", "test");

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load app.properties", e);
        }
        overrideFromEnv("ENVIRONMENT", "app.environment");
        overrideFromEnv("DB_URL", "db.url");
        overrideFromEnv("DB_USERNAME", "db.username");
        overrideFromEnv("DB_PASSWORD", "db.password");
        overrideFromEnv("JWT_SECRET", "jwt.secret");
        overrideFromEnv("ID_CIPHER_SECRET", "id.cipher.secret");
        overrideFromEnv("FUNNEL_API_KEY", "funnel.api.key");
        overrideFromEnv("ADMIN_USERNAME", "admin.username");
        overrideFromEnv("ADMIN_PASSWORD", "admin.password");
        overrideFromEnv("SMTP_HOST", "mail.smtp.host");
        overrideFromEnv("SMTP_PORT", "mail.smtp.port");
        overrideFromEnv("SMTP_USERNAME", "mail.smtp.username");
        overrideFromEnv("SMTP_PASSWORD", "mail.smtp.password");
        overrideFromEnv("MAIL_FROM", "mail.from");
        overrideFromEnv("APP_PUBLIC_URL", "app.public.url");
        overrideFromEnv("PROXY_TRUST_FORWARDED", "proxy.trust.forwarded");
        
        validateSecurityConfig();
    }

    private AppConfig() {
    }

    private static void overrideFromEnv(String envKey, String propKey) {
        String value = System.getenv(envKey);
        if (value != null && !value.isBlank()) {
            PROPS.setProperty(propKey, value);
        }
    }

    private static void validateSecurityConfig() {
        if (!isProduction()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        
        validateSecret("jwt.secret", "JWT_SECRET", errors);
        validateSecret("id.cipher.secret", "ID_CIPHER_SECRET", errors);
        validateSecret("funnel.api.key", "FUNNEL_API_KEY", errors);

        if (!errors.isEmpty()) {
            String message = "Production security validation failed:\n" + String.join("\n", errors);
            throw new IllegalStateException(message);
        }
    }

    private static void validateSecret(String key, String envName, List<String> errors) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            errors.add("  - " + key + " is not set. Set environment variable " + envName);
            return;
        }

        String lowerValue = value.toLowerCase();
        for (String pattern : INSECURE_PATTERNS) {
            if (lowerValue.contains(pattern)) {
                errors.add("  - " + key + " contains insecure pattern '" + pattern + "'. Set environment variable " + envName);
                return;
            }
        }

        if (value.length() < 32) {
            errors.add("  - " + key + " must be at least 32 characters long. Set environment variable " + envName);
        }
    }

    public static boolean isProduction() {
        String env = get("app.environment", "development");
        return "production".equalsIgnoreCase(env) || "prod".equalsIgnoreCase(env);
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
