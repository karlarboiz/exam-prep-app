# GoogleDriveAccount

**Source:** `com.examprep.model.GoogleDriveAccount`  
**Table:** `google_drive_accounts`

Per-admin Google OAuth tokens used to list Drive folders on `/admin/n8n`. One row per admin.

| Field | Type | Notes |
|-------|------|--------|
| id | Long | Identity |
| adminUserId | Long | FK `users.id`, unique |
| googleEmail | String | Account shown as “Connected as” |
| accessToken | String | Short-lived Drive API bearer token |
| refreshToken | String | Used to mint a new access token |
| accessExpiresAt | LocalDateTime | Refresh a minute before this |
| createdAt | LocalDateTime | First connect |
| updatedAt | LocalDateTime | Last token refresh or reconnect |

Disconnect deletes the row. This app does not download file bytes.
