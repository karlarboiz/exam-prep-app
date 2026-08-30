# Auth

Login, registration, session, and account self-service.

| Feature | Status | Notes |
|---------|--------|-------|
| Login (username/password → JWT cookie) | Done | [login](../features/auth/login.md) |
| Logout (clear cookie) | Done | [logout](../features/auth/logout.md) |
| Register with purchase token | Done | [register](../features/auth/register.md) |
| Password change (logged-in) | Done | [change-password](../features/auth/change-password.md) — `/account` for ADMIN and USER |
| Password reset / forgot password | Done | `/forgot-password` + `/reset-password`; hashed tokens; email via outbox/SMTP |
| Profile self-service (view/edit account) | Done | [profile](../features/auth/profile.md) — edit username/email on `/account`; role/level stay admin-only |
