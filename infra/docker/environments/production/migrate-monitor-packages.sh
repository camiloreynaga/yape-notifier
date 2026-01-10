#!/bin/bash

# ============================================
# Script Helper: Migrar Paquetes Globales
# ============================================
# Script para migrar paquetes globales a commerces de forma segura
# Uso: ./migrate-monitor-packages.sh [--delete-global] [--force]
# ============================================

set -e

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
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

section() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.yml" ] || [ ! -f ".env" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/production"
    exit 1
fi

# Parsear argumentos
DELETE_GLOBAL=false
FORCE=false

for arg in "$@"; do
    case $arg in
        --delete-global)
            DELETE_GLOBAL=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        *)
            warn "Argumento desconocido: $arg"
            ;;
    esac
done

section "🔍 PASO 1: Verificar Estado Actual"

info "Verificando paquetes globales..."
GLOBAL_COUNT=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo \App\Models\MonitorPackage::whereNull('commerce_id')->count();" 2>/dev/null | grep -E '^[0-9]+$' || echo "0")

if [ "$GLOBAL_COUNT" = "0" ]; then
    warn "No se encontraron paquetes globales para migrar."
    info "Los paquetes ya fueron migrados o no existen."
    exit 0
fi

info "✅ Encontrados $GLOBAL_COUNT paquetes globales"

info "Verificando commerces..."
COMMERCE_COUNT=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo \App\Models\Commerce::count();" 2>/dev/null | grep -E '^[0-9]+$' || echo "0")

if [ "$COMMERCE_COUNT" = "0" ]; then
    error "No se encontraron commerces."
    error "Crea un commerce primero para migrar los paquetes."
    exit 1
fi

info "✅ Encontrados $COMMERCE_COUNT commerces"

section "💾 PASO 2: Crear Backup"

if [ -f "./backup.sh" ]; then
    info "Creando backup de la base de datos..."
    ./backup.sh
    BACKUP_FILE=$(ls -t backups/backup_*.sql.gz 2>/dev/null | head -1)
    if [ -n "$BACKUP_FILE" ]; then
        info "✅ Backup creado: $BACKUP_FILE"
        info "💡 Guarda esta ruta para rollback si es necesario: $BACKUP_FILE"
    else
        warn "⚠️  No se pudo verificar el backup. Continuando de todas formas..."
    fi
else
    warn "⚠️  Script de backup no encontrado. Se recomienda crear backup manualmente."
    if [ "$FORCE" != "true" ]; then
        read -p "¿Deseas continuar sin backup? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            info "Operación cancelada."
            exit 0
        fi
    fi
fi

section "🚀 PASO 3: Ejecutar Migración"

# Construir comando
CMD="php artisan monitor-packages:migrate-global"
if [ "$DELETE_GLOBAL" = "true" ]; then
    CMD="$CMD --delete-global"
    warn "⚠️  ADVERTENCIA: Se eliminarán los paquetes globales después de migrar."
    warn "⚠️  Esta acción es IRREVERSIBLE."
fi
if [ "$FORCE" = "true" ]; then
    CMD="$CMD --force"
fi

if [ "$FORCE" != "true" ] && [ "$DELETE_GLOBAL" = "true" ]; then
    read -p "¿Estás seguro de que deseas continuar? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        info "Operación cancelada."
        exit 0
    fi
fi

info "Ejecutando: $CMD"
docker compose --env-file .env exec php-fpm $CMD

if [ $? -eq 0 ]; then
    info "✅ Migración completada exitosamente"
else
    error "❌ Error durante la migración"
    error "💡 Revisa los logs: docker compose --env-file .env logs php-fpm"
    exit 1
fi

section "✅ PASO 4: Verificar Resultados"

info "Verificando paquetes por commerce..."
docker compose --env-file .env exec -T php-fpm php artisan tinker <<'EOF'
\App\Models\Commerce::all()->each(function($commerce) {
    $count = \App\Models\MonitorPackage::where('commerce_id', $commerce->id)->count();
    echo "Commerce {$commerce->name} (ID: {$commerce->id}): {$count} paquetes\n";
});
EOF

if [ "$DELETE_GLOBAL" != "true" ]; then
    info "Verificando que los paquetes globales siguen existiendo..."
    REMAINING_GLOBAL=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo \App\Models\MonitorPackage::whereNull('commerce_id')->count();" 2>/dev/null | grep -E '^[0-9]+$' || echo "0")
    if [ "$REMAINING_GLOBAL" = "$GLOBAL_COUNT" ]; then
        info "✅ Los paquetes globales siguen existiendo ($REMAINING_GLOBAL)"
    else
        warn "⚠️  El número de paquetes globales cambió: $REMAINING_GLOBAL (esperado: $GLOBAL_COUNT)"
    fi
else
    info "Verificando que los paquetes globales fueron eliminados..."
    REMAINING_GLOBAL=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo \App\Models\MonitorPackage::whereNull('commerce_id')->count();" 2>/dev/null | grep -E '^[0-9]+$' || echo "0")
    if [ "$REMAINING_GLOBAL" = "0" ]; then
        info "✅ Los paquetes globales fueron eliminados correctamente"
    else
        warn "⚠️  Aún quedan $REMAINING_GLOBAL paquetes globales"
    fi
fi

section "📋 Resumen"

info "✅ Migración completada"
info "📦 Paquetes globales encontrados: $GLOBAL_COUNT"
info "🏢 Commerces procesados: $COMMERCE_COUNT"
if [ -n "$BACKUP_FILE" ]; then
    info "💾 Backup guardado en: $BACKUP_FILE"
fi

if [ "$DELETE_GLOBAL" != "true" ]; then
    echo ""
    warn "💡 RECORDATORIO: Los paquetes globales aún existen."
    warn "💡 Ejecuta con --delete-global para eliminarlos después de verificar que todo funciona."
fi

echo ""
info "🎉 Proceso completado exitosamente!"

