#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo "🚀 MODO DESENVOLVEDOR - Dragonfly Performance Edition"
echo "Iniciando todos os serviços em modo de desenvolvimento..."
echo "As alterações nos arquivos Java serão aplicadas em tempo real."
echo "API disponível em: http://localhost:80"
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

# Load environment variables safely
echo "📄 Loading environment variables..."
set -o allexport
# Use a safer method to load env vars, avoiding command interpretation
while IFS= read -r line; do
    # Skip comments and empty lines
    if [[ $line =~ ^[[:space:]]*# ]] || [[ -z "${line// }" ]]; then
        continue
    fi
    # Skip problematic variables that might contain commands
    if [[ $line =~ ^[[:space:]]*JAVA_OPTS ]] || [[ $line =~ ^[[:space:]]*NOT_CRON ]]; then
        continue
    fi
    # Export the variable safely
    if [[ $line =~ ^[[:space:]]*([^=]+)=(.*)$ ]]; then
        export "${BASH_REMATCH[1]}"="${BASH_REMATCH[2]}"
    fi
done < "$ENV_FILE"
set +o allexport

# Clean up any existing containers
echo "🧹 Limpando contêineres existentes..."
docker-compose down --remove-orphans || true

# Build and start all services with Dragonfly
echo "🔨 Construindo e iniciando todos os serviços..."
echo "📊 Serviços incluídos:"
echo "  - DragonflyDB (Redis alternativo de alta performance)"
echo "  - Store24h API (Spring Boot)"
echo "  - RabbitMQ (Message Queue)"
echo "  - Hono.js Accelerator"

# Start docker-compose with the correct env file
docker-compose --env-file "$ENV_FILE" up --build

# Optional: Add a trap to ensure containers are stopped on exit
trap "echo '🛑 Parando contêineres...'; docker-compose --env-file '$ENV_FILE' down; exit" INT TERM
