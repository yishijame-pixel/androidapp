#!/usr/bin/env python3
"""
企业级皮肤 normalize：trim → 统一画布 → 固定脚点 → 写 anim_manifest.json v2。
用法:
  python backend/tools/normalize_pac_maze_skin.py dist/pac_maze_skins/food_chick_walker_pro_max
  python backend/tools/normalize_pac_maze_skin.py dist/pac_maze_skins --all
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
ANCHOR_BOTTOM_PAD = 2
CANVAS_SIDE_PAD = 2
# Extra top padding for jump hair in normalized canvas
HEAD_CANVAS_PAD_JUMP = 200
# Walk-only skins: minimal headroom (200px made cells ~3× too tall → corridor width under-fill)
HEAD_CANVAS_PAD_WALK = 48
# jump 动作抬手/发型更高，trim 时保留更多顶部透明边
JUMP_TRIM_TOP_PAD_FRAC = 0.24
# 源 jump 帧常为 864×480，发顶贴 y=0 被导出裁平 → 先垫高画布再向上补发顶
JUMP_HAIR_EXTEND_ROWS = 72
JUMP_HAIR_CLIP_MAX_Y = 10
# 归一化后：发顶半透明羽化行数，减轻 750→~100px 缩放时的硬切线
HAIR_TOP_FEATHER_ROWS = 14
# 写入 manifest，运行时读缓存不再逐帧扫像素（walk/jump/die）
PLATFORMER_METRIC_CLIPS = frozenset({"walk", "jump", "die"})

CLIP_FOLDERS = {
    "idle": ("idle", "idle"),
    "walk": ("walk", "walk"),
    "run": ("run", "run"),
    "jump": ("jump", "jump"),
    "attack": ("attack", "attack"),
    "die": ("die", "die"),
}


@dataclass
class TrimmedFrame:
    clip: str
    path: Path
    image: Image.Image
    feet_x: float
    feet_y: float


def alpha_bbox(img: Image.Image, clip: str = "") -> Optional[Tuple[int, int, int, int]]:
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
    pad_top = max(6, int((max_y - min_y + 1) * 0.035))
    if clip == "jump":
        pad_top = max(pad_top, int((max_y - min_y + 1) * JUMP_TRIM_TOP_PAD_FRAC), 28)
    pad_bottom = min(2, max(1, int((max_y - min_y + 1) * 0.008)))
    left = max(0, min_x - pad_x)
    top = max(0, min_y - pad_top)
    right = min(w, max_x + pad_x + 1)
    bottom = min(h, max_y + pad_bottom)
    # jump 发顶常贴在导出画布 y=0；任何顶部裁切都会把紫发削平
    if clip == "jump":
        top = 0
        bottom = h
    return left, top, right, bottom


def trim_image(img: Image.Image, clip: str = "") -> Image.Image:
    box = alpha_bbox(img, clip=clip)
    if box is None:
        return img.convert("RGBA")
    left, top, right, bottom = box
    if left == 0 and top == 0 and right == img.width and bottom == img.height:
        return img.convert("RGBA")
    return img.crop(box).convert("RGBA")


def crop_to_content(img: Image.Image) -> Image.Image:
    """四边裁到 alpha 内容框（去掉竖屏 jump 导出的大块透明边）。"""
    return trim_image(img, clip="")


def prepare_jump_source(img: Image.Image) -> Image.Image:
    """jump 导出画布偏矮时：垫高 + 用发顶像素向上渐变补全，减轻平切发顶。"""
    rgba = img.convert("RGBA")
    w, h = rgba.size
    min_y = _content_min_y(rgba)
    if min_y > JUMP_HAIR_CLIP_MAX_Y:
        return rgba

    px = rgba.load()
    row_min, row_max = w, -1
    for x in range(w):
        if px[x, min_y][3] > ALPHA_FLOOR:
            row_min = min(row_min, x)
            row_max = max(row_max, x)
    if row_max < row_min:
        return rgba
    row_width = row_max - row_min + 1
    if row_width < w * 0.08:
        return rgba

    ext = JUMP_HAIR_EXTEND_ROWS
    out = Image.new("RGBA", (w, h + ext), (0, 0, 0, 0))
    out.paste(rgba, (0, ext), rgba)
    opx = out.load()
    sample_depth = min(16, h - min_y - 1)
    center_x = (row_min + row_max) / 2
    half_span = row_width / 2

    for x in range(w):
        base_y = ext + min_y
        sr, sg, sb, sa = opx[x, base_y]
        if sa <= ALPHA_FLOOR:
            continue
        for y in range(base_y + 1, min(base_y + sample_depth, ext + h)):
            _, _, _, a = opx[x, y]
            if a > sa:
                sr, sg, sb, sa = opx[x, y]
        for dy in range(1, ext + 1):
            y = base_y - dy
            if y < 0:
                break
            taper = 1.0 - 0.38 * (dy / ext)
            if abs(x - center_x) > half_span * taper:
                continue
            fade = int(sa * (1.0 - (dy / (ext + 6)) ** 0.82))
            fade = max(fade, 18 if dy <= ext // 2 else 0)
            if fade <= opx[x, y][3]:
                continue
            opx[x, y] = (sr, sg, sb, fade)
    return out


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


def load_manifest_clips(skin_dir: Path) -> Dict[str, int]:
    manifest_path = skin_dir / "anim_manifest.json"
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


def collect_frames(skin_dir: Path, clip_counts: Dict[str, int]) -> List[TrimmedFrame]:
    frames: List[TrimmedFrame] = []
    for clip, count in clip_counts.items():
        if count <= 0:
            continue
        folder_name, prefix = CLIP_FOLDERS.get(clip, (clip, clip))
        folder = skin_dir / folder_name
        pngs = sorted_pngs(folder, prefix)[:count]
        for path in pngs:
            raw = Image.open(path)
            if clip == "jump":
                raw = crop_to_content(raw)
                raw = prepare_jump_source(raw)
            trimmed = trim_image(raw, clip=clip)
            feet_x, feet_y = detect_feet(trimmed)
            frames.append(TrimmedFrame(clip=clip, path=path, image=trimmed, feet_x=feet_x, feet_y=feet_y))
    return frames


def head_canvas_pad(clip_counts: Dict[str, int]) -> int:
    if int(clip_counts.get("jump", 0) or 0) > 0:
        return HEAD_CANVAS_PAD_JUMP
    return HEAD_CANVAS_PAD_WALK


def compute_canvas(frames: List[TrimmedFrame], head_pad: int = HEAD_CANVAS_PAD_WALK) -> Tuple[int, int, int, int]:
    """保证每帧 paste 后内容完整落在画布内（修复 v12 底部裁腿 bug）。"""
    max_left = 0.0
    max_right = 0.0
    max_above = 0.0
    max_below = 0.0
    for f in frames:
        w, h = f.image.size
        max_left = max(max_left, f.feet_x)
        max_right = max(max_right, w - f.feet_x)
        max_above = max(max_above, f.feet_y)
        max_below = max(max_below, h - f.feet_y)
    canvas_w = int(max_left + max_right + 2 * CANVAS_SIDE_PAD)
    canvas_h = int(max_above + max_below + 2 * CANVAS_SIDE_PAD + head_pad)
    anchor_x = int(max_left + CANVAS_SIDE_PAD)
    anchor_y = int(max_above + CANVAS_SIDE_PAD + head_pad)
    return canvas_w, canvas_h, anchor_x, anchor_y


def verify_paste_bounds(
    frame: TrimmedFrame,
    canvas_w: int,
    canvas_h: int,
    anchor_x: int,
    anchor_y: int,
) -> None:
    paste_x = int(anchor_x - frame.feet_x)
    paste_y = int(anchor_y - frame.feet_y)
    fw, fh = frame.image.size
    if paste_x < 0 or paste_y < 0 or paste_x + fw > canvas_w or paste_y + fh > canvas_h:
        raise RuntimeError(
            f"{frame.path.name}: paste out of bounds "
            f"({paste_x},{paste_y})+{fw}x{fh} in {canvas_w}x{canvas_h}"
        )


def paste_normalized(frame: TrimmedFrame, canvas_w: int, canvas_h: int, anchor_x: int, anchor_y: int) -> Image.Image:
    out = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    paste_x = int(anchor_x - frame.feet_x)
    paste_y = int(anchor_y - frame.feet_y)
    out.paste(frame.image, (paste_x, paste_y), frame.image)
    return out


def measure_platformer_metrics(img: Image.Image) -> Dict[str, float]:
    """归一化画布上 bbox 脚点 + 头顶，供横版 O(1) 布局（避免读盘时逐帧扫像素）。"""
    rgba = img.convert("RGBA")
    w, h = rgba.size
    if w <= 1 or h <= 1:
        return {"fy": 0.92, "fx": 0.5, "ty": 0.0}
    pixels = rgba.load()
    min_x, min_y, max_x, max_y = w, h, -1, -1
    step = 2 if w * h > 480_000 else 1
    for y in range(0, h, step):
        for x in range(0, w, step):
            if pixels[x, y][3] > ALPHA_FLOOR:
                if x < min_x:
                    min_x = x
                if x > max_x:
                    max_x = x
                if y < min_y:
                    min_y = y
                if y > max_y:
                    max_y = y
    if max_y < 0:
        return {"fy": 0.92, "fx": 0.5, "ty": 0.0, "ow": 0.55, "oh": 0.55}
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
    # fy 必须用 bbox 最底行（鞋底），band 最宽行常偏上 ~10% 导致局内悬空
    sole_y = max_y + 0.5
    ow = (max_x - min_x + 1) / w
    oh = (max_y - min_y + 1) / h
    return {
        "fy": round(sole_y / h, 6),
        "fx": round((best_cx + 0.5) / w, 6),
        "ty": round(min_y / h, 6),
        "ow": round(ow, 6),
        "oh": round(oh, 6),
    }


def _content_min_y(img: Image.Image) -> int:
    rgba = img.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    for y in range(h):
        for x in range(0, w, 1):
            if px[x, y][3] > ALPHA_FLOOR:
                return y
    return h


def feather_top_fringe(img: Image.Image, fringe_rows: int = HAIR_TOP_FEATHER_ROWS) -> Image.Image:
    """在首个不透明行上方补渐变 alpha，避免缩小时发顶 9→255 硬边被糊成平切。"""
    rgba = img.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    min_y = _content_min_y(rgba)
    if min_y >= h:
        return rgba

    out = rgba.copy()
    opx = out.load()
    for x in range(w):
        solid_y = min_y
        for y in range(min_y, min(min_y + 5, h)):
            if opx[x, y][3] > 200:
                solid_y = y
                break
        sr, sg, sb, sa = opx[x, solid_y]
        if sa <= ALPHA_FLOOR:
            continue
        for dy in range(1, fringe_rows + 1):
            y = solid_y - dy
            if y < 0:
                break
            fade = int(sa * (1.0 - dy / (fringe_rows + 1)))
            if fade <= opx[x, y][3]:
                continue
            opx[x, y] = (sr, sg, sb, fade)
    return out


def touch_up_normalized_skin_dir(skin_dir: Path) -> bool:
    """已归一化皮肤：羽化发顶 + 重算 platformerMetrics（不 re-trim）。"""
    manifest_path = skin_dir / "anim_manifest.json"
    if not manifest_path.is_file():
        return False
    data = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    if not data.get("normalized"):
        print(f"  skip touch-up (not normalized): {skin_dir.name}")
        return False

    clip_counts = load_manifest_clips(skin_dir)
    platformer_metrics: Dict[str, List[dict]] = {}

    for clip, count in clip_counts.items():
        if count <= 0:
            continue
        folder_name, prefix = CLIP_FOLDERS.get(clip, (clip, clip))
        folder = skin_dir / folder_name
        pngs = sorted_pngs(folder, prefix)[:count]
        clip_metrics: List[dict] = []
        for path in pngs:
            img = Image.open(path).convert("RGBA")
            if clip in PLATFORMER_METRIC_CLIPS:
                img = feather_top_fringe(img)
                clip_metrics.append(measure_platformer_metrics(img))
            img.save(path, format="PNG")
        if clip_metrics:
            platformer_metrics[clip] = clip_metrics

    if platformer_metrics:
        data["platformerMetrics"] = platformer_metrics
    data["schemaVersion"] = 2
    manifest_path.write_text(
        json.dumps(data, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(f"  touch-up {skin_dir.name}: feathered platformer clips, metrics refreshed")
    return True


def read_render_overrides(skin_dir: Path) -> dict:
    manifest_path = skin_dir / "anim_manifest.json"
    if not manifest_path.is_file():
        return {}
    data = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    return data.get("render") or {}


def normalize_skin_dir(skin_dir: Path) -> bool:
    skin_id = skin_dir.name
    manifest_path = skin_dir / "anim_manifest.json"
    manifest_raw = json.loads(manifest_path.read_text(encoding="utf-8-sig")) if manifest_path.is_file() else {}
    clip_counts = load_manifest_clips(skin_dir)
    if not clip_counts:
        print(f"  skip (no clips): {skin_id}")
        return False

    if manifest_raw.get("normalized"):
        canvas = manifest_raw.get("canvas") or {}
        cw, ch = int(canvas.get("w", 0)), int(canvas.get("h", 0))
        if cw > 0 and ch > 0:
            for clip, count in clip_counts.items():
                folder_name, prefix = CLIP_FOLDERS.get(clip, (clip, clip))
                for path in sorted_pngs(skin_dir / folder_name, prefix)[:count]:
                    with Image.open(path) as probe:
                        if probe.size == (cw, ch):
                            print(
                                f"  skip (already normalized — re-extract raw zips first): {skin_id}",
                                file=sys.stderr,
                            )
                            return False

    frames = collect_frames(skin_dir, clip_counts)
    if not frames:
        print(f"  skip (no png): {skin_id}")
        return False

    canvas_w, canvas_h, anchor_x, anchor_y = compute_canvas(frames, head_canvas_pad(clip_counts))
    for frame in frames:
        verify_paste_bounds(frame, canvas_w, canvas_h, anchor_x, anchor_y)
    anchor_frac_x = anchor_x / canvas_w
    anchor_frac_y = anchor_y / canvas_h

    platformer_metrics: Dict[str, List[dict]] = {}

    for clip, count in clip_counts.items():
        folder_name, prefix = CLIP_FOLDERS.get(clip, (clip, clip))
        folder = skin_dir / folder_name
        pngs = sorted_pngs(folder, prefix)[:count]
        clip_metrics: List[dict] = []
        for path in pngs:
            frame = next(f for f in frames if f.path == path)
            normalized = paste_normalized(frame, canvas_w, canvas_h, anchor_x, anchor_y)
            if clip in PLATFORMER_METRIC_CLIPS:
                clip_metrics.append(measure_platformer_metrics(normalized))
            normalized.save(path, format="PNG")
        if clip_metrics:
            platformer_metrics[clip] = clip_metrics

    clips_manifest = {}
    for clip, count in clip_counts.items():
        folder_name, prefix = CLIP_FOLDERS.get(clip, (clip, clip))
        clips_manifest[clip] = {
            "count": count,
            "folder": folder_name,
            "prefix": prefix,
        }

    manifest = {
        "schemaVersion": 2,
        "skinId": skin_id,
        "normalized": True,
        "canvas": {"w": canvas_w, "h": canvas_h},
        "anchorFrac": {"x": round(anchor_frac_x, 6), "y": round(anchor_frac_y, 6)},
        "clips": clips_manifest,
        "render": read_render_overrides(skin_dir),
    }
    if platformer_metrics:
        manifest["platformerMetrics"] = platformer_metrics
    (skin_dir / "anim_manifest.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(f"  normalized {skin_id}: canvas={canvas_w}x{canvas_h} anchor=({anchor_frac_x:.3f},{anchor_frac_y:.3f}) frames={len(frames)}")
    return True


def main(argv: List[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        print("  python backend/tools/normalize_pac_maze_skin.py --touch-up dist/pac_maze_skins/food_chick_walker_pro_max")
        return 1
    if argv[1] == "--touch-up":
        if len(argv) > 2 and argv[2] == "--all":
            root = Path(argv[3]) if len(argv) > 3 else Path("dist/pac_maze_skins")
            if not root.is_dir():
                print(f"not found: {root}", file=sys.stderr)
                return 1
            ok = sum(
                1 for skin_dir in sorted(root.iterdir())
                if skin_dir.is_dir() and (skin_dir / "anim_manifest.json").is_file()
                and touch_up_normalized_skin_dir(skin_dir)
            )
            print(f"touch-up done: {ok} skins")
            return 0
        target = Path(argv[2]) if len(argv) > 2 else Path("dist/pac_maze_skins/food_chick_walker_pro_max")
        if not target.is_dir():
            print(f"not found: {target}", file=sys.stderr)
            return 1
        return 0 if touch_up_normalized_skin_dir(target) else 1
    target = Path(argv[1])
    if argv[1] == "--all":
        root = Path(argv[2]) if len(argv) > 2 else Path("dist/pac_maze_skins")
        if not root.is_dir():
            print(f"not found: {root}", file=sys.stderr)
            return 1
        ok = 0
        for skin_dir in sorted(root.iterdir()):
            if skin_dir.is_dir() and (skin_dir / "anim_manifest.json").is_file():
                if normalize_skin_dir(skin_dir):
                    ok += 1
        print(f"done: {ok} skins normalized")
        return 0

    if not target.is_dir():
        print(f"not found: {target}", file=sys.stderr)
        return 1
    return 0 if normalize_skin_dir(target) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
