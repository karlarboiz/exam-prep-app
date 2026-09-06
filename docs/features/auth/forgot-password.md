# Forgot / reset password

**Routes:** `GET|POST /forgot-password`, `GET|POST /reset-password`  
**Servlet:** `ForgotPasswordServlet`, `ResetPasswordServlet`  
**Service:** `AuthService.requestPasswordReset` / `resetPassword`  
**Pages:** [forgot-password.jsp](../../pages/auth/forgot-password.md), [reset-password.jsp](../../pages/auth/reset-password.md)

Public routes (JWT not required). CSRF applies. POSTs are rate-limited with login/register.

## Behavior

1. **GET /forgot-password** — email form.
2. **POST /forgot-password** — If the email exists, a one-time SHA-256 hashed token is stored in `password_reset_tokens` (default 60 minutes) and a mail is written to `email_outbox` (SMTP when configured). The page always shows the same success message so accounts cannot be enumerated.
3. **GET /reset-password?token=** — Stores the token in the session and redirects to `/reset-password` so the raw token does not stay in the address bar.
4. **POST /reset-password** — Sets a new password, increments `users.token_version` (invalidates existing JWTs), and marks the token used.

## Out of scope

- Admin-initiated reset
- SMS / magic-link login
