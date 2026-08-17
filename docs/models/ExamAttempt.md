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
| diagnostic | boolean | Joined from `exams.is_diagnostic`; drives history links |
| weekly | boolean | Joined from `exams.is_weekly` |
| attemptKind | AttemptKind | PRACTICE / DIAGNOSTIC / WEEKLY / CHECKPOINT |
| regimenId | Long | Weekly / checkpoint owner week |
| leaveCount | int | Cached count of `LEAVE` events; refreshed on each event and on submit |
| suspectLeaveCount | int | Cached count of suspect leaves (unanswered HARD). Admin flagged list uses `> 0` |
| integrityTracking | boolean | When false, ingest ignores events. Diagnostic starts false until intro `begin` |
| username | String | Join helper for admin integrity views |

Deadline = `startedAt.plusMinutes(durationMinutes)` (`COALESCE(duration_minutes_override, exams.duration_minutes)`).

For **diagnostic** attempts, the question set is `attempt_questions`, not `exam_questions`. Subject breakdown is stored in [DiagnosticSubjectScore](DiagnosticSubjectScore.md). Only `COMPLETED` clears the placement gate; `EXPIRED` requires a retake — see [diagnostic flow](../features/diagnostic/flow.md).

History must route diagnostic Continue / View Result to `/user/diagnostic` and `/user/diagnostic/result` respectively — see [history](../features/results-history/history.md).
