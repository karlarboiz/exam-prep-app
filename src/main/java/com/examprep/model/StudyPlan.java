package com.examprep.model;

import java.util.ArrayList;
import java.util.List;

public class StudyPlan {

    private WeeklyRegimen regimen;
    private List<WeeklySubjectScore> subjectScores = new ArrayList<>();
    private List<AttemptAnswer> misses = new ArrayList<>();
    private List<String> targets = new ArrayList<>();
    private boolean emailSent;
    private String emailTo;
    private boolean fromDiagnostic;

    public WeeklyRegimen getRegimen() {
        return regimen;
    }

    public void setRegimen(WeeklyRegimen regimen) {
        this.regimen = regimen;
    }

    public List<WeeklySubjectScore> getSubjectScores() {
        return subjectScores;
    }

    public void setSubjectScores(List<WeeklySubjectScore> subjectScores) {
        this.subjectScores = subjectScores != null ? subjectScores : new ArrayList<>();
    }

    public List<AttemptAnswer> getMisses() {
        return misses;
    }

    public void setMisses(List<AttemptAnswer> misses) {
        this.misses = misses != null ? misses : new ArrayList<>();
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets != null ? targets : new ArrayList<>();
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public boolean isFromDiagnostic() {
        return fromDiagnostic;
    }

    public void setFromDiagnostic(boolean fromDiagnostic) {
        this.fromDiagnostic = fromDiagnostic;
    }
}
