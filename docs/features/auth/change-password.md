# Change password

**Route:** `GET|POST /account`  
**Servlet:** `AccountServlet`  
**Service:** `AuthService.changePassword`  
**Page:** [account.jsp](../../pages/auth/account.md)

Logged-in `ADMIN` and `USER` can change their own password. The route is **not** under `/user/**`, so the subscription and diagnostic gates do not block it.

## Behavior

1. **GET** — Requires a JWT session. Shows read-only profile fields and the change-password form. `?changed=1` after a successful POST shows `"Password updated."`
2. **POST** — Validates current password, new password, and confirm; updates `users.password_hash` and increments `token_version` so other sessions die. Re-issues the current JWT cookie. Redirects to `GET /account?changed=1` (PRG). The form includes `<ep:csrf/>`.

A user can only change **their own** password (`currentUser.id`). There is no admin-impersonation path.

## Validation

| Condition | Message |
|-----------|---------|
| Any password field blank | `"All password fields are required"` |
| New and confirm differ | `"New passwords do not match"` |
| New password shorter than 6 characters | `"Password must be at least 6 characters"` |
| New password equals current | `"New password must be different from the current password"` |
| Current password does not match hash | `"Current password is incorrect"` |

Minimum length matches [register](register.md).

## Out of scope

- Admin-initiated reset (users use [forgot-password](forgot-password.md))
- Editing username or email
- Forcing a password change on the seeded admin
