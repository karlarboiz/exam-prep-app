# Pre-implementation guide

Read this **before writing or changing code**. It is the ordered checklist for starting a task: what the stack is, what depends on what, and the steps to take before you implement.

It is **not** the place to track missing or pending product work. Status of features (Done / Pending) lives in [feature-tracker/](feature-tracker/README.md).

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

1. Confirm you are touching the right area (routes, models, filters) for the task.
2. Check [feature-tracker/](feature-tracker/README.md) for the item’s status and notes.
3. Walk the **pre-implementation checklist** top to bottom — do not skip layers.
4. Follow the **build / dependency order** so models and schema land before servlets and JSPs.
5. Before UI work, skim the [UI guide](UI-GUIDE.md) and reuse existing classes.
6. After implementation: update feature docs under `features/` / `models/` / `pages/`, then sync [feature-tracker](#feature-tracker-after-every-task) (required).

---

## Feature-tracker after every task

Every time a task is implemented (or deliberately deferred), sync [feature-tracker/](feature-tracker/README.md):

1. **Search** the category files under `docs/feature-tracker/` for the task (or a clear match by wording).
2. **If it is present** — set **Status** to **Done** when the work shipped, or leave / set **Pending** if it is not finished (partial work, blocked, or explicitly out of scope for this change). Update **Notes** when helpful.
3. **If it is not present** — add a new row in the **category file that fits best** (auth, access-grants, admin-content, exam-results, examinee-tracking, security, diagnostic, weekly-regimen, or a new category file + link from [feature-tracker/README.md](feature-tracker/README.md) if none fit).
4. Do this even for small fixes that close a tracked gap — the tracker is the source of truth for Done / Pending.

Do **not** put backlog lists in this pre-implementation guide; only update the tracker files.

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
9. Feature-tracker sync (update existing row, or add row in best-fit category)
10. Manual smoke (login → capability → logout / expiry)
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
- [ ] Matching row in [feature-tracker/](feature-tracker/README.md) identified (or added)

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
- [ ] Feature-tracker synced (see [Feature-tracker after every task](#feature-tracker-after-every-task)): existing row updated to Done or Pending, **or** new row added in the best-fit category file
- [ ] Links from [docs README](README.md) if it is a new area
- [ ] Smoke path written (steps below)

---

## Smoke paths (baseline)

Run these after any auth, grant, or exam change.

### Admin

1. Login `admin` / seeded password → `/admin/dashboard`
2. Create subject → question → exam (attach questions, set order, set active)
3. Confirm user list loads; change a student's exam level or role
4. `/admin/questions` → download template, import (batch defaults to `cse-import-YYYY-MM-DD`), export
4. `/admin/access-grants` → mint token → raw token + register link shown once; grant appears in the table
5. `/admin/integrity` → flagged list loads (empty or with suspect leaves)

### Funnel → student

1. `POST /api/access-tokens` with `X-Api-Key` → raw token once
2. Open `/register`, paste the token (or a one-time `?token=` that is stripped into the session) → create user
3. Login as that user → `/user/diagnostic` → complete → `/user/dashboard` shows this week’s regimen
4. Start this week’s exam → submit → study plan + email outbox row; review misses (no new official score)
5. History shows attempt; reopen result review (including unanswered)

### Expiry gate

1. With a redeemed grant past `expires_at`, hit `/user/dashboard` → redirect `/user/subscription-expired`
2. Login still works; quiz routes stay blocked
3. Admin routes remain reachable for `ADMIN`

### Account

1. Login as admin or student → header account chip → `/account`
2. Profile shows username / email editable; role and exam level stay read-only
3. Wrong current password on profile save → error, username/email unchanged
4. Valid profile save → success banner; login with the new username works
5. Wrong current password on password change → error, old password still works
6. Valid password change → success banner; login with the new password works
7. `/forgot-password` with a real email → outbox row; `/reset-password?token=` strips into session → new password works

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
| Feature Done / Pending status | [feature-tracker/](feature-tracker/README.md) |
| This ordered start-task checklist | **this file** |

---

## Do-not-assume

- Prefer extending existing CSS tokens/classes over new hex values or parallel stylesheets.
- Cargo/`target/` artifacts are not product source — confirm something exists under `src/` before documenting or extending it.
- For what is Pending vs Done, trust [feature-tracker/](feature-tracker/README.md), not memory.

When in doubt: run this checklist, then implement in dependency order, then update the tracker.
