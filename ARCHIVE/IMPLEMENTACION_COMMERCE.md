# Guía de Implementación - Sistema Commerce

Esta guía describe los pasos necesarios para implementar los cambios del sistema Commerce en el servidor de producción.

## 📋 Resumen de Cambios

Los cambios implementados incluyen:
- ✅ Creación automática de commerce al registrar usuarios nuevos
- ✅ Seeder para migrar usuarios existentes
- ✅ Validación y creación automática de commerce en NotificationService
- ✅ Mejoras en manejo de errores y logging
- ✅ Nuevo endpoint `/api/commerces/check`
- ✅ Sincronización automática de commerce_id en DeviceService

## 🚀 Pasos de Implementación en el Servidor

### Opción 1: Usando el Script de Actualización (Recomendado)

El script `update.sh` ya incluye backup automático y rollback.

```bash
# 1. Conectarse al servidor
ssh usuario@servidor

# 2. Ir al directorio del proyecto
cd /ruta/al/proyecto/yape-notifier

# 3. Actualizar código desde Git
git pull origin main
# O si estás en otra rama: git pull origin master

# 4. Ir al directorio de producción
cd infra/docker/environments/production

# 5. Ejecutar script de actualización
./update.sh
```

El script realizará:
- ✅ Backup automático de la base de datos
- ✅ Verificación del estado actual
- ✅ Reconstrucción de imágenes Docker
- ✅ Ejecución de migraciones
- ✅ Reinicio de servicios
- ✅ Verificación de salud

**IMPORTANTE**: Cuando el script pregunte sobre migraciones, confirma la ejecución.

### Opción 2: Despliegue Manual

Si prefieres control total sobre cada paso:

```bash
# 1. Conectarse al servidor
ssh usuario@servidor

# 2. Ir al directorio del proyecto
cd /ruta/al/proyecto/yape-notifier

# 3. Actualizar código
git pull origin main

# 4. Ir al directorio de producción
cd infra/docker/environments/production

# 5. Crear backup manual (RECOMENDADO)
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier > backup_$(date +%Y%m%d_%H%M%S).sql

# 6. Reconstruir imágenes Docker
docker compose --env-file .env build

# 7. Ejecutar migraciones (si hay nuevas)
docker compose --env-file .env exec php-fpm php artisan migrate --force

# 8. Reiniciar servicios
docker compose --env-file .env up -d

# 9. Limpiar caches de Laravel
docker compose --env-file .env exec php-fpm php artisan config:clear
docker compose --env-file .env exec php-fpm php artisan route:clear
docker compose --env-file .env exec php-fpm php artisan cache:clear
```

## 🔄 Paso Crítico: Migrar Usuarios Existentes

**Este paso es OBLIGATORIO** para usuarios que ya existen en el sistema sin `commerce_id`.

```bash
# Ejecutar el seeder para crear commerce para usuarios existentes
docker compose --env-file .env exec php-fpm php artisan db:seed --class=UpdateExistingUsersCommerceSeeder
```

Este seeder:
- ✅ Busca todos los usuarios sin `commerce_id`
- ✅ Crea un commerce para cada uno
- ✅ Asigna el commerce al usuario (rol: admin)
- ✅ Actualiza dispositivos asociados
- ✅ Usa transacciones para garantizar consistencia
- ✅ Registra todo en logs

**Verificación después del seeder:**

```bash
# Verificar que los usuarios tienen commerce
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:
```php
// Contar usuarios sin commerce
User::whereNull('commerce_id')->count();
// Debe retornar 0

// Verificar que los commerces se crearon
Commerce::count();
// Debe ser igual o mayor al número de usuarios
```

## ✅ Verificación Post-Implementación

### 1. Verificar Estado de Servicios

```bash
# Ver estado de contenedores
docker compose --env-file .env ps

# Ver logs
docker compose --env-file .env logs -f php-fpm
```

### 2. Verificar Endpoints

```bash
# Verificar endpoint de check
curl -H "Authorization: Bearer TU_TOKEN" https://api.notificaciones.space/api/commerces/check

# Debe retornar:
# {
#   "has_commerce": true,
#   "commerce_id": 1
# }
```

### 3. Probar Flujo Completo

1. **Registrar nuevo usuario** (debe crear commerce automáticamente)
2. **Verificar que tiene commerce**: `GET /api/commerces/check`
3. **Enviar notificación** (debe funcionar sin errores 500)
4. **Verificar logs** para confirmar que todo funciona

### 4. Verificar Logs

```bash
# Ver logs de Laravel
docker compose --env-file .env exec php-fpm tail -f storage/logs/laravel.log

