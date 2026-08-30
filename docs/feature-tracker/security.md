# Security & production hygiene

Secrets, cookies, and related production defaults.

| Feature | Status | Notes |
|---------|--------|-------|
| JWT session cookie `HttpOnly` | Done | Set in `WebUtil.setAuthCookie` |
| Fail-fast / require non-default secrets in production | Done | `AppConfig` validates secrets in production mode. Checks for insecure patterns and minimum length. Set `ENVIRONMENT=production` to enable validation. `n8n.webhook.secret` is required only when an n8n webhook URL is set |
| Auth cookie `Secure` flag | Done | `WebUtil` reads `cookie.secure`; also set when the request is HTTPS or SameSite=None |
| Auth cookie `SameSite` | Done | `WebUtil` reads `cookie.samesite` (`Strict` / `Lax` / `None`) |
| Timing attack protection for API keys | Done | `SecurityUtil.constantTimeEquals` uses constant-time comparison via SHA-256 hashing in `CreateAccessTokenServlet` and `RevokeAccessTokenServlet` |
| H2 console disabled by default | Done | `h2.console.enabled` defaults to `false` in `app.properties` with security warning |
| Sensitive files excluded from git | Done | `.gitignore` updated to exclude `*.env`, `.env*`, `*.key`, `*.pem`, `*.log` |
| CSRF protection | Done | `CsrfFilter` validates tokens on POST/PUT/DELETE requests. `CsrfUtil` for token generation/validation. Use `<ep:csrf/>` tag in forms. API endpoints exempt |
| Rate limiting on API endpoints | Done | `ApiRateLimitFilter` implements sliding window rate limiting. Default: 10 requests per minute per IP. Configurable via `rate.limit.api.max.requests` and `rate.limit.api.window.minutes` |
| Login / register rate limit | Done | `AuthRateLimitFilter` on POST `/login`, `/register`, `/forgot-password`, `/reset-password`. Default 8 / 15 min per IP |
| Login lockout | Done | 5 failed attempts per username, 15-minute window (`LoginLockout`) |
| HTTP security headers | Done | [headers](../features/security/headers.md) — CSP, nosniff, frame deny, Referrer-Policy, HSTS on HTTPS |
| JWT invalidation on password change | Done | `users.token_version` + JWT `tv` claim |
| JSP output encoding | Done | `<c:out>` on user/admin-authored text |
| Register token kept out of the URL | Done | Session + paste form; `?token=` still accepted then stripped |
| Admin password policy | Done | Admin credentials configurable via `ADMIN_USERNAME` and `ADMIN_PASSWORD` environment variables. Production deployments require `ADMIN_PASSWORD` to be set, otherwise startup fails |
