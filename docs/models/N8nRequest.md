# N8nRequest

**Source:** `com.examprep.model.N8nRequest`  
**Table:** `n8n_requests`

Audit row for an admin send to n8n. Not a results inbox — generated questions and file analysis arrive outside the app.

| Field | Type | Notes |
|-------|------|--------|
| id | Long | Identity |
| adminUserId | Long | FK `users.id` |
| kind | [N8nRequestKind](N8nRequestKind.md) | `QUESTIONS` or `ANALYZE` |
| summary | String | Trimmed prompt or filename (≤ 300) |
| status | [N8nRequestStatus](N8nRequestStatus.md) | `ACCEPTED` or `FAILED` |
| errorMessage | String | Optional; HTTP/timeout detail on failure (≤ 500) |
| createdAt | LocalDateTime | Server insert time |
