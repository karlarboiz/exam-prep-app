#!/usr/bin/env python3
"""Remind the agent to sync docs/feature-tracker after product file edits."""
import json
import sys

PRODUCT_MARKERS = (
    "/src/main/java/",
    "/src/main/webapp/",
    "/src/main/resources/schema.sql",
    "/src/test/",
)


def file_path_from(payload: dict) -> str:
    for key in ("file_path", "path", "filePath", "file"):
        value = payload.get(key)
        if isinstance(value, str) and value:
            return value
    for nested_key in ("edit", "file"):
        nested = payload.get(nested_key)
        if isinstance(nested, dict):
            found = file_path_from(nested)
            if found:
                return found
    return ""


def is_product_path(path: str) -> bool:
    normalized = path.replace("\\", "/")
    return any(marker in normalized for marker in PRODUCT_MARKERS)


def main() -> int:
    raw = sys.stdin.read()
    if not raw.strip():
        print("{}")
        return 0
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        print("{}")
        return 0

    path = file_path_from(payload if isinstance(payload, dict) else {})
    if not is_product_path(path):
        print("{}")
        return 0

    reminder = (
        "Product file changed. Before finishing, sync docs/feature-tracker/ "
        "(Done or Pending) and update docs/features, docs/models, or docs/pages "
        "if behavior, fields, or screens changed."
    )
    print(json.dumps({"additional_context": reminder}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
