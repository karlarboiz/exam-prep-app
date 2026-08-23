package com.examprep.service;

/**
 * Flag rules for examinee leave events. Warn-only policy — flags are for admin review.
 */
public final class BehaviorIntegrity {

    private BehaviorIntegrity() {
    }

    /**
     * A leave is suspect when the visible question is unanswered and marked HARD.
     * Answered items and EASY/MEDIUM leaves are stored but not flagged.
     */
    public static boolean isSuspectLeave(String difficulty, boolean answered) {
        if (answered) {
            return false;
        }
        if (difficulty == null || difficulty.isBlank()) {
            return false;
        }
        return "HARD".equalsIgnoreCase(difficulty.trim());
    }
}
