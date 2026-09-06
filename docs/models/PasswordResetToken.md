# PasswordResetToken

**Source:** `com.examprep.model.PasswordResetToken`  
**Table:** `password_reset_tokens`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| userId | Long | Owner |
| tokenHash | String | SHA-256 hex of the raw token |
| expiresAt | LocalDateTime | Default 60 minutes from issue |
| usedAt | LocalDateTime | Set on successful reset or when superseded |
| createdAt | LocalDateTime | Issued at |

Raw tokens are never stored. `isUsable(now)` is unused and not yet expired.
