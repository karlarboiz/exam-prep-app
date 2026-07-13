package com.examprep.model;

import java.time.LocalDateTime;

public class AccessGrant {

    private Long id;
    private String tokenHash;
    private AccessGrantStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime redeemedAt;
    private Long userId;
    private String planCode;
    private String sourceRef;
    private LocalDateTime createdAt;

    public AccessGrant() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public AccessGrantStatus getStatus() {
        return status;
    }

    public void setStatus(AccessGrantStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(LocalDateTime redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActiveAt(LocalDateTime when) {
        return status == AccessGrantStatus.REDEEMED
                && expiresAt != null
                && when.isBefore(expiresAt);
    }
}
