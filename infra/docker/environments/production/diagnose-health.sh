#!/bin/bash

# ============================================
# Script de Diagnóstico de Healthchecks
# ============================================
# Diagnostica por qué los servicios están unhealthy
# y proporciona soluciones
#
# Uso: ./diagnose-health.sh
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
info "  Diagnóstico de Healthchecks"
info "=========================================="
echo ""

# Obtener servicios unhealthy
UNHEALTHY_SERVICES=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -o '"Health":"unhealthy"' -B 5 | grep -o '"Name":"[^"]*"' | cut -d'"' -f4 || echo "")

if [ -z "$UNHEALTHY_SERVICES" ]; then
    info "✅ Todos los servicios están healthy"
    exit 0
fi

warn "Servicios unhealthy detectados:"
for SERVICE in $UNHEALTHY_SERVICES; do
    echo "  - $SERVICE"
done
echo ""

# Diagnosticar cada servicio
for SERVICE in $UNHEALTHY_SERVICES; do
    step "Diagnosticando: $SERVICE"
    echo ""
    
    case $SERVICE in
        *caddy*)
            info "Caddy - Verificando endpoint de admin..."
            if docker compose --env-file .env exec -T caddy wget --quiet --tries=1 --spider --timeout=5 http://localhost:2019/config/ 2>&1; then
                info "  ✅ Endpoint de admin responde"
            else
                error "  ❌ Endpoint de admin NO responde"
                warn "  Verificando proceso de Caddy..."
                docker compose --env-file .env exec -T caddy ps aux | grep caddy || error "  ❌ Proceso de Caddy no encontrado"
            fi
            ;;
            
        *dashboard*)
            info "Dashboard - Verificando endpoint /health..."
            if docker compose --env-file .env exec -T dashboard wget --quiet --tries=1 --spider --timeout=5 http://localhost/health 2>&1; then
                info "  ✅ Endpoint /health responde"
            else
                warn "  ⚠️  Endpoint /health NO responde"
                info "  Verificando endpoint raíz /..."
                if docker compose --env-file .env exec -T dashboard wget --quiet --tries=1 --spider --timeout=5 http://localhost/ 2>&1; then
                    info "  ✅ Endpoint raíz responde (healthcheck puede estar mal configurado)"
                    warn "  Solución: Cambiar healthcheck a usar / en lugar de /health"
                else
                    error "  ❌ Endpoint raíz tampoco responde"
                    warn "  Verificando proceso de Nginx..."
                    docker compose --env-file .env exec -T dashboard ps aux | grep nginx || error "  ❌ Proceso de Nginx no encontrado"
                fi
            fi
            ;;
            
        *)
            info "Verificando proceso del servicio..."
            docker compose --env-file .env exec -T "$SERVICE" ps aux | head -5 || warn "  No se pudo verificar el proceso"
            ;;
    esac
    
    # Ver logs recientes
    info "Últimos logs del servicio:"
    docker compose --env-file .env logs "$SERVICE" --tail=10 2>&1 | tail -5 || warn "  No se pudieron obtener logs"
    
    echo ""
done

# Resumen y recomendaciones
echo ""
info "=========================================="
info "  Recomendaciones"
info "=========================================="
echo ""

if echo "$UNHEALTHY_SERVICES" | grep -q "caddy"; then
    warn "Para Caddy:"
    warn "  1. Verificar que el puerto 2019 esté accesible: docker compose --env-file .env exec caddy netstat -tlnp | grep 2019"
    warn "  2. Ver logs: docker compose --env-file .env logs caddy --tail=50"
    warn "  3. Si el problema persiste, el healthcheck puede estar demasiado estricto"
    echo ""
fi

if echo "$UNHEALTHY_SERVICES" | grep -q "dashboard"; then
    warn "Para Dashboard:"
    warn "  1. Verificar que el endpoint /health existe en la configuración de Nginx"
    warn "  2. Si no existe, cambiar healthcheck a usar / en lugar de /health"
    warn "  3. Ver logs: docker compose --env-file .env logs dashboard --tail=50"
    echo ""
fi

info "Para ver detalles completos de healthchecks:"
info "  docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Status}}\t{{.Health}}'"
info ""
info "Para reiniciar un servicio específico:"
info "  docker compose --env-file .env restart [nombre-servicio]"
info ""
info "Para intentar reparar automáticamente:"
info "  ./fix-healthchecks.sh"

