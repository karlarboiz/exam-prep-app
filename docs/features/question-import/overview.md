# Question import — overview

Bulk-load multiple-choice questions from an Excel (`.xlsx`) file into the question bank.

Import does **not** attach questions to exams; use [admin exams](../admin-exams/overview.md) for that.

Re-import **upserts within a batch**: the same batch label + subject name + prompt (trim, case-insensitive) updates the existing question instead of inserting a duplicate. A different batch label always inserts, even when the prompt matches an item from another import. Within one file, later rows with the same key win.

## Triggers

| Trigger | Entry | Notes |
|---------|--------|-------|
| CLI batch job | `QuestionImportJob` | Path to `.xlsx`; optional batch label (defaults to `cse-import-YYYY-MM-DD`) |
| Admin upload | `POST /admin/questions` with `action=import` | Multipart file upload; requires `Role.ADMIN`. Batch label defaults to `cse-import-YYYY-MM-DD` |
| Admin template | `GET /admin/questions?action=template` | Sample `.xlsx` with headers + one example row |
| Admin export | `GET /admin/questions?action=export` | Current bank; optional `subjectId` and `batchLabel` filters |

All parse/write paths share `ExcelQuestionParser` / `ExcelQuestionWriter` and `QuestionImportService`.

## Flow

1. Parse `.xlsx` via `ExcelQuestionParser` (Apache POI).
2. Validate each data row (required fields, lengths, `correct_option`, difficulty).
3. Stamp every written row with the import batch label (form or CLI). A non-blank Excel `batch_label` must match that label or the row is rejected.
4. Resolve subject by name (trim, case-insensitive); create subject if missing (see level flags below).
5. Match existing questions by **batch label + subject + prompt**; insert new rows and update matches (JDBC batch). Questions in other batches are not updated.
6. Return `QuestionImportResult`: imported count, updated count, and per-row errors (partial success).

## Excel contract

Header row (exact column names), one question per data row:

| Column | Required | Rules |
|--------|----------|--------|
| `subject` | yes | Subject name |
| `prompt` | yes | ≤ 1000; together with subject and batch label, the upsert key |
| `option_a` … `option_d` | yes | ≤ 500 each |
| `correct_option` | yes | `A` / `B` / `C` / `D` |
| `difficulty` | no | `EASY` / `MEDIUM` / `HARD` (default `MEDIUM`) |
| `explanation` | yes | ≤ 2000; shown on result review |
| `is_professional` | no | `true` / `false` / `1` / `0` / `yes` / `no` — used only when creating a **new** subject |
| `is_sub_professional` | no | same as above |
| `batch_label` | no | ≤ 100. If present, must match this import’s batch label (case-insensitive). Written on export so a file documents its batch. |

Format: `.xlsx` only.

### Batch label

Every imported or updated question stores a batch label (1–100 characters after trim).

The default for admin upload and CLI (when omitted) is **`cse-import-YYYY-MM-DD`** using the server's local date — for example `cse-import-2026-08-23`. Same-day re-imports reuse that label so updates stay inside today's batch.

- Reuse the same label to correct items from that import.
- Use a new label to add a separate set; existing items with the same prompt stay untouched.
- Manual questions and older unlabeled imports stay in the unlabeled group and are not updated by a labeled import.
- `__unlabeled__` is reserved for the admin filter and is not a valid batch name.

### Subject level flags on create

- If both level columns are omitted (or blank) when creating a new subject → both tracks default to **true** so imported content is visible to Professional and Sub-Professional students.
- If either column is provided → parsed booleans must enable at least one track; both false is rejected as a row error and the subject is not created.
- Existing subjects matched by name are **not** updated; their level flags stay as configured in admin.

`schema.sql` also repairs any subjects that already have both flags false (legacy import bug) by enabling both tracks on startup.

## CLI

```bash
mvn -q exec:java -Dexec.mainClass=com.examprep.job.QuestionImportJob -Dexec.args="path/to/questions.xlsx"
mvn -q exec:java -Dexec.mainClass=com.examprep.job.QuestionImportJob -Dexec.args="path/to/questions.xlsx cse-import-2026-08-23"
```

Exit `0` when at least one row was imported **or** updated and the file was readable. Non-zero if the file is unreadable or zero rows were written when errors exist.

## Related

- Model: [Question](../../models/Question.md), [Subject](../../models/Subject.md)
- Admin page: [questions](../../pages/admin/questions.md)
- Result review: [result](../results-history/result.md)
