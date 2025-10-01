#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo "🚀 MODO DESENVOLVEDOR - Dragonfly Performance Edition"
echo "Iniciando todos os serviços em modo de desenvolvimento..."
echo "As alterações nos arquivos Java serão aplicadas em tempo real."
echo "Dashboard disponível em: http://localhost:3000"
echo "Pressione Ctrl+C para parar todos os contêineres."

# Define the environment file for development
ENV_FILE=.env.dev

# Check if the dev environment file exists, if not, create it from the example.
if [ ! -f "$ENV_FILE" ]; then
    echo "AVISO: O arquivo de ambiente de desenvolvimento '$ENV_FILE' não foi encontrado."
    echo "Copiando de '.env.example'. Por favor, revise as configurações."
    cp .env.example "$ENV_FILE"
fi

# Check if Docker is running
if ! docker ps &> /dev/null; then
    echo "❌ Docker não está rodando. Por favor, inicie o Docker primeiro."
    exit 1
fi

# Clean up any existing containers
echo "🧹 Limpando contêineres existentes..."
docker-compose down --remove-orphans || true

# Build and start all services with Dragonfly and Dashboard
echo "🔨 Construindo e iniciando todos os serviços..."
echo "📊 Serviços incluídos:"
echo "  - DragonflyDB (Redis alternativo de alta performance)"
echo "  - Store24h API (Spring Boot)"
echo "  - Dashboard React (Monitoramento em tempo real)"
echo "  - RabbitMQ (Message Queue)"
echo "  - Hono.js Accelerator"

# Start docker-compose with the correct env file
docker-compose --env-file "$ENV_FILE" up --build

# Optional: Add a trap to ensure containers are stopped on exit
trap "echo '🛑 Parando contêineres...'; docker-compose --env-file '$ENV_FILE' down; exit" INT TERM
