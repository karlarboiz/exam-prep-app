# Components

Reusable patterns from `app.css`:

## Buttons

| Class | Style |
|-------|-------|
| `.btn` | Base |
| `.btn-primary` | Blue filled |
| `.btn-outline` | Border only |
| `.btn-danger` | Red filled |
| `.btn-sm` / `.btn-lg` | Size variants |

## Cards & lists

- `.card` — general content panel
- `.exam-card` — selectable exam on user dashboard
- `.question-card` — exam question block
- `.stat-card` — dashboard metric
- `.review-card` (+ `.correct` / `.incorrect`) — result review
- `.empty-state` — muted centered “no data”
- `.expired-panel` — subscription-expired messaging inside `.auth-card`

## Badges

- `.badge-success`, `.badge-muted`
- `.badge-admin`, `.badge-user`
- `.badge-IN_PROGRESS`, `.badge-COMPLETED`, `.badge-EXPIRED`

## Tables

- `.data-table` — full-width admin/user tables with header background `--color-bg`

## Exam / timer

- `.timer-bar`, `.timer-value`, `.timer-warning`, `.timer-expired`
- `.option-label`, `.option-letter`, `.options`
- `.score-circle`, `.score-value`, `.result-summary`
- `.error-page` — 403/404
- `.alert-warning` — amber warning alert (expired subscription)
- `.intro-modal`, `.intro-modal-panel`, `.intro-modal-backdrop` — placement diagnostic intro overlay
- `.exam-shell.is-intro-pending` — dims exam content while intro is visible
