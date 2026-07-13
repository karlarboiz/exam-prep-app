# Enforce subscription access

**Filter:** `com.examprep.filter.SubscriptionFilter` (runs after `JwtAuthFilter`)  
**Service:** `AccessGrantService.hasActiveAccess`

## Who is checked

| Caller | Behavior |
|--------|----------|
| Paths not under `/user` | Pass through |
| `/user/subscription-expired` | Always allowed (for entitled messaging) |
| `Role.ADMIN` | Skip entitlement check |
| `Role.USER` on `/user/**` | Must have `REDEEMED` grant with `expires_at > now` |

If the user has no active grant → redirect to `/user/subscription-expired`.

## Relation to session JWT

- Login JWT proves **who** you are (cookie `access_token`).
- Access grant proves **whether** the subscription still allows quizzes/materials.
- Login may still succeed after expiry so the expired page can show the end date; quiz routes stay blocked.

## Page

See [subscription-expired](../../pages/user/subscription-expired.md).
