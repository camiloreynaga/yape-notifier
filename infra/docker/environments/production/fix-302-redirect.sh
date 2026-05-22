#!/bin/bash

# ============================================
# Script para resolver el problema del 302 Redirect
# ============================================
# Este script verifica y corrige la configuración
# para evitar redirects HTTP incorrectos
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
info "  Resolviendo problema del 302 Redirect"
info "=========================================="
echo ""

# ============================================
# PASO 1: Verificar APP_URL en .env
# ============================================
step "1/5: Verificando APP_URL en .env..."

APP_URL=$(grep "^APP_URL=" .env | cut -d'=' -f2 || echo "")

if [ -z "$APP_URL" ]; then
    warn "APP_URL no está configurado en .env"
    info "Agregando APP_URL=https://api.notificaciones.space..."
    echo "APP_URL=https://api.notificaciones.space" >> .env
    APP_URL="https://api.notificaciones.space"
elif [[ "$APP_URL" == http://* ]]; then
    warn "APP_URL está configurado como HTTP, debe ser HTTPS"
    info "Actualizando APP_URL a HTTPS..."
    sed -i "s|^APP_URL=.*|APP_URL=https://api.notificaciones.space|" .env
    APP_URL="https://api.notificaciones.space"
else
    info "✅ APP_URL está configurado correctamente: $APP_URL"
fi

# ============================================
# PASO 2: Reiniciar servicios para aplicar cambios
# ============================================
step "2/5: Reiniciando servicios para aplicar cambios..."

info "Reiniciando nginx-api y php-fpm..."
docker compose --env-file .env restart nginx-api php-fpm

info "Esperando 10 segundos para que los servicios se estabilicen..."
sleep 10

# ============================================
# PASO 3: Limpiar caches de Laravel
# ============================================
step "3/5: Limpiando caches de Laravel..."

docker compose --env-file .env exec -T php-fpm php artisan config:clear || warn "Error al limpiar config cache"
docker compose --env-file .env exec -T php-fpm php artisan route:clear || warn "Error al limpiar route cache"
docker compose --env-file .env exec -T php-fpm php artisan cache:clear || warn "Error al limpiar cache"

# ============================================
# PASO 4: Regenerar caches
# ============================================
step "4/5: Regenerando caches de Laravel..."

docker compose --env-file .env exec -T php-fpm php artisan config:cache || error "Error al regenerar config cache"
docker compose --env-file .env exec -T php-fpm php artisan route:cache || error "Error al regenerar route cache"

# ============================================
# PASO 5: Verificar que el problema está resuelto
# ============================================
step "5/5: Verificando que el problema está resuelto..."

info "Probando endpoint de login..."
RESPONSE=$(curl -X POST https://api.notificaciones.space/api/login \
  -H "Content-Type: application/json" \
  -H "Origin: https://dashboard.notificaciones.space" \
  -d '{"email":"test@example.com","password":"test123"}' \
  -s -w "\nHTTP_STATUS:%{http_code}" \
  -o /tmp/login_response.txt 2>&1 || echo "HTTP_STATUS:000")

HTTP_STATUS=$(echo "$RESPONSE" | grep -o "HTTP_STATUS:[0-9]*" | cut -d':' -f2 || echo "000")

if [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "422" ]; then
    info "✅ Problema resuelto! El endpoint devuelve HTTP $HTTP_STATUS (correcto)"
    info "   - 200: Login exitoso"
    info "   - 401: Credenciales incorrectas (esperado)"
    info "   - 422: Validación fallida (esperado)"
elif [ "$HTTP_STATUS" = "302" ]; then
    error "❌ El problema persiste. HTTP 302 (redirect)"
    warn "Verifica:"
    warn "  1. Que APP_URL en .env sea https://api.notificaciones.space"
    warn "  2. Que los headers X-Forwarded-Proto se estén pasando correctamente"
    warn "  3. Logs de Caddy: docker compose --env-file .env logs caddy --tail=50"
    warn "  4. Logs de Nginx: docker compose --env-file .env logs nginx-api --tail=50"
    exit 1
else
    warn "⚠️  HTTP Status inesperado: $HTTP_STATUS"
    warn "Revisa los logs para más información"
fi

# Mostrar respuesta
if [ -f /tmp/login_response.txt ]; then
    echo ""
    info "Respuesta del servidor:"
    head -20 /tmp/login_response.txt
    rm -f /tmp/login_response.txt
fi

echo ""
info "=========================================="
info "  Resolución completada"
info "=========================================="
echo ""
info "Si el problema persiste, verifica:"
info "  1. Logs de Caddy: docker compose --env-file .env logs caddy --tail=50"
info "  2. Logs de Nginx: docker compose --env-file .env logs nginx-api --tail=50"
info "  3. Logs de PHP-FPM: docker compose --env-file .env logs php-fpm --tail=50"
info "  4. Configuración: cat .env | grep APP_URL"
echo ""

