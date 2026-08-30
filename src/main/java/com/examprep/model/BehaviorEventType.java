package com.examprep.model;

public enum BehaviorEventType {
    LEAVE,
    RETURN;

    public static BehaviorEventType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        return BehaviorEventType.valueOf(value.trim().toUpperCase());
    }
}
