# EmailOutbox

**Source:** `com.examprep.model.EmailOutbox`  
**Table:** `email_outbox`

Stored study-plan digest (to, subject, body, regimen). No SMTP dependency; `MailService` inserts a row and logs. Skip when the access grant is expired or revoked.
