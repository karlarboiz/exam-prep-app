# Admin — Questions

**Route:** `/admin/questions`  
**Servlet:** `QuestionServlet`  
**Service:** `AdminService` (QuestionDao), `QuestionImportService`  
**Page:** [questions.jsp](../../pages/admin/questions.md)  
**Model:** [Question](../../models/Question.md)

## Actions

CRUD for multiple-choice questions: prompt, options A–D, correct option, difficulty, optional explanation, and subject.

Questions are later attached to exams (many-to-many via exam–question links).

| HTTP | Action | Description |
|------|--------|-------------|
| POST | create / update / delete | Manual question CRUD (single-row delete) |
| POST | deleteBatch | Delete one or more selected questions (`ids`) |
| POST | import | Multipart `.xlsx` upload |
| GET | `action=template` | Download sample import workbook |
| GET | `action=export` | Download current bank (optional `subjectId` filter) |

Batch delete uses the same cascade as a single delete (`exam_questions`, attempt answers/links, weekly regimen links). The subject filter is preserved on redirect. A success banner shows the number removed (`?deleted=N`). Selecting none returns a validation error.

Bulk Excel import/export: see [question-import](../question-import/overview.md). When import creates a missing subject, both Professional and Sub-Professional flags default to true so the subject is not hidden from dashboards. Re-import matches on subject + prompt (case-insensitive) and updates the existing question.

## Access

Requires `Role.ADMIN`.
