#!/bin/bash

# ============================================
# Fix: Add is_active column to users table
# ============================================
# Soluciona el error: column "is_active" of relation "users" does not exist
#
# Uso: ./fix-is-active-column.sh
# Requisitos: Estar en el directorio de producción
# ============================================

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
if [ ! -f "docker-compose.yml" ] || [ ! -f ".env" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/production"
    error "Y debe existir el archivo .env"
    exit 1
fi

info "=========================================="
info "  Fix: Agregar columna is_active a users"
info "=========================================="
echo ""

# Verificar que la base de datos está corriendo
if ! docker compose --env-file .env ps db | grep -q "Up"; then
    error "Base de datos no está corriendo. Iniciando..."
    docker compose --env-file .env up -d db
    info "Esperando a que la base de datos esté lista..."
    sleep 10
fi

# Verificar si la columna ya existe
info "Verificando si la columna is_active existe..."
COLUMN_EXISTS=$(docker compose --env-file .env exec -T db psql -U postgres -d yape_notifier -tAc \
    "SELECT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name='users' AND column_name='is_active'
    );" 2>/dev/null || echo "false")

if [ "$COLUMN_EXISTS" = "t" ]; then
    info "✅ La columna is_active ya existe en la tabla users"
    info "No es necesario hacer nada más"
    exit 0
fi

warn "⚠️  La columna is_active NO existe. Agregándola..."

# Agregar la columna is_active
info "Agregando columna is_active a la tabla users..."
if docker compose --env-file .env exec -T db psql -U postgres -d yape_notifier <<EOF
-- Agregar columna is_active
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- Crear índice si no existe
CREATE INDEX IF NOT EXISTS users_is_active_index ON users(is_active);

-- Actualizar usuarios existentes a activos (si no tienen valor)
UPDATE users SET is_active = true WHERE is_active IS NULL;
EOF
then
    info "✅ Columna is_active agregada exitosamente"
else
    error "❌ Error al agregar la columna is_active"
    exit 1
fi

# Verificar si la migración 2026_01_10_000002 está registrada como fallida
info "Verificando estado de migraciones..."
MIGRATION_NAME="2026_01_10_000002_make_user_id_required_in_devices"
MIGRATION_EXISTS=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo DB::table('migrations')->where('migration', '$MIGRATION_NAME')->exists() ? '1' : '0';" 2>/dev/null || echo "0")

if [ "$MIGRATION_EXISTS" = "1" ]; then
    warn "⚠️  La migración $MIGRATION_NAME está registrada (probablemente falló)"
    warn "Puedes eliminarla para re-ejecutarla con la versión mejorada:"
    warn "  docker compose --env-file .env exec php-fpm php artisan tinker"
    warn "  DB::table('migrations')->where('migration', '$MIGRATION_NAME')->delete();"
    warn "  exit"
    warn ""
    warn "Luego ejecuta: docker compose --env-file .env exec php-fpm php artisan migrate"
else
    info "✅ La migración no está registrada (se ejecutará con la versión mejorada)"
fi

echo ""
info "=========================================="
info "  ✅ Fix completado exitosamente"
info "=========================================="
echo ""
info "Próximos pasos:"
info "  1. Ejecutar migraciones: docker compose --env-file .env exec php-fpm php artisan migrate"
info "  2. O continuar con el despliegue: ./deploy.sh o ./update.sh"
echo ""

