#!/bin/bash

# Production deployment script for AWS EC2 with Dragonfly Performance Edition
# This script builds and deploys the Store24h API with DragonflyDB and Dashboard
# FORCES 100% FRESH BUILD - Deletes all images and rebuilds from scratch

set -e

echo "🚀 Starting Store24h API deployment with Dragonfly Performance Edition on EC2..."

# Guard: never run on macOS (local dev). This script is for Ubuntu EC2 only.
OS_NAME=$(uname -s)
if [ "$OS_NAME" = "Darwin" ]; then
    echo "❌ This deployment script must not be run on macOS. It is intended for Ubuntu EC2 only."
    echo "🛑 Aborting."
    exit 1
fi

# Pull latest code from git repository
echo "📥 Pulling latest code from git repository..."
git pull origin main

# Determine Docker and Compose commands (prefer Compose V2)
# Always use sudo unless running as root
if [ "$EUID" -eq 0 ]; then
    DOCKER_CMD="docker"
else
    DOCKER_CMD="sudo docker"
fi

# Prefer Docker Compose V2: `docker compose`
if $DOCKER_CMD compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD="$DOCKER_CMD compose"
else
    # Fallback to legacy docker-compose binary
    if [ "$EUID" -eq 0 ]; then
        DOCKER_COMPOSE_CMD="docker-compose"
    else
        DOCKER_COMPOSE_CMD="sudo docker-compose"
    fi
fi

# Check if .env file exists
if [ ! -f .env ]; then
    echo "❌ Error: .env file not found!"
    echo "📝 Please create .env file with your production environment variables"
    echo "📋 You can copy from .env.example and update the values"
    exit 1
fi

# Validate Docker is installed and running
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! $DOCKER_CMD ps &> /dev/null; then
    echo "❌ Docker daemon is not running or permission denied."
    echo "💡 Try: sudo systemctl start docker"
    exit 1
fi

# Validate docker compose is available (v2 or v1)
if ! $DOCKER_COMPOSE_CMD version >/dev/null 2>&1; then
    echo "❌ Docker Compose is not available. Install Docker Compose V2 (docker compose) or legacy docker-compose."
    exit 1
fi

# Load environment variables safely (excluding problematic ones)
echo "📄 Loading environment variables..."
set -o allexport
source <(grep -v '^#' .env | grep -v '^JAVA_OPTS' | grep -v '^\s*$' | sed 's/[[:space:]]*=[[:space:]]*/=/g')
set +o allexport

# Control infra rebuild (dragonfly/rabbitmq) via env flag
REBUILD_INFRA=${REBUILD_INFRA:-false}

# Do NOT stop Dragonfly/RabbitMQ by default. Only rebuild app services.
echo "🛑 Stopping/recreating only application services (store24h-api, hono-accelerator, dashboard)..."
$DOCKER_COMPOSE_CMD stop store24h-api hono-accelerator dashboard || true

# FORCE DELETE ALL APPLICATION IMAGES - 100% Fresh Build
echo "🗑️ FORCE DELETING all application images..."
$DOCKER_CMD images | grep -E "(store24h|api-ecr-extracted|dashboard)" | awk '{print $3}' | xargs -r $DOCKER_CMD rmi -f || true

# FORCE DELETE ALL IMAGES WITH SAME NAME PATTERN
echo "🗑️ FORCE DELETING images by name pattern..."
$DOCKER_CMD images | grep -E "(api-ecr-extracted|store24h-api|store24h-dashboard)" | awk '{print $1":"$2}' | xargs -r $DOCKER_CMD rmi -f || true

# Clean up ALL unused images and build cache
echo "🧹 AGGRESSIVE cleanup of ALL Docker images and build cache..."
$DOCKER_CMD system prune -a -f || true
$DOCKER_CMD builder prune -a -f || true

# Remove any existing target directory to force fresh JAR build
echo "🗑️ Removing existing target directory to force fresh JAR build..."
rm -rf target/ || true

# Build application images with FORCE NO-CACHE (does not affect dragonfly/rabbitmq)
echo "🔨 FORCE BUILDING application images from scratch (100% fresh)..."
echo "⏱️ Setting build timeout to 30 minutes to prevent context cancellation..."

# Disable BuildKit completely to avoid bake issues on EC2
export DOCKER_BUILDKIT=0
export COMPOSE_DOCKER_CLI_BUILD=0
unset BUILDKIT_PROGRESS

# Build store24h-api using direct docker build (bypass compose bake)
echo "🔨 Building store24h-api using direct docker build..."
BUILD_ATTEMPTS=0
MAX_ATTEMPTS=3

