#!/bin/bash

# ============================================
# Production Update Script
# ============================================
# Script profesional para actualizar el sistema en producción
# con backup automático y rollback plan
#
# Uso: ./update.sh
# Requisitos: Estar en el directorio de producción
# ============================================

set -e  # Salir si hay error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
PROJECT_DIR="$(pwd)"
DATE=$(date +%Y%m%d_%H%M%S)

# Detectar ruta de backup escribible
# Prioridad: 1) Variable BACKUP_DIR, 2) ./backups, 3) ~/backups/yape-notifier, 4) /tmp/yape-notifier-backups
if [ -n "$BACKUP_DIR" ]; then
    # Usar la variable de entorno si está configurada
    TARGET_DIR="$BACKUP_DIR"
elif [ -w "$PROJECT_DIR" ]; then
    # Usar directorio dentro del proyecto si es escribible
    TARGET_DIR="$PROJECT_DIR/backups"
elif [ -w "$HOME" ]; then
    # Usar directorio en el home del usuario
    TARGET_DIR="$HOME/backups/yape-notifier"
else
    # Último recurso: /tmp
    TARGET_DIR="/tmp/yape-notifier-backups"
fi

BACKUP_DIR="$TARGET_DIR"
BACKUP_FILE="$BACKUP_DIR/backup_pre_update_$DATE.sql.gz"
ROLLBACK_FILE="$BACKUP_DIR/rollback_$DATE.sh"

# Funciones de logging
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
    error "Y debe existir el archivo .env"
    exit 1
fi

# Intentar crear el directorio de backup
if mkdir -p "$BACKUP_DIR" 2>/dev/null; then
    # Verificar que realmente podemos escribir
    if [ -w "$BACKUP_DIR" ]; then
        info "Usando directorio de backup: $BACKUP_DIR"
    else
        error "No se puede escribir en: $BACKUP_DIR"
        error "Configura BACKUP_DIR con una ruta escribible:"
        error "  export BACKUP_DIR=/ruta/escribible && ./update.sh"
        exit 1
    fi
else
    error "No se pudo crear el directorio de backup: $BACKUP_DIR"
    error "Configura BACKUP_DIR con una ruta escribible:"
    error "  export BACKUP_DIR=/ruta/escribible && ./update.sh"
    exit 1
fi

# Verificar que docker compose está disponible
if ! command -v docker &> /dev/null; then
    error "Docker no está instalado o no está en PATH"
    exit 1
fi

info "=========================================="
info "  Yape Notifier - Actualización de Producción"
info "  Fecha: $(date)"
info "=========================================="
echo ""

# ============================================
# PASO 1: BACKUP DE BASE DE DATOS
# ============================================
step "1/6: Creando backup de base de datos..."

# Verificar que la base de datos está corriendo
if ! docker compose --env-file .env ps db | grep -q "Up"; then
    warn "Base de datos no está corriendo. Iniciando..."
    docker compose --env-file .env up -d db
    info "Esperando a que la base de datos esté lista..."
    sleep 10
fi

# Realizar backup comprimido
info "Creando backup: $BACKUP_FILE"
if docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > "$BACKUP_FILE"; then
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    info "✅ Backup creado exitosamente: $BACKUP_SIZE"
else
    error "❌ Error al crear backup. ABORTANDO actualización."
    exit 1
fi

# Verificar integridad del backup
info "Verificando integridad del backup..."
if gunzip -t "$BACKUP_FILE" 2>/dev/null; then
    info "✅ Backup verificado correctamente"
else
    error "❌ El backup está corrupto. ABORTANDO actualización."
    exit 1
fi

# ============================================
# PASO 2: CREAR SCRIPT DE ROLLBACK
# ============================================
step "2/6: Creando script de rollback..."

cat > "$ROLLBACK_FILE" << EOF
#!/bin/bash
# Script de rollback generado automáticamente
# Fecha: $(date)
# Backup: $BACKUP_FILE

set -e

echo "🔄 Iniciando rollback..."
cd "$PROJECT_DIR"

# Detener servicios
echo "Deteniendo servicios..."
docker compose --env-file .env stop php-fpm nginx-api dashboard caddy || true

# Restaurar backup
echo "Restaurando backup desde: $BACKUP_FILE"
docker compose --env-file .env up -d db
sleep 5
gunzip < "$BACKUP_FILE" | docker compose --env-file .env exec -T db psql -U postgres yape_notifier

# Reiniciar servicios
echo "Reiniciando servicios..."
docker compose --env-file .env up -d

echo "✅ Rollback completado"
EOF

chmod +x "$ROLLBACK_FILE"
info "✅ Script de rollback creado: $ROLLBACK_FILE"

