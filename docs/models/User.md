# User

**Source:** `com.examprep.model.User`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| username | String | Unique |
| email | String | Unique |
| passwordHash | String | BCrypt (via PasswordUtil) |
| role | Role | ADMIN or USER |
| createdAt | LocalDateTime | Registration time |
| diagnosticCompletedAt | LocalDateTime | Set only when placement diagnostic status is `COMPLETED`; null until then (expired/abandoned do not set it) |

## Helpers

- `isAdmin()` → `role == Role.ADMIN`
- `isDiagnosticCompleted()` → `diagnosticCompletedAt != null`
