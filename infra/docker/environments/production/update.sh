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

# ============================================
# VALIDACIÓN CRÍTICA: PHP 8.2 LTS y composer.lock
# ============================================
# PHP 8.2 es LTS (Long Term Support) hasta diciembre 2026
# Esta validación asegura que composer.lock sea compatible con PHP 8.2
# y previene problemas de incompatibilidad durante actualizaciones
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
    error "Debe ser: \"php\": \">=8.2 <8.3\" para usar PHP 8.2 LTS"
    exit 1
fi
info "✅ composer.json especifica PHP 8.2 LTS correctamente"

if [ ! -f "$API_DIR/composer.lock" ]; then
    error "composer.lock no encontrado en $API_DIR"
    error "Ejecuta 'composer update' en apps/api usando PHP 8.2 y haz commit del composer.lock"
    exit 1
fi

# Validar que composer.lock sea compatible con PHP 8.2 LTS
info "Validando compatibilidad de composer.lock con PHP 8.2 LTS..."
cd "$API_DIR"
VALIDATION_OUTPUT=$(docker run --rm -v "$(pwd):/app" -w /app \
    php:8.2-cli sh -c "curl -sS https://getcomposer.org/installer | php && php composer.phar install --dry-run --no-dev --no-interaction --prefer-dist" 2>&1 || true)

# Detectar errores de compatibilidad
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
    error ""
    error "Detalles del error:"
    echo "$VALIDATION_OUTPUT" | grep -E "lock file|not present|Required package|does not satisfy|compatible set|requires php" | head -5
    error ""
    error "CAUSA: composer.lock fue generado con una versión de PHP diferente a 8.2"
    error ""
    error "SOLUCIÓN PROFESIONAL (usar PHP 8.2 LTS):"
    error "  1. cd apps/api"
    error "  2. docker run --rm -v \$(pwd):/app -w /app php:8.2-cli sh -c 'curl -sS https://getcomposer.org/installer | php && php composer.phar update --no-interaction'"
    error "  3. Verificar: docker run --rm -v \$(pwd):/app -w /app php:8.2-cli sh -c 'curl -sS https://getcomposer.org/installer | php && php composer.phar install --dry-run --no-dev --no-interaction'"
    error "  4. git add composer.lock && git commit -m 'fix: update composer.lock for PHP 8.2 LTS compatibility'"
    error "  5. git push"
    error "  6. Vuelve a ejecutar este script de actualización"
    error ""
    error "⚠️  IMPORTANTE: Siempre usa PHP 8.2 LTS para mantener consistencia con producción"
    cd - > /dev/null
    exit 1
fi
cd - > /dev/null
info "✅ composer.lock es compatible con PHP 8.2 LTS"

# Reconstruir imágenes Docker
# BuildKit está habilitado para usar cache mounts y optimizar builds
info "Reconstruyendo imágenes Docker (con BuildKit para cache optimizado)..."
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
if docker compose --env-file .env build --no-cache; then
    info "✅ Imágenes reconstruidas"
else
    error "❌ Error al reconstruir imágenes"
    exit 1
fi

# ============================================
# PASO 5: LIMPIAR CACHES Y REGENERAR CONFIGURACIÓN
# ============================================
step "5/6: Limpiando caches y regenerando configuración..."

# CRÍTICO: Limpiar TODOS los archivos de cache ANTES de ejecutar migraciones
# Esto elimina archivos generados en desarrollo que pueden contener referencias
# a dependencias de desarrollo (como nunomaduro/collision)
info "Limpiando archivos de cache de Laravel..."
docker compose --env-file .env exec -T php-fpm php artisan config:clear || true
docker compose --env-file .env exec -T php-fpm php artisan route:clear || true
docker compose --env-file .env exec -T php-fpm php artisan view:clear || true
docker compose --env-file .env exec -T php-fpm php artisan cache:clear || true

