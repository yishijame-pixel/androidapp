"""Parse SuperTux tiles.strf → tile id → image crop (32px grid)."""
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from supertux_sexpr import find_blocks, parse, prop, prop_atoms, prop_int, sym

TILE_PX = 32


@dataclass(frozen=True)
class TileImageRef:
    path: str  # relative to data/images/
    x: int
    y: int
    w: int = TILE_PX
    h: int = TILE_PX


def _image_path(node) -> Optional[str]:
    if isinstance(node, str):
        return node.strip().strip('"')
    if isinstance(node, list) and node:
        if sym(node[0]) == "surface":
            for child in node[1:]:
                if isinstance(child, list) and sym(child[0]) == "diffuse-texture":
                    f = prop(child, "file")
                    if isinstance(f, str):
                        return f.strip().strip('"')
        for child in node[1:]:
            if isinstance(child, str) and ("/" in child or child.endswith(".png")):
                return child.strip().strip('"')
    return None


def _ids_matrix(block: List, width: int, height: int) -> List[List[int]]:
    raw = prop_atoms(block, "ids")
    if not raw:
        return [[0] * width for _ in range(height)]
    out: List[List[int]] = []
    idx = 0
    for _ in range(height):
        row: List[int] = []
        for _ in range(width):
            if idx < len(raw):
                row.append(int(raw[idx]))
                idx += 1
            else:
                row.append(0)
        out.append(row)
    return out


def parse_tiles_strf(strf_path: Path) -> Dict[int, TileImageRef]:
    tree = parse(strf_path.read_text(encoding="utf-8", errors="replace"))
    registry: Dict[int, TileImageRef] = {}

    for block in find_blocks(tree, "tile"):
        tid = prop_int(block, "id")
        if tid <= 0:
            continue
        img_node = prop(block, "images")
        path = _image_path(img_node)
        if path:
            registry[tid] = TileImageRef(path=path, x=0, y=0)

    for block in find_blocks(tree, "tiles"):
        tw = prop_int(block, "width")
        th = prop_int(block, "height")
        if tw <= 0 or th <= 0:
            continue
        img_node = prop(block, "images")
        path = _image_path(img_node)
        if not path:
            continue
        matrix = _ids_matrix(block, tw, th)
        for row in range(th):
            for col in range(tw):
                tid = matrix[row][col]
                if tid <= 0:
                    continue
                registry[tid] = TileImageRef(
                    path=path,
                    x=col * TILE_PX,
                    y=row * TILE_PX,
                )
    return registry


def export_tile_pngs(
    registry: Dict[int, TileImageRef],
    tile_ids: List[int],
    images_root: Path,
    out_dir: Path,
    out_px: int = 32,
) -> Dict[int, str]:
    """Export used tile ids → atlas/{id}.png; return id → relative path."""
    try:
        from PIL import Image
    except ImportError as exc:
        raise SystemExit("Pillow required: pip install Pillow") from exc

    out_dir.mkdir(parents=True, exist_ok=True)
    mapping: Dict[int, str] = {}
    for tid in sorted(set(tile_ids)):
        if tid <= 0:
            continue
        ref = registry.get(tid)
        if ref is None:
            continue
        src = images_root / ref.path.replace("\\", "/")
        if not src.is_file():
            alt = images_root / Path(ref.path).name
            src = alt if alt.is_file() else src
        if not src.is_file():
            continue
        sheet = Image.open(src).convert("RGBA")
        cell = sheet.crop((ref.x, ref.y, ref.x + ref.w, ref.y + ref.h))
        if out_px != ref.w:
            cell = cell.resize((out_px, out_px), Image.Resampling.NEAREST)
        rel = f"atlas/{tid}.png"
        cell.save(out_dir / f"{tid}.png")
        mapping[tid] = rel
    return mapping
