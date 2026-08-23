# Admin questions page

**Path:** `WEB-INF/jsp/admin/questions.jsp`  
**Route:** `/admin/questions`  
**Feature:** [admin-questions](../../features/admin-questions/overview.md)

Question bank management: prompt, A–D options, correct answer, difficulty, optional explanation, subject and batch filters.

Excel:

- Upload via `POST` `action=import` (multipart). The batch label field defaults to **`cse-import-YYYY-MM-DD`** (today). Matching batch + subject + prompt **updates** the existing row; other batches are not touched.
- `GET ?action=template` downloads a sample `.xlsx`.
- `GET ?action=export` downloads the current bank (honors the subject and batch filters).

The question table and edit form show each item’s batch (or Unlabeled). Manual edit does not change the batch label.

See [question-import](../../features/question-import/overview.md).