# Eliminar archivos de cache del bootstrap manualmente para asegurar limpieza completa
# Estos archivos pueden contener referencias a providers de desarrollo
info "Eliminando archivos de cache del bootstrap..."
docker compose --env-file .env exec -T php-fpm sh -c "rm -f /var/www/bootstrap/cache/packages.php /var/www/bootstrap/cache/services.php /var/www/bootstrap/cache/config.php 2>/dev/null || true" || true

# Regenerar package discovery DESPUÉS de limpiar caches
# Esto regenera packages.php solo con dependencias de producción instaladas
info "Regenerando package discovery (solo dependencias de producción)..."
docker compose --env-file .env exec -T php-fpm php artisan package:discover --ansi || warn "package:discover falló (puede ser normal si no hay packages nuevos)"

# ============================================
# PASO 6: EJECUTAR MIGRACIONES
# ============================================
step "6/7: Ejecutando migraciones..."

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

# Función para sincronizar migraciones desincronizadas
sync_migrations() {
    # Intentar ejecutar migraciones
    MIGRATE_OUTPUT=$(docker compose --env-file .env exec -T php-fpm php artisan migrate --force 2>&1 || true)
    
    # Detectar errores de "Duplicate table"
    if echo "$MIGRATE_OUTPUT" | grep -q "Duplicate table\|relation.*already exists"; then
        warn "⚠️  Detectado error de migraciones desincronizadas (tablas ya existen)"
        warn "Intentando sincronizar migraciones..."
        
        # Obtener el batch actual
        CURRENT_BATCH=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo DB::table('migrations')->max('batch') ?? 0;" 2>/dev/null || echo "0")
        NEXT_BATCH=$((CURRENT_BATCH + 1))
        
        # Extraer nombres de migraciones que fallaron
        FAILED_MIGRATIONS=$(echo "$MIGRATE_OUTPUT" | grep -oE "[0-9]{4}_[0-9]{2}_[0-9]{2}_[0-9]{6}_[a-z_]+" | sort -u || echo "")
        
        if [ -n "$FAILED_MIGRATIONS" ]; then
            for MIGRATION in $FAILED_MIGRATIONS; do
                # Verificar si la migración ya está registrada
                EXISTS=$(docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="echo DB::table('migrations')->where('migration', '$MIGRATION')->exists() ? '1' : '0';" 2>/dev/null || echo "0")
                
                if [ "$EXISTS" = "0" ]; then
                    warn "  → Marcando migración como ejecutada: $MIGRATION"
                    docker compose --env-file .env exec -T php-fpm php artisan tinker --execute="DB::table('migrations')->insert(['migration' => '$MIGRATION', 'batch' => $NEXT_BATCH]);" 2>/dev/null || true
                fi
            done
            
            # Intentar ejecutar migraciones de nuevo
            info "Reintentando migraciones después de sincronización..."
            RETRY_OUTPUT=$(docker compose --env-file .env exec -T php-fpm php artisan migrate --force 2>&1 || true)
            
            if echo "$RETRY_OUTPUT" | grep -q "Nothing to migrate"; then
                info "✅ Todas las migraciones están sincronizadas"
                return 0
            elif echo "$RETRY_OUTPUT" | grep -q "Migrating\|Migrated"; then
                info "✅ Migraciones ejecutadas exitosamente"
                return 0
            else
                error "❌ Error al ejecutar migraciones después de sincronización"
                echo "$RETRY_OUTPUT" | tail -20
                return 1
            fi
        fi
    elif echo "$MIGRATE_OUTPUT" | grep -q "Nothing to migrate"; then
        info "✅ No hay migraciones pendientes"
        return 0
    elif echo "$MIGRATE_OUTPUT" | grep -q "Migrating\|Migrated"; then
        info "✅ Migraciones ejecutadas exitosamente"
        return 0
    else
        error "❌ Error al ejecutar migraciones"
        echo "$MIGRATE_OUTPUT" | tail -20
        return 1
    fi
    
    return 0
}

