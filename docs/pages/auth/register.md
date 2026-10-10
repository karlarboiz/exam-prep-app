# Register page

**Path:** `WEB-INF/jsp/auth/register.jsp`  
**Route:** `/register`  
**Feature:** [redeem](../../features/access-grants/redeem.md)

Paste an access token (or arrive via a one-time `?token=` that is immediately moved into the session). Shows a read-only token field (`.token-readonly`) and a read-only exam level (from the grant) when the token is valid. Without a token, shows a paste form.
