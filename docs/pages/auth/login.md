# Login page

**Path:** `WEB-INF/jsp/auth/login.jsp`  
**Route:** `/login`  
**Feature:** [login](../../features/auth/login.md)

Username/password form with `<ep:csrf/>`. Labels follow the header language switcher (Tagalog default). New users are directed to use the purchase registration link (token-gated), not open self-registration. Session JWT is separate from subscription entitlement.
