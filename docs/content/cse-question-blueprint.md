# CSE-PPT question blueprint

Writing spec for original practice items. Source of coverage: Civil Service Commission Examination Announcement No. 03, s. 2026 (CSE-PPT, 09 August 2026). CSC does **not** publish per-subtest item weights, past papers, or review materials. This file is an **app convention** for building a bank that matches the official topic list — not a copy of any CSC questionnaire.

Do not ingest Facebook-group PDFs, “leaked” papers, or third-party reviewers as stems, options, or paraphrases. Unauthorized possession or reproduction of CSC exam materials is prohibited under Republic Act No. 9416.

Related: [question import](../features/question-import/overview.md), [Question](../models/Question.md), [Subject](../models/Subject.md).

## Official constraints (CSC)

| Fact | Professional | Subprofessional |
|------|----------------|-----------------|
| Subtests | Verbal, Numerical, Analytical, General Information | Verbal, Numerical, Clerical, General Information |
| Total entries (incl. EDQ) | 170 | 165 |
| Test proper | 150 | 145 |
| Time (includes EDQ) | 3 hours 10 minutes | 2 hours 40 minutes |
| Passing grade | General rating of at least **80.00** | Same |
| Verbal language | English **and** Filipino | Same |
| Other subtests | English | English |
| Calculator | Not allowed | Not allowed |

The 20-item Examinee Descriptive Questionnaire is personal data. **Do not write EDQ items.** Practice and mocks are test-proper only.

## Map to this app

Create these `subjects` (names must match Excel `subject` exactly):

| Subject | `is_professional` | `is_sub_professional` |
|---------|-------------------|------------------------|
| Verbal Ability | true | true |
| Numerical Ability | true | true |
| General Information | true | true |
| Analytical Ability | true | false |
| Clerical Ability | false | true |

The seeded **General Knowledge** placeholder is not a CSC subtest. Keep it only for demos, or retire it once the five subjects above have a real bank.

Topic is a **writer tag**, not a database column. Keep it in the working spreadsheet; import only the [Excel contract](../features/question-import/overview.md) columns.

### Import limits

- `prompt` ≤ 1000 characters (keep reading passages to 3–5 short sentences)
- each option ≤ 500
- `explanation` ≤ 2000 and **required** on import
- `difficulty`: `EASY` / `MEDIUM` / `HARD`
- four options, one key `A`–`D`
- `image_url` for figure-based abstract reasoning only

### Difficulty (app convention)

Diagnostic sampling round-robins EASY → MEDIUM → HARD per subject, so each subject needs all three bands.

| Band | What the examinee must do | Distractors |
|------|---------------------------|-------------|
| EASY | One step; common word or fact; arithmetic without a trap | Obviously wrong |
| MEDIUM | Two steps, or one close synonym / near-miss number | One attractive wrong option |
| HARD | Multi-step, fine distinction, or time pressure | Two near-misses |

Target mix **per topic**: 6 EASY, 8 MEDIUM, 6 HARD (20 items). That is 30 / 40 / 30 overall.

## Recommended first bank

20 items × 25 topic-language cells = **500 original questions**.

| Subject | Cells | Items | Tracks |
|---------|-------|-------|--------|
| Verbal Ability | 6 topics × 2 languages | 240 | Both |
| Numerical Ability | 3 topics | 60 | Both |
| General Information | 4 topics | 80 | Both |
| Analytical Ability | 4 topics | 80 | Professional |
| Clerical Ability | 2 topics | 40 | Subprofessional |
| **Total unique** | | **500** | |

Professional pool: 460. Subprofessional pool: 420.

Phase 1 (half bank): 3 / 4 / 3 per cell = 250 items. Do not ship a diagnostic until each of the five subjects has at least a few EASY, MEDIUM, and HARD items.

### Timed mock (app convention — not CSC weights)

CSC does not publish how many items sit in each subtest. For a full mock, use equal-ish splits of **test proper only**:

