# Role

**Source:** `com.examprep.model.Role` (enum)

| Value | Meaning |
|-------|---------|
| ADMIN | Access to `/admin/**` |
| USER | Student; take exams, history, results |

`fromString(String)` → `Role.valueOf(value.toUpperCase())`.
