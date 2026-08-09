# Placement diagnostic page

**Path:** `WEB-INF/jsp/user/diagnostic.jsp`  
**Route:** `/user/diagnostic`  
**Feature:** [diagnostic flow](../../features/diagnostic/flow.md)

## Intro

On a fresh attempt, an `.intro-modal` covers the page for up to **10 seconds** (or until **Start now**): what the diagnostic is, timing rules, and that unfinished/expired attempts require a retake. A `?retake=1` visit shows an amber warning that the previous attempt did not count. After begin, the exam shell is interactive and dual timers start.

## Exam UI

One question at a time with Previous / Next, per-question and overall timers (same UX as take-exam). Shows subject name on each question. Submit confirms then posts to `/user/diagnostic`.
