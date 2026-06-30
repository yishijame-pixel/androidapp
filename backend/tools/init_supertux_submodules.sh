#!/usr/bin/env bash
# Ensure SuperTux upstream and nested submodules (simplesquirrel/libs/squirrel) are populated.
set -euo pipefail

UPSTREAM="${1:?Usage: init_supertux_submodules.sh UPSTREAM_DIR}"

if [[ ! -d "$UPSTREAM/.git" ]]; then
  echo "Not a git checkout: $UPSTREAM" >&2
  exit 1
fi

git -C "$UPSTREAM" submodule sync --recursive
git -C "$UPSTREAM" submodule update --init --recursive --depth 1 --force

test -f "$UPSTREAM/external/simplesquirrel/CMakeLists.txt"
test -f "$UPSTREAM/external/simplesquirrel/libs/squirrel/CMakeLists.txt"

sq_count="$(find "$UPSTREAM/external/simplesquirrel/libs/squirrel" -type f | wc -l | tr -d ' ')"
if [[ "${sq_count:-0}" -lt 20 ]]; then
  echo "Nested squirrel submodule empty ($sq_count files) under $UPSTREAM" >&2
  exit 1
fi

echo "Submodules OK under $UPSTREAM ($sq_count squirrel files)"
