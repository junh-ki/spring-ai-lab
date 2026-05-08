#!/usr/bin/env bash
# regression-demo.sh — Demo 1: memory-advisor regression detection
#
# Runs three promptfoo evaluations in sequence to demonstrate that disabling
# the chat-memory advisor in the Spring AI app causes the multi-turn tests
# to fail, and that re-enabling it restores them.
#
# Phase layout:
#   1. Baseline (memory enabled)        → expects 4/4 PASS
#   2. Broken  (memory disabled)        → expects multi-turn checks to FAIL
#   3. Restored (memory enabled again)  → expects 4/4 PASS
#
# The script orchestrates the eval runs and pauses between phases for the
# operator to restart Spring AI with the appropriate environment variable.
# It does NOT touch Spring AI's lifecycle directly — that responsibility
# stays with start-demo.sh from the repo root.
#
# Usage:
#   ./regression-demo.sh
#
# Prerequisites:
#   - Spring AI is running (./start-demo.sh from repo root)
#   - Memory advisor is currently enabled (no APP_DEMO_MEMORY_DISABLED set)
#   - llama3.1:8b pulled in Ollama (judge model)
#   - Node 20+ in PATH (for promptfoo via npx)
set -euo pipefail

cd "$(dirname "$0")"

readonly TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
readonly REPORT_DIR="report/regression-${TIMESTAMP}"
readonly SPRING_BASE_URL="${SPRING_BASE_URL:-http://localhost:8080}"
readonly SPRING_USER="${SPRING_USER:-user}"
readonly SPRING_PASSWORD="${SPRING_PASSWORD:-demo}"

mkdir -p "${REPORT_DIR}"

# ----- pretty output ----------------------------------------------------------
log()   { printf '\n\033[1;36m▶ %s\033[0m\n' "$*"; }
warn()  { printf '\n\033[1;33m! %s\033[0m\n' "$*"; }
ok()    { printf '\033[1;32m✓ %s\033[0m\n' "$*"; }
err()   { printf '\033[1;31m✗ %s\033[0m\n' "$*"; }

# ----- helpers ----------------------------------------------------------------
require_sut_alive() {
  local status
  status=$(curl -s -o /dev/null -w '%{http_code}' \
    -u "${SPRING_USER}:${SPRING_PASSWORD}" \
    "${SPRING_BASE_URL}/ai/generate?message=ping&chatId=_regr_health" || true)
  if [[ "${status}" != "200" ]]; then
    err "Spring AI not reachable at ${SPRING_BASE_URL} (HTTP ${status})."
    echo "   Start it with ./start-demo.sh from the repo root, then re-run." >&2
    exit 1
  fi
  ok "Spring AI alive (HTTP 200)"
}

run_phase() {
  local label="$1"
  local outname="$2"
  log "Running promptfoo — ${label}"
  # promptfoo returns non-zero exit when assertions fail (correct CI behavior).
  # In this demo, Phase 2 *expects* failures, so we must not abort on non-zero.
  # `|| true` neutralises the exit code; the JSON report carries the real verdict.
  ./run.sh --filter-pattern memory -j 1 --no-cache || true
  cp report/last.html "${REPORT_DIR}/${outname}.html"
  cp report/last.json "${REPORT_DIR}/${outname}.json"
  ok "Saved ${REPORT_DIR}/${outname}.html"
}

pause_for_restart() {
  local title="$1"
  shift
  warn "${title}"
  printf '%s\n' "$@"
  read -r -p "   Press Enter once Spring AI has restarted and is ready..."
  sleep 2
  require_sut_alive
}

count_pass_fail() {
  local jsonfile="$1"
  if ! command -v jq >/dev/null 2>&1; then
    printf '(install jq for counts)'
    return
  fi
  local p f
  p=$(jq -r '.results.stats.successes // "?"' "${jsonfile}" 2>/dev/null)
  f=$(jq -r '.results.stats.failures  // "?"' "${jsonfile}" 2>/dev/null)
  printf '%s passed / %s failed' "${p}" "${f}"
}

# ----- phase 1: baseline ------------------------------------------------------
log "Phase 1 — Baseline (memory enabled)"
require_sut_alive
run_phase "baseline" "01-baseline"

# ----- phase 2: broken --------------------------------------------------------
pause_for_restart \
  "Phase 2 — Break it." \
  "" \
  "  Stop Spring AI (Ctrl+C in the start-demo.sh terminal) and relaunch with the flag:" \
  "" \
  "      APP_DEMO_MEMORY_DISABLED=true ./start-demo.sh" \
  "" \
  "  Wait until you see 'Started SpringAiLabApplication' and the warning:" \
  "" \
  "      WARN c.e.s.config.ChatClientConfig — Chat memory advisor DISABLED ..." \
  ""
run_phase "broken (memory disabled)" "02-broken"

# ----- phase 3: restored ------------------------------------------------------
pause_for_restart \
  "Phase 3 — Restore it." \
  "" \
  "  Stop Spring AI again and relaunch normally (without the flag):" \
  "" \
  "      ./start-demo.sh" \
  ""
run_phase "restored" "03-restored"

# ----- summary ----------------------------------------------------------------
log "Summary"
echo "  Phase 1 (baseline):  $(count_pass_fail "${REPORT_DIR}/01-baseline.json")"
echo "  Phase 2 (broken):    $(count_pass_fail "${REPORT_DIR}/02-broken.json")"
echo "  Phase 3 (restored):  $(count_pass_fail "${REPORT_DIR}/03-restored.json")"
echo
echo "Reports: ${REPORT_DIR}/"
echo "Open the side-by-side comparison in your browser, e.g."
echo "    open ${REPORT_DIR}/01-baseline.html"
echo "    open ${REPORT_DIR}/02-broken.html"
echo "    open ${REPORT_DIR}/03-restored.html"
