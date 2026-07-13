# Forms

## Structure

- Wrap fields in `.form-group` (label above, full-width control).
- Inputs / selects / textareas: border `--color-border`, radius `--radius` (8px), padding `0.6rem 0.75rem`.
- Auth screens: wrap form in `.auth-card` (max-width 420px, centered).

## Feedback

| Class | Use |
|-------|-----|
| `.alert` + `.alert-error` | Validation / server errors |
| `.alert` + `.alert-warning` | Subscription / soft warnings |
| `.hint` | Small muted helper under forms |
| `.auth-link` | Centered link row (login ↔ register) |
| `.token-readonly` | Read-only access token on register (monospace) |

## Inline admin actions

- `.inline-form` for delete/update buttons inside tables.
- `.actions` for horizontal button groups.
- `.filter-bar` for subject/filter selects above tables.
- `.checkbox-list` + `.checkbox-item` for multi-select question lists when editing exams.

## Registration token

- Hidden form field `token` posts the grant; visible `.token-readonly` input is display-only.
- Without a valid token, register shows `.empty-state` instead of the form.
