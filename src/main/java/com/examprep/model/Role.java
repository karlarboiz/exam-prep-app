package com.examprep.model;

public enum Role {
    ADMIN,
    USER;

    public static Role fromString(String value) {
        return Role.valueOf(value.toUpperCase());
    }
}
