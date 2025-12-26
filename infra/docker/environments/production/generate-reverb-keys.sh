#!/bin/bash

# ============================================
# Script para generar keys de Reverb
# ============================================
# Uso: ./generate-reverb-keys.sh
#
# Este script genera las keys de Reverb necesarias
# y las muestra para copiarlas al .env
# ============================================

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

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.yml" ]; then
    error "Este script debe ejecutarse desde infra/docker/environments/production"
    exit 1
fi

# Verificar que .env existe
if [ ! -f ".env" ]; then
    error "Archivo .env no encontrado"
    error "Crea el archivo .env antes de continuar"
    exit 1
fi

info "Generando keys de Reverb..."

# Crear contenedor temporal para generar keys
TEMP_CONTAINER=$(docker run -d --rm \
    -v "$(pwd)/../../../../apps/api:/var/www" \
    -w /var/www \
    php:8.2-cli \
    sh -c "sleep 3600")

info "Instalando dependencias en contenedor temporal..."

# Instalar composer y dependencias
docker exec $TEMP_CONTAINER sh -c "
    curl -sS https://getcomposer.org/installer | php && \
    php composer.phar install --no-dev --optimize-autoloader --no-interaction --prefer-dist --no-scripts
" > /dev/null 2>&1

# Copiar .env al contenedor si existe
if [ -f ".env" ]; then
    docker cp .env $TEMP_CONTAINER:/var/www/.env
fi

info "Generando keys de Reverb..."

# Generar keys
OUTPUT=$(docker exec $TEMP_CONTAINER php artisan reverb:install --show 2>&1)

# Extraer keys del output
REVERB_APP_KEY=$(echo "$OUTPUT" | grep -oP 'REVERB_APP_KEY=\K[^\s]+' || echo "")
REVERB_APP_SECRET=$(echo "$OUTPUT" | grep -oP 'REVERB_APP_SECRET=\K[^\s]+' || echo "")

# Limpiar contenedor temporal
docker stop $TEMP_CONTAINER > /dev/null 2>&1

if [ -z "$REVERB_APP_KEY" ] || [ -z "$REVERB_APP_SECRET" ]; then
    error "No se pudieron generar las keys de Reverb"
    error "Output del comando:"
    echo "$OUTPUT"
    exit 1
fi

info "✅ Keys generadas correctamente"
echo ""
echo "Agrega estas líneas a tu archivo .env:"
echo ""
echo "# Laravel Reverb WebSocket Server"
echo "REVERB_APP_ID=yape-notifier"
echo "REVERB_APP_KEY=$REVERB_APP_KEY"
echo "REVERB_APP_SECRET=$REVERB_APP_SECRET"
echo "REVERB_HOST=0.0.0.0"
echo "REVERB_PORT=8080"
echo "REVERB_SCHEME=http"
echo ""
echo "BROADCAST_CONNECTION=reverb"
echo ""

# Preguntar si quiere actualizar .env automáticamente
read -p "¿Deseas actualizar el archivo .env automáticamente? (y/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Actualizar .env
    if grep -q "^REVERB_APP_KEY=" .env; then
        sed -i "s|^REVERB_APP_KEY=.*|REVERB_APP_KEY=$REVERB_APP_KEY|" .env
    else
        echo "REVERB_APP_KEY=$REVERB_APP_KEY" >> .env
    fi
    
    if grep -q "^REVERB_APP_SECRET=" .env; then
        sed -i "s|^REVERB_APP_SECRET=.*|REVERB_APP_SECRET=$REVERB_APP_SECRET|" .env
    else
        echo "REVERB_APP_SECRET=$REVERB_APP_SECRET" >> .env
    fi
    
    # Agregar otras variables si no existen
    grep -q "^REVERB_APP_ID=" .env || echo "REVERB_APP_ID=yape-notifier" >> .env
    grep -q "^REVERB_HOST=" .env || echo "REVERB_HOST=0.0.0.0" >> .env
    grep -q "^REVERB_PORT=" .env || echo "REVERB_PORT=8080" >> .env
    grep -q "^REVERB_SCHEME=" .env || echo "REVERB_SCHEME=http" >> .env
    grep -q "^BROADCAST_CONNECTION=" .env || echo "BROADCAST_CONNECTION=reverb" >> .env
    
    info "✅ Archivo .env actualizado"
else
    warn "No se actualizó el archivo .env"
    warn "Copia manualmente las keys mostradas arriba"
fi



