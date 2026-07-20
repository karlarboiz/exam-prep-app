package com.examprep.model;

public enum ReadinessLabel {
    NEEDS_FOUNDATION("Needs foundation"),
    BUILDING("Building"),
    NEAR_READY("Near ready"),
    EXAM_READY("Exam-ready");

    private final String displayName;

    ReadinessLabel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
