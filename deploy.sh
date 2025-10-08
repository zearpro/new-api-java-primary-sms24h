#!/bin/bash

set -euo pipefail

echo "🚀 Store24h API - Production Deploy (Valkey TLS, no Dragonfly)"

# Guard against macOS local execution
OS_NAME=$(uname -s)
if [ "$OS_NAME" = "Darwin" ]; then
    echo "❌ This script is intended for Linux EC2 only."
    exit 1
fi

# Compose command resolver
if [ "$EUID" -eq 0 ]; then
    DOCKER_CMD="docker"
else
    DOCKER_CMD="sudo docker"
fi
if $DOCKER_CMD compose version >/dev/null 2>&1; then
    COMPOSE="$DOCKER_CMD compose"
else
    if command -v docker-compose >/dev/null 2>&1; then
        COMPOSE="docker-compose"
    else
        echo "❌ docker compose not found. Install Docker Compose V2."
        exit 1
    fi
fi

# Require .env
if [ ! -f .env ]; then
    echo "❌ .env not found. Create it with prod variables (REDIS_HOST, REDIS_PORT, REDIS_SSL, MYSQL_*, MONGO_URL, etc)."
    exit 1
fi

echo "📥 Pulling latest code..."
GIT_BRANCH=${GIT_BRANCH:-main}
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git fetch origin
    git checkout "$GIT_BRANCH" || true
    git pull origin "$GIT_BRANCH" || true
fi

# Load env safely
set -o allexport
while IFS= read -r line; do
  [[ $line =~ ^[[:space:]]*# ]] && continue
  [[ -z "${line// }" ]] && continue
  if [[ $line =~ ^[[:space:]]*([^=]+)=(.*)$ ]]; then
    export "${BASH_REMATCH[1]}"="${BASH_REMATCH[2]}"
  fi
done < .env
set +o allexport

# Build images
echo "🔨 Building images..."
$DOCKER_CMD build -t store24h-api .
$DOCKER_CMD build -t store24h-hono ./hono-accelerator

# Start stack (Valkey via env, no Dragonfly)
echo "🚀 Starting containers..."
$COMPOSE -f docker-compose.prod.yml up -d --no-deps store24h-api hono-accelerator rabbitmq

# Health check
echo "⏳ Waiting for health..."
PORT=${LISTEN_PORT:-80}
for i in {1..24}; do
  if curl -fsS "http://localhost:${PORT}/actuator/health" >/dev/null; then
    echo "✅ API healthy on port ${PORT}"
    break
  fi
  sleep 5
  echo "... still waiting"
  if [ "$i" -eq 24 ]; then
    echo "❌ Health check failed"
    $COMPOSE -f docker-compose.prod.yml logs --tail=100 store24h-api || true
    exit 1
  fi
done

echo "📊 Status:" 
$COMPOSE -f docker-compose.prod.yml ps

echo "🌐 API:  http://localhost:${PORT}"
echo "🧪 Health: http://localhost:${PORT}/actuator/health"

# Show Java application logs
echo ""
echo "📋 Java Application Logs (Last 50 lines):"
echo "=========================================="
$COMPOSE -f docker-compose.prod.yml logs --tail=50 store24h-api

echo ""
echo "🎉 Deployment Complete!"
echo "💡 To see live logs: $COMPOSE -f docker-compose.prod.yml logs -f store24h-api"
echo "💡 To see all logs: $COMPOSE -f docker-compose.prod.yml logs --tail=200 store24h-api"