package com.examprep.model;

public enum N8nRequestKind {
    QUESTIONS,
    ANALYZE;

    public static N8nRequestKind fromString(String value) {
        return N8nRequestKind.valueOf(value.toUpperCase());
    }
}
