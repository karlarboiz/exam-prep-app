# Exam Prep App

Java servlet exam practice app (JSP + H2).

Student access is **subscription-token gated**: the funnel calls this app to create a one-time access token; users redeem it at registration. The locked grant’s `expires_at` controls how long quizzes remain available.

## CI/CD

GitHub Actions workflows:

- **CI** (`.github/workflows/ci.yml`) — on push/PR to `main`/`master`/`develop`: JDK 17 + `mvn package`, uploads `exam-prep-app.war`
- **Docker** (`.github/workflows/docker.yml`) — on push to `main`/`master`: builds image and pushes to `ghcr.io/<owner>/exam-prep-app`

Local Docker run (after build):

```bash
docker build -t exam-prep-app .
docker run --rm -p 8080:8080 exam-prep-app
```

App: http://localhost:8080

## Documentation

Full project docs live under [`docs/`](docs/README.md).

Start with:

- [docs/PRE-IMPLEMENTATION.md](docs/PRE-IMPLEMENTATION.md) — inventory, build order, pre-coding checklist
- [docs/UI-GUIDE.md](docs/UI-GUIDE.md) — colors, layout, components, screen map

Then:

- **features/** — access-grants, auth, admin CRUD, take exam, results, JWT filter
- **models/** — domain objects (including AccessGrant)
- **pages/** — JSP screen notes
- **ui-rules/** — split styling notes (summarized in the UI guide)

## Funnel API (brief)

`POST /api/access-tokens` with header `X-Api-Key: <funnel.api.key>`. See [docs/features/access-grants/create-token-api.md](docs/features/access-grants/create-token-api.md).
