# Admin — Exams

**Route:** `/admin/exams`  
**Servlet:** `ExamServlet`  
**Service:** `AdminService` (ExamDao)  
**Page:** [exams.jsp](../../pages/admin/exams.md)  
**Model:** [Exam](../../models/Exam.md)

## Actions

| Action | Description |
|--------|-------------|
| create | Create exam with subject, title, duration, active flag, and selected question IDs |
| update | Update exam fields and replace linked questions via `setExamQuestions` |
| delete | Delete exam by `id` |

## Edit mode

`GET /admin/exams?edit={id}` loads `editExam` and `selectedQuestionIds` for the form.

## Access

Requires `Role.ADMIN`.
