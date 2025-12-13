#!/bin/bash
set -e

# ============================================
# Script de Deployment - Producción
# ============================================
# Uso: ./infra/docker/deploy.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT/infra/docker"

echo "🚀 Iniciando deployment de PRODUCCIÓN..."

# Verificar que existe .env.production
if [ ! -f .env.production ]; then
    echo "⚠️  .env.production no existe. Creando desde plantilla..."
    if [ -f .env.production.example ]; then
        cp .env.production.example .env.production
        echo "📝 Por favor, edita .env.production y configura al menos DB_PASSWORD y APP_KEY"
        echo "   Luego ejecuta este script nuevamente."
        exit 1
    else
        echo "❌ No existe .env.production.example. Por favor crea .env.production manualmente."
        exit 1
    fi
fi

# Verificar que DB_PASSWORD está configurado
if ! grep -q "^DB_PASSWORD=.*" .env.production || grep -q "^DB_PASSWORD=$" .env.production; then
    echo "❌ Error: DB_PASSWORD no está configurado en .env.production"
    echo "   Por favor, edita .env.production y configura DB_PASSWORD con un valor no vacío"
    exit 1
fi

# Pull del código
echo "📥 Actualizando código..."
cd "$PROJECT_ROOT"
git pull origin main || echo "⚠️  No se pudo hacer git pull (continuando...)"

# Reconstruir y levantar usando --env-file para interpolación de variables
echo "🔨 Construyendo imágenes..."
docker compose --env-file .env.production -f infra/docker/docker-compose.yml build --no-cache

echo "⬆️ Levantando servicios..."
docker compose --env-file .env.production -f infra/docker/docker-compose.yml up -d

# Esperar a que los servicios estén listos
echo "⏳ Esperando a que los servicios estén listos..."
sleep 15

# Verificar si APP_KEY está configurado
if ! grep -q "APP_KEY=base64:" .env.production 2>/dev/null; then
    echo "🔑 Generando APP_KEY..."
    APP_KEY=$(docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm php artisan key:generate --show 2>/dev/null | grep -oP 'base64:[^\s]+' || echo "")
    if [ ! -z "$APP_KEY" ]; then
        echo "📝 Actualizando .env.production con APP_KEY..."
        sed -i "s/APP_KEY=.*/APP_KEY=$APP_KEY/" .env.production
        docker compose --env-file .env.production -f infra/docker/docker-compose.yml restart php-fpm
        sleep 5
    fi
fi

# Ejecutar migraciones
echo "🗄️ Ejecutando migraciones..."
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm php artisan migrate --force

# Optimizar Laravel
echo "⚡ Optimizando Laravel..."
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm php artisan config:cache
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm php artisan route:cache
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm php artisan view:cache

# Verificar permisos
echo "🔐 Verificando permisos..."
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm chown -R www-data:www-data /var/www/storage
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm chown -R www-data:www-data /var/www/bootstrap/cache
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm chmod -R 775 /var/www/storage
docker compose --env-file .env.production -f infra/docker/docker-compose.yml exec -T php-fpm chmod -R 775 /var/www/bootstrap/cache

echo "✅ Deployment de producción completado!"
echo ""
echo "🔍 Verificando salud del servicio..."
if curl -f http://localhost/up > /dev/null 2>&1; then
    echo "✅ Health check OK: http://localhost/up"
else
    echo "⚠️  Health check falló. Revisa los logs:"
    echo "   docker compose --env-file .env.production -f infra/docker/docker-compose.yml logs"
fi

echo ""
echo "📊 Estado de contenedores:"
docker compose --env-file .env.production -f infra/docker/docker-compose.yml ps
