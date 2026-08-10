package com.examprep.model;

public enum SubjectBand {
    STRONG,
    DEVELOPING,
    WEAK;

    public static SubjectBand fromString(String value) {
        return SubjectBand.valueOf(value.toUpperCase());
    }
}
