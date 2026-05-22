#!/bin/bash

# ============================================
# Staging Deployment Script
# ============================================
# Uso: ./deploy.sh

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

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
    error "Este script debe ejecutarse desde infra/docker/environments/staging"
    exit 1
fi

info "Iniciando despliegue en staging..."

# Verificar archivo .env
if [ ! -f ".env" ]; then
    warn "Archivo .env no encontrado"
    if [ -f ".env.example" ]; then
        warn "Copiando desde .env.example..."
        cp .env.example .env
        error "Por favor configura .env antes de continuar"
        exit 1
    fi
fi

# Verificar que DB_PASSWORD está configurado
if ! grep -q "^DB_PASSWORD=.*" .env || grep -q "^DB_PASSWORD=$" .env || grep -q "TU_CONTRASEÑA" .env; then
    error "DB_PASSWORD no está configurado en .env"
    exit 1
fi

# ============================================
# VALIDACIÓN: PHP 8.2 LTS y composer.lock
# ============================================
info "Validando compatibilidad con PHP 8.2 LTS..."
API_DIR="../../../../apps/api"

if [ ! -f "$API_DIR/composer.json" ]; then
    error "No se encontró composer.json en $API_DIR"
    exit 1
fi

# Verificar que composer.json especifica PHP 8.2
PHP_VERSION_REQUIREMENT=$(grep -o '"php":\s*"[^"]*"' "$API_DIR/composer.json" | head -1)
if ! echo "$PHP_VERSION_REQUIREMENT" | grep -q "8\.2"; then
    error "❌ composer.json no especifica PHP 8.2 LTS"
    error "Versión encontrada: $PHP_VERSION_REQUIREMENT"
    exit 1
fi

if [ ! -f "$API_DIR/composer.lock" ]; then
    error "composer.lock no encontrado en $API_DIR"
    error "Ejecuta 'composer update' en apps/api usando PHP 8.2 y haz commit del composer.lock"
    exit 1
fi

# Validar compatibilidad con PHP 8.2 LTS
cd "$API_DIR"
VALIDATION_OUTPUT=$(docker run --rm -v "$(pwd):/app" -w /app \
    php:8.2-cli sh -c "curl -sS https://getcomposer.org/installer | php && php composer.phar install --dry-run --no-dev --no-interaction --prefer-dist" 2>&1 || true)

HAS_ERRORS=false
if echo "$VALIDATION_OUTPUT" | grep -q "lock file is not up to date\|not present in the lock file\|Required package.*is not present in the lock file"; then
    HAS_ERRORS=true
    ERROR_TYPE="desactualizado"
elif echo "$VALIDATION_OUTPUT" | grep -q "does not satisfy that requirement\|Your lock file does not contain a compatible set\|requires php >=8\.[34]"; then
    HAS_ERRORS=true
    ERROR_TYPE="incompatible con PHP 8.2"
fi

if [ "$HAS_ERRORS" = true ]; then
    error "❌ composer.lock está $ERROR_TYPE"
    echo "$VALIDATION_OUTPUT" | grep -E "lock file|not present|Required package|does not satisfy|compatible set|requires php" | head -3
    error ""
    error "SOLUCIÓN: Usar PHP 8.2 LTS para actualizar composer.lock"
    error "  docker run --rm -v \$(pwd):/app -w /app php:8.2-cli sh -c 'curl -sS https://getcomposer.org/installer | php && php composer.phar update --no-interaction'"
    cd - > /dev/null
    exit 1
fi
cd - > /dev/null
info "✅ composer.lock es compatible con PHP 8.2 LTS"

# Construir imágenes con BuildKit para cache optimizado
info "Construyendo imágenes Docker (con BuildKit para cache optimizado)..."
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
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

# Limpiar caches (en staging no cacheamos)
info "Limpiando caches..."
docker compose --env-file .env exec -T php-fpm php artisan config:clear
docker compose --env-file .env exec -T php-fpm php artisan route:clear
docker compose --env-file .env exec -T php-fpm php artisan view:clear

# Verificar estado
info "Verificando estado de los contenedores..."
docker compose --env-file .env ps

info ""
info "✅ Despliegue completado!"
info ""
info "Servicios disponibles:"
info "  - API: http://localhost:8080/up"
info "  - Dashboard: http://localhost:8080/"
info ""
info "Para ver los logs:"
info "  docker compose --env-file .env logs -f"

