# Access grants

Purchase tokens, redeem, expiry gate, and admin tooling.

| Feature | Status | Notes |
|---------|--------|-------|
| Funnel create-token API | Done | [create-token-api](../features/access-grants/create-token-api.md) |
| Funnel revoke-token API | Done | [revoke-token-api](../features/access-grants/revoke-token-api.md) |
| One-time redeem at register | Done | [redeem](../features/access-grants/redeem.md) |
| Subscription gate on `/user/**` | Done | [enforce-access](../features/access-grants/enforce-access.md) |
| Subscription-expired page | Done | [page](../pages/user/subscription-expired.md) |
| Admin access-grant console (list / revoke / inspect) | Done | [admin-access-grants](../features/access-grants/admin-access-grants.md) — list + revoke; inspect is grant metadata, not raw token |
| Admin mint-token UI | Done | [admin-access-grants](../features/access-grants/admin-access-grants.md) — mint on `/admin/access-grants`; raw token shown once |
