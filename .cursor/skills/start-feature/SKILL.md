---
name: start-feature
description: Start a product feature using the pre-implementation checklist, feature tracker, and layer order (schema → DAO → service → servlet → JSP). Use when the user asks to implement, build, add, or start a feature, page, or capability in this exam-prep app.
---

# Start a feature

1. Read `docs/PRE-IMPLEMENTATION.md` and find or add the row in `docs/feature-tracker/`.
2. Identify actor, route, and out of scope. Skim the matching `docs/features/` and `docs/pages/` notes.
3. For UI work, skim `docs/UI-GUIDE.md` and reuse existing classes.
4. Implement in order: schema + model → DAO → service → filter (only if auth/entitlement changes) → servlet → JSP.
5. Do not add Spring, a second stylesheet, or SQL in JSP.
6. After code: update feature/model/page docs, then sync the tracker to Done or Pending.

If the request is docs-only or Cursor-config-only, skip product layers and say so.
