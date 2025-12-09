# Script PowerShell para configurar el entorno Docker del proyecto Yape Notifier

Write-Host "🚀 Configurando entorno Docker para Yape Notifier..." -ForegroundColor Cyan

# Verificar que estamos en el directorio correcto
if (-not (Test-Path "docker-compose.yml")) {
    Write-Host "❌ Error: Este script debe ejecutarse desde el directorio infra/docker" -ForegroundColor Red
    exit 1
}

# Copiar .env.example si no existe .env
if (-not (Test-Path ".env")) {
    Write-Host "📝 Creando archivo .env desde .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "✅ Archivo .env creado. Por favor, revisa y ajusta las variables si es necesario." -ForegroundColor Green
}

# Verificar que existe el directorio de la API
if (-not (Test-Path "../../apps/api")) {
    Write-Host "❌ Error: No se encuentra el directorio apps/api" -ForegroundColor Red
    exit 1
}

# Crear .env en apps/api si no existe
if (-not (Test-Path "../../apps/api/.env")) {
    Write-Host "📝 Creando archivo .env en apps/api..." -ForegroundColor Yellow
    if (Test-Path "../../apps/api/.env.example") {
        Copy-Item "../../apps/api/.env.example" "../../apps/api/.env"
    } else {
        $envContent = @"
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
MAIL_FROM_NAME="`${APP_NAME}"

SANCTUM_STATEFUL_DOMAINS=localhost,127.0.0.1
"@
        Set-Content -Path "../../apps/api/.env" -Value $envContent
    }
}

Write-Host "🐳 Construyendo contenedores Docker..." -ForegroundColor Cyan
docker-compose build

Write-Host "🚀 Iniciando contenedores..." -ForegroundColor Cyan
docker-compose up -d

Write-Host "⏳ Esperando a que la base de datos esté lista..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "📦 Instalando dependencias de Composer..." -ForegroundColor Cyan
docker-compose exec -T app composer install

Write-Host "🔑 Generando clave de aplicación..." -ForegroundColor Cyan
docker-compose exec -T app php artisan key:generate

Write-Host "🗄️ Ejecutando migraciones..." -ForegroundColor Cyan
docker-compose exec -T app php artisan migrate --force

Write-Host "✅ ¡Configuración completada!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Información del entorno:" -ForegroundColor Cyan
Write-Host "   - API disponible en: http://localhost:8000"
Write-Host "   - Base de datos PostgreSQL en: localhost:5432"
Write-Host "   - Redis en: localhost:6379"
Write-Host ""
Write-Host "📝 Comandos útiles:" -ForegroundColor Cyan
Write-Host "   - Ver logs: docker-compose logs -f"
Write-Host "   - Detener: docker-compose down"
Write-Host "   - Reiniciar: docker-compose restart"
Write-Host "   - Acceder al contenedor: docker-compose exec app bash"
Write-Host "   - Ejecutar artisan: docker-compose exec app php artisan [comando]"

