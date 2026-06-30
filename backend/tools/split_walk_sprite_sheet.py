#!/usr/bin/env python3
"""
从黑色底 walk sprite sheet 切分出逐帧 PNG（非均匀网格）。

用法:
  python backend/tools/split_walk_sprite_sheet.py sheet.png out_dir
  python backend/tools/split_walk_sprite_sheet.py sheet.png out_dir --prefix walk
"""
from __future__ import annotations

import sys
from collections import deque
from pathlib import Path
from typing import List, Tuple

try:
    from PIL import Image
except ImportError:
    print("需要 Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

ALPHA_FLOOR = 14
BLACK_FLOOR = 24
MIN_BLOB_W = 48
MIN_BLOB_H = 48
MIN_BLOB_AREA = 4000
ROW_CLUSTER_FRAC = 0.38


def content_mask(img: Image.Image):
    rgba = img.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    mask = [[False] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            mask[y][x] = a > ALPHA_FLOOR or max(r, g, b) > BLACK_FLOOR
    return mask, w, h


def find_blobs(mask, w: int, h: int) -> List[Tuple[int, int, int, int]]:
    seen = [[False] * w for _ in range(h)]
    blobs: List[Tuple[int, int, int, int]] = []

    for y0 in range(h):
        for x0 in range(w):
            if not mask[y0][x0] or seen[y0][x0]:
                continue
            q = deque([(y0, x0)])
            seen[y0][x0] = True
            min_x, min_y, max_x, max_y = x0, y0, x0, y0
            area = 0
            while q:
                y, x = q.popleft()
                area += 1
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
                for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
                    if 0 <= ny < h and 0 <= nx < w and mask[ny][nx] and not seen[ny][nx]:
                        seen[ny][nx] = True
                        q.append((ny, nx))
            bw, bh = max_x - min_x + 1, max_y - min_y + 1
            if bw >= MIN_BLOB_W and bh >= MIN_BLOB_H and area >= MIN_BLOB_AREA:
                pad = max(2, int(min(bw, bh) * 0.04))
                blobs.append((
                    max(0, min_x - pad),
                    max(0, min_y - pad),
                    min(w, max_x + pad + 1),
                    min(h, max_y + pad + 1),
                ))
    return blobs


def merge_row_blobs(boxes: List[Tuple[int, int, int, int]]) -> List[Tuple[int, int, int, int]]:
    """同一行内合并相邻碎片（如蜻蜓翅膀）。"""
    if len(boxes) <= 1:
        return boxes
    boxes = sorted(boxes, key=lambda b: (b[0] + b[2]) / 2)
    widths = [b[2] - b[0] for b in boxes]
    avg_w = sum(widths) / len(widths)
    gap_tol = max(48, int(avg_w * 0.55))
    merged: List[Tuple[int, int, int, int]] = []
    cur = boxes[0]
    for nxt in boxes[1:]:
        gap = nxt[0] - cur[2]
        if gap <= gap_tol:
            cur = (
                min(cur[0], nxt[0]),
                min(cur[1], nxt[1]),
                max(cur[2], nxt[2]),
                max(cur[3], nxt[3]),
            )
        else:
            merged.append(cur)
            cur = nxt
    merged.append(cur)
    return merged


def sort_blobs(blobs: List[Tuple[int, int, int, int]]) -> List[Tuple[int, int, int, int]]:
    if not blobs:
        return []
    centers = [((x0 + x1) / 2, (y0 + y1) / 2, (x0, y0, x1, y1)) for x0, y0, x1, y1 in blobs]
    heights = [y1 - y0 for _, _, (_, y0, _, y1) in centers]
    avg_h = sum(heights) / len(heights) if heights else 128
    row_tol = max(64, int(avg_h * ROW_CLUSTER_FRAC))

    rows: List[List[Tuple[float, Tuple[int, int, int, int]]]] = []
    for cx, cy, box in sorted(centers, key=lambda t: t[1]):
        placed = False
        for row in rows:
            row_cy = sum(item[0] for item in row) / len(row)
            if abs(cy - row_cy) <= row_tol:
                row.append((cy, box))
                placed = True
                break
        if not placed:
            rows.append([(cy, box)])

    rows.sort(key=lambda row: sum(item[0] for item in row) / len(row))
    ordered: List[Tuple[int, int, int, int]] = []
    for row in rows:
        row.sort(key=lambda item: (item[1][0] + item[1][2]) / 2)
        ordered.extend(merge_row_blobs([item[1] for item in row]))
    return ordered


def cell_has_content(img: Image.Image, box: Tuple[int, int, int, int], min_frac: float = 0.012) -> bool:
    x0, y0, x1, y1 = box
    cell = img.crop(box)
    w, h = cell.size
    if w <= 0 or h <= 0:
        return False
    px = cell.load()
    hits = 0
    step = max(1, min(w, h) // 64)
    for y in range(0, h, step):
        for x in range(0, w, step):
            r, g, b, a = px[x, y]
            if a > ALPHA_FLOOR or max(r, g, b) > BLACK_FLOOR:
                hits += 1
    return hits >= max(8, int((w * h) / (step * step) * min_frac))


def split_grid_4096(img: Image.Image) -> List[Tuple[int, int, int, int]]:
    """4096 宽导出 sheet：8 列 × 512，行数按高度推断。"""
    w, h = img.size
    if w % 512 != 0 or w < 2048:
        return []
    cols = w // 512
    if h % 512 == 0:
        rows = h // 512
    elif h % 512 <= 128:
        rows = h // 512
    else:
        return []
    boxes: List[Tuple[int, int, int, int]] = []
    for row in range(rows):
        for col in range(cols):
            x0, y0 = col * 512, row * 512
            box = (x0, y0, min(w, x0 + 512), min(h, y0 + 512))
            if cell_has_content(img, box):
                boxes.append(box)
    return boxes


def split_sheet(sheet_path: Path, out_dir: Path, prefix: str = "walk") -> int:
    img = Image.open(sheet_path).convert("RGBA")
    boxes = split_grid_4096(img)
    if not boxes:
        mask, w, h = content_mask(img)
        boxes = sort_blobs(find_blobs(mask, w, h))
    if not boxes:
        raise RuntimeError(f"no sprites detected in {sheet_path}")

    out_dir.mkdir(parents=True, exist_ok=True)
    for old in out_dir.glob(f"{prefix}_*.png"):
        old.unlink()

    for i, box in enumerate(boxes, start=1):
        frame = img.crop(box)
        frame.save(out_dir / f"{prefix}_{i}.png", format="PNG")
    return len(boxes)


def main(argv: List[str]) -> int:
    if len(argv) < 3:
        print(__doc__)
        return 1
    sheet = Path(argv[1])
    out = Path(argv[2])
    prefix = "walk"
    if len(argv) > 3 and argv[3] == "--prefix" and len(argv) > 4:
        prefix = argv[4]
    if not sheet.is_file():
        print(f"not found: {sheet}", file=sys.stderr)
        return 1
    count = split_sheet(sheet, out, prefix=prefix)
    print(f"split {sheet.name} -> {count} frames in {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
