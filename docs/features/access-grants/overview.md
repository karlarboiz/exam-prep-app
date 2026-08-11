# Access grants — overview

Quiz access is controlled by **one-time purchase access grants**, not by the login session JWT alone. This is **not** a renewable subscription: the buyer pays once, redeems a token at registration, and keeps access until the grant’s `expires_at` (typically prep time plus a short post-exam review window, e.g. 3 days after the exam). When that window ends, access ends permanently for that account — there is no in-app renewal or re-link flow.

## Flow

1. Funnel completes a purchase and calls `POST /api/access-tokens` with the shared API key and `examLevel`.
2. This app generates a high-entropy raw token, stores only its SHA-256 hash (`UNUSED`) plus exam level, and returns the raw token once.
3. Buyer opens `/register?token=...`, creates an account; exam track is fixed from the grant.
4. The grant is marked `REDEEMED`, locked to that `user_id`, and cannot be reused.
5. Ongoing access to `/user/**` requires a redeemed grant with `expires_at` still in the future and status not `REVOKED`.
6. Session auth remains the existing JWT cookie (`access_token`).
7. Support (or the funnel) may **revoke** unused or redeemed grants; revoked redeemed grants cut access immediately.

## Docs in this folder

| Doc | Topic |
|-----|--------|
| [create-token-api.md](create-token-api.md) | Funnel create API |
| [revoke-token-api.md](revoke-token-api.md) | Funnel / support revoke API |
| [redeem.md](redeem.md) | One-time registration redeem |
| [enforce-access.md](enforce-access.md) | Access-period gating (`SubscriptionFilter`) |
| [admin-access-grants.md](admin-access-grants.md) | Admin list + revoke UI |

## Related

- Model: [AccessGrant](../../models/AccessGrant.md)
- Status enum: [AccessGrantStatus](../../models/AccessGrantStatus.md)
- Expired access page: [subscription-expired](../../pages/user/subscription-expired.md)
