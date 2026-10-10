# WeeklyRegimen

**Source:** `com.examprep.model.WeeklyRegimen`  
**Table:** `weekly_regimens`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| userId | Long | Owner |
| weekNumber | int | 1-based from diagnostic completion |
| weekStart / weekEnd | LocalDateTime | Last week ends at grant `expires_at` |
| status | WeeklyRegimenStatus | `OPEN` / `COMPLETED` / `MISSED` |
| finalWeek | boolean | Mixed readiness exam |
| officialAttemptId | Long | First terminal weekly submit; never overwritten |
| emailSentAt | LocalDateTime | Digest written to `email_outbox` |

Unique `(user_id, week_number)`. Form items: `weekly_form_questions`.
