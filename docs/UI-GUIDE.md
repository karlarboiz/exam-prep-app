# UI guide

Single front door for visual and interaction conventions. Source of truth for CSS: `src/main/webapp/css/app.css`.

Detailed splits (same content, deeper): [colors](ui-rules/colors.md) · [layout](ui-rules/layout.md) · [forms](ui-rules/forms.md) · [components](ui-rules/components.md)

Pre-implementation order: [PRE-IMPLEMENTATION.md](PRE-IMPLEMENTATION.md)

---

## Principles

1. **One stylesheet** — add rules to `app.css`; do not invent a second theme file.
2. **Tokens first** — use CSS variables (`--color-*`, `--radius`, `--shadow`); avoid hard-coded hex in new markup unless defining a token.
3. **Reuse classes** — prefer `.card`, `.btn-*`, `.alert-*`, `.data-table`, exam/result patterns over new one-offs.
4. **Shared chrome** — app pages include [header](pages/layout/header.md) and [footer](pages/layout/footer.md).
5. **One job per page** — title, short subtitle (`.subtitle`), then the primary interaction.

---

## Design tokens

Defined on `:root` in `app.css`:

| Token | Value | Use |
|-------|-------|-----|
| `--color-primary` | `#2563eb` | Links, logo, primary buttons |
| `--color-primary-dark` | `#1d4ed8` | Primary hover |
| `--color-danger` | `#dc2626` | Destructive, errors, expired |
| `--color-success` | `#16a34a` | Success, correct answers |
| `--color-warning` | `#d97706` | Timer warning, in-progress |
| `--color-bg` | `#f8fafc` | Page background |
| `--color-surface` | `#ffffff` | Cards, header, footer |
| `--color-border` | `#e2e8f0` | Borders / dividers |
| `--color-text` | `#1e293b` | Body text |
| `--color-muted` | `#64748b` | Secondary text |
| `--radius` | `8px` | Controls, cards |
| `--shadow` | `0 1px 3px rgba(0,0,0,.1)` | Soft elevation |

Typography today: system UI stack on `body`. Keep sizes consistent with existing `h1` / `h2` / `.subtitle` rather than introducing display fonts ad hoc.

---

## Page shell

```
body (column flex, min-height 100vh)
├── .site-header > .container > .header-inner  (logo + .header-nav)
├── .main-content > .container                 (page body, flex: 1)
└── .site-footer > .container                  (muted centered)
```

| Class | Role |
|-------|------|
| `.container` | Max-width **1100px**, horizontal padding `1.5rem` |
| `.main-content` | Grows so footer stays at bottom |
| `.grid-2` | Two columns; **stacks at ≤768px** |
| `.stats-grid` | Four-column admin/user stats |
| `.exam-grid` | Responsive cards `minmax(260px, 1fr)` |

---

## Buttons

| Class | When |
|-------|------|
| `.btn` | Base (always pair with a variant) |
| `.btn-primary` | Main CTA (start exam, save, submit) |
| `.btn-outline` | Secondary |
| `.btn-danger` | Delete / destructive |
| `.btn-sm` / `.btn-lg` | Density |

Group related actions with `.actions`. Table row deletes: `.inline-form`.

---

## Forms

- Field wrapper: `.form-group` (label above, full-width control).
- Auth screens: wrap in `.auth-card` (max-width 420px, centered).
- Errors: `.alert.alert-error` · Soft warnings: `.alert.alert-warning`.
- Helpers: `.hint` · Login/register switch: `.auth-link`.
- Register / admin mint token display: `.token-readonly` (monospace).
- Admin filters: `.filter-bar` · Multi-select questions: `.checkbox-list` / `.checkbox-item`.
- Practice exam order: `.exam-question-row` with Up/Down.
- Users admin row editors: `.table-inline-form`.

---

## Content patterns

