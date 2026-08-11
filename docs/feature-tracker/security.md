# Security & production hygiene

Secrets, cookies, and related production defaults.

| Feature | Status | Notes |
|---------|--------|-------|
| JWT session cookie `HttpOnly` | Done | Set in `WebUtil.setAuthCookie` |
| Fail-fast / require non-default secrets in production | Pending | `app.properties` still ships `change-me-…` JWT / funnel (and related) secrets |
| Auth cookie `Secure` flag | Pending | Not set today — needed for HTTPS |
| Auth cookie `SameSite` | Pending | Not set today — prefer `Lax` or `Strict`, config-driven for local HTTP vs prod HTTPS |
