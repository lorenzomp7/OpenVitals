#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
npm --prefix web run build
exec python3 -m http.server "${1:-8765}" --bind 127.0.0.1 --directory web/dist
