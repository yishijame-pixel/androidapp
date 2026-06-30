"""SuperTux 全关卡注册表：章节 → levelId → .stl 路径。"""
from __future__ import annotations

import re
from pathlib import Path
from typing import Any, Dict, List

# 跳过 SuperTux 菜单 / credits（非可玩关）
SKIP_WORLD_DIRS = frozenset({"misc", "world3", "world4"})

CHAPTERS: List[Dict[str, Any]] = [
    {
        "id": "supertux_antarctic",
        "title": "南极探险",
        "subtitle": "SuperTux World 1 · 改编",
        "world_dir": "world1",
        "level_start": 901,
        "tilesetId": "supertux_antarctic",
        "theme": "PACK_SUPERTUX",
        "bgmEvent": "bgm_supertux_antarctic",
        "sky_top": "#FFB0D4E8",
        "sky_bottom": "#FFE8F4FC",
    },
    {
        "id": "supertux_forest",
        "title": "森林秘境",
        "subtitle": "SuperTux World 2 · 改编",
        "world_dir": "world2",
        "level_start": 941,
        "tilesetId": "supertux_forest",
        "theme": "PACK_SUPERTUX",
        "bgmEvent": "bgm_supertux_forest",
        "sky_top": "#FF81C784",
        "sky_bottom": "#FFE8F5E9",
    },
    {
        "id": "supertux_bonus",
        "title": "Bonus 挑战",
        "subtitle": "SuperTux Bonus · 改编",
        "world_dir": "bonus1",
        "level_start": 981,
        "tilesetId": "supertux_bonus",
        "theme": "PACK_SUPERTUX",
        "bgmEvent": "bgm_supertux_bonus",
        "sky_top": "#FF90CAF9",
        "sky_bottom": "#FFE3F2FD",
    },
    {
        "id": "supertux_redmond",
        "title": "Redmond 复仇",
        "subtitle": "SuperTux Revenge · 改编",
        "world_dir": "revenge_in_redmond",
        "level_start": 1011,
        "tilesetId": "supertux_redmond",
        "theme": "PACK_SUPERTUX",
        "bgmEvent": "bgm_supertux_redmond",
        "sky_top": "#FF7986CB",
        "sky_bottom": "#FFE8EAF6",
    },
]

# 已知关卡中文名（其余从文件名 humanize）
STL_TITLES_ZH: Dict[str, str] = {
    "world1/welcome_antarctica.stl": "欢迎来到南极",
    "world1/journey_begins.stl": "旅程开始",
    "world1/fork_in_the_road.stl": "岔路",
    "world1/frosted_fields.stl": "霜冻原野",
    "world1/bouncy_mountainside.stl": "弹跳山坡",
    "world1/stone_cold.stl": "石寒",
    "world1/into_stars.stl": "星空之下",
    "world1/path_in_clouds.stl": "云中径",
    "world1/more_snowballs.stl": "更多雪球",
    "world1/night_chill.stl": "寒夜",
    "world1/intro.stl": "企鹅入门",
    "world1/castle_of_nolok.stl": "Nolok 城堡",
    "world1/yeti_boss.stl": "雪人 Boss",
    "world2/welcome_forest.stl": "欢迎来到森林",
    "world2/ghosttree_boss.stl": "幽灵树 Boss",
}


def humanize_stl(stl_rel: str) -> str:
    stem = Path(stl_rel).stem
    return re.sub(r"\b\w", lambda m: m.group(0).upper(), stem.replace("_", " "))


def title_for_stl(stl_rel: str) -> str:
    return STL_TITLES_ZH.get(stl_rel) or humanize_stl(stl_rel)


def worldmap_stl_order(supertux_root: Path, world_dir: str) -> List[str]:
    """按 worldmap.stwm 中 (level \"…\") 出现顺序排列。"""
    stwm = supertux_root / "data" / "levels" / world_dir / "worldmap.stwm"
    if not stwm.is_file():
        return []
    text = stwm.read_text(encoding="utf-8", errors="replace")
    names = re.findall(r'\(level\s+"([^"]+\.stl)"\)', text)
    return [f"{world_dir}/{name}" for name in names]


def discover_level_specs(supertux_root: Path) -> List[Dict[str, Any]]:
    levels_dir = supertux_root / "data" / "levels"
    specs: List[Dict[str, Any]] = []
    for chapter in CHAPTERS:
        world = chapter["world_dir"]
        world_path = levels_dir / world
        if not world_path.is_dir():
            continue
        on_disk = sorted(f"{world}/{p.name}" for p in world_path.glob("*.stl"))
        ordered = worldmap_stl_order(supertux_root, world)
        seen = set(ordered)
        for rel in on_disk:
            if rel not in seen:
                ordered.append(rel)
        for i, stl_rel in enumerate(ordered):
            specs.append(
                {
                    "id": chapter["level_start"] + i,
                    "stl": stl_rel,
                    "title": title_for_stl(stl_rel),
                    "chapterId": chapter["id"],
                    "scroll": None,
                }
            )
    return specs


def chapter_catalog_entries(specs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    entries: List[Dict[str, Any]] = []
    for chapter in CHAPTERS:
        ids = [s["id"] for s in specs if s.get("chapterId") == chapter["id"]]
        if not ids:
            continue
        entries.append(
            {
                "id": chapter["id"],
                "title": chapter["title"],
                "subtitle": chapter["subtitle"],
                "levelRange": [min(ids), max(ids)],
                "tilesetId": chapter["tilesetId"],
                "theme": chapter["theme"],
                "bgmEvent": chapter["bgmEvent"],
                "skyTop": chapter["sky_top"],
                "skyBottom": chapter["sky_bottom"],
            }
        )
    return entries


def level_end_id(specs: List[Dict[str, Any]]) -> int:
    return max(s["id"] for s in specs) if specs else 910
