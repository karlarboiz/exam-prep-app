# Question

**Source:** `com.examprep.model.Question`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| subjectId | Long | FK to Subject |
| prompt | String | Question text |
| optionA–D | String | Four choices. Admin edit can reassign which text is A–D by dragging; stored columns stay `option_a`–`option_d`. Per-attempt display shuffle (`OptionShuffle`) is separate. |
| correctOption | String | `"A"` / `"B"` / `"C"` / `"D"`. On drag-reorder, the key follows the option text. |
| difficulty | String | e.g. easy/medium/hard |
| explanation | String | Why the correct answer is right; optional for manual CRUD, required on Excel import |
| imageUrl | String | Optional diagram URL |
| batchLabel | String | Import batch (≤ 100). Excel imports default to `cse-import-YYYY-MM-DD`. Null/blank for manual or unlabeled items. Upsert key with subject + prompt. |
| subjectName | String | Join helper |

## Helpers

- `getOptionText(option)` returns the text for letter A–D.
