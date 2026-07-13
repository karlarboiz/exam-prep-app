# AccessGrant

**Source:** `com.examprep.model.AccessGrant`  
**Table:** `access_grants`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| tokenHash | String | SHA-256 hex of raw token; unique |
| status | AccessGrantStatus | UNUSED / REDEEMED / REVOKED |
| expiresAt | LocalDateTime | Subscription end; gates ongoing access |
| redeemedAt | LocalDateTime | When locked to a user |
| userId | Long | Set on redeem; null while UNUSED |
| planCode | String | Optional funnel plan |
| sourceRef | String | Optional order id |
| createdAt | LocalDateTime | Creation time |

Raw tokens are never stored. Helper `isActiveAt(when)` is true when `REDEEMED` and `when < expiresAt`.

Source of truth for access length is this grant (not a column on `users`).
