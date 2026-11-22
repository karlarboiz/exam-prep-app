# Results & History

## History

**Route:** `/user/history`  
**Servlet:** `HistoryServlet`  
**Service:** `ExamService.getUserHistory`  
**Page:** [history.jsp](../../pages/user/history.md)

Lists the current user's exam attempts with `AttemptKind`, status, score, and timestamps.

### Routing by attempt type

| Attempt | Status | Link |
|---------|--------|------|
| Practice | not `IN_PROGRESS` | `/user/result?attemptId=…` |
| Practice | `IN_PROGRESS` | `/user/exam?attemptId=…` |
| Diagnostic | not `IN_PROGRESS` | `/user/diagnostic/result?attemptId=…` |
| Diagnostic | `IN_PROGRESS` | `/user/diagnostic?attemptId=…` |
| Weekly | not `IN_PROGRESS` | `/user/study-plan?regimenId=…` |
| Weekly | `IN_PROGRESS` | `/user/weekly?attemptId=…` |
| Checkpoint | not `IN_PROGRESS` | `/user/result?attemptId=…` |
| Checkpoint | `IN_PROGRESS` | `/user/checkpoint?attemptId=…` |

## Result detail

**Route:** `/user/result?attemptId=N`  
**Servlet:** `ResultsServlet`  
**Service:** `ExamService.getAttempt` + `getAttemptAnswers`  
**Page:** [result.jsp](../../pages/user/result.md)

Shows score percent, attempt status, and per-question review (selected vs correct) for **practice** exams. Diagnostic results use `/user/diagnostic/result`.

## Access

Authenticated `USER` (or any logged-in user reaching these routes). Ownership checks apply on attempt IDs where enforced by the servlet.
