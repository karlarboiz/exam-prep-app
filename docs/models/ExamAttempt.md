# ExamAttempt

**Source:** `com.examprep.model.ExamAttempt`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| userId | Long | Who is taking it |
| examId | Long | Which exam |
| startedAt | LocalDateTime | Timer start |
| completedAt | LocalDateTime | When finished |
| scorePercent | BigDecimal | 0–100 |
| status | AttemptStatus | IN_PROGRESS / COMPLETED / EXPIRED |
| examTitle | String | Display helper |
| subjectName | String | Display helper |
| durationMinutes | int | Copied/joined from exam for deadline checks |

Deadline = `startedAt.plusMinutes(durationMinutes)`.
