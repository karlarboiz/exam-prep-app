# Access grants

Purchase tokens, redeem, expiry gate, and admin tooling.

| Feature | Status | Notes |
|---------|--------|-------|
| Funnel create-token API | Done | [create-token-api](../features/access-grants/create-token-api.md) |
| One-time redeem at register | Done | [redeem](../features/access-grants/redeem.md) |
| Subscription gate on `/user/**` | Done | [enforce-access](../features/access-grants/enforce-access.md) |
| Subscription-expired page | Done | [page](../pages/user/subscription-expired.md) |
| Admin access-grant console (list / revoke / inspect) | Pending | Extends existing grant model/APIs; needs admin UI to list and revoke (inspect = grant metadata, not raw token) |
| Admin mint-token UI | Pending | Create today is funnel API only |
