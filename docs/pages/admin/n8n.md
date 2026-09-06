# Admin n8n page

**Path:** `WEB-INF/jsp/admin/n8n.jsp`  
**Route:** `/admin/n8n`  
**Feature:** [n8n overview](../../features/n8n/overview.md)

Stacked `.card`s:

- **Request question batch** — `.grid-2` of instructions and a Drive picker. Hidden when `n8n.webhook.questions` is blank.
  - **Connect Google Drive** (`/admin/n8n/drive/connect`) when OAuth is configured but the admin has not signed in.
  - After connect: folder name, **Up** link, folder rows as links (`?folder=`), file checkboxes (`driveFileIds`), Select all / Clear (`js/n8n-drive.js`), Disconnect (`POST /admin/n8n/drive/disconnect`).
  - Service-account fallback lists one configured folder when OAuth is not set.
- **Analyze a file** — file input plus optional message. Hidden when `n8n.webhook.analyze` is blank.

Success uses `.alert-success`. Errors use `.alert-error`. CSRF via `<ep:csrf/>`.

A recent-sends table lists `n8n_requests`. Question summaries include selected Drive file names when present.
A recent-sends table lists `n8n_requests` (kind, summary, status, time) — accepted/failed only, not generated content. On small screens that table stacks into labeled rows.

Linked from the admin header nav and the dashboard quick actions.
