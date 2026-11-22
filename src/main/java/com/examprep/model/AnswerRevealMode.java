package com.examprep.model;

/**
 * When correct answers are shown during a practice exam attempt.
 */
public enum AnswerRevealMode {
    /** Show correct / incorrect feedback after each answered item. */
    AFTER_EACH,
    /** Hide answers until the exam is submitted (result review). */
    AFTER_EXAM;

    public static AnswerRevealMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return AFTER_EXAM;
        }
        return AnswerRevealMode.valueOf(value.trim().toUpperCase());
    }

    public String displayName() {
        return switch (this) {
            case AFTER_EACH -> "After each question";
            case AFTER_EXAM -> "After the exam";
        };
    }
}
