# Admin — Exams

**Route:** `/admin/exams`  
**Servlet:** `ExamServlet`  
**Service:** `AdminService` (ExamDao)  
**Page:** [exams.jsp](../../pages/admin/exams.md)  
**Model:** [Exam](../../models/Exam.md)

## Actions

| Action | Description |
|--------|-------------|
| create | Create practice or diagnostic exam |
| update | Update exam fields; practice replaces linked questions; diagnostic clears `exam_questions` |
| delete | Delete exam by `id` |

## Practice vs diagnostic

| | Practice | Diagnostic |
|--|----------|------------|
| Flag | `is_diagnostic = false` | `is_diagnostic = true` |
| Questions | Explicit `exam_questions` multi-select | Sampled at runtime (`questions_per_subject`) |
| Student list | Shown on dashboard when active | Hidden from practice list; used by placement gate |
| Active uniqueness | Many allowed | Saving an **active** diagnostic deactivates other diagnostics |

Subject on a diagnostic exam is an anchor FK only; sampling uses **all** subjects with questions in the bank.

## Edit mode

`GET /admin/exams?edit={id}` loads `editExam` and `selectedQuestionIds` for the form.

## Access

Requires `Role.ADMIN`.
