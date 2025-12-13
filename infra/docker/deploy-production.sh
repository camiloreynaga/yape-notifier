#!/bin/bash

# ============================================
# Script de Despliegue en Producción
# ============================================
# Uso: ./deploy-production.sh

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.yml" ]; then
    error "Este script debe ejecutarse desde el directorio infra/docker"
    exit 1
fi

# Verificar Docker
if ! command -v docker &> /dev/null; then
    error "Docker no está instalado. Por favor instálalo primero."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    error "Docker Compose no está instalado. Por favor instálalo primero."
    exit 1
fi

info "Iniciando despliegue en producción..."

# Verificar archivo .env.production
if [ ! -f ".env.production" ]; then
    warn "Archivo .env.production no encontrado"
    if [ -f ".env.production.example" ]; then
        warn "Copiando desde .env.production.example..."
        cp .env.production.example .env.production
        error "Por favor configura .env.production antes de continuar"
        exit 1
    else
        error "Archivo .env.production.example no encontrado"
        exit 1
    fi
fi

# Verificar archivo .env.production del API
if [ ! -f "../../apps/api/.env.production" ]; then
    warn "Archivo apps/api/.env.production no encontrado"
    error "Por favor crea apps/api/.env.production con las variables necesarias"
    exit 1
fi

# Construir imágenes
info "Construyendo imágenes Docker..."
docker compose -f docker-compose.yml build --no-cache

# Detener contenedores existentes
info "Deteniendo contenedores existentes..."
docker compose -f docker-compose.yml down

# Iniciar contenedores
info "Iniciando contenedores..."
docker compose -f docker-compose.yml up -d

# Esperar a que los servicios estén listos
info "Esperando a que los servicios estén listos..."
sleep 15

# Ejecutar migraciones
info "Ejecutando migraciones..."
docker compose -f docker-compose.yml exec -T php-fpm php artisan migrate --force

# Generar APP_KEY si no existe
info "Verificando APP_KEY..."
docker compose -f docker-compose.yml exec -T php-fpm php artisan key:generate --force || true

# Optimizar Laravel
info "Optimizando Laravel..."
docker compose -f docker-compose.yml exec -T php-fpm php artisan config:cache
docker compose -f docker-compose.yml exec -T php-fpm php artisan route:cache
docker compose -f docker-compose.yml exec -T php-fpm php artisan view:cache

# Verificar estado
info "Verificando estado de los contenedores..."
docker compose -f docker-compose.yml ps

info ""
info "✅ Despliegue completado!"
info ""
info "Servicios disponibles:"
info "  - API: https://api.notificaciones.space"
info "  - Dashboard: https://dashboard.notificaciones.space"
info ""
info "Próximos pasos:"
info "  1. Verificar que los DNS estén configurados (api.notificaciones.space, dashboard.notificaciones.space)"
info "  2. Verificar que Caddy obtenga los certificados SSL automáticamente"
info "  3. Verificar que los servicios responden correctamente"
info ""
info "Para ver los logs:"
info "  docker compose -f docker-compose.yml logs -f"



