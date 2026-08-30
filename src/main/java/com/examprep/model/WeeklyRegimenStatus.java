package com.examprep.model;

public enum WeeklyRegimenStatus {
    OPEN,
    COMPLETED,
    MISSED;

    public static WeeklyRegimenStatus fromString(String value) {
        return WeeklyRegimenStatus.valueOf(value.toUpperCase());
    }
}
