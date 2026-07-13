package com.examprep.model;

public enum AccessGrantStatus {
    UNUSED,
    REDEEMED,
    REVOKED;

    public static AccessGrantStatus fromString(String value) {
        return AccessGrantStatus.valueOf(value.toUpperCase());
    }
}
