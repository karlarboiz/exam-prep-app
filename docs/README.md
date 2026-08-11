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

## Features

- [access-grants/](features/access-grants/overview.md) — funnel tokens, redeem, expiry gate
- [auth/](features/auth/login.md) — login / register / logout
- [jwt-auth-filter/](features/jwt-auth-filter/jwt.md) — session JWT + public paths
- Admin subjects / exams / questions / users
- Take exam, results & history

```
docs/
├── PRE-IMPLEMENTATION.md   ← start-task checklist
├── UI-GUIDE.md             ← visual system front door
├── feature-tracker/        ← Done / Pending by category
├── features/
│   ├── access-grants/
│   ├── auth/
│   └── …
├── models/
├── pages/
└── ui-rules/
```
