# Weekly regimen flow

## Week clock

1. Anchor = `diagnostic_completed_at`. Horizon = latest redeemed grant `expires_at`.
2. `totalWeeks = max(1, floor(days / 7))`. Current week = days since anchor / 7 + 1, capped at total.
3. Last week ends at grant expiry (readiness exam, even subject mix).
4. On resolve: any earlier `OPEN` week with no official attempt is marked `MISSED`. The current week row is created if missing and its form is sampled into `weekly_form_questions`.

## Official weekly exam

- Route: `GET/POST /user/weekly`
- Start copies the week’s form into `attempt_questions` (`attempt_kind = WEEKLY`).
- First terminal submit (`COMPLETED` or `EXPIRED`) sets `weekly_regimens.official_attempt_id` and subject bands.
- A second start throws. A second submit does not overwrite the official mark.

## Study plan + email

- Route: `GET /user/study-plan` (optional `regimenId`)
- Bands + 3–5 targets + misses. Lives in the app even if mail is never opened.
- After official submit, `MailService` writes `email_outbox` and logs, with a link back to the study plan. Skipped when the grant is expired or revoked.

## Review

- Route: `GET /user/review`
- Untimed replay of misses + explanations. No `exam_attempts` row. Official score unchanged.

## Checkpoint

- Route: `GET/POST /user/checkpoint`
- After official submit, before `week_end`, if at least `weekly.checkpoint.min.fresh` unused items exist on WEAK / DEVELOPING subjects.
- `attempt_kind = CHECKPOINT`. Does not set `official_attempt_id`.

## Follow-up email

Sent by `WeeklyRegimenService` on dashboard resolve, and by `FollowUpMailJob` (first pass about a minute after startup, then every `weekly.followup.interval.hours`).

Each kind is stored once in `regimen_follow_ups` and written to `email_outbox` (SMTP when `mail.smtp.host` is set). Skipped when the latest redeemed grant is expired, or the user has no email.

| Kind | When |
|------|------|
| `WEEKLY_EXAM_DUE` | Week still open, no official score, and now is at or after the follow-up point (`weekly.followup.after.days`, or the midpoint of a shorter week) |
| `WEEKLY_EXAM_MISSED` | No official score and the week is `MISSED` or past `week_end`. Sent even if a due email already went out |
| `CHECKPOINT_DUE` | Official exam is in, no completed or expired checkpoint, the bank still has `weekly.checkpoint.min.fresh` unused weak/developing items, and the follow-up point has passed while the week is open |
| `CHECKPOINT_MISSED` | Same requirement, but `week_end` has passed. Still sent after a due email, including when a later week has used the spare questions |

The dashboard shows a short hint with the address that was emailed.

## Dashboard

`/user/dashboard` primary CTA is this week’s exam, study plan, review, or checkpoint. Practice exams are secondary (“Optional practice”).
