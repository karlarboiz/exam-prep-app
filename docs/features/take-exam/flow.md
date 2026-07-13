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

## Rules

- Only the owning user may view/submit the attempt (403 otherwise).
- Exam must be `active` to start.
- Duration: `startedAt + durationMinutes`. Past deadline marks `EXPIRED` and still scores saved answers.
- Score = (correct answers / total questions) × 100, 2 decimal places.
