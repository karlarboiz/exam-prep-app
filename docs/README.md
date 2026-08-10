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

- [access-grants/](features/access-grants/overview.md) — funnel tokens, redeem, expiry gate
- [auth/](features/auth/login.md) — login / register / logout
- [jwt-auth-filter/](features/jwt-auth-filter/jwt.md) — session JWT + public paths
- Admin subjects / exams / questions / users
- Take exam, results & history

```
docs/
├── PRE-IMPLEMENTATION.md   ← ordered task guide
├── UI-GUIDE.md             ← visual system front door
├── features/
│   ├── access-grants/
│   ├── auth/
│   └── …
├── models/
├── pages/
└── ui-rules/
```
