# n8n connector — overview

Admin-only page that **sends** a question-batch request or a source file to n8n webhooks. The admin can **sign in with Google**, browse Drive folders, and check which files n8n should read. n8n generates questions and notifies the admin outside this app (email or Drive). This app does not wait for generated questions or auto-import them.

After n8n delivers an `.xlsx`, import it on [admin questions](../admin-questions/overview.md) using the [Excel contract](../question-import/overview.md).

**Route:** `/admin/n8n`  
**Servlets:** `N8nServlet`, `GoogleDriveOAuthServlet`  
**Services:** `N8nService`, `GoogleOAuthService`, `GoogleDriveService`  
**Page:** [n8n.jsp](../../pages/admin/n8n.md)  
**Models:** [N8nRequest](../../models/N8nRequest.md), [GoogleDriveFile](../../models/GoogleDriveFile.md), [GoogleDriveAccount](../../models/GoogleDriveAccount.md)

## Access

Requires `Role.ADMIN` (`JwtAuthFilter` on `/admin/**`). CSRF applies on POST (same as other admin forms). The Google callback is GET.

## Config

| Property | Env | Purpose |
|----------|-----|---------|
| `n8n.webhook.questions` | `N8N_WEBHOOK_QUESTIONS` | Question-batch webhook URL |
| `n8n.webhook.analyze` | `N8N_WEBHOOK_ANALYZE` | File-analyze webhook URL |
| `n8n.webhook.secret` | `N8N_WEBHOOK_SECRET` | Sent as `X-N8n-Secret` when non-blank |
| `google.oauth.clientId` | `GOOGLE_OAUTH_CLIENT_ID` | OAuth web client id |
| `google.oauth.clientSecret` | `GOOGLE_OAUTH_CLIENT_SECRET` | OAuth web client secret |
| `google.oauth.redirectUri` | `GOOGLE_OAUTH_REDIRECT_URI` | Optional exact callback URL |
| `google.drive.folderId` | `GOOGLE_DRIVE_FOLDER_ID` | Optional service-account fallback folder |
| `google.drive.serviceAccountJson` | `GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON` | Optional service-account JSON or file path |

If a webhook URL is blank, that form is hidden. In production, when at least one URL is set, the n8n secret must be 32+ characters and must not contain insecure patterns. If an OAuth client id is set, the client secret is required. If a Drive folder id is set, the service-account JSON is required.

Redirect URI defaults to `{app.public.url}/admin/n8n/drive/callback`, or the current host + that path. It must match the Google Cloud OAuth client **exactly**.

Configure n8n webhooks to **Respond Immediately**. The Java client treats HTTP 2xx within ~15 seconds as accepted.

### Drive setup (OAuth — preferred)

1. In Google Cloud, enable the **Google Drive API**.
2. Create an **OAuth client** (Web application).
3. Add authorized redirect URI `http://localhost:8080/admin/n8n/drive/callback` (and the production URL when you have one).
4. Set `GOOGLE_OAUTH_CLIENT_ID` and `GOOGLE_OAUTH_CLIENT_SECRET`.
5. On `/admin/n8n`, tap **Connect Google Drive**, sign in, and allow Drive metadata access.

This app uses `drive.metadata.readonly` and **does not download file bytes**. Tokens are stored per admin in `google_drive_accounts`. n8n still opens the selected files.

A service-account folder remains an optional fallback when OAuth is not configured.

## Actions

| HTTP | Action | Description |
|------|--------|-------------|
| GET `/admin/n8n` | — | Forms + Drive picker + recent `n8n_requests` |
| GET `/admin/n8n/drive/connect` | — | Redirect to Google sign-in |
| GET `/admin/n8n/drive/callback` | — | Store tokens; redirect back to `/admin/n8n` |
| POST `/admin/n8n/drive/disconnect` | — | Drop the stored Google tokens |
| POST `/admin/n8n` | `questions` | JSON webhook + selected Drive file ids |
| POST `/admin/n8n` | `analyze` | Multipart file + optional message. Allow `.pdf`, `.docx`, `.txt`, `.xlsx`, `.png`, `.jpg` / `.jpeg`. Max 10 MB |

Every send writes an audit row ([N8nRequest](../../models/N8nRequest.md)): `ACCEPTED` or `FAILED`.

Selected Drive ids are resolved against the folder currently open (max 20). Unknown ids are rejected. Folder links browse one level at a time (up to 200 items).

## Question webhook JSON

```json
{
  "requestedBy": "admin",
  "message": "20 CSE items on Philippine constitution",
  "subject": "General Knowledge",
  "count": "20",
  "difficulty": "MEDIUM",
  "batchLabel": "cse-import-2026-08-30",
  "outputContract": "Excel .xlsx with header row: subject, prompt, option_a, option_b, option_c, option_d, correct_option, difficulty, explanation, optional batch_label. One MCQ per row. correct_option is A/B/C/D. difficulty is EASY/MEDIUM/HARD.",
  "driveFolderId": "1AbCFolderId",
  "driveFiles": [
    {"id": "1XyZFileId", "name": "constitution.pdf", "mimeType": "application/pdf"}
  ]
}
```

**n8n must only scan `driveFiles`.** Download each item by id. If `driveFiles` is empty, generate from `message` only — do not list or scan the whole folder.

## Analyze webhook multipart

| Part | Notes |
|------|--------|
| `file` | Binary upload |
| `originalFilename` | Submitted name |
| `message` | Optional analysis instructions |
| `requestedBy` | Admin username |

## Out of scope

- Downloading Drive file bytes in Java
- Inbound n8n callback or in-app job inbox
- Auto-import of generated questions
- Student access
- Building the n8n workflows themselves
