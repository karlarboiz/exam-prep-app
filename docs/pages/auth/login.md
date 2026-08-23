# Login page

**Path:** `WEB-INF/jsp/auth/login.jsp`  
**Route:** `/login`  
**Feature:** [login](../../features/auth/login.md)

Username/password form with `<ep:csrf/>`. Labels follow the header language switcher (Tagalog default). Includes a forgot-password link. The default admin hint is hidden in production. New users paste a purchase token on `/register`. Session JWT is separate from subscription entitlement.
