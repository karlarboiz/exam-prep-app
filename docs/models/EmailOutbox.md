# EmailOutbox

**Source:** `com.examprep.model.EmailOutbox`  
**Table:** `email_outbox`

Stored outbound mail (to, subject, body, optional regimen). `MailService` always inserts a row. When `mail.smtp.host` is set it also sends via SMTP; otherwise it logs to stdout. Used for study-plan digests and password-reset mail.
