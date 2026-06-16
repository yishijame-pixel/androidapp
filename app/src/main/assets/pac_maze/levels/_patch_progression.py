#!/usr/bin/env python3
"""Patch all pac maze levels: progressive difficulty, item spawners, star criteria."""
import json
from pathlib import Path

OUT = Path(__file__).parent

WALKABLE = set(".o *=-@GHI><^v$E")


def is_walkable(ch: str) -> bool:
    return ch in WALKABLE


def ghost_speed_floor(level_id: int) -> float:
    return 0.42 + (level_id - 1) * 0.035


def ai_floor(level_id: int) -> float:
    return 0.45 + (level_id - 1) * 0.032


def spawner_count(level_id: int) -> int:
    if level_id <= 2:
        return 2
    if level_id <= 5:
        return 3
    if level_id <= 9:
        return 4
    return 5


def spawner_interval(level_id: int) -> int:
    return max(420, 900 - (level_id - 1) * 32)


POOLS = {
    1: [
        ["magnet", "shield"],
        ["shield", "magnet"],
    ],
    2: [
        ["magnet", "shield"],
        ["shield", "magnet"],
    ],
    3: [
        ["frost", "shield"],
        ["magnet", "speed"],
        ["speed", "frost"],
    ],
    4: [
        ["frost", "shield"],
        ["magnet", "speed"],
        ["speed", "frost"],
    ],
    5: [
        ["frost", "shield"],
        ["magnet", "speed"],
        ["speed", "frost"],
    ],
    6: [
        ["frost", "shield"],
        ["magnet", "speed"],
        ["speed", "frost"],
        ["shield", "magnet", "frost"],
    ],
    7: [
        ["frost", "shield"],
        ["magnet", "speed"],
        ["speed", "frost"],
        ["shield", "magnet", "frost"],
    ],
    8: [
        ["frost", "double"],
        ["magnet", "charge"],
        ["shield", "speed"],
        ["double", "frost", "charge"],
    ],
    9: [
        ["frost", "double"],
        ["magnet", "charge"],
        ["shield", "speed"],
        ["double", "frost", "charge"],
    ],
    10: [
        ["frost", "double"],
        ["magnet", "charge"],
        ["shield", "speed"],
        ["double", "frost", "charge"],
    ],
}


def pool_for(level_id: int, index: int) -> list[str]:
    tier = 1 if level_id <= 2 else 3 if level_id <= 5 else 4 if level_id <= 7 else 10
    pools = POOLS.get(tier, POOLS[10])
    if level_id >= 11:
        pools = [
            ["frost", "charge", "double"],
            ["magnet", "shield", "charge"],
            ["speed", "double", "frost"],
            ["shield", "charge", "magnet"],
            ["frost", "double", "speed", "charge"],
        ]
    return pools[index % len(pools)]


def open_neighbors(grid, w, h, x, y):
    n = 0
    for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        nx, ny = x + dx, y + dy
        if 0 <= nx < w and 0 <= ny < h and is_walkable(grid[ny][nx]):
            n += 1
    return n


def find_positions(data):
    grid = data["grid"]
    h = data["height"]
    w = data["width"]
    pac = tuple(data["spawn"]["pac"])
    ghosts = {tuple(g) for g in data["spawn"]["ghosts"]}
    cx, cy = w / 2, h / 2
    quads = [
        lambda x, y: x < cx and y < cy,
        lambda x, y: x >= cx and y < cy,
        lambda x, y: x < cx and y >= cy,
        lambda x, y: x >= cx and y >= cy,
        lambda x, y: abs(x - cx) < 1 and y >= cy,
    ]
    picked = []
    used = {pac} | ghosts
    for in_quad in quads:
        cands = []
        for y in range(1, h - 1):
            for x in range(1, w - 1):
                if not in_quad(x, y):
                    continue
                if (x, y) in used:
                    continue
                if not is_walkable(grid[y][x]):
                    continue
                if open_neighbors(grid, w, h, x, y) < 2:
                    continue
                dist = ((x - pac[0]) ** 2 + (y - pac[1]) ** 2) ** 0.5
                if dist < 3.5:
                    continue
                score = dist * 1.4 + open_neighbors(grid, w, h, x, y) * 0.6
                cands.append((score, x, y))
        if cands:
            _, x, y = max(cands)
            picked.append((x, y))
            used.add((x, y))
    return picked


def patch_level(data):
    lid = data["id"]
    data["difficulty"] = {
        "ghost_speed_mul": round(ghost_speed_floor(lid), 2),
        "ai_aggression": round(ai_floor(lid), 2),
    }

    target = spawner_count(lid)
    existing = data.get("itemSpawners") or []
    used = {(s["x"], s["y"]) for s in existing}
    if len(existing) < target:
        for x, y in find_positions(data):
            if len(existing) >= target:
                break
            if (x, y) in used:
                continue
            idx = len(existing)
            existing.append({
                "id": f"factory_{lid}_{idx:02d}",
                "x": x,
                "y": y,
                "intervalTicks": spawner_interval(lid),
                "pool": pool_for(lid, idx),
            })
            used.add((x, y))
    else:
        for i, sp in enumerate(existing):
            sp["intervalTicks"] = min(sp.get("intervalTicks", 900), spawner_interval(lid) + 60)
            if not sp.get("pool"):
                sp["pool"] = pool_for(lid, i)
    data["itemSpawners"] = existing[:target]

    base = 1000 + lid * 120
    data["starCriteria"] = {
        "twoStarMinScore": base + 400,
        "threeStarMinScore": base + 1200,
        "threeStarMaxSeconds": 100 + lid * 8,
        "threeStarNoDeath": lid >= 9,
    }

    if lid == 6 and not data.get("hazards"):
        data["hazards"] = [{"type": "laser_h", "y": 7, "x1": 4, "x2": 14, "id": "fork_scan"}]
    if lid == 11 and not data.get("hazards"):
        data["hazards"] = [{"type": "laser_v", "x": 10, "y1": 4, "y2": 12, "id": "courtyard_gate"}]

    return data


def main():
    for path in sorted(OUT.glob("level_*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        patch_level(data)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("patched", path.name, "spawners=", len(data["itemSpawners"]), "diff=", data["difficulty"])


if __name__ == "__main__":
    main()
