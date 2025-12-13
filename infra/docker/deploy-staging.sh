#!/bin/bash
set -e

# ============================================
# Script de Deployment - Staging
# ============================================
# Uso: ./infra/docker/deploy-staging.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT/infra/docker"

echo "🧪 Iniciando deployment de STAGING..."

# Verificar que existe .env.staging
if [ ! -f .env.staging ]; then
    echo "⚠️  .env.staging no existe. Creando desde plantilla..."
    if [ -f .env.staging.example ]; then
        cp .env.staging.example .env.staging
        echo "📝 Por favor, edita .env.staging y configura al menos DB_PASSWORD"
        echo "   Luego ejecuta este script nuevamente."
        exit 1
    else
        echo "❌ No existe .env.staging.example. Por favor crea .env.staging manualmente."
        exit 1
    fi
fi

# Pull del código
echo "📥 Actualizando código..."
cd "$PROJECT_ROOT"
git pull origin main || echo "⚠️  No se pudo hacer git pull (continuando...)"

# Reconstruir y levantar
echo "🔨 Construyendo imágenes..."
docker compose -f infra/docker/docker-compose.staging.yml build --no-cache

echo "⬆️ Levantando servicios de staging..."
docker compose -f infra/docker/docker-compose.staging.yml up -d

# Esperar a que los servicios estén listos
echo "⏳ Esperando a que los servicios estén listos..."
sleep 15

# Verificar si APP_KEY está configurado
if ! grep -q "APP_KEY=base64:" .env.staging 2>/dev/null; then
    echo "🔑 Generando APP_KEY..."
    APP_KEY=$(docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm php artisan key:generate --show 2>/dev/null | grep -oP 'base64:[^\s]+' || echo "")
    if [ ! -z "$APP_KEY" ]; then
        echo "📝 Actualizando .env.staging con APP_KEY..."
        sed -i "s/APP_KEY=.*/APP_KEY=$APP_KEY/" .env.staging
        docker compose -f infra/docker/docker-compose.staging.yml restart php-fpm
        sleep 5
    fi
fi

# Ejecutar migraciones
echo "🗄️ Ejecutando migraciones..."
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm php artisan migrate --force

# Limpiar caches (en staging no cacheamos)
echo "🧹 Limpiando caches..."
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm php artisan config:clear
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm php artisan route:clear
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm php artisan view:clear

# Verificar permisos
echo "🔐 Verificando permisos..."
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm chown -R www-data:www-data /var/www/storage
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm chown -R www-data:www-data /var/www/bootstrap/cache
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm chmod -R 775 /var/www/storage
docker compose -f infra/docker/docker-compose.staging.yml exec -T php-fpm chmod -R 775 /var/www/bootstrap/cache

echo "✅ Deployment de staging completado!"
echo ""
echo "🔍 Verificando salud del servicio..."
if curl -f http://localhost:8080/up > /dev/null 2>&1; then
    echo "✅ Health check OK: http://localhost:8080/up"
else
    echo "⚠️  Health check falló. Revisa los logs:"
    echo "   docker compose -f infra/docker/docker-compose.staging.yml logs"
fi

echo ""
echo "📊 Estado de contenedores:"
docker compose -f infra/docker/docker-compose.staging.yml ps

echo ""
echo "🌐 Acceso:"
echo "   Local: http://localhost:8080/up"
echo "   Dashboard: http://localhost:8080/"


