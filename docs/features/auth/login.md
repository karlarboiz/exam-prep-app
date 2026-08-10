# Login

**Route:** `GET|POST /login`  
**Servlet:** `LoginServlet`  
**Service:** `AuthService`  
**Page:** [login.jsp](../../pages/auth/login.md)

## Behavior

1. **GET** — If already authenticated, redirect to role dashboard. Otherwise show the login form.
2. **POST** — Validate username/password, verify hash via `PasswordUtil`, issue **session** JWT via `AuthService.issueToken`, set auth cookie via `WebUtil.setAuthCookie`.
3. Redirect: `ADMIN` → `/admin/dashboard`; `USER` with incomplete diagnostic → `/user/diagnostic`; otherwise → `/user/dashboard`.

## Session vs subscription

The login JWT (cookie `access_token`) identifies the user for the session.  
Quiz access after login still requires an active [access grant](../access-grants/overview.md) (`expires_at` in the future). Expired users are sent to `/user/subscription-expired`.  
Until [diagnostic](../diagnostic/overview.md) is completed, `DiagnosticFilter` blocks other `/user/**` routes.

## Errors

- Blank credentials → `"Username and password are required"`
- Bad credentials → `"Invalid username or password"`
- Unexpected failure → `"Login failed. Please try again."`
