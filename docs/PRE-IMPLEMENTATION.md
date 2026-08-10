# Pre-implementation guide

Read this **before writing or changing code**. It is the ordered checklist for this app: what exists, what depends on what, and the steps to take for any new work.

Companion: [UI guide](UI-GUIDE.md) · Index: [docs README](README.md)

---

## Stack snapshot

| Layer | Choice |
|-------|--------|
| Language / build | Java 17, Maven WAR |
| Web | Jakarta Servlet 6 + JSP / JSTL |
| DB | H2 + HikariCP (`schema.sql` on startup) |
| Auth session | JWT cookie `access_token` (`JwtAuthFilter`) |
| Quiz entitlement | Access grants + `SubscriptionFilter` |
| Styles | Single sheet `src/main/webapp/css/app.css` |

Config: `src/main/resources/app.properties` · Schema: `src/main/resources/schema.sql`

---

## How to use this doc

1. Confirm the **current inventory** (below) matches what you are about to touch.
2. For **new features**, walk the **pre-implementation checklist** top to bottom — do not skip layers.
3. Follow the **build / dependency order** so models and schema land before servlets and JSPs.
4. Before UI work, skim the [UI guide](UI-GUIDE.md) and reuse existing classes.
5. After implementation, update the matching doc under `features/`, `models/`, or `pages/`.

---

## Current inventory (as of docs)

Use this as a readiness map. Items marked done are already in source + documented.

### Foundation

| Item | Status | Docs / location |
|------|--------|-----------------|
| Maven WAR, JDK 17, Tomcat-oriented packaging | Done | Root [README](../README.md), `pom.xml` |
| H2 schema + seed (admin, sample subject/exam) | Done | `schema.sql`, `SeedData` |
| App config + DB pool on startup | Done | `AppConfig`, `DatabaseManager`, `AppContextListener` |
| Character encoding filter | Done | `CharacterEncodingFilter` |
| Shared chrome (header / footer) + `app.css` | Done | [UI guide](UI-GUIDE.md), [layout](ui-rules/layout.md) |

### Domain models

| Model | Status | Doc |
|-------|--------|-----|
| User, Role | Done | [User](models/User.md), [Role](models/Role.md) |
| Subject | Done | [Subject](models/Subject.md) |
| Question | Done | [Question](models/Question.md) |
| Exam (+ exam_questions) | Done | [Exam](models/Exam.md) |
| ExamAttempt, AttemptStatus | Done | [ExamAttempt](models/ExamAttempt.md), [AttemptStatus](models/AttemptStatus.md) |
| AttemptAnswer | Done | [AttemptAnswer](models/AttemptAnswer.md) |
| AccessGrant, AccessGrantStatus | Done | [AccessGrant](models/AccessGrant.md), [AccessGrantStatus](models/AccessGrantStatus.md) |

### Auth & access

| Capability | Status | Doc |
|------------|--------|-----|
| Login / logout / register | Done | [login](features/auth/login.md), [logout](features/auth/logout.md), [register](features/auth/register.md) |
| JWT session filter | Done | [jwt](features/jwt-auth-filter/jwt.md) |
| Funnel create-token API | Done | [create-token-api](features/access-grants/create-token-api.md) |
| One-time token redeem at register | Done | [redeem](features/access-grants/redeem.md) |
| Subscription gate on `/user/**` | Done | [enforce-access](features/access-grants/enforce-access.md) |
| Subscription-expired page | Done | [page](pages/user/subscription-expired.md) |

### Admin

| Capability | Status | Doc |
|------------|--------|-----|
| Admin dashboard | Done | [overview page](pages/admin/dashboard.md) |
| Subjects CRUD | Done | [admin-subjects](features/admin-subjects/overview.md) |
| Questions CRUD | Done | [admin-questions](features/admin-questions/overview.md) |
| Exams CRUD (+ question attach) | Done | [admin-exams](features/admin-exams/overview.md) |
| Users admin | Done | [admin-users](features/admin-users/overview.md) |
| Access-grants admin UI | **Not in source** | Cargo/target had a stale JSP only — no servlet/page docs yet |

### Student flow

| Capability | Status | Doc |
|------------|--------|-----|
| User dashboard (active exams) | Done | [dashboard](pages/user/dashboard.md) |
| Take exam (timer, answer, submit) | Done | [flow](features/take-exam/flow.md) |
| Result + answer review | Done | [result](features/results-history/result.md) |
| History | Done | [history](features/results-history/history.md) |
| Diagnostic exam / result pages | **Not in source** | Only appeared under `target/` cargo work — treat as out of scope until specified |