- Professional: **150** items, **180 minutes** (slightly tighter than wall-clock with EDQ)
- Subprofessional: **145** items, **150 minutes**

Suggested internal split (relabel in admin as practice, never as “official weights”):

| Subtest | Professional (~150) | Subprofessional (~145) |
|---------|---------------------|------------------------|
| Verbal Ability | 40 | 40 |
| Numerical Ability | 35 | 35 |
| Analytical Ability | 40 | — |
| Clerical Ability | — | 35 |
| General Information | 35 | 35 |

## Topic × skill × difficulty

### Verbal Ability (English and Filipino)

Write **separate** English and Filipino items for every topic. Do not machine-translate English stems into Filipino.

| Topic | Skill | EASY | MEDIUM | HARD |
|-------|-------|------|--------|------|
| Word meaning | Closest meaning **in context** | Common word; context makes the sense obvious | Less common word; two near synonyms | Formal/legal register or a fine pair (e.g. *imply* / *infer*) |
| Sentence completion | Best word or phrase for the blank | Single blank; collocation is obvious | Tone or logic of the sentence decides | Two blanks or a connector (*however*, *therefore*) that flips meaning |
| Error recognition | Spot the grammar/usage error, or “no error” | Clear subject–verb or tense error | Pronoun, article, or preposition | Subtle parallelism or a “no error” key |
| Sentence structure | Grammar, modifiers, parallelism | Obvious fragment vs complete sentence | Misplaced modifier | Ambiguous attachment; all options grammatical-looking |
| Paragraph organization | Best order of 3–4 numbered sentences | Clear topic → support → close | One sentence that could sit in two places | Logical connectors required to lock the order |
| Reading comprehension | Main idea, stated detail, or inference | Detail copied in paraphrase | Main idea vs a true-but-narrow option | Inference that **must** follow; reject “could be true” |

Filipino items test the same skills (kahulugan, pagkumpleto, pagkilala ng mali, kayarian, pag-ayos ng talata, pag-unawa). Use natural Filipino, including words that appear in civic/office Filipino, not slang.

### Numerical Ability (English, no calculator)

| Topic | Skill | EASY | MEDIUM | HARD |
|-------|-------|------|--------|------|
| Basic operations | Arithmetic, fractions, percent, order of operations | Whole-number +/−/×/÷ | Fraction + percent in one item | Nested operations or a percent-of-percent |
| Number sequence | Next or missing term | Add/subtract a constant | Multiply, squares, or two interleaved rules | Second-order rule (differences of differences) |
| Word problems | Rate, ratio, work, profit, average | One-step (distance = rate × time) | Two-step; extra unused number as distractor | Reverse percent, combined work, or unit change |

Keep numbers small enough to compute by hand. Put units in the prompt. Wrong options should be results of a common mistake (forgot to convert minutes, used the discount as the original, and so on).

### Analytical Ability (Professional only)

| Topic | Skill | EASY | MEDIUM | HARD |
|-------|-------|------|--------|------|
| Word analogy | Preserve the **relationship**, not the topic | Obvious category or synonym | Part–whole, cause–effect, degree | Reversed relationship or abstract function |
| Symbolic logic / abstract reasoning | One or more visual rules | Single changing feature (count or rotation) | Two features; one alternates | Two rules plus a distractor that matches only one |
| Assumptions and conclusions | What **must** follow from a short stem | Direct restatement | Unstated but necessary assumption | “Could be true” vs “must be true”; reject extra facts |
| Data interpretation | Read a mini table in the prompt | Read one cell | Compare two cells or a simple percent | Wrong base, wrong year, or unit trap |

Abstract items may use `image_url`. Tables must fit in the 1000-character prompt (3×3 or smaller). Number sequence belongs under Numerical Ability, not here.

### Clerical Ability (Subprofessional only)

