#!/bin/bash

# Production deployment script for EC2
# This script builds and deploys the Store24h API

set -e

echo "🚀 Starting Store24h API deployment..."

# Check if .env file exists
if [ ! -f .env ]; then
    echo "❌ Error: .env file not found!"
    echo "📝 Please create .env file with your production environment variables"
    echo "📋 You can copy from .env.example and update the values"
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Build the Docker image
echo "🔨 Building Docker image..."
sudo docker-compose build

# Start the application
echo "🚀 Starting the application..."
sudo docker-compose up -d

# Wait for the application to start
echo "⏳ Waiting for application to start..."
sleep 30

# Check if the application is running
if sudo docker-compose ps | grep -q "Up"; then
    echo "✅ Application is running successfully!"
    echo "🌐 Application URL: http://localhost:${LISTEN_PORT:-80}"
    echo "🔍 Health check: http://localhost:${LISTEN_PORT:-80}/actuator/health"
    echo "📚 API docs: http://localhost:${LISTEN_PORT:-80}/docs/"
else
    echo "❌ Application failed to start. Checking logs..."
    sudo docker-compose logs
    exit 1
fi

echo "✨ Deployment completed!"