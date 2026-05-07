#!/usr/bin/env bash
# Convenience wrapper: sets defaults, then runs `npx promptfoo eval`.
# Pass any promptfoo flag through, e.g.:
#   ./run.sh --filter-pattern memory
#   ./run.sh --no-cache
set -euo pipefail

cd "$(dirname "$0")"

# --- prerequisites -----------------------------------------------------------
if ! command -v npx >/dev/null 2>&1; then
  echo "ERROR: npx not found. Install Node.js 20+ (e.g. 'brew install node')." >&2
  exit 1
fi

# --- defaults (override via env) --------------------------------------------
export SPRING_BASE_URL="${SPRING_BASE_URL:-http://localhost:8080}"
export SPRING_USER="${SPRING_USER:-user}"
export SPRING_PASSWORD="${SPRING_PASSWORD:-demo}"
export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://127.0.0.1:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-llama3.2:1b}"

# Bypass corporate HTTP proxy for localhost — promptfoo's undici fetch
# otherwise routes /ai/generate and /v1 (Ollama) through HTTP(S)_PROXY
# and times out trying to reach localhost via the corporate proxy.
export NO_PROXY="${NO_PROXY:-localhost,127.0.0.1}"
export no_proxy="${no_proxy:-localhost,127.0.0.1}"

# --- pre-flight: SUT reachable? ---------------------------------------------
http_status=$(curl -s -o /dev/null -w '%{http_code}' \
  -u "${SPRING_USER}:${SPRING_PASSWORD}" \
  "${SPRING_BASE_URL}/ai/generate?message=ping&chatId=_pf_health" || true)
if [[ "${http_status}" != "200" ]]; then
  echo "ERROR: ${SPRING_BASE_URL}/ai/generate not reachable (HTTP ${http_status})." >&2
  echo "Start the SUT first: ./start-demo.sh from the repo root." >&2
  exit 1
fi

# --- run ---------------------------------------------------------------------
mkdir -p report
npx --yes promptfoo@latest eval \
  --config promptfooconfig.yaml \
  --output report/last.html \
  --output report/last.json \
  "$@"

echo
echo "Report: $(pwd)/report/last.html"
