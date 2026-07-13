# Results & History

## History

**Route:** `/user/history`  
**Servlet:** `HistoryServlet`  
**Service:** `ExamService.getUserHistory`  
**Page:** [history.jsp](../../pages/user/history.md)

Lists the current user's exam attempts with status, score, and timestamps.

## Result detail

**Route:** `/user/result?attemptId=N`  
**Servlet:** `ResultsServlet`  
**Service:** `ExamService.getAttempt` + `getAttemptAnswers`  
**Page:** [result.jsp](../../pages/user/result.md)

Shows score percent, attempt status, and per-question review (selected vs correct).

## Access

Authenticated `USER` (or any logged-in user reaching these routes). Ownership checks apply on attempt IDs where enforced by the servlet.
