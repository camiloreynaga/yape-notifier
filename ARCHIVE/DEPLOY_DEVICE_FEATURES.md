# 🚀 Guía de Despliegue - Sistema de Vinculación y Salud de Dispositivos

Esta guía describe los pasos para desplegar en producción los nuevos sistemas:

- ✅ Vinculación de dispositivos por código/QR
- ✅ Sistema de salud de dispositivos

## 📋 Resumen de Cambios

### Nuevas Migraciones

1. `2025_01_20_000001_create_device_link_codes_table.php` - Tabla para códigos de vinculación
2. `2025_01_20_000002_add_health_fields_to_devices_table.php` - Campos de salud en devices

### Nuevos Endpoints

- `POST /api/devices/generate-link-code` - Generar código de vinculación
- `GET /api/devices/link-code/{code}` - Validar código (público)
- `POST /api/devices/link-by-code` - Vincular dispositivo
- `GET /api/devices/link-codes` - Listar códigos activos
- `POST /api/devices/{id}/health` - Actualizar salud del dispositivo

## 🚀 Pasos de Despliegue

### Opción 1: Script Automático (Recomendado)

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

# 6. Cuando el script pregunte sobre migraciones, responder 's' (sí)
```

El script realizará automáticamente:

- ✅ Backup de base de datos
- ✅ Reconstrucción de imágenes Docker
- ✅ Ejecución de migraciones
- ✅ Reinicio de servicios
- ✅ Limpieza de caches

### Opción 2: Despliegue Manual

```bash
# 1. Conectarse al servidor
ssh usuario@servidor

# 2. Ir al directorio del proyecto
cd /ruta/al/proyecto/yape-notifier

# 3. Actualizar código
git pull origin main

# 4. Ir al directorio de producción
cd infra/docker/environments/production

# 5. ⚠️ CRÍTICO: Crear backup de base de datos
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz

# 6. Verificar integridad del backup
gunzip -t backup_*.sql.gz

# 7. Reconstruir imágenes Docker
docker compose --env-file .env build

# 8. Verificar migraciones pendientes
docker compose --env-file .env exec php-fpm php artisan migrate:status

# 9. Ejecutar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate --force

# 10. Reiniciar servicios
docker compose --env-file .env up -d

# 11. Limpiar caches de Laravel
docker compose --env-file .env exec php-fpm php artisan config:clear
docker compose --env-file .env exec php-fpm php artisan route:clear
docker compose --env-file .env exec php-fpm php artisan cache:clear

# 12. Optimizar Laravel (opcional pero recomendado)
docker compose --env-file .env exec php-fpm php artisan config:cache
docker compose --env-file .env exec php-fpm php artisan route:cache
```

## ✅ Verificación Post-Despliegue

### 1. Verificar Migraciones

```bash
docker compose --env-file .env exec php-fpm php artisan migrate:status
```

Debes ver:

- ✅ `2025_01_20_000001_create_device_link_codes_table` - Ran
- ✅ `2025_01_20_000002_add_health_fields_to_devices_table` - Ran

### 2. Verificar Estructura de Tablas

```bash
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:

```php
// Verificar tabla device_link_codes
Schema::hasTable('device_link_codes');
// Debe retornar: true

// Verificar campos de salud en devices
Schema::hasColumn('devices', 'battery_level');
Schema::hasColumn('devices', 'battery_optimization_disabled');
Schema::hasColumn('devices', 'notification_permission_enabled');
Schema::hasColumn('devices', 'last_heartbeat');
// Todos deben retornar: true

exit
```

### 3. Verificar Endpoints

#### Endpoint de Generar Código (requiere auth + admin)

```bash
curl -X POST \
  -H "Authorization: Bearer TU_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.notificaciones.space/api/devices/generate-link-code
```

Respuesta esperada:

```json
{
  "message": "Código de vinculación generado exitosamente",
  "code": "ABC12345",
  "expires_at": "2025-01-21T10:30:00Z",
  "link_code": { ... }
}
```

#### Endpoint de Validar Código (público)

```bash
curl https://api.notificaciones.space/api/devices/link-code/ABC12345
```

Respuesta esperada (si el código es válido):

```json
{
  "valid": true,
  "message": "Código válido",
  "commerce": {
    "id": 1,
    "name": "Mi Negocio"
  }
}
```

#### Endpoint de Salud (requiere auth)

```bash
curl -X POST \
  -H "Authorization: Bearer TU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "battery_level": 85,
    "battery_optimization_disabled": true,
    "notification_permission_enabled": true
  }' \
  https://api.notificaciones.space/api/devices/1/health
```

Respuesta esperada:

```json
{
  "message": "Device health updated successfully",
  "device": { ... },
  "health": {
    "is_online": true,
    "battery_level": 85,
    "battery_optimization_disabled": true,
    "notification_permission_enabled": true,
    "last_heartbeat": "2025-01-20T10:30:00Z",
    "last_seen_at": "2025-01-20T10:25:00Z"
  }
}
```

### 4. Verificar Estado de Servicios

```bash
# Ver estado de contenedores
docker compose --env-file .env ps

# Ver logs en tiempo real
docker compose --env-file .env logs -f php-fpm

# Ver logs de Laravel
docker compose --env-file .env exec php-fpm tail -f storage/logs/laravel.log
```

### 5. Probar Flujo Completo

1. **Generar código de vinculación** desde dashboard o API
2. **Validar código** (endpoint público)
3. **Vincular dispositivo** usando el código
4. **Enviar datos de salud** desde dispositivo Android
5. **Verificar en dashboard** que se muestre la información de salud

