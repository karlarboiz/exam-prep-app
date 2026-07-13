# JWT Auth Filter

**Class:** `com.examprep.filter.JwtAuthFilter`  
**Token helper:** `JwtUtil`  
**Cookie helper:** `WebUtil`

## Public paths

- `/login`, `/register`
- `/`, `/index.jsp`
- `/css/**`, `/error/**`
- `/api/access-tokens` (authenticated via `X-Api-Key`, not user JWT)

## Protected behavior

1. Read JWT from auth cookie.
2. Parse claims → load `User` by id; on failure treat as anonymous.
3. Unauthenticated + non-public → redirect `/login`.
4. Path starts with `/admin` and role ≠ `ADMIN` → HTTP 403.
5. Set request attribute `CURRENT_USER` for JSPs/servlets.

## Subscription check (separate filter)

After this filter, `SubscriptionFilter` gates `/user/**` for non-admin users based on redeemed access grants. See [enforce-access](../access-grants/enforce-access.md).

## Related

- Token issued on login: `AuthService.issueToken`
- Cleared on logout: `LogoutServlet`
- Purchase tokens: [access-grants](../access-grants/overview.md)
