# Admin — Users

**Route:** `/admin/users`  
**Servlet:** `UserServlet`  
**Service:** `AuthService.findAllUsers` / `updateUser` / `deleteUser`  
**Page:** [users.jsp](../../pages/admin/users.md)  
**Model:** [User](../../models/User.md)

## Behavior

Lists registered users (username, email, registered time). Admins can change **role** and **exam level** per row, or **delete** a user (cascades attempts).

| Action | Description |
|--------|-------------|
| update | Set `role` (`ADMIN` / `USER`) and optional `examLevel` |
| delete | Remove user by `id` |

## Rules

- Student (`USER`) accounts **require** an exam level.
- Admin accounts may have a blank exam level.
- You cannot change your own role or delete your own account.
- You cannot demote or delete the last remaining admin.
- Changing a student's exam level clears `diagnostic_completed_at` so they retake placement on the new track.

## Access

Requires `Role.ADMIN`.
