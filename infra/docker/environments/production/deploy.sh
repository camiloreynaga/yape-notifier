#!/bin/bash

# ============================================
# Production Deployment Script
# ============================================
# Uso: ./deploy.sh [--no-cache]
#
# Notas importantes:
# - En producción, primero se detienen los contenedores, luego se construyen las imágenes
# - APP_KEY nunca se regenera automáticamente en producción (debe existir en .env)
# - Se usa wait loop activo para PostgreSQL en lugar de sleep
# - El script es idempotente y seguro para ejecutarse múltiples veces

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

# Verificar si se solicita build sin cache
BUILD_NO_CACHE=""
if [ "$1" == "--no-cache" ]; then
    BUILD_NO_CACHE="--no-cache"
    warn "Modo --no-cache activado (build sin cache)"
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

# Verificar que APP_KEY existe y no está vacío
# En producción, APP_KEY NUNCA debe regenerarse automáticamente
if ! grep -q "^APP_KEY=base64:.*" .env || grep -q "^APP_KEY=$" .env; then
    error "APP_KEY no está configurado en .env"
    error "En producción, APP_KEY debe existir y ser válido."
    error "Por favor, edita .env y configura APP_KEY con un valor seguro"
    error "Puedes generarlo localmente con: php artisan key:generate"
    exit 1
fi

info "APP_KEY verificado correctamente"

# PASO 1: Detener contenedores existentes (con --remove-orphans para limpiar servicios huérfanos)
# Esto debe hacerse ANTES de construir nuevas imágenes para evitar conflictos
info "Deteniendo contenedores existentes..."
docker compose --env-file .env down --remove-orphans

# PASO 2: Construir imágenes Docker
# Se construye después de detener para evitar problemas con contenedores en ejecución
info "Construyendo imágenes Docker..."
if [ -n "$BUILD_NO_CACHE" ]; then
    docker compose --env-file .env build $BUILD_NO_CACHE
else
    docker compose --env-file .env build
fi

# PASO 3: Iniciar contenedores
info "Iniciando contenedores..."
docker compose --env-file .env up -d

# PASO 4: Esperar activamente a que PostgreSQL esté listo
# Reemplazamos sleep por un wait loop que valida realmente la disponibilidad
info "Esperando a que PostgreSQL esté listo..."
MAX_ATTEMPTS=30
ATTEMPT=0
POSTGRES_READY=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if docker compose --env-file .env exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
        POSTGRES_READY=1
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done

echo ""

if [ $POSTGRES_READY -eq 0 ]; then
    error "PostgreSQL no está disponible después de $MAX_ATTEMPTS intentos"
    error "Revisa los logs: docker compose --env-file .env logs postgres"
    exit 1
fi

info "PostgreSQL está listo"

# PASO 5: Asegurar permisos y directorios (antes de migraciones)
# Esto debe hacerse antes de ejecutar migraciones para evitar errores de permisos
info "Verificando permisos y directorios..."
docker compose --env-file .env exec -T php-fpm sh -c "mkdir -p /var/www/storage/framework/{sessions,views,cache} /var/www/storage/logs /var/www/bootstrap/cache && chown -R www-data:www-data /var/www/storage /var/www/bootstrap/cache && chmod -R 775 /var/www/storage /var/www/bootstrap/cache"

# PASO 6: Ejecutar migraciones (solo después de que PostgreSQL esté listo)
info "Ejecutando migraciones..."
docker compose --env-file .env exec -T php-fpm php artisan migrate --force

# PASO 7: Optimizar Laravel
info "Optimizando Laravel..."
docker compose --env-file .env exec -T php-fpm php artisan config:cache
docker compose --env-file .env exec -T php-fpm php artisan route:cache
# No cachear vistas en producción inicialmente (puede causar problemas si faltan directorios)
# docker compose --env-file .env exec -T php-fpm php artisan view:cache

# PASO 8: Verificar estado
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

