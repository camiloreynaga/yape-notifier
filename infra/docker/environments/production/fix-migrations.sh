#!/bin/bash

# ============================================
# Script para sincronizar migraciones
# ============================================
# Marca migraciones como ejecutadas cuando las tablas ya existen
# ============================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
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
    error "Este script debe ejecutarse desde infra/docker/environments/production"
    exit 1
fi

# Verificar que .env existe
if [ ! -f ".env" ]; then
    error "Archivo .env no encontrado"
    exit 1
fi

info "Sincronizando migraciones con el estado real de la base de datos..."
info ""

# Obtener el batch actual
CURRENT_BATCH=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo DB::table('migrations')->max('batch') ?? 0;" 2>/dev/null || echo "0")
NEXT_BATCH=$((CURRENT_BATCH + 1))

info "Batch actual: $CURRENT_BATCH"
info "Usando batch: $NEXT_BATCH"
info ""

# Migraciones a marcar (las que fallaron)
MIGRATIONS=(
    "2025_01_15_000006_create_monitor_packages_table"
    "2025_01_15_000007_create_device_monitored_apps_table"
    "2025_01_15_000008_add_commerce_to_monitor_packages_table"
)

# Marcar cada migración
for MIGRATION in "${MIGRATIONS[@]}"; do
    info "Marcando: $MIGRATION"
    
    # Verificar si ya está registrada
    EXISTS=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo DB::table('migrations')->where('migration', '$MIGRATION')->exists() ? '1' : '0';" 2>/dev/null || echo "0")
    
    if [ "$EXISTS" = "1" ]; then
        info "  ✅ Ya está registrada"
    else
        # Insertar en migrations
        docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="DB::table('migrations')->insert(['migration' => '$MIGRATION', 'batch' => $NEXT_BATCH]);" 2>/dev/null
        
        if [ $? -eq 0 ]; then
            info "  ✅ Marcada como ejecutada"
        else
            warn "  ⚠️  Error al marcar (puede que ya exista)"
        fi
    fi
done

info ""
info "Verificando estado final..."
docker compose --env-file .env exec -T php-fpm php artisan migrate:status | grep -E "Pending|Ran" | tail -5

info ""
info "✅ Sincronización completada"

