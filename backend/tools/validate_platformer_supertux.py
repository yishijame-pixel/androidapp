#!/usr/bin/env python3
"""校验 dist/platformer_supertux 与 platformer_sfx 完整性。"""
from __future__ import annotations

import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DIST_ST = REPO / "dist" / "platformer_supertux"
DIST_SFX = REPO / "dist" / "platformer_sfx"


def fail(msg: str) -> None:
    print(f"FAIL: {msg}")
    sys.exit(1)


def check_sfx() -> None:
    manifest_path = DIST_SFX / "sfx_manifest.json"
    if not manifest_path.is_file():
        fail(f"missing {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    events = manifest.get("events") or {}
    required = ["player_jump", "bgm_platformer", "level_clear"]
    for key in required:
        rel = events.get(key, {}).get("file")
        if not rel:
            fail(f"sfx manifest missing event {key}")
        file = DIST_SFX / rel
        if not file.is_file():
            fail(f"missing sfx file {file}")


def check_supertux() -> None:
    catalog_path = DIST_ST / "content_catalog.json"
    if not catalog_path.is_file():
        fail(f"missing {catalog_path}")
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    levels = catalog.get("levels") or []
    if len(levels) < 100:
        fail(f"expected >=100 levels, got {len(levels)}")
    chapters = catalog.get("chapters") or []
    if len(chapters) < 4:
        fail(f"expected 4 chapters, got {len(chapters)}")
    ids = {lv["id"] for lv in levels}
    for lv in levels:
        lid = lv["id"]
        level_json = DIST_ST / "levels" / f"level_{lid}" / "level.json"
        if not level_json.is_file():
            fail(f"missing {level_json}")
        data = json.loads(level_json.read_text(encoding="utf-8"))
        if not data.get("rows") and not data.get("useCampaignScroll"):
            fail(f"level {lid} has no rows and no scroll flag")
        if not data.get("chapterId"):
            fail(f"level {lid} missing chapterId")
    if 901 not in ids or max(ids) < 1011:
        fail(f"level id range unexpected: min={min(ids)} max={max(ids)}")
    tile = DIST_ST / "tilesets" / "antarctic" / "tileset_manifest.json"
    if not tile.is_file():
        fail(f"missing {tile}")
    atlas = DIST_ST / "tilesets" / "antarctic" / "atlas_manifest.json"
    if not atlas.is_file():
        fail(f"missing {atlas}")
    bv = (DIST_ST / "bundle_version.txt").read_text(encoding="utf-8").strip()
    if int(bv) < 4:
        fail(f"bundle_version must be >=4, got {bv}")
    print(f"OK levels={len(levels)} chapters={len(chapters)} bundleVersion={bv}")


def main() -> None:
    check_sfx()
    check_supertux()
    print("OK platformer_sfx + platformer_supertux validation passed")


if __name__ == "__main__":
    main()
