# Take exam page

**Path:** `WEB-INF/jsp/user/take-exam.jsp`  
**Route:** `/user/exam`  
**Feature:** [take-exam flow](../../features/take-exam/flow.md)


Exam header, countdown `.timer-bar`, question cards with radio options, submit action.

One question at a time with progress (`Question X of Y`), dual timers (per-question share and overall exam), Prev/Next navigation, and submit on the last question. Per-question remaining time is preserved when navigating. Selecting an option saves via AJAX.

A disclosure line states that leaving the page is recorded. Tab/window leave and header navigation are posted as `action=behavior`. Returning shows a stay-on-this-page overlay with the attempt leave count. Timers do not pause while away.
