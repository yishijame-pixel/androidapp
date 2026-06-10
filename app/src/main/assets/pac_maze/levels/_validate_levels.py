#!/usr/bin/env python3
"""Validate pac maze level grid row widths."""
import json
import sys
from pathlib import Path

def validate(path: Path) -> list[str]:
    errors = []
    data = json.loads(path.read_text(encoding="utf-8"))
    w, h = data["width"], data["height"]
    grid = data["grid"]
    if len(grid) != h:
        errors.append(f"{path.name}: expected {h} rows, got {len(grid)}")
    for i, row in enumerate(grid):
        if len(row) != w:
            errors.append(f"{path.name}: row {i} width {len(row)} != {w}: {row!r}")
    return errors

if __name__ == "__main__":
    root = Path(__file__).parent
    all_errors = []
    for p in sorted(root.glob("level_*.json")):
        all_errors.extend(validate(p))
    if all_errors:
        print("\n".join(all_errors))
        sys.exit(1)
    print(f"OK: {len(list(root.glob('level_*.json')))} levels")
