# AttemptAnswer

**Source:** `com.examprep.model.AttemptAnswer`

| Field | Type | Notes |
|-------|------|-------|
| attemptId | Long | Parent attempt |
| questionId | Long | Question in the exam set |
| selectedOption | String | A–D, or **null** when unanswered |
| correct | Boolean | Whether it matched `correctOption`; **false** when unanswered |
| question | Question | Optional nested for review UI |

Composite identity conceptually: (attemptId, questionId).

On submit, every exam question gets a row (answered or unanswered) so result review matches the score denominator.