## 🔍 Troubleshooting

### Error: "Table 'device_link_codes' already exists"

**Causa**: La migración ya se ejecutó anteriormente.

**Solución**: Verificar estado de migraciones:

```bash
docker compose --env-file .env exec php-fpm php artisan migrate:status
```

Si la migración está como "Ran", no hay problema. Si está como "Pending" pero la tabla existe, puedes marcar la migración como ejecutada:

```bash
docker compose --env-file .env exec php-fpm php artisan migrate:status --pretend
```

### Error: "Column 'battery_level' already exists"

**Causa**: Los campos de salud ya fueron agregados.

**Solución**: Verificar si los campos existen:

```bash
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:

```php
Schema::hasColumn('devices', 'battery_level');
// Si retorna true, los campos ya existen, no hay problema
```

### Error: "Route [devices.generate-link-code] not defined"

**Causa**: Las rutas no se cargaron correctamente.

**Solución**:

```bash
# Limpiar cache de rutas
docker compose --env-file .env exec php-fpm php artisan route:clear
docker compose --env-file .env exec php-fpm php artisan route:cache

# Verificar rutas
docker compose --env-file .env exec php-fpm php artisan route:list | grep device
```

### Error: "Class 'DeviceLinkCode' not found"

**Causa**: El autoloader no se actualizó.

**Solución**:

```bash
# Regenerar autoloader
docker compose --env-file .env exec php-fpm composer dump-autoload

# Limpiar caches
docker compose --env-file .env exec php-fpm php artisan config:clear
docker compose --env-file .env exec php-fpm php artisan cache:clear
```

## 📊 Verificación de Datos

### Verificar Códigos de Vinculación

```bash
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:

```php
// Contar códigos activos
\App\Models\DeviceLinkCode::valid()->count();

// Ver códigos recientes
\App\Models\DeviceLinkCode::latest()->take(5)->get(['code', 'commerce_id', 'expires_at', 'used_at']);

// Ver códigos expirados
\App\Models\DeviceLinkCode::expired()->whereNull('used_at')->count();

exit
```

### Verificar Salud de Dispositivos

```bash
docker compose --env-file .env exec php-fpm php artisan tinker
```

En tinker:

```php
// Dispositivos con datos de salud
\App\Models\Device::whereNotNull('last_heartbeat')->count();

// Dispositivos online (últimos 5 minutos)
\App\Models\Device::whereNotNull('last_heartbeat')
    ->where('last_heartbeat', '>', now()->subMinutes(5))
    ->count();

// Dispositivos con batería baja (< 20%)
\App\Models\Device::whereNotNull('battery_level')
    ->where('battery_level', '<', 20)
    ->count();

exit
```

## 🔄 Rollback (Si es Necesario)

Si necesitas revertir los cambios:

```bash
# 1. Restaurar backup de base de datos
cd infra/docker/environments/production
gunzip < backup_YYYYMMDD_HHMMSS.sql.gz | docker compose --env-file .env exec -T db psql -U postgres yape_notifier

# 2. Revertir código (volver a commit anterior)
cd /ruta/al/proyecto/yape-notifier
git log --oneline  # Ver commits
git checkout <commit-anterior>
cd infra/docker/environments/production
docker compose --env-file .env build
docker compose --env-file .env up -d
```

**⚠️ IMPORTANTE**: El rollback eliminará:

- Todos los códigos de vinculación generados
- Todos los datos de salud de dispositivos

## 📝 Checklist de Despliegue

- [ ] Código actualizado en el servidor (`git pull`)
- [ ] Backup de base de datos creado y verificado
- [ ] Imágenes Docker reconstruidas
- [ ] Migraciones ejecutadas correctamente
- [ ] Servicios reiniciados
- [ ] Caches de Laravel limpiados
- [ ] Endpoint `/api/devices/generate-link-code` verificado
- [ ] Endpoint `/api/devices/link-code/{code}` verificado (público)
- [ ] Endpoint `/api/devices/{id}/health` verificado
- [ ] Tabla `device_link_codes` verificada
- [ ] Campos de salud en `devices` verificados
- [ ] Logs revisados (sin errores críticos)
- [ ] Dashboard muestra información de salud correctamente

## 🎯 Resultado Esperado

Después del despliegue:

1. ✅ Tabla `device_link_codes` creada y funcionando
2. ✅ Campos de salud agregados a `devices`
3. ✅ Endpoints de vinculación funcionando
4. ✅ Endpoint de salud funcionando
5. ✅ Dashboard muestra información de salud
6. ✅ Los dispositivos pueden reportar su estado de salud
7. ✅ Los códigos de vinculación se generan y validan correctamente

## 📞 Soporte

Si encuentras problemas durante el despliegue:

1. **Revisa los logs**:

   ```bash
   docker compose --env-file .env logs -f php-fpm
   docker compose --env-file .env exec php-fpm tail -f storage/logs/laravel.log
   ```

2. **Verifica el estado de los servicios**:

   ```bash
   docker compose --env-file .env ps
   ```

3. **Verifica las migraciones**:

   ```bash
   docker compose --env-file .env exec php-fpm php artisan migrate:status
   ```

4. **Verifica las rutas**:
   ```bash
   docker compose --env-file .env exec php-fpm php artisan route:list | grep device
   ```

---

**Última actualización**: 2025-01-20
**Versión**: 1.0
