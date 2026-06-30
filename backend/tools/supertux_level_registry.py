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

# 关卡中文名（全量 107 关；import / apply_supertux_level_titles_zh.py 共用）
STL_TITLES_ZH: Dict[str, str] = {
    # World 1 — 南极 901–931
    "world1/welcome_antarctica.stl": "欢迎来到南极",
    "world1/journey_begins.stl": "旅程开始",
    "world1/somewhat_smaller_bath.stl": "略小的浴池",
    "world1/fork_in_the_road.stl": "岔路",
    "world1/frosted_fields.stl": "霜冻原野",
    "world1/stone_cold.stl": "石寒",
    "world1/more_snowballs.stl": "更多雪球",
    "world1/bouncy_mountainside.stl": "弹跳山坡",
    "world1/above_arctic_skies.stl": "北极上空",
    "world1/23rd_airborne.stl": "第23空降旅",
    "world1/night_chill.stl": "寒夜",
    "world1/into_stars.stl": "星空之下",
    "world1/entrance_cave.stl": "入口洞穴",
    "world1/under_the_ice.stl": "冰层之下",
    "world1/living_inside_fridge.stl": "冰箱之中",
    "world1/or_just_me.stl": "或者只有我",
    "world1/deep_dive_chill.stl": "深潜极寒",
    "world1/ice_in_the_hole.stl": "冰洞危机",
    "world1/end_of_tunnel.stl": "隧道尽头",
    "world1/path_in_clouds.stl": "云中径",
    "world1/slippery_slide.stl": "湿滑滑道",
    "world1/shattered_bridge.stl": "破碎桥梁",
    "world1/antarctic_outpost.stl": "南极前哨",
    "world1/castle_of_nolok.stl": "Nolok 城堡",
    "world1/yeti_boss.stl": "雪人 Boss",
    "world1/crystal_mine.stl": "水晶矿洞",
    "world1/between_glaciers.stl": "冰川之间",
    "world1/intro.stl": "企鹅入门",
    "world1/yeti_cutscene.stl": "雪人剧情",
    "world1/castle_cutscene.stl": "城堡剧情",
    "world1/yetiwin_cutscene.stl": "雪人胜利剧情",
    # World 2 — 森林 941–978
    "world2/welcome_forest.stl": "欢迎来到森林",
    "world2/rock_roll.stl": "滚石奇遇",
    "world2/shallow_green.stl": "浅绿之地",
    "world2/find_bigger_fish.stl": "更大的鱼",
    "world2/mount_crushmore.stl": "碎石山",
    "world2/tux_builder.stl": "Tux 建造师",
    "world2/mouldy_grotto.stl": "霉穴",
    "world2/wooden_roots.stl": "木根迷宫",
    "world2/penguin_on_tree.stl": "树上企鹅",
    "world2/three_sheets_wind.stl": "三口之风",
    "world2/bouncy_coils.stl": "弹跳线圈",
    "world2/granito_village.stl": "花岗岩村",
    "world2/flooded_chambers.stl": "淹没密室",
    "world2/drop_ball.stl": "落球机关",
    "world2/crumbling_path.stl": "崩裂之路",
    "world2/owls_skydive_commando.stl": "猫头鹰跳伞队",
    "world2/shocking.stl": "电击陷阱",
    "world2/going_underground.stl": "地下行进",
    "world2/cave_patrol.stl": "洞穴巡逻",
    "world2/through_dark.stl": "穿越黑暗",
    "world2/entangled_roots.stl": "缠绕之根",
    "world2/square_root_agony.stl": "平方根之痛",
    "world2/worse_salmonella.stl": "沙门菌危机",
    "world2/home_dead_home.stl": "家不成家",
    "world2/hollow_earth.stl": "空心地球",
    "world2/sticks_stones.stl": "棍石齐飞",
    "world2/root_for_you.stl": "为你扎根",
    "world2/striking_wood.stl": "击木成路",
    "world2/collapse_imminent.stl": "崩塌在即",
    "world2/rooted_tower.stl": "根须高塔",
    "world2/floral_blossom.stl": "花开时节",
    "world2/ancient_ruin.stl": "古代遗迹",
    "world2/lost_sanctuary.stl": "失落圣所",
    "world2/ghosttree_boss.stl": "幽灵树 Boss",
    "world2/forest_intro.stl": "森林序章",
    "world2/corrupted_cutscene.stl": "腐化剧情",
    "world2/ghosttreewin_cutscene.stl": "幽灵树胜利剧情",
    "world2/ghosttree_cutscene.stl": "幽灵树剧情",
    # Bonus — 981–1010
    "bonus1/where_everything_possible.stl": "一切皆有可能",
    "bonus1/fjerd.stl": "峡湾",
    "bonus1/bonus_dias.stl": "奖励 Dias",
    "bonus1/maze_in_sky.stl": "空中迷宫",
    "bonus1/mysterious_house_of_ice.stl": "神秘冰屋",
    "bonus1/snow_bowling.stl": "雪地保龄",
    "bonus1/pipe_down_over_there.stl": "管道入口",
    "bonus1/tip_of_iceberg.stl": "冰山一角",
    "bonus1/train_leaves_in_one_minute.stl": "一分钟发车",
    "bonus1/cave_of_mirrors.stl": "镜之洞穴",
    "bonus1/crumbel_cavern.stl": "面包屑洞穴",
    "bonus1/all_that_glistens.stl": "闪光皆诱惑",
    "bonus1/luft_airship.stl": "Luft 飞艇",
    "bonus1/mario.stl": "马里奥致敬关",
    "bonus1/high_gravity.stl": "高重力",
    "bonus1/snowmans_land.stl": "雪人国",
    "bonus1/let_us_snow.stl": "让雪落下",
    "bonus1/ice_test.stl": "冰雪试炼",
    "bonus1/end_of_ice_age.stl": "冰河期末日",
    "bonus1/penguins_cant_fly.stl": "企鹅不会飞",
    "bonus1/lies.stl": "谎言",
    "bonus1/area_42.stl": "42 区",
    "bonus1/noloks_party_pit.stl": "Nolok 派对坑",
    "bonus1/semi_frozen.stl": "半冻结",
    "bonus1/fire_in_the_sky.stl": "空中烈火",
    "bonus1/castle_in_sky.stl": "天空城堡",
    "bonus1/flanders_ice_field.stl": "弗兰德冰原",
    "bonus1/another_cold_day.stl": "又一个寒冷日",
    "bonus1/snowy_sunset.stl": "雪色夕阳",
    "bonus1/refisherator.stl": "制冷再制",
    # Redmond — 1011–1018
    "revenge_in_redmond/antarctica.stl": "南极",
    "revenge_in_redmond/who_is_gown.stl": "谁在穿袍",
    "revenge_in_redmond/long_office_nights.stl": "漫长办公夜",
    "revenge_in_redmond/get_to_choppa.stl": "快上直升机",
    "revenge_in_redmond/where_my_super_cape.stl": "我的超级披风呢",
    "revenge_in_redmond/redmond_headquarters.stl": "Redmond 总部",
    "revenge_in_redmond/intro.stl": "Redmond 序章",
    "revenge_in_redmond/outro.stl": "Redmond 终章",
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
