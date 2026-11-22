# Exam start options

**Path:** `WEB-INF/jsp/user/exam-start.jsp`  
**Route:** `GET /user/exam?examId=` (when no in-progress attempt)  
**Feature:** [take-exam flow](../../features/take-exam/flow.md)

Shows exam title, subject, question count, and duration. The examinee picks **Answer feedback**: after each question (`AFTER_EACH`) or after the exam (`AFTER_EXAM`, default). Submit posts `action=start` with CSRF and begins the attempt. Cancel returns to the dashboard.
