# Create access token API

**Route:** `POST /api/access-tokens`  
**Servlet:** `com.examprep.servlet.api.CreateAccessTokenServlet`  
**Auth:** `X-Api-Key` header must match `funnel.api.key` in `app.properties` (or `FUNNEL_API_KEY` env).  
**Public:** Yes (not user JWT). Bypass listed in `JwtAuthFilter`.

## Request body (JSON)

Provide **either** `expiresAt` **or** `durationDays`.

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| expiresAt | string | one of | ISO local date-time, e.g. `2026-12-31T23:59:59` |
| durationDays | number | one of | Days from now |
| planCode | string | no | Funnel plan label |
| sourceRef | string | no | Order / payment id for support |

Example:

```json
{
  "durationDays": 30,
  "planCode": "standard",
  "sourceRef": "order_123"
}
```

## Success response — `201`

```json
{
  "token": "<raw hex token>",
  "expiresAt": "2026-08-13T06:00:00",
  "id": "1",
  "status": "UNUSED"
}
```

Show the buyer a link such as `/register?token=<token>`. The raw token is **not** stored in the DB.

## Errors

| Status | When |
|--------|------|
| 401 | Missing/wrong API key, or `funnel.api.key` blank |
| 400 | Missing expiry, expiry in the past, bad payload |
| 500 | Unexpected server error |

## Config

```
funnel.api.key=change-me-funnel-api-key
```
