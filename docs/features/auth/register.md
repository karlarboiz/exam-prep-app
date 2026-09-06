# Register

**Route:** `GET|POST /register`  
**Servlet:** `RegisterServlet`  
**Service:** `AccessGrantService`  
**Page:** [register.jsp](../../pages/auth/register.md)

## Behavior

1. **GET** — If `?token=` is present, the token is moved into the session and the browser is redirected to `/register` (no token in the address bar). Otherwise the page accepts a pasted token (`action=claim`).
2. **POST** (claim) — Stores the pasted token in session and reloads the setup form.
3. **POST** (register) — Creates `USER`, redeems/locks grant in one transaction, issues session JWT cookie.
4. Redirect → `/user/diagnostic` (hard-gated until placement completes; see [diagnostic](../diagnostic/overview.md)).

Open registration without a purchase token is **not** allowed. See [access-grants redeem](../access-grants/redeem.md). Prefer sharing `/register` plus the raw token — do not put the token in a URL.
