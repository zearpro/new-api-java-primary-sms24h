#!/bin/bash

# Store24h EC2 Migration Script
# This script helps migrate from old deployment to new CDC-enabled deployment

set -e

echo "🔄 Store24h EC2 Migration Script"
echo "================================="

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
    print_error "Please run as root or with sudo for migration"
    exit 1
fi

# Function to stop all existing containers
stop_existing_containers() {
    print_status "Stopping all existing Store24h containers..."
    
    # Stop containers with common names
    containers_to_stop=(
        "store24h-api"
        "store24h-redis"
        "store24h-rabbitmq"
        "store24h-hono-accelerator"
        "store24h-dashboard"
        "api-ecr-extracted-store24h-api"
        "api-ecr-extracted-redis"
        "api-ecr-extracted-rabbitmq"
        "api-ecr-extracted-hono-accelerator"
    )
    
    for container in "${containers_to_stop[@]}"; do
        if docker ps -q -f name="$container" | grep -q .; then
            print_status "Stopping container: $container"
            docker stop "$container" || true
            docker rm "$container" || true
        fi
    done
    
    # Stop all containers with store24h in name
    print_status "Stopping all containers with 'store24h' in name..."
    docker ps -q -f name="store24h" | xargs -r docker stop || true
    docker ps -aq -f name="store24h" | xargs -r docker rm || true
    
    # Stop all containers with api-ecr-extracted in name
    print_status "Stopping all containers with 'api-ecr-extracted' in name..."
    docker ps -q -f name="api-ecr-extracted" | xargs -r docker stop || true
    docker ps -aq -f name="api-ecr-extracted" | xargs -r docker rm || true
    
    print_success "All existing containers stopped and removed"
}

# Function to clean up old networks
cleanup_networks() {
    print_status "Cleaning up old Docker networks..."
    
    # Remove old networks
    networks_to_remove=(
        "api-ecr-extracted_app-network"
        "api-ecr-extracted_default"
        "store24h_app-network"
        "store24h_default"
    )
    
    for network in "${networks_to_remove[@]}"; do
        if docker network ls -q -f name="$network" | grep -q .; then
            print_status "Removing network: $network"
            docker network rm "$network" || true
        fi
    done
    
    print_success "Old networks cleaned up"
}

# Function to clean up old volumes
cleanup_volumes() {
    print_status "Cleaning up old Docker volumes..."
    
    # Remove old volumes
    volumes_to_remove=(
        "api-ecr-extracted_redis_data"
        "api-ecr-extracted_rabbitmq_data"
        "store24h_redis_data"
        "store24h_rabbitmq_data"
    )
    
    for volume in "${volumes_to_remove[@]}"; do
        if docker volume ls -q -f name="$volume" | grep -q .; then
            print_status "Removing volume: $volume"
            docker volume rm "$volume" || true
        fi
    done
    
    print_success "Old volumes cleaned up"
}

# Function to backup current configuration
backup_configuration() {
    print_status "Backing up current configuration..."
    
    backup_dir="/opt/store24h-backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$backup_dir"
    
    # Backup environment files
    if [ -f ".env.dev" ]; then
        cp .env.dev "$backup_dir/"
        print_status "Backed up .env.dev"
    fi
    
    if [ -f ".env.prod" ]; then
        cp .env.prod "$backup_dir/"
        print_status "Backed up .env.prod"
    fi
    
    # Backup docker-compose files
    if [ -f "docker-compose.dev.yml" ]; then
        cp docker-compose.dev.yml "$backup_dir/"
        print_status "Backed up docker-compose.dev.yml"
    fi
    
    if [ -f "docker-compose.yml" ]; then
        cp docker-compose.yml "$backup_dir/"
        print_status "Backed up docker-compose.yml"
    fi
    
    # Backup scripts
    if [ -f "dev.sh" ]; then
        cp dev.sh "$backup_dir/"
        print_status "Backed up dev.sh"
    fi
    
    if [ -f "deploy.sh" ]; then
        cp deploy.sh "$backup_dir/"
        print_status "Backed up deploy.sh"
    fi
    
    print_success "Configuration backed up to: $backup_dir"
}

