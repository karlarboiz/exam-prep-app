package com.examprep.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUrlsTest {

    @Test
    void normalizeTreatsBlankAsMissing() {
        assertNull(ImageUrls.normalize(null));
        assertNull(ImageUrls.normalize("   "));
    }

    @Test
    void normalizeAcceptsHttpAndRelativePaths() {
        assertEquals("https://cdn.example/q1.png", ImageUrls.normalize(" https://cdn.example/q1.png "));
        assertEquals("/media/diagram.png", ImageUrls.normalize("/media/diagram.png"));
    }

    @Test
    void normalizeRejectsUnsafeSchemes() {
        assertThrows(IllegalArgumentException.class, () -> ImageUrls.normalize("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> ImageUrls.normalize("data:image/png;base64,xx"));
        assertThrows(IllegalArgumentException.class, () -> ImageUrls.normalize("ftp://files.example/q1.png"));
    }

    @Test
    void resolvePrefixesContextPathForAppRelativeUrls() {
        assertEquals("/app/media/q1.png", ImageUrls.resolve("/media/q1.png", "/app"));
        assertEquals("https://cdn.example/q1.png", ImageUrls.resolve("https://cdn.example/q1.png", "/app"));
        assertNull(ImageUrls.resolve("  ", "/app"));
    }
}
