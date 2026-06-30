#!/usr/bin/env python3
"""从 Craftpix 风格 zip 导入横版角色/敌人序列帧 → platformer 标准目录。"""
from __future__ import annotations

import json
import re
import shutil
import zipfile
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

# 动作名 → 标准 clip
CLIP_ALIASES = {
    "idle": "idle",
    "walk": "walk",
    "run": "run",
    "jump": "jump",
    "slide": "slide",
    "dead": "die",
    "die": "die",
    "attack": "attack",
    "melee": "attack",
    "shoot": "shoot",
    "hurt": "hurt",
    "fall": "fall",
    "climb": "climb",
    "glide": "glide",
    "throw": "throw",
    "jumpattack": "jump_attack",
    "jump_attack": "jump_attack",
    "jumpthrow": "jump_throw",
    "jump_throw": "jump_throw",
    "jumpmelee": "jump_attack",
    "jumpshoot": "jump_shoot",
    "runshoot": "run_shoot",
    "fly": "fly",
    "shoot": "shoot",
}

FRAME_PATTERNS = [
    re.compile(r"^(?P<action>[A-Za-z_ ]+?)(?:__|_)(?P<idx>\d+)\.png$", re.I),
    re.compile(r"^(?P<action>[A-Za-z_ ]+?) \((?P<idx>\d+)\)\.png$", re.I),
]


@dataclass
class CharacterImportSpec:
    character_id: str
    zip_name: str
    zip_subfolder: str = ""  # e.g. png/male
    role: str = "character"  # character | enemy
    group: str = "hero"
    title: str = ""
    subtitle: str = ""
    unlock_type: str = "default"  # default | level_clear | endless_tiles | event
    unlock_value: int = 0
    abilities: List[str] = field(default_factory=list)
    mirror_default: bool = True
    height_cell_frac: float = 1.55


def normalize_action(raw: str) -> Optional[str]:
    key = raw.strip().lower().replace(" ", "").replace("-", "_")
    if key in CLIP_ALIASES:
        return CLIP_ALIASES[key]
    for alias, clip in CLIP_ALIASES.items():
        if key == alias.replace("_", ""):
            return clip
    return None


def parse_frame_name(name: str) -> Optional[Tuple[str, int]]:
    for pat in FRAME_PATTERNS:
        m = pat.match(name)
        if not m:
            continue
        action = normalize_action(m.group("action"))
        if action is None:
            continue
        return action, int(m.group("idx"))
    return None


def iter_zip_pngs(zf: zipfile.ZipFile, subfolder: str = "") -> Iterable[Tuple[str, zipfile.ZipInfo]]:
    prefix = subfolder.replace("\\", "/").strip("/")
    if prefix:
        prefix += "/"
    for info in zf.infolist():
        if info.is_dir():
            continue
        name = info.filename.replace("\\", "/")
        if not name.lower().endswith(".png"):
            continue
        if "__MACOSX" in name or name.split("/")[-1].startswith("._"):
            continue
        base = name.split("/")[-1]
        if prefix and not name.startswith(prefix):
            continue
        yield base, info


def role_folder(role: str) -> str:
    return "enemies" if role == "enemy" else f"{role}s"


def import_character_from_zip(
    zip_path: Path,
    dest_root: Path,
    spec: CharacterImportSpec,
) -> Dict[str, int]:
    dest = dest_root / role_folder(spec.role) / spec.character_id
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True, exist_ok=True)

    grouped: Dict[str, List[Tuple[int, zipfile.ZipInfo, str]]] = defaultdict(list)
    with zipfile.ZipFile(zip_path) as zf:
        for base, info in iter_zip_pngs(zf, spec.zip_subfolder):
            parsed = parse_frame_name(base)
            if not parsed:
                continue
            clip, idx = parsed
            grouped[clip].append((idx, info, base))

        clip_counts: Dict[str, int] = {}
        for clip, items in sorted(grouped.items()):
            items.sort(key=lambda t: t[0])
            clip_dir = dest / clip
            clip_dir.mkdir(parents=True, exist_ok=True)
            for out_i, (_, info, _) in enumerate(items, start=1):
                out_name = f"{clip}_{out_i}.png"
                out_path = clip_dir / out_name
                with zf.open(info) as src, open(out_path, "wb") as dst:
                    dst.write(src.read())
            clip_counts[clip] = len(items)

    clips_manifest = {}
    for clip, count in clip_counts.items():
        clips_manifest[clip] = {"count": count, "folder": clip, "prefix": clip}

    manifest = {
        "schemaVersion": 1,
        "skinId": spec.character_id,
        "normalized": False,
        "clips": clips_manifest,
        "render": {"syncWalkCycleToSprite": True, "sampleSize": 1},
        "platformer": {
            "role": spec.role,
            "group": spec.group,
            "title": spec.title or spec.character_id,
            "subtitle": spec.subtitle,
            "unlock": {"type": spec.unlock_type, "value": spec.unlock_value},
            "abilities": spec.abilities,
            "mirrorDefault": spec.mirror_default,
            "heightCellFrac": spec.height_cell_frac,
        },
    }
    (dest / "anim_manifest.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return clip_counts


def import_plane_assets(zip_path: Path, dest_root: Path) -> None:
    dest = dest_root / "modes" / "plane"
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path) as zf:
        for _, info in iter_zip_pngs(zf):
            rel = info.filename.replace("\\", "/")
            parts = rel.split("/")
            if len(parts) >= 2:
                sub = parts[-2]
                out = dest / sub / parts[-1]
            else:
                out = dest / parts[-1]
            out.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(info) as src, open(out, "wb") as dst:
                dst.write(src.read())


def import_hillclimb_assets(zip_path: Path, dest_root: Path) -> None:
    dest = dest_root / "modes" / "hillclimb"
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path) as zf:
        for _, info in iter_zip_pngs(zf):
            rel = info.filename.replace("\\", "/")
            out = dest / rel.replace("png/separate/", "").replace("/", "_")
            out.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(info) as src, open(out, "wb") as dst:
                dst.write(src.read())


def import_jelly_assets(zip_path: Path, dest_root: Path) -> None:
    dest = dest_root / "modes" / "jelly"
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path) as zf:
        for _, info in iter_zip_pngs(zf):
            rel = info.filename.replace("\\", "/")
            out_name = rel.replace("/", "__")
            out = dest / out_name
            with zf.open(info) as src, open(out, "wb") as dst:
                dst.write(src.read())
