#!/bin/bash

# ============================================
# Script: Verificar Recursos de Staging vs Producción
# ============================================
# Muestra el uso de recursos de staging y producción
# Uso: ./check-resources.sh
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
if [ ! -f "docker-compose.yml" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/staging"
    exit 1
fi

section "📊 Uso de Recursos - Staging vs Producción"

info "Recursos de Producción:"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | grep -E "NAME|prod" || warn "No se encontraron contenedores de producción"

echo ""

info "Recursos de Staging:"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | grep -E "NAME|staging" || warn "No se encontraron contenedores de staging"

echo ""

section "💾 Uso de Disco"

info "Volúmenes de Producción:"
docker volume ls --filter "label=com.yape-notifier.environment=production" --format "table {{.Name}}\t{{.Driver}}"

echo ""

info "Volúmenes de Staging:"
docker volume ls --filter "label=com.yape-notifier.environment=staging" --format "table {{.Name}}\t{{.Driver}}"

echo ""

section "🌐 Redes"

info "Redes Docker:"
docker network ls --filter "label=com.yape-notifier.network" --format "table {{.Name}}\t{{.Driver}}\t{{.Scope}}"

echo ""

section "✅ Verificación de Límites"

info "Verificando límites de recursos configurados..."

# Verificar si staging tiene límites configurados
if docker inspect yape-notifier-php-fpm-staging 2>/dev/null | grep -q "Memory"; then
    info "✅ Staging tiene límites de recursos configurados"
else
    warn "⚠️  Staging no tiene límites de recursos configurados"
    warn "💡 Considera agregar límites en docker-compose.yml para proteger producción"
fi

echo ""
info "✅ Verificación completada"

