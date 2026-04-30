#!/usr/bin/env bash
# Convenience runner. Assumes ./start-demo.sh is already running in another terminal
# (Spring app on :8080, Ollama on :11434).
set -euo pipefail

SUITE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SUITE_DIR}"

# DeepEval requires Python 3.10+ (uses PEP 604 `X | None` union syntax). macOS ships 3.9,
# so prefer an explicit pythonX.Y on PATH and only fall back to `python3` if it's recent enough.
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

DeepEval depends on PEP 604 union syntax (`X | None`), which is Python 3.10+.
macOS ships only 3.9 by default. Install one of these and re-run ./run.sh:

  brew install python@3.12        # Homebrew (recommended on macOS)
  # or use pyenv:
  pyenv install 3.12.6 && pyenv shell 3.12.6
EOF
  exit 1
fi

# If a previous run created a venv with the wrong Python (e.g. 3.9 from before this fix),
# rebuild it so we don't keep importing broken DeepEval bytecode.
if [[ -d ".venv" ]]; then
  if ! .venv/bin/python -c 'import sys; sys.exit(0 if sys.version_info >= (3, 10) else 1)' 2>/dev/null; then
    echo "Existing .venv uses an unsupported Python; recreating with ${PYTHON_BIN}..."
    rm -rf .venv
  fi
fi

if [[ ! -d ".venv" ]]; then
  echo "Creating virtualenv at deepeval/.venv (using ${PYTHON_BIN}) ..."
  "${PYTHON_BIN}" -m venv .venv
fi

# shellcheck disable=SC1091
source .venv/bin/activate
pip install -q --upgrade pip
pip install -q -r requirements.txt

# DeepEval otherwise prompts for a Confident AI login on first run.
export DEEPEVAL_TELEMETRY_OPT_OUT="${DEEPEVAL_TELEMETRY_OPT_OUT:-YES}"

exec pytest "$@"
