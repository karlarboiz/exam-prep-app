package com.examprep.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Loads {@code .properties} bundles as UTF-8 so Tagalog text does not need escapes.
 */
public final class Utf8Control extends ResourceBundle.Control {

    public static final Utf8Control INSTANCE = new Utf8Control();

    private Utf8Control() {
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IOException {
        String bundleName = toBundleName(baseName, locale);
        String resourceName = toResourceName(bundleName, "properties");
        try (InputStream stream = loader.getResourceAsStream(resourceName)) {
            if (stream == null) {
                return null;
            }
            return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
