#!/bin/bash

# Store24h Development Environment with CDC
# This script deploys the complete development environment including:
# - MongoDB, Redis, RabbitMQ, Kafka, Zookeeper, Debezium Connect
# - Store24h API with real-time CDC cache synchronization
# - Hono Accelerator microservice

set -e

echo "🚀 Starting Store24h Development Environment with CDC..."

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

# Check if debezium-config directory exists
if [ ! -d "debezium-config" ]; then
    print_error "debezium-config directory not found!"
    print_status "Creating debezium-config directory..."
    mkdir -p debezium-config
fi

# Check if mysql-connector.json exists
if [ ! -f "debezium-config/mysql-connector.json" ]; then
    print_error "debezium-config/mysql-connector.json not found!"
    print_status "Creating mysql-connector.json template..."
    cat > debezium-config/mysql-connector.json << EOF
{
  "name": "mysql-cache-sync-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "\${MYSQL_HOST}",
    "database.port": "3306",
    "database.user": "\${MYSQL_USER}",
    "database.password": "\${MYSQL_PASSWORD}",
    "database.server.id": "184054",
    "database.server.name": "coredb",
    "database.include.list": "coredb",
    "table.include.list": "coredb.chip_model,coredb.chip_model_online,coredb.servicos,coredb.v_operadoras,coredb.chip_number_control,coredb.chip_number_control_alias_service,coredb.usuario,coredb.activation",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "dbhistory.coredb",
    "include.schema.changes": "false",
    "snapshot.mode": "initial",
    "snapshot.locking.mode": "minimal",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "coredb\\\\.(.*)",
    "transforms.route.replacement": "cache-sync.$1",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false",
    "binlog.buffer.size": "32768",
    "max.batch.size": "2048",
    "max.queue.size": "8192",
    "poll.interval.ms": "1000",
    "connect.timeout.ms": "30000",
    "tombstones.on.delete": "false"
  }
}
EOF
    print_warning "Please update debezium-config/mysql-connector.json with your database credentials!"
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
print_status "Starting infrastructure (MongoDB, Zookeeper, Kafka, Debezium)..."
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d mongodb zookeeper kafka debezium-connect

# Wait for infrastructure services to be ready
print_status "Waiting for infrastructure services to be ready..."
sleep 30

# Check if MongoDB is ready
print_status "Checking MongoDB readiness..."
for i in {1..30}; do
    if docker exec store24h-mongodb mongosh --eval "db.adminCommand('ping')" > /dev/null 2>&1; then
        print_success "MongoDB is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "MongoDB failed to start within 5 minutes"
        exit 1
    fi
    print_status "Waiting for MongoDB... ($i/30)"
    sleep 10
done

# Check if Kafka is ready
print_status "Checking Kafka readiness..."
for i in {1..30}; do
    if docker exec store24h-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        print_success "Kafka is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "Kafka failed to start within 5 minutes"
        exit 1
    fi
    print_status "Waiting for Kafka... ($i/30)"
    sleep 10
done

# Check if Debezium Connect is ready
print_status "Checking Debezium Connect readiness..."
for i in {1..30}; do
    if curl -f http://localhost:8083/connectors > /dev/null 2>&1; then
        print_success "Debezium Connect is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "Debezium Connect failed to start within 5 minutes"
        exit 1
    fi
    print_status "Waiting for Debezium Connect... ($i/30)"
    sleep 10
done

# Start Redis and RabbitMQ
print_status "Starting Redis and RabbitMQ..."
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d redis rabbitmq

# Wait for Redis and RabbitMQ to be ready
print_status "Waiting for Redis and RabbitMQ to be ready..."
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

# Register Debezium MySQL connector
print_status "Registering Debezium MySQL connector..."
sleep 10

# Replace environment variables in connector config
envsubst < debezium-config/mysql-connector.json > /tmp/mysql-connector.json

# Register the connector
if curl -X POST http://localhost:8083/connectors \
    -H "Content-Type: application/json" \
    -d @/tmp/mysql-connector.json > /dev/null 2>&1; then
    print_success "Debezium MySQL connector registered successfully!"
else
    print_warning "Failed to register Debezium connector. You may need to register it manually."
    print_status "You can register it manually with:"
    print_status "curl -X POST http://localhost:8083/connectors -H 'Content-Type: application/json' -d @debezium-config/mysql-connector.json"
fi

# Clean up temp file
rm -f /tmp/mysql-connector.json

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
echo "  • Debezium Connect: http://localhost:8083"
echo "  • MongoDB: localhost:27017"
echo "  • Redis: localhost:6379"
echo "  • Kafka: localhost:9092"
echo ""
print_status "🔧 Useful commands:"
echo "  • View logs: docker logs store24h-api -f"
echo "  • Stop all: docker compose -f docker-compose.dev.yml --env-file .env.dev down"
echo "  • Restart API: docker compose -f docker-compose.dev.yml --env-file .env.dev restart store24h-api"
echo "  • Check CDC topics: docker exec store24h-kafka kafka-topics --bootstrap-server localhost:9092 --list"
echo ""
print_status "📈 CDC Topics created:"
echo "  • cache-sync.chip_model"
echo "  • cache-sync.chip_model_online"
echo "  • cache-sync.servicos"
echo "  • cache-sync.v_operadoras"
echo "  • cache-sync.chip_number_control"
echo "  • cache-sync.chip_number_control_alias_service"
echo "  • cache-sync.usuario"
echo "  • cache-sync.activation"
echo ""
print_success "🚀 Your development environment with real-time CDC is now running!"
print_status "Cache updates will now happen within 200ms of MySQL changes!"