# AttemptStatus

**Source:** `com.examprep.model.AttemptStatus` (enum)

| Value | Meaning |
|-------|---------|
| IN_PROGRESS | Student is still taking the exam |
| COMPLETED | Submitted before (or at) deadline |
| EXPIRED | Timer ran out; may still have a score |

`fromString(String)` → `AttemptStatus.valueOf(value)`.

UI badges use classes `.badge-IN_PROGRESS`, `.badge-COMPLETED`, `.badge-EXPIRED`.
