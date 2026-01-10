#!/bin/bash

# ============================================
# Script de Rollback Limpio
# ============================================
# Este script hace un rollback COMPLETO limpiando
# la base de datos antes de restaurar el backup
# ============================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.yml" ] || [ ! -f ".env" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/production"
    exit 1
fi

# Verificar que se pasó el archivo de backup
if [ -z "$1" ]; then
    error "Uso: $0 <archivo_backup.sql.gz>"
    error ""
    error "Ejemplo:"
    error "  $0 ./backups/backup_pre_update_20260109_173003.sql.gz"
    error ""
    error "Backups disponibles:"
    ls -lh ./backups/backup_*.sql.gz 2>/dev/null || echo "  No hay backups disponibles"
    exit 1
fi

BACKUP_FILE="$1"

# Verificar que el archivo existe
if [ ! -f "$BACKUP_FILE" ]; then
    error "El archivo de backup no existe: $BACKUP_FILE"
    exit 1
fi

# Verificar integridad del backup
info "Verificando integridad del backup..."
if ! gunzip -t "$BACKUP_FILE" 2>/dev/null; then
    error "El backup está corrupto o no es un archivo gzip válido"
    exit 1
fi
info "✅ Backup verificado correctamente"

# Mostrar información del backup
BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
BACKUP_DATE=$(stat -c %y "$BACKUP_FILE" 2>/dev/null || stat -f "%Sm" "$BACKUP_FILE" 2>/dev/null || echo "desconocida")

echo ""
warn "=========================================="
warn "  ⚠️  ROLLBACK COMPLETO DE BASE DE DATOS"
warn "=========================================="
echo ""
warn "Backup a restaurar:"
warn "  Archivo: $BACKUP_FILE"
warn "  Tamaño: $BACKUP_SIZE"
warn "  Fecha: $BACKUP_DATE"
echo ""
warn "⚠️  ADVERTENCIA: Esta operación:"
warn "  1. ELIMINARÁ COMPLETAMENTE la base de datos actual"
warn "  2. Restaurará el backup seleccionado"
warn "  3. Perderás TODOS los datos posteriores al backup"
echo ""
read -p "¿Estás SEGURO de que quieres continuar? (escribe 'SI' en mayúsculas): " -r
echo ""

if [ "$REPLY" != "SI" ]; then
    warn "Rollback cancelado por el usuario"
    exit 0
fi

# ============================================
# PASO 1: DETENER SERVICIOS
# ============================================
step "1/5: Deteniendo servicios..."
docker compose --env-file .env stop php-fpm nginx-api dashboard caddy reverb || true
info "✅ Servicios detenidos"

# ============================================
# PASO 2: ASEGURAR QUE LA BD ESTÉ CORRIENDO
# ============================================
step "2/5: Iniciando base de datos..."
docker compose --env-file .env up -d db
info "Esperando a que la base de datos esté lista..."
sleep 5

# Verificar que esté healthy
if ! docker compose --env-file .env ps db | grep -q "healthy"; then
    warn "Base de datos no está healthy, esperando 10 segundos más..."
    sleep 10
fi

info "✅ Base de datos lista"

# ============================================
# PASO 3: LIMPIAR BASE DE DATOS COMPLETAMENTE
# ============================================
step "3/5: Limpiando base de datos actual..."

info "Eliminando base de datos..."
docker compose --env-file .env exec -T db psql -U postgres -c "DROP DATABASE IF EXISTS yape_notifier;" || true

info "Recreando base de datos limpia..."
docker compose --env-file .env exec -T db psql -U postgres -c "CREATE DATABASE yape_notifier;" || true

info "✅ Base de datos limpiada"

# ============================================
# PASO 4: RESTAURAR BACKUP
# ============================================
step "4/5: Restaurando backup..."

info "Restaurando desde: $BACKUP_FILE"
if gunzip < "$BACKUP_FILE" | docker compose --env-file .env exec -T db psql -U postgres yape_notifier; then
    info "✅ Backup restaurado exitosamente"
else
    error "❌ Error al restaurar backup"
    error "La base de datos puede estar en un estado inconsistente"
    error "Intenta restaurar manualmente o contacta al equipo de soporte"
    exit 1
fi

# Verificar que la restauración fue exitosa
info "Verificando restauración..."
TABLE_COUNT=$(docker compose --env-file .env exec -T db psql -U postgres -d yape_notifier -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')

if [ "$TABLE_COUNT" -gt 0 ]; then
    info "✅ Restauración verificada: $TABLE_COUNT tablas restauradas"
else
    error "❌ La restauración puede haber fallado: 0 tablas encontradas"
    exit 1
fi

# ============================================
# PASO 5: REINICIAR SERVICIOS
# ============================================
step "5/5: Reiniciando servicios..."

docker compose --env-file .env up -d

info "Esperando a que los servicios estén listos..."
sleep 15

# Verificar estado de contenedores
info "Estado de contenedores:"
docker compose --env-file .env ps

# Verificar API
info "Verificando API..."
sleep 5
if curl -f -s http://localhost/up > /dev/null 2>&1 || curl -f -s https://api.notificaciones.space/up > /dev/null 2>&1; then
    info "✅ API respondiendo correctamente"
else
    warn "⚠️  API no responde inmediatamente (puede tardar unos segundos más)"
fi

# Verificar migraciones
info "Estado de migraciones:"
docker compose --env-file .env exec -T php-fpm php artisan migrate:status || warn "No se pudo verificar estado de migraciones"

# ============================================
# RESUMEN
# ============================================
echo ""
info "=========================================="
info "  ✅ ROLLBACK COMPLETADO EXITOSAMENTE"
info "=========================================="
echo ""
info "Base de datos restaurada desde: $BACKUP_FILE"
echo ""
info "Verificaciones recomendadas:"
info "  1. Verificar API: curl https://api.notificaciones.space/up"
info "  2. Verificar Dashboard: curl -I https://dashboard.notificaciones.space"
info "  3. Ver logs: docker compose --env-file .env logs -f"
info "  4. Verificar migraciones: docker compose --env-file .env exec php-fpm php artisan migrate:status"
echo ""

