#!/usr/bin/env python3
"""
企业级横版素材全量导入：zip → dist/platformer + app/assets/tilesets + content_catalog.json
用法:
  python backend/tools/platformer_catalog/import_platformer_catalog.py
  python backend/tools/platformer_catalog/import_platformer_catalog.py --download-dir d:\\download
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
import zipfile
from dataclasses import asdict
from pathlib import Path
from typing import Any, Dict, List, Optional

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(Path(__file__).resolve().parent))

from import_platformer_character import (  # noqa: E402
    CharacterImportSpec,
    import_character_from_zip,
    import_hillclimb_assets,
    import_jelly_assets,
    import_plane_assets,
)

DEFAULT_DOWNLOAD = Path(r"d:\download")
DIST = ROOT / "dist" / "platformer"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "platformer"
BUNDLE_VERSION = 1

CHARACTER_SPECS: List[CharacterImportSpec] = [
    CharacterImportSpec("temple_runner", "templerun.zip", group="hero", title="神庙跑者", subtitle="跑酷 · 滑铲", unlock_type="default"),
    CharacterImportSpec("adventure_girl", "adventure_girl.zip", group="hero", title="冒险少女", subtitle="近战 · 射击", unlock_type="level_clear", unlock_value=5, abilities=["shoot", "melee"]),
    CharacterImportSpec("ninja_girl", "ninjagirlnew.zip", group="hero", title="忍者少女", subtitle="攀爬 · 滑翔 · 苦无", unlock_type="level_clear", unlock_value=12, abilities=["climb", "glide", "throw"]),
    CharacterImportSpec("ninja_boy", "ninjaadventurenew.zip", group="hero", title="忍者少年", subtitle="攀爬 · 滑翔 · 苦无", unlock_type="endless_tiles", unlock_value=500, abilities=["climb", "glide", "throw"]),
    CharacterImportSpec("jack", "jackfree.zip", group="hero", title="杰克", subtitle="滑铲 · 跑酷", unlock_type="level_clear", unlock_value=8, abilities=["slide"]),
    CharacterImportSpec("red_hat", "redhatfiles.zip", group="hero", title="小红帽", subtitle="受击反馈", unlock_type="level_clear", unlock_value=15, abilities=["hurt"]),
    CharacterImportSpec("robot", "robotfree.zip", group="hero", title="战斗机器人", subtitle="射击 · 近战", unlock_type="level_clear", unlock_value=20, abilities=["shoot", "melee"]),
    CharacterImportSpec("dino", "freedinosprite.zip", group="heavy", title="小恐龙", subtitle="重型跳跃", unlock_type="endless_tiles", unlock_value=300),
    CharacterImportSpec("knight", "freeknight.zip", group="heavy", title="圣骑士", subtitle="攻击 · 跳攻", unlock_type="level_clear", unlock_value=25, abilities=["attack", "jump_attack"]),
    CharacterImportSpec("santa", "santasprites.zip", group="event", title="圣诞老人", subtitle="节日限定", unlock_type="event", unlock_value=1, abilities=["slide"]),
    CharacterImportSpec("cat", "catndog (1).zip", "png/cat", group="pet", title="猫咪", subtitle="宠物系", unlock_type="level_clear", unlock_value=10),
    CharacterImportSpec("dog", "catndog (1).zip", "png/dog", group="pet", title="狗狗", subtitle="宠物系", unlock_type="level_clear", unlock_value=10),
]

ENEMY_SPECS: List[CharacterImportSpec] = [
    CharacterImportSpec("zombie_male", "zombiefiles.zip", "png/male", role="enemy", title="僵尸(男)", group="undead"),
    CharacterImportSpec("zombie_female", "zombiefiles.zip", "png/female", role="enemy", title="僵尸(女)", group="undead"),
    CharacterImportSpec("wild_dog", "catndog (1).zip", "png/dog", role="enemy", title="野狗", group="beast"),
    CharacterImportSpec("dino_enemy", "freedinosprite.zip", role="enemy", title="恐龙", group="beast"),
    CharacterImportSpec("robot_sentry", "robotfree.zip", role="enemy", title="机械哨兵", group="mech"),
]

TILESET_SPECS = [
    {"id": "forest", "zip": "freetileset (1).zip", "tiles": "png/Tiles/{n}.png", "bg": "png/BG/BG.png", "objects": "png/Object/", "tileCount": 18, "theme": "PACK_FOREST"},
    {"id": "graveyard", "zip": "graveyardtilesetnew.zip", "tiles": "png/Tiles/Tile ({n}).png", "bg": "png/BG.png", "objects": "png/Objects/", "tileCount": 20, "theme": "PACK_GRAVEYARD"},
    {"id": "scifi", "zip": "freescifiplatform (1).zip", "tiles": "png/Tiles/Tile ({n}).png", "bg": "png/Tiles/BGTile (1).png", "objects": "png/Objects/", "tileCount": 15, "theme": "PACK_SCIFI"},
    {"id": "desert", "zip": "deserttileset (1).zip", "tiles": "png/Tile/{n}.png", "bg": "png/BG.png", "objects": "png/Objects/", "tileCount": 16, "theme": "PACK_DESERT"},
    {"id": "winter", "zip": "wintertileset (1).zip", "tiles": "png/Tiles/{n}.png", "bg": "png/BG/BG.png", "objects": "png/Object/", "tileCount": 18, "theme": "PACK_WINTER"},
]


def ensure_dir(p: Path) -> None:
    p.mkdir(parents=True, exist_ok=True)


def extract_entry(zf: zipfile.ZipFile, entry_name: str, dest: Path) -> bool:
    entry = next((e for e in zf.infolist() if e.filename.replace("\\", "/") == entry_name.replace("\\", "/")), None)
    if entry is None:
        entry = next((e for e in zf.infolist() if e.filename.replace("\\", "/").endswith("/" + entry_name.split("/")[-1])), None)
    if entry is None:
        return False
    ensure_dir(dest.parent)
    with zf.open(entry) as src, open(dest, "wb") as dst:
        dst.write(src.read())
    return True


def import_tileset(download: Path, spec: Dict[str, Any]) -> None:
    zip_path = download / spec["zip"]
    if not zip_path.is_file():
        print(f"  skip tileset {spec['id']}: missing {spec['zip']}")
        return
    dest = ASSETS / "tilesets" / spec["id"]
    if dest.exists():
        shutil.rmtree(dest)
    ensure_dir(dest / "tiles")
    ensure_dir(dest / "objects")
    tile_count = int(spec.get("tileCount", 18))
    with zipfile.ZipFile(zip_path) as zf:
        for i in range(1, tile_count + 1):
            name = spec["tiles"].format(n=i)
            extract_entry(zf, name, dest / "tiles" / f"{i}.png")
        # pad to 18 for autotile engine
        fill = dest / "tiles" / "1.png"
        target_count = max(tile_count, 18)
        if fill.is_file():
            for i in range(1, target_count + 1):
                tile_path = dest / "tiles" / f"{i}.png"
                if not tile_path.is_file():
                    shutil.copy(fill, tile_path)
        bg_pattern = spec["bg"]
        bg_entry = next((e for e in zf.infolist() if bg_pattern.split("/")[-1] in e.filename), None)
        if bg_entry:
            extract_entry(zf, bg_entry.filename, dest / "bg.png")
        obj_prefix = spec["objects"]
        for e in zf.infolist():
            fn = e.filename.replace("\\", "/")
            if obj_prefix in fn and fn.endswith(".png") and not fn.split("/")[-1].startswith("._"):
                extract_entry(zf, fn, dest / "objects" / fn.split("/")[-1])
    manifest = {
        "id": spec["id"],
        "schemaVersion": 1,
        "tileSize": 128,
        "tileCount": tile_count,
        "mapping": "craftpix_v1",
        "theme": spec["theme"],
        "sourceZip": spec["zip"],
    }
    (dest / "tileset_manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"  tileset {spec['id']} imported")


def build_catalog(download: Path) -> Dict[str, Any]:
    characters = []
    for spec in CHARACTER_SPECS:
        characters.append({
            "id": spec.character_id,
            "title": spec.title,
            "subtitle": spec.subtitle,
            "group": spec.group,
            "assetRoot": f"platformer_characters/characters/{spec.character_id}",
            "sourceZip": spec.zip_name,
            "unlock": {"type": spec.unlock_type, "value": spec.unlock_value},
            "abilities": spec.abilities,
            "render": {
                "heightCellFrac": spec.height_cell_frac,
                "mirrorDefault": spec.mirror_default,
            },
            "requiredClips": ["idle", "run", "jump", "die"],
        })
    # legacy built-in characters
    characters.extend([
        {"id": "chick_pro_max", "title": "行走小鸡 Pro Max", "subtitle": "官方角色 · 默认", "group": "default",
         "assetRoot": "pac_maze_skins/food_chick_walker_pro_max", "source": "pac_maze", "unlock": {"type": "default", "value": 0}},
        {"id": "treasure_hunter", "title": "宝藏猎人", "subtitle": "本地像素", "group": "legacy",
         "assetRoot": "platformer/characters/treasure_hunter", "source": "local_apk", "unlock": {"type": "default", "value": 0}},
        {"id": "pixel_walker", "title": "像素行者", "subtitle": "本地行走", "group": "legacy",
         "assetRoot": "platformer/player", "source": "local_apk", "unlock": {"type": "default", "value": 0}},
    ])

    enemies = []
    for spec in ENEMY_SPECS:
        enemies.append({
            "id": spec.character_id,
            "title": spec.title,
            "group": spec.group,
            "assetRoot": f"platformer_characters/enemies/{spec.character_id}",
            "sourceZip": spec.zip_name,
            "behavior": "PATROL",
        })

    tilesets = []
    for spec in TILESET_SPECS:
        tilesets.append({
            "id": spec["id"],
            "assetRoot": f"platformer/tilesets/{spec['id']}",
            "theme": spec["theme"],
            "tileCount": spec.get("tileCount", 18),
            "sourceZip": spec["zip"],
        })

    modes = [
        {"id": "campaign", "title": "闯关", "entry": "hub_default"},
        {"id": "endless", "title": "无尽跑酷", "entry": "hub_chip"},
        {"id": "temple_run", "title": "神庙跑酷", "characterId": "temple_runner", "entry": "hub_mode", "sourceZip": "templerun.zip"},
        {"id": "plane_shooter", "title": "天空射击", "entry": "hub_mode", "sourceZip": "free_plane_sprite.zip"},
        {"id": "hill_climb", "title": "登山挑战", "entry": "hub_mode", "sourceZip": "hillclimb.zip"},
        {"id": "jelly_decor", "title": "果冻换装", "entry": "character_detail", "sourceZip": "jelly.zip"},
    ]

    hero_levels = []
    level_specs = [
        (63, "神庙试炼", "forest", "temple_runner", None, "TEMPLE_TRIAL"),
        (64, "少女远征", "desert", "adventure_girl", "zombie_female", "SHOOT_GALLERY"),
        (65, "忍者道场", "scifi", "ninja_girl", "robot_sentry", "CLIMB_WALL"),
        (66, "杰克大冒险", "winter", "jack", "wild_dog", "SLIDE_TUNNEL"),
        (67, "小红帽森林", "forest", "red_hat", "zombie_male", "HURT_SPIKES"),
        (68, "机械废墟", "scifi", "robot", "robot_sentry", "TURRET_ALLEY"),
        (69, "恐龙峡谷", "desert", "dino", "dino_enemy", "HEAVY_JUMP"),
        (70, "骑士远征", "graveyard", "knight", "zombie_male", "MELEE_ARENA"),
        (71, "圣诞夜行", "winter", "santa", None, "GIFT_COLLECT"),
        (72, "猫狗大战", "forest", "cat", "wild_dog", "PET_DASH"),
        (73, "果冻沼泽", "forest", None, "jelly_slime", "BOUNCE_MARSH"),
        (74, "群英终章", "scifi", None, None, "HERO_FINALE"),
    ]
    for lid, title, tile, char, enemy, segment in level_specs:
        hero_levels.append({
            "id": lid,
            "title": title,
            "chapter": "HEROES",
            "tilesetId": tile,
            "featuredCharacterId": char,
            "featuredEnemyId": enemy,
            "segmentProfile": segment,
        })

    return {
        "schemaVersion": 1,
        "bundleVersion": BUNDLE_VERSION,
        "generatedFrom": str(download),
        "characters": characters,
        "enemies": enemies,
        "tilesets": tilesets,
        "modes": modes,
        "campaign": {
            "heroChapter": {
                "id": "heroes",
                "title": "群英荟萃",
                "filter": "HEROES",
                "levelRange": [63, 74],
                "levels": hero_levels,
            },
        },
    }


def main() -> int:
    download = DEFAULT_DOWNLOAD
    if "--download-dir" in sys.argv:
        download = Path(sys.argv[sys.argv.index("--download-dir") + 1])

    print(f"Import platformer catalog from {download}")
    if DIST.exists():
        shutil.rmtree(DIST)
    ensure_dir(DIST / "characters")
    ensure_dir(DIST / "enemies")
    ensure_dir(DIST / "modes")

    print("Tilesets → APK assets")
    for spec in TILESET_SPECS:
        import_tileset(download, spec)

    print("Characters → dist/platformer")
    for spec in CHARACTER_SPECS:
        zp = download / spec.zip_name
        if not zp.is_file():
            print(f"  skip {spec.character_id}: {spec.zip_name}")
            continue
        counts = import_character_from_zip(zp, DIST, spec)
        print(f"  {spec.character_id}: {counts}")

    print("Enemies → dist/platformer")
    for spec in ENEMY_SPECS:
        zp = download / spec.zip_name
        if not zp.is_file():
            continue
        counts = import_character_from_zip(zp, DIST, spec)
        print(f"  {spec.character_id}: {counts}")

    for name, fn in [
        ("free_plane_sprite.zip", import_plane_assets),
        ("hillclimb.zip", import_hillclimb_assets),
        ("jelly.zip", import_jelly_assets),
    ]:
        zp = download / name
        if zp.is_file():
            fn(zp, DIST)
            print(f"  mode assets from {name}")

    catalog = build_catalog(download)
    catalog_path = DIST / "content_catalog.json"
    catalog_path.write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    # also ship catalog in APK for offline catalog parsing
    ensure_dir(ASSETS)
    (ASSETS / "content_catalog.json").write_text(catalog_path.read_text(encoding="utf-8"), encoding="utf-8")
    (DIST / "bundle_version.txt").write_text(f"{BUNDLE_VERSION}\n", encoding="utf-8")

    # Ship characters/enemies into APK for offline/dev (cloud bundle mirrors same layout)
    apk_chars = ASSETS / "platformer_characters"
    if apk_chars.exists():
        shutil.rmtree(apk_chars)
    shutil.copytree(DIST / "characters", apk_chars / "characters")
    shutil.copytree(DIST / "enemies", apk_chars / "enemies")
    shutil.copytree(DIST / "modes", apk_chars / "modes")
    (apk_chars / "content_catalog.json").write_text(catalog_path.read_text(encoding="utf-8"), encoding="utf-8")
    (apk_chars / "bundle_version.txt").write_text(f"{BUNDLE_VERSION}\n", encoding="utf-8")
    print(f"  APK bundle mirror: {apk_chars}")

    norm_script = ROOT / "backend" / "tools" / "platformer_catalog" / "normalize_platformer_character.py"
    print("Normalize all character/enemy manifests")
    subprocess.run([sys.executable, str(norm_script), "--all", str(DIST)], check=False)

    print(f"Done. Catalog: {catalog_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
