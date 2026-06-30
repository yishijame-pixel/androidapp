#!/usr/bin/env python3
"""Apply Chinese level titles to platformer_supertux catalog + level.json files."""
from __future__ import annotations

import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
from supertux_level_registry import STL_TITLES_ZH, title_for_stl  # noqa: E402

ASSETS = REPO / "app" / "src" / "main" / "assets" / "platformer_supertux"
DIST = REPO / "dist" / "platformer_supertux"
CATALOG_NAME = "content_catalog.json"


def chapter_subtitle(chapter_id: str) -> str:
    return {
        "supertux_antarctic": "南极探险 · SuperTux",
        "supertux_forest": "森林秘境 · SuperTux",
        "supertux_bonus": "Bonus 挑战 · SuperTux",
        "supertux_redmond": "Redmond 复仇 · SuperTux",
    }.get(chapter_id, "SuperTux · 改编")


def apply_root(root: Path) -> int:
    catalog_path = root / CATALOG_NAME
    if not catalog_path.is_file():
        print(f"skip (no catalog): {root}")
        return 0
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    updated = 0
    for entry in catalog.get("levels", []):
        stl = entry.get("sourceStl") or ""
        zh = title_for_stl(stl)
        if entry.get("title") != zh:
            entry["title"] = zh
            updated += 1
        level_id = entry["id"]
        level_json_path = root / "levels" / f"level_{level_id}" / "level.json"
        if not level_json_path.is_file():
            continue
        level_data = json.loads(level_json_path.read_text(encoding="utf-8"))
        changed = False
        if level_data.get("title") != zh:
            level_data["title"] = zh
            changed = True
        sub = chapter_subtitle(entry.get("chapterId") or level_data.get("chapterId") or "")
        if level_data.get("subtitle") != sub:
            level_data["subtitle"] = sub
            changed = True
        if changed:
            level_json_path.write_text(
                json.dumps(level_data, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
    catalog_path.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"OK {root.relative_to(REPO)}: catalog titles synced ({updated} catalog entries updated)")
    return updated


def main() -> None:
    total = 0
    for root in (ASSETS, DIST):
        if root.is_dir():
            total += apply_root(root)
    if total == 0 and not ASSETS.is_dir():
        raise SystemExit(f"assets not found: {ASSETS}")
    print(f"done — {len(STL_TITLES_ZH)} zh titles in registry")


if __name__ == "__main__":
    main()
