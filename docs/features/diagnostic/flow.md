# Diagnostic flow

**Routes:** `/user/diagnostic`, `/user/diagnostic/result`  
**Servlets:** `DiagnosticServlet`, `DiagnosticResultServlet`  
**Service:** `DiagnosticService`

## Start / resume

```
GET /user/diagnostic
  → if diagnostic_completed_at set → /user/dashboard
  → else startDiagnostic(userId)
       resolve active is_diagnostic exam
       resume IN_PROGRESS if not past deadline
       if IN_PROGRESS past deadline → submit as EXPIRED (no gate clear) then create new attempt
       else create attempt + sample attempt_questions
  → redirect ?attemptId=N (&retake=1 when forcing a new attempt)

GET /user/diagnostic?attemptId=N
  → if COMPLETED → result
  → if EXPIRED / not in progress → /user/diagnostic?retake=1
  → if past deadline → submit EXPIRED → retake
  → else show diagnostic.jsp
       showIntro when no answers yet (intro modal)
```

## Intro modal

On a fresh attempt (no saved answers):

1. Overlay explains purpose, timing, and that unfinished/expired attempts require a retake.
2. Countdown **10 seconds** (or **Start now**).
3. `POST action=begin` (AJAX) → `beginDiagnostic` resets `started_at` if still unanswered, returns new `deadline`.
4. Modal hides; overall + per-question timers start.

Resume with existing answers skips the intro and continues the existing deadline.

## Sampling

For each subject with ≥ 1 question, sample up to `questions_per_subject` IDs (or all if fewer). Prefer a difficulty mix (round-robin EASY / MEDIUM / HARD, fill remainder randomly). Subjects with zero questions are omitted (not assessed). Persist as `attempt_questions (attempt_id, question_id, sort_order)`.

## Answer / submit

```
POST action=begin   → beginDiagnostic (start clock after intro)
POST action=answer  → saveAnswer (AJAX supported)
POST action=submit  → submitDiagnostic
  → overall score from attempt_questions size
  → diagnostic_subject_scores snapshot + bands
  → if COMPLETED → users.diagnostic_completed_at = now → /user/diagnostic/result
  → if EXPIRED   → do not set diagnostic_completed_at → /user/diagnostic?retake=1
```

Unanswered count as incorrect.

## AFK / timeout → retake

| Situation | Result |
|-----------|--------|
| Leaves mid-exam and returns before deadline | Resume `IN_PROGRESS` (no intro if answers exist) |
| Time runs out or returns after deadline | Attempt `EXPIRED`, gate stays open, new attempt required |
| Result URL for non-COMPLETED while gate incomplete | Redirect to retake |

## Hard gate

`DiagnosticFilter` reads completion from DB (`UserDao.isDiagnosticCompleted`), not only the request user attribute. Login/register redirect incomplete USERs to `/user/diagnostic`.
