---
name: finish-task
description: Close out product work by syncing docs/feature-tracker and related feature, model, and page docs. Use when a feature is implemented, deferred, or the user asks to wrap up, finish the task, or update the tracker.
---

# Finish a task

1. Search `docs/feature-tracker/` for the work (or a clear wording match).
2. Set **Status** to **Done** if it shipped, or **Pending** if it is partial, blocked, or out of scope. Update **Notes** when useful.
3. If no row exists, add one in the best-fit category file. If none fit, add a category file and link it from `docs/feature-tracker/README.md`.
4. If behavior, fields, or screens changed, update the matching files under `docs/features/`, `docs/models/`, and `docs/pages/`.
5. Skip the tracker only when the change is Cursor/config-only with no product behavior.

Do not leave the tracker stale after product code lands.
