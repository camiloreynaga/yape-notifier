#!/bin/bash

# ============================================
# Método Simple para Generar Keys de Reverb
# ============================================
# Este script es más simple y directo
# Usa el contenedor PHP-FPM existente
# ============================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
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

info "Método Simple: Generando keys de Reverb..."
info ""

# Verificar que PHP-FPM está corriendo
if ! docker compose --env-file .env ps php-fpm | grep -q "Up"; then
    warn "PHP-FPM no está corriendo. Iniciando..."
    docker compose --env-file .env up -d php-fpm
    sleep 5
fi

info "Ejecutando: php artisan reverb:install"
info ""

# Ejecutar comando directamente (sin --show, esa opción no existe)
# El comando mostrará las keys en el output
docker compose --env-file .env exec php-fpm php artisan reverb:install

info ""
info "=========================================="
info "  INSTRUCCIONES:"
info "=========================================="
info ""
info "1. Copia las líneas REVERB_APP_KEY y REVERB_APP_SECRET mostradas arriba"
info "2. Agrégalas a tu archivo .env junto con:"
info ""
info "   BROADCAST_CONNECTION=reverb"
info "   REVERB_APP_ID=yape-notifier"
info "   REVERB_HOST=0.0.0.0"
info "   REVERB_PORT=8080"
info "   REVERB_SCHEME=http"
info ""
info "3. Guarda el archivo .env"
info "4. Reinicia los servicios: docker compose --env-file .env restart"
info ""

