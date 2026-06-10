#!/usr/bin/env python3
"""Courtyard maps L11-L13 with material chars: b=青砖 w=木廊 t=瓦片 o=草地/池."""
import json
from pathlib import Path

OUT = Path(__file__).parent


def r(s: str, w: int) -> str:
    if len(s) != w:
        raise ValueError(f"len {len(s)} != {w}: {s!r}")
    return s


def save(d):
    w, h = d["width"], d["height"]
    for row in d["grid"]:
        r(row, w)
    assert len(d["grid"]) == h
    path = OUT / f"level_{d['id']:03d}.json"
    path.write_text(json.dumps(d, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("ok", path.name, f"{w}x{h}")


LEVELS = [
    {
        "id": 11,
        "name": "古风-四合院",
        "width": 21,
        "height": 17,
        "grid": [
            r("bbbbbbbbbbbbbbbbbbbbb", 21),
            r("b*.................*b", 21),
            r("b.bbbbbbbbbbbbbbbbb.b", 21),
            r("b.b...............b.b", 21),
            r("b.b.ttttttttttttt.b.b", 21),
            r("b.b.b...........b.b.b", 21),
            r("b.b.b.www...www.b.b.b", 21),
            r("b.b.b.b.......b.b.b.b", 21),
            r("b=..b.b.b.@..b.b.b.=b", 21),
            r("b.b.b.b.......b.b.b.b", 21),
            r("b.b.b.www...www.b.b.b", 21),
            r("b.b.b...........b.b.b", 21),
            r("b.b.ttttttttttttt.b.b", 21),
            r("b.b...............b.b", 21),
            r("b.bbbbbbbbbbbbbbbbb.b", 21),
            r("b*.................*b", 21),
            r("bbbbbbbbbbbbbbbbbbbbb", 21),
        ],
        "spawn": {"pac": [10, 14], "ghosts": [[8, 8], [9, 8], [10, 8], [11, 8], [12, 8]]},
        "markers": [
            {"type": "start", "x": 10, "y": 14},
            {"type": "checkpoint", "x": 1, "y": 8, "label": "东厢", "tag": "厢-东"},
            {"type": "checkpoint", "x": 19, "y": 8, "label": "西厢", "tag": "厢-西"},
        ],
        "difficulty": {"ghost_speed_mul": 0.75, "ai_aggression": 0.72},
    },
    {
        "id": 12,
        "name": "花园-江南园林",
        "width": 23,
        "height": 21,
        "grid": [
            r("bbbbbbbbbbbbbbbbbbbbbbb", 23),
            r("b*...................*b", 23),
            r("b.bbbbbbbbbbbbbbbbbbb.b", 23),
            r("b.b.................b.b", 23),
            r("b.b.wwww.......wwww.b.b", 23),
            r("b.b.w.............w.b.b", 23),
            r("b.b.w.www.....www.w.b.b", 23),
            r("b.b.w.w...........w.b.b", 23),
            r("b.b.w.w.ooooooooo.w.b.b", 23),
            r("b.b.w.w.ooooooooo.w.b.b", 23),
            r("b=..w.w.ooo@oooo.w.w.=b", 23),
            r("b.b.w.w.ooooooooo.w.b.b", 23),
            r("b.b.w.w.ooooooooo.w.b.b", 23),
            r("b.b.w.w...........w.b.b", 23),
            r("b.b.w.www.....www.w.b.b", 23),
            r("b.b.w.............w.b.b", 23),
            r("b.b.wwww.......wwww.b.b", 23),
            r("b.b.................b.b", 23),
            r("b.bbbbbbbbbbbbbbbbbbb.b", 23),
            r("b*...................*b", 23),
            r("bbbbbbbbbbbbbbbbbbbbbbb", 23),
        ],
        "spawn": {"pac": [11, 18], "ghosts": [[9, 10], [10, 10], [11, 10], [12, 10], [13, 10]]},
        "markers": [
            {"type": "start", "x": 11, "y": 18},
            {"type": "checkpoint", "x": 1, "y": 10, "label": "曲桥", "tag": "桥-1"},
            {"type": "checkpoint", "x": 21, "y": 10, "label": "亭台", "tag": "亭-1"},
        ],
        "hazards": [{"type": "laser_h", "y": 10, "x1": 5, "x2": 17, "id": "willow_mist"}],
        "difficulty": {"ghost_speed_mul": 0.65, "ai_aggression": 0.66},
    },
    {
        "id": 13,
        "name": "古风-回字院落",
        "width": 21,
        "height": 21,
        "grid": [
            r("bbbbbbbbbbbbbbbbbbbbb", 21),
            r("b*.................*b", 21),
            r("b.bbbbbbbbbbbbbbbbb.b", 21),
            r("b.b...............b.b", 21),
            r("b.b.bbbbbbbbbbbbb.b.b", 21),
            r("b.b.b...........b.b.b", 21),
            r("b.b.b.bbbbbbbbb.b.b.b", 21),
            r("b.b.b.b.......b.b.b.b", 21),
            r("b.b.b.b.wwwww.b.b.b.b", 21),
            r("b.b.b.b.w...w.b.b.b.b", 21),
            r("b=..b.b.b.w@w.b.b.b=.", 21),
            r("b.b.b.b.w...w.b.b.b.b", 21),
            r("b.b.b.b.wwwww.b.b.b.b", 21),
            r("b.b.b.b.......b.b.b.b", 21),
            r("b.b.b.btttttttt.b.b.b", 21),
            r("b.b.b...........b.b.b", 21),
            r("b.b.bbbbbbbbbbbbb.b.b", 21),
            r("b.b...............b.b", 21),
            r("b.bbbbbbbbbbbbbbbbb.b", 21),
            r("b*.................*b", 21),
            r("bbbbbbbbbbbbbbbbbbbbb", 21),
        ],
        "spawn": {"pac": [10, 19], "ghosts": [[8, 10], [9, 10], [10, 10], [11, 10], [12, 10]]},
        "markers": [
            {"type": "start", "x": 10, "y": 19},
            {"type": "checkpoint", "x": 1, "y": 10, "label": "外廊", "tag": "廊-外"},
            {"type": "checkpoint", "x": 19, "y": 10, "label": "内宅", "tag": "宅-内"},
        ],
        "hazards": [
            {"type": "laser_h", "y": 10, "x1": 4, "x2": 16, "id": "courtyard_scan"},
            {"type": "laser_v", "x": 10, "y1": 5, "y2": 15, "id": "gate_pillar"},
        ],
        "difficulty": {"ghost_speed_mul": 0.85, "ai_aggression": 0.82},
    },
]

if __name__ == "__main__":
    for lv in LEVELS:
        save(lv)
    print("done", len(LEVELS))
