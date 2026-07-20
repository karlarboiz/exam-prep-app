package com.examprep.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticResult {

    private ExamAttempt attempt;
    private ReadinessLabel readiness;
    private BigDecimal meanSubjectPercent;
    private List<DiagnosticSubjectScore> subjectScores = new ArrayList<>();
    private List<AttemptAnswer> answers = new ArrayList<>();

    public ExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ExamAttempt attempt) {
        this.attempt = attempt;
    }

    public ReadinessLabel getReadiness() {
        return readiness;
    }

    public void setReadiness(ReadinessLabel readiness) {
        this.readiness = readiness;
    }

    public BigDecimal getMeanSubjectPercent() {
        return meanSubjectPercent;
    }

    public void setMeanSubjectPercent(BigDecimal meanSubjectPercent) {
        this.meanSubjectPercent = meanSubjectPercent;
    }

    public List<DiagnosticSubjectScore> getSubjectScores() {
        return subjectScores;
    }

    public void setSubjectScores(List<DiagnosticSubjectScore> subjectScores) {
        this.subjectScores = subjectScores;
    }

    public List<AttemptAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AttemptAnswer> answers) {
        this.answers = answers;
    }
}
