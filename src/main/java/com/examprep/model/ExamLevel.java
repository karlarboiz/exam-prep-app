package com.examprep.model;

public enum ExamLevel {
    PROFESSIONAL,
    SUB_PROFESSIONAL;

    public static ExamLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ExamLevel.valueOf(value.trim().toUpperCase());
    }

    public String getDisplayName() {
        return this == PROFESSIONAL ? "Professional" : "Sub-Professional";
    }
}
