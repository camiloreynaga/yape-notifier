#!/bin/bash

# ============================================
# Generar Keys de Reverb
# ============================================
# Este script genera keys reales de Reverb usando PHP
# y las actualiza automáticamente en el .env
#
# Uso: ./generate-reverb-keys.sh
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

info "Generando keys reales de Reverb..."
info ""

# Verificar que PHP-FPM está corriendo
if ! docker compose --env-file .env ps php-fpm | grep -q "Up"; then
    warn "PHP-FPM no está corriendo. Iniciando..."
    docker compose --env-file .env up -d php-fpm
    sleep 5
fi

# Generar keys usando PHP directamente
info "Generando REVERB_APP_KEY y REVERB_APP_SECRET..."
KEYS=$(docker compose --env-file .env exec -T php-fpm php -r "
    // Generar REVERB_APP_KEY (base64 de 32 bytes aleatorios)
    \$key = base64_encode(random_bytes(32));
    
    // Generar REVERB_APP_SECRET (hex de 16 bytes aleatorios)
    \$secret = bin2hex(random_bytes(16));
    
    echo \"REVERB_APP_KEY=base64:\" . \$key . \"\n\";
    echo \"REVERB_APP_SECRET=\" . \$secret . \"\n\";
")

# Extraer las keys
REVERB_APP_KEY=$(echo "$KEYS" | grep "REVERB_APP_KEY=" | cut -d'=' -f2)
REVERB_APP_SECRET=$(echo "$KEYS" | grep "REVERB_APP_SECRET=" | cut -d'=' -f2)

if [ -z "$REVERB_APP_KEY" ] || [ -z "$REVERB_APP_SECRET" ]; then
    error "No se pudieron generar las keys"
    exit 1
fi

info "✅ Keys generadas correctamente"
info ""
info "Keys generadas:"
info "  REVERB_APP_KEY=$REVERB_APP_KEY"
info "  REVERB_APP_SECRET=$REVERB_APP_SECRET"
info ""

# Actualizar .env
info "Actualizando .env..."

# Actualizar REVERB_APP_KEY
if grep -q "^REVERB_APP_KEY=" .env; then
    sed -i "s|^REVERB_APP_KEY=.*|REVERB_APP_KEY=$REVERB_APP_KEY|" .env
    info "✅ REVERB_APP_KEY actualizado en .env"
else
    echo "REVERB_APP_KEY=$REVERB_APP_KEY" >> .env
    info "✅ REVERB_APP_KEY agregado a .env"
fi

# Actualizar REVERB_APP_SECRET
if grep -q "^REVERB_APP_SECRET=" .env; then
    sed -i "s|^REVERB_APP_SECRET=.*|REVERB_APP_SECRET=$REVERB_APP_SECRET|" .env
    info "✅ REVERB_APP_SECRET actualizado en .env"
else
    echo "REVERB_APP_SECRET=$REVERB_APP_SECRET" >> .env
    info "✅ REVERB_APP_SECRET agregado a .env"
fi

# Asegurar que BROADCAST_CONNECTION está configurado
if ! grep -q "^BROADCAST_CONNECTION=" .env; then
    echo "BROADCAST_CONNECTION=reverb" >> .env
    info "✅ BROADCAST_CONNECTION agregado a .env"
elif ! grep -q "^BROADCAST_CONNECTION=reverb" .env; then
    sed -i "s|^BROADCAST_CONNECTION=.*|BROADCAST_CONNECTION=reverb|" .env
    info "✅ BROADCAST_CONNECTION actualizado a 'reverb'"
fi

info ""
info "=========================================="
info "  ✅ CONFIGURACIÓN COMPLETA"
info "=========================================="
info ""
info "Las keys reales se han generado y agregado al .env"
info ""
info "Verifica la configuración:"
info "  grep REVERB .env"
info ""
info "Próximos pasos:"
info "  1. Limpiar cache de Laravel:"
info "     docker compose --env-file .env exec php-fpm php artisan config:clear"
info ""
info "  2. Reiniciar servicios:"
info "     docker compose --env-file .env restart"
info ""
info "  3. Verificar que Reverb puede iniciar:"
info "     docker compose --env-file .env up -d reverb"
info "     docker compose --env-file .env logs reverb"
info ""

