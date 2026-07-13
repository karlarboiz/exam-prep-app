# Exam Prep App

Java servlet exam practice app (JSP + H2).

Student access is **subscription-token gated**: the funnel calls this app to create a one-time access token; users redeem it at registration. The locked grant’s `expires_at` controls how long quizzes remain available.

## Documentation

Full project docs live under [`docs/`](docs/README.md):

- **features/** — access-grants, auth, admin CRUD, take exam, results, JWT filter
- **models/** — domain objects (including AccessGrant)
- **pages/** — JSP screen notes
- **ui-rules/** — colors, layout, forms, components

## Funnel API (brief)

`POST /api/access-tokens` with header `X-Api-Key: <funnel.api.key>`. See [docs/features/access-grants/create-token-api.md](docs/features/access-grants/create-token-api.md).
