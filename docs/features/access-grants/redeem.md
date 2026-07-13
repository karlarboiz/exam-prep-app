# Redeem access token

**Route:** `GET|POST /register`  
**Servlet:** `RegisterServlet`  
**Service:** `AccessGrantService.registerWithToken` / `requireUnusedToken`  
**Page:** [register.jsp](../../pages/auth/register.md)

## Rules

- Registration **requires** a valid access token (query `?token=` or form field `token`).
- Token must be `UNUSED`, not revoked, and `expires_at` still in the future.
- On success, in one DB transaction:
  1. Create `USER` account
  2. Set grant to `REDEEMED`, set `user_id`, set `redeemed_at`
- The same raw token cannot be redeemed again (status locked).
- After redeem, a normal session JWT cookie is issued (same as login).

## Errors

- Missing token
- Invalid / already used / revoked / expired token
- Duplicate username or email
- Validation failures (password length, mismatch)

## Open registration

Public self-registration without a purchase token is disabled. Admin remains seeded via `SeedData`.
