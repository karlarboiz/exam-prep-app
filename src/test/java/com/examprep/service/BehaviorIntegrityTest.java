package com.examprep.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorIntegrityTest {

    @Test
    void unansweredHardIsSuspect() {
        assertTrue(BehaviorIntegrity.isSuspectLeave("HARD", false));
        assertTrue(BehaviorIntegrity.isSuspectLeave("hard", false));
    }

    @Test
    void answeredHardIsNotSuspect() {
        assertFalse(BehaviorIntegrity.isSuspectLeave("HARD", true));
    }

    @Test
    void unansweredEasyOrMediumIsNotSuspect() {
        assertFalse(BehaviorIntegrity.isSuspectLeave("EASY", false));
        assertFalse(BehaviorIntegrity.isSuspectLeave("MEDIUM", false));
    }

    @Test
    void blankDifficultyIsNotSuspect() {
        assertFalse(BehaviorIntegrity.isSuspectLeave(null, false));
        assertFalse(BehaviorIntegrity.isSuspectLeave("  ", false));
    }
}
