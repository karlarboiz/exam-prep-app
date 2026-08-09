# Diagnostic result page

**Path:** `WEB-INF/jsp/user/diagnostic-result.jsp`  
**Route:** `/user/diagnostic/result?attemptId=`  
**Feature:** [diagnostic overview](../../features/diagnostic/overview.md), [rubric](../../features/diagnostic/rubric.md)

Shows overall score, readiness label, mean subject %, per-subject band table, and answer review. Primary CTA continues to the dashboard (gate cleared after a `COMPLETED` submit).

If the attempt is not `COMPLETED` and the user has not cleared the gate, the servlet redirects to `/user/diagnostic?retake=1`.
