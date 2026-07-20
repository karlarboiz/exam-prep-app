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
       else create attempt + sample attempt_questions
  → redirect ?attemptId=N

GET /user/diagnostic?attemptId=N
  → if not IN_PROGRESS or expired → submit then result
  → else show diagnostic.jsp (one Q at a time, dual timers)
```

## Sampling

For each subject with ≥ 1 question, sample up to `questions_per_subject` IDs (or all if fewer). Prefer a difficulty mix (round-robin EASY / MEDIUM / HARD, fill remainder randomly). Subjects with zero questions are omitted (not assessed). Persist as `attempt_questions (attempt_id, question_id, sort_order)`.

## Answer / submit

```
POST action=answer  → saveAnswer (AJAX supported)
POST action=submit  → submitDiagnostic
  → overall score from attempt_questions size
  → diagnostic_subject_scores snapshot + bands
  → users.diagnostic_completed_at = now
  → redirect /user/diagnostic/result?attemptId=N
```

Unanswered count as incorrect. Expired attempts still score and complete the gate.

## Hard gate

`DiagnosticFilter` reads completion from DB (`UserDao.isDiagnosticCompleted`), not only the request user attribute. Login/register redirect incomplete USERs to `/user/diagnostic`.
