#!/bin/bash

# ============================================
# Script para Reparar Healthchecks
# ============================================
# Intenta reparar servicios unhealthy
#
# Uso: ./fix-healthchecks.sh
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
info "  Reparación de Healthchecks"
info "=========================================="
echo ""

# Obtener servicios unhealthy
UNHEALTHY_SERVICES=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -B 5 '"Health":"unhealthy"' | grep -o '"Name":"[^"]*"' | cut -d'"' -f4 || echo "")

if [ -z "$UNHEALTHY_SERVICES" ]; then
    info "✅ Todos los servicios están healthy"
    exit 0
fi

warn "Servicios unhealthy detectados:"
for SERVICE in $UNHEALTHY_SERVICES; do
    echo "  - $SERVICE"
done
echo ""

# Reparar cada servicio
for SERVICE in $UNHEALTHY_SERVICES; do
    step "Reparando: $SERVICE"
    
    case $SERVICE in
        *caddy*)
            info "Reiniciando Caddy..."
            docker compose --env-file .env restart caddy
            sleep 5
            info "Verificando endpoint de admin..."
            if docker compose --env-file .env exec -T caddy wget --quiet --tries=1 --spider --timeout=5 http://localhost:2019/config/ 2>&1; then
                info "  ✅ Caddy reparado"
            else
                warn "  ⚠️  Caddy aún no responde. Verifica logs: docker compose --env-file .env logs caddy"
            fi
            ;;
            
        *dashboard*)
            info "Reiniciando Dashboard..."
            docker compose --env-file .env restart dashboard
            sleep 5
            info "Verificando endpoint /health..."
            if docker compose --env-file .env exec -T dashboard wget --quiet --tries=1 --spider --timeout=5 http://localhost/health 2>&1; then
                info "  ✅ Dashboard reparado"
            else
                warn "  ⚠️  Dashboard aún no responde. Verificando endpoint raíz..."
                if docker compose --env-file .env exec -T dashboard wget --quiet --tries=1 --spider --timeout=5 http://localhost/ 2>&1; then
                    warn "  ⚠️  Endpoint raíz responde pero /health no. Verifica configuración de Nginx."
                else
                    warn "  ⚠️  Dashboard no responde. Verifica logs: docker compose --env-file .env logs dashboard"
                fi
            fi
            ;;
            
        *)
            info "Reiniciando $SERVICE..."
            docker compose --env-file .env restart "$SERVICE"
            sleep 5
            ;;
    esac
    
    echo ""
done

# Esperar a que los healthchecks se ejecuten
info "Esperando a que los healthchecks se ejecuten..."
sleep 30

# Verificar estado final
info "Estado final de servicios:"
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Status}}\t{{.Health}}'

# Contar servicios healthy/unhealthy
HEALTHY_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Health":"healthy"' || echo "0")
UNHEALTHY_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Health":"unhealthy"' || echo "0")
TOTAL_COUNT=$(docker compose --env-file .env ps --format json 2>/dev/null | grep -c '"Name"' || echo "0")

echo ""
if [ "$HEALTHY_COUNT" -eq "$TOTAL_COUNT" ]; then
    info "✅ Todos los servicios están healthy ($HEALTHY_COUNT/$TOTAL_COUNT)"
else
    warn "⚠️  Algunos servicios aún están unhealthy ($HEALTHY_COUNT/$TOTAL_COUNT healthy, $UNHEALTHY_COUNT/$TOTAL_COUNT unhealthy)"
    warn ""
    warn "Si el problema persiste:"
    warn "  1. Ejecuta diagnóstico: ./diagnose-health.sh"
    warn "  2. Verifica logs: docker compose --env-file .env logs [nombre-servicio]"
    warn "  3. Verifica configuración de healthchecks en docker-compose.yml"
fi

