#!/usr/bin/env bash
# Fail fast if upstream submodules did not sync into engine/supertux-fork.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FORK="${FORK_ROOT:-$REPO_ROOT/engine/supertux-fork}"

required=(
  external/simplesquirrel/CMakeLists.txt
  external/simplesquirrel/libs/squirrel/CMakeLists.txt
  external/simplesquirrel/libs/squirrel/squirrel/CMakeLists.txt
  external/sexp-cpp/CMakeLists.txt
  external/SDL_ttf/CMakeLists.txt
  external/tinygettext/CMakeLists.txt
  CMakeLists.txt
  mk/android/app/build.gradle
  data/levels/world1/welcome_antarctica.stl
)

missing=0
for rel in "${required[@]}"; do
  if [[ ! -f "$FORK/$rel" ]]; then
    echo "MISSING: $FORK/$rel" >&2
    missing=1
  fi
done

sq_count="$(find "$FORK/external/simplesquirrel/libs/squirrel" -type f 2>/dev/null | wc -l | tr -d ' ')"
if [[ "${sq_count:-0}" -lt 20 ]]; then
  echo "simplesquirrel/libs/squirrel looks empty ($sq_count files)" >&2
  missing=1
fi

if [[ "$missing" -ne 0 ]]; then
  echo "Fork tree incomplete — run submodule update --init --recursive on upstream before bootstrap." >&2
  exit 1
fi

echo "Fork tree OK ($sq_count squirrel files)"
