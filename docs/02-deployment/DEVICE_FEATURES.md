# Sistema de Vinculación y Salud de Dispositivos

## Descripción

Sistema para vincular dispositivos Android al comercio mediante códigos QR/códigos numéricos, y monitorear la salud de los dispositivos.

## Características

- ✅ Vinculación de dispositivos por código/QR
- ✅ Sistema de salud de dispositivos
- ✅ Generación de códigos de vinculación con expiración
- ✅ Validación de códigos antes de vincular

## Componentes

### Vinculación de Dispositivos

**Backend:**
- Tabla `device_link_codes` para códigos de vinculación
- Endpoints:
  - `POST /api/devices/generate-link-code` - Generar código (admin)
  - `GET /api/devices/link-code/{code}` - Validar código (público)
  - `POST /api/devices/link-by-code` - Vincular dispositivo
  - `GET /api/devices/link-codes` - Listar códigos activos

**Android:**
- `LinkDeviceActivity` para escanear QR/ingresar código
- Escaneo QR con ML Kit
- Validación de código antes de vincular

**Dashboard Web:**
- `AddDevicePage` para generar QR/código
- Visualización de códigos activos

### Salud de Dispositivos

**Campos en tabla `devices`:**
- `battery_level` - Nivel de batería
- `battery_optimization_disabled` - Si la optimización está desactivada
- `notification_permission_enabled` - Si el permiso de notificaciones está activo
- `last_heartbeat` - Última vez que el dispositivo reportó su estado

**Endpoint:**
- `POST /api/devices/{id}/health` - Actualizar salud del dispositivo

**Dashboard Web:**
- Indicadores visuales de salud
- Estado online/offline
- Alertas cuando el dispositivo está offline o con problemas

## Flujo de Vinculación

1. **Admin genera código**: Desde dashboard web, genera código QR/código numérico
2. **Código expira**: Los códigos expiran en 24 horas (configurable)
3. **Captador escanea**: App Android escanea QR o ingresa código
4. **Validación**: Backend valida que el código sea válido y no haya expirado
5. **Vinculación**: Dispositivo se asocia al commerce del código

## Migraciones

1. `2025_01_20_000001_create_device_link_codes_table.php` - Tabla para códigos
2. `2025_01_20_000002_add_health_fields_to_devices_table.php` - Campos de salud

## Despliegue

Ver `DEPLOYMENT.md` para guía completa de despliegue.

### Pasos Rápidos

```bash
# 1. Actualizar código
git pull origin main

# 2. Ir a producción
cd infra/docker/environments/production

# 3. Ejecutar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate --force

# 4. Reiniciar servicios
docker compose --env-file .env restart
```

## Referencias

- **Deployment**: Ver `DEPLOYMENT.md`
- **Arquitectura**: Ver `../03-architecture/`
- **Estado de implementación**: Ver `../07-reference/IMPLEMENTATION_STATUS.md`