# ============================================
# PASO 3: VERIFICAR ESTADO ACTUAL
# ============================================
step "3/6: Verificando estado actual del sistema..."

# Verificar estado de contenedores
info "Estado de contenedores:"
docker compose --env-file .env ps

# Verificar migraciones actuales
info "Migraciones actuales en la base de datos:"
docker compose --env-file .env exec -T php-fpm php artisan migrate:status || warn "No se pudo verificar estado de migraciones"

# Verificar que la API responde
if curl -f -s http://localhost/up > /dev/null 2>&1 || curl -f -s https://api.notificaciones.space/up > /dev/null 2>&1; then
    info "✅ API respondiendo correctamente"
else
    warn "⚠️  API no responde (puede ser normal si está detrás de proxy)"
fi

# ============================================
# PASO 4: ACTUALIZAR CÓDIGO
# ============================================
step "4/6: Actualizando código..."

# Preguntar confirmación
echo ""
warn "⚠️  IMPORTANTE: Asegúrate de haber actualizado el código en el servidor"
warn "   (git pull, o copiar archivos nuevos)"
read -p "¿El código ya está actualizado en el servidor? (s/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    error "Actualiza el código primero y vuelve a ejecutar este script"
    exit 1
fi

# Reconstruir imágenes Docker
info "Reconstruyendo imágenes Docker..."
if docker compose --env-file .env build --no-cache; then
    info "✅ Imágenes reconstruidas"
else
    error "❌ Error al reconstruir imágenes"
    exit 1
fi

# ============================================
# PASO 5: EJECUTAR MIGRACIONES
# ============================================
step "5/6: Ejecutando migraciones..."

# Verificar qué migraciones se van a ejecutar
info "Migraciones pendientes:"
docker compose --env-file .env exec -T php-fpm php artisan migrate:status | grep "Pending" || info "No hay migraciones pendientes"

# Preguntar confirmación
read -p "¿Continuar con la ejecución de migraciones? (s/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    warn "Migraciones canceladas por el usuario"
    exit 0
fi

# Ejecutar migraciones
info "Ejecutando migraciones..."
if docker compose --env-file .env exec -T php-fpm php artisan migrate --force; then
    info "✅ Migraciones ejecutadas exitosamente"
else
    error "❌ Error al ejecutar migraciones"
    error "Ejecuta el rollback: $ROLLBACK_FILE"
    exit 1
fi

# Verificar estado final de migraciones
info "Estado final de migraciones:"
docker compose --env-file .env exec -T php-fpm php artisan migrate:status

# ============================================
# PASO 6: REINICIAR SERVICIOS Y VERIFICAR
# ============================================
step "6/6: Reiniciando servicios y verificando..."

# Reiniciar servicios
info "Reiniciando servicios..."
docker compose --env-file .env up -d

# Esperar a que los servicios estén listos
info "Esperando a que los servicios estén listos..."
sleep 15

# Verificar estado de contenedores
info "Estado de contenedores después de la actualización:"
docker compose --env-file .env ps

# Verificar healthchecks
info "Verificando healthchecks..."
HEALTHY_COUNT=$(docker compose --env-file .env ps --format json | grep -c '"Health":"healthy"' || echo "0")
TOTAL_COUNT=$(docker compose --env-file .env ps --format json | grep -c '"Name"' || echo "0")

if [ "$HEALTHY_COUNT" -eq "$TOTAL_COUNT" ]; then
    info "✅ Todos los servicios están healthy"
else
    warn "⚠️  Algunos servicios no están healthy. Revisa los logs:"
    warn "   docker compose --env-file .env logs"
fi

# Verificar API
info "Verificando API..."
if curl -f -s http://localhost/up > /dev/null 2>&1 || curl -f -s https://api.notificaciones.space/up > /dev/null 2>&1; then
    info "✅ API respondiendo correctamente"
else
    warn "⚠️  API no responde. Revisa los logs."
fi

# Limpiar caches
info "Limpiando caches..."
docker compose --env-file .env exec -T php-fpm php artisan config:clear || true
docker compose --env-file .env exec -T php-fpm php artisan route:clear || true
docker compose --env-file .env exec -T php-fpm php artisan view:clear || true
docker compose --env-file .env exec -T php-fpm php artisan cache:clear || true

# ============================================
# RESUMEN
# ============================================
echo ""
info "=========================================="
info "  ✅ ACTUALIZACIÓN COMPLETADA"
info "=========================================="
echo ""
info "Backup guardado en: $BACKUP_FILE"
info "Script de rollback: $ROLLBACK_FILE"
echo ""
info "Para hacer rollback si es necesario:"
info "  $ROLLBACK_FILE"
echo ""
info "Para ver logs:"
info "  docker compose --env-file .env logs -f"
echo ""


