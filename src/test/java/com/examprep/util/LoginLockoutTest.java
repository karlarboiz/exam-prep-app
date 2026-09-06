package com.examprep.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginLockoutTest {

    @Test
    void locksAfterMaxFailuresAndClearsOnSuccess() {
        LoginLockout lockout = new LoginLockout(3, 60_000);
        lockout.recordFailure("Pat");
        lockout.recordFailure("pat");
        assertFalse(lockout.isLocked("pat"));
        lockout.recordFailure("PAT");
        assertTrue(lockout.isLocked("pat"));
        lockout.recordSuccess("pat");
        assertFalse(lockout.isLocked("pat"));
    }
}
