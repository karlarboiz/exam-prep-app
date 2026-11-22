---
name: smoke-run
description: Build and run the exam-prep app locally with Maven Cargo or Docker on port 8080. Use when the user asks to run, smoke-test, start Tomcat, or verify locally.
---

# Smoke / local run

Prefer Maven Cargo when JDK 17 + Maven are available:

```bash
mvn -B test
mvn cargo:run
```

App: http://localhost:8080

Docker alternative:

```bash
docker build -t exam-prep-app .
docker run --rm -p 8080:8080 exam-prep-app
```

After auth, grant, or exam changes, walk the smoke paths in `docs/PRE-IMPLEMENTATION.md` (admin, funnel → student, expiry gate, account, negatives). Do not invent a different port or context path; the WAR deploys at `/`.
