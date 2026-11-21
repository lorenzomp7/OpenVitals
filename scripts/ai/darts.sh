#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
venv="$root/tools/darts/.venv"
if [[ "${1:-}" == install ]]; then
  command -v uv >/dev/null || { echo 'Install uv first.' >&2; exit 1; }
  uv venv --python 3.12 --allow-existing "$venv" >&2
  uv pip sync --python "$venv/bin/python" "$root/tools/darts/requirements.lock" >&2
  exit
fi
[[ -x "$venv/bin/python" ]] || { echo 'Run bash scripts/ai/darts.sh install first.' >&2; exit 1; }
exec "$venv/bin/python" "$root/tools/darts/forecast.py" "$@"
