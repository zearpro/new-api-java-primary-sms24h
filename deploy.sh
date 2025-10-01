#!/bin/bash

# Production deployment script for AWS EC2
# This script builds and deploys the Store24h API
# FORCES 100% FRESH BUILD - Deletes all images and rebuilds from scratch

set -e

echo "🚀 Starting Store24h API deployment on EC2..."

# Check if running as root or with sudo
if [ "$EUID" -eq 0 ]; then
    DOCKER_CMD="docker"
    DOCKER_COMPOSE_CMD="docker-compose"
else
    DOCKER_CMD="sudo docker"
    DOCKER_COMPOSE_CMD="sudo docker-compose"
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

# Validate docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose is not installed. Please install docker-compose first."
    exit 1
fi

# Load environment variables safely (excluding problematic ones)
echo "📄 Loading environment variables..."
set -o allexport
source <(grep -v '^#' .env | grep -v '^JAVA_OPTS' | grep -v '^\s*$' | sed 's/[[:space:]]*=[[:space:]]*/=/g')
set +o allexport

# Stop existing containers if running
echo "🛑 Stopping existing containers..."
$DOCKER_COMPOSE_CMD down --remove-orphans || true

# FORCE DELETE ALL JAVA IMAGES - 100% Fresh Build
echo "🗑️ FORCE DELETING all Java application images..."
$DOCKER_CMD images | grep -E "(store24h|api-ecr-extracted)" | awk '{print $3}' | xargs -r $DOCKER_CMD rmi -f || true

# FORCE DELETE ALL IMAGES WITH SAME NAME PATTERN
echo "🗑️ FORCE DELETING images by name pattern..."
$DOCKER_CMD images | grep -E "(api-ecr-extracted|store24h-api)" | awk '{print $1":"$2}' | xargs -r $DOCKER_CMD rmi -f || true

# Clean up ALL unused images and build cache
echo "🧹 AGGRESSIVE cleanup of ALL Docker images and build cache..."
$DOCKER_CMD system prune -a -f || true
$DOCKER_CMD builder prune -a -f || true

# Remove any existing target directory to force fresh JAR build
echo "🗑️ Removing existing target directory to force fresh JAR build..."
rm -rf target/ || true

# Build the Docker image with FORCE NO-CACHE
echo "🔨 FORCE BUILDING Docker image from scratch (100% fresh)..."
$DOCKER_COMPOSE_CMD build --no-cache --pull

# Start the application
echo "🚀 Starting the application..."
$DOCKER_COMPOSE_CMD --env-file .env up -d

# Wait for the application to start with better health checking
echo "⏳ Waiting for application to start..."
MAX_WAIT=120
WAIT_TIME=0
PORT=${LISTEN_PORT:-80}

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    if $DOCKER_COMPOSE_CMD ps | grep -q "Up"; then
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

# Final status check
if $DOCKER_COMPOSE_CMD ps | grep -q "Up"; then
    echo "✅ Application containers are running!"
    echo "🌐 Application URL: http://localhost:$PORT"
    echo "🔍 Health check: http://localhost:$PORT/actuator/health"
    echo "📚 API docs: http://localhost:$PORT/docs/"
    echo ""
    echo "📊 Container status:"
    $DOCKER_COMPOSE_CMD ps
else
    echo "❌ Application failed to start. Checking logs..."
    echo "📋 Container logs:"
    $DOCKER_COMPOSE_CMD logs --tail=50
    echo ""
    echo "🔍 Container status:"
    $DOCKER_COMPOSE_CMD ps -a
    exit 1
fi

echo "✨ Deployment completed successfully!"
echo "💡 To view logs: $DOCKER_COMPOSE_CMD logs -f"
echo "💡 To stop: $DOCKER_COMPOSE_CMD down"