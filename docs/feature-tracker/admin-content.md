# Admin content

Subjects, questions, exams, and bulk import.

| Feature | Status | Notes |
|---------|--------|-------|
| Subjects CRUD | Done | [admin-subjects](../features/admin-subjects/overview.md) |
| Questions CRUD | Done | [admin-questions](../features/admin-questions/overview.md) |
| Exams CRUD (+ attach questions) | Done | [admin-exams](../features/admin-exams/overview.md) |
| Users admin list | Done | [admin-users](../features/admin-users/overview.md) |
| Excel question import | Done | CLI + admin upload |
| Excel import: subject level flags (no both-false / empty dashboard) | Done | New subjects default both tracks true; both-false rejected; schema backfill for stuck subjects |
| Users admin: edit exam level / role | Done | Inline role + exam level; delete with last-admin / self guards |
| Excel import sample template | Done | `GET /admin/questions?action=template` |
| Excel import upsert / de-dupe | Done | Match **batch label + subject + prompt** (case-insensitive); later rows win |
| Excel import batch label | Done | Required on write; admin/CLI default `cse-import-YYYY-MM-DD`; stamps every item; other batches are not updated |
| Question bank Excel export | Done | `GET /admin/questions?action=export` (optional `subjectId`) |
| Reorder questions on a practice exam | Done | Up/Down on `/admin/exams`; stored as `exam_questions.sort_order` |
