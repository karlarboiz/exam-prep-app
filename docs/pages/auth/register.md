# Register page

**Path:** `WEB-INF/jsp/auth/register.jsp`  
**Route:** `/register`  
**Feature:** [redeem](../../features/access-grants/redeem.md)

Requires a valid access token (`?token=` on GET; hidden `token` field on POST). Shows a read-only token field (`.token-readonly`) and a read-only exam level (from the grant) when the token is valid. Without a token, shows an empty-state message instead of the form.
