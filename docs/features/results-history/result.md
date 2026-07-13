# Result page feature notes

See [history.md](history.md) for routes and servlets.

Result view highlights:

- Score circle / summary from `ExamAttempt.scorePercent`
- Status badge: `COMPLETED`, `EXPIRED`, or redirected away if still `IN_PROGRESS`
- Review cards use [AttemptAnswer](../../models/AttemptAnswer.md) + nested [Question](../../models/Question.md)
