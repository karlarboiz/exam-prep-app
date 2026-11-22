package com.examprep.model;

import java.time.LocalDateTime;

public class N8nRequest {

    private Long id;
    private Long adminUserId;
    private N8nRequestKind kind;
    private String summary;
    private N8nRequestStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public N8nRequestKind getKind() {
        return kind;
    }

    public void setKind(N8nRequestKind kind) {
        this.kind = kind;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public N8nRequestStatus getStatus() {
        return status;
    }

    public void setStatus(N8nRequestStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
