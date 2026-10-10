# AccessGrantStatus

**Source:** `com.examprep.model.AccessGrantStatus` (enum)

| Value | Meaning |
|-------|---------|
| UNUSED | Created for funnel; not yet redeemed |
| REDEEMED | Locked to a user account; drives access until `expires_at` |
| REVOKED | Explicitly invalidated via admin UI or `POST /api/access-tokens/revoke`; cannot redeem; redeemed → revoked ends access immediately |

`fromString(String)` → `valueOf(uppercased)`.
