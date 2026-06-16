#!/usr/bin/env python3
"""Generate extreme levels L14-L23: enterable pulse-gate chambers, hazards, rewards."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

OUT = Path(__file__).parent
WALL = set("#bwt")
MECH = set("&=G@")
DYNAMIC = "&"
STRIPE = 3

LEVELS: Dict[int, Tuple[int, int, str, str, str]] = {
    14: (23, 21, "#", "赛博-脉冲闸道", "脉冲闸道 · 移动墙初现"),
    15: (23, 23, "#", "赛博-光束矩阵", "光束矩阵 · 十字激光网"),
    16: (25, 21, "b", "花园-机关园林", "机关园林 · 能量门阵"),
    17: (25, 23, "#", "糖果-糖霜要塞", "糖霜要塞 · 炮塔走廊"),
    18: (25, 25, "b", "古风-天罗地网", "天罗地网 · 五重围合"),
    19: (27, 23, "#", "赛博-数据风暴", "数据风暴 · 动态迷宫"),
    20: (27, 25, "b", "花园-移动屏风", "移动屏风 · 条纹机关"),
    21: (27, 27, "b", "古风-奇门八卦", "奇门八卦 · 八卦阵图"),
    22: (29, 25, "#", "赛博-终极协议", "终极协议 · 六厂熔炉"),
    23: (29, 27, "#", "赛博-核心熔炉", "核心熔炉 · 地狱终局"),
}


def complexity(level_id: int, w: int, h: int, hazards: int, grid: Sequence[str]) -> int:
    walls = sum(1 for row in grid for ch in row if ch in WALL)
    mech = sum(1 for row in grid for ch in row if ch in MECH)
    return level_id * 60 + walls * 3 + hazards * 140 + mech * 28 + (w + h) * 6


def blank(w: int, h: int, border: str) -> List[List[str]]:
    return [[border if x in (0, w - 1) or y in (0, h - 1) else "." for x in range(w)] for y in range(h)]


def stamp(g: List[List[str]], x: int, y: int, ch: str) -> None:
    if 0 <= y < len(g) and 0 <= x < len(g[0]):
        g[y][x] = ch


def rows(g: List[List[str]]) -> List[str]:
    return ["".join(r) for r in g]


def rect_outline_wide_gates(g, x1, y1, x2, y2, ch, cx, cy, gate: int = 3):
    """矩形外墙 + 四向宽门 + 四角通道（避免死区）。"""
    x1, x2 = sorted((x1, x2))
    y1, y2 = sorted((y1, y2))
    half = gate // 2
    for x in range(x1, x2 + 1):
        stamp(g, x, y1, ch)
        stamp(g, x, y2, ch)
    for y in range(y1, y2 + 1):
        stamp(g, x1, y, ch)
        stamp(g, x2, y, ch)
    for dx in range(-half, half + 1):
        stamp(g, cx + dx, y1, ".")
        stamp(g, cx + dx, y2, ".")
        stamp(g, x1, cy + dx, ".")
        stamp(g, x2, cy + dx, ".")
    for dx in range(-1, 2):
        for dy in range(-1, 2):
            stamp(g, cx + dx, cy + dy, ".")
    # 四角斜向通道，防止嵌套壳层形成不可进入的角落
    stamp(g, x1 + 1, y1 + 1, ".")
    stamp(g, x2 - 1, y1 + 1, ".")
    stamp(g, x1 + 1, y2 - 1, ".")
    stamp(g, x2 - 1, y2 - 1, ".")
    _vein_corner(g, x1 + 1, y1 + 1, cx, cy)
    _vein_corner(g, x2 - 1, y1 + 1, cx, cy)
    _vein_corner(g, x1 + 1, y2 - 1, cx, cy)
    _vein_corner(g, x2 - 1, y2 - 1, cx, cy)


def _vein_corner(g, lx: int, ly: int, cx: int, cy: int) -> None:
    """从壳层角落向主轴挖 L 形永久通道。"""
    step_x = 1 if lx <= cx else -1
    step_y = 1 if ly <= cy else -1
    x, y = lx, ly
    while x != cx:
        stamp(g, x, y, ".")
        x += step_x
    while y != cy:
        stamp(g, x, y, ".")
        y += step_y
    stamp(g, cx, cy, ".")


def dynamic_lanes_in_room(g, x1, y1, x2, y2, leave_border: int = 1):
    """室内移动墙条纹：留永久走道边带，中间为可切换的 & 栅栏。"""
    x1, x2 = sorted((x1, x2))
    y1, y2 = sorted((y1, y2))
    for y in range(y1 + leave_border, y2 - leave_border + 1):
        if (y - y1) % 2 == 0:
            continue
        for x in range(x1 + leave_border, x2 - leave_border + 1):
            if g[y][x] not in (".", "*"):
                continue
            stamp(g, x, y, DYNAMIC if (x + y) % 2 == 1 else ".")


def pulse_gate_wing(g, x1, y1, x2, y2, theme: str, open_edge: str, open_center: Tuple[int, int], gate: int = 3):
    """
    脉冲闸道舱：固定宽门 + 内部移动墙条纹。
    open_edge: left|right|top|bottom — 门朝向主轴的一侧。
    """
    x1, x2 = sorted((x1, x2))
    y1, y2 = sorted((y1, y2))
    half = gate // 2
    ox, oy = open_center
    for x in range(x1, x2 + 1):
        stamp(g, x, y1, theme)
        stamp(g, x, y2, theme)
    for y in range(y1, y2 + 1):
        stamp(g, x1, y, theme)
        stamp(g, x2, y, theme)
    if open_edge == "right":
        for dy in range(-half, half + 1):
            stamp(g, x2, oy + dy, ".")
        for y in range(y1 + 1, y2):
            if g[x1 + 1][y] == theme:
                stamp(g, x1 + 1, y, ".")
        dynamic_lanes_in_room(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, leave_border=1)
    elif open_edge == "left":
        for dy in range(-half, half + 1):
            stamp(g, x1, oy + dy, ".")
        for y in range(y1 + 1, y2):
            if g[x2 - 1][y] == theme:
                stamp(g, x2 - 1, y, ".")
        dynamic_lanes_in_room(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, leave_border=1)
    elif open_edge == "bottom":
        for dx in range(-half, half + 1):
            stamp(g, ox + dx, y2, ".")
        for x in range(x1 + 1, x2):
            if g[x][y1 + 1] == theme:
                stamp(g, x, y1 + 1, ".")
        dynamic_lanes_in_room(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, leave_border=1)
    else:  # top
        for dx in range(-half, half + 1):
            stamp(g, ox + dx, y1, ".")
        for x in range(x1 + 1, x2):
            if g[x][y2 - 1] == theme:
                stamp(g, x, y2 - 1, ".")
        dynamic_lanes_in_room(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, leave_border=1)
    # 主轴不被闸道外墙截断
    for y in range(y1, y2 + 1):
        if g[ox][y] == theme and ox != x1 and ox != x2:
            stamp(g, ox, y, ".")
    for x in range(x1, x2 + 1):
        if g[x][oy] == theme and oy != y1 and oy != y2:
            stamp(g, x, oy, ".")


def is_passable(ch: str, x: int, y: int, phase: int) -> bool:
    if ch in WALL:
        return False
    if ch == DYNAMIC:
        return (x + y) % STRIPE == phase
    return True


def flood_union_set(grid: List[str], sx: int, sy: int) -> set[Tuple[int, int]]:
    union: set[Tuple[int, int]] = set()
    for phase in range(STRIPE):
        g = [list(r) for r in grid]
        h, w = len(g), len(g[0])
        if not is_passable(g[sy][sx], sx, sy, phase):
            continue
        seen, q = {(sx, sy)}, [(sx, sy)]
        while q:
            x, y = q.pop()
            union.add((x, y))
            for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in seen and is_passable(g[ny][nx], nx, ny, phase):
                    seen.add((nx, ny))
                    q.append((nx, ny))
    return union


def dynamic_tiles(grid: Sequence[str]) -> List[Tuple[int, int]]:
    out = []
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch == DYNAMIC:
                out.append((x, y))
    return out


def heal_sealed_regions(g: List[List[str]], pac: Tuple[int, int], w: int, h: int) -> List[str]:
    """把未连通区域用永久通道接到玩家可达区（必要时破墙）。"""
    px, py = pac
    for _ in range(w * h):
        grid = rows(g)
        reach = flood_union_set(grid, px, py)
        sealed = next(
            ((x, y) for y in range(h) for x in range(w) if g[y][x] not in WALL and (x, y) not in reach),
            None,
        )
        if sealed is None:
            return rows(g)
        x, y = sealed
        guard = 0
        while (x, y) not in reach and guard < w + h:
            guard += 1
            stamp(g, x, y, ".")
            if x != px:
                x += 1 if px > x else -1
            elif y != py:
                y += 1 if py > y else -1
            else:
                break
            grid = rows(g)
            reach = flood_union_set(grid, px, py)
    return rows(g)


def validate_enterable(grid: List[str], pac: Tuple[int, int]) -> None:
    """闸道内所有非墙格（含 &）须在某个相位从玩家出生点可达。"""
    reachable = flood_union_set(grid, pac[0], pac[1])
    blocked = []
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch in WALL:
                continue
            if (x, y) not in reachable:
                blocked.append((x, y, ch))
    if blocked:
        sample = blocked[:8]
        raise ValueError(f"sealed chamber cells {len(blocked)} e.g. {sample}")
    for x, y in dynamic_tiles(grid):
        if (x, y) not in reachable:
            raise ValueError(f"dynamic gate unreachable at {x},{y}")


def ok_grid(grid: List[str], pac: Tuple[int, int]) -> bool:
    reachable = flood_union_set(grid, pac[0], pac[1])
    unreachable = sum(
        1
        for y, row in enumerate(grid)
        for x, ch in enumerate(row)
        if ch not in WALL and ch != DYNAMIC and (x, y) not in reachable
    )
    return unreachable <= 4


def build_grid(level_id: int) -> Tuple[List[str], int, int]:
    builders = {
        14: _layout_side_tunnels,
        15: _layout_nine_rooms,
        16: _layout_orbit_ring,
        17: _layout_twin_shafts,
        18: _layout_wrapping_maze,
        19: _layout_s_snake,
        20: _layout_archive_aisles,
        21: _layout_metro_platforms,
        22: _layout_scattered_forge,
        23: _layout_dense_core,
    }
    return builders[level_id]()


def _finish_layout(
    level_id: int,
    g: List[List[str]],
    cx: int,
    cy: int,
    pac_y: int | None = None,
) -> Tuple[List[str], int, int]:
    w, h = len(g[0]), len(g)
    theme, _, _, _, _ = LEVELS[level_id]
    py = pac_y if pac_y is not None else h - 3
    for x in range(1, w - 1):
        if g[1][x] not in WALL:
            g[1][x] = "*" if x in (1, w - 2) else g[1][x]
        if g[h - 2][x] not in WALL:
            g[h - 2][x] = "*" if x in (1, w - 2) else g[h - 2][x]
    stamp(g, 1, cy, "=")
    stamp(g, w - 2, cy, "=")
    stamp(g, cx, cy, "@")
    stamp(g, cx, cy - 1, "H")
    if level_id >= 15:
        stamp(g, cx - 1, cy, "I")
    if level_id >= 17:
        stamp(g, cx + 1, cy, ">")
    if level_id >= 19:
        stamp(g, cx + 2, cy - 1, "^")
        stamp(g, cx - 2, cy + 1, "v")
    if level_id >= 21:
        stamp(g, cx + 3, cy, ">")
        stamp(g, cx - 3, cy, "<")
    if level_id >= 20:
        stamp(g, cx, cy + 1, "G")
    for y in range(max(2, py - 1), min(h - 1, py + 2)):
        stamp(g, cx, y, ".")
    for x in range(max(2, cx - 2), min(w - 2, cx + 3)):
        stamp(g, x, py, ".")
    stamp(g, cx, py, ".")
    return rows(g), cx, cy


def _layout_side_tunnels() -> Tuple[List[str], int, int]:
    """L14: 经典侧向隧道 + 上下不对称翼舱（非嵌套方框）。"""
    w, h, theme, _, _ = LEVELS[14]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    for x in range(2, w - 2):
        stamp(g, x, cy, ".")
        stamp(g, x, cy - 4, ".")
        stamp(g, x, cy + 3, ".")
    for y in range(3, h - 3):
        stamp(g, 2, y, ".")
        stamp(g, w - 3, y, ".")
    # 左舱：L 形，非对称
    for x in range(4, cx - 2):
        stamp(g, x, cy - 2, theme)
        stamp(g, x, cy + 2, theme)
    for y in range(cy - 5, cy + 4):
        stamp(g, 4, y, theme)
        stamp(g, cx - 3, y, theme)
    for y in range(cy - 1, cy + 2):
        stamp(g, 4, y, ".")
    dynamic_lanes_in_room(g, 5, cy - 4, cx - 4, cy + 3)
    # 右舱：窄竖井
    for y in range(cy - 6, cy + 5):
        stamp(g, w - 5, y, theme)
        stamp(g, w - 8, y, theme)
    for x in range(w - 8, w - 4):
        stamp(g, x, cy - 5, theme)
        stamp(g, x, cy + 4, theme)
    for y in range(cy - 3, cy + 3):
        stamp(g, w - 8, y, ".")
    # 上区：三列柱廊
    for x in (cx - 5, cx, cx + 5):
        for y in range(3, cy - 5):
            stamp(g, x, y, theme)
    for y in range(3, cy - 5):
        stamp(g, cx - 5, y, ".")
        stamp(g, cx, y, ".")
        stamp(g, cx + 5, y, ".")
    return _finish_layout(14, g, cx, cy)


def _layout_nine_rooms() -> Tuple[List[str], int, int]:
    """L15: 3×3 不规则九宫格房间。"""
    w, h, theme, _, _ = LEVELS[15]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    xs = [3, cx - 2, cx + 2, w - 4]
    ys = [3, cy - 3, cy + 2, h - 4]
    for xi in range(len(xs) - 1):
        for yi in range(len(ys) - 1):
            x1, x2 = xs[xi], xs[xi + 1]
            y1, y2 = ys[yi], ys[yi + 1]
            for x in range(x1, x2 + 1):
                stamp(g, x, y1, theme)
                stamp(g, x, y2, theme)
            for y in range(y1, y2 + 1):
                stamp(g, x1, y, theme)
                stamp(g, x2, y, theme)
            door_x = x1 + (x2 - x1) // 2 + (1 if xi == 1 else 0)
            door_y = y1 + (y2 - y1) // 2 + (-1 if yi == 0 else 0)
            stamp(g, door_x, y1, ".")
            stamp(g, door_x, y2, ".")
            stamp(g, x1, door_y, ".")
            stamp(g, x2, door_y, ".")
            for x in range(x1 + 1, x2):
                for y in range(y1 + 1, y2):
                    stamp(g, x, y, ".")
    for x in range(2, w - 2):
        stamp(g, x, cy, ".")
    for y in range(2, h - 2):
        stamp(g, cx, y, ".")
    dynamic_lanes_in_room(g, xs[1] + 1, ys[1] + 1, xs[2] - 1, ys[2] - 1)
    return _finish_layout(15, g, cx, cy)


def _layout_orbit_ring() -> Tuple[List[str], int, int]:
    """L16: 椭圆跑道 + 中心岛群（非十字嵌套）。"""
    w, h, theme, _, _ = LEVELS[16]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    rx, ry = w // 2 - 3, h // 2 - 3
    for y in range(2, h - 2):
        for x in range(2, w - 2):
            nx = (x - cx) / max(rx, 1)
            ny = (y - cy) / max(ry, 1)
            dist = nx * nx + ny * ny
            if 0.42 < dist < 0.92:
                stamp(g, x, y, ".")
            elif dist >= 0.92:
                stamp(g, x, y, theme)
    for x in range(cx - 2, cx + 3):
        stamp(g, x, cy - 1, theme)
        stamp(g, x, cy + 2, theme)
    for y in range(cy - 1, cy + 3):
        stamp(g, cx - 3, y, theme)
        stamp(g, cx + 3, y, theme)
    for x in range(cx - 2, cx + 3):
        for y in range(cy, cy + 2):
            stamp(g, x, y, ".")
    stamp(g, cx - 5, cy - 4, theme)
    stamp(g, cx + 4, cy + 3, theme)
    stamp(g, cx + 6, cy - 2, theme)
    return _finish_layout(16, g, cx, cy)


def _layout_twin_shafts() -> Tuple[List[str], int, int]:
    """L17: 双竖井 + 错层天桥 + 中央障碍带。"""
    w, h, theme, _, _ = LEVELS[17]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    for y in range(2, h - 2):
        for col in (cx - 8, cx - 6, cx - 5, cx + 5, cx + 6, cx + 8):
            stamp(g, col, y, theme)
    for x in range(2, w - 2):
        if x < cx - 8 or (cx - 4 < x < cx + 4) or x > cx + 8:
            stamp(g, x, cy, ".")
    for bridge_y, x1, x2 in (
        (cy - 6, 3, cx - 9),
        (cy - 2, 4, cx - 9),
        (cy + 2, cx + 9, w - 4),
        (cy + 6, cx + 9, w - 4),
        (cy - 4, cx - 3, cx + 3),
    ):
        for x in range(x1, x2 + 1):
            stamp(g, x, bridge_y, ".")
        if x1 > 2:
            stamp(g, x1, bridge_y, ".")
        if x2 < w - 3:
            stamp(g, x2, bridge_y, ".")
    for y in range(3, h - 3):
        stamp(g, cx - 6, y, ".")
        stamp(g, cx + 6, y, ".")
    for x in range(cx - 3, cx + 4):
        stamp(g, x, cy - 4, theme)
        stamp(g, x, cy + 4, theme)
    dynamic_lanes_in_room(g, cx - 4, cy - 2, cx - 2, cy + 2)
    dynamic_lanes_in_room(g, cx + 2, cy - 3, cx + 4, cy + 1)
    dynamic_lanes_in_room(g, 6, cy - 5, 8, cy - 3)
    return _finish_layout(17, g, cx, cy, pac_y=h - 3)


def _layout_wrapping_maze() -> Tuple[List[str], int, int]:
    """L18: 四角独立舱 + 侧向回廊（类经典吃豆人外圈）。"""
    w, h, theme, _, _ = LEVELS[18]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    corners = [(4, 4, 8, 8), (w - 9, 4, w - 5, 8), (4, h - 9, 9, h - 5), (w - 10, h - 9, w - 5, h - 5)]
    for i, (x1, y1, x2, y2) in enumerate(corners):
        for x in range(x1, x2 + 1):
            stamp(g, x, y1, theme)
            stamp(g, x, y2, theme)
        for y in range(y1, y2 + 1):
            stamp(g, x1, y, theme)
            stamp(g, x2, y, theme)
        stamp(g, (x1 + x2) // 2, y1, ".")
        stamp(g, (x1 + x2) // 2, y2, ".")
        stamp(g, x1 if i % 2 == 0 else x2, (y1 + y2) // 2, ".")
        for x in range(x1 + 1, x2):
            for y in range(y1 + 1, y2):
                stamp(g, x, y, ".")
        if i == 1:
            dynamic_lanes_in_room(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1)
    for x in range(2, w - 2):
        stamp(g, x, cy, ".")
        stamp(g, x, 2, ".")
        stamp(g, x, h - 3, ".")
    for y in range(2, h - 2):
        stamp(g, 2, y, ".")
        stamp(g, w - 3, y, ".")
    stamp(g, cx, 1, "=")
    stamp(g, cx, h - 2, "=")
    return _finish_layout(18, g, cx, cy)


def _layout_s_snake() -> Tuple[List[str], int, int]:
    """L19: S 形主通道 + 侧凹室。"""
    w, h, theme, _, _ = LEVELS[19]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    spine = [
        (3, h - 4), (6, h - 4), (9, h - 6), (12, h - 8), (15, h - 10),
        (18, h - 10), (21, h - 8), (21, h - 5), (18, h - 3), (14, h - 3),
        (10, h - 5), (7, cy), (10, cy - 3), (14, cy - 5), (18, cy - 5),
    ]
    for i in range(len(spine) - 1):
        x1, y1 = spine[i]
        x2, y2 = spine[i + 1]
        x, y = x1, y1
        while x != x2:
            stamp(g, x, y, ".")
            x += 1 if x2 > x else -1
        while y != y2:
            stamp(g, x, y, ".")
            y += 1 if y2 > y else -1
        stamp(g, x2, y2, ".")
    alcoves = [(5, cy + 2, 8, cy + 5), (w - 8, cy - 2, w - 5, cy + 2), (cx - 2, 4, cx + 3, 7)]
    for x1, y1, x2, y2 in alcoves:
        for x in range(x1, x2 + 1):
            stamp(g, x, y1, theme)
            stamp(g, x, y2, theme)
        for y in range(y1, y2 + 1):
            stamp(g, x1, y, theme)
            stamp(g, x2, y, theme)
        stamp(g, x1, (y1 + y2) // 2, ".")
        for x in range(x1 + 1, x2):
            for y in range(y1 + 1, y2):
                stamp(g, x, y, ".")
    dynamic_lanes_in_room(g, alcoves[0][0] + 1, alcoves[0][1] + 1, alcoves[0][2] - 1, alcoves[0][3] - 1)
    return _finish_layout(19, g, cx, cy)


def _layout_archive_aisles() -> Tuple[List[str], int, int]:
    """L20: 档案架平行竖廊 + 错层横向连廊。"""
    w, h, theme, _, _ = LEVELS[20]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    shelves = [5, 9, 13, 17, 21]
    for col in shelves:
        for y in range(2, h - 2):
            if y not in (cy - 3, cy, cy + 4, cy + 7):
                stamp(g, col, y, theme)
            else:
                stamp(g, col, y, ".")
    for link_y in (cy - 3, cy, cy + 4):
        for x in range(3, w - 3):
            stamp(g, x, link_y, ".")
    for x in range(3, w - 3):
        if x not in shelves:
            stamp(g, x, cy + 7, ".")
    dynamic_lanes_in_room(g, 10, cy - 1, 12, cy + 1)
    dynamic_lanes_in_room(g, 18, cy + 2, 20, cy + 4)
    return _finish_layout(20, g, cx, cy)


def _layout_metro_platforms() -> Tuple[List[str], int, int]:
    """L21: 三层地铁站台 + 端部换乘竖井。"""
    w, h, theme, _, _ = LEVELS[21]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    platforms = (cy - 6, cy, cy + 6)
    for py in platforms:
        for x in range(4, w - 4):
            stamp(g, x, py, ".")
        for x in range(6, w - 6, 4):
            stamp(g, x, py - 1, theme)
            stamp(g, x, py + 1, theme)
    for vy in range(3, h - 3):
        stamp(g, 4, vy, ".")
        stamp(g, w - 5, vy, ".")
        stamp(g, cx, vy, ".")
    for py in platforms:
        stamp(g, 4, py, ".")
        stamp(g, w - 5, py, ".")
    dynamic_lanes_in_room(g, cx - 3, cy - 7, cx + 3, cy - 5)
    dynamic_lanes_in_room(g, cx - 3, cy + 5, cx + 3, cy + 7)
    return _finish_layout(21, g, cx, cy)


def _layout_scattered_forge() -> Tuple[List[str], int, int]:
    """L22: 散点熔炉岛 + 斜向连廊。"""
    w, h, theme, _, _ = LEVELS[22]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    islands = [
        (5, 5, 8, 7), (12, 4, 15, 7), (20, 6, 23, 9),
        (6, 12, 9, 15), (16, 11, 19, 14), (22, 13, 25, 16),
        (8, 18, 11, 20), (18, 17, 21, 20),
    ]
    for x1, y1, x2, y2 in islands:
        for x in range(x1, min(x2 + 1, w - 1)):
            stamp(g, x, y1, theme)
            stamp(g, x, min(y2, h - 2), theme)
        for y in range(y1, min(y2 + 1, h - 1)):
            stamp(g, x1, y, theme)
            stamp(g, min(x2, w - 2), y, theme)
        stamp(g, (x1 + x2) // 2, y1, ".")
    links = [(8, 7, 12, 4), (15, 7, 16, 11), (9, 15, 8, 18), (19, 14, 18, 17), (cx, cy, 12, 4)]
    for x1, y1, x2, y2 in links:
        x, y = x1, y1
        while x != x2 or y != y2:
            stamp(g, x, y, ".")
            if x != x2:
                x += 1 if x2 > x else -1
            elif y != y2:
                y += 1 if y2 > y else -1
    stamp(g, cx, cy, ".")
    dynamic_lanes_in_room(g, 12, 4, 14, 6)
    dynamic_lanes_in_room(g, 20, 6, 22, 8)
    return _finish_layout(22, g, cx, cy)


def _layout_dense_core() -> Tuple[List[str], int, int]:
    """L23: 高密度折线迷宫 + 中央熔核。"""
    w, h, theme, _, _ = LEVELS[23]
    cx, cy = w // 2, h // 2
    g = blank(w, h, theme)
    for x in range(1, w - 1):
        g[1][x] = g[h - 2][x] = "."
    for y in range(2, h - 2):
        g[y][1] = g[y][w - 2] = "."
    maze_stamps = []
    for x in range(4, w - 4, 3):
        for y in range(3, h - 3, 2):
            if abs(x - cx) <= 2 and abs(y - cy) <= 2:
                continue
            if (x + y) % 5 == 0:
                continue
            maze_stamps.append((x, y))
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if dx == 0 and dy == 0:
                        continue
                    if (x + dx + y + dy) % 4 == 0:
                        stamp(g, x + dx, y + dy, theme)
    for x in range(3, w - 3):
        if x % 2 == 0:
            stamp(g, x, cy - 5, theme)
            stamp(g, x, cy + 5, theme)
    for y in range(3, h - 3):
        if y % 3 == 0:
            stamp(g, cx - 7, y, theme)
            stamp(g, cx + 7, y, theme)
    for x in range(3, w - 3):
        stamp(g, x, cy, ".")
    for y in range(3, h - 3):
        stamp(g, cx, y, ".")
    rect_outline_wide_gates(g, cx - 4, cy - 3, cx + 4, cy + 3, "w", cx, cy, gate=3)
    dynamic_lanes_in_room(g, cx - 2, cy - 1, cx + 2, cy + 1, leave_border=0)
    return _finish_layout(23, g, cx, cy)



def hazard_defs(level_id: int, w: int, h: int, cx: int, cy: int) -> List[dict]:
    hazards: List[dict] = [
        {"type": "laser_h", "y": cy - 1, "x1": 4, "x2": w - 5, "id": f"l{level_id}_h_main"},
    ]
    if level_id >= 15:
        hazards.append({"type": "laser_v", "x": cx, "y1": 4, "y2": h - 5, "id": f"l{level_id}_v_main"})
    if level_id >= 15:
        hazards.append({"type": "laser_h", "y": cy + 2, "x1": 6, "x2": w - 7, "id": f"l{level_id}_h_sub"})
    if level_id >= 16:
        hazards.append({"type": "laser_v", "x": cx - 5, "y1": 5, "y2": h - 6, "id": f"l{level_id}_v_west"})
        hazards.append({"type": "laser_v", "x": cx + 5, "y1": 5, "y2": h - 6, "id": f"l{level_id}_v_east"})
    if level_id >= 18:
        hazards.append({"type": "laser_h", "y": cy - 4, "x1": 5, "x2": w - 6, "id": f"l{level_id}_h_north"})
        hazards.append({"type": "laser_h", "y": cy + 4, "x1": 5, "x2": w - 6, "id": f"l{level_id}_h_south"})
    if level_id >= 20:
        hazards.append({"type": "laser_v", "x": cx - 7, "y1": 4, "y2": h - 5, "id": f"l{level_id}_v_far_w"})
        hazards.append({"type": "laser_v", "x": cx + 7, "y1": 4, "y2": h - 5, "id": f"l{level_id}_v_far_e"})
    if level_id >= 22:
        hazards.append({"type": "laser_h", "y": 3, "x1": 3, "x2": w - 4, "id": f"l{level_id}_h_rim"})
        hazards.append({"type": "laser_h", "y": h - 4, "x1": 3, "x2": w - 4, "id": f"l{level_id}_h_rim_s"})
    if level_id >= 23:
        hazards.append({"type": "laser_v", "x": 3, "y1": 3, "y2": h - 4, "id": f"l{level_id}_v_rim"})
        hazards.append({"type": "laser_v", "x": w - 4, "y1": 3, "y2": h - 4, "id": f"l{level_id}_v_rim_e"})
    return hazards


def spawner_count(level_id: int) -> int:
    return 6 if level_id >= 19 else 5


def spawner_interval(level_id: int) -> int:
    return max(360, 900 - (level_id - 1) * 28)


def item_pools(level_id: int) -> List[List[str]]:
    if level_id < 19:
        return [
            ["frost", "charge", "double"],
            ["magnet", "shield", "charge"],
            ["speed", "double", "frost"],
            ["shield", "charge", "magnet"],
            ["frost", "double", "speed", "charge"],
        ]
    return [
        ["frost", "charge", "double", "shield"],
        ["magnet", "shield", "charge", "speed"],
        ["speed", "double", "frost", "magnet"],
        ["shield", "charge", "magnet", "double"],
        ["frost", "double", "speed", "charge"],
        ["magnet", "frost", "shield", "charge", "double"],
    ]


def spawner_positions(level_id: int, w: int, h: int, cx: int, cy: int) -> List[Tuple[int, int]]:
    """工厂放在闸道舱内（可进入区域）。"""
    count = spawner_count(level_id)
    wing_y = cy
    positions = [
        (cx - 4, wing_y),
        (cx + 4, wing_y),
        (cx - 4, cy - 2),
        (cx + 4, cy + 2),
        (1, cy),
        (w - 2, cy),
        (cx, 2),
        (cx, h - 3),
    ]
    if level_id >= 17:
        positions.extend([(cx, cy - 4), (cx, cy + 4)])
    picked: List[Tuple[int, int]] = []
    for p in positions:
        if len(picked) >= count:
            break
        if p not in picked:
            picked.append(p)
    return picked[:count]


def ghost_spawns(level_id: int, w: int, h: int, cx: int) -> List[List[int]]:
    if level_id == 14:
        return [[cx - 1, 4], [cx, 4], [cx + 1, 4]]
    n = 4 + min(2, (level_id - 14) // 3)
    base_y = 3 + (level_id % 3)
    xs = [cx - 2, cx - 1, cx, cx + 1, cx + 2, cx + 3][:n]
    return [[x, base_y] for x in xs]


def make_level(level_id: int) -> dict:
    w, h, _, name, _ = LEVELS[level_id]
    grid, cx, cy = build_grid(level_id)
    pac = [cx, h - 3]
    reserved = {tuple(pac)}
    for gx, gy in ghost_spawns(level_id, w, h, cx):
        reserved.add((gx, gy))

    g = [list(r) for r in grid]
    for x, y in reserved:
        stamp(g, x, y, ".")
    grid = heal_sealed_regions(g, tuple(pac), w, h)

    validate_enterable(grid, tuple(pac))
    if not ok_grid(grid, tuple(pac)):
        raise ValueError(f"L{level_id} too many isolated floor tiles")

    hazards = hazard_defs(level_id, w, h, cx, cy)
    pools = item_pools(level_id)
    interval = spawner_interval(level_id)
    positions = spawner_positions(level_id, w, h, cx, cy)

    item_spawners = []
    for i, (sx, sy) in enumerate(positions):
        item_spawners.append(
            {
                "id": f"factory_{level_id:02d}_{i:02d}",
                "x": sx,
                "y": sy,
                "intervalTicks": interval,
                "pool": pools[i % len(pools)],
            }
        )

    base_score = 1000 + level_id * 120
    required_tags = [f"L{level_id}-W", f"L{level_id}-E", f"L{level_id}-CORE"] if level_id >= 14 else []
    star = {
        "twoStarMinScore": base_score + 400,
        "threeStarMinScore": base_score + 1200,
        "threeStarMaxSeconds": 100 + level_id * 8,
        "threeStarNoDeath": level_id >= 16,
    }
    if required_tags:
        star["threeStarRequiredTags"] = required_tags
    return {
        "id": level_id,
        "name": name,
        "width": w,
        "height": h,
        "grid": grid,
        "spawn": {"pac": pac, "ghosts": ghost_spawns(level_id, w, h, cx)},
        "markers": [
            {"type": "start", "x": pac[0], "y": pac[1]},
            {"type": "checkpoint", "x": cx - 4, "y": cy, "label": "西闸", "tag": f"L{level_id}-W"},
            {"type": "checkpoint", "x": cx + 4, "y": cy, "label": "东闸", "tag": f"L{level_id}-E"},
            {"type": "checkpoint", "x": cx, "y": cy, "label": "核心", "tag": f"L{level_id}-CORE"},
        ],
        "hazards": hazards,
        "difficulty": {
            "ghost_speed_mul": round(0.42 + (level_id - 1) * 0.035, 2),
            "ai_aggression": round(min(1.0, 0.45 + (level_id - 1) * 0.032), 2),
        },
        "itemSpawners": item_spawners,
        "starCriteria": star,
    }


def write_all():
    prev = 0
    for lid in range(14, 24):
        data = make_level(lid)
        w, h = data["width"], data["height"]
        hz = len(data["hazards"])
        cx_score = complexity(lid, w, h, hz, data["grid"])
        if prev and cx_score < prev:
            raise ValueError(f"L{lid} complexity {cx_score} < prev {prev}")
        path = OUT / f"level_{lid:03d}.json"
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        dyn = len(dynamic_tiles(data["grid"]))
        print(f"Wrote {path.name} cx={cx_score} hazards={hz} dynamic={dyn}")
        prev = cx_score


if __name__ == "__main__":
    write_all()
