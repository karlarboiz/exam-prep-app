# Question import — overview

Bulk-load multiple-choice questions from an Excel (`.xlsx`) file into the question bank.

Import does **not** attach questions to exams; use [admin exams](../admin-exams/overview.md) for that. Re-import always inserts new rows (no upsert).

## Triggers

| Trigger | Entry | Notes |
|---------|-------|--------|
| CLI batch job | `QuestionImportJob` | One-shot; path to `.xlsx` as argument |
| Admin upload | `POST /admin/questions` with `action=import` | Multipart file upload; requires `Role.ADMIN` |

Both call `QuestionImportService`.

## Flow

1. Parse `.xlsx` via `ExcelQuestionParser` (Apache POI).
2. Validate each data row (required fields, lengths, `correct_option`, difficulty).
3. Resolve subject by name (trim, case-insensitive); create subject if missing.
4. Insert valid questions (JDBC batch).
5. Return `QuestionImportResult`: imported count + per-row errors (partial success).

## Excel contract

Header row (exact column names), one question per data row:

| Column | Required | Rules |
|--------|----------|--------|
| `subject` | yes | Subject name |
| `prompt` | yes | ≤ 1000 |
| `option_a` … `option_d` | yes | ≤ 500 each |
| `correct_option` | yes | `A` / `B` / `C` / `D` |
| `difficulty` | no | `EASY` / `MEDIUM` / `HARD` (default `MEDIUM`) |
| `explanation` | yes | ≤ 2000; shown on result review |

Format: `.xlsx` only.

## CLI

```bash
mvn -q exec:java -Dexec.mainClass=com.examprep.job.QuestionImportJob -Dexec.args="path/to/questions.xlsx"
```

Exit `0` when at least one row imported and the file was readable. Non-zero if the file is unreadable or zero rows were imported when errors exist.

## Related

- Model: [Question](../../models/Question.md)
- Admin page: [questions](../../pages/admin/questions.md)
- Result review: [result](../results-history/result.md)
