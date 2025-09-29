#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo " MODO DESENVOLVEDOR "
echo "Iniciando todos os serviços em modo de desenvolvimento..."
echo "As alterações nos arquivos Java serão aplicadas em tempo real."
echo "Pressione Ctrl+C para parar todos os contêineres."

# Define the environment file for development
ENV_FILE=.env.dev

# Check if the dev environment file exists, if not, create it from the example.
if [ ! -f "$ENV_FILE" ]; then
    echo "AVISO: O arquivo de ambiente de desenvolvimento '$ENV_FILE' não foi encontrado."
    echo "Copiando de '.env.example'. Por favor, revise as configurações."
    cp .env.example "$ENV_FILE"
fi

# Start docker-compose with the correct env file and dev override.
# The command will attach to the logs of the services.
docker-compose -f docker-compose.yml -f docker-compose.dev.yml --env-file "$ENV_FILE" up --build

# Optional: Add a trap to ensure containers are stopped on exit
trap "echo 'Parando contêineres...'; docker-compose -f docker-compose.yml -f docker-compose.dev.yml --env-file '$ENV_FILE' down; exit" INT TERM
