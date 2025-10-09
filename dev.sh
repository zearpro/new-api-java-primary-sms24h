#!/bin/bash

# Store24h Development Environment
# This script deploys the complete development environment including:
# - MongoDB, Redis, RabbitMQ
# - Store24h API with scheduled cache synchronization
# - Hono Accelerator microservice

set -e

echo "🚀 Starting Store24h Development Environment..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if .env.dev exists
if [ ! -f ".env.dev" ]; then
    print_error ".env.dev file not found!"
    print_status "Creating .env.dev template..."
    cat > .env.dev << EOF
# Database Configuration
MYSQL_HOST=your-mysql-host
MYSQL_USER=your-mysql-user
MYSQL_PASSWORD=your-mysql-password
MYSQL_DATABASE=coredb
MYSQL_PORT=3306

# MongoDB Configuration (Local Docker Container)
MONGO_URL=mongodb://root:6N5S0dASN1U62tNI@mongodb:27017/ativacoes?authSource=admin

# Performance Tuning
TOMCAT_MAX_THREADS=200
TOMCAT_ACCEPT_COUNT=1000
TOMCAT_MIN_SPARE_THREADS=50
TOMCAT_MAX_CONNECTIONS=10000
ASYNC_EXECUTOR_CORE_POOL_SIZE=50
ASYNC_EXECUTOR_MAX_POOL_SIZE=100
ASYNC_EXECUTOR_QUEUE_CAPACITY=1000
ASYNC_EXECUTOR_THREAD_NAME_PREFIX=WarmupThread-
TOMCAT_THREADS_MAX=200
TOMCAT_THREADS_MIN_SPARE=50
HIKARI_MINIMUM_IDLE=10
EOF
    print_warning "Please edit .env.dev with your actual database credentials!"
    exit 1
fi


# Stop any existing containers
print_status "Stopping existing containers..."
docker compose -f docker-compose.dev.yml --env-file .env.dev down --remove-orphans || true

# Clean up old images (optional)
if [ "$1" = "--clean" ]; then
    print_status "Cleaning up old images..."
    docker image prune -f
    docker system prune -f
fi

# Start infrastructure first
print_status "Starting infrastructure (MongoDB, Redis, RabbitMQ)..."
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d redis rabbitmq

# Wait for infrastructure services to be ready
print_status "Waiting for infrastructure services to be ready..."
sleep 15


# Start Hono Accelerator
print_status "Starting Hono Accelerator..."
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d hono-accelerator

# Wait for Hono Accelerator to be ready
print_status "Waiting for Hono Accelerator to be ready..."
sleep 10

# Start main API service
print_status "Starting Store24h API service..."
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d store24h-api

# Wait for API to be ready
print_status "Waiting for Store24h API to be ready..."
sleep 30

# Check API health
print_status "Checking API health..."
for i in {1..30}; do
    if curl -f http://localhost:80/actuator/health > /dev/null 2>&1; then
        print_success "Store24h API is healthy!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "Store24h API failed to start within 5 minutes"
        print_status "Checking logs..."
        docker logs store24h-api --tail 50
        exit 1
    fi
    print_status "Waiting for API... ($i/30)"
    sleep 10
done


# Show service status
print_status "Checking all services status..."
docker compose -f docker-compose.dev.yml --env-file .env.dev ps

# Show useful URLs
print_success "🎉 Development environment is ready!"
echo ""
print_status "📊 Service URLs:"
echo "  • Store24h API: http://localhost:80"
echo "  • API Health: http://localhost:80/actuator/health"
echo "  • Hono Accelerator: http://localhost:3001"
echo "  • RabbitMQ Management: http://localhost:15672 (admin/admin123)"
echo "  • Redis: localhost:6379"
echo ""
print_status "🔧 Useful commands:"
echo "  • View logs: docker logs store24h-api -f"
echo "  • Stop all: docker compose -f docker-compose.dev.yml --env-file .env.dev down"
echo "  • Restart API: docker compose -f docker-compose.dev.yml --env-file .env.dev restart store24h-api"
echo ""
print_success "🚀 Your development environment is now running!"
print_status "Cache updates will happen every 2-15 minutes via scheduled sync!"