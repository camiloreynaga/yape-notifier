#!/bin/bash

# ============================================
# Production Setup Script
# ============================================
# Uso: ./setup.sh
# Configura el entorno de producción por primera vez

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

info "Configurando entorno de producción..."

# Crear .env desde ejemplo
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example .env
        info "Archivo .env creado desde .env.example"
        warn "IMPORTANTE: Edita .env y configura DB_PASSWORD con un valor seguro"
        warn "Luego ejecuta ./deploy.sh"
    else
        warn "Archivo .env.example no encontrado"
    fi
else
    warn "Archivo .env ya existe"
fi

info ""
info "✅ Configuración inicial completada"
info ""
info "Próximos pasos:"
info "  1. Edita .env y configura DB_PASSWORD"
info "  2. Ejecuta ./deploy.sh para desplegar"

