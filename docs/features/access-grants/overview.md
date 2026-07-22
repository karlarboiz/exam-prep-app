# Access grants — overview

Subscription access for quizzes is controlled by **access grants**, not by the login session JWT alone.

## Flow

1. Funnel completes a purchase and calls `POST /api/access-tokens` with the shared API key and `examLevel`.
2. This app generates a high-entropy raw token, stores only its SHA-256 hash (`UNUSED`) plus exam level, and returns the raw token once.
3. Buyer opens `/register?token=...`, creates an account; exam track is fixed from the grant.
4. The grant is marked `REDEEMED`, locked to that `user_id`, and cannot be reused.
5. Ongoing access to `/user/**` requires a redeemed grant with `expires_at` still in the future.
6. Session auth remains the existing JWT cookie (`access_token`).

## Docs in this folder

| Doc | Topic |
|-----|--------|
| [create-token-api.md](create-token-api.md) | Funnel API |
| [redeem.md](redeem.md) | One-time registration redeem |
| [enforce-access.md](enforce-access.md) | SubscriptionFilter gating |

## Related

- Model: [AccessGrant](../../models/AccessGrant.md)
- Status enum: [AccessGrantStatus](../../models/AccessGrantStatus.md)
