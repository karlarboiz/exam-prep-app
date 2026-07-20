# Take Exam

**Route:** `/user/exam`  
**Servlet:** `TakeExamServlet`  
**Service:** `ExamService`  
**Page:** [take-exam.jsp](../../pages/user/take-exam.md)

## Flow

```
GET ?examId=N
  → startExam (reuse IN_PROGRESS or create new)
  → redirect ?attemptId=N

GET ?attemptId=N
  → if not IN_PROGRESS or expired → redirect to result
  → else show questions + timer

POST action=answer
  → saveAnswer for one question

POST action=submit
  → submitExam with all answers
  → redirect /user/result?attemptId=N
```

## UI timing

- Shows one question at a time; Previous / Next allowed.
- Per-question budget: `max(1, (durationMinutes × 60) / questionCount)` seconds; remaining time is preserved per question when navigating.
- Question timeout auto-advances; on the last question it auto-submits.
- Overall deadline (`startedAt + durationMinutes`) still forces submit.

## Rules

- Only the owning user may view/submit the attempt (403 otherwise).
- Exam must be `active` to start.
- Diagnostic exams (`is_diagnostic`) are **not** started via this route — use [diagnostic](../diagnostic/overview.md). `findActive()` excludes them from the dashboard list.
- Questions come from `exam_questions` (fixed set).
- Duration: `startedAt + durationMinutes`. Past deadline marks `EXPIRED` and still scores saved answers.
- Score = (correct answers / total questions) × 100, 2 decimal places.
