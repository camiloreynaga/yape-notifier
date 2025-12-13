#!/bin/bash

# Script para configurar el entorno Docker del proyecto Yape Notifier

set -e

echo "🚀 Configurando entorno Docker para Yape Notifier..."

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.staging.yml" ]; then
    echo "❌ Error: Este script debe ejecutarse desde el directorio infra/docker"
    exit 1
fi

# Copiar .env.example si no existe .env
if [ ! -f ".env" ]; then
    echo "📝 Creando archivo .env desde .env.example..."
    cp .env.example .env
    echo "✅ Archivo .env creado. Por favor, revisa y ajusta las variables si es necesario."
fi

# Verificar que existe el directorio de la API
if [ ! -d "../../apps/api" ]; then
    echo "❌ Error: No se encuentra el directorio apps/api"
    exit 1
fi

# Verificar que existe composer.json
if [ ! -f "../../apps/api/composer.json" ]; then
    echo "❌ Error: No se encuentra composer.json en apps/api"
    exit 1
fi

# Crear .env en apps/api si no existe
if [ ! -f "../../apps/api/.env" ]; then
    echo "📝 Creando archivo .env en apps/api..."
    if [ -f "../../apps/api/.env.example" ]; then
        cp ../../apps/api/.env.example ../../apps/api/.env
    else
        cat > ../../apps/api/.env << EOF
APP_NAME="Yape Notifier API"
APP_ENV=local
APP_KEY=
APP_DEBUG=true
APP_TIMEZONE=UTC
APP_URL=http://localhost:8000
APP_LOCALE=en
APP_FALLBACK_LOCALE=en
APP_FAKER_LOCALE=en_US

APP_MAINTENANCE_DRIVER=file
APP_MAINTENANCE_STORE=database

BCRYPT_ROUNDS=12

LOG_CHANNEL=stack
LOG_STACK=single
LOG_DEPRECATIONS_CHANNEL=null
LOG_LEVEL=debug

DB_CONNECTION=pgsql
DB_HOST=db
DB_PORT=5432
DB_DATABASE=yape_notifier
DB_USERNAME=postgres
DB_PASSWORD=password

SESSION_DRIVER=database
SESSION_LIFETIME=120
SESSION_ENCRYPT=false
SESSION_PATH=/
SESSION_DOMAIN=null

BROADCAST_CONNECTION=log
FILESYSTEM_DISK=local
QUEUE_CONNECTION=database

CACHE_STORE=database
CACHE_PREFIX=

MEMCACHED_HOST=127.0.0.1

REDIS_CLIENT=phpredis
REDIS_HOST=redis
REDIS_PASSWORD=null
REDIS_PORT=6379

MAIL_MAILER=smtp
MAIL_HOST=127.0.0.1
MAIL_PORT=2525
MAIL_USERNAME=null
MAIL_PASSWORD=null
MAIL_ENCRYPTION=null
MAIL_FROM_ADDRESS="hello@example.com"
MAIL_FROM_NAME="\${APP_NAME}"

SANCTUM_STATEFUL_DOMAINS=localhost,127.0.0.1
EOF
    fi
fi

echo "🐳 Construyendo contenedores Docker..."
docker-compose -f docker-compose.staging.yml build

echo "🚀 Iniciando contenedores (staging)..."
docker-compose -f docker-compose.staging.yml up -d

echo "⏳ Esperando a que la base de datos esté lista..."
sleep 10

echo "📦 Instalando dependencias de Composer..."
docker-compose -f docker-compose.staging.yml exec -T php-fpm composer install

echo "🔑 Generando clave de aplicación..."
docker-compose -f docker-compose.staging.yml exec -T php-fpm php artisan key:generate

echo "🗄️ Ejecutando migraciones..."
docker-compose -f docker-compose.staging.yml exec -T php-fpm php artisan migrate --force

echo "✅ ¡Configuración completada!"
echo ""
echo "📋 Información del entorno:"
echo "   - API disponible en: http://localhost:8080/up"
echo "   - Dashboard disponible en: http://localhost:8080"
echo "   - Base de datos PostgreSQL en: localhost:5432 (interno)"
echo ""
echo "📝 Comandos útiles:"
echo "   - Ver logs: docker-compose -f docker-compose.staging.yml logs -f"
echo "   - Detener: docker-compose -f docker-compose.staging.yml down"
echo "   - Reiniciar: docker-compose -f docker-compose.staging.yml restart"
echo "   - Acceder al contenedor: docker-compose -f docker-compose.staging.yml exec php-fpm sh"
echo "   - Ejecutar artisan: docker-compose -f docker-compose.staging.yml exec php-fpm php artisan [comando]"

