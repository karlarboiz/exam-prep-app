# Admin — Access grants

**Route:** `/admin/access-grants`  
**Servlet:** `AccessGrantServlet`  
**Service:** `AccessGrantService.listAll` / `revoke`  
**Page:** [access-grants.jsp](../../pages/admin/access-grants.md)  
**Model:** [AccessGrant](../../models/AccessGrant.md)

## Behavior

- Lists all access grants (status, exam level, expiry, linked username, plan, source ref, created).
- Admin can **revoke** `UNUSED` or `REDEEMED` grants.
- Revoking a redeemed grant cuts student access immediately.
- Raw tokens are never shown (only hashes are stored).

## Access

Requires `Role.ADMIN`.

## Related

- Funnel revoke API: [revoke-token-api](revoke-token-api.md)
- Overview: [overview](overview.md)
