#!/bin/bash

# ============================================
# Production Dashboard Update Script (Optimized)
# ============================================
# Script optimizado para actualizar SOLO el dashboard (React/Vite)
# cuando no hay cambios en backend, base de datos o configuración
#
# Uso: ./update-dashboard.sh
# Requisitos: Estar en el directorio de producción
# ============================================

set -e  # Salir si hay error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Verificar que docker compose está disponible
if ! command -v docker &> /dev/null; then
    error "Docker no está instalado o no está en PATH"
    exit 1
fi

info "=========================================="
info "  Yape Notifier - Actualización de Dashboard"
info "  Fecha: $(date)"
info "=========================================="
echo ""

# ============================================
# PASO 1: VERIFICAR QUE EL CÓDIGO ESTÁ ACTUALIZADO
# ============================================
step "1/4: Verificando que el código está actualizado..."

warn "⚠️  IMPORTANTE: Asegúrate de haber actualizado el código en el servidor"
warn "   (git pull, o copiar archivos nuevos)"
read -p "¿El código del dashboard ya está actualizado en el servidor? (s/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    error "Actualiza el código primero y vuelve a ejecutar este script"
    exit 1
fi

# ============================================
# PASO 2: RECONSTRUIR IMAGEN DEL DASHBOARD
# ============================================
step "2/4: Reconstruyendo imagen del dashboard..."

info "Reconstruyendo imagen del dashboard (con BuildKit para cache optimizado)..."
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

if docker compose --env-file .env build dashboard; then
    info "✅ Imagen del dashboard reconstruida"
else
    error "❌ Error al reconstruir imagen del dashboard"
    exit 1
fi

# ============================================
# PASO 3: REINICIAR CONTENEDOR DEL DASHBOARD
# ============================================
step "3/4: Reiniciando contenedor del dashboard..."

info "Reiniciando contenedor del dashboard..."
if docker compose --env-file .env up -d dashboard; then
    info "✅ Contenedor del dashboard reiniciado"
else
    error "❌ Error al reiniciar contenedor del dashboard"
    exit 1
fi

# Esperar a que el servicio esté listo
info "Esperando a que el dashboard esté listo..."
sleep 10

# ============================================
# PASO 4: VERIFICAR QUE EL DASHBOARD FUNCIONA
# ============================================
step "4/4: Verificando que el dashboard funciona..."

# Verificar estado del contenedor
info "Estado del contenedor:"
docker compose --env-file .env ps dashboard

# Verificar healthcheck
info "Verificando healthcheck..."
sleep 5  # Dar tiempo adicional para que el healthcheck se ejecute

HEALTH_STATUS=$(docker compose --env-file .env ps --format json dashboard 2>/dev/null | grep -o '"Health":"[^"]*"' | cut -d'"' -f4 || echo "unknown")

if [ "$HEALTH_STATUS" = "healthy" ]; then
    info "✅ Dashboard está healthy"
elif [ "$HEALTH_STATUS" = "starting" ]; then
    warn "⚠️  Dashboard está iniciando (puede tardar unos segundos más)"
elif [ "$HEALTH_STATUS" = "unhealthy" ]; then
    error "❌ Dashboard está unhealthy"
    error "Ver logs: docker compose --env-file .env logs dashboard"
    exit 1
else
    warn "⚠️  No se pudo determinar el estado del healthcheck"
fi

# Verificar que responde
info "Verificando que el dashboard responde..."
if curl -f -s http://localhost/health > /dev/null 2>&1 || curl -f -s https://dashboard.notificaciones.space/health > /dev/null 2>&1; then
    info "✅ Dashboard respondiendo correctamente"
else
    warn "⚠️  Dashboard no responde inmediatamente (puede tardar unos segundos más)"
    warn "Verifica manualmente: curl https://dashboard.notificaciones.space/health"
fi

# ============================================
# RESUMEN
# ============================================
echo ""
info "=========================================="
info "  ✅ ACTUALIZACIÓN DEL DASHBOARD COMPLETADA"
info "=========================================="
echo ""
info "Dashboard disponible en:"
info "  - https://dashboard.notificaciones.space"
echo ""
info "Para ver logs:"
info "  docker compose --env-file .env logs dashboard -f"
echo ""
info "Para verificar estado:"
info "  docker compose --env-file .env ps dashboard"
echo ""

