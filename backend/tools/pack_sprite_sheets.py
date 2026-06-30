#!/usr/bin/env python3
"""
将已 normalize 的逐帧 PNG 自动拼成精灵图（sprite sheet），并写回 anim_manifest.json。

用法:
  python backend/tools/pack_sprite_sheets.py dist/pac_maze_skins/food_chick_walker_pro_max
  python backend/tools/pack_sprite_sheets.py --all dist/pac_maze_skins
  python backend/tools/pack_sprite_sheets.py --prune-frames dist/pac_maze_skins/food_chick_walker_pro_max

输出（每个 clip）:
  walk/walk_sheet.webp
  manifest.clips.walk.sheet = { file, columns, rows, cellW, cellH }

归一化皮肤（canvas 固定）用等分网格；非归一化角色取该 clip 各帧 max(w,h) 作为 cell。
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

try:
    from PIL import Image
except ImportError:
    print("需要 Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

CLIP_FOLDERS = {
    "idle": ("idle", "idle"),
    "walk": ("walk", "walk"),
    "run": ("run", "run"),
    "jump": ("jump", "jump"),
    "attack": ("attack", "attack"),
    "die": ("die", "die"),
    "slide": ("slide", "slide"),
    "shoot": ("shoot", "shoot"),
    "fly": ("fly", "fly"),
    "climb": ("climb", "climb"),
    "glide": ("glide", "glide"),
    "hurt": ("hurt", "hurt"),
    "fall": ("fall", "fall"),
}


def load_manifest(asset_dir: Path) -> dict:
    path = asset_dir / "anim_manifest.json"
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8-sig"))


def save_manifest(asset_dir: Path, data: dict) -> None:
    (asset_dir / "anim_manifest.json").write_text(
        json.dumps(data, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def sorted_pngs(folder: Path, prefix: str) -> List[Path]:
    import re

    pattern = re.compile(rf"^{re.escape(prefix)}_(\d+)\.png$", re.I)
    files: List[Tuple[int, Path]] = []
    if not folder.is_dir():
        return []
    for p in folder.iterdir():
        if not p.is_file() or p.name.endswith("_sheet.webp"):
            continue
        m = pattern.match(p.name)
        if m:
            files.append((int(m.group(1)), p))
    files.sort(key=lambda t: t[0])
    return [p for _, p in files]


def grid_for_count(count: int) -> Tuple[int, int]:
    cols = max(1, math.ceil(math.sqrt(count)))
    rows = max(1, math.ceil(count / cols))
    return cols, rows


def pack_frames_to_sheet(
    frame_paths: List[Path],
    cell_w: int,
    cell_h: int,
    out_path: Path,
) -> Tuple[int, int]:
    count = len(frame_paths)
    if count == 0:
        raise ValueError("no frames")
    cols, rows = grid_for_count(count)
    sheet_w = cols * cell_w
    sheet_h = rows * cell_h
    sheet = Image.new("RGBA", (sheet_w, sheet_h), (0, 0, 0, 0))
    for index, path in enumerate(frame_paths):
        col = index % cols
        row = index // cols
        with Image.open(path) as img:
            frame = img.convert("RGBA")
            fw, fh = frame.size
            if fw != cell_w or fh != cell_h:
                # 居中贴入 cell（非归一化资源）
                ox = (cell_w - fw) // 2
                oy = (cell_h - fh) // 2
                cell = Image.new("RGBA", (cell_w, cell_h), (0, 0, 0, 0))
                cell.paste(frame, (ox, oy), frame)
                frame = cell
            sheet.paste(frame, (col * cell_w, row * cell_h))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path, format="WEBP", lossless=True, quality=90, method=6)
    return cols, rows


def cell_size_for_clip(
    frame_paths: List[Path],
    manifest: dict,
    clip_name: str,
) -> Tuple[int, int]:
    canvas = manifest.get("canvas") or {}
    cw, ch = int(canvas.get("w", 0)), int(canvas.get("h", 0))
    if manifest.get("normalized") and cw > 0 and ch > 0:
        return cw, ch
    max_w = max_h = 1
    for path in frame_paths:
        with Image.open(path) as img:
            w, h = img.size
            max_w = max(max_w, w)
            max_h = max(max_h, h)
    return max_w, max_h


def pack_asset_dir(asset_dir: Path, prune_frames: bool = False) -> bool:
    manifest = load_manifest(asset_dir)
    clips = manifest.get("clips") or {}
    if not clips:
        print(f"  skip (no clips): {asset_dir.name}")
        return False

    changed = False
    for clip_name, clip_val in clips.items():
        if isinstance(clip_val, int):
            count = clip_val
            folder_name, prefix = CLIP_FOLDERS.get(clip_name.lower(), (clip_name, clip_name))
        elif isinstance(clip_val, dict):
            count = int(clip_val.get("count") or 0)
            folder_name = clip_val.get("folder") or CLIP_FOLDERS.get(clip_name.lower(), (clip_name, clip_name))[0]
            prefix = clip_val.get("prefix") or CLIP_FOLDERS.get(clip_name.lower(), (clip_name, clip_name))[1]
        else:
            continue
        if count <= 0:
            continue

        folder = asset_dir / folder_name
        pngs = sorted_pngs(folder, prefix)[:count]
        if not pngs:
            print(f"  skip clip {clip_name}: no png in {folder}")
            continue

        cell_w, cell_h = cell_size_for_clip(pngs, manifest, clip_name)
        sheet_name = f"{prefix}_sheet.webp"
        sheet_path = folder / sheet_name

        cols, rows = pack_frames_to_sheet(pngs, cell_w, cell_h, sheet_path)
        print(
            f"  {asset_dir.name}/{clip_name}: {count} frames → "
            f"{sheet_path.name} ({cols}x{rows} grid, cell {cell_w}x{cell_h})"
        )

        if not isinstance(clips[clip_name], dict):
            clips[clip_name] = {
                "count": count,
                "folder": folder_name,
                "prefix": prefix,
            }
        clips[clip_name]["sheet"] = {
            "file": sheet_name,
            "columns": cols,
            "rows": rows,
            "cellW": cell_w,
            "cellH": cell_h,
        }
        changed = True

        if prune_frames:
            for p in pngs:
                p.unlink(missing_ok=True)

    if changed:
        manifest["clips"] = clips
        manifest["schemaVersion"] = max(int(manifest.get("schemaVersion") or 2), 3)
        save_manifest(asset_dir, manifest)
    return changed


def main(argv: List[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 1

    prune = "--prune-frames" in argv
    args = [a for a in argv[1:] if a != "--prune-frames"]

    if args[0] == "--all":
        root = Path(args[1]) if len(args) > 1 else Path("dist/pac_maze_skins")
        if not root.is_dir():
            print(f"not found: {root}", file=sys.stderr)
            return 1
        ok = sum(
            1 for d in sorted(root.iterdir())
            if d.is_dir() and (d / "anim_manifest.json").is_file() and pack_asset_dir(d, prune)
        )
        print(f"packed sheets: {ok} assets")
        return 0

    target = Path(args[0])
    if not target.is_dir():
        print(f"not found: {target}", file=sys.stderr)
        return 1
    return 0 if pack_asset_dir(target, prune) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
