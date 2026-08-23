# Auth

Login, registration, session, and account self-service.

| Feature | Status | Notes |
|---------|--------|-------|
| Login (username/password → JWT cookie) | Done | [login](../features/auth/login.md) |
| Logout (clear cookie) | Done | [logout](../features/auth/logout.md) |
| Register with purchase token | Done | [register](../features/auth/register.md) |
| Password change (logged-in) | Done | [change-password](../features/auth/change-password.md) — `/account` for ADMIN and USER |
| Password reset / forgot password | Pending | No reset tokens or email flow |
| Profile self-service (view/edit account) | Pending | `/account` shows username/email/role/level read-only; edit not implemented |
