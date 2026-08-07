# AccessGrant

**Source:** `com.examprep.model.AccessGrant`  
**Table:** `access_grants`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| tokenHash | String | SHA-256 hex of raw token; unique |
| status | AccessGrantStatus | UNUSED / REDEEMED / REVOKED |
| expiresAt | LocalDateTime | End of one-time purchase access period; gates ongoing access |
| redeemedAt | LocalDateTime | When locked to a user |
| userId | Long | Set on redeem; null while UNUSED |
| planCode | String | Optional funnel plan |
| sourceRef | String | Optional order id |
| examLevel | ExamLevel | PROFESSIONAL or SUB_PROFESSIONAL; required for new tokens |
| createdAt | LocalDateTime | Creation time |
| username | String | Display helper when joined to `users` (admin list) |

Raw tokens are never stored. Helper `isActiveAt(when)` is true when `REDEEMED` and `when < expiresAt`.

Source of truth for access length is this grant (not a column on `users`). Exam track for a new account is taken from `examLevel` at redeem time.

Access is a **one-time purchase**: when `expires_at` passes (or the grant is revoked), access ends; there is no renewal API for existing accounts.
