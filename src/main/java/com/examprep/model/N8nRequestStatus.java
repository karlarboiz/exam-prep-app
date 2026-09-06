package com.examprep.model;

public enum N8nRequestStatus {
    ACCEPTED,
    FAILED;

    public static N8nRequestStatus fromString(String value) {
        return N8nRequestStatus.valueOf(value.toUpperCase());
    }
}
