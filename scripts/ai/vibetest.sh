#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
venv="$root/tools/vibetest/.venv"
command -v uv >/dev/null || { echo 'Install uv and Python 3.11-3.13 first.' >&2; exit 1; }
if [[ "${1:-}" == install ]]; then
  uv venv --python 3.12 --allow-existing "$venv" >&2
  uv pip sync --python "$venv/bin/python" "$root/tools/vibetest/requirements.lock" >&2
  "$venv/bin/python" -m playwright install chromium >&2
  exit
fi
[[ -x "$venv/bin/python" ]] || { echo 'Run bash scripts/ai/vibetest.sh install first.' >&2; exit 1; }
cd "$root"
exec "$venv/bin/python" "$root/scripts/ai/vibetest-server.py" "$@"
