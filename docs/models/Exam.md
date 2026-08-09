# Exam

**Source:** `com.examprep.model.Exam`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| subjectId | Long | FK to Subject (anchor for diagnostic; practice exam subject) |
| title | String | Exam name |
| durationMinutes | int | Timer length |
| active | boolean | Visible/usable if true |
| diagnostic | boolean | Placement diagnostic when true |
| questionsPerSubject | Integer | Sample size per subject (diagnostic only) |
| subjectName | String | Join helper (display) |
| questionCount | int | Join helper for practice (`exam_questions` count) |

Practice questions are linked via `exam_questions`. Diagnostic runtime questions are sampled into `attempt_questions` — see [diagnostic](../features/diagnostic/overview.md).
