# Profile self-service

**Route:** `GET|POST /account` (`action=profile`)  
**Servlet:** `AccountServlet`  
**Service:** `AuthService.updateProfile`  
**Page:** [account.jsp](../../pages/auth/account.md)

Logged-in `ADMIN` and `USER` can change their own username and email. Role and exam level stay read-only (admins change those on `/admin/users`). The route is **not** under `/user/**`, so the subscription and diagnostic gates do not block it.

## Behavior

1. **GET** — Requires a JWT session. Shows the profile form (editable username/email, read-only role and exam level) and the change-password form. `?profile=1` after a successful POST shows `"Profile updated."`
2. **POST `action=profile`** — Validates username, email, and current password; updates `users.username` and `users.email`. Re-issues the current JWT cookie so the username claim matches. Does **not** increment `token_version`. Redirects to `GET /account?profile=1` (PRG). The form includes `<ep:csrf/>`.

A user can only edit **their own** profile (`currentUser.id`). There is no admin-impersonation path.

## Validation

| Condition | Message |
|-----------|---------|
| Current password blank | `"Current password is required"` |
| Username blank | `"Username is required"` |
| Email blank | `"Email is required"` |
| Username longer than 50 characters | `"Username is too long"` |
| Email longer than 100 characters | `"Email is too long"` |
| Email missing `@` / domain dot | `"Email is invalid"` |
| Current password does not match hash | `"Current password is incorrect"` |
| Username taken by another user | `"Username already exists"` |
| Email taken by another user | `"Email already exists"` |

Keeping the same username and email is allowed when the current password is correct.

## Out of scope

- Editing role or exam level (see [admin users](../admin-users/overview.md))
- Changing password (see [change-password](change-password.md))
