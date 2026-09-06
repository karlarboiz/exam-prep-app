# GoogleDriveFile

**Source:** `com.examprep.model.GoogleDriveFile`  
**Persistence:** none — live metadata from the Google Drive API

A file or folder in the admin’s Google Drive (or the optional service-account folder), shown on `/admin/n8n` so an admin can pick sources for a question-batch send. Folders are links; files are checkboxes.

| Field | Type | Notes |
|-------|------|--------|
| id | String | Google Drive file id |
| name | String | Display name |
| mimeType | String | Drive MIME type; folders are omitted from the picker |
| modifiedTime | String | RFC3339 from Drive, optional |
| sizeBytes | Long | Present for binary files; often null for Google Docs |

`kindLabel` is a short UI label (PDF, Word, Google Doc, …). This app lists metadata only; it does not download file bytes.
