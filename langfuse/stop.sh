#!/usr/bin/env bash
# Stop the Langfuse stack started by ./start-demo.sh.
# By default volumes persist (trace data, project keys, and migrations stick around).
# Pass --wipe to also delete volumes (you'll re-run init on next start).
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

if [[ "${1:-}" == "--wipe" ]]; then
  compose_cmd -f "${HERE}/docker-compose.yml" down -v --remove-orphans
else
  compose_cmd -f "${HERE}/docker-compose.yml" down --remove-orphans
fi
