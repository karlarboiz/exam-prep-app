# Diagnostic rubric

Implemented in `DiagnosticService` (`bandForPercent`, `computeReadiness`).

## Subject bands

| Band | Accuracy |
|------|----------|
| STRONG | ≥ 75% |
| DEVELOPING | 50–74% |
| WEAK | &lt; 50% |

Per-subject accuracy = correct answers / questions sampled for that subject (unanswered = incorrect).

## Overall readiness

Computed from the **mean** of assessed subject percentages and the count of WEAK subjects:

| Label | Rule |
|-------|------|
| Needs foundation | mean &lt; 50% **or** ≥ 2 WEAK subjects |
| Building | mean 50–74% and ≤ 1 WEAK |
| Near ready | mean ≥ 75% and ≤ 1 WEAK |
| Exam-ready | mean ≥ 75% and **0** WEAK |

Subjects with no questions in the bank are not assessed and do not appear in the mean.
