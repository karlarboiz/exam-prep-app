# Subject

**Source:** `com.examprep.model.Subject`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Primary key |
| name | String | Display name |
| description | String | Optional detail |
| professional | boolean | Visible on Professional exam track (`is_professional`) |
| subProfessional | boolean | Visible on Sub-Professional exam track (`is_sub_professional`) |

Subjects group questions and exams. At least one level flag must be true for students on that track to see the subject (and its practice exams) on the dashboard.

## Seed

`schema.sql` seeds **General Knowledge** with both `is_professional` and `is_sub_professional` set to `TRUE` so both tracks have content out of the box.

## Import

Excel question import creates missing subjects with **both** flags `TRUE` by default, or uses optional `is_professional` / `is_sub_professional` columns — see [question-import](../features/question-import/overview.md).
