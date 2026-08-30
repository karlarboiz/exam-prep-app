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
| POST | create / update / delete | Manual question CRUD |
| POST | import | Multipart `.xlsx` upload; `batchLabel` defaults to `cse-import-YYYY-MM-DD` |
| GET | `action=template` | Download sample import workbook |
| GET | `action=export` | Download current bank (optional `subjectId` / `batchLabel` filters) |

Bulk Excel import/export: see [question-import](../question-import/overview.md). When import creates a missing subject, both Professional and Sub-Professional flags default to true so the subject is not hidden from dashboards. Re-import matches on **batch label + subject + prompt** (case-insensitive) and updates only items in that batch.

## Access

Requires `Role.ADMIN`.
