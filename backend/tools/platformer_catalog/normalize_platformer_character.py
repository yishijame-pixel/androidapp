#!/usr/bin/env python3
"""
企业级横版素材 normalize：trim → 统一画布 → 固定脚点 → anim_manifest v2。
复用 pac_maze normalize 算法，支持 platformer characters/enemies。
"""
from __future__ import annotations

import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

try:
    from PIL import Image
except ImportError:
    print("需要 Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

ALPHA_FLOOR = 14
CANVAS_SIDE_PAD = 2


@dataclass
class TrimmedFrame:
    clip: str
    path: Path
    image: Image.Image
    feet_x: float
    feet_y: float


def alpha_bbox(img: Image.Image) -> Optional[Tuple[int, int, int, int]]:
    rgba = img.convert("RGBA")
    w, h = rgba.size
    pixels = rgba.load()
    min_x, min_y, max_x, max_y = w, h, -1, -1
    step = 2 if w * h > 640_000 else 1
    for y in range(0, h, step):
        for x in range(0, w, step):
            if pixels[x, y][3] > ALPHA_FLOOR:
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
    if max_x < min_x or max_y < min_y:
        return None
    pad_x = max(1, int((max_x - min_x + 1) * 0.02))
    pad_y = min(2, max(1, int((max_y - min_y + 1) * 0.008)))
    return max(0, min_x - pad_x), max(0, min_y - pad_y), min(w, max_x + pad_x + 1), min(h, max_y + pad_y)


def trim_image(img: Image.Image) -> Image.Image:
    box = alpha_bbox(img)
    if box is None:
        return img.convert("RGBA")
    left, top, right, bottom = box
    if left == 0 and top == 0 and right == img.width and bottom == img.height:
        return img.convert("RGBA")
    return img.crop(box).convert("RGBA")


def detect_feet(trimmed: Image.Image) -> Tuple[float, float]:
    w, h = trimmed.size
    if w <= 1 or h <= 1:
        return w / 2, h - 1
    pixels = trimmed.load()
    max_y = -1
    for y in range(h):
        for x in range(0, w, max(1, w // 128)):
            if pixels[x, y][3] > ALPHA_FLOOR:
                max_y = max(max_y, y)
    if max_y < 0:
        return w / 2, h - 1
    band_top = max(0, max_y - max(2, int(h * 0.24)))
    best_y = max_y
    best_span = 0
    best_cx = w / 2
    for y in range(max_y, band_top - 1, -1):
        row_min, row_max = w, -1
        for x in range(0, w, max(1, w // 128)):
            if pixels[x, y][3] > ALPHA_FLOOR:
                row_min = min(row_min, x)
                row_max = max(row_max, x)
        span = row_max - row_min + 1 if row_max >= row_min else 0
        if span > best_span or (span == best_span and y > best_y):
            best_span = span
            best_y = y
            best_cx = (row_min + row_max) / 2 if span > 0 else w / 2
    return best_cx, best_y + 0.5


def sorted_pngs(folder: Path, prefix: str) -> List[Path]:
    pattern = re.compile(rf"^{re.escape(prefix)}_(\d+)\.png$", re.I)
    files: List[Tuple[int, Path]] = []
    if not folder.is_dir():
        return []
    for p in folder.iterdir():
        if not p.is_file():
            continue
        m = pattern.match(p.name)
        if m:
            files.append((int(m.group(1)), p))
    files.sort(key=lambda t: t[0])
    return [p for _, p in files]


def load_manifest_clips(asset_dir: Path) -> Dict[str, int]:
    manifest_path = asset_dir / "anim_manifest.json"
    if not manifest_path.is_file():
        return {}
    data = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    clips = data.get("clips") or {}
    out: Dict[str, int] = {}
    for name, value in clips.items():
        if isinstance(value, int):
            out[name.lower()] = value
        elif isinstance(value, dict) and "count" in value:
            out[name.lower()] = int(value["count"])
    return out


def collect_frames(asset_dir: Path, clip_counts: Dict[str, int]) -> List[TrimmedFrame]:
    frames: List[TrimmedFrame] = []
    for clip, count in clip_counts.items():
        if count <= 0:
            continue
        folder = asset_dir / clip
        prefix = clip
        if (asset_dir / "anim_manifest.json").is_file():
            data = json.loads((asset_dir / "anim_manifest.json").read_text(encoding="utf-8-sig"))
            entry = (data.get("clips") or {}).get(clip)
            if isinstance(entry, dict):
                folder = asset_dir / (entry.get("folder") or clip)
                prefix = entry.get("prefix") or clip
        pngs = sorted_pngs(folder, prefix)[:count]
        for path in pngs:
            raw = Image.open(path)
            trimmed = trim_image(raw)
            feet_x, feet_y = detect_feet(trimmed)
            frames.append(TrimmedFrame(clip=clip, path=path, image=trimmed, feet_x=feet_x, feet_y=feet_y))
    return frames


def compute_canvas(frames: List[TrimmedFrame]) -> Tuple[int, int, int, int]:
    max_left = max_right = max_above = max_below = 0.0
    for f in frames:
        w, h = f.image.size
        max_left = max(max_left, f.feet_x)
        max_right = max(max_right, w - f.feet_x)
        max_above = max(max_above, f.feet_y)
        max_below = max(max_below, h - f.feet_y)
    canvas_w = int(max_left + max_right + 2 * CANVAS_SIDE_PAD)
    canvas_h = int(max_above + max_below + 2 * CANVAS_SIDE_PAD)
    anchor_x = int(max_left + CANVAS_SIDE_PAD)
    anchor_y = int(max_above + CANVAS_SIDE_PAD)
    return canvas_w, canvas_h, anchor_x, anchor_y


def paste_normalized(frame: TrimmedFrame, canvas_w: int, canvas_h: int, anchor_x: int, anchor_y: int) -> Image.Image:
    out = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    paste_x = int(anchor_x - frame.feet_x)
    paste_y = int(anchor_y - frame.feet_y)
    out.paste(frame.image, (paste_x, paste_y), frame.image)
    return out


def normalize_asset_dir(asset_dir: Path) -> bool:
    asset_id = asset_dir.name
    clip_counts = load_manifest_clips(asset_dir)
    if not clip_counts:
        print(f"  skip (no clips): {asset_id}")
        return False
    frames = collect_frames(asset_dir, clip_counts)
    if not frames:
        print(f"  skip (no png): {asset_id}")
        return False

    canvas_w, canvas_h, anchor_x, anchor_y = compute_canvas(frames)
    anchor_frac_x = anchor_x / canvas_w
    anchor_frac_y = anchor_y / canvas_h

    for frame in frames:
        normalized = paste_normalized(frame, canvas_w, canvas_h, anchor_x, anchor_y)
        normalized.save(frame.path, format="PNG")

    manifest_path = asset_dir / "anim_manifest.json"
    old = json.loads(manifest_path.read_text(encoding="utf-8-sig")) if manifest_path.is_file() else {}
    platformer_meta = old.get("platformer") or {}

    clips_manifest = {}
    for clip, count in clip_counts.items():
        entry = (old.get("clips") or {}).get(clip)
        if isinstance(entry, dict):
            clips_manifest[clip] = {
                "count": count,
                "folder": entry.get("folder", clip),
                "prefix": entry.get("prefix", clip),
            }
        else:
            clips_manifest[clip] = {"count": count, "folder": clip, "prefix": clip}

    manifest = {
        "schemaVersion": 2,
        "skinId": asset_id,
        "normalized": True,
        "canvas": {"w": canvas_w, "h": canvas_h},
        "anchorFrac": {"x": round(anchor_frac_x, 6), "y": round(anchor_frac_y, 6)},
        "clips": clips_manifest,
        "render": old.get("render") or {"syncWalkCycleToSprite": True, "sampleSize": 1},
        "platformer": platformer_meta,
    }
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"  normalized {asset_id}: {canvas_w}x{canvas_h} frames={len(frames)}")
    return True


def normalize_tree(root: Path, subdirs: Tuple[str, ...] = ("characters", "enemies")) -> int:
    ok = 0
    for sub in subdirs:
        base = root / sub
        if not base.is_dir():
            continue
        for asset_dir in sorted(base.iterdir()):
            if asset_dir.is_dir() and (asset_dir / "anim_manifest.json").is_file():
                if normalize_asset_dir(asset_dir):
                    ok += 1
    return ok


def main(argv: List[str]) -> int:
    if len(argv) < 2:
        print("Usage: normalize_platformer_character.py <asset_dir|dist/platformer [--all]>")
        return 1
    target = Path(argv[1])
    if argv[1] == "--all":
        root = Path(argv[2]) if len(argv) > 2 else Path("dist/platformer")
        count = normalize_tree(root)
        print(f"done: {count} assets normalized")
        return 0
    if not target.is_dir():
        print(f"not found: {target}", file=sys.stderr)
        return 1
    return 0 if normalize_asset_dir(target) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
