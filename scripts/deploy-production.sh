#!/usr/bin/env bash

set -euo pipefail

EXPECTED_SHA="${1:-}"
RELEASE_SCOPE="${2:-}"
APP_DIR="/opt/eventflow"
DEPLOY_DIR="$APP_DIR/deploy"
COMPOSE=(docker compose --env-file "$DEPLOY_DIR/.env" -f "$DEPLOY_DIR/docker-compose.yml")

if [[ ! "$EXPECTED_SHA" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Expected a 40-character lowercase Git commit SHA." >&2
  exit 2
fi

if [[ ! "$RELEASE_SCOPE" =~ ^(frontend|backend|all)$ ]]; then
  echo "Release scope must be frontend, backend, or all." >&2
  exit 2
fi

deployment_failed() {
  local exit_code="$1"
  echo "Deployment failed. Current Compose status and recent logs follow." >&2
  "${COMPOSE[@]}" ps || true
  "${COMPOSE[@]}" logs --tail=200 backend frontend || true
  exit "$exit_code"
}

trap 'deployment_failed $?' ERR

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Required file is missing: $path" >&2
    exit 1
  fi
}

require_file "$DEPLOY_DIR/.env"
require_file "$DEPLOY_DIR/docker-compose.yml"

if [[ ! -d "$APP_DIR/.git" ]]; then
  echo "Application directory is not a Git checkout: $APP_DIR" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required for production deployment." >&2
  exit 1
fi

cd "$APP_DIR"

if [[ "$(git symbolic-ref --quiet --short HEAD)" != "main" ]]; then
  echo "Production checkout must stay on the main branch." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Production checkout has uncommitted changes. Resolve them before deployment." >&2
  exit 1
fi

git fetch --quiet origin main
REMOTE_SHA="$(git rev-parse origin/main)"
if [[ "$REMOTE_SHA" != "$EXPECTED_SHA" ]]; then
  echo "GitHub workflow revision does not match origin/main; refusing deployment." >&2
  echo "Expected: $EXPECTED_SHA" >&2
  echo "Remote:   $REMOTE_SHA" >&2
  exit 1
fi

git merge --ff-only "$EXPECTED_SHA"

cd "$DEPLOY_DIR"
"${COMPOSE[@]}" config --quiet

if [[ "$RELEASE_SCOPE" == "backend" || "$RELEASE_SCOPE" == "all" ]]; then
  ./backup.sh
fi

case "$RELEASE_SCOPE" in
  frontend)
    "${COMPOSE[@]}" build frontend
    "${COMPOSE[@]}" up -d --no-deps frontend
    ;;
  backend)
    "${COMPOSE[@]}" build backend
    "${COMPOSE[@]}" up -d --no-deps backend
    ;;
  all)
    "${COMPOSE[@]}" up -d --build
    ;;
esac

"${COMPOSE[@]}" ps

HTTP_PORT="$(awk -F= '$1 == "EVENTFLOW_HTTP_PORT" { print $2; exit }' .env | tr -d '\r')"
HTTP_PORT="${HTTP_PORT:-8083}"
if [[ ! "$HTTP_PORT" =~ ^[0-9]{1,5}$ ]]; then
  echo "EVENTFLOW_HTTP_PORT must be a valid port in deploy/.env." >&2
  exit 1
fi

health_code=""
for _ in {1..30}; do
  health_code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:${HTTP_PORT}/actuator/health" || true)"
  if [[ "$health_code" == "200" ]]; then
    break
  fi
  sleep 2
done

if [[ "$health_code" != "200" ]]; then
  echo "Health check expected HTTP 200 but received: ${health_code:-connection failure}" >&2
  exit 1
fi

echo "Deployment succeeded: $EXPECTED_SHA ($RELEASE_SCOPE)"