while [ $BUILD_ATTEMPTS -lt $MAX_ATTEMPTS ]; do
    BUILD_ATTEMPTS=$((BUILD_ATTEMPTS + 1))
    echo "🔄 Build attempt $BUILD_ATTEMPTS/$MAX_ATTEMPTS..."
    
    # Use direct docker build instead of compose build
    if timeout 1800 $DOCKER_CMD build --no-cache --pull -t store24h-api .; then
        echo "✅ store24h-api built successfully!"
        break
    else
        echo "❌ Build attempt $BUILD_ATTEMPTS failed"
        if [ $BUILD_ATTEMPTS -lt $MAX_ATTEMPTS ]; then
            echo "🧹 Cleaning up and retrying..."
            $DOCKER_CMD system prune -f || true
            $DOCKER_CMD builder prune -f || true
            sleep 10
        else
            echo "❌ All build attempts failed. Exiting."
            exit 1
        fi
    fi
done

# Build dashboard using direct docker build
echo "🔨 Building dashboard using direct docker build..."
cd dashboard
timeout 600 $DOCKER_CMD build --no-cache --pull -t store24h-dashboard .
cd ..

# Build hono-accelerator using direct docker build
echo "🔨 Building hono-accelerator using direct docker build..."
cd hono-accelerator
timeout 600 $DOCKER_CMD build --no-cache --pull -t hono-accelerator .
cd ..

# Ensure Dragonfly and RabbitMQ are running (start if not running)
echo "🔧 Ensuring Dragonfly and RabbitMQ are running..."
$DOCKER_COMPOSE_CMD up -d dragonfly rabbitmq || true

# Optionally refresh infra if explicitly requested
if [ "$REBUILD_INFRA" = "true" ]; then
    echo "🏗️ REBUILD_INFRA=true -> Rebuilding Dragonfly and RabbitMQ..."
    $DOCKER_COMPOSE_CMD pull dragonfly rabbitmq || true
    $DOCKER_COMPOSE_CMD up -d --force-recreate dragonfly rabbitmq || true
else
    echo "ℹ️ Dragonfly/RabbitMQ are running (preserving data). Set REBUILD_INFRA=true to rebuild."
fi

# Start/recreate application services
echo "🚀 Starting application services..."
$DOCKER_COMPOSE_CMD --env-file .env up -d --no-deps store24h-api hono-accelerator dashboard

# Wait for the application to start with better health checking
echo "⏳ Waiting for application to start..."
MAX_WAIT=120
WAIT_TIME=0
PORT=${LISTEN_PORT:-80}

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    if $DOCKER_COMPOSE_CMD ps store24h-api | grep -q "Up"; then
        # Check if the health endpoint is responding
        if curl -f -s "http://localhost:$PORT/actuator/health" > /dev/null 2>&1; then
            echo "✅ Application is running and healthy!"
            break
        fi
    fi

    echo "⏳ Still waiting... ($WAIT_TIME/$MAX_WAIT seconds)"
    sleep 10
    WAIT_TIME=$((WAIT_TIME + 10))
done

# Check dashboard health
echo "📊 Checking dashboard health..."
if $DOCKER_COMPOSE_CMD ps dashboard | grep -q "Up"; then
    if curl -f -s "http://localhost:3000" > /dev/null 2>&1; then
        echo "✅ Dashboard is running and accessible!"
    else
        echo "⚠️ Dashboard container is up but not responding on port 3000"
    fi
else
    echo "⚠️ Dashboard container is not running"
fi

# Final status check
if $DOCKER_COMPOSE_CMD ps store24h-api | grep -q "Up"; then
    echo "✅ Application containers are running!"
    echo "🌐 Application URL: http://localhost:$PORT"
    echo "📊 Dashboard URL: http://localhost:3000"
    echo "⚡ Accelerator URL: http://localhost:3001"
    echo "🔍 Health check: http://localhost:$PORT/actuator/health"
    echo "🔥 Warmup status: http://localhost:$PORT/api/warmup/status"
    echo ""
    echo "📊 Container status:"
    $DOCKER_COMPOSE_CMD ps store24h-api hono-accelerator dashboard dragonfly rabbitmq || $DOCKER_COMPOSE_CMD ps
else
    echo "❌ Application failed to start. Checking logs..."
    echo "📋 Container logs:"
    $DOCKER_COMPOSE_CMD logs --tail=50 store24h-api hono-accelerator dashboard || $DOCKER_COMPOSE_CMD logs --tail=50
    echo ""
    echo "🔍 Container status:"
    $DOCKER_COMPOSE_CMD ps -a
    exit 1
fi

echo "✨ Deployment completed successfully!"
echo "🎯 Dragonfly Performance Edition is now running!"
echo "💡 To view logs: $DOCKER_COMPOSE_CMD logs -f"
echo "💡 To stop: $DOCKER_COMPOSE_CMD down"
echo "💡 To access dashboard: http://localhost:3000"