# Admin — Questions

**Route:** `/admin/questions`  
**Servlet:** `QuestionServlet`  
**Service:** `AdminService` (QuestionDao), `QuestionImportService`  
**Page:** [questions.jsp](../../pages/admin/questions.md)  
**Model:** [Question](../../models/Question.md)

## Actions

CRUD for multiple-choice questions: prompt, options A–D, correct option, difficulty, optional explanation, and subject.

On the edit form, option rows are drag-and-drop (grip handle, or arrow keys on the handle). Letters stay **A–D in visual order**; the option text moves. The **correct option letter follows the moved text** (if C was keyed and that row is dragged to the first slot, the key becomes A). Saving still posts `optionA`–`optionD` and `correctOption` as today. Manual typing of option text is unchanged.

Questions are later attached to exams (many-to-many via exam–question links).

| HTTP | Action | Description |
|------|--------|-------------|
| POST | create / update / delete | Manual question CRUD (single-row delete) |
| POST | deleteBatch | Delete one or more selected questions (`ids`) |
| POST | import | Multipart `.xlsx` upload; `batchLabel` defaults to `cse-import-YYYY-MM-DD` |
| GET | `action=template` | Download sample import workbook |
| GET | `action=export` | Download current bank (optional `subjectId` / `batchLabel` filters) |

Batch delete uses the same cascade as a single delete (`exam_questions`, attempt answers/links, weekly regimen links). Subject and batch filters are preserved on redirect. A success banner shows the number removed (`?deleted=N`). Selecting none returns a validation error.

Bulk Excel import/export: see [question-import](../question-import/overview.md). When import creates a missing subject, both Professional and Sub-Professional flags default to true so the subject is not hidden from dashboards. Re-import matches on **batch label + subject + prompt** (case-insensitive) and updates only items in that batch.

## Access

Requires `Role.ADMIN`.