| Pattern | Classes | Typical page |
|---------|---------|--------------|
| Generic panel | `.card` | Admin forms, settings blocks |
| Metric | `.stat-card` + `.stat-value` / `.stat-label` | Admin dashboard |
| Exam picker | `.exam-card` inside `.exam-grid` | User dashboard |
| Question block | `.question-card`, `.options`, `.option-label`, `.option-letter` | Take exam |
| Timer | `.timer-bar`, `.timer-value`, `.timer-warning`, `.timer-expired` | Take exam |
| Score | `.result-summary`, `.score-circle`, `.score-value` | Result |
| Review row | `.review-card` + `.correct` / `.incorrect` | Result |
| Table | `.data-table` | Admin lists, history |
| Empty | `.empty-state` | No exams / no token |
| Expired sub | `.expired-panel` inside `.auth-card` | Subscription expired |
| Errors | `.error-page` | 403 / 404 |

### Badges

`.badge-success`, `.badge-muted`, `.badge-admin`, `.badge-user`,  
`.badge-IN_PROGRESS`, `.badge-COMPLETED`, `.badge-EXPIRED`, `.badge-suspect`,  
`.badge-WEAK` / `.badge-DEVELOPING` / `.badge-STRONG`,  
`.badge-WEEKLY` / `.badge-CHECKPOINT` / `.badge-OPEN` / `.badge-MISSED`

---

## Screen map (JSP ↔ route)

Use existing pages as the visual reference before inventing new layouts.

### Auth & errors

| Screen | Route | Page doc |
|--------|-------|----------|
| Login | `/login` | [login](pages/auth/login.md) |
| Register | `/register` | [register](pages/auth/register.md) |
| Account | `/account` | [account](pages/auth/account.md) |
| 403 / 404 | error pages | [403](pages/error/403.md), [404](pages/error/404.md) |

### Student

| Screen | Route | Page doc |
|--------|-------|----------|
| Dashboard | `/user/dashboard` | [dashboard](pages/user/dashboard.md) |
| Take exam | `/user/exam` | [take-exam](pages/user/take-exam.md) |
| Weekly exam | `/user/weekly` | [take-exam](pages/user/take-exam.md) |
| Checkpoint | `/user/checkpoint` | [take-exam](pages/user/take-exam.md) |
| Study plan | `/user/study-plan` | [study-plan](pages/user/study-plan.md) |
| Review | `/user/review` | [review](pages/user/review.md) |
| Result | `/user/result` | [result](pages/user/result.md) |
| History | `/user/history` | [history](pages/user/history.md) |
| Subscription expired | `/user/subscription-expired` | [subscription-expired](pages/user/subscription-expired.md) |

### Admin

| Screen | Route | Page doc |
|--------|-------|----------|
| Dashboard | `/admin/dashboard` | [dashboard](pages/admin/dashboard.md) |
| Subjects | `/admin/subjects` | [subjects](pages/admin/subjects.md) |
| Questions | `/admin/questions` | [questions](pages/admin/questions.md) |
| Exams | `/admin/exams` | [exams](pages/admin/exams.md) |
| Users | `/admin/users` | [users](pages/admin/users.md) |
| Access grants | `/admin/access-grants` | [access-grants](pages/admin/access-grants.md) |
| Integrity | `/admin/integrity` | [integrity](pages/admin/integrity.md) |

### Layout partials

| Partial | Doc |
|---------|-----|
| Header | [header](pages/layout/header.md) |
| Footer | [footer](pages/layout/footer.md) |

---

## Checklist before shipping UI

- [ ] Uses tokens / existing classes from this guide
- [ ] Header + footer included (unless auth-card-only / error-page pattern already used)
- [ ] Primary action is a `.btn-primary`
- [ ] Error and empty states use `.alert-*` / `.empty-state`
- [ ] Tables use `.data-table`; exam lists use `.exam-grid`
- [ ] ≤768px: no unbroken two-column layout (use `.grid-2` or stack)
- [ ] Page note updated under `docs/pages/…` if markup contract changed

---

## Anti-patterns

- New purple/indigo gradient themes or dark-mode forks unless product asks
- Inline `style="color:#…"` when a token or badge exists
- Card-inside-card nesting that does not aid an interaction
- Parallel CSS files that duplicate `app.css` variables
- Diagnostic or other `target/`-only JSPs as UI references — stick to `src/main/webapp`
