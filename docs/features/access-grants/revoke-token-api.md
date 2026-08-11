# Revoke access token API

**Route:** `POST /api/access-tokens/revoke`  
**Servlet:** `com.examprep.servlet.api.RevokeAccessTokenServlet`  
**Auth:** `X-Api-Key` header must match `funnel.api.key`  
**Public:** Yes (not user JWT). Listed in `JwtAuthFilter` public paths.

## Request body (JSON)

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| id | number | yes | Access grant id returned from create |

Example:

```json
{
  "id": 12
}
```

## Behavior

- `UNUSED` → `REVOKED`: token can no longer be redeemed at register.
- `REDEEMED` → `REVOKED`: quiz access ends immediately for that user.
- Already `REVOKED` → `400`.

## Success response — `200`

```json
{
  "id": "12",
  "status": "REVOKED"
}
```

## Errors

| Status | When |
|--------|------|
| 401 | Missing/wrong API key |
| 400 | Missing id, not found, already revoked |
| 500 | Unexpected server error |

## Related

- Admin UI: [admin-access-grants](admin-access-grants.md)
- Create: [create-token-api](create-token-api.md)
