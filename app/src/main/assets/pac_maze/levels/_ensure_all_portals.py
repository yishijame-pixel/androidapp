#!/usr/bin/env python3
"""Add LINK portal markers to all bundled levels."""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).parent


def load(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader
    spec.loader.exec_module(mod)
    return mod


def main() -> None:
    conn = load("conn", ROOT / "_level_connectivity.py")
    errors: list[str] = []
    for path in sorted(ROOT.glob("level_*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        conn.ensure_link_portals(data)
        try:
            conn.validate_level(data)
        except ValueError as e:
            conn.heal_connectivity(data)
            conn.ensure_link_portals(data)
            try:
                conn.validate_level(data)
            except ValueError as e2:
                errors.append(f"{path.name}: {e2}")
                continue
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        links = [m for m in data["markers"] if m.get("tag") == "LINK"]
        print(f"{path.name}: {len(links)} LINK @ {[(m['x'], m['y']) for m in links]}")
    if errors:
        print("\n".join(errors))
        sys.exit(1)
    print("OK")


if __name__ == "__main__":
    main()
