#!/usr/bin/env bash
# Bootstrap SDL android-project into engine/supertux-fork/mk/android.
#
# Upstream tools/bootstrap-android-project.sh uses `git rev-parse --show-toplevel`,
# which resolves to the FunLife repo root in CI — not engine/supertux-fork.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FORK_ROOT="${FORK_ROOT:-$REPO_ROOT/engine/supertux-fork}"
SDL_TAG="${1:?Usage: bootstrap_supertux_android.sh SDL_TAG (e.g. 2.32.10)}"

BOOTSTRAP="$FORK_ROOT/tools/bootstrap-android-project.sh"
if [[ ! -f "$BOOTSTRAP" ]]; then
  echo "Missing $BOOTSTRAP — run bootstrap_supertux_fork first" >&2
  exit 1
fi

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
sed "s|^ROOT=\$(git rev-parse --show-toplevel)|ROOT=\"$FORK_ROOT\"|" "$BOOTSTRAP" > "$tmp"
chmod +x "$tmp"
"$tmp" "$SDL_TAG"

echo "Bootstrap OK -> $FORK_ROOT/mk/android"
