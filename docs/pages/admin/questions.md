# Admin questions page

**Path:** `WEB-INF/jsp/admin/questions.jsp`  
**Route:** `/admin/questions`  
**Feature:** [admin-questions](../../features/admin-questions/overview.md)

Question bank management: prompt, A–D options, correct answer, difficulty, optional explanation, subject filter.

Also supports Excel (`.xlsx`) upload via `action=import` (multipart). New subjects created by import default to both exam tracks unless optional `is_professional` / `is_sub_professional` columns are set. See [question-import](../../features/question-import/overview.md).
