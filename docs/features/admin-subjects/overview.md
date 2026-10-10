# Admin — Subjects

**Route:** `/admin/subjects`  
**Servlet:** `SubjectServlet`  
**Service:** `AdminService` (SubjectDao)  
**Page:** [subjects.jsp](../../pages/admin/subjects.md)  
**Model:** [Subject](../../models/Subject.md)

## Actions

| Action | Description |
|--------|-------------|
| create | Add subject (`name`, `description`, Professional / Sub-Professional checkboxes) |
| update | Edit existing subject by `id` (including level flags) |
| delete | Remove subject by `id` |

Create/update require at least one of Professional or Sub-Professional.

## Access

Requires authenticated `Role.ADMIN` (enforced by `JwtAuthFilter`).
