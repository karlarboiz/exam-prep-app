---
name: ship-deploy
description: Explain and follow the GitHub Actions deploy path for this exam-prep app (WAR CI, Docker image on GHCR). Use when the user asks to ship, deploy, publish, push to main, or how releases reach GitHub Container Registry.
---

# Ship / deploy

This app deploys from GitHub Actions, not from a local kubectl/ftp step.

1. Confirm `mvn -B package` passes locally when the change is risky.
2. Push the branch. Open a PR if the user asked for one.
3. Merge or push to `main` / `master` (only when the user asks).
4. **CI** (`.github/workflows/ci.yml`) — JDK 17, `mvn package`, uploads `exam-prep-app.war`. Also runs on `develop` and on PRs.
5. **Docker** (`.github/workflows/docker.yml`) — on push to `main`/`master`, builds and pushes `ghcr.io/karlarboiz/exam-prep-app` (commit SHA tag; `latest` on the default branch).

Do not add a second deploy path. Do not force-push `main`. Pull-request and commit only when the user explicitly asks.
