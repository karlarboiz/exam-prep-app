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
| weekly | boolean | Weekly regimen template when true (sampled per user) |
| questionsPerSubject | Integer | Sample size per subject (diagnostic / weekly base quota) |
| subjectName | String | Join helper (display) |
| questionCount | int | Join helper for practice (`exam_questions` count) |

Practice questions are linked via `exam_questions`. Diagnostic and weekly/checkpoint runtime questions are sampled into `attempt_questions`. Weekly templates are excluded from the practice catalog (`ExamDao.findActive`). See [diagnostic](../features/diagnostic/overview.md) and [weekly regimen](../features/weekly-regimen/overview.md).
