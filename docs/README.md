# Exam Prep App — Documentation

**Start here before coding:**

1. [PRE-IMPLEMENTATION.md](PRE-IMPLEMENTATION.md) — inventory, dependency order, checklists, smoke paths
2. [UI-GUIDE.md](UI-GUIDE.md) — tokens, layout, components, screen map

Then drill into the folders below as needed.

| Folder | Purpose |
|--------|---------|
| [features/](features/) | How each capability works (servlets, services, flows) |
| [models/](models/) | Domain objects and enums |
| [pages/](pages/) | JSP screens and their routes |
| [ui-rules/](ui-rules/) | Split styling notes (also summarized in UI guide) |

## Features

- [access-grants/](features/access-grants/overview.md) — one-time purchase tokens, redeem, expiry gate, revoke
- [auth/](features/auth/login.md) — login / register / logout
- [jwt-auth-filter/](features/jwt-auth-filter/jwt.md) — session JWT + public paths
- [diagnostic/](features/diagnostic/overview.md) — first-login placement diagnostic (hard gate + sampled)
- Admin subjects / exams / questions / users / [access grants](features/access-grants/admin-access-grants.md)
- [question-import/](features/question-import/overview.md) — Excel bulk import into question bank
- Take exam, results & history
- [testing/](features/testing/overview.md) — automated JUnit tests

```
docs/
├── PRE-IMPLEMENTATION.md   ← ordered task guide
├── UI-GUIDE.md             ← visual system front door
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
