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

## Helpers

- `isAdmin()` → `role == Role.ADMIN`
