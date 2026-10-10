# Regimen follow-up

**Source:** `com.examprep.model.FollowUpKind`  
**Table:** `regimen_follow_ups`

One row per weekly regimen and kind. `WeeklyRegimenService` inserts the row only after `MailService` writes the email, so the same follow-up is not sent twice.

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| regimenId | Long | Week the follow-up is about |
| kind | FollowUpKind | `WEEKLY_EXAM_DUE`, `WEEKLY_EXAM_MISSED`, `CHECKPOINT_DUE`, `CHECKPOINT_MISSED` |
| sentAt | LocalDateTime | When the outbox row was written |

Unique `(regimen_id, kind)`.
