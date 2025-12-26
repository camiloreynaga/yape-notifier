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

# Verificar que APP_KEY está configurado (necesario para Reverb)
if ! grep -q "^APP_KEY=base64:" .env; then
    error "APP_KEY no está configurado en .env"
    error "Reverb requiere APP_KEY para generar las keys"
    error "Ejecuta primero: docker compose --env-file .env exec php-fpm php artisan key:generate"
    exit 1
fi

info "Generando keys de Reverb..."

# Método 1: Intentar usar contenedor PHP-FPM existente (más rápido y confiable)
USE_EXISTING_CONTAINER=false
if docker compose --env-file .env ps php-fpm | grep -q "Up"; then
    info "Usando contenedor PHP-FPM existente..."
    USE_EXISTING_CONTAINER=true
fi

if [ "$USE_EXISTING_CONTAINER" = true ]; then
    # Usar contenedor existente
    info "Generando keys usando contenedor PHP-FPM existente..."
    
    # Generar keys (sin --show, esa opción no existe en esta versión)
    OUTPUT=$(docker compose --env-file .env exec -T php-fpm php artisan reverb:install 2>&1 || true)
    
    # Mostrar output completo para debugging
    if [ -n "$OUTPUT" ]; then
        info "Output del comando reverb:install:"
        echo "$OUTPUT"
        echo ""
    fi
    
    # Extraer keys del output (múltiples patrones para compatibilidad)
    REVERB_APP_KEY=$(echo "$OUTPUT" | grep -oE 'REVERB_APP_KEY=([^[:space:]]+)' | cut -d'=' -f2 | head -1 || echo "")
    REVERB_APP_SECRET=$(echo "$OUTPUT" | grep -oE 'REVERB_APP_SECRET=([^[:space:]]+)' | cut -d'=' -f2 | head -1 || echo "")
    
    # Si no se encontraron, intentar otro formato
    if [ -z "$REVERB_APP_KEY" ]; then
        REVERB_APP_KEY=$(echo "$OUTPUT" | grep -i "app_key" | grep -oE 'base64:[^[:space:]]+' | head -1 || echo "")
    fi
    if [ -z "$REVERB_APP_SECRET" ]; then
        REVERB_APP_SECRET=$(echo "$OUTPUT" | grep -i "app_secret" | grep -oE '[a-zA-Z0-9+/=]{40,}' | head -1 || echo "")
    fi
else
    # Método 2: Crear contenedor temporal
    info "Creando contenedor temporal para generar keys..."
    
    # Crear contenedor temporal
    TEMP_CONTAINER=$(docker run -d --rm \
        -v "$(pwd)/../../../../apps/api:/var/www" \
        -w /var/www \
        php:8.2-cli \
        sh -c "sleep 3600")
    
    info "Instalando dependencias en contenedor temporal..."
    
    # Instalar composer y dependencias
    docker exec $TEMP_CONTAINER sh -c "
        apk add --no-cache git unzip curl > /dev/null 2>&1 && \
        curl -sS https://getcomposer.org/installer | php && \
        php composer.phar install --no-dev --optimize-autoloader --no-interaction --prefer-dist --no-scripts
    " > /dev/null 2>&1 || warn "Algunas dependencias pueden no haberse instalado correctamente"
    
    # Copiar .env al contenedor
    docker cp .env $TEMP_CONTAINER:/var/www/.env 2>/dev/null || warn "No se pudo copiar .env al contenedor"
    
    info "Generando keys de Reverb..."
    
    # Generar keys (sin --show, esa opción no existe en esta versión)
    OUTPUT=$(docker exec $TEMP_CONTAINER php artisan reverb:install 2>&1 || true)
    
    # Mostrar output completo
    if [ -n "$OUTPUT" ]; then
        info "Output del comando reverb:install:"
        echo "$OUTPUT"
        echo ""
    fi
    
    # Extraer keys del output
    REVERB_APP_KEY=$(echo "$OUTPUT" | grep -oE 'REVERB_APP_KEY=([^[:space:]]+)' | cut -d'=' -f2 | head -1 || echo "")
    REVERB_APP_SECRET=$(echo "$OUTPUT" | grep -oE 'REVERB_APP_SECRET=([^[:space:]]+)' | cut -d'=' -f2 | head -1 || echo "")
    
    # Limpiar contenedor temporal
    docker stop $TEMP_CONTAINER > /dev/null 2>&1 || true
fi

# Verificar que se generaron las keys
if [ -z "$REVERB_APP_KEY" ] || [ -z "$REVERB_APP_SECRET" ]; then
    error "❌ No se pudieron extraer las keys de Reverb del output"
    error ""
    error "SOLUCIÓN ALTERNATIVA:"
    error "Ejecuta manualmente en el contenedor PHP-FPM:"
    error "  docker compose --env-file .env exec php-fpm php artisan reverb:install"
    error ""
    error "Luego copia las keys mostradas y agrégalas manualmente al .env"
    error ""
    if [ -n "$OUTPUT" ]; then
        error "Output completo recibido:"
        echo "$OUTPUT"
    fi
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



