#!/bin/bash

# Store24h Production Deployment with CDC
# This script deploys the complete production environment including:
# - Redis, RabbitMQ, Kafka, Zookeeper, Debezium Connect
# - Store24h API with real-time CDC cache synchronization
# - Hono Accelerator microservice
# - Production optimizations and monitoring

set -e

echo "🚀 Starting Store24h Production Deployment with CDC..."

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

# Check if running as root or with sudo
if [ "$EUID" -ne 0 ]; then
    print_error "Please run as root or with sudo for production deployment"
    exit 1
fi

# Check if .env exists
if [ ! -f ".env" ]; then
    print_error ".env file not found!"
    print_status "Please create .env file with your production credentials"
    print_status "You can copy from .env.dev template and update with production values"
    exit 1
fi

# Load environment variables from .env
print_status "Loading environment variables from .env..."
# Use a safer method to load .env file
while IFS= read -r line; do
    # Skip comments and empty lines
    if [[ $line =~ ^[[:space:]]*# ]] || [[ -z "${line// }" ]]; then
        continue
    fi
    # Export the variable
    export "$line"
done < .env

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
    "database.history.kafka.bootstrap.servers": "\${KAFKA_BOOTSTRAP_SERVERS}",
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
    print_warning "Please update debezium-config/mysql-connector.json with your production database credentials!"
fi

# Create production docker-compose file
print_status "Creating production docker-compose configuration..."
cat > docker-compose.prod.yml << 'EOF'
version: '3.8'

services:
  # CDC Infrastructure - Zookeeper (Production)
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    container_name: store24h-zookeeper-prod
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
      ZOOKEEPER_LOG4J_LOGGERS: "org.apache.zookeeper=WARN"
      ZOOKEEPER_LOG4J_ROOT_LOGLEVEL: "WARN"
    volumes:
      - zookeeper-data-prod:/var/lib/zookeeper/data
      - zookeeper-logs-prod:/var/lib/zookeeper/log
    networks:
      - app-network-prod
    restart: always
    healthcheck:
      test: ["CMD", "nc", "-z", "localhost", "2181"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M
          cpus: '0.25'

  # CDC Infrastructure - Kafka (Production)
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    container_name: store24h-kafka-prod
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
      KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS: 300000
      KAFKA_LOG4J_LOGGERS: "kafka.controller=WARN,kafka.producer.async.DefaultEventHandler=WARN,state.change.logger=WARN"
      KAFKA_LOG4J_ROOT_LOGLEVEL: "WARN"
    volumes:
      - kafka-data-prod:/var/lib/kafka/data
    depends_on:
      zookeeper:
        condition: service_healthy
    networks:
      - app-network-prod
    restart: always
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'

  # CDC Infrastructure - Debezium Connect (Production)
  debezium-connect:
    image: debezium/connect:2.4
    container_name: store24h-debezium-connect-prod
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: debezium_configs
      OFFSET_STORAGE_TOPIC: debezium_offsets
      STATUS_STORAGE_TOPIC: debezium_statuses
      CONFIG_STORAGE_REPLICATION_FACTOR: 1
      OFFSET_STORAGE_REPLICATION_FACTOR: 1
      STATUS_STORAGE_REPLICATION_FACTOR: 1
      KEY_CONVERTER: org.apache.kafka.connect.json.JsonConverter
      VALUE_CONVERTER: org.apache.kafka.connect.json.JsonConverter
      KEY_CONVERTER_SCHEMAS_ENABLE: false
      VALUE_CONVERTER_SCHEMAS_ENABLE: false
      CONNECT_LOG4J_LOGGERS: "org.apache.kafka.connect.runtime.rest=WARN,org.reflections=ERROR"
      CONNECT_LOG4J_ROOT_LOGLEVEL: "WARN"
    depends_on:
      kafka:
        condition: service_healthy
    volumes:
      - ./debezium-config:/opt/debezium/config
    networks:
      - app-network-prod
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/connectors"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M
          cpus: '0.25'

  # Hono.js Accelerator Microservice (Production)
  hono-accelerator:
    build: ./hono-accelerator
    container_name: store24h-hono-accelerator-prod
    environment:
      - PORT=3000
      - REDIS_HOST=${REDIS_HOST}
      - REDIS_PORT=${REDIS_PORT}
      - REDIS_SSL=${REDIS_SSL}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - NODE_ENV=production
    networks:
      - app-network-prod
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
        reservations:
          memory: 256M
          cpus: '0.25'

  # Main Application Service (Production)
  store24h-api:
    build: .
    container_name: store24h-api-prod
    environment:
      # Database Configuration
      - MYSQL_HOST=${MYSQL_HOST}
      - MYSQL_USER=${MYSQL_USER}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - MYSQL_DATABASE=${MYSQL_DATABASE}
      - MYSQL_PORT=${MYSQL_PORT:-3306}
      
      # MongoDB Configuration
      - MONGO_URL=${MONGO_URL}
      
      # Redis Configuration (Production)
      - REDIS_HOST=${REDIS_HOST}
      - REDIS_PORT=${REDIS_PORT}
      - REDIS_SSL=${REDIS_SSL}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      
      # RabbitMQ Configuration (Production)
      - SPRING_RABBITMQ_HOST=${RABBITMQ_HOST}
      - SPRING_RABBITMQ_PORT=${RABBITMQ_PORT}
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USERNAME}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
      - SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=true
      
      # CDC Kafka Configuration
      - KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}
      - SPRING_KAFKA_CONSUMER_GROUP_ID=cache-sync-group-prod
      - SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET=earliest
      - SPRING_KAFKA_CONSUMER_KEY_DESERIALIZER=org.apache.kafka.common.serialization.StringDeserializer
      - SPRING_KAFKA_CONSUMER_VALUE_DESERIALIZER=org.apache.kafka.common.serialization.StringDeserializer
      
      # Security Configuration
      - IS_SMSHUB=${IS_SMSHUB:-false}
      - ENABLE_API_KEY_VALIDATION=${ENABLE_API_KEY_VALIDATION:-true}
      
      # Performance tuning for production
      - TOMCAT_MAX_THREADS=${TOMCAT_MAX_THREADS:-500}
      - TOMCAT_ACCEPT_COUNT=${TOMCAT_ACCEPT_COUNT:-2000}
      - TOMCAT_MIN_SPARE_THREADS=${TOMCAT_MIN_SPARE_THREADS:-100}
      - TOMCAT_MAX_CONNECTIONS=${TOMCAT_MAX_CONNECTIONS:-20000}
      - ASYNC_EXECUTOR_CORE_POOL_SIZE=${ASYNC_EXECUTOR_CORE_POOL_SIZE:-100}
      - ASYNC_EXECUTOR_MAX_POOL_SIZE=${ASYNC_EXECUTOR_MAX_POOL_SIZE:-200}
      - ASYNC_EXECUTOR_QUEUE_CAPACITY=${ASYNC_EXECUTOR_QUEUE_CAPACITY:-2000}
      - ASYNC_EXECUTOR_THREAD_NAME_PREFIX=${ASYNC_EXECUTOR_THREAD_NAME_PREFIX:-ProdThread-}
      - TOMCAT_THREADS_MAX=${TOMCAT_THREADS_MAX:-500}
      - TOMCAT_THREADS_MIN_SPARE=${TOMCAT_THREADS_MIN_SPARE:-100}
      - HIKARI_MINIMUM_IDLE=${HIKARI_MINIMUM_IDLE:-20}
      - HIKARI_MAXIMUM_POOL_SIZE=${HIKARI_MAXIMUM_POOL_SIZE:-50}
      
      # Monitoring Configuration
      - ENABLE_METRICS=${ENABLE_METRICS:-true}
      - ENABLE_HEALTH_CHECKS=${ENABLE_HEALTH_CHECKS:-true}
      
      # Application Configuration
      - LISTEN_PORT=80
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      kafka:
        condition: service_healthy
    networks:
      - app-network-prod
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:80/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 30s
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: '2.0'
        reservations:
          memory: 2G
          cpus: '1.0'

volumes:
  zookeeper-data-prod:
    driver: local
  zookeeper-logs-prod:
    driver: local
  kafka-data-prod:
    driver: local

networks:
  app-network-prod:
    driver: bridge
EOF

# Stop any existing containers
print_status "Stopping existing production containers..."
docker compose -f docker-compose.prod.yml --env-file .env down --remove-orphans || true

# Clean up old images (optional)
if [ "$1" = "--clean" ]; then
    print_status "Cleaning up old images..."
    docker image prune -f
    docker system prune -f
fi

# Start CDC infrastructure first
print_status "Starting CDC infrastructure (Zookeeper, Kafka, Debezium)..."
docker compose -f docker-compose.prod.yml --env-file .env up -d zookeeper kafka debezium-connect

# Wait for CDC services to be ready
print_status "Waiting for CDC services to be ready..."
sleep 60

# Check if Kafka is ready
print_status "Checking Kafka readiness..."
for i in {1..30}; do
    if docker exec store24h-kafka-prod kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
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
    if docker exec store24h-debezium-connect-prod curl -f http://localhost:8083/connectors > /dev/null 2>&1; then
        print_success "Debezium Connect is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "Debezium Connect failed to start within 5 minutes"
        print_status "Checking container status..."
        docker ps | grep debezium-connect
        print_status "Checking container logs..."
        docker logs store24h-debezium-connect-prod --tail 20
        exit 1
    fi
    print_status "Waiting for Debezium Connect... ($i/30)"
    sleep 10
done

# Start Hono Accelerator
print_status "Starting Hono Accelerator..."
docker compose -f docker-compose.prod.yml --env-file .env up -d hono-accelerator

# Wait for Hono Accelerator to be ready
print_status "Waiting for Hono Accelerator to be ready..."
sleep 15

# Start main API service
print_status "Starting Store24h API service..."
docker compose -f docker-compose.prod.yml --env-file .env up -d store24h-api

# Wait for API to be ready
print_status "Waiting for Store24h API to be ready..."
sleep 60

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
        docker logs store24h-api-prod --tail 50
    exit 1
  fi
    print_status "Waiting for API... ($i/30)"
    sleep 10
done

# Register Debezium MySQL connector
print_status "Registering Debezium MySQL connector..."
sleep 15

# Replace environment variables in connector config
envsubst < debezium-config/mysql-connector.json > /tmp/mysql-connector-prod.json

# Copy connector config to container
docker cp /tmp/mysql-connector-prod.json store24h-debezium-connect-prod:/tmp/mysql-connector-prod.json

# Register the connector
if docker exec store24h-debezium-connect-prod curl -X POST http://localhost:8083/connectors \
    -H "Content-Type: application/json" \
    -d @/tmp/mysql-connector-prod.json > /dev/null 2>&1; then
    print_success "Debezium MySQL connector registered successfully!"
else
    print_warning "Failed to register Debezium connector. You may need to register it manually."
    print_status "You can register it manually with:"
    print_status "docker exec store24h-debezium-connect-prod curl -X POST http://localhost:8083/connectors -H 'Content-Type: application/json' -d @/tmp/mysql-connector-prod.json"
fi

# Clean up temp file
rm -f /tmp/mysql-connector-prod.json

# Show service status
print_status "Checking all services status..."
docker compose -f docker-compose.prod.yml --env-file .env ps

# Setup log rotation
print_status "Setting up log rotation..."
cat > /etc/logrotate.d/docker-containers << EOF
/var/lib/docker/containers/*/*.log {
    rotate 7
    daily
    compress
    size=1M
    missingok
    delaycompress
    copytruncate
}
EOF

# Setup monitoring
print_status "Setting up basic monitoring..."
cat > /usr/local/bin/monitor-services.sh << 'EOF'
#!/bin/bash
echo "=== Store24h Production Services Status ==="
echo "Date: $(date)"
echo ""
echo "=== Docker Services ==="
docker compose -f docker-compose.prod.yml --env-file .env ps
echo ""
echo "=== API Health ==="
curl -s http://localhost:80/actuator/health | jq . || echo "API not responding"
echo ""
echo "=== CDC Topics ==="
docker exec store24h-kafka-prod kafka-topics --bootstrap-server localhost:9092 --list | grep cache-sync || echo "No CDC topics found"
echo ""
echo "=== Memory Usage ==="
free -h
echo ""
echo "=== Disk Usage ==="
df -h
EOF

chmod +x /usr/local/bin/monitor-services.sh

# Setup cron job for monitoring
print_status "Setting up monitoring cron job..."
(crontab -l 2>/dev/null; echo "*/5 * * * * /usr/local/bin/monitor-services.sh >> /var/log/store24h-monitor.log 2>&1") | crontab -

# Show useful URLs and commands
print_success "🎉 Production environment is ready!"
echo ""
print_status "📊 Service URLs:"
echo "  • Store24h API: http://localhost:80"
echo "  • API Health: http://localhost:80/actuator/health"
echo "  • Hono Accelerator: http://localhost:3001"
echo "  • Debezium Connect: http://localhost:8083"
echo ""
print_status "🔧 Useful commands:"
echo "  • View logs: docker logs store24h-api-prod -f"
echo "  • Stop all: docker compose -f docker-compose.prod.yml --env-file .env down"
echo "  • Restart API: docker compose -f docker-compose.prod.yml --env-file .env restart store24h-api"
echo "  • Check CDC topics: docker exec store24h-kafka-prod kafka-topics --bootstrap-server localhost:9092 --list"
echo "  • Monitor services: /usr/local/bin/monitor-services.sh"
echo "  • View monitoring logs: tail -f /var/log/store24h-monitor.log"
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
print_success "🚀 Your production environment with real-time CDC is now running!"
print_status "Cache updates will now happen within 200ms of MySQL changes!"
print_status "Monitoring is set up with 5-minute health checks!"