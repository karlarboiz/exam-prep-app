# Weekly regimen

**Purpose:** After the placement diagnostic, students follow a weekly cycle: one official scored exam, an in-app study plan (plus email digest), untimed review of misses, optional mid-week checkpoint on fresh items, then a **new** form the following week.

**Service:** `WeeklyRegimenService`  
**Sampler / clock:** `QuestionSampler`, `WeekClock`  
**Mail:** `MailService` (writes `email_outbox`; logs; skips after grant expiry)  
**Pages:** [dashboard](../../pages/user/dashboard.md), [study-plan](../../pages/user/study-plan.md), [review](../../pages/user/review.md), take-exam reused for `/user/weekly` and `/user/checkpoint`

## Locked rules

- Week 1 starts at `users.diagnostic_completed_at`. Total weeks = `floor(days until grant expires_at / 7)`, at least 1. Last week is a mixed readiness exam.
- Diagnostic bands seed week 1 sampling and the first study plan.
- One official score per weekly form. Same form cannot be retaken as a scored exam that overwrites that mark.
- Review is untimed and does not write an official score.
- Checkpoints use new questions, do not replace the week mark, and are skipped when the bank is too thin.
- Next week over-samples last week’s WEAK / DEVELOPING subjects and keeps a STRONG maintenance slice (even mix on the final week).
- If a week ends without a submit, it is marked **MISSED**. The last study plan remains; the next form unlocks only when the clock rolls.

See [flow](flow.md). Tracker: [weekly-regimen](../../feature-tracker/weekly-regimen.md).
