#!/bin/bash

# ============================================
# Yape Notifier - Update Architecture Script
# ============================================
# Actualiza el backend con la nueva arquitectura QR
# - Ejecuta migraciones pendientes
# - Migra dispositivos existentes (opcional)
# - Verifica el estado del sistema
# ============================================

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================
# 1. Verificar entorno
# ============================================
log_info "Verificando entorno..."

if [ ! -f .env ]; then
    log_error "Archivo .env no encontrado"
    exit 1
fi

if ! docker compose --env-file .env ps | grep -q "php-fpm"; then
    log_error "Contenedores no están corriendo"
    log_info "Ejecuta: docker compose --env-file .env up -d"
    exit 1
fi

log_success "Entorno OK"

# ============================================
# 2. Backup de base de datos
# ============================================
log_info "Creando backup de base de datos..."

BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"

docker exec yape-notifier-postgres-prod pg_dump \
    -U yapenotifier \
    -d yapenotifier_prod \
    > "$BACKUP_FILE"

log_success "Backup creado: $BACKUP_FILE"

# ============================================
# 3. Verificar migraciones pendientes
# ============================================
log_info "Verificando migraciones pendientes..."

MIGRATIONS=$(docker compose --env-file .env exec -T php-fpm php artisan migrate:status | grep "Pending" || true)

if [ -z "$MIGRATIONS" ]; then
    log_info "No hay migraciones pendientes"
else
    log_warning "Migraciones pendientes encontradas:"
    echo "$MIGRATIONS"
    
    read -p "¿Ejecutar migraciones? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "Ejecutando migraciones..."
        docker compose --env-file .env exec -T php-fpm php artisan migrate --force
        log_success "Migraciones ejecutadas"
    else
        log_warning "Migraciones omitidas"
    fi
fi

# ============================================
# 4. Verificar schema de devices
# ============================================
log_info "Verificando schema de devices..."

SCHEMA=$(docker exec yape-notifier-postgres-prod psql -U yapenotifier -d yapenotifier_prod -t -c "\d devices" | grep -E "(user_id|commerce_id)")

if echo "$SCHEMA" | grep -q "user_id.*bigint"; then
    log_success "user_id está presente"
else
    log_error "user_id no encontrado en schema"
    exit 1
fi

if echo "$SCHEMA" | grep -q "commerce_id.*bigint"; then
    log_success "commerce_id está presente"
else
    log_error "commerce_id no encontrado en schema"
    exit 1
fi

# ============================================
# 5. Diagnóstico de dispositivos
# ============================================
log_info "Diagnosticando dispositivos..."

DEVICE_STATS=$(docker exec yape-notifier-postgres-prod psql -U yapenotifier -d yapenotifier_prod -t -c "
SELECT 
    COUNT(*) as total,
    COUNT(commerce_id) as con_comercio,
    COUNT(*) - COUNT(commerce_id) as sin_comercio
FROM devices;
")

TOTAL=$(echo "$DEVICE_STATS" | awk '{print $1}')
CON_COMERCIO=$(echo "$DEVICE_STATS" | awk '{print $2}')
SIN_COMERCIO=$(echo "$DEVICE_STATS" | awk '{print $3}')

log_info "Dispositivos totales: $TOTAL"
log_info "Con commerce_id: $CON_COMERCIO"
log_warning "Sin commerce_id: $SIN_COMERCIO"

# ============================================
# 6. Migración de dispositivos (opcional)
# ============================================
if [ "$SIN_COMERCIO" -gt 0 ]; then
    log_warning "Hay $SIN_COMERCIO dispositivos sin commerce_id"
    
    echo ""
    echo "Opciones:"
    echo "  1) Auto-reparación pasiva (recomendado) - Se reparan al hacer login"
    echo "  2) Migración proactiva - Sincronizar commerce_id ahora"
    echo "  3) Omitir - Continuar sin migrar"
    echo ""
    
    read -p "Selecciona una opción (1/2/3): " -n 1 -r
    echo
    
    case $REPLY in
        1)
            log_info "Opción seleccionada: Auto-reparación pasiva"
            log_info "Los dispositivos se repararán automáticamente al hacer login"
            ;;
        2)
            log_info "Opción seleccionada: Migración proactiva"
            log_info "Sincronizando commerce_id de usuarios a dispositivos..."
            
            UPDATED=$(docker exec yape-notifier-postgres-prod psql -U yapenotifier -d yapenotifier_prod -t -c "
UPDATE devices d
SET 
    commerce_id = u.commerce_id,
    updated_at = NOW()
FROM users u
WHERE 
    d.user_id = u.id
    AND d.commerce_id IS NULL
    AND u.commerce_id IS NOT NULL;
" | grep "UPDATE" | awk '{print $2}')
            
            log_success "Dispositivos actualizados: $UPDATED"
            ;;
        3)
            log_info "Migración omitida"
            ;;
        *)
            log_warning "Opción inválida, omitiendo migración"
            ;;
    esac
