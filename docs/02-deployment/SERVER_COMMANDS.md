# Comandos para ejecutar en el servidor

# 1. Actualizar cÃ³digo
cd /var/apps/yape-notifier
git pull origin tenant-version

# 2. Agregar columna is_active si no existe
cd infra/docker/environments/production
chmod +x fix-is-active-column.sh
./fix-is-active-column.sh

# 3. Si la migraciÃ³n estÃ¡ registrada como fallida, eliminarla
docker compose --env-file .env exec php-fpm php artisan tinker
# Dentro de tinker:
# DB::table('migrations')->where('migration', '2026_01_10_000002_make_user_id_required_in_devices')->delete();
# exit

# 4. Re-ejecutar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate

# 5. Verificar
docker compose --env-file .env exec php-fpm php artisan migrate:status



