#!/usr/bin/env python3
"""
从 Desktop/yishi/new_juese 导入 walk sprite sheet → dist/pac_maze_skins。

用法:
  python backend/tools/import_new_juese_skins.py
  python backend/tools/import_new_juese_skins.py --src "C:\\Users\\...\\new_juese"
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional

REPO = Path(__file__).resolve().parents[2]
DEFAULT_SRC = Path(r"C:\Users\Administrator\Desktop\yishi\new_juese")
SKINS_ROOT = REPO / "dist" / "pac_maze_skins"
BUNDLE_VERSION = "25"

TOOLS = Path(__file__).resolve().parent
SPLIT_SCRIPT = TOOLS / "split_walk_sprite_sheet.py"
NORMALIZE_SCRIPT = TOOLS / "normalize_pac_maze_skin.py"
PACK_SCRIPT = TOOLS / "pack_sprite_sheets.py"
VALIDATE_SCRIPT = TOOLS / "validate_pac_maze_skins.py"


@dataclass
class SkinImportSpec:
    skin_id: str
    sheet_name: str
    render: Optional[Dict] = None
    walk_only: bool = False


SPECS: List[SkinImportSpec] = [
    SkinImportSpec("laoshu_walk", "laoshu-sprite_sheet_014.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("wenzi_walk", "wenzi-sprite_sheet_002.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("xia_walk", "xiaoxia-sprite_sheet_013.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("qinting_walk", "qingting-sprite_sheet_010.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("long_walk", "sprite_sheet.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("toushi_walk", "投石侠.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("zombie_walk", "僵尸.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("food_chick_walker_pro_max", "行走小鸡pro-max.png", {"syncWalkCycleToSprite": True}, walk_only=True),
    SkinImportSpec("fire_long_walk", "fire-long.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("green_long_walk", "green-long.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("haimian_walk", "haimian.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("ice_long_walk", "ice-long.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("bl_long_walk", "lanlong.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("magic_dog_walk", "mage-dog.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("paidaxin_walk", "paidaxin.png", {"syncWalkCycleToSprite": True}),
    SkinImportSpec("qishi_dog_walk", "qishi-dog.png", {"syncWalkCycleToSprite": True}),
]


def run_py(script: Path, *args: str) -> None:
    cmd = [sys.executable, str(script), *args]
    print("+", " ".join(cmd))
    subprocess.run(cmd, check=True, cwd=str(REPO))


def write_manifest(skin_root: Path, skin_id: str, walk_count: int, render: Optional[Dict], walk_only: bool) -> None:
    manifest_path = skin_root / "anim_manifest.json"
    if walk_only and manifest_path.is_file():
        data = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
        clips = data.setdefault("clips", {})
        walk_clip = clips.get("walk")
        if isinstance(walk_clip, dict):
            walk_clip["count"] = walk_count
            if "sheet" in walk_clip:
                del walk_clip["sheet"]
        else:
            clips["walk"] = {"count": walk_count, "folder": "walk", "prefix": "walk"}
        data["normalized"] = False
        render_block = data.setdefault("render", {})
        if render:
            render_block.update(render)
        manifest_path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        return

    manifest = {
        "schemaVersion": 1,
        "skinId": skin_id,
        "normalized": False,
        "clips": {"walk": walk_count},
    }
    if render:
        manifest["render"] = render
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def import_skin(src_dir: Path, spec: SkinImportSpec) -> int:
    sheet_path = src_dir / spec.sheet_name
    if not sheet_path.is_file():
        raise FileNotFoundError(f"missing sheet: {sheet_path}")

    skin_root = SKINS_ROOT / spec.skin_id
    walk_dir = skin_root / "walk"
    if spec.walk_only:
        skin_root.mkdir(parents=True, exist_ok=True)
    else:
        if skin_root.exists():
            shutil.rmtree(skin_root)
        skin_root.mkdir(parents=True)

    run_py(SPLIT_SCRIPT, str(sheet_path), str(walk_dir))
    walk_count = len(list(walk_dir.glob("walk_*.png")))
    write_manifest(skin_root, spec.skin_id, walk_count, spec.render, spec.walk_only)

    preview_src = walk_dir / "walk_1.png"
    if preview_src.is_file():
        shutil.copy2(preview_src, skin_root / "preview.png")

    print(f"  imported {spec.skin_id}: {walk_count} walk frames")
    return walk_count


def normalize_walk_only(skin_root: Path) -> None:
    """Pro Max：仅重归一化 walk 片段，保留其它 clip 的 webp sheet。"""
    manifest_path = skin_root / "anim_manifest.json"
    data = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    walk = data.get("clips", {}).get("walk", {})
    count = int(walk.get("count", 0) if isinstance(walk, dict) else walk)
    tmp_root = skin_root.parent / f"_tmp_{skin_root.name}_walk"
    if tmp_root.exists():
        shutil.rmtree(tmp_root)
    tmp_root.mkdir(parents=True)
    tmp_walk = tmp_root / "walk"
    shutil.copytree(skin_root / "walk", tmp_walk)
    tmp_manifest = {
        "schemaVersion": 1,
        "skinId": skin_root.name,
        "normalized": False,
        "clips": {"walk": count},
        "render": data.get("render") or {},
    }
    (tmp_root / "anim_manifest.json").write_text(
        json.dumps(tmp_manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    run_py(NORMALIZE_SCRIPT, str(tmp_root))
    walk_sheet = skin_root / "walk" / "walk_sheet.webp"
    if walk_sheet.is_file():
        walk_sheet.unlink()
    for png in (skin_root / "walk").glob("walk_*.png"):
        png.unlink()
    for png in tmp_walk.glob("walk_*.png"):
        shutil.copy2(png, skin_root / "walk" / png.name)
    norm = json.loads((tmp_root / "anim_manifest.json").read_text(encoding="utf-8-sig"))
    data["clips"]["walk"] = norm["clips"]["walk"]
    data["normalized"] = norm.get("normalized", True)
    data["canvas"] = norm.get("canvas")
    data["anchorFrac"] = norm.get("anchorFrac")
    data["schemaVersion"] = norm.get("schemaVersion", 2)
    if norm.get("platformerMetrics"):
        pm = data.setdefault("platformerMetrics", {})
        pm["walk"] = norm["platformerMetrics"]["walk"]
    manifest_path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    shutil.rmtree(tmp_root)


def main(argv: List[str]) -> int:
    src = Path(argv[2]) if len(argv) > 2 and argv[1] == "--src" else DEFAULT_SRC
    if not src.is_dir():
        print(f"source not found: {src}", file=sys.stderr)
        return 1
    SKINS_ROOT.mkdir(parents=True, exist_ok=True)

    print(f"=== Import new_juese from {src} ===")
    for spec in SPECS:
        import_skin(src, spec)

    print("\n=== Normalize ===")
    for spec in SPECS:
        skin_root = SKINS_ROOT / spec.skin_id
        if spec.walk_only:
            normalize_walk_only(skin_root)
        else:
            run_py(NORMALIZE_SCRIPT, str(skin_root))

    print("\n=== Pack sheets ===")
    run_py(PACK_SCRIPT, "--all", str(SKINS_ROOT))

    print("\n=== Validate ===")
    run_py(VALIDATE_SCRIPT, str(SKINS_ROOT))

    (SKINS_ROOT / "bundle_version.txt").write_text(BUNDLE_VERSION, encoding="ascii")
    print(f"\nDone. bundle_version={BUNDLE_VERSION}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
