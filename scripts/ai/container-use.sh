#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
bin="$root/.tools/bin/container-use"
if [[ "${1:-}" == install ]]; then
  go version >/dev/null 2>&1 || { echo 'Go with toolchain auto-download is required.' >&2; exit 1; }
  mkdir -p "$root/.tools/bin"
  GOBIN="$root/.tools/bin" go install github.com/dagger/container-use/cmd/container-use@2e43e625e95216b719ec9338f4034fd3a0be2734
  exit
fi
[[ -x "$bin" ]] || { echo 'Run bash scripts/ai/container-use.sh install first.' >&2; exit 1; }
cd "$root"
if [[ "${1:-}" == doctor ]]; then
  "$bin" version
  "$bin" config show --json
  command -v docker >/dev/null || { echo 'Docker is required for isolated environments.' >&2; exit 1; }
  docker info >/dev/null
  exit
fi
exec "$bin" "$@"
