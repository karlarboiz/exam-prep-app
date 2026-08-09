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
  → persist an attempt_answers row for every exam question
       blank / missing → selected_option NULL, is_correct false
  → redirect /user/result?attemptId=N
```

## Rules

- Only the owning user may view/submit the attempt (403 otherwise).
- Exam must be `active` to start.
- Duration: `startedAt + durationMinutes`. Past deadline marks `EXPIRED` and still scores the full question set.
- Score = (correct answers / total questions) × 100, 2 decimal places. Unanswered count as incorrect.
- Result [answer review](../results-history/result.md) lists every question, including unanswered (“Not answered”).
