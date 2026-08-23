# Weekly regimen

Study cadence after the placement diagnostic: **one scored exam per week**, a study plan from that result, review during the week, then a **new** exam the following week.

The goal is to accumulate knowledge and raise proficiency on exam-critical (weak, high-weight) subjects — not to grind the same paper until the score looks good.

**In scope:** weekly form assignment, one official score per form, in-app study plan, email digest that links back into the app, unlimited untimed review of that week’s misses, optional mid-week checkpoint on *fresh* questions, next-week sampling that over-weights last week’s WEAK / DEVELOPING subjects.

**Out of scope:** unlimited retakes of the same weekly exam (that measures memory of the form, not the subject). Same-form retakes must not replace the official week score.

Depends on diagnostic bands (`diagnostic_subject_scores`) and existing practice attempt / result review. Digest is stored in `email_outbox` and logged (no SMTP). Cadence fits the one-time access grant window (`expires_at`).

| Feature | Status | Notes |
|---------|--------|-------|
| Week clock aligned to access grant | Done | `WeekClock`: week 1 at diagnostic complete; `floor(days to expires_at / 7)`; last week is mixed readiness |
| Diagnostic bands seed week 1 | Done | `diagnostic_subject_scores` drive week 1 quotas and the first study plan |
| Weekly exam assignment (new form) | Done | Sampled into `weekly_form_questions` / `attempt_questions`. Template exam `is_weekly` |
| One official scored attempt per weekly form | Done | `official_attempt_id` set once; start after lock throws; submit does not overwrite |
| In-app study plan from misses | Done | `/user/study-plan` — bands, 3–5 targets, misses |
| Email digest of study plan | Done | `MailService` → `email_outbox`; SMTP when `mail.smtp.host` is set; skipped after grant expiry/revoke |
| Unlimited untimed review of week’s misses | Done | `/user/review` — no attempt row, no official score write |
| Optional mid-week checkpoint (fresh items) | Done | `/user/checkpoint`; skipped when bank &lt; `weekly.checkpoint.min.fresh` |
| Next-week form over-samples weak subjects | Done | `QuestionSampler.quotas` WEAK &gt; DEVELOPING &gt; STRONG; even mix on final week |
| Dashboard “this week” instead of open catalog | Done | Primary CTA is regimen; practice exams are “Optional practice” |
| Carry-forward if they miss a week | Done | `MISSED` when clock rolls with no official submit; last plan kept; next form unlocks |
| Tests for week lock, sampling, and review | Done | `WeekClockTest`, `QuestionSamplerTest`, `WeeklyRegimenServiceTest` |
