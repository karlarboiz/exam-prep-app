# Logout

**Route:** `/logout`  
**Servlet:** `LogoutServlet`

## Behavior

Clears the JWT auth cookie and redirects the user to `/login` (or home). After logout, `JwtAuthFilter` treats the session as unauthenticated.
