package com.examprep.model;

public enum AttemptKind {
    PRACTICE,
    DIAGNOSTIC,
    WEEKLY,
    CHECKPOINT,
    REVIEW;

    public static AttemptKind fromString(String value) {
        if (value == null || value.isBlank()) {
            return PRACTICE;
        }
        return AttemptKind.valueOf(value.toUpperCase());
    }

    public String displayName() {
        return switch (this) {
            case PRACTICE -> "Practice";
            case DIAGNOSTIC -> "Diagnostic";
            case WEEKLY -> "Weekly";
            case CHECKPOINT -> "Checkpoint";
            case REVIEW -> "Review";
        };
    }
}
