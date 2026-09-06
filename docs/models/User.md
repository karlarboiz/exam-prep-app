# User

**Source:** `com.examprep.model.User`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| username | String | Unique. Self-editable on `/account` (max 50). |
| email | String | Unique. Self-editable on `/account` (max 100). |
| passwordHash | String | BCrypt (via PasswordUtil) |
| role | Role | ADMIN or USER |
| examLevel | ExamLevel | PROFESSIONAL or SUB_PROFESSIONAL for learners; null for admin. Admin users page can change this; changing a student's level clears diagnostic completion. |
| createdAt | LocalDateTime | Registration time |
| diagnosticCompletedAt | LocalDateTime | Set only when placement diagnostic status is `COMPLETED`; null until then (expired/abandoned do not set it) |
| locale | AppLocale | UI language (`tl` or `en`). Default Tagalog. Synced from the language switcher when the user is signed in. |
| tokenVersion | int | Incremented on password change or reset. JWT `tv` claim must match or the session is rejected. |

## Helpers

- `isAdmin()` → `role == Role.ADMIN`
- `isDiagnosticCompleted()` → `diagnosticCompletedAt != null`
