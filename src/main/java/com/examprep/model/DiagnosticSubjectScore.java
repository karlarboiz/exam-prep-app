package com.examprep.model;

import java.math.BigDecimal;

public class DiagnosticSubjectScore {

    private Long attemptId;
    private Long subjectId;
    private String subjectName;
    private BigDecimal scorePercent;
    private SubjectBand band;

    public DiagnosticSubjectScore() {
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public BigDecimal getScorePercent() {
        return scorePercent;
    }

    public void setScorePercent(BigDecimal scorePercent) {
        this.scorePercent = scorePercent;
    }

    public SubjectBand getBand() {
        return band;
    }

    public void setBand(SubjectBand band) {
        this.band = band;
    }
}
