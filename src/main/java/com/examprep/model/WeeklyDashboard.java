package com.examprep.model;

public class WeeklyDashboard {

    private WeeklyRegimen current;
    private StudyPlan studyPlan;
    private int totalWeeks;
    private boolean canStartWeekly;
    private boolean canContinueWeekly;
    private Long inProgressWeeklyAttemptId;
    private boolean canReview;
    private boolean checkpointAvailable;
    private boolean canContinueCheckpoint;
    private Long inProgressCheckpointAttemptId;
    private boolean missedWeekNotice;
    private String bankWarning;

    public WeeklyRegimen getCurrent() {
        return current;
    }

    public void setCurrent(WeeklyRegimen current) {
        this.current = current;
    }

    public StudyPlan getStudyPlan() {
        return studyPlan;
    }

    public void setStudyPlan(StudyPlan studyPlan) {
        this.studyPlan = studyPlan;
    }

    public int getTotalWeeks() {
        return totalWeeks;
    }

    public void setTotalWeeks(int totalWeeks) {
        this.totalWeeks = totalWeeks;
    }

    public boolean isCanStartWeekly() {
        return canStartWeekly;
    }

    public void setCanStartWeekly(boolean canStartWeekly) {
        this.canStartWeekly = canStartWeekly;
    }

    public boolean isCanContinueWeekly() {
        return canContinueWeekly;
    }

    public void setCanContinueWeekly(boolean canContinueWeekly) {
        this.canContinueWeekly = canContinueWeekly;
    }

    public Long getInProgressWeeklyAttemptId() {
        return inProgressWeeklyAttemptId;
    }

    public void setInProgressWeeklyAttemptId(Long inProgressWeeklyAttemptId) {
        this.inProgressWeeklyAttemptId = inProgressWeeklyAttemptId;
    }

    public boolean isCanReview() {
        return canReview;
    }

    public void setCanReview(boolean canReview) {
        this.canReview = canReview;
    }

    public boolean isCheckpointAvailable() {
        return checkpointAvailable;
    }

    public void setCheckpointAvailable(boolean checkpointAvailable) {
        this.checkpointAvailable = checkpointAvailable;
    }

    public boolean isCanContinueCheckpoint() {
        return canContinueCheckpoint;
    }

    public void setCanContinueCheckpoint(boolean canContinueCheckpoint) {
        this.canContinueCheckpoint = canContinueCheckpoint;
    }

    public Long getInProgressCheckpointAttemptId() {
        return inProgressCheckpointAttemptId;
    }

    public void setInProgressCheckpointAttemptId(Long inProgressCheckpointAttemptId) {
        this.inProgressCheckpointAttemptId = inProgressCheckpointAttemptId;
    }

    public boolean isMissedWeekNotice() {
        return missedWeekNotice;
    }

    public void setMissedWeekNotice(boolean missedWeekNotice) {
        this.missedWeekNotice = missedWeekNotice;
    }

    public String getBankWarning() {
        return bankWarning;
    }

    public void setBankWarning(String bankWarning) {
        this.bankWarning = bankWarning;
    }
}
