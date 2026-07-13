# Admin — Users

**Route:** `/admin/users`  
**Servlet:** `UserServlet`  
**Service:** `AuthService.findAllUsers`  
**Page:** [users.jsp](../../pages/admin/users.md)  
**Model:** [User](../../models/User.md)

## Behavior

Lists registered users (username, email, role, created time). Primarily a read-only admin view unless extended.

## Access

Requires `Role.ADMIN`.
