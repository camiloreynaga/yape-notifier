#!/bin/bash

# ============================================
# Production Deployment Script
# ============================================
# Uso: ./deploy.sh

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
    error "Este script debe ejecutarse desde infra/docker/environments/production"
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

# Verificar archivo .env
if [ ! -f ".env" ]; then
    warn "Archivo .env no encontrado"
    if [ -f ".env.example" ]; then
        warn "Copiando desde .env.example..."
        cp .env.example .env
        error "Por favor configura .env antes de continuar"
        exit 1
    else
        error "Archivo .env.example no encontrado"
        exit 1
    fi
fi

# Verificar que DB_PASSWORD está configurado
if ! grep -q "^DB_PASSWORD=.*" .env || grep -q "^DB_PASSWORD=$" .env || grep -q "TU_CONTRASEÑA" .env; then
    error "DB_PASSWORD no está configurado en .env"
    error "Por favor, edita .env y configura DB_PASSWORD con un valor seguro"
    exit 1
fi

# Construir imágenes
info "Construyendo imágenes Docker..."
docker compose --env-file .env build --no-cache

# Detener contenedores existentes
info "Deteniendo contenedores existentes..."
docker compose --env-file .env down

# Iniciar contenedores
info "Iniciando contenedores..."
docker compose --env-file .env up -d

# Esperar a que los servicios estén listos
info "Esperando a que los servicios estén listos..."
sleep 15

# Ejecutar migraciones
info "Ejecutando migraciones..."
docker compose --env-file .env exec -T php-fpm php artisan migrate --force

# Generar APP_KEY si no existe
info "Verificando APP_KEY..."
docker compose --env-file .env exec -T php-fpm php artisan key:generate --force || true

# Asegurar permisos y directorios
info "Verificando permisos y directorios..."
docker compose --env-file .env exec -T php-fpm sh -c "mkdir -p /var/www/storage/framework/{sessions,views,cache} /var/www/storage/logs /var/www/bootstrap/cache && chown -R www-data:www-data /var/www/storage /var/www/bootstrap/cache && chmod -R 775 /var/www/storage /var/www/bootstrap/cache"

# Optimizar Laravel
info "Optimizando Laravel..."
docker compose --env-file .env exec -T php-fpm php artisan config:cache
docker compose --env-file .env exec -T php-fpm php artisan route:cache
# No cachear vistas en producción inicialmente (puede causar problemas si faltan directorios)
# docker compose --env-file .env exec -T php-fpm php artisan view:cache

# Verificar estado
info "Verificando estado de los contenedores..."
docker compose --env-file .env ps

info ""
info "✅ Despliegue completado!"
info ""
info "Servicios disponibles:"
info "  - API: https://api.notificaciones.space"
info "  - Dashboard: https://dashboard.notificaciones.space"
info ""
info "Próximos pasos:"
info "  1. Verificar que los DNS estén configurados"
info "  2. Verificar que Caddy obtenga los certificados SSL automáticamente"
info "  3. Verificar que los servicios responden correctamente"
info ""
info "Para ver los logs:"
info "  docker compose --env-file .env logs -f"

