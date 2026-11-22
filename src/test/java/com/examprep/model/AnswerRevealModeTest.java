package com.examprep.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnswerRevealModeTest {

    @Test
    void fromStringDefaultsBlankToAfterExam() {
        assertEquals(AnswerRevealMode.AFTER_EXAM, AnswerRevealMode.fromString(null));
        assertEquals(AnswerRevealMode.AFTER_EXAM, AnswerRevealMode.fromString("  "));
    }

    @Test
    void fromStringParsesKnownValues() {
        assertEquals(AnswerRevealMode.AFTER_EACH, AnswerRevealMode.fromString("after_each"));
        assertEquals(AnswerRevealMode.AFTER_EXAM, AnswerRevealMode.fromString("AFTER_EXAM"));
    }

    @Test
    void fromStringRejectsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> AnswerRevealMode.fromString("NEVER"));
    }
}
