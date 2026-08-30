# Exam Prep App — agent notes

Java 17 Maven WAR. Jakarta Servlet 6 + JSP/JSTL, H2 + HikariCP, JWT cookie `access_token`, one-time access grants. Package root: `com.examprep`.

## Before coding

1. Read [docs/PRE-IMPLEMENTATION.md](docs/PRE-IMPLEMENTATION.md) (stack, dependency order, checklist).
2. Check [docs/feature-tracker/](docs/feature-tracker/README.md) for Done / Pending.
3. For UI, follow [docs/UI-GUIDE.md](docs/UI-GUIDE.md) — one stylesheet `src/main/webapp/css/app.css`.

Config: `src/main/resources/app.properties` · Schema: `src/main/resources/schema.sql`

## Layers (build down, do not skip)

Schema + model → DAO → service → filter (only if auth/entitlement changes) → servlet → JSP → docs → feature-tracker → smoke.

## Commands

| Goal | Command |
|------|---------|
| Test + WAR | `mvn -B package` |
| Local Tomcat (port 8080) | `mvn cargo:run` |
| Docker image | `docker build -t exam-prep-app .` |
| Docker run | `docker run --rm -p 8080:8080 exam-prep-app` |

App: http://localhost:8080

## Deploy

Push to `main`/`master`: GitHub Actions runs CI (`mvn package`) then publishes `ghcr.io/karlarboiz/exam-prep-app`.

## After every product task

Update the matching row in `docs/feature-tracker/` (or add one). Update `docs/features/`, `docs/models/`, or `docs/pages/` when behavior, fields, or screens change.