# Function to check system requirements
check_system_requirements() {
    print_status "Checking system requirements..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed!"
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed!"
        exit 1
    fi
    
    # Check available memory
    available_memory=$(free -m | awk 'NR==2{printf "%.0f", $7}')
    if [ "$available_memory" -lt 2048 ]; then
        print_warning "Available memory is less than 2GB. CDC services may not perform optimally."
    fi
    
    # Check available disk space
    available_disk=$(df / | awk 'NR==2{print $4}')
    if [ "$available_disk" -lt 10485760 ]; then # 10GB in KB
        print_warning "Available disk space is less than 10GB. Consider cleaning up."
    fi
    
    print_success "System requirements check completed"
}

# Function to install required packages
install_required_packages() {
    print_status "Installing required packages..."
    
    # Update package list
    apt-get update
    
    # Install required packages
    packages=(
        "curl"
        "jq"
        "netcat-openbsd"
        "htop"
        "iotop"
        "nethogs"
    )
    
    for package in "${packages[@]}"; do
        if ! dpkg -l | grep -q "^ii  $package "; then
            print_status "Installing $package..."
            apt-get install -y "$package"
        else
            print_status "$package is already installed"
        fi
    done
    
    print_success "Required packages installed"
}

# Function to setup firewall rules
setup_firewall() {
    print_status "Setting up firewall rules..."
    
    # Check if ufw is available
    if command -v ufw &> /dev/null; then
        # Allow required ports
        ports=(
            "80"      # HTTP
            "443"     # HTTPS
            "3001"    # Hono Accelerator
            "8083"    # Debezium Connect
            "9092"    # Kafka
            "2181"    # Zookeeper
            "6379"    # Redis
            "5672"    # RabbitMQ
            "15672"   # RabbitMQ Management
        )
        
        for port in "${ports[@]}"; do
            ufw allow "$port" || true
        done
        
        print_success "Firewall rules configured"
    else
        print_warning "UFW not available, skipping firewall configuration"
    fi
}

# Function to create systemd service for monitoring
create_monitoring_service() {
    print_status "Creating monitoring service..."
    
    cat > /etc/systemd/system/store24h-monitor.service << 'EOF'
[Unit]
Description=Store24h Service Monitor
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/monitor-services.sh
User=root

[Install]
WantedBy=multi-user.target
EOF

    cat > /etc/systemd/system/store24h-monitor.timer << 'EOF'
[Unit]
Description=Run Store24h Monitor every 5 minutes
Requires=store24h-monitor.service

[Timer]
OnCalendar=*:0/5
Persistent=true

[Install]
WantedBy=timers.target
EOF

    systemctl daemon-reload
    systemctl enable store24h-monitor.timer
    systemctl start store24h-monitor.timer
    
    print_success "Monitoring service created and started"
}

# Main migration function
main() {
    echo ""
    print_status "Starting Store24h EC2 Migration..."
    echo ""
    
    # Step 1: Check system requirements
    check_system_requirements
    
    # Step 2: Install required packages
    install_required_packages
    
    # Step 3: Backup current configuration
    backup_configuration
    
    # Step 4: Stop existing containers
    stop_existing_containers
    
    # Step 5: Clean up old resources
    cleanup_networks
    cleanup_volumes
    
    # Step 6: Setup firewall
    setup_firewall
    
    # Step 7: Create monitoring service
    create_monitoring_service
    
    echo ""
    print_success "🎉 Migration preparation completed!"
    echo ""
    print_status "Next steps:"
    echo "  1. Update your .env.prod file with production credentials"
    echo "  2. Run: ./deploy.sh"
    echo "  3. Monitor the deployment with: /usr/local/bin/monitor-services.sh"
    echo ""
    print_status "Migration backup location: /opt/store24h-backup-*"
    echo ""
    print_warning "Important: Make sure to update your database credentials in .env.prod before running deploy.sh!"
}

# Handle command line arguments
case "${1:-}" in
    "stop")
        print_status "Stopping all existing containers..."
        stop_existing_containers
        cleanup_networks
        cleanup_volumes
        print_success "All containers stopped and cleaned up"
        ;;
    "backup")
        print_status "Creating backup..."
        backup_configuration
        print_success "Backup completed"
        ;;
    "check")
        print_status "Checking system requirements..."
        check_system_requirements
        ;;
    "install")
        print_status "Installing required packages..."
        install_required_packages
        ;;
    "firewall")
        print_status "Setting up firewall..."
        setup_firewall
        ;;
    "monitor")
        print_status "Setting up monitoring..."
        create_monitoring_service
        ;;
    *)
        main
        ;;
esac
