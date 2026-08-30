# Admin n8n page

**Path:** `WEB-INF/jsp/admin/n8n.jsp`  
**Route:** `/admin/n8n`  
**Feature:** [n8n overview](../../features/n8n/overview.md)

Two `.card`s in `.grid-2`:

- **Request question batch** — message (required) plus optional subject, count, difficulty, batch label. Hidden when `n8n.webhook.questions` is blank.
- **Analyze a file** — file input plus optional message. Hidden when `n8n.webhook.analyze` is blank.

Success uses `.alert-success` and points the admin to import an `.xlsx` on `/admin/questions`. Errors use `.alert-error`. CSRF via `<ep:csrf/>`.

A recent-sends table lists `n8n_requests` (kind, summary, status, time) — accepted/failed only, not generated content.

Linked from the admin header nav and the dashboard quick actions.
