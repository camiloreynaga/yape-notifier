#!/bin/bash

# ============================================
# Development Setup Script
# ============================================
# Uso: ./setup.sh
# Configura el entorno de desarrollo por primera vez

set -e

GREEN='\033[0;32m'
NC='\033[0m'

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

info "Configurando entorno de desarrollo..."

# Crear .env desde ejemplo
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example .env
        info "Archivo .env creado desde .env.example"
    else
        echo "Archivo .env.example no encontrado"
    fi
else
    info "Archivo .env ya existe"
fi

info ""
info "✅ Configuración inicial completada"
info ""
info "Próximos pasos:"
info "  Ejecuta ./deploy.sh para iniciar el entorno"

