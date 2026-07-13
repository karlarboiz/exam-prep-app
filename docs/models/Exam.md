# Exam

**Source:** `com.examprep.model.Exam`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| subjectId | Long | FK to Subject |
| title | String | Exam name |
| durationMinutes | int | Timer length |
| active | boolean | Visible to students if true |
| subjectName | String | Join helper (display) |
| questionCount | int | Join helper (display) |

Questions are linked separately via exam–question association (see AdminService.createExam / updateExam).
