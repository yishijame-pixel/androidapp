#!/usr/bin/env python3
"""Regenerate all 23 levels: distinct terrain + guaranteed connectivity."""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).parent


def _load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader
    spec.loader.exec_module(mod)
    return mod


def _finalize_level(data: dict, conn) -> None:
    conn.heal_connectivity(data)
    conn.fix_link_portal_markers(data)
    conn.validate_level(data)


def _write_level(data: dict) -> None:
    w, h = data["width"], data["height"]
    for row in data["grid"]:
        if len(row) != w:
            raise ValueError(f"L{data['id']} bad row width {len(row)} != {w}")
    if len(data["grid"]) != h:
        raise ValueError(f"L{data['id']} bad height")
    path = ROOT / f"level_{data['id']:03d}.json"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"  wrote {path.name} ({w}x{h})")


def regen_foundation(conn) -> None:
    print("L1-L10: hand-crafted layouts")
    gen = _load_module("gen_levels", ROOT / "_gen_levels.py")
    for lv in gen.LEVELS:
        data = json.loads(json.dumps(lv))
        _finalize_level(data, conn)
        _write_level(data)

    print("L11-L13: courtyard layouts")
    court = _load_module("gen_court", ROOT / "_gen_courtyard_levels.py")
    for lv in court.LEVELS:
        data = json.loads(json.dumps(lv))
        _finalize_level(data, conn)
        _write_level(data)


def regen_extreme(conn) -> None:
    print("L14-L23: distinct extreme layouts")
    extreme = _load_module("gen_extreme", ROOT / "_gen_extreme_levels.py")
    prev = 0
    for lid in range(14, 24):
        data = extreme.make_level(lid)
        _finalize_level(data, conn)
        cx = extreme.complexity(lid, data["width"], data["height"], len(data["hazards"]), data["grid"])
        if prev and cx < prev:
            print(f"  note L{lid} complexity {cx} (prev {prev})")
        _write_level(data)
        prev = max(prev, cx)


def main() -> None:
    conn = _load_module("conn", ROOT / "_level_connectivity.py")
    regen_foundation(conn)
    regen_extreme(conn)
    validate = _load_module("validate", ROOT / "_validate_levels.py")
    errors: list[str] = []
    for p in sorted(ROOT.glob("level_*.json")):
        errors.extend(validate.validate(p))
        data = json.loads(p.read_text(encoding="utf-8"))
        try:
            conn.validate_level(data)
        except ValueError as e:
            errors.append(f"{p.name}: {e}")
    if errors:
        print("\n".join(errors))
        sys.exit(1)
    print(f"OK: {len(list(ROOT.glob('level_*.json')))} connected distinct levels")


if __name__ == "__main__":
    main()
