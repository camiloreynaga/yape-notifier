#!/bin/sh
set -e

# Docker entrypoint script para PHP-FPM
# Asegura que los permisos de Laravel estén correctos al iniciar el contenedor

echo "🔧 Verificando permisos de Laravel..."

# Crear directorios si no existen
mkdir -p /var/www/storage/framework/sessions
mkdir -p /var/www/storage/framework/views
mkdir -p /var/www/storage/framework/cache/data
mkdir -p /var/www/storage/logs
mkdir -p /var/www/bootstrap/cache

# Asegurar permisos correctos
# Solo cambiar permisos si el propietario no es www-data (evita problemas en builds)
if [ "$(stat -c '%U' /var/www/storage 2>/dev/null || echo 'www-data')" != "www-data" ]; then
    echo "📝 Ajustando permisos de storage y bootstrap/cache..."
    chown -R www-data:www-data /var/www/storage /var/www/bootstrap/cache 2>/dev/null || true
    chmod -R 775 /var/www/storage /var/www/bootstrap/cache 2>/dev/null || true
fi

# Verificar que Laravel puede escribir en storage/logs
if [ -w /var/www/storage/logs ] || [ "$(stat -c '%U' /var/www/storage/logs 2>/dev/null || echo 'www-data')" = "www-data" ]; then
    echo "✅ Permisos de storage/logs correctos"
else
    echo "⚠️  Advertencia: storage/logs puede no tener permisos de escritura"
fi

echo "✅ Verificación de permisos completada"

# Ejecutar el comando original (php-fpm o el comando pasado)
exec "$@"

