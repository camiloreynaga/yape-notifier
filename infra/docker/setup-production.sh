#!/bin/bash

# ============================================
# Script de Configuración Inicial para Producción
# ============================================
# Este script crea los archivos .env necesarios desde los ejemplos

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

info "Configurando entorno de producción..."

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.prod.yml" ]; then
    error "Este script debe ejecutarse desde el directorio infra/docker"
    exit 1
fi

# Crear .env.production si no existe
if [ ! -f ".env.production" ]; then
    if [ -f ".env.production.example" ]; then
        info "Creando .env.production desde .env.production.example..."
        cp .env.production.example .env.production
        warn "Por favor edita .env.production y configura las variables necesarias"
    else
        error "Archivo .env.production.example no encontrado"
        exit 1
    fi
else
    warn "Archivo .env.production ya existe, no se sobrescribirá"
fi

# Crear apps/api/.env.production si no existe
if [ ! -f "../../apps/api/.env.production" ]; then
    if [ -f "../../apps/api/.env.production.example" ]; then
        info "Creando apps/api/.env.production desde .env.production.example..."
        cp ../../apps/api/.env.production.example ../../apps/api/.env.production
        warn "Por favor edita apps/api/.env.production y configura las variables necesarias"
    else
        warn "Archivo apps/api/.env.production.example no encontrado"
        warn "Necesitarás crear apps/api/.env.production manualmente"
    fi
else
    warn "Archivo apps/api/.env.production ya existe, no se sobrescribirá"
fi

info ""
info "✅ Configuración inicial completada!"
info ""
info "Próximos pasos:"
info "  1. Edita .env.production y configura:"
info "     - DASHBOARD_API_URL (URL pública del API con HTTPS)"
info "     - DB_PASSWORD (contraseña segura)"
info ""
info "  2. Edita apps/api/.env.production y configura:"
info "     - APP_KEY (generar con: php artisan key:generate)"
info "     - APP_URL (URL pública del API con HTTPS)"
info "     - DB_PASSWORD (misma que en .env.production)"
info "     - Todas las demás variables de producción"
info ""
info "  3. Ejecuta el despliegue:"
info "     ./deploy-production.sh"
info "     o"
info "     make prod-deploy"



