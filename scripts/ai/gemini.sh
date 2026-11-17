#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
bin="$root/tools/ai-node/node_modules/.bin/gemini"
[[ -x "$bin" ]] || { echo 'Run npm ci --prefix tools/ai-node first.' >&2; exit 1; }
cd "$root"
exec "$bin" "$@"
