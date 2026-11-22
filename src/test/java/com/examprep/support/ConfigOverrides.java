package com.examprep.support;

import com.examprep.config.AppConfig;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Temporarily replaces {@link AppConfig} values. Callers must restore the previous value.
 */
public final class ConfigOverrides {

    private ConfigOverrides() {
    }

    public static String set(String key, String value) {
        Properties props = properties();
        String previous = props.getProperty(key);
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
        return previous;
    }

    public static void restore(String key, String previous) {
        Properties props = properties();
        if (previous == null) {
            props.remove(key);
        } else {
            props.setProperty(key, previous);
        }
    }

    private static Properties properties() {
        try {
            Field field = AppConfig.class.getDeclaredField("PROPS");
            field.setAccessible(true);
            return (Properties) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
