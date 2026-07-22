package com.examprep.model;

import java.time.LocalDateTime;

public class User {

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private Role role;
    private ExamLevel examLevel;
    private LocalDateTime createdAt;
    private LocalDateTime diagnosticCompletedAt;

    public User() {
    }

    public User(Long id, String username, String email, String passwordHash, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public ExamLevel getExamLevel() {
        return examLevel;
    }

    public void setExamLevel(ExamLevel examLevel) {
        this.examLevel = examLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDiagnosticCompletedAt() {
        return diagnosticCompletedAt;
    }

    public void setDiagnosticCompletedAt(LocalDateTime diagnosticCompletedAt) {
        this.diagnosticCompletedAt = diagnosticCompletedAt;
    }

    public boolean isDiagnosticCompleted() {
        return diagnosticCompletedAt != null;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