# Buscar errores relacionados con commerce
docker compose --env-file .env exec php-fpm grep -i "commerce" storage/logs/laravel.log | tail -20
```

## 🔍 Troubleshooting

### Error: "No se puede crear commerce"

**Síntoma**: El seeder falla al crear commerce.

**Solución**:
```bash
# Verificar que la tabla commerce existe
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:
```php
// Verificar estructura
Schema::hasTable('commerces');
// Debe retornar true

// Verificar migraciones
DB::table('migrations')->where('migration', 'like', '%commerce%')->get();
```

### Error: "Usuarios sin commerce después del seeder"

**Síntoma**: Algunos usuarios siguen sin commerce.

**Solución**:
```bash
# Ejecutar seeder nuevamente (es idempotente)
docker compose --env-file .env exec php-fpm php artisan db:seed --class=UpdateExistingUsersCommerceSeeder
```

### Error: "500 Internal Server Error en notificaciones"

**Síntoma**: Las notificaciones fallan con error 500.

**Solución**:
1. Verificar logs: `docker compose --env-file .env exec php-fpm tail -f storage/logs/laravel.log`
2. Verificar que el usuario tiene commerce:
   ```bash
   docker compose --env-file .env exec php-fpm php artisan tinker
   ```
   ```php
   $user = User::find(USER_ID);
   $user->commerce_id; // No debe ser null
   ```
3. Si el usuario no tiene commerce, ejecutar el seeder nuevamente

### Error: "Device sin commerce_id"

**Síntoma**: Los dispositivos no tienen commerce_id.

**Solución**:
El sistema ahora sincroniza automáticamente, pero puedes forzar la actualización:

```bash
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:
```php
// Actualizar dispositivos sin commerce_id
Device::whereNull('commerce_id')
    ->whereHas('user', function($q) {
        $q->whereNotNull('commerce_id');
    })
    ->get()
    ->each(function($device) {
        $device->update(['commerce_id' => $device->user->commerce_id]);
    });
```

## 📊 Monitoreo Post-Implementación

### Verificar Estadísticas

```bash
# Ver usuarios con y sin commerce
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:
```php
// Usuarios con commerce
User::whereNotNull('commerce_id')->count();

// Usuarios sin commerce (debe ser 0)
User::whereNull('commerce_id')->count();

// Total de commerces
Commerce::count();

// Dispositivos con commerce
Device::whereNotNull('commerce_id')->count();

// Dispositivos sin commerce
Device::whereNull('commerce_id')->count();
```

### Verificar Notificaciones

```bash
# Ver notificaciones recientes
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:
```php
// Notificaciones con commerce_id
Notification::whereNotNull('commerce_id')->count();

// Notificaciones sin commerce_id (puede haber algunas antiguas)
Notification::whereNull('commerce_id')->count();

// Ver últimas notificaciones
Notification::latest()->take(10)->get(['id', 'commerce_id', 'created_at']);
```

## 🔄 Rollback (Si es Necesario)

Si necesitas revertir los cambios:

```bash
# Si usaste update.sh, el script creó un rollback automático
# Busca el archivo rollback_*.sh en el directorio de backups

# O restaurar desde backup manual
cd infra/docker/environments/production
docker compose --env-file .env exec -T db psql -U postgres yape_notifier < backup_YYYYMMDD_HHMMSS.sql
```

## 📝 Checklist de Implementación

- [ ] Código actualizado en el servidor (`git pull`)
- [ ] Backup de base de datos creado
- [ ] Imágenes Docker reconstruidas
- [ ] Migraciones ejecutadas (si hay nuevas)
- [ ] Seeder ejecutado para usuarios existentes
- [ ] Servicios reiniciados
- [ ] Caches de Laravel limpiados
- [ ] Endpoint `/api/commerces/check` verificado
- [ ] Registro de nuevo usuario probado
- [ ] Envío de notificación probado
- [ ] Logs revisados (sin errores críticos)
- [ ] Estadísticas verificadas (todos los usuarios tienen commerce)

## 🎯 Resultado Esperado

Después de la implementación:

1. ✅ Todos los usuarios existentes tienen `commerce_id`
2. ✅ Los usuarios nuevos reciben commerce automáticamente
3. ✅ Las notificaciones se crean sin errores 500
4. ✅ Los dispositivos tienen `commerce_id` sincronizado
5. ✅ El endpoint `/api/commerces/check` funciona correctamente
6. ✅ Los logs muestran operaciones exitosas

## 📞 Soporte

Si encuentras problemas durante la implementación:

1. Revisa los logs: `docker compose --env-file .env logs -f`
2. Verifica el estado de los servicios: `docker compose --env-file .env ps`
3. Revisa los logs de Laravel: `storage/logs/laravel.log`
4. Ejecuta verificaciones en tinker para diagnosticar problemas

---

**Última actualización**: 2025-01-XX
**Versión**: 1.0

