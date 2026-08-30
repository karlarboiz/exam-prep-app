# JWT Auth Filter

**Class:** `com.examprep.filter.JwtAuthFilter`  
**Token helper:** `JwtUtil`  
**Cookie helper:** `WebUtil`

## Public paths

- `/login`, `/register`, `/forgot-password`, `/reset-password`, `/locale`
- `/`, `/index.jsp`
- `/css/**`, `/js/**`, `/error/**`
- `/api/access-tokens` (create; authenticated via `X-Api-Key`, not user JWT)
- `/api/access-tokens/revoke` (revoke; same API key)

## Protected behavior

1. Read JWT from auth cookie.
2. Parse claims → load `User` by id; reject if JWT `tv` does not match `users.token_version`.
3. Unauthenticated + non-public → redirect `/login`.
4. Path starts with `/admin` and role ≠ `ADMIN` → HTTP 403.
5. Set request attribute `CURRENT_USER` for JSPs/servlets.
6. `/account` is JWT-required for any authenticated role (not public; not under `/user/**`).

## Access-period check (separate filter)

After this filter, `SubscriptionFilter` gates `/user/**` for non-admin users based on redeemed one-time purchase grants. See [enforce-access](../access-grants/enforce-access.md).

## Related

- Token issued on login: `AuthService.issueToken`
- Cleared on logout: `LogoutServlet`
- Purchase tokens: [access-grants](../access-grants/overview.md)
