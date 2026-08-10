# Question

**Source:** `com.examprep.model.Question`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| subjectId | Long | FK to Subject |
| prompt | String | Question text |
| optionA–D | String | Four choices |
| correctOption | String | `"A"` / `"B"` / `"C"` / `"D"` |
| difficulty | String | e.g. easy/medium/hard |
| explanation | String | Why the correct answer is right; optional for manual CRUD, required on Excel import |
| subjectName | String | Join helper |

## Helpers

- `getOptionText(option)` returns the text for letter A–D.
