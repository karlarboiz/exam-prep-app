# AttemptBehaviorEvent

**Source:** `com.examprep.model.AttemptBehaviorEvent`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| attemptId | Long | Parent [ExamAttempt](ExamAttempt.md) |
| questionId | Long | Question showing when the event fired |
| eventType | BehaviorEventType | `LEAVE` or `RETURN` |
| occurredAt | LocalDateTime | Server time |
| questionAnswered | boolean | Whether that question already had a saved option |
| remainingQuestionMs | Integer | Per-question timer remaining, from the client |
| awayDurationMs | Integer | Set on `RETURN` — time since the unpaired `LEAVE` |
| suspect | boolean | `LEAVE` on an unanswered `HARD` question — see [BehaviorIntegrity](../features/examinee-tracking/overview.md) |
| questionDifficulty | String | Copied from the question at event time |
| questionPrompt | String | Join helper for admin timeline |
| questionNumber | Integer | `sort_order` from `exam_questions` or `attempt_questions` |

`getAwayDurationLabel()` formats duration for admin tables (`<1s`, `12s`, `1m 4s`).
