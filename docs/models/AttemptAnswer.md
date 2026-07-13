# AttemptAnswer

**Source:** `com.examprep.model.AttemptAnswer`

| Field | Type | Notes |
|-------|------|-------|
| attemptId | Long | Parent attempt |
| questionId | Long | Question answered |
| selectedOption | String | A–D |
| correct | Boolean | Whether it matched correctOption |
| question | Question | Optional nested for review UI |

Composite identity conceptually: (attemptId, questionId).
