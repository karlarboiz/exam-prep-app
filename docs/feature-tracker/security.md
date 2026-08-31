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
| CSRF protection | Done | `CsrfFilter` validates tokens on POST/PUT/DELETE requests. `CsrfUtil` for token generation/validation. Use `<ep:csrf/>` tag in forms. API endpoints exempt |
| Rate limiting on API endpoints | Done | `ApiRateLimitFilter` implements sliding window rate limiting. Default: 10 requests per minute per IP. Configurable via `rate.limit.api.max.requests` and `rate.limit.api.window.minutes` |
| Admin password policy | Done | Admin credentials configurable via `ADMIN_USERNAME` and `ADMIN_PASSWORD` environment variables. Production deployments require `ADMIN_PASSWORD` to be set, otherwise startup fails |
| Header nav toggle CSP-safe | Done | Burger script moved from inline `<script>` in `header.jsp` to `js/nav-toggle.js` so `script-src 'self'` can run it |
