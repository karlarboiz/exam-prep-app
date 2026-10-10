package com.examprep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamAttempt {

    private Long id;
    private Long userId;
    private Long examId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private BigDecimal scorePercent;
    private AttemptStatus status;
    private String examTitle;
    private String subjectName;
    private int durationMinutes;
    private boolean diagnostic;
    private boolean weekly;
    private AttemptKind attemptKind = AttemptKind.PRACTICE;
    private Long regimenId;
    private int leaveCount;
    private int suspectLeaveCount;
    private boolean integrityTracking;
    private String username;

    public ExamAttempt() {
    }

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

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public BigDecimal getScorePercent() {
        return scorePercent;
    }

    public void setScorePercent(BigDecimal scorePercent) {
        this.scorePercent = scorePercent;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isDiagnostic() {
        return diagnostic;
    }

    public void setDiagnostic(boolean diagnostic) {
        this.diagnostic = diagnostic;
    }

    public boolean isWeekly() {
        return weekly;
    }

    public void setWeekly(boolean weekly) {
        this.weekly = weekly;
    }

    public AttemptKind getAttemptKind() {
        return attemptKind;
    }

    public void setAttemptKind(AttemptKind attemptKind) {
        this.attemptKind = attemptKind != null ? attemptKind : AttemptKind.PRACTICE;
    }

    public Long getRegimenId() {
        return regimenId;
    }

    public void setRegimenId(Long regimenId) {
        this.regimenId = regimenId;
    }

    public int getLeaveCount() {
        return leaveCount;
    }

    public void setLeaveCount(int leaveCount) {
        this.leaveCount = leaveCount;
    }

    public int getSuspectLeaveCount() {
        return suspectLeaveCount;
    }

    public void setSuspectLeaveCount(int suspectLeaveCount) {
        this.suspectLeaveCount = suspectLeaveCount;
    }

    public boolean isIntegrityTracking() {
        return integrityTracking;
    }

    public void setIntegrityTracking(boolean integrityTracking) {
        this.integrityTracking = integrityTracking;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
