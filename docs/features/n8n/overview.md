# n8n connector — overview

Admin-only page that **sends** a question-batch request or a source file to n8n webhooks. n8n generates questions, uploads files to Google Drive, and analyzes files. It **notifies the admin outside this app** (email or Drive). This app does not call Google APIs, wait for generated questions, or auto-import them.

After n8n delivers an `.xlsx`, import it on [admin questions](../admin-questions/overview.md) using the [Excel contract](../question-import/overview.md).

**Route:** `/admin/n8n`  
**Servlet:** `com.examprep.servlet.admin.N8nServlet`  
**Service:** `N8nService`  
**Page:** [n8n.jsp](../../pages/admin/n8n.md)  
**Model:** [N8nRequest](../../models/N8nRequest.md)

## Access

Requires `Role.ADMIN` (`JwtAuthFilter` on `/admin/**`). CSRF applies (same as other admin forms).

## Config

| Property | Env | Purpose |
|----------|-----|---------|
| `n8n.webhook.questions` | `N8N_WEBHOOK_QUESTIONS` | Question-batch webhook URL |
| `n8n.webhook.analyze` | `N8N_WEBHOOK_ANALYZE` | File-analyze webhook URL |
| `n8n.webhook.secret` | `N8N_WEBHOOK_SECRET` | Sent as `X-N8n-Secret` when non-blank |

If a URL is blank, that form is hidden (empty state) and the service rejects a send. In production, when at least one URL is set, the secret must be 32+ characters and must not contain insecure patterns (`change-me`, `default`, `example`, `test`).

Configure n8n webhooks to **Respond Immediately** so Tomcat is not waiting for LLM work. The Java client treats HTTP 2xx within ~15 seconds as accepted.

## Actions

| HTTP | Action | Description |
|------|--------|-------------|
| GET | — | Forms + recent `n8n_requests` |
| POST | `questions` | JSON webhook: instructions + optional subject / count / difficulty / batch label |
| POST | `analyze` | Multipart webhook: file + optional message. Allow `.pdf`, `.docx`, `.txt`, `.xlsx`, `.png`, `.jpg` / `.jpeg`. Max 10 MB |

Every send writes an audit row ([N8nRequest](../../models/N8nRequest.md)): `ACCEPTED` or `FAILED`. This is not a results inbox.

## Question webhook JSON

```json
{
  "requestedBy": "admin",
  "message": "20 CSE items on Philippine constitution",
  "subject": "General Knowledge",
  "count": "20",
  "difficulty": "MEDIUM",
  "batchLabel": "cse-import-2026-08-30",
  "outputContract": "Excel .xlsx with header row: subject, prompt, option_a, option_b, option_c, option_d, correct_option, difficulty, explanation, optional batch_label. One MCQ per row. correct_option is A/B/C/D. difficulty is EASY/MEDIUM/HARD."
}
```

## Analyze webhook multipart

| Part | Notes |
|------|--------|
| `file` | Binary upload |
| `originalFilename` | Submitted name |
| `message` | Optional analysis instructions |
| `requestedBy` | Admin username |

n8n should upload the file to Google Drive and return topics / study notes / a summary via email or a Drive doc. No Google credentials live in this WAR.

## Out of scope

- Google Drive SDK / OAuth in Java
- Inbound n8n callback or in-app job inbox
- Auto-import of generated questions
- Student access
- Building the n8n workflows themselves