### Ops

| Item | Status |
|------|--------|
| CI (`mvn package`) + Docker / GHCR workflows | Done (see root README) |

---

## Recommended dependency order

Build or change work **down this stack**. Do not start a servlet before its model/DAO/service exists.

```
0. Product / acceptance notes (who, route, happy path, errors)
1. Schema + model + enum
2. DAO
3. Service (rules, scoring, auth, grants)
4. Filter changes (only if path/role/entitlement changes)
5. Servlet (routing, validation, redirects)
6. JSP page (reuse UI guide classes)
7. Seed / sample data (if demos need it)
8. Docs update (feature + page + model as needed)
9. Manual smoke (login → capability → logout / expiry)
```

### Feature build sequence (historical / greenfield order)

If rebuilding from scratch or teaching the system, implement in this order:

1. **Project shell** — Maven WAR, `web.xml`, encoding filter, `app.css`, header/footer, 403/404
2. **Users & roles** — schema, `User` / `Role`, password hash, seed admin
3. **JWT auth** — issue cookie, `JwtAuthFilter`, login / logout pages
4. **Subjects → Questions → Exams** — admin CRUD in that order (exams need questions)
5. **Attempts & answers** — take exam, submit, score, result, history
6. **Access grants** — create-token API → redeem on register → `SubscriptionFilter` → expired page
7. **Hardening** — ownership checks (403), active-exam rules, timer expiry scoring

---

## Pre-implementation checklist (every change)

Copy this into the PR/task notes and tick before coding.

### A. Intent

- [ ] One-sentence goal (what the user can do when done)
- [ ] Actor: `ADMIN` / `USER` / funnel (API key) / anonymous
- [ ] Primary route(s) and HTTP methods
- [ ] Out of scope explicitly listed

### B. Data

- [ ] Tables/columns needed? Migration note for H2 (`schema.sql` + existing DBs)
- [ ] Model fields + enums documented under `docs/models/`
- [ ] Indexes / uniqueness (e.g. token hash, username)

### C. Rules

- [ ] Auth: public vs JWT-required vs admin-only
- [ ] Entitlement: does `/user/**` subscription gate apply?
- [ ] Ownership / IDOR: can user A touch user B’s attempt?
- [ ] Validation + user-visible error messages
- [ ] Side effects (redirects, cookie set/clear, grant status transitions)

### D. UI

- [ ] Page listed under `docs/pages/…` (or new page doc drafted)
- [ ] Classes chosen from [UI guide](UI-GUIDE.md) — no one-off colors
- [ ] Empty, error, and success states covered
- [ ] Mobile: grids that should stack (see `.grid-2` breakpoint)

### E. Docs & verify

- [ ] Feature doc under `docs/features/…` written or updated
- [ ] Links from this file / [docs README](README.md) if it is a new area
- [ ] Smoke path written (steps below)

---

## Smoke paths (baseline)

Run these after any auth, grant, or exam change.

### Admin

1. Login `admin` / seeded password → `/admin/dashboard`
2. Create subject → question → exam (attach questions, set active)
3. Confirm user list loads

### Funnel → student

1. `POST /api/access-tokens` with `X-Api-Key` → raw token once
2. Open `/register?token=…` → create user
3. Login as that user → `/user/dashboard` → start exam → answer → submit → result
4. History shows attempt; reopen result review (including unanswered)

### Expiry gate

1. With a redeemed grant past `expires_at`, hit `/user/dashboard` → redirect `/user/subscription-expired`
2. Login still works; quiz routes stay blocked
3. Admin routes remain reachable for `ADMIN`

### Negative

1. `/register` without token → empty state, no form
2. Reuse redeemed token → reject
3. Another user’s `attemptId` → 403

---

## Doc map (where to write)

| Concern | Folder |
|---------|--------|
| Behavior / flow / API | `docs/features/<area>/` |
| Fields / enums | `docs/models/` |
| JSP route + markup notes | `docs/pages/…` |
| Visual system | [UI-GUIDE.md](UI-GUIDE.md) + `docs/ui-rules/` |
| This ordered checklist | **this file** |

---

## Gaps / do-not-assume

- **Diagnostic** JSPs under cargo `target/` are not part of `src/` — do not document or extend until product-specified.
- **Admin access-grants UI** is not implemented in source; grants today are created via funnel API + redeem.
- Prefer extending existing CSS tokens/classes over new hex values or parallel stylesheets.

When in doubt: update this inventory first, then implement in dependency order.
