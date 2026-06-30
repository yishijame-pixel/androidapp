#!/usr/bin/env bash
# Sync reference-assets/supertux -> engine/supertux-fork (preserve FunLife meta).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
UPSTREAM="${UPSTREAM_ROOT:-$REPO_ROOT/reference-assets/supertux}"
FORK="${FORK_ROOT:-$REPO_ROOT/engine/supertux-fork}"
PATCH_DIR="$REPO_ROOT/backend/tools/patches/supertux"

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

# Fail if nested submodules (e.g. simplesquirrel/libs/squirrel) were not copied.
bash "$(dirname "$0")/verify_supertux_fork_tree.sh"

if [[ -d "$PATCH_DIR" ]]; then
  shopt -s nullglob
  patches=("$PATCH_DIR"/*.patch)
  shopt -u nullglob
  if (( ${#patches[@]} > 0 )); then
    for patchfile in "${patches[@]}"; do
      echo "Applying patch $(basename "$patchfile")"
      patch -p1 -d "$FORK" --batch < "$patchfile"
    done
  fi
fi

echo "Bootstrap OK -> $FORK"
