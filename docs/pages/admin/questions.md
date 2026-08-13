# Admin questions page

**Path:** `WEB-INF/jsp/admin/questions.jsp`  
**Route:** `/admin/questions`  
**Feature:** [admin-questions](../../features/admin-questions/overview.md)

Question bank management: prompt, A–D options, correct answer, difficulty, optional explanation, subject filter.

Excel:

- Upload via `POST` `action=import` (multipart). Matching subject + prompt **updates** the existing row.
- `GET ?action=template` downloads a sample `.xlsx`.
- `GET ?action=export` downloads the current bank (honors the subject filter).

See [question-import](../../features/question-import/overview.md).
