# Admin — Access grants

**Route:** `/admin/access-grants`  
**Servlet:** `AccessGrantServlet`  
**Service:** `AccessGrantService.createToken` / `listAll` / `revoke`  
**Page:** [access-grants.jsp](../../pages/admin/access-grants.md)  
**Model:** [AccessGrant](../../models/AccessGrant.md)

## Behavior

- Admin can **mint** a token: exam level (required), duration in days (required), optional plan code and source ref. Uses the same `createToken` path as the funnel API.
- After mint, the **raw token is shown once** (read-only) with the `/register` page. Share the token separately; do not put it in a URL. Raw tokens are never stored.
- Lists all access grants (status, exam level, expiry, linked username, plan, source ref, created).
- Admin can **revoke** `UNUSED` or `REDEEMED` grants.
- Revoking a redeemed grant cuts student access immediately.

## Access

Requires `Role.ADMIN`.

## Related

- Funnel create API: [create-token-api](create-token-api.md)
- Funnel revoke API: [revoke-token-api](revoke-token-api.md)
- Overview: [overview](overview.md)
