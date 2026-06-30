#!/usr/bin/env python3
"""校验 dist/platformer 内容目录与企业级 manifest 完整性。"""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Dict, List, Set

REQUIRED_CHARACTER_CLIPS = {"idle", "run", "jump", "die"}
RECOMMENDED_CHARACTER_CLIPS = {"walk", "slide"}
REQUIRED_ENEMY_CLIPS = {"die"}
REQUIRED_ENEMY_LOCOMOTE = {"walk", "run", "idle"}


def load_json(path: Path) -> Dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def validate_manifest(asset_dir: Path, role: str) -> List[str]:
    errors: List[str] = []
    manifest_path = asset_dir / "anim_manifest.json"
    if not manifest_path.is_file():
        return [f"{asset_dir.name}: missing anim_manifest.json"]

    data = load_json(manifest_path)
    clips = data.get("clips") or {}
    if not clips:
        errors.append(f"{asset_dir.name}: no clips")

    clip_names = {k.lower() for k in clips.keys()}
    if role == "character":
        missing = REQUIRED_CHARACTER_CLIPS - clip_names
        if missing:
            errors.append(f"{asset_dir.name}: missing required clips {sorted(missing)}")
    else:
        if "die" not in clip_names:
            errors.append(f"{asset_dir.name}: missing required clip die")
        if not (clip_names & REQUIRED_ENEMY_LOCOMOTE):
            errors.append(f"{asset_dir.name}: missing locomotion clip (walk/run/idle)")

    if role == "character" and not data.get("normalized"):
        errors.append(f"{asset_dir.name}: not normalized")

    if role == "character":
        canvas = data.get("canvas")
        anchor = data.get("anchorFrac")
        if not canvas or not anchor:
            errors.append(f"{asset_dir.name}: missing canvas/anchorFrac")
        elif anchor.get("y", 0) < 0.65 or anchor.get("y", 0) > 0.98:
            errors.append(f"{asset_dir.name}: suspicious anchorFrac.y={anchor.get('y')}")

    for clip_name, entry in clips.items():
        if not isinstance(entry, dict):
            continue
        count = int(entry.get("count", 0))
        folder = entry.get("folder", clip_name)
        prefix = entry.get("prefix", clip_name)
        folder_path = asset_dir / folder
        if not folder_path.is_dir():
            errors.append(f"{asset_dir.name}/{clip_name}: folder missing {folder}")
            continue
        pngs = sorted(folder_path.glob(f"{prefix}_*.png"))
        if len(pngs) < count:
            errors.append(f"{asset_dir.name}/{clip_name}: expected {count} frames, found {len(pngs)}")

    return errors


def validate_tileset(tile_dir: Path) -> List[str]:
    errors: List[str] = []
    manifest_path = tile_dir / "tileset_manifest.json"
    tiles_dir = tile_dir / "tiles"
    if not tiles_dir.is_dir():
        return [f"{tile_dir.name}: missing tiles/"]
    tile_count = 18
    if manifest_path.is_file():
        data = load_json(manifest_path)
        tile_count = int(data.get("tileCount", 18))
    for i in range(1, tile_count + 1):
        if not (tiles_dir / f"{i}.png").is_file():
            errors.append(f"{tile_dir.name}: missing tiles/{i}.png")
    return errors


def validate_catalog(catalog_path: Path, root: Path) -> List[str]:
    errors: List[str] = []
    if not catalog_path.is_file():
        return [f"missing catalog: {catalog_path}"]
    catalog = load_json(catalog_path)

    seen_ids: Set[str] = set()
    for char in catalog.get("characters", []):
        cid = char.get("id")
        if not cid:
            errors.append("character entry missing id")
            continue
        if cid in seen_ids:
            errors.append(f"duplicate character id: {cid}")
        seen_ids.add(cid)
        asset_root = char.get("assetRoot", "")
        source = char.get("source")
        if source in ("pac_maze", "local_apk"):
            continue
        asset_dir = root / asset_root.replace("platformer_characters/", "characters/").replace("platformer/", "")
        if not asset_dir.is_dir():
            # try direct
            alt = root / "characters" / cid
            if alt.is_dir():
                asset_dir = alt
            else:
                errors.append(f"{cid}: asset dir not found ({asset_root})")
                continue
        errors.extend(validate_manifest(asset_dir, "character"))

    for enemy in catalog.get("enemies", []):
        eid = enemy.get("id")
        asset_dir = root / "enemies" / eid
        if asset_dir.is_dir():
            errors.extend(validate_manifest(asset_dir, "enemy"))
        else:
            errors.append(f"{eid}: enemy dir not found")

    for tile in catalog.get("tilesets", []):
        tid = tile.get("id")
        tdir = Path(__file__).resolve().parents[3] / "app" / "src" / "main" / "assets" / "platformer" / "tilesets" / tid
        if not tdir.is_dir():
            tdir = root / "tilesets" / tid
        if tdir.is_dir():
            errors.extend(validate_tileset(tdir))
        else:
            errors.append(f"{tid}: tileset dir not found")

    return errors


def main(argv: List[str]) -> int:
    root = Path(argv[1]) if len(argv) > 1 else Path("dist/platformer")
    catalog = root / "content_catalog.json"
    errors = validate_catalog(catalog, root)
    if errors:
        print("VALIDATION FAILED:")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"OK: {catalog} validated")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
