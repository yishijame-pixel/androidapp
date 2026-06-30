#!/usr/bin/env bash
# Clone SuperTux upstream at pin commit into reference-assets/supertux (CI / local bootstrap).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
UPSTREAM="${UPSTREAM_ROOT:-$REPO_ROOT/reference-assets/supertux}"
PIN_FILE="${PIN_FILE:-$REPO_ROOT/engine/supertux-fork/source_pin.json}"

if [[ ! -f "$PIN_FILE" ]]; then
  echo "Missing $PIN_FILE" >&2
  exit 1
fi

COMMIT="$(python3 -c "import json, pathlib; print(json.loads(pathlib.Path('${PIN_FILE}').read_text())['commit'])")"
COMMIT="${COMMIT//[[:space:]]/}"
if [[ -z "$COMMIT" || "$COMMIT" == "None" ]]; then
  echo "Invalid commit in $PIN_FILE" >&2
  exit 1
fi

mkdir -p "$(dirname "$UPSTREAM")"
rm -rf "$UPSTREAM"

echo "Cloning SuperTux @ ${COMMIT} -> ${UPSTREAM}"
# --revision works on Git 2.36+ (ubuntu-latest); shallow single commit.
git clone --depth 1 --revision="${COMMIT}" \
  https://github.com/SuperTux/supertux.git "${UPSTREAM}"

git -C "${UPSTREAM}" submodule update --init --recursive --depth 1 || true

HEAD="$(git -C "${UPSTREAM}" rev-parse HEAD)"
if [[ "${HEAD}" != "${COMMIT}" ]]; then
  echo "Checkout mismatch: want ${COMMIT} got ${HEAD}" >&2
  exit 1
fi

echo "OK upstream $(git -C "${UPSTREAM}" rev-parse --short HEAD)"
