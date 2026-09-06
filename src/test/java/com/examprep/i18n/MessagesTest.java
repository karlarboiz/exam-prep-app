package com.examprep.i18n;

import com.examprep.model.AppLocale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    @Test
    void tagalogLoginDiffersFromEnglish() {
        String english = Messages.get(AppLocale.EN, "login.heading");
        String tagalog = Messages.get(AppLocale.TL, "login.heading");
        assertEquals("Login", english);
        assertEquals("Mag-login", tagalog);
    }

    @Test
    void defaultLocaleIsTagalog() {
        assertEquals(AppLocale.TL, AppLocale.DEFAULT);
        assertEquals(AppLocale.TL, AppLocale.fromCode(null));
        assertEquals(AppLocale.EN, AppLocale.fromCode("en-US"));
        assertEquals(AppLocale.TL, AppLocale.fromCode("fil"));
    }

    @Test
    void exceptionMapCoversStudentFacingAuthErrors() {
        assertEquals("Username already exists",
                Messages.get(AppLocale.EN, "error.username.exists"));
        assertEquals("May ganitong username na",
                Messages.get(AppLocale.TL, "error.username.exists"));
        assertEquals("Profile updated.",
                Messages.get(AppLocale.EN, "error.account.profileUpdated"));
        assertEquals("Na-update na ang profile.",
                Messages.get(AppLocale.TL, "error.account.profileUpdated"));
    }

    @Test
    void formatInsertsArguments() {
        String text = Messages.format(AppLocale.TL, "dashboard.welcome", "Ana");
        assertTrue(text.contains("Ana"));
        assertFalse(text.contains("{0}"));
    }
}
