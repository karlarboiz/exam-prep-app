# Examinee tracking

Integrity signals while a student is on an `IN_PROGRESS` practice or diagnostic attempt. The app records when they leave the exam page and flags a leave if the visible question is unanswered and `HARD`. It does not see what they opened and does not pause timers.

**Policy:** warn-only. Leaves are logged and the student is told when they return. There is no auto-submit or lock after N leaves.

## Routes

| Actor | Route | Role |
|-------|-------|------|
| Student | `POST /user/exam` `action=behavior` | Practice ingest |
| Student | `POST /user/diagnostic` `action=behavior` | Diagnostic ingest (after intro) |
| Admin | `GET /admin/integrity` | Flagged attempts (`suspect_leave_count > 0`) |
| Admin | `GET /admin/integrity?attemptId=` | Per-attempt timeline |

Ingest requires CSRF (`_csrf` or `X-CSRF-Token`), the owning user, `IN_PROGRESS`, and `integrity_tracking = true`. Duplicate `LEAVE` while already away is ignored. Events after submit are ignored.

## Flag rule

`BehaviorIntegrity.isSuspectLeave(difficulty, answered)` is true only for unanswered `HARD`. EASY/MEDIUM and answered HARD leaves are stored but not flagged.

## Client

Shared script `js/exam-tracking.js` (Page Visibility, blur/focus, header/logo clicks, `pagehide` / `beforeunload`). Practice starts tracking when the first question is showing. Diagnostic starts tracking only after `action=begin` (intro is not counted). Timers keep running while away.

## Data

- `attempt_behavior_events` — source of truth
- `exam_attempts.leave_count` / `suspect_leave_count` — list cache, refreshed on each event and on submit

Student result pages stay score + answer review. Flags are admin-only.

## Pages

- [take-exam](../../pages/user/take-exam.md)
- [diagnostic](../../pages/user/diagnostic.md)
- [admin integrity](../../pages/admin/integrity.md)
