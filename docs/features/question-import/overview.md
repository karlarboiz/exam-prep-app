# Question import — overview

Bulk-load multiple-choice questions from an Excel (`.xlsx`) file into the question bank.

Import does **not** attach questions to exams; use [admin exams](../admin-exams/overview.md) for that.

Re-import **upserts**: the same subject name + prompt (trim, case-insensitive) updates the existing question instead of inserting a duplicate. Within one file, later rows with the same key win.

## Triggers

| Trigger | Entry | Notes |
|---------|-------|--------|
| CLI batch job | `QuestionImportJob` | One-shot; path to `.xlsx` as argument |
| Admin upload | `POST /admin/questions` with `action=import` | Multipart file upload; requires `Role.ADMIN` |
| Admin template | `GET /admin/questions?action=template` | Sample `.xlsx` with headers + one example row |
| Admin export | `GET /admin/questions?action=export` | Current bank; optional `subjectId` filter |

All parse/write paths share `ExcelQuestionParser` / `ExcelQuestionWriter` and `QuestionImportService`.

## Flow

1. Parse `.xlsx` via `ExcelQuestionParser` (Apache POI).
2. Validate each data row (required fields, lengths, `correct_option`, difficulty).
3. Resolve subject by name (trim, case-insensitive); create subject if missing (see level flags below).
4. Match existing questions by subject + prompt; insert new rows and update matches (JDBC batch).
5. Return `QuestionImportResult`: imported count, updated count, and per-row errors (partial success).

## Excel contract

Header row (exact column names), one question per data row:

| Column | Required | Rules |
|--------|----------|--------|
| `subject` | yes | Subject name |
| `prompt` | yes | ≤ 1000; together with subject, the upsert key |
| `option_a` … `option_d` | yes | ≤ 500 each |
| `correct_option` | yes | `A` / `B` / `C` / `D` |
| `difficulty` | no | `EASY` / `MEDIUM` / `HARD` (default `MEDIUM`) |
| `explanation` | yes | ≤ 2000; shown on result review |
| `is_professional` | no | `true` / `false` / `1` / `0` / `yes` / `no` — used only when creating a **new** subject |
| `is_sub_professional` | no | same as above |

Format: `.xlsx` only.

### Subject level flags on create

- If both level columns are omitted (or blank) when creating a new subject → both tracks default to **true** so imported content is visible to Professional and Sub-Professional students.
- If either column is provided → parsed booleans must enable at least one track; both false is rejected as a row error and the subject is not created.
- Existing subjects matched by name are **not** updated; their level flags stay as configured in admin.

`schema.sql` also repairs any subjects that already have both flags false (legacy import bug) by enabling both tracks on startup.

## CLI

```bash
mvn -q exec:java -Dexec.mainClass=com.examprep.job.QuestionImportJob -Dexec.args="path/to/questions.xlsx"
```

Exit `0` when at least one row was imported **or** updated and the file was readable. Non-zero if the file is unreadable or zero rows were written when errors exist.

## Related

- Model: [Question](../../models/Question.md), [Subject](../../models/Subject.md)
- Admin page: [questions](../../pages/admin/questions.md)
- Result review: [result](../results-history/result.md)
