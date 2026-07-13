# Admin — Subjects

**Route:** `/admin/subjects`  
**Servlet:** `SubjectServlet`  
**Service:** `AdminService` (SubjectDao)  
**Page:** [subjects.jsp](../../pages/admin/subjects.md)  
**Model:** [Subject](../../models/Subject.md)

## Actions

| Action | Description |
|--------|-------------|
| create | Add subject (`name`, `description`) |
| update | Edit existing subject by `id` |
| delete | Remove subject by `id` |

## Access

Requires authenticated `Role.ADMIN` (enforced by `JwtAuthFilter`).
