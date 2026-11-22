# AnswerRevealMode

**Source:** `com.examprep.model.AnswerRevealMode`

When a practice exam attempt shows correct answers.

| Value | Meaning |
|-------|---------|
| `AFTER_EACH` | After the examinee answers an item, show correct/incorrect, the correct option, and explanation |
| `AFTER_EXAM` | Keep answers hidden until submit; full review on the result page |

Chosen on the start screen before the attempt is created and stored on [ExamAttempt](ExamAttempt.md). Resume keeps the mode already saved. Diagnostic attempts keep the default `AFTER_EXAM` (result review only).
