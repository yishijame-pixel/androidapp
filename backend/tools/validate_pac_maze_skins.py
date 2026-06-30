#!/usr/bin/env python3
"""
校验 dist/pac_maze_skins 企业级 manifest + PNG 规范。
用法: python backend/tools/validate_pac_maze_skins.py [dist/pac_maze_skins]
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import List, Tuple

try:
    from PIL import Image
except ImportError:
    Image = None


def validate_manifest(skin_dir: Path) -> List[str]:
    errors: List[str] = []
    manifest_path = skin_dir / "anim_manifest.json"
    if not manifest_path.is_file():
        return [f"{skin_dir.name}: missing anim_manifest.json"]
    try:
        data = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    except json.JSONDecodeError as e:
        return [f"{skin_dir.name}: invalid json: {e}"]

    skin_id = data.get("skinId")
    if skin_id != skin_dir.name:
        errors.append(f"{skin_id}: skinId mismatch folder {skin_dir.name}")

    clips = data.get("clips") or {}
    if not clips:
        errors.append(f"{skin_id}: clips empty")

    schema = data.get("schemaVersion", 1)
    normalized = data.get("normalized", False)

    if schema >= 3:
        for clip_name, clip_data in clips.items():
            if isinstance(clip_data, int):
                sheet = None
            else:
                sheet = clip_data.get("sheet")
            if not sheet and clip_name.lower() in ("walk", "idle", "jump", "attack", "die", "run"):
                errors.append(f"{skin_id}: schema v3+ clip '{clip_name}' missing sheet metadata")

    if schema >= 2 and normalized:
        canvas = data.get("canvas") or {}
        cw, ch = canvas.get("w"), canvas.get("h")
        if not cw or not ch:
            errors.append(f"{skin_id}: normalized but canvas missing")
        anchor = data.get("anchorFrac") or {}
        if "x" not in anchor or "y" not in anchor:
            errors.append(f"{skin_id}: normalized but anchorFrac missing")

        if Image is not None and cw and ch:
            for clip_name, clip_data in clips.items():
                if isinstance(clip_data, int):
                    count, folder, prefix = clip_data, clip_name, clip_name
                    sheet = None
                else:
                    count = clip_data.get("count", 0)
                    folder = clip_data.get("folder", clip_name)
                    prefix = clip_data.get("prefix", clip_name)
                    sheet = clip_data.get("sheet")
                if sheet:
                    sheet_path = skin_dir / folder / sheet.get("file", f"{prefix}_sheet.webp")
                    if not sheet_path.is_file():
                        errors.append(f"{skin_id}: missing sheet {sheet_path.relative_to(skin_dir.parent.parent)}")
                        continue
                    cols = int(sheet.get("columns") or 0)
                    rows = int(sheet.get("rows") or 0)
                    cell_w = int(sheet.get("cellW") or cw)
                    cell_h = int(sheet.get("cellH") or ch)
                    if cols <= 0 or rows <= 0:
                        errors.append(f"{skin_id}: {clip_name} sheet grid invalid")
                        continue
                    if cols * rows < count:
                        errors.append(f"{skin_id}: {clip_name} sheet grid {cols}x{rows} < count {count}")
                    with Image.open(sheet_path) as img:
                        expected = (cols * cell_w, rows * cell_h)
                        if img.size != expected:
                            errors.append(
                                f"{skin_id}: {sheet_path.name} size {img.size} != expected {expected}"
                            )
                    continue
                for i in range(1, count + 1):
                    png = skin_dir / folder / f"{prefix}_{i}.png"
                    if not png.is_file():
                        errors.append(f"{skin_id}: missing {png.relative_to(skin_dir.parent.parent)}")
                        continue
                    with Image.open(png) as img:
                        if img.size != (cw, ch):
                            errors.append(
                                f"{skin_id}: {png.name} size {img.size} != canvas ({cw},{ch})"
                            )
    else:
        for clip_name, clip_data in clips.items():
            if isinstance(clip_data, int):
                count, folder, prefix = clip_data, clip_name, clip_name
                sheet = None
            else:
                count = clip_data.get("count", 0)
                folder = clip_data.get("folder", clip_name)
                prefix = clip_data.get("prefix", clip_name)
                sheet = clip_data.get("sheet")
            if sheet:
                sheet_path = skin_dir / folder / sheet.get("file", f"{prefix}_sheet.webp")
                if not sheet_path.is_file():
                    errors.append(f"{skin_id}: missing sheet {clip_name}")
                continue
            found = list((skin_dir / folder).glob(f"{prefix}_*.png")) if (skin_dir / folder).is_dir() else []
            if len(found) < count:
                errors.append(f"{skin_id}: {clip_name} expected {count} found {len(found)}")

    return errors


def validate_bundle(root: Path) -> Tuple[int, List[str]]:
    version_file = root / "bundle_version.txt"
    if not version_file.is_file():
        return 0, ["missing bundle_version.txt"]
    version = version_file.read_text(encoding="ascii").strip()
    all_errors: List[str] = []
    skin_count = 0
    for skin_dir in sorted(root.iterdir()):
        if not skin_dir.is_dir():
            continue
        if not (skin_dir / "anim_manifest.json").is_file():
            continue
        skin_count += 1
        all_errors.extend(validate_manifest(skin_dir))
    return skin_count, all_errors


def main(argv: List[str]) -> int:
    root = Path(argv[1]) if len(argv) > 1 else Path("dist/pac_maze_skins")
    if not root.is_dir():
        print(f"not found: {root}", file=sys.stderr)
        return 1
    skin_count, errors = validate_bundle(root)
    if errors:
        print(f"FAILED: {len(errors)} issue(s) in {skin_count} skins")
        for e in errors[:50]:
            print(f"  - {e}")
        if len(errors) > 50:
            print(f"  ... and {len(errors) - 50} more")
        return 1
    print(f"OK: {skin_count} skins validated under {root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
