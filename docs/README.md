# Exam Prep App — Documentation

**Start here before coding:**

1. [PRE-IMPLEMENTATION.md](PRE-IMPLEMENTATION.md) — start-task guide (dependency order, checklists, smoke paths)
2. [UI-GUIDE.md](UI-GUIDE.md) — tokens, layout, components, screen map
3. [feature-tracker/](feature-tracker/README.md) — Done / Pending status by category

Then drill into the folders below as needed.

| Folder | Purpose |
|--------|---------|
| [feature-tracker/](feature-tracker/) | What is Done vs Pending (update when you ship) |
| [features/](features/) | How each capability works (servlets, services, flows) |
| [models/](models/) | Domain objects and enums |
| [pages/](pages/) | JSP screens and their routes |
| [ui-rules/](ui-rules/) | Split styling notes (also summarized in UI guide) |
| [content/](content/cse-question-blueprint.md) | Original CSE-PPT question writing spec |

## Features

- [access-grants/](features/access-grants/overview.md) — one-time purchase tokens, redeem, expiry gate, revoke
- [i18n/](features/i18n/overview.md) — Tagalog (default) / English UI switcher
- [auth/](features/auth/login.md) — login / register / logout / [change password](features/auth/change-password.md)
- [jwt-auth-filter/](features/jwt-auth-filter/jwt.md) — session JWT + public paths
- [diagnostic/](features/diagnostic/overview.md) — first-login placement diagnostic (hard gate + sampled)
- Admin subjects / exams / questions / users / [access grants](features/access-grants/admin-access-grants.md)
- [question-import/](features/question-import/overview.md) — Excel bulk import into question bank
- Take exam, results & history
- [weekly-regimen/](features/weekly-regimen/overview.md) — weekly official exam, study plan, review, checkpoint
- [examinee-tracking/](features/examinee-tracking/overview.md) — tab-leave integrity signals
- [testing/](features/testing/overview.md) — automated JUnit tests

```
docs/
├── PRE-IMPLEMENTATION.md   ← start-task checklist
├── UI-GUIDE.md             ← visual system front door
├── feature-tracker/        ← Done / Pending by category
├── features/
│   ├── access-grants/
│   ├── auth/
│   ├── diagnostic/
│   ├── testing/
│   └── …
├── models/
├── pages/
└── ui-rules/
```
