# Enforce one-time purchase access

**Filter:** `com.examprep.filter.SubscriptionFilter` (runs after `JwtAuthFilter`)  
**Service:** `AccessGrantService.hasActiveAccess`

> Naming note: the filter class is still called `SubscriptionFilter` for historical reasons. Product-wise this is a **one-time purchase access period**, not a renewable subscription.

## Who is checked

| Caller | Behavior |
|--------|----------|
| Paths not under `/user` | Pass through |
| `/user/subscription-expired` | Always allowed (for entitled messaging) |
| `Role.ADMIN` | Skip entitlement check |
| `Role.USER` on `/user/**` | Must have `REDEEMED` grant with `expires_at > now` |

If the user has no active grant → redirect to `/user/subscription-expired`.

Revoking a redeemed grant (status → `REVOKED`) also ends access immediately, even if `expires_at` is still in the future.

## Filter order

1. `JwtAuthFilter` — session identity  
2. `SubscriptionFilter` — active access grant  
3. `DiagnosticFilter` — placement diagnostic completed (see [diagnostic](../diagnostic/overview.md))

## Relation to session JWT

- Login JWT proves **who** you are (cookie `access_token`).
- Access grant proves **whether** the one-time purchase still allows quizzes/materials.
- Login may still succeed after expiry so the expired page can show the end date; quiz routes stay blocked.
- There is **no** renewal or attach-new-token flow for an existing account — access ending after the review window is expected.

## Page

See [subscription-expired](../../pages/user/subscription-expired.md).
