#!/usr/bin/env python3
"""Safe terrain: cross-spine + nested house outlines + connectivity-checked pillars."""
from __future__ import annotations

import json
from pathlib import Path
from typing import List, Sequence, Tuple

OUT = Path(__file__).parent
WALL = set("#bwt")

LEVELS = {
    1: (17, 13, 7, 8, 6, "#"),
    2: (19, 15, 7, 9, 7, "#"),
    3: (17, 15, 7, 8, 7, "#"),
    4: (19, 15, 7, 9, 7, "#"),
    5: (19, 17, 7, 9, 8, "#"),
    6: (19, 14, 6, 9, 6, "#"),
    7: (21, 15, 7, 10, 7, "#"),
    8: (19, 16, 7, 9, 8, "#"),
    9: (17, 15, 7, 8, 7, "#"),
    10: (21, 17, 7, 10, 8, "#"),
    11: (21, 17, 8, 10, 8, "b"),
    12: (23, 21, 10, 11, 10, "b"),
    13: (21, 21, 10, 10, 10, "b"),
}

TARGET = [0, 200, 280, 360, 440, 520, 600, 680, 760, 840, 920, 1000, 1100, 1200]


def wall_count(grid: Sequence[str]) -> int:
    return sum(1 for row in grid for ch in row if ch in WALL)


def complexity(level_id: int, w: int, h: int, hazards: int, grid: Sequence[str]) -> int:
    walls = wall_count(grid)
    return level_id * 60 + walls * 3 + hazards * 140 + (w + h) * 6


def flood(grid: List[List[str]], sx: int, sy: int) -> int:
    h, w = len(grid), len(grid[0])

    def ok(x: int, y: int) -> bool:
        return grid[y][x] not in WALL

    if not ok(sx, sy):
        return 0
    seen, q = {(sx, sy)}, [(sx, sy)]
    while q:
        x, y = q.pop()
        for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in seen and ok(nx, ny):
                seen.add((nx, ny))
                q.append((nx, ny))
    return len(seen)


def walkable_total(grid: Sequence[str]) -> int:
    return sum(1 for row in grid for ch in row if ch not in WALL)


def blank(w: int, h: int, b: str) -> List[List[str]]:
    return [[b if x in (0, w - 1) or y in (0, h - 1) else "." for x in range(w)] for y in range(h)]


def stamp(g, x, y, ch):
    if 0 <= y < len(g) and 0 <= x < len(g[0]):
        g[y][x] = ch


def rect_outline_open(g, x1, y1, x2, y2, ch, cx, cy):
    x1, x2 = sorted((x1, x2))
    y1, y2 = sorted((y1, y2))
    for x in range(x1, x2 + 1):
        stamp(g, x, y1, ch)
        stamp(g, x, y2, ch)
    for y in range(y1, y2 + 1):
        stamp(g, x1, y, ch)
        stamp(g, x2, y, ch)
    for x, y in ((cx, y1), (cx, y2), (x1, cy), (x2, cy), (cx, cy)):
        stamp(g, x, y, ".")


def build(level_id: int) -> List[str]:
    w, h, gate, cx, cy, theme = LEVELS[level_id]
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = "*" if x in (1, w - 2) else "."
        g[h - 2][x] = "*" if x in (1, w - 2) else "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    stamp(g, 1, gate, "=")
    stamp(g, w - 2, gate, "=")
    for x in range(2, w - 2):
        stamp(g, x, cy, ".")
    for y in range(2, h - 2):
        stamp(g, cx, y, ".")
    # nested house / courtyard shells
    shells = (1 + (level_id - 11)) if level_id >= 11 else (1 + level_id // 4)
    if level_id in (9, 10):
        shells += 1
    shells = min(shells, min(w, h) // 4)
    for i in range(shells):
        m = 3 + i * 2
        if m >= min(cx, cy) - 1:
            break
        rect_outline_open(g, m, m, w - 1 - m, h - 1 - m - (2 if level_id < 11 else 3), theme, cx, cy)
    if level_id >= 11:
        rect_outline_open(g, cx - 3, cy - 2, cx + 3, cy + 2, "w", cx, cy)
        stamp(g, cx, h - 4, ".")
    if level_id >= 13:
        rect_outline_open(g, cx - 1, cy - 1, cx + 1, cy + 1, "w", cx, cy)
    stamp(g, cx, cy, "@" if level_id >= 11 else "G")
    stamp(g, cx, cy - 1, "H")
    return ["".join(r) for r in g]


def ok_grid(grid: List[str], pac: Tuple[int, int]) -> bool:
    g = [list(r) for r in grid]
    return walkable_total(grid) - flood(g, pac[0], pac[1]) <= 4


def bump(
    grid: List[str],
    level_id: int,
    pac: Tuple[int, int],
    reserved: set[Tuple[int, int]],
    w: int,
    h: int,
    hazards: int,
    target: int,
    theme: str,
) -> List[str]:
    g = [list(r) for r in grid]
    if not ok_grid(rows(g), pac):
        raise ValueError("base disconnected")
    cands = [(x, y) for y in range(2, h - 2) for x in range(2, w - 2) if g[y][x] == "."]
    cands.sort(key=lambda p: abs(p[0] - pac[0]) + abs(p[1] - pac[1]), reverse=True)
    for x, y in cands:
        if complexity(level_id, w, h, hazards, rows(g)) >= target:
            break
        if (x, y) in reserved:
            continue
        ch = g[y][x]
        g[y][x] = theme
        if ok_grid(rows(g), pac):
            continue
        g[y][x] = ch
    return rows(g)


def rows(g):
    return ["".join(r) for r in g]


def force_walkable(grid: List[str], positions: set[Tuple[int, int]]) -> List[str]:
    g = [list(r) for r in grid]
    for x, y in positions:
        if 0 <= y < len(g) and 0 <= x < len(g[0]):
            g[y][x] = "."
    return rows(g)


def patch(path: Path, prev_cx: int) -> int:
    data = json.loads(path.read_text(encoding="utf-8"))
    lid = data["id"]
    w, h, _, _, _, theme = LEVELS[lid]
    pac = tuple(data["spawn"]["pac"])
    hazards = len(data.get("hazards") or [])
    reserved = {pac}
    for gx, gy in data["spawn"].get("ghosts") or []:
        reserved.add((gx, gy))
    target = max(TARGET[lid], prev_cx + 32)
    grid = bump(build(lid), lid, pac, reserved, w, h, hazards, target, theme)
    grid = force_walkable(grid, reserved)
    if not ok_grid(grid, pac):
        raise ValueError(f"{path.name} disconnected")
    data["grid"] = grid
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    cx = complexity(lid, w, h, hazards, grid)
    print(f"{path.name} cx={cx} target={target}")
    return cx


def audit():
    prev = 0
    for p in sorted(OUT.glob("level_*.json")):
        d = json.loads(p.read_text(encoding="utf-8"))
        cx = complexity(d["id"], d["width"], d["height"], len(d.get("hazards") or []), d["grid"])
        tag = "OK" if prev == 0 or cx >= prev else "DROP"
        print(f"{p.name} cx={cx} {tag}")
        prev = cx


if __name__ == "__main__":
    prev = 0
    for p in sorted(OUT.glob("level_*.json")):
        prev = patch(p, prev)
    print("---")
    audit()
