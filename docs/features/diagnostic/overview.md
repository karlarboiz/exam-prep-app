# Diagnostic (placement)

**Purpose:** After first login (with active subscription), every `USER` must complete a placement diagnostic before using the dashboard or practice exams.

**Service:** `DiagnosticService`  
**Filter:** `DiagnosticFilter` (after `SubscriptionFilter`)  
**Pages:** [diagnostic.jsp](../../pages/user/diagnostic.md), [diagnostic-result.jsp](../../pages/user/diagnostic-result.md)

## Locked rules

- **Hard gate** — incomplete users may only hit `/user/diagnostic` and `/user/diagnostic/result` under `/user/**`.
- **Sampled content** — on start, pick **N questions per subject** from the bank (default N = 5); not a fixed `exam_questions` list.
- **Complete to pass** — resume `IN_PROGRESS` while time remains; only `COMPLETED` sets `users.diagnostic_completed_at`. `EXPIRED` / abandoned attempts are scored for history but **do not** clear the gate — the user must retake.
- **Intro before clock** — a 10-second info modal runs before timers start; `POST action=begin` resets `started_at` when there are no answers yet so intro time does not consume exam duration.
- **Grain** — subject + difficulty mix only (no topics).

## Configuration

Admins create/activate a diagnostic exam (`is_diagnostic = true`) with `questions_per_subject` and duration. At most one **active** diagnostic exam is allowed. Practice exams exclude diagnostics from `ExamDao.findActive()`.

See [flow](flow.md) and [rubric](rubric.md).
