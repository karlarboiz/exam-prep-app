# User dashboard

**Path:** `WEB-INF/jsp/user/dashboard.jsp`  
**Route:** `/user/dashboard`  
**Servlet:** `DashboardServlet`

Primary card is **this week’s regimen** (start / continue official exam, study plan, review, optional checkpoint). Subject list and **optional practice** sit in `.grid-2`. Recent attempts stay full width below. Study-plan scores and recent-attempt tables use `.data-table` and stack into labeled rows at ≤768px.

Missed-week notice uses `.alert.alert-warning`. When a follow-up email has been sent, a `.hint` under that alert or on the week card names the address (`dashboard.followUpExam`, `dashboard.followUpExamMissed`, `dashboard.followUpCheckpoint`, `dashboard.followUpCheckpointMissed`). Practice exams do not replace the official week score.
