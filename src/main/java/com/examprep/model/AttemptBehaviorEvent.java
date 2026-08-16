package com.examprep.model;

import java.time.LocalDateTime;

public class AttemptBehaviorEvent {

    private Long id;
    private Long attemptId;
    private Long questionId;
    private BehaviorEventType eventType;
    private LocalDateTime occurredAt;
    private boolean questionAnswered;
    private Integer remainingQuestionMs;
    private Integer awayDurationMs;
    private boolean suspect;
    private String questionDifficulty;
    private String questionPrompt;
    private Integer questionNumber;

    public AttemptBehaviorEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public BehaviorEventType getEventType() {
        return eventType;
    }

    public void setEventType(BehaviorEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public boolean isQuestionAnswered() {
        return questionAnswered;
    }

    public void setQuestionAnswered(boolean questionAnswered) {
        this.questionAnswered = questionAnswered;
    }

    public Integer getRemainingQuestionMs() {
        return remainingQuestionMs;
    }

    public void setRemainingQuestionMs(Integer remainingQuestionMs) {
        this.remainingQuestionMs = remainingQuestionMs;
    }

    public Integer getAwayDurationMs() {
        return awayDurationMs;
    }

    public void setAwayDurationMs(Integer awayDurationMs) {
        this.awayDurationMs = awayDurationMs;
    }

    public boolean isSuspect() {
        return suspect;
    }

    public void setSuspect(boolean suspect) {
        this.suspect = suspect;
    }

    public String getQuestionDifficulty() {
        return questionDifficulty;
    }

    public void setQuestionDifficulty(String questionDifficulty) {
        this.questionDifficulty = questionDifficulty;
    }

    public String getQuestionPrompt() {
        return questionPrompt;
    }

    public void setQuestionPrompt(String questionPrompt) {
        this.questionPrompt = questionPrompt;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getAwayDurationLabel() {
        if (awayDurationMs == null) {
            return "—";
        }
        int secs = Math.max(0, awayDurationMs / 1000);
        if (secs < 1) {
            return "<1s";
        }
        if (secs < 60) {
            return secs + "s";
        }
        return (secs / 60) + "m " + (secs % 60) + "s";
    }
}
