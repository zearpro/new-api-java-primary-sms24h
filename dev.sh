#!/bin/bash

set -euo pipefail

echo "🚀 DEV MODE - Local Redis, API, RabbitMQ, Hono"
echo "API: http://localhost:80  |  Hono: http://localhost:3001"

ENV_FILE=.env.dev
if [ ! -f "$ENV_FILE" ]; then
  echo "⚠️  $ENV_FILE not found. Creating from .env.example"
  cp .env.example "$ENV_FILE" || true
fi

if ! docker ps >/dev/null 2>&1; then
  echo "❌ Docker is not running. Start Docker first."
  exit 1
fi

echo "📄 Loading $ENV_FILE ..."
set -o allexport
while IFS= read -r line; do
  [[ $line =~ ^[[:space:]]*# ]] && continue
  [[ -z "${line// }" ]] && continue
  if [[ $line =~ ^[[:space:]]*([^=]+)=(.*)$ ]]; then
    export "${BASH_REMATCH[1]}"="${BASH_REMATCH[2]}"
  fi
done < "$ENV_FILE"
set +o allexport

echo "🧹 Cleaning previous stack..."
docker compose -f docker-compose.dev.yml down --remove-orphans || true

echo "🔨 Building & starting dev stack (local Redis)..."
docker compose -f docker-compose.dev.yml up -d --build

echo "📊 Status:" 
docker compose -f docker-compose.dev.yml ps

echo "🧪 Health check:"
curl -fsS http://localhost:80/actuator/health || true
