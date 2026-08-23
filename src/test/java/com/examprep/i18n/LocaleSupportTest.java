package com.examprep.i18n;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocaleSupportTest {

    @Test
    void acceptsSafeRelativePaths() {
        assertTrue(LocaleSupport.isSafeRelativePath("/login"));
        assertTrue(LocaleSupport.isSafeRelativePath("/user/dashboard"));
        assertTrue(LocaleSupport.isSafeRelativePath("/admin/questions?edit=1"));
    }

    @Test
    void rejectsOpenRedirects() {
        assertFalse(LocaleSupport.isSafeRelativePath(null));
        assertFalse(LocaleSupport.isSafeRelativePath("https://evil.example/"));
        assertFalse(LocaleSupport.isSafeRelativePath("//evil.example"));
        assertFalse(LocaleSupport.isSafeRelativePath("/\\evil"));
        assertFalse(LocaleSupport.isSafeRelativePath("login"));
    }
}
