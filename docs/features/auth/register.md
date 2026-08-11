# Register

**Route:** `GET|POST /register`  
**Servlet:** `RegisterServlet`  
**Service:** `AccessGrantService`  
**Page:** [register.jsp](../../pages/auth/register.md)

## Behavior

1. **GET** — Requires `token` query param. Validates unused grant; shows setup form or error.
2. **POST** — Creates `USER`, redeems/locks grant in one transaction, issues session JWT cookie.
3. Redirect → `/user/diagnostic` (hard-gated until placement completes; see [diagnostic](../diagnostic/overview.md)).

Open registration without a purchase token is **not** allowed. See [access-grants redeem](../access-grants/redeem.md).
