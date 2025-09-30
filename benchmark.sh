#!/bin/bash

# Performance Benchmarking Script
# Tests getBalance, getPrices, and getNumber endpoints

echo "🚀 Starting API Performance Benchmark..."
echo "========================================"

# Configuration
API_BASE_URL="http://localhost:80"
API_KEY="your_api_key_here"  # Replace with actual API key
COUNTRY="BR"
OPERATOR="VIVO"
SERVICE_ID="1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to make API call and measure time
benchmark_endpoint() {
    local endpoint=$1
    local method=$2
    local data=$3
    local description=$4
    
    echo -e "\n${BLUE}Testing: $description${NC}"
    echo "Endpoint: $endpoint"
    
    # Make 10 requests and calculate average
    total_time=0
    success_count=0
    
    for i in {1..10}; do
        if [ "$method" = "GET" ]; then
            response=$(curl -s -w "%{time_total}" -o /dev/null "$API_BASE_URL$endpoint")
        else
            response=$(curl -s -w "%{time_total}" -o /dev/null -X POST \
                -H "Content-Type: application/json" \
                -d "$data" \
                "$API_BASE_URL$endpoint")
        fi
        
        if [ $? -eq 0 ]; then
            time_ms=$(echo "$response * 1000" | bc -l)
            total_time=$(echo "$total_time + $time_ms" | bc -l)
            success_count=$((success_count + 1))
            echo "  Request $i: ${time_ms}ms"
        else
            echo "  Request $i: ${RED}FAILED${NC}"
        fi
    done
    
    if [ $success_count -gt 0 ]; then
        avg_time=$(echo "scale=2; $total_time / $success_count" | bc -l)
        echo -e "  ${GREEN}Average: ${avg_time}ms${NC}"
        echo -e "  ${GREEN}Success Rate: $success_count/10${NC}"
    else
        echo -e "  ${RED}All requests failed${NC}"
    fi
}

# Test getBalance endpoint
benchmark_endpoint \
    "/api/v2/getBalance?api_key=$API_KEY" \
    "GET" \
    "" \
    "getBalance Endpoint"

# Test getPrices endpoint
benchmark_endpoint \
    "/api/v2/getPrices?api_key=$API_KEY&country=$COUNTRY&operator=$OPERATOR" \
    "GET" \
    "" \
    "getPrices Endpoint"

# Test getNumber endpoint
benchmark_endpoint \
    "/api/v2/getNumber" \
    "POST" \
    "{\"api_key\":\"$API_KEY\",\"service_id\":\"$SERVICE_ID\",\"country\":\"$COUNTRY\",\"operator\":\"$OPERATOR\"}" \
    "getNumber Endpoint"

# Test getExtraActivation endpoint
benchmark_endpoint \
    "/api/v2/getExtraActivation?api_key=$API_KEY&service_id=$SERVICE_ID&number=1234567890&country=$COUNTRY&operator=$OPERATOR" \
    "GET" \
    "" \
    "getExtraActivation Endpoint"

echo -e "\n${YELLOW}Benchmark Complete!${NC}"
echo "========================================"

# Additional performance tests
echo -e "\n${BLUE}Additional Performance Tests${NC}"

# Test Redis connectivity
echo -e "\n${BLUE}Testing Redis Connectivity${NC}"
redis-cli -h localhost -p 6379 ping

# Test RabbitMQ connectivity
echo -e "\n${BLUE}Testing RabbitMQ Connectivity${NC}"
curl -s -u guest:guest http://localhost:15672/api/overview | jq '.message_stats' 2>/dev/null || echo "RabbitMQ management API not accessible"

# Memory usage
echo -e "\n${BLUE}Memory Usage${NC}"
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep store24h

# Redis memory usage
echo -e "\n${BLUE}Redis Memory Usage${NC}"
redis-cli -h localhost -p 6379 info memory | grep used_memory_human

echo -e "\n${GREEN}Performance benchmark completed!${NC}"
echo "Check the results above to compare with your original API performance."
echo "Target: Sub-millisecond response times for hot path endpoints."
