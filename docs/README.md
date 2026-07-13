# Exam Prep App — Documentation

Project documentation is organized by concern:

| Folder | Purpose |
|--------|---------|
| [features/](features/) | How each capability works (servlets, services, flows) |
| [models/](models/) | Domain objects and enums |
| [pages/](pages/) | JSP screens and their routes |
| [ui-rules/](ui-rules/) | Styling and UX conventions |

## Features

- [access-grants/](features/access-grants/overview.md) — funnel tokens, redeem, expiry gate
- [auth/](features/auth/login.md) — login / register / logout
- [jwt-auth-filter/](features/jwt-auth-filter/jwt.md) — session JWT + public paths
- Admin subjects / exams / questions / users
- Take exam, results & history

```
docs/
├── features/
│   ├── access-grants/
│   ├── auth/
│   └── …
├── models/
├── pages/
└── ui-rules/
```
