#!/bin/bash

# ============================================
# Script de Despliegue Rápido
# ============================================
# Automatiza todo el proceso de despliegue
# con verificación y resolución de problemas
#
# Uso: ./quick-deploy.sh [--no-cache]
# ============================================

set -e

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

step() {
    echo -e "${BLUE}[STEP]${NC} $1"
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

info "=========================================="
info "  Despliegue Rápido - Yape Notifier"
info "=========================================="
echo ""

# Verificar si se solicita build sin cache
BUILD_NO_CACHE=""
if [ "$1" == "--no-cache" ]; then
    BUILD_NO_CACHE="--no-cache"
    warn "Modo --no-cache activado"
fi

# ============================================
# PASO 1: Limpiar artefactos
# ============================================
step "1/7: Limpiando artefactos de BuildKit..."
if [ -f "clean-buildkit-artifacts.sh" ]; then
    chmod +x clean-buildkit-artifacts.sh
    ./clean-buildkit-artifacts.sh || warn "Algunos artefactos no se pudieron limpiar"
else
    warn "Script clean-buildkit-artifacts.sh no encontrado, saltando..."
fi

# ============================================
# PASO 2: Actualizar código (si es Git)
# ============================================
step "2/7: Verificando código..."
if [ -d "../../../../.git" ]; then
    info "Repositorio Git detectado"
    read -p "¿Actualizar código desde Git? (s/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        cd ../../../../..
        git pull || warn "Error al actualizar código (puede continuar)"
        cd infra/docker/environments/production
    fi
else
    info "No es un repositorio Git, continuando..."
fi

# ============================================
# PASO 3: Desplegar
# ============================================
step "3/7: Desplegando servicios..."
if [ -f "deploy.sh" ]; then
    chmod +x deploy.sh
    if [ -n "$BUILD_NO_CACHE" ]; then
        ./deploy.sh --no-cache
    else
        ./deploy.sh
    fi
else
    error "Script deploy.sh no encontrado"
    exit 1
fi

# ============================================
# PASO 4: Resolver migraciones desincronizadas
# ============================================
step "4/7: Verificando migraciones..."
if [ -f "fix-migrations.sh" ]; then
    chmod +x fix-migrations.sh
    # Verificar si hay migraciones pendientes que fallan
    PENDING_ERRORS=$(docker compose --env-file .env exec -T php-fpm php artisan migrate:status 2>&1 | grep -c "Pending" || echo "0")
    if [ "$PENDING_ERRORS" -gt 0 ]; then
        warn "Hay migraciones pendientes, verificando si hay problemas..."
        # Intentar ejecutar migraciones
        MIGRATE_OUTPUT=$(docker compose --env-file .env exec -T php-fpm php artisan migrate --force 2>&1 || true)
        if echo "$MIGRATE_OUTPUT" | grep -q "Duplicate table\|relation.*already exists"; then
            warn "Detectadas migraciones desincronizadas, sincronizando..."
            ./fix-migrations.sh || warn "Error al sincronizar migraciones (puede continuar)"
        fi
    fi
else
    warn "Script fix-migrations.sh no encontrado"
fi

# ============================================
# PASO 5: Verificar healthchecks
# ============================================
step "5/7: Verificando healthchecks..."
sleep 15  # Dar tiempo a que los healthchecks se ejecuten

HEALTHY_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Health":"healthy"' || echo "0")
UNHEALTHY_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Health":"unhealthy"' || echo "0")
TOTAL_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Name"' || echo "0")

if [ "$UNHEALTHY_COUNT" -gt 0 ]; then
    warn "Hay $UNHEALTHY_COUNT servicio(s) unhealthy"
    if [ -f "fix-healthchecks.sh" ]; then
        chmod +x fix-healthchecks.sh
        read -p "¿Intentar reparar servicios unhealthy automáticamente? (s/N): " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            ./fix-healthchecks.sh
        fi
    fi
else
    info "✅ Todos los servicios están healthy ($HEALTHY_COUNT/$TOTAL_COUNT)"
fi

# ============================================
# PASO 6: Verificar API
# ============================================
step "6/7: Verificando API..."
sleep 5

if curl -f -s https://api.notificaciones.space/up > /dev/null 2>&1; then
    info "✅ API responde correctamente"
else
    warn "⚠️  API no responde inmediatamente"
    warn "Puede tardar unos segundos más. Verifica manualmente:"
    warn "  curl https://api.notificaciones.space/up"
fi

# ============================================
# PASO 7: Resumen final
# ============================================
step "7/7: Resumen final..."
echo ""
info "=========================================="
info "  Estado Final"
info "=========================================="
echo ""

docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Status}}\t{{.Health}}'

echo ""
info "Servicios disponibles:"
info "  - API: https://api.notificaciones.space"
info "  - Dashboard: https://dashboard.notificaciones.space"
info ""
info "Para ver logs:"
info "  docker compose --env-file .env logs -f"
info ""
info "Para diagnosticar problemas:"
info "  ./diagnose-health.sh"
info "  ./fix-migrations.sh"
info ""

