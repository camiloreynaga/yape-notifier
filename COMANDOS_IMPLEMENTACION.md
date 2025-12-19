# 🚀 Comandos para Implementar Commerce en el Servidor

## Pasos Rápidos (Usando Script Automático)

```bash
# 1. Conectarse al servidor
ssh usuario@servidor

# 2. Ir al proyecto y actualizar código
cd /ruta/al/proyecto/yape-notifier
git pull origin main  # o master, según tu rama

# 3. Ir a producción y ejecutar script
cd infra/docker/environments/production
./update.sh

# 4. Cuando el script pregunte sobre migraciones, responde 's' (sí)

# 5. DESPUÉS del script, ejecutar el seeder para usuarios existentes
docker compose --env-file .env exec php-fpm php artisan db:seed --class=UpdateExistingUsersCommerceSeeder
```

## Pasos Manuales (Si Prefieres Control Total)

```bash
# 1. Conectarse y actualizar código
ssh usuario@servidor
cd /ruta/al/proyecto/yape-notifier
git pull origin main

# 2. Crear backup
cd infra/docker/environments/production
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier > backup_$(date +%Y%m%d_%H%M%S).sql

# 3. Reconstruir imágenes
docker compose --env-file .env build

# 4. Ejecutar migraciones (si hay nuevas)
docker compose --env-file .env exec php-fpm php artisan migrate --force

# 5. Reiniciar servicios
docker compose --env-file .env up -d

# 6. Limpiar caches
docker compose --env-file .env exec php-fpm php artisan config:clear
docker compose --env-file .env exec php-fpm php artisan route:clear
docker compose --env-file .env exec php-fpm php artisan cache:clear

# 7. ⚠️ CRÍTICO: Migrar usuarios existentes
docker compose --env-file .env exec php-fpm php artisan db:seed --class=UpdateExistingUsersCommerceSeeder
```

## Verificación Rápida

```bash
# Verificar que no hay usuarios sin commerce
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:

```php
User::whereNull('commerce_id')->count();  // Debe ser 0
Commerce::count();  // Debe ser >= número de usuarios
exit
```

## Verificar Endpoint Nuevo

```bash
# Probar endpoint de verificación
curl -H "Authorization: Bearer TU_TOKEN" \
  https://api.notificaciones.space/api/commerces/check
```

## Ver Logs

```bash
# Ver logs en tiempo real
docker compose --env-file .env logs -f php-fpm

# Ver logs de Laravel
docker compose --env-file .env exec php-fpm tail -f storage/logs/laravel.log
```

---

**⚠️ IMPORTANTE**: El paso del seeder es OBLIGATORIO para usuarios existentes.
Sin él, los usuarios antiguos seguirán sin commerce y causarán errores 500.
