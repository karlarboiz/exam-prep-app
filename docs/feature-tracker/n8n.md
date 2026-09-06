# n8n connector

Admin send-only webhooks to n8n. Results arrive outside the app (email / Drive). Generated questions are imported later via Excel.

| Feature | Status | Notes |
|---------|--------|-------|
| Question-batch send | Done | `POST /admin/n8n` `action=questions` → n8n webhook JSON |
| File-analyze send | Done | `POST /admin/n8n` `action=analyze` → n8n webhook multipart; n8n owns Drive upload |
| Drive file picker | Done | Admin taps Connect, signs in with Google, browses folders, checks files. Selected ids go in `driveFiles`. Service-account folder remains an optional fallback |
| Request audit log | Done | `n8n_requests` — accepted/failed only, not a results inbox |
