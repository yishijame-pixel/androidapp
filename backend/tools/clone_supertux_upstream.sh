#!/usr/bin/env bash
# Clone SuperTux upstream at pin commit into reference-assets/supertux (for CI / local bootstrap).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
UPSTREAM="${UPSTREAM_ROOT:-$REPO_ROOT/reference-assets/supertux}"
PIN_FILE="${PIN_FILE:-$REPO_ROOT/engine/supertux-fork/source_pin.json}"

if [[ ! -f "$PIN_FILE" ]]; then
  echo "Missing $PIN_FILE" >&2
  exit 1
fi

COMMIT="$(python3 -c "import json; print(json.load(open('$PIN_FILE'))['commit'])")"
if [[ -z "$COMMIT" || "$COMMIT" == "None" ]]; then
  echo "Invalid commit in $PIN_FILE" >&2
  exit 1
fi

mkdir -p "$(dirname "$UPSTREAM")"
if [[ -d "$UPSTREAM/.git" ]]; then
  echo "Updating existing clone at $UPSTREAM"
  git -C "$UPSTREAM" fetch --depth 1 origin "$COMMIT"
  git -C "$UPSTREAM" checkout -q FETCH_HEAD
else
  rm -rf "$UPSTREAM"
  echo "Cloning SuperTux @ $COMMIT -> $UPSTREAM"
  git init "$UPSTREAM"
  git -C "$UPSTREAM" remote add origin https://github.com/SuperTux/supertux.git
  git -C "$UPSTREAM" fetch --depth 1 origin "$COMMIT"
  git -C "$UPSTREAM" checkout -q FETCH_HEAD
fi

git -C "$UPSTREAM" submodule update --init --recursive --depth 1 || true
echo "OK upstream $(git -C "$UPSTREAM" rev-parse --short HEAD)"
