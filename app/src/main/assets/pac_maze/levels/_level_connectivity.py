#!/usr/bin/env python3
"""Pac maze level connectivity: pac spawn must reach all ghosts and checkpoints."""
from __future__ import annotations

from collections import deque
from typing import Callable, List, Sequence, Set, Tuple

WALL = set("#bwt")
DYNAMIC = "&"
ENERGY_GATE = "G"


def rows(g: List[List[str]]) -> List[str]:
    return ["".join(r) for r in g]


def is_static_passable(ch: str) -> bool:
    if ch in WALL:
        return False
    if ch in (DYNAMIC, ENERGY_GATE):
        return False
    return True


def flood(grid: Sequence[str], sx: int, sy: int, passable: Callable[[str], bool] = is_static_passable) -> Set[Tuple[int, int]]:
    h, w = len(grid), len(grid[0])
    if not (0 <= sx < w and 0 <= sy < h) or not passable(grid[sy][sx]):
        return set()
    seen: Set[Tuple[int, int]] = {(sx, sy)}
    q: deque[Tuple[int, int]] = deque([(sx, sy)])
    while q:
        x, y = q.popleft()
        for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in seen and passable(grid[ny][nx]):
                seen.add((nx, ny))
                q.append((nx, ny))
    return seen


def required_tiles(data: dict) -> List[Tuple[int, int]]:
    out: List[Tuple[int, int]] = []
    for gx, gy in data["spawn"].get("ghosts") or []:
        out.append((int(gx), int(gy)))
    for m in data.get("markers") or []:
        if m.get("type") == "checkpoint":
            out.append((int(m["x"]), int(m["y"])))
    return out


def find_spawn(data: dict) -> Tuple[int, int]:
    pac = data["spawn"]["pac"]
    return int(pac[0]), int(pac[1])


def snap_spawn_to_walkable(data: dict) -> None:
    w, h = data["width"], data["height"]
    g = data["grid"]
    px, py = find_spawn(data)
    if is_static_passable(g[py][px]):
        return
    candidates = []
    for y in range(h - 2, 1, -1):
        for x in range(1, w - 1):
            if is_static_passable(g[y][x]):
                candidates.append((abs(x - px) + abs(y - py), x, y))
    if not candidates:
        raise ValueError(f"L{data['id']} no walkable pac spawn found")
    _, x, y = min(candidates)
    data["spawn"]["pac"] = [x, y]


def open_center_gates(g: List[List[str]], cx: int) -> None:
    h, w = len(g), len(g[0])
    for y in range(2, h - 2):
        if g[y][cx] not in WALL:
            continue
        wall_cells = sum(1 for x in range(2, w - 2) if g[y][x] in WALL)
        if wall_cells < (w - 4) // 3:
            continue
        g[y][cx] = "."
        for dx in (-1, 1):
            nx = cx + dx
            if 2 <= nx < w - 2:
                g[y][nx] = "."


def carve_manhattan(g: List[List[str]], x1: int, y1: int, x2: int, y2: int) -> None:
    x, y = x1, y1
    while x != x2:
        if g[y][x] in WALL:
            g[y][x] = "."
        x += 1 if x2 > x else -1
    while y != y2:
        if g[y][x] in WALL:
            g[y][x] = "."
        y += 1 if y2 > y else -1
    if g[y][x] in WALL:
        g[y][x] = "."


def nearest_missing(reach: Set[Tuple[int, int]], missing: Tuple[int, int]) -> Tuple[int, int]:
    mx, my = missing
    return min(reach, key=lambda p: abs(p[0] - mx) + abs(p[1] - my))


def heal_connectivity(data: dict) -> None:
    snap_spawn_to_walkable(data)
    g = [list(r) for r in data["grid"]]
    w, h = data["width"], data["height"]
    px, py = find_spawn(data)
    cx = w // 2
    open_center_gates(g, cx)

    targets = required_tiles(data)
    for _ in range(w * h * 2):
        grid = rows(g)
        reach = flood(grid, px, py)
        missing = [t for t in targets if t not in reach]
        if not missing:
            data["grid"] = grid
            return
        anchor = nearest_missing(reach, missing[0])
        carve_manhattan(g, anchor[0], anchor[1], missing[0][0], missing[0][1])

    data["grid"] = rows(g)
    reach = flood(data["grid"], px, py)
    missing = [t for t in targets if t not in reach]
    if missing:
        raise ValueError(f"L{data['id']} still disconnected: {missing[:6]}")


def ensure_link_portals(data: dict) -> None:
    """Ensure every level has a horizontal LINK pair on side = tunnel tiles."""
    w, h = data["width"], data["height"]
    grid = data["grid"]
    markers = data.setdefault("markers", [])

    markers[:] = [m for m in markers if m.get("tag") != "LINK"]

    left = [(1, y) for y in range(h) if len(grid[y]) > 2 and grid[y][1] == "="]
    right = [(w - 2, y) for y in range(h) if len(grid[y]) > w - 2 and grid[y][w - 2] == "="]

    pair_y = None
    for _lx, ly in left:
        if (w - 2, ly) in right:
            pair_y = ly
            break

    if pair_y is None:
        cy = h // 2
        g = [list(r) for r in grid]
        for y in range(max(2, cy - 3), min(h - 2, cy + 4)):
            if g[y][1] not in WALL:
                g[y][1] = "="
                if g[y][w - 2] not in WALL:
                    g[y][w - 2] = "="
                pair_y = y
                data["grid"] = rows(g)
                grid = data["grid"]
                break
        if pair_y is None:
            g[cy][1] = "."
            g[cy][1] = "="
            g[cy][w - 2] = "."
            g[cy][w - 2] = "="
            data["grid"] = rows(g)
            pair_y = cy

    def occupied(x: int, y: int) -> bool:
        return any(int(m["x"]) == x and int(m["y"]) == y for m in markers)

    lx, rx = 1, w - 2
    if not occupied(lx, pair_y):
        markers.append({"type": "checkpoint", "x": lx, "y": pair_y, "label": "001", "tag": "LINK"})
    if not occupied(rx, pair_y):
        markers.append({"type": "checkpoint", "x": rx, "y": pair_y, "label": "002", "tag": "LINK"})


def fix_link_portal_markers(data: dict) -> None:
    ensure_link_portals(data)


def validate_level(data: dict) -> None:
    snap_spawn_to_walkable(data)
    px, py = find_spawn(data)
    grid = data["grid"]
    if not is_static_passable(grid[py][px]):
        raise ValueError(f"L{data['id']} pac spawn not walkable at {px},{py}")
    reach = flood(grid, px, py)
    for t in required_tiles(data):
        if t not in reach:
            raise ValueError(f"L{data['id']} tile {t} unreachable from pac {px},{py}")
