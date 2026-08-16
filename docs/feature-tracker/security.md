# Security & production hygiene

Secrets, cookies, and related production defaults.

| Feature | Status | Notes |
|---------|--------|-------|
| JWT session cookie `HttpOnly` | Done | Set in `WebUtil.setAuthCookie` |
| Fail-fast / require non-default secrets in production | Done | `AppConfig` validates secrets in production mode. Checks for insecure patterns and minimum length. Set `ENVIRONMENT=production` to enable validation |
| Auth cookie `Secure` flag | Done | Configurable via `cookie.secure` in `app.properties`. Set to `true` in production (requires HTTPS) |
| Auth cookie `SameSite` | Done | Configurable via `cookie.samesite` in `app.properties`. Defaults to `Lax`, supports `Strict` and `None` |
| Timing attack protection for API keys | Done | `SecurityUtil.constantTimeEquals` uses constant-time comparison via SHA-256 hashing in `CreateAccessTokenServlet` and `RevokeAccessTokenServlet` |
| H2 console disabled by default | Done | `h2.console.enabled` defaults to `false` in `app.properties` with security warning |
| Sensitive files excluded from git | Done | `.gitignore` updated to exclude `*.env`, `.env*`, `*.key`, `*.pem`, `*.log` |
| CSRF protection | Pending | No CSRF token validation implemented. Forms vulnerable to cross-site request forgery |
| Rate limiting on API endpoints | Pending | `/api/access-tokens` and `/api/access-tokens/revoke` lack rate limiting. Vulnerable to brute force and DoS |
| Admin password policy | Pending | Default admin `admin/admin123` created on first run. Should require password change or use env-based credentials |
