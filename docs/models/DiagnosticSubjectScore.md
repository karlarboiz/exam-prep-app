# DiagnosticSubjectScore

**Source:** `com.examprep.model.DiagnosticSubjectScore`  
**Table:** `diagnostic_subject_scores`

| Field | Type | Notes |
|-------|------|-------|
| attemptId | Long | Diagnostic attempt |
| subjectId | Long | Assessed subject |
| subjectName | String | Display join |
| scorePercent | BigDecimal | 0–100 for that subject |
| band | SubjectBand | STRONG / DEVELOPING / WEAK |

Snapshot written on diagnostic submit. See [rubric](../features/diagnostic/rubric.md).

## Related: attempt_questions

Diagnostic attempts store their sampled question set in `attempt_questions (attempt_id, question_id, sort_order)` instead of using `exam_questions`. Practice attempts continue to use `exam_questions`.
