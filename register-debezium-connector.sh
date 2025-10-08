#!/bin/bash

# Script to register Debezium MySQL connector with environment variable substitution
# This script reads the connector configuration and substitutes environment variables

set -e

# Load environment variables from .env if it exists
if [ -f ".env" ]; then
    echo "📋 Loading environment variables from .env..."
    set -a  # automatically export all variables
    source .env
    set +a  # stop automatically exporting
fi

# Check if required environment variables are set
if [ -z "$MYSQL_HOST" ] || [ -z "$MYSQL_USER" ] || [ -z "$MYSQL_PASSWORD" ]; then
    echo "❌ Error: Required MySQL environment variables not set:"
    echo "   MYSQL_HOST: ${MYSQL_HOST:-'NOT SET'}"
    echo "   MYSQL_USER: ${MYSQL_USER:-'NOT SET'}"
    echo "   MYSQL_PASSWORD: ${MYSQL_PASSWORD:-'NOT SET'}"
    echo ""
    echo "Please set these variables in your .env file or environment"
    exit 1
fi

# Default values
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_DATABASE=${MYSQL_DATABASE:-coredb}

echo "🔧 Registering Debezium MySQL connector with configuration:"
echo "   Host: $MYSQL_HOST:$MYSQL_PORT"
echo "   User: $MYSQL_USER"
echo "   Database: $MYSQL_DATABASE"
echo ""

# Create temporary connector configuration with substituted values
TEMP_CONFIG=$(mktemp)
envsubst < debezium-config/mysql-connector.json > "$TEMP_CONFIG"

echo "📋 Connector configuration:"
cat "$TEMP_CONFIG"
echo ""

# Check if Debezium Connect is available
echo "🔍 Checking Debezium Connect availability..."
if ! curl -s -f http://localhost:8083/connectors > /dev/null; then
    echo "❌ Error: Debezium Connect is not available at http://localhost:8083"
    echo "   Please ensure the CDC infrastructure is running:"
    echo "   docker-compose -f docker-compose.dev.yml up debezium-connect -d"
    exit 1
fi

echo "✅ Debezium Connect is available"

# Register the connector
echo "🚀 Registering MySQL connector..."
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d @"$TEMP_CONFIG" http://localhost:8083/connectors)

# Check if registration was successful
if echo "$RESPONSE" | grep -q '"error_code"'; then
    echo "❌ Error registering connector:"
    echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
    exit 1
else
    echo "✅ Connector registered successfully!"
    echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
fi

# Clean up temporary file
rm "$TEMP_CONFIG"

echo ""
echo "🎉 Debezium MySQL connector is now active!"
echo "📊 You can monitor CDC events at: http://localhost:8083/connectors/mysql-cache-sync-connector/status"
echo "🔄 Real-time cache synchronization is now enabled for all configured tables"
