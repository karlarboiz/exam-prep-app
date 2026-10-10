# Examinee tracking

Integrity signals while a student is on an `IN_PROGRESS` practice or diagnostic attempt. The goal is **not** live proctoring or seeing what they opened. It is to record when they leave the exam page — especially on a hard item they have not answered yet — so an admin can review the pattern.

Today the take-exam and diagnostic pages show one question at a time, save answers over AJAX, and keep both timers running in the browser. Questions already have `difficulty` (`EASY` / `MEDIUM` / `HARD`). There is no visibility, blur, or leave logging. The site header still offers Dashboard, History, Account, and Logout during an exam, so leaving is possible without changing browser tabs.

| Feature | Status | Notes |
|---------|--------|-------|
| Tab / window leave detection (practice exam) | Done | `js/exam-tracking.js` on [take-exam.jsp](../pages/user/take-exam.md) — Page Visibility + blur/focus after the first question is showing |
| Tab / window leave detection (diagnostic) | Done | Same script on [diagnostic.jsp](../pages/user/diagnostic.md) after `action=begin`. Intro is not counted (`integrity_tracking` starts false) |
| Leave / return event persist | Done | `attempt_behavior_events`: attempt, question, `LEAVE`/`RETURN`, time, answered?, remaining ms, duration, suspect. Cascade-delete with the attempt |
| Hard-item leave flag | Done | `BehaviorIntegrity`: unanswered `HARD` only. Answered or EASY/MEDIUM stored, not flagged |
| Time-away duration | Done | `RETURN.away_duration_ms` is elapsed since the unpaired `LEAVE` |
| In-app leave (header / logout) | Done | Header/logo clicks plus `pagehide` / `beforeunload` send `LEAVE`. Resume GET records `RETURN` if last event was a leave |
| Return warning to examinee | Done | Overlay: stay on this page; leave was logged; leave count. Timers keep running |
| Pre-exam disclosure | Done | Practice header hint + diagnostic intro bullet |
| Integrity policy (warn vs limit) | Done | Warn-only. No auto-submit or lock after N leaves |
| Event ingest (CSRF, owner-only) | Done | `POST action=behavior` on `/user/exam` and `/user/diagnostic`. CSRF, owner, `IN_PROGRESS`. Duplicate leave ignored. After submit ignored |
| Integrity summary on submit | Done | `leave_count` / `suspect_leave_count` on `exam_attempts`, refreshed on each event and on submit |
| Admin attempt integrity view | Done | [integrity-detail](../pages/admin/integrity.md) timeline |
| Flagged attempts list (admin) | Done | `/admin/integrity` — `suspect_leave_count > 0`. Header + dashboard link |
| Student result stays score-only | Done | Result pages unchanged; flags are admin-only |
| Timers keep running while away | Done | Tracking script does not pause overall or per-question timers |
| Tests for flag rules | Done | `BehaviorIntegrityTest`, `BehaviorTrackingServiceTest` |