| Topic | Skill | EASY | MEDIUM | HARD |
|-------|-------|------|--------|------|
| Filing | Alphabetical, numerical, or date order | First-letter alphabetization | Names with *Mc*, *St.*, or titles | Same surname; middle initial or JR/SR decides |
| Spelling | Standard English spelling | Common everyday word | Office/civic vocabulary | Frequently confused pair (*principal* / *principle*) |

### General Information (English)

Cite the official text in the explanation (Constitution article, RA section). Stems must be original. Do not write current-events trivia that will rot.

| Topic | Skill | EASY | MEDIUM | HARD |
|-------|-------|------|--------|------|
| Philippine Constitution | 1987 Constitution: citizenship, Bill of Rights, branches, local government | Landmark label (e.g. Bill of Rights is Article III) | Which branch holds a named power | Short scenario: which provision applies |
| RA 6713 | Code of Conduct: norms, duties, prohibited acts, disclosure | Name of the law / a listed norm of conduct | Which act is prohibited | Conflict-of-interest scenario |
| Peace and Human Rights | Civic concepts: rights, CHR’s existence, non-discrimination, peace as public policy | Identify a civil/political right | Duty-bearer vs rights-holder | Apply a right to a short workplace/civic scenario |
| Environment management and protection | Public PH environmental policy (e.g. Clean Air Act, ecological solid waste, EIA as a system) | Identify the policy area | Which instrument matches the problem (waste vs air vs EIA) | Scenario: which duty or principle applies |

Prefer statutes and the Constitution over news blogs. If a fact is not in an official text you can cite, do not write the item.

## Writing rules

1. **Original stems and options.** If a Facebook PDF already used the sentence, rewrite from the topic skill, not from that sentence.
2. **One skill per item.** Do not mix a grammar error with a Constitution fact.
3. **Explanation teaches.** State why the key is right and why the closest distractor fails. For GI, cite article/section.
4. **No “actual exam” language** in prompts, explanations, or admin titles.
5. **Plausible options.** All four must be the same type (all nouns, all numbers, all provisions).
6. **Shuffle-safe.** The key must remain correct if options are reordered (the app can shuffle display). Avoid “all of the above” / “none of the above” / “A and C”.
7. **No calculator; no scratch-paper gimmicks** that need a figure the prompt cannot show.

## Working spreadsheet

Keep a writer workbook with extra columns, then copy the import columns into the admin template:

| Writer column | Import? | Notes |
|---------------|---------|--------|
| subject | yes | Exact subject name above |
| topic | no | From the tables |
| language | no | `EN` or `FIL` (Verbal only) |
| skill | no | One line from the skill column |
| prompt | yes | |
| option_a–d | yes | |
| correct_option | yes | |
| difficulty | yes | |
| explanation | yes | Include citation for GI |
| is_professional / is_sub_professional | yes, on **new** subjects only | See import docs |
| source_note | no | Always `original` |

## Example shapes (original — not CSC items)

These illustrate house style only. Do not treat them as coverage completeness.

**Verbal / Word meaning / EASY / EN**  
Prompt: In “The officer was commended for her *diligence*,” *diligence* most nearly means:  
A. luck  B. steady effort  C. seniority  D. popularity  
Key: B. Explanation: Commendation for diligence points to careful, sustained work, not chance or rank.

**Numerical / Word problems / MEDIUM / EN**  
Prompt: A clerk files 80 papers in 40 minutes. At the same rate, how many papers in 2.5 hours?  
A. 200  B. 240  C. 300  D. 400  
Key: C. Explanation: 80 / 40 min = 2 per minute; 2.5 h = 150 min; 2 × 150 = 300. A is 2.5 × 80 (ignored time). B is 3 papers/min.

**General Information / RA 6713 / EASY / EN**  
Prompt: Republic Act No. 6713 is primarily concerned with:  
A. creating new government positions  B. standards of conduct for public officials and employees  C. setting civil service exam fees  D. local government boundaries  
Key: B. Explanation: RA 6713 is the Code of Conduct and Ethical Standards for Public Officials and Employees (CSC exam coverage).
