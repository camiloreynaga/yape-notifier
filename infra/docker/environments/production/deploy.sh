#!/bin/bash

# ============================================
# Production Deployment Script
# ============================================
# Uso: ./deploy.sh [--no-cache]
#
# Notas importantes:
# - En producción, primero se detienen los contenedores, luego se construyen las imágenes
# - APP_KEY se genera SOLO UNA VEZ si está vacío (primer despliegue), nunca se regenera si ya existe
# - El archivo .env debe existir previamente con los secretos configurados (no se copia desde .env.example)
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

# Verificar archivo .env (OBLIGATORIO - no se copia desde .env.example)
if [ ! -f ".env" ]; then
    error "Archivo .env no encontrado"
    error "El archivo .env es OBLIGATORIO y debe existir previamente con los secretos configurados"
    error "Por favor, crea el archivo .env con las variables necesarias antes de continuar"
    exit 1
fi

# Verificar que DB_PASSWORD está configurado
if ! grep -q "^DB_PASSWORD=.*" .env || grep -q "^DB_PASSWORD=$" .env || grep -q "TU_CONTRASEÑA" .env; then
    error "DB_PASSWORD no está configurado en .env"
    error "Por favor, edita .env y configura DB_PASSWORD con un valor seguro"
    exit 1
fi

# Verificar que las variables de base de datos estén configuradas correctamente
# Laravel necesita DB_HOST=db para conectarse al servicio PostgreSQL dentro de Docker
if ! grep -q "^DB_HOST=" .env; then
    warn "DB_HOST no está configurado en .env, agregando DB_HOST=db (valor correcto para Docker)"
    echo "DB_HOST=db" >> .env
elif ! grep -q "^DB_HOST=db" .env; then
    warn "DB_HOST está configurado pero no es 'db'. Para Docker debe ser 'db' (nombre del servicio)"
    warn "Valor actual: $(grep '^DB_HOST=' .env)"
fi

if ! grep -q "^DB_PORT=" .env; then
    warn "DB_PORT no está configurado en .env, agregando DB_PORT=5432 (default PostgreSQL)"
    echo "DB_PORT=5432" >> .env
fi

if ! grep -q "^DB_DATABASE=" .env; then
    error "DB_DATABASE no está configurado en .env"
    error "Por favor, edita .env y configura DB_DATABASE"
    exit 1
fi

if ! grep -q "^DB_USERNAME=" .env; then
    error "DB_USERNAME no está configurado en .env"
    error "Por favor, edita .env y configura DB_USERNAME"
    exit 1
fi

# Verificar APP_KEY y determinar si necesita generarse (solo si está vacío)
# En producción, APP_KEY se genera SOLO UNA VEZ en el primer despliegue si está vacío
APP_KEY_LINE=$(grep "^APP_KEY=" .env || echo "")
NEEDS_APP_KEY=0

if [ -z "$APP_KEY_LINE" ] || echo "$APP_KEY_LINE" | grep -q "^APP_KEY=$"; then
    warn "APP_KEY no está configurado o está vacío"
    warn "Se generará automáticamente después de construir las imágenes (primer despliegue)"
    NEEDS_APP_KEY=1
else
    # APP_KEY ya existe, verificar que sea válido
    if ! echo "$APP_KEY_LINE" | grep -q "^APP_KEY=base64:"; then
        error "APP_KEY existe pero no tiene un formato válido (debe comenzar con 'base64:')"
        error "Valor actual: $APP_KEY_LINE"
        exit 1
    fi
    info "APP_KEY verificado correctamente (ya existe, no se regenerará)"
fi

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
    if docker compose --env-file .env exec -T db pg_isready -U postgres > /dev/null 2>&1; then
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
    error "Revisa los logs: docker compose --env-file .env logs db"
    exit 1
fi

info "PostgreSQL está listo"

# PASO 5: Verificar que las variables de entorno se inyectaron correctamente en el contenedor
info "Verificando variables de entorno en el contenedor php-fpm..."
if docker compose --env-file .env exec -T php-fpm env | grep -q "^DB_HOST=db"; then
    info "✅ DB_HOST correctamente configurado en el contenedor"
else
    warn "⚠️  DB_HOST no está configurado como 'db' en el contenedor"
    warn "Variables DB_* encontradas en el contenedor:"
    docker compose --env-file .env exec -T php-fpm env | grep "^DB_" || warn "No se encontraron variables DB_*"
fi

# PASO 6: Generar APP_KEY si es necesario (solo si estaba vacío - primer despliegue)
if [ $NEEDS_APP_KEY -eq 1 ]; then
    info "Generando APP_KEY (primer despliegue)..."
    GENERATED_KEY=$(docker compose --env-file .env exec -T php-fpm php artisan key:generate --show 2>/dev/null | grep -o "base64:[^[:space:]]*" || echo "")
    
    if [ -z "$GENERATED_KEY" ]; then
        error "No se pudo generar APP_KEY. Verifica que los contenedores estén funcionando correctamente"
        error "Puedes generarlo manualmente con: docker compose --env-file .env exec php-fpm php artisan key:generate"
        exit 1
    fi
    
    # Persistir APP_KEY en .env
    if grep -q "^APP_KEY=" .env; then
        # Reemplazar línea existente vacía
        sed -i "s|^APP_KEY=.*|APP_KEY=$GENERATED_KEY|" .env
    else
        # Agregar nueva línea
        echo "APP_KEY=$GENERATED_KEY" >> .env
    fi
    
    info "APP_KEY generado y persistido en .env (solo se genera una vez)"
fi

# PASO 7: Asegurar permisos y directorios (antes de migraciones)
# Esto debe hacerse antes de ejecutar migraciones para evitar errores de permisos
info "Verificando permisos y directorios..."
docker compose --env-file .env exec -T php-fpm sh -c "mkdir -p /var/www/storage/framework/{sessions,views,cache} /var/www/storage/logs /var/www/bootstrap/cache && chown -R www-data:www-data /var/www/storage /var/www/bootstrap/cache && chmod -R 775 /var/www/storage /var/www/bootstrap/cache"

# PASO 8: Limpiar caches de Laravel antes de migraciones
# Esto asegura que Laravel use las variables de entorno actuales y no valores cacheados
info "Limpiando caches de Laravel..."
docker compose --env-file .env exec -T php-fpm php artisan config:clear
docker compose --env-file .env exec -T php-fpm php artisan cache:clear

# PASO 9: Ejecutar migraciones (solo después de que PostgreSQL esté listo y caches limpiados)
info "Ejecutando migraciones..."
docker compose --env-file .env exec -T php-fpm php artisan migrate --force

# PASO 10: Optimizar Laravel (después de migraciones)
info "Optimizando Laravel..."
docker compose --env-file .env exec -T php-fpm php artisan config:cache
docker compose --env-file .env exec -T php-fpm php artisan route:cache
# No cachear vistas en producción inicialmente (puede causar problemas si faltan directorios)
# docker compose --env-file .env exec -T php-fpm php artisan view:cache

# PASO 11: Verificar estado
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

