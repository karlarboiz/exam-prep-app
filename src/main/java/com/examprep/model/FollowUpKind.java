package com.examprep.model;

/**
 * One follow-up email per weekly regimen. Due means the week is still open.
 * Missed means the week ended without that requirement.
 */
public enum FollowUpKind {
    WEEKLY_EXAM_DUE,
    WEEKLY_EXAM_MISSED,
    CHECKPOINT_DUE,
    CHECKPOINT_MISSED;

    public static FollowUpKind fromString(String value) {
        return FollowUpKind.valueOf(value);
    }
}
