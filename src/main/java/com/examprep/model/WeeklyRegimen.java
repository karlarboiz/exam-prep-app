package com.examprep.model;

import java.time.LocalDateTime;

public class WeeklyRegimen {

    private Long id;
    private Long userId;
    private int weekNumber;
    private LocalDateTime weekStart;
    private LocalDateTime weekEnd;
    private WeeklyRegimenStatus status;
    private boolean finalWeek;
    private Long officialAttemptId;
    private LocalDateTime emailSentAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public LocalDateTime getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDateTime weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDateTime getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(LocalDateTime weekEnd) {
        this.weekEnd = weekEnd;
    }

    public WeeklyRegimenStatus getStatus() {
        return status;
    }

    public void setStatus(WeeklyRegimenStatus status) {
        this.status = status;
    }

    public boolean isFinalWeek() {
        return finalWeek;
    }

    public void setFinalWeek(boolean finalWeek) {
        this.finalWeek = finalWeek;
    }

    public Long getOfficialAttemptId() {
        return officialAttemptId;
    }

    public void setOfficialAttemptId(Long officialAttemptId) {
        this.officialAttemptId = officialAttemptId;
    }

    public LocalDateTime getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(LocalDateTime emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean hasOfficialScore() {
        return officialAttemptId != null;
    }
}
