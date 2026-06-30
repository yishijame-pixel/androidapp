"""Minimal S-expression parser for SuperTux .stl / .strf (subset)."""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any, Iterator, List, Optional, Union

Atom = Union[str, int, float, bool]
SExpr = Union[Atom, List["SExpr"]]


def _tokenize(text: str) -> Iterator[str]:
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c in "();":
            if c == ";":
                while i < n and text[i] != "\n":
                    i += 1
                continue
            yield c
            i += 1
            continue
        if c.isspace():
            i += 1
            continue
        if c == '"':
            i += 1
            start = i
            out = []
            while i < n:
                if text[i] == "\\" and i + 1 < n:
                    out.append(text[i + 1])
                    i += 2
                    continue
                if text[i] == '"':
                    break
                out.append(text[i])
                i += 1
            yield '"' + "".join(out) + '"'
            i += 1
            continue
        start = i
        while i < n and not text[i].isspace() and text[i] not in "();":
            i += 1
        yield text[start:i]


def parse(text: str) -> SExpr:
    tokens = list(_tokenize(text))
    pos = 0

    def parse_one() -> SExpr:
        nonlocal pos
        if pos >= len(tokens):
            raise ValueError("unexpected EOF")
        tok = tokens[pos]
        if tok == "(":
            pos += 1
            items: List[SExpr] = []
            while pos < len(tokens) and tokens[pos] != ")":
                items.append(parse_one())
            if pos >= len(tokens) or tokens[pos] != ")":
                raise ValueError("unclosed list")
            pos += 1
            return items
        if tok == ")":
            raise ValueError("unexpected )")
        pos += 1
        if tok.startswith('"') and tok.endswith('"'):
            return tok[1:-1]
        if tok == "#t":
            return True
        if tok == "#f":
            return False
        if re.fullmatch(r"-?\d+", tok):
            return int(tok)
        if re.fullmatch(r"-?\d+\.\d+", tok):
            return float(tok)
        return tok

    result = parse_one()
    return result


def sym(node: SExpr, default: str = "") -> str:
    if isinstance(node, str):
        return node
    return default


def find_blocks(tree: SExpr, name: str) -> List[List[SExpr]]:
    out: List[List[SExpr]] = []
    if not isinstance(tree, list) or not tree:
        return out
    if sym(tree[0]) == name:
        out.append(tree)
    for child in tree:
        if isinstance(child, list):
            out.extend(find_blocks(child, name))
    return out


def prop(block: List[SExpr], key: str) -> Optional[SExpr]:
    for i in range(1, len(block)):
        item = block[i]
        if isinstance(item, list) and item and sym(item[0]) == key:
            return item[1] if len(item) > 1 else None
    return None


def prop_int(block: List[SExpr], key: str, default: int = 0) -> int:
    v = prop(block, key)
    if isinstance(v, int):
        return v
    if isinstance(v, float):
        return int(v)
    return default


def prop_bool(block: List[SExpr], key: str, default: bool = False) -> bool:
    v = prop(block, key)
    if isinstance(v, bool):
        return v
    return default


def prop_atoms(block: List[SExpr], key: str) -> List[int]:
    for i in range(1, len(block)):
        item = block[i]
        if isinstance(item, list) and item and sym(item[0]) == key:
            vals: List[int] = []
            for v in item[1:]:
                if isinstance(v, int):
                    vals.append(v)
                elif isinstance(v, float):
                    vals.append(int(v))
            return vals
    return []
