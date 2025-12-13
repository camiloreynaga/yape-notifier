#!/bin/bash

# ============================================
# Development Deployment Script
# ============================================
# Uso: ./deploy.sh

set -e

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.yml" ]; then
    echo "Este script debe ejecutarse desde infra/docker/environments/development"
    exit 1
fi

info "Iniciando entorno de desarrollo..."

# Verificar archivo .env
if [ ! -f ".env" ]; then
    warn "Archivo .env no encontrado"
    if [ -f ".env.example" ]; then
        warn "Copiando desde .env.example..."
        cp .env.example .env
        info "Archivo .env creado. Puedes editarlo si necesitas cambiar valores."
    fi
fi

# Construir imágenes
info "Construyendo imágenes Docker..."
docker compose --env-file .env build

# Detener contenedores existentes
info "Deteniendo contenedores existentes..."
docker compose --env-file .env down

# Iniciar contenedores
info "Iniciando contenedores..."
docker compose --env-file .env up -d

# Esperar a que los servicios estén listos
info "Esperando a que los servicios estén listos..."
sleep 10

# Ejecutar migraciones
info "Ejecutando migraciones..."
docker compose --env-file .env exec -T php-fpm php artisan migrate --force || true

# Limpiar caches
info "Limpiando caches..."
docker compose --env-file .env exec -T php-fpm php artisan config:clear
docker compose --env-file .env exec -T php-fpm php artisan route:clear
docker compose --env-file .env exec -T php-fpm php artisan view:clear

# Verificar estado
info "Verificando estado de los contenedores..."
docker compose --env-file .env ps

info ""
info "✅ Entorno de desarrollo listo!"
info ""
info "Servicios disponibles:"
info "  - API: http://localhost:8000/up"
info "  - Database: localhost:5432"
info ""
info "Para ver los logs:"
info "  docker compose --env-file .env logs -f"
info ""
info "Para detener:"
info "  docker compose --env-file .env down"