fi

# ============================================
# 7. Verificar estado final
# ============================================
log_info "Verificando estado final..."

FINAL_STATS=$(docker exec yape-notifier-postgres-prod psql -U yapenotifier -d yapenotifier_prod -t -c "
SELECT 
    COUNT(*) as total,
    COUNT(commerce_id) as con_comercio,
    COUNT(*) - COUNT(commerce_id) as sin_comercio,
    COUNT(*) FILTER (WHERE commerce_id IS NOT NULL AND user_id IS NULL) as modo_capturador
FROM devices;
")

TOTAL=$(echo "$FINAL_STATS" | awk '{print $1}')
CON_COMERCIO=$(echo "$FINAL_STATS" | awk '{print $2}')
SIN_COMERCIO=$(echo "$FINAL_STATS" | awk '{print $3}')
MODO_CAPTURADOR=$(echo "$FINAL_STATS" | awk '{print $4}')

echo ""
log_success "Estado final:"
log_info "  Total dispositivos: $TOTAL"
log_info "  Con commerce_id: $CON_COMERCIO"
log_info "  Sin commerce_id: $SIN_COMERCIO"
log_info "  Modo capturador: $MODO_CAPTURADOR"

# ============================================
# 8. Verificar rutas de API
# ============================================
log_info "Verificando rutas de API..."

ROUTES=$(docker compose --env-file .env exec -T php-fpm php artisan route:list | grep -E "(device-link|notifications)" || true)

if echo "$ROUTES" | grep -q "device-link/link-by-code"; then
    log_success "Ruta de vinculación OK"
else
    log_error "Ruta de vinculación no encontrada"
fi

if echo "$ROUTES" | grep -q "POST.*api/notifications"; then
    log_success "Ruta de notificaciones OK"
else
    log_error "Ruta de notificaciones no encontrada"
fi

# ============================================
# 9. Limpiar cache
# ============================================
log_info "Limpiando cache..."

docker compose --env-file .env exec -T php-fpm php artisan config:clear
docker compose --env-file .env exec -T php-fpm php artisan route:clear
docker compose --env-file .env exec -T php-fpm php artisan cache:clear

log_success "Cache limpiado"

# ============================================
# 10. Resumen final
# ============================================
echo ""
echo "============================================"
log_success "Actualización completada"
echo "============================================"
echo ""
echo "Próximos pasos:"
echo ""
echo "1. Verificar logs:"
echo "   docker compose --env-file .env logs -f php-fpm"
echo ""
echo "2. Probar endpoint de vinculación:"
echo "   curl https://api.notificaciones.space/api/device-link/generate-code"
echo ""
echo "3. Actualizar Android app:"
echo "   - Cambiar BASE_URL a https://api.notificaciones.space/"
echo "   - Compilar APK: ./gradlew assembleRelease"
echo ""
echo "4. Distribuir APK a usuarios"
echo ""
echo "Documentación completa:"
echo "  docs/02-deployment/DIGITAL_OCEAN_DEPLOYMENT.md"
echo ""

if [ "$SIN_COMERCIO" -gt 0 ]; then
    log_warning "Hay $SIN_COMERCIO dispositivos sin commerce_id"
    log_info "Se auto-repararán al hacer login (si seleccionaste opción 1)"
fi

echo ""
log_success "¡Listo para producción! 🚀"


