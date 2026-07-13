package com.examprep.model;

import java.time.LocalDateTime;

public enum AttemptStatus {
    IN_PROGRESS,
    COMPLETED,
    EXPIRED;

    public static AttemptStatus fromString(String value) {
        return AttemptStatus.valueOf(value);
    }
}
