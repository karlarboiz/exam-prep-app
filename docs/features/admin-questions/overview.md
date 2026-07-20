# Admin — Questions

**Route:** `/admin/questions`  
**Servlet:** `QuestionServlet`  
**Service:** `AdminService` (QuestionDao)  
**Page:** [questions.jsp](../../pages/admin/questions.md)  
**Model:** [Question](../../models/Question.md)

## Actions

CRUD for multiple-choice questions: prompt, options A–D, correct option, difficulty, optional explanation, and subject.

Questions are later attached to exams (many-to-many via exam–question links).

Bulk Excel import: see [question-import](../question-import/overview.md).

## Access

Requires `Role.ADMIN`.