# Ejecutar migraciones con manejo de errores mejorado
info "Ejecutando migraciones..."
if ! sync_migrations; then
    error "❌ Error al ejecutar migraciones"
    error "Ejecuta el rollback: $ROLLBACK_FILE"
    error ""
    error "O intenta sincronizar manualmente:"
    error "  ./fix-migrations.sh"
    exit 1
fi

# Verificar estado final de migraciones
info "Estado final de migraciones:"
docker compose --env-file .env exec -T php-fpm php artisan migrate:status

# ============================================
# PASO 7: REINICIAR SERVICIOS Y VERIFICAR
# ============================================
step "7/7: Reiniciando servicios y verificando..."

# Reiniciar servicios
info "Reiniciando servicios..."
docker compose --env-file .env up -d

# Esperar a que los servicios estén listos
info "Esperando a que los servicios estén listos..."
sleep 15

# Verificar estado de contenedores
info "Estado de contenedores después de la actualización:"
docker compose --env-file .env ps

# Verificar healthchecks con diagnóstico detallado
info "Verificando healthchecks..."
sleep 10  # Dar tiempo adicional para que los healthchecks se ejecuten

HEALTHY_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Health":"healthy"' || echo "0")
UNHEALTHY_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Health":"unhealthy"' || echo "0")
TOTAL_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Name"' || echo "0")

if [ "$TOTAL_COUNT" -gt 0 ]; then
    if [ "$HEALTHY_COUNT" -eq "$TOTAL_COUNT" ]; then
        info "✅ Todos los servicios están healthy ($HEALTHY_COUNT/$TOTAL_COUNT)"
    else
        warn "⚠️  Algunos servicios no están healthy ($HEALTHY_COUNT/$TOTAL_COUNT healthy, $UNHEALTHY_COUNT/$TOTAL_COUNT unhealthy)"
        
        # Mostrar servicios unhealthy
        UNHEALTHY_SERVICES=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -B 5 '"Health":"unhealthy"' | grep -o '"Name":"[^"]*"' | cut -d'"' -f4 || echo "")
        if [ -n "$UNHEALTHY_SERVICES" ]; then
            warn "Servicios unhealthy:"
            for SERVICE in $UNHEALTHY_SERVICES; do
                warn "  - $SERVICE"
            done
            warn ""
            warn "Para diagnosticar: ./diagnose-health.sh"
            warn "O ver logs: docker compose --env-file .env logs [nombre-servicio]"
        fi
    fi
fi

# Verificar API
info "Verificando API..."
sleep 5  # Dar tiempo a que los servicios estén completamente listos
if curl -f -s http://localhost/up > /dev/null 2>&1 || curl -f -s https://api.notificaciones.space/up > /dev/null 2>&1; then
    info "✅ API respondiendo correctamente"
else
    warn "⚠️  API no responde inmediatamente (puede tardar unos segundos más)"
    warn "Verifica manualmente: curl https://api.notificaciones.space/up"
fi

# Verificar migraciones finales
info "Verificando estado final de migraciones..."
PENDING_COUNT=$(docker compose --env-file .env exec -T php-fpm php artisan migrate:status 2>/dev/null | grep -c "Pending" || echo "0")
if [ "$PENDING_COUNT" -eq 0 ]; then
    info "✅ Todas las migraciones están ejecutadas"
else
    warn "⚠️  Hay $PENDING_COUNT migraciones pendientes"
    warn "Revisa: docker compose --env-file .env exec php-fpm php artisan migrate:status"
    warn "Si hay tablas que ya existen, ejecuta: ./fix-migrations.sh"
fi

# Optimizar caches para producción (después de migraciones exitosas)
info "Optimizando caches para producción..."
docker compose --env-file .env exec -T php-fpm php artisan config:cache || warn "config:cache falló"
docker compose --env-file .env exec -T php-fpm php artisan route:cache || warn "route:cache falló"
docker compose --env-file .env exec -T php-fpm php artisan view:cache || warn "view:cache falló"

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


