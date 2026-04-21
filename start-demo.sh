#!/usr/bin/env bash
# Brings up Redis + Ollama via docker compose, pulls models, then runs the app.
# On exit (normal, Ctrl+C, or signal): stops Spring Boot, removes Compose containers, and deletes project volumes (Ollama model cache, Redis data).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)" || exit 1
readonly REPO_ROOT
readonly REDIS_PORT="${REDIS_HOST_PORT:-6379}"
readonly OLLAMA_PORT="${OLLAMA_HOST_PORT:-11434}"
readonly OLLAMA_URL="${OLLAMA_BASE_URL:-http://127.0.0.1:${OLLAMA_PORT}}"
readonly CHAT_MODEL="${OLLAMA_MODEL:-llama3.2:1b}"
readonly EMBED_MODEL="${OLLAMA_EMBEDDING_MODEL:-nomic-embed-text}"

CLEANED=0
COMPOSE_STARTED=0
MVN_PID=""

log() {
  printf '%s\n' "$*"
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

# Called only via trap (EXIT/INT/TERM/HUP); static analysis does not see invocations.
# shellcheck disable=SC2317
cleanup() {
  if [[ "${CLEANED}" -eq 1 ]]; then
    return 0
  fi
  CLEANED=1
  set +e
  log ""
  log "Shutting down demo: stopping Spring Boot, then Docker Compose (containers + volumes)..."
  if [[ -n "${MVN_PID}" ]] && kill -0 "${MVN_PID}" 2>/dev/null; then
    kill -TERM "${MVN_PID}" 2>/dev/null
    sleep 2
    kill -KILL "${MVN_PID}" 2>/dev/null
  fi
  # DevTools or forked JVM may outlive mvnw; match this app's main class only.
  pkill -TERM -f "com.example.springailab.SpringAiLabApplication" 2>/dev/null
  sleep 1
  pkill -KILL -f "com.example.springailab.SpringAiLabApplication" 2>/dev/null
  if [[ "${COMPOSE_STARTED}" -eq 1 ]]; then
    (cd "${REPO_ROOT}" && compose_cmd down --remove-orphans) || true
  fi
  set -e
  log "Cleanup finished."
}

wait_for_redis() {
  local max_seconds="${1:-90}"
  local i=0
  while (( i < max_seconds )); do
    if compose_cmd exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
      return 0
    fi
    sleep 1
    i=$((i + 1))
  done
  fail "Redis (compose service 'redis') did not respond within ${max_seconds}s. Try: docker compose logs redis"
}

wait_for_http() {
  local url="$1"
  local label="$2"
  local max_seconds="${3:-120}"
  local i=0
  while (( i < max_seconds )); do
    if curl -sf --connect-timeout 2 "${url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
    i=$((i + 1))
  done
  fail "${label} did not become ready within ${max_seconds}s (${url})"
}

cd "${REPO_ROOT}"

command -v docker >/dev/null 2>&1 || fail "Docker is required (install from https://docs.docker.com/get-docker/)."
compose_cmd version >/dev/null 2>&1 || fail "Docker Compose plugin is required (docker compose)."

trap cleanup EXIT INT TERM HUP

if ! docker image inspect ollama/ollama:latest >/dev/null 2>&1; then
  log "ollama/ollama:latest not found locally — pulling once..."
  docker pull ollama/ollama:latest
fi

log "Starting Redis + Ollama (docker compose)..."
compose_cmd up -d
COMPOSE_STARTED=1

wait_for_redis 90

readonly tags_url="${OLLAMA_URL%/}/api/tags"
wait_for_http "${tags_url}" "Ollama API" 180

log "Pulling models (first run may take several minutes)..."
compose_cmd exec -T ollama ollama pull "${CHAT_MODEL}"
compose_cmd exec -T ollama ollama pull "${EMBED_MODEL}"

log ""
log "Starting spring-ai-lab (profile: dev, Maven profile: dev-ollama)..."
log "  OLLAMA_BASE_URL=${OLLAMA_URL}"
log "  OLLAMA_MODEL=${CHAT_MODEL}"
log "  OLLAMA_EMBEDDING_MODEL=${EMBED_MODEL}"
log "  Redis: 127.0.0.1:${REDIS_PORT}"
log "  Stop with Ctrl+C — containers and Compose volumes will be removed."
log ""

export OLLAMA_BASE_URL="${OLLAMA_URL}"
export OLLAMA_MODEL="${CHAT_MODEL}"
export OLLAMA_EMBEDDING_MODEL="${EMBED_MODEL}"
export SPRING_DATA_REDIS_HOST="${SPRING_DATA_REDIS_HOST:-127.0.0.1}"
export SPRING_DATA_REDIS_PORT="${REDIS_PORT}"

./mvnw -q -Pdev-ollama spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  "$@" &
MVN_PID=$!

MVN_EXIT=0
wait "${MVN_PID}" || MVN_EXIT=$?
exit "${MVN_EXIT}"
