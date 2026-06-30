#!/usr/bin/env bash
# Sync reference-assets/supertux -> engine/supertux-fork (preserve FunLife meta).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
UPSTREAM="${UPSTREAM_ROOT:-$REPO_ROOT/reference-assets/supertux}"
FORK="${FORK_ROOT:-$REPO_ROOT/engine/supertux-fork}"

if [[ ! -d "$UPSTREAM" ]]; then
  echo "Missing upstream at $UPSTREAM" >&2
  exit 1
fi

mkdir -p "$FORK/patches"
rsync -a --delete \
  --exclude patches \
  --exclude NOTICE \
  --exclude source_pin.json \
  --exclude FUNLIFE.md \
  --exclude .gitignore \
  "$UPSTREAM/" "$FORK/"

echo "Bootstrap OK -> $FORK"
