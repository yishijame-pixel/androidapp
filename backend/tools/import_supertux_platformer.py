#!/usr/bin/env python3
"""
SuperTux → FunLife 横版冒险资源导入（企业级离线管线）。

用法:
  python backend/tools/import_supertux_platformer.py
  python backend/tools/import_supertux_platformer.py --supertux-root reference-assets/supertux
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

REPO = Path(__file__).resolve().parents[2]
DEFAULT_SUPERTUX = REPO / "reference-assets" / "supertux"
DIST_SFX = REPO / "dist" / "platformer_sfx"
DIST_SUPERTUX = REPO / "dist" / "platformer_supertux"
ASSETS_SFX = REPO / "app" / "src" / "main" / "assets" / "platformer_sfx"
BUNDLE_ZIP_DIR = REPO / "dist" / "asset-bundles"

sys.path.insert(0, str(Path(__file__).resolve().parent))
from supertux_level_registry import (  # noqa: E402
    CHAPTERS,
    chapter_catalog_entries,
    discover_level_specs,
    level_end_id,
)
from supertux_sexpr import find_blocks, parse, prop, prop_atoms, prop_bool, prop_int, sym  # noqa: E402

BUNDLE_VERSION = 4
LEVEL_ROWS = 14
SEGMENT_W = 28
SUPERTUX_TILE_PX = 32
PACK_TILE_PX = 128
# ice-floor.png 6×5 @32px，与 tiles.strf 中 snow 主 autotile 布局一致
ICE_FLOOR_CRAFTPIX: Dict[int, Tuple[int, int]] = {
    1: (0, 0),   # topLeft
    2: (1, 0),   # top
    3: (2, 0),   # topRight
    4: (0, 1),   # left
    5: (1, 1),   # fill
    7: (0, 2),   # bottom
    8: (3, 1),   # right
    14: (4, 1),  # platform / thin cap
}
UNDERGROUND_FILL_RATIO = 0.85

SFX_MAP: Dict[str, str] = {
    "player_jump": "jump.wav",
    "player_big_jump": "bigjump.wav",
    "player_land": "thud.ogg",
    "player_hurt": "hurt.wav",
    "player_die": "kill.wav",
    "pickup_gem": "coin.wav",
    "enemy_stomp": "stomp.wav",
    "spring_bounce": "trampoline.wav",
    "level_clear": "welldone.ogg",
    "checkpoint": "savebell2.wav",
    "shoot": "shoot.wav",
    "splash": "splash.ogg",
    "switch_toggle": "switch.ogg",
    "power_up": "grow.ogg",
    "bgm_platformer": "music/antarctic/snowm_theme.ogg",
    "bgm_supertux_antarctic": "music/antarctic/snowm_theme.ogg",
    "bgm_supertux_forest": "music/forest/velf.theme.ogg",
    "bgm_supertux_bonus": "music/misc/bonus.theme.ogg",
    "bgm_supertux_redmond": "music/misc/redmond.theme.ogg",
}


def chapter_meta(chapter_id: str) -> Dict[str, Any]:
    for ch in CHAPTERS:
        if ch["id"] == chapter_id:
            return ch
    return CHAPTERS[0]


def resolve_scroll(stl_path: Path, spec: Dict[str, Any]) -> bool:
    if spec.get("scroll") is not None:
        return bool(spec["scroll"])
    probe = convert_level(stl_path, {**spec, "scroll": False})
    return probe["width"] > SEGMENT_W

def parse_stl(path: Path) -> Any:
    return parse(path.read_text(encoding="utf-8", errors="replace"))


def sector_main(tree: Any) -> Optional[List[Any]]:
    for sec in find_blocks(tree, "sector"):
        name = prop(sec, "name")
        if name is None or sym(name) in ("main", "Main"):
            return sec
    sectors = find_blocks(tree, "sector")
    return sectors[0] if sectors else None


def decompress_tiles(raw: List[int], width: int, height: int) -> List[int]:
    """SuperTux RLE: negative N repeats next value N times; positive appends once."""
    out: List[int] = []
    repeater = 0
    for val in raw:
        if repeater:
            if val < 0:
                raise ValueError(f"expected positive tile after repeater, got {val}")
            out.extend([val] * repeater)
            repeater = 0
        elif val < 0:
            repeater = -val
        else:
            out.append(val)
    if repeater:
        raise ValueError("truncated tile RLE stream")
    need = width * height
    if len(out) < need:
        out.extend([0] * (need - len(out)))
    return out[:need]


def merge_visual_tilemaps(sector: List[Any]) -> Optional[Tuple[int, int, List[int]]]:
    """合并非 solid 装饰层（Background + foreground），保留真实 SuperTux tile id。"""
    merged: Optional[List[int]] = None
    w = h = 0
    for tm in find_blocks(sector, "tilemap"):
        if prop_bool(tm, "solid", False):
            continue
        tw = prop_int(tm, "width")
        th = prop_int(tm, "height")
        raw = prop_atoms(tm, "tiles")
        if tw <= 0 or th <= 0 or not raw:
            continue
        try:
            tiles = decompress_tiles(raw, tw, th)
        except ValueError:
            continue
        if merged is None:
            w, h = tw, th
            merged = [0] * (w * h)
        if tw != w or th != h:
            continue
        for i, tid in enumerate(tiles):
            if tid > 0:
                merged[i] = tid
    if merged is None:
        return None
    return w, h, merged


def merge_solid_tilemaps(sector: List[Any]) -> Optional[Tuple[int, int, List[int]]]:
    merged: Optional[List[int]] = None
    w = h = 0
    for tm in find_blocks(sector, "tilemap"):
        if not prop_bool(tm, "solid", False):
            continue
        tw = prop_int(tm, "width")
        th = prop_int(tm, "height")
        raw = prop_atoms(tm, "tiles")
        if tw <= 0 or th <= 0 or not raw:
            continue
        try:
            tiles = decompress_tiles(raw, tw, th)
        except ValueError:
            continue
        if merged is None:
            w, h = tw, th
            merged = [0] * (w * h)
        if tw != w or th != h:
            continue
        for i, tid in enumerate(tiles):
            if tid > 0:
                merged[i] = tid
    if merged is None:
        return None
    return w, h, merged


def find_spawn(sector: List[Any]) -> Tuple[int, int]:
    for sp in find_blocks(sector, "spawnpoint"):
        name = prop(sp, "name")
        if name is not None and sym(name) not in ("main", "Main", ""):
            continue
        return prop_int(sp, "x", 1), prop_int(sp, "y", 1)
    return 1, 1


def solid_rows(w: int, h: int, tiles: List[int]) -> List[List[bool]]:
    grid = [[False] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            grid[y][x] = tiles[y * w + x] > 0
    return grid


def underground_row(grid: List[List[bool]], w: int, h: int) -> int:
    """首个「地下填充层」行：整行 85%+ 为 solid，再往下不应进入 14 行视口。"""
    for y in range(h):
        if sum(1 for x in range(w) if grid[y][x]) >= w * UNDERGROUND_FILL_RATIO:
            return y
    return h


def viewport_y_range(
    grid: List[List[bool]],
    w: int,
    h: int,
    spawn_ty: int,
    view_h: int,
) -> Tuple[int, int]:
    """以出生点 + 地下边界选取可玩 14 行，避免切到 SuperTux 底部实心填充。"""
    ug = underground_row(grid, w, h)
    y1 = min(h, ug if ug < h else h)
    y0 = max(0, min(spawn_ty - (view_h - 3), y1 - view_h))
    y0 = max(0, min(y0, h - view_h))
    y1 = min(h, y0 + view_h)
    return y0, y1


def trim_bounds(grid: List[List[bool]]) -> Tuple[int, int, int, int]:
    h = len(grid)
    w = len(grid[0]) if h else 0
    min_x, min_y, max_x, max_y = w, h, -1, -1
    for y in range(h):
        for x in range(w):
            if grid[y][x]:
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
    if max_x < 0:
        return 0, 0, w - 1, h - 1
    return min_x, min_y, max_x, max_y


def viewport_window(
    grid: List[List[bool]],
    spawn: Tuple[int, int],
    scroll: bool,
) -> Tuple[int, int, int, int, int]:
    """返回 x0, y0, y1, width（与 to_platformer_rows 一致）。"""
    h = len(grid)
    w = len(grid[0]) if h else 0
    min_x, _, max_x, _ = trim_bounds(grid)
    view_h = LEVEL_ROWS
    sx_px, sy_px = spawn
    spawn_ty = max(0, sy_px // SUPERTUX_TILE_PX)
    y0, y1 = viewport_y_range(grid, w, h, spawn_ty, view_h)
    x0 = min_x
    x1 = max_x + 1
    if scroll:
        x1 = max(x1, x0 + SEGMENT_W * 4)
    width = max(SEGMENT_W, x1 - x0)
    return x0, y0, y1, width, max(0, sx_px // SUPERTUX_TILE_PX)


def to_platformer_rows(
    grid: List[List[bool]],
    spawn: Tuple[int, int],
    scroll: bool,
) -> Tuple[List[str], bool, int]:
    h = len(grid)
    w = len(grid[0]) if h else 0
    x0, y0, y1, width, spawn_tx = viewport_window(grid, spawn, scroll)
    spawn_ty = max(0, spawn[1] // SUPERTUX_TILE_PX)
    rows: List[str] = []
    placed_spawn = False
    placed_goal = False
    for y in range(y0, y1):
        chars: List[str] = []
        for x in range(x0, x0 + width):
            if x >= w or y >= h:
                chars.append(".")
                continue
            if not grid[y][x]:
                if x == spawn_tx and y == spawn_ty and not placed_spawn:
                    chars.append("@")
                    placed_spawn = True
                elif x == x0 + width - 2 and y == y1 - 1 and not placed_goal:
                    chars.append("O")
                    placed_goal = True
                else:
                    chars.append(".")
            else:
                chars.append("#")
        rows.append("".join(chars))
    while len(rows) < LEVEL_ROWS:
        rows.insert(0, "." * width)
    if not placed_spawn:
        rows[-1] = rows[-1][:2] + "@" + rows[-1][3:] if len(rows[-1]) > 3 else "@" + "." * (width - 1)
        placed_spawn = True
    if not placed_goal:
        row = list(rows[-1])
        if len(row) > 3:
            row[-2] = "O"
        rows[-1] = "".join(row)
    return rows, placed_spawn, width


def to_visual_rows(
    tile_ids: List[int],
    map_w: int,
    map_h: int,
    x0: int,
    y0: int,
    y1: int,
    width: int,
) -> List[List[int]]:
    view_h = LEVEL_ROWS
    rows: List[List[int]] = []
    for y in range(y0, y1):
        row: List[int] = []
        for x in range(x0, x0 + width):
            if x >= map_w or y >= map_h:
                row.append(0)
            else:
                row.append(tile_ids[y * map_w + x])
        rows.append(row)
    while len(rows) < view_h:
        rows.insert(0, [0] * width)
    return rows


def split_visual_segments(rows: List[List[int]], segment_w: int = SEGMENT_W) -> List[List[List[int]]]:
    if not rows:
        return []
    width = len(rows[0])
    out: List[List[List[int]]] = []
    for x0 in range(0, width, segment_w):
        seg: List[List[int]] = []
        for row in rows:
            chunk = row[x0 : x0 + segment_w]
            seg.append(chunk + [0] * (segment_w - len(chunk)))
        out.append(seg)
    return out


def extract_objects(sector: List[Any]) -> Dict[str, List[Dict[str, Any]]]:
    coins: List[Dict[str, Any]] = []
    for c in find_blocks(sector, "coin"):
        coins.append({"x": prop_int(c, "x"), "y": prop_int(c, "y")})
    badguys: List[Dict[str, Any]] = []
    for b in find_blocks(sector, "badguy"):
        name = prop(b, "name")
        badguys.append({
            "name": sym(name) if name is not None else "unknown",
            "x": prop_int(b, "x"),
            "y": prop_int(b, "y"),
        })
    return {"coins": coins, "badguys": badguys}


def split_segments(rows: List[str], segment_w: int = SEGMENT_W) -> List[List[str]]:
    if not rows:
        return []
    width = len(rows[0])
    out: List[List[str]] = []
    for x0 in range(0, width, segment_w):
        seg: List[str] = []
        for row in rows:
            chunk = row[x0 : x0 + segment_w].ljust(segment_w, ".")
            seg.append(chunk)
        out.append(seg)
    return out


def convert_level(stl_path: Path, spec: Dict[str, Any]) -> Dict[str, Any]:
    tree = parse_stl(stl_path)
    sector = sector_main(tree)
    if sector is None:
        raise ValueError(f"no sector in {stl_path}")
    merged = merge_solid_tilemaps(sector)
    if merged is None:
        raise ValueError(f"no solid tilemap in {stl_path}")
    w, h, tiles = merged
    grid = solid_rows(w, h, tiles)
    spawn = find_spawn(sector)
    rows, _, width = to_platformer_rows(grid, spawn, scroll=spec["scroll"])
    x0, y0, y1, width, _ = viewport_window(grid, spawn, scroll=spec["scroll"])
    segments = split_segments(rows) if spec["scroll"] else []
    visual_merged = merge_visual_tilemaps(sector)
    visual_segments: List[List[List[int]]] = []
    visual_rows: List[List[int]] = []
    if visual_merged:
        vw, vh, vtiles = visual_merged
        visual_rows = to_visual_rows(vtiles, vw, vh, x0, y0, y1, width)
        if spec["scroll"]:
            visual_segments = split_visual_segments(visual_rows)
    objects = extract_objects(sector)
    coin_tiles = [
        {"tx": c["x"] // SUPERTUX_TILE_PX - x0, "ty": c["y"] // SUPERTUX_TILE_PX - y0}
        for c in objects["coins"]
        if x0 <= c["x"] // SUPERTUX_TILE_PX < x0 + width and y0 <= c["y"] // SUPERTUX_TILE_PX < y1
    ]
    badguy_tiles = [
        {
            "name": b["name"],
            "tx": b["x"] // SUPERTUX_TILE_PX - x0,
            "ty": b["y"] // SUPERTUX_TILE_PX - y0,
        }
        for b in objects["badguys"]
        if x0 <= b["x"] // SUPERTUX_TILE_PX < x0 + width and y0 <= b["y"] // SUPERTUX_TILE_PX < y1
    ]
    level_name = ""
    for nb in find_blocks(tree, "supertux-level"):
        n = prop(nb, "name")
        if isinstance(n, str):
            level_name = n
        elif isinstance(n, list) and len(n) > 1:
            level_name = sym(n[1], spec["title"])
    used_ids: List[int] = []
    for row in visual_rows:
        used_ids.extend(t for t in row if t > 0)
    ch = chapter_meta(spec.get("chapterId", "supertux_antarctic"))
    return {
        "id": spec["id"],
        "title": spec["title"],
        "subtitle": f"SuperTux · {level_name or spec['title']}",
        "sourceStl": spec["stl"],
        "chapterId": ch["id"],
        "seriesId": ch["id"],
        "tilesetId": ch["tilesetId"],
        "bgmEvent": ch["bgmEvent"],
        "skyTop": ch["sky_top"],
        "skyBottom": ch["sky_bottom"],
        "useCampaignScroll": spec["scroll"],
        "rows": rows,
        "segments": segments,
        "visualRows": visual_rows,
        "visualSegments": visual_segments,
        "width": width,
        "viewport": {"x0": x0, "y0": y0},
        "spawn": {"x": spawn[0], "y": spawn[1]},
        "coinTiles": coin_tiles,
        "badguyTiles": badguy_tiles,
        "coins": objects["coins"],
        "badguys": objects["badguys"],
        "usedTileIds": sorted(set(used_ids)),
    }


def copy_sfx(supertux: Path) -> None:
    sounds = supertux / "data" / "sounds"
    music_root = supertux / "data" / "music"
    curated = DIST_SFX / "curated" / "platformer"
    curated.mkdir(parents=True, exist_ok=True)
    bgm_dir = curated / "bgm"
    bgm_dir.mkdir(parents=True, exist_ok=True)
    copied: Dict[str, str] = {}
    for event, rel in SFX_MAP.items():
        if rel.startswith("music/"):
            src = music_root / rel.replace("music/", "", 1)
            if not src.is_file():
                alt = list(music_root.rglob("*.ogg"))
                src = alt[0] if alt else None
            if src is None or not src.is_file():
                continue
            dst_name = Path(rel).name
            dst = bgm_dir / dst_name
            shutil.copy2(src, dst)
            copied[event] = f"curated/platformer/bgm/{dst_name}"
        else:
            src = sounds / rel
            if not src.is_file():
                continue
            dst = curated / rel.replace("/", "_")
            shutil.copy2(src, dst)
            copied[event] = f"curated/platformer/{dst.name}"
    manifest = {
        "version": 1,
        "game": "platformer",
        "updatedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "description": "横版冒险音效（SuperTux 精选 + 全局事件）",
        "attribution": "SuperTux Team · CC-BY-SA 3.0 / GPL v2+ — see ATTRIBUTION.json",
        "events": {
            k: {"file": v, "volume": 0.72 if k.startswith("bgm") else 0.65, "loop": k.startswith("bgm")}
            for k, v in copied.items()
        },
    }
    DIST_SFX.mkdir(parents=True, exist_ok=True)
    (DIST_SFX / "sfx_manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    (DIST_SFX / "bundle_version.txt").write_text("1\n", encoding="utf-8")
    (DIST_SFX / "LICENSE.txt").write_text(
        "SuperTux sound assets — dual licensed GPL v2+ and CC-BY-SA 3.0.\nSee data/AUTHORS in SuperTux source.\n",
        encoding="utf-8",
    )
    (DIST_SFX / "ATTRIBUTION.json").write_text(
        json.dumps({"source": "SuperTux/supertux", "license": "CC-BY-SA 3.0 / GPL v2+"}, indent=2),
        encoding="utf-8",
    )


def extract_ice_floor_autotiles(snow: Path, out_tiles: Path) -> int:
    """从 ice-floor.png 按 32px 切出 Craftpix autotile 槽位，再放大到 128px。"""
    try:
        from PIL import Image
    except ImportError as exc:
        raise SystemExit("PIL required for SuperTux tileset export: pip install Pillow") from exc

    sheet_path = snow / "ice-floor.png"
    if not sheet_path.is_file():
        raise FileNotFoundError(sheet_path)
    sheet = Image.open(sheet_path).convert("RGBA")
    out_tiles.mkdir(parents=True, exist_ok=True)
    written = 0
    for craft_idx, (col, row) in sorted(ICE_FLOOR_CRAFTPIX.items()):
        x0 = col * SUPERTUX_TILE_PX
        y0 = row * SUPERTUX_TILE_PX
        cell = sheet.crop((x0, y0, x0 + SUPERTUX_TILE_PX, y0 + SUPERTUX_TILE_PX))
        cell = cell.resize((PACK_TILE_PX, PACK_TILE_PX), Image.Resampling.NEAREST)
        dst = out_tiles / f"{craft_idx:02d}.png"
        cell.save(dst)
        written += 1
    fill_path = out_tiles / "05.png"
    if fill_path.is_file():
        fill = Image.open(fill_path)
        for i in range(1, 15):
            dst = out_tiles / f"{i:02d}.png"
            if not dst.is_file():
                fill.save(dst)
    return written


def copy_tileset(supertux: Path) -> None:
    snow = supertux / "data" / "images" / "tiles" / "snow"
    out_tiles = DIST_SUPERTUX / "tilesets" / "antarctic" / "tiles"
    if out_tiles.exists():
        shutil.rmtree(out_tiles)
    tile_count = extract_ice_floor_autotiles(snow, out_tiles)
    bg_src = supertux / "data" / "images" / "background" / "antarctic" / "snow_panorama.png"
    bg_dst = DIST_SUPERTUX / "tilesets" / "antarctic" / "background.png"
    if bg_src.is_file():
        shutil.copy2(bg_src, bg_dst)
    manifest = {
        "id": "supertux_antarctic",
        "schemaVersion": 1,
        "tileSize": PACK_TILE_PX,
        "tileCount": 14,
        "mapping": "craftpix_v1",
        "sourceSheet": "tiles/snow/ice-floor.png",
        "theme": "PACK_SUPERTUX",
        "source": "SuperTux data/images/tiles/snow",
        "license": "CC-BY-SA 3.0 / GPL v2+",
    }
    tile_root = DIST_SUPERTUX / "tilesets" / "antarctic"
    (tile_root / "tileset_manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")


def convert_levels(supertux: Path) -> Tuple[List[Dict[str, Any]], List[int]]:
    levels_dir = DIST_SUPERTUX / "levels"
    if levels_dir.exists():
        shutil.rmtree(levels_dir)
    levels_dir.mkdir(parents=True, exist_ok=True)
    specs = discover_level_specs(supertux)
    out_levels: List[Dict[str, Any]] = []
    all_tile_ids: List[int] = []
    for spec in specs:
        stl = supertux / "data" / "levels" / spec["stl"]
        if not stl.is_file():
            print(f"WARN missing {stl}")
            continue
        scroll = resolve_scroll(stl, spec)
        data = convert_level(stl, {**spec, "scroll": scroll})
        lid = spec["id"]
        level_dir = levels_dir / f"level_{lid:03d}"
        level_dir.mkdir(parents=True, exist_ok=True)
        all_tile_ids.extend(data.get("usedTileIds") or [])
        level_meta = {
            k: v
            for k, v in data.items()
            if k not in ("visualRows", "visualSegments", "usedTileIds")
        }
        level_meta["hasVisualTiles"] = bool(data.get("visualRows"))
        level_meta["coinCount"] = len(data.get("coinTiles") or [])
        level_meta["badguyCount"] = len(data.get("badguyTiles") or [])
        (level_dir / "level.json").write_text(
            json.dumps(level_meta, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
        if data["segments"]:
            (level_dir / "segments.json").write_text(
                json.dumps(data["segments"], ensure_ascii=False),
                encoding="utf-8",
            )
        if data.get("visualSegments"):
            (level_dir / "visual_segments.json").write_text(
                json.dumps(data["visualSegments"], ensure_ascii=False),
                encoding="utf-8",
            )
        elif data.get("visualRows"):
            (level_dir / "visual_rows.json").write_text(
                json.dumps(data["visualRows"], ensure_ascii=False),
                encoding="utf-8",
            )
        out_levels.append(data)
        print(
            f"OK level {lid}: {spec['title']} w={data['width']} scroll={data['useCampaignScroll']} "
            f"visual={level_meta['hasVisualTiles']} coins={level_meta['coinCount']} badguys={level_meta['badguyCount']}"
        )
    return out_levels, sorted(set(all_tile_ids))


def build_tile_atlas(supertux: Path, tile_ids: List[int]) -> None:
    from supertux_tiles_strf import export_tile_pngs, parse_tiles_strf

    strf = supertux / "data" / "images" / "tiles.strf"
    if not strf.is_file() or not tile_ids:
        print("WARN skip tile atlas (no strf or no ids)")
        return
    registry = parse_tiles_strf(strf)
    atlas_dir = DIST_SUPERTUX / "tilesets" / "antarctic" / "atlas"
    if atlas_dir.exists():
        shutil.rmtree(atlas_dir)
    images_root = supertux / "data" / "images"
    mapping = export_tile_pngs(registry, tile_ids, images_root, atlas_dir, out_px=32)
    manifest = {
        "schemaVersion": 1,
        "tilePx": 32,
        "count": len(mapping),
        "tiles": {str(k): v for k, v in mapping.items()},
    }
    (DIST_SUPERTUX / "tilesets" / "antarctic" / "atlas_manifest.json").write_text(
        json.dumps(manifest, indent=2),
        encoding="utf-8",
    )
    print(f"OK tile atlas: {len(mapping)} / {len(set(tile_ids))} ids")


def copy_tux_character(supertux: Path) -> None:
    """复制 Tux 小体型行走/站立/跳跃帧到 bundle。"""
    src_root = supertux / "data" / "images" / "creatures" / "tux" / "small"
    dst_root = DIST_SUPERTUX / "characters" / "tux"
    if not src_root.is_dir():
        print("WARN missing Tux sprites")
        return
    copies = [
        ("stand", [f"stand-{i}.png" for i in range(6)]),
        ("walk", [f"walk-{i}.png" for i in range(6)]),
        ("jump", ["jump-0.png", "jump-1.png"]),
    ]
    for folder, names in copies:
        out = dst_root / folder
        out.mkdir(parents=True, exist_ok=True)
        for name in names:
            src = src_root / name
            if src.is_file():
                shutil.copy2(src, out / name)
    (dst_root / "character_manifest.json").write_text(
        json.dumps(
            {
                "id": "supertux_tux",
                "title": "Tux",
                "subtitle": "SuperTux 企鹅",
                "license": "GPL v2+ / CC-BY-SA",
            },
            indent=2,
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    print("OK Tux character sprites")


def write_content_catalog(levels: List[Dict[str, Any]], supertux: Path) -> None:
    commit = ""
    git_dir = supertux / ".git"
    if git_dir.is_dir():
        commit = subprocess.check_output(
            ["git", "-C", str(supertux), "rev-parse", "HEAD"], text=True
        ).strip()
    specs = discover_level_specs(supertux)
    catalog = {
        "schemaVersion": 2,
        "bundleVersion": BUNDLE_VERSION,
        "sourcePin": commit,
        "levelRange": [901, level_end_id(specs)],
        "chapters": chapter_catalog_entries(specs),
        "levels": [
            {
                "id": lv["id"],
                "title": lv["title"],
                "sourceStl": lv["sourceStl"],
                "chapterId": lv.get("chapterId"),
            }
            for lv in levels
        ],
    }
    (DIST_SUPERTUX / "content_catalog.json").write_text(
        json.dumps(catalog, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )
    (DIST_SUPERTUX / "bundle_version.txt").write_text(f"{BUNDLE_VERSION}\n", encoding="utf-8")
    (DIST_SUPERTUX / "LICENSE.txt").write_text(
        "SuperTux game data — GPL v2+ and CC-BY-SA 3.0.\n",
        encoding="utf-8",
    )
    (DIST_SUPERTUX / "ATTRIBUTION.json").write_text(
        json.dumps({"source": "https://github.com/SuperTux/supertux", "commit": commit}, indent=2),
        encoding="utf-8",
    )


def mirror_to_assets() -> None:
    """Debug APK 回退：复制到 assets（ResourceStore 第二解析源）。"""
    if ASSETS_SFX.exists():
        shutil.rmtree(ASSETS_SFX)
    if DIST_SFX.exists():
        shutil.copytree(DIST_SFX, ASSETS_SFX)
    assets_st = REPO / "app" / "src" / "main" / "assets" / "platformer_supertux"
    if assets_st.exists():
        shutil.rmtree(assets_st)
    if DIST_SUPERTUX.exists():
        shutil.copytree(DIST_SUPERTUX, assets_st)


def pack_zips() -> None:
    BUNDLE_ZIP_DIR.mkdir(parents=True, exist_ok=True)
    for name, src in [("platformer_sfx", DIST_SFX), ("platformer_supertux", DIST_SUPERTUX)]:
        if not src.is_dir():
            continue
        zip_path = BUNDLE_ZIP_DIR / f"{name}.zip"
        if zip_path.exists():
            zip_path.unlink()
        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
            for f in src.rglob("*"):
                if f.is_file():
                    zf.write(f, f"{name}/{f.relative_to(src).as_posix()}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--supertux-root", type=Path, default=DEFAULT_SUPERTUX)
    ap.add_argument("--skip-assets-mirror", action="store_true")
    args = ap.parse_args()
    root = args.supertux_root
    if not root.is_dir():
        print(f"Missing SuperTux clone: {root}")
        sys.exit(1)
    print("Import SFX…")
    copy_sfx(root)
    print("Import tileset…")
    copy_tileset(root)
    print("Convert levels…")
    levels, tile_ids = convert_levels(root)
    build_tile_atlas(root, tile_ids)
    copy_tux_character(root)
    write_content_catalog(levels, root)
    if not args.skip_assets_mirror:
        mirror_to_assets()
    pack_zips()
    print(f"Done. levels={len(levels)} dist={DIST_SUPERTUX} sfx={DIST_SFX}")


if __name__ == "__main__":
    main()
