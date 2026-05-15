#!/usr/bin/env bash
set -euo pipefail

SUITE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SUITE_DIR}"

# Garak 0.15+ needs Python 3.10+.
find_python() {
  local candidate
  for candidate in python3.13 python3.12 python3.11 python3.10; do
    if command -v "${candidate}" >/dev/null 2>&1; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done
  if command -v python3 >/dev/null 2>&1; then
    if python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 10) else 1)' 2>/dev/null; then
      printf '%s\n' python3
      return 0
    fi
  fi
  return 1
}

if ! PYTHON_BIN="$(find_python)"; then
  cat >&2 <<'EOF'
ERROR: No Python 3.10+ interpreter found on PATH.
Install one and re-run:
  brew install python@3.12
EOF
  exit 1
fi

if [[ -d ".venv" ]]; then
  if ! .venv/bin/python -c 'import sys; sys.exit(0 if sys.version_info >= (3, 10) else 1)' 2>/dev/null; then
    rm -rf .venv
  fi
fi

if [[ ! -d ".venv" ]]; then
  "${PYTHON_BIN}" -m venv .venv
fi

# shellcheck disable=SC1091
source .venv/bin/activate
pip install -q --upgrade pip
pip install -q -r requirements.txt

# Keep Garak runtime config inside the workspace.
export XDG_CONFIG_HOME="${XDG_CONFIG_HOME:-${SUITE_DIR}/.config}"
export XDG_DATA_HOME="${XDG_DATA_HOME:-${SUITE_DIR}/.local/share}"
export XDG_CACHE_HOME="${XDG_CACHE_HOME:-${SUITE_DIR}/.cache}"

# Keep the scope intentionally small for a POC.
PROBES="${GARAK_PROBES:-dan.Dan_11_0}"
GENERATIONS="${GARAK_GENERATIONS:-1}"
PARALLEL_ATTEMPTS="${GARAK_PARALLEL_ATTEMPTS:-8}"

exec python -m garak \
  --target_type rest \
  --generator_option_file generator_options.json \
  --probes "${PROBES}" \
  --generations "${GENERATIONS}" \
  --parallel_attempts "${PARALLEL_ATTEMPTS}" \
  "$@"
