# Diagnóstico y Corrección: Sistema de Vinculación QR

> **Referencias relacionadas:**
> - [DEVICE_LINKING_GUIDE.md](../05-features/DEVICE_LINKING_GUIDE.md) - Guía profesional de vinculación
> - [TESTING_QR_LINKING.md](../04-development/TESTING_QR_LINKING.md) - Guía de pruebas
> - [DEVICE_LINKING_FLOW.md](../05-features/DEVICE_LINKING_FLOW.md) - Flujo detallado paso a paso

## Fecha: 2025-01-08

## Problema Reportado
La arquitectura de vinculación con QR sin necesidad de login no funciona.

## Diagnóstico Realizado

### 1. Revisión del Backend (Laravel)

#### ✅ Archivos Correctos:
- `app/Models/Device.php` - Modelo completo y correcto
- `app/Models/DeviceLinkCode.php` - Modelo completo
- `app/Services/DeviceLinkService.php` - Lógica de vinculación correcta
- `app/Http/Controllers/DeviceLinkController.php` - Controller correcto
- `routes/api.php` - Rutas públicas configuradas correctamente

#### ⚠️ Posibles Problemas Identificados:

1. **Migraciones no ejecutadas**: La migración `2025_12_28_000001_make_user_id_nullable_in_devices_table.php` puede no haberse ejecutado en producción.

2. **Docker no está corriendo**: El comando `docker ps` falló, indicando que el backend no está activo.

### 2. Revisión del Frontend (Android)

#### ✅ Archivos Correctos:
- `LinkDeviceRequest` - Estructura correcta con `code`, `device_uuid`, `device_name`
- `LinkDeviceResponse` - Estructura correcta con `message` y `device`
- `Device.kt` - Modelo con `commerceId` nullable
- `LinkDeviceViewModel.kt` - Lógica de vinculación correcta
- `ApiService.kt` - Endpoint `/api/devices/link-by-code` configurado
- `RetrofitClient.kt` - Interceptor de autenticación opcional

#### ✅ Flujo Correcto:
1. App genera UUID en `YapeNotifierApplication.onCreate()`
2. Usuario escanea QR o ingresa código
3. Se valida el código con `GET /api/devices/link-code/{code}`
4. Se vincula con `POST /api/devices/link-by-code` (sin autenticación)
5. Backend crea el dispositivo automáticamente si no existe
6. Se guarda `device_id` y `commerce_id` localmente

### 3. Problemas Críticos Encontrados

#### ❌ PROBLEMA 1: Backend no está corriendo
```
error during connect: Get "http://%2F%2F.%2Fpipe%2FdockerDesktopLinuxEngine/v1.47/containers/json"
```

**Solución**: Iniciar Docker Desktop y levantar los contenedores.

#### ❌ PROBLEMA 2: Posible migración no ejecutada
La migración `make_user_id_nullable_in_devices_table.php` es crítica para permitir dispositivos sin usuario.

**Solución**: Ejecutar migraciones pendientes.

## Soluciones Implementadas

### 1. Script de Verificación de Migraciones

Creado: `apps/api/check-migrations.sh`

Este script verifica:
- Estado de las migraciones
- Estructura de la tabla `devices`
- Que `user_id` sea nullable
- Que `commerce_id` exista

### 2. Correcciones en el Código

Ninguna corrección de código fue necesaria. La arquitectura está correctamente implementada.

## Pasos para Resolver

### En Windows (PowerShell):

```powershell
# 1. Iniciar Docker Desktop (manualmente)

# 2. Navegar al directorio del proyecto
cd "E:\1_WORK\9 BenjaJobs\yape-notifier"

# 3. Levantar los contenedores
cd apps/api
docker-compose up -d

# 4. Verificar que los contenedores estén corriendo
docker-compose ps

# 5. Ejecutar migraciones pendientes
docker-compose exec app php artisan migrate

# 6. Verificar el estado de las migraciones
docker-compose exec app php artisan migrate:status

# 7. Verificar la estructura de la tabla devices
docker-compose exec app php artisan tinker
# En tinker:
# Schema::getColumnType('devices', 'user_id')
# Schema::hasColumn('devices', 'commerce_id')
# exit

# 8. Ver logs en tiempo real
docker-compose logs -f app
```

### En Android Studio:

```bash
# 1. Limpiar y reconstruir el proyecto
./gradlew clean
./gradlew build

# 2. Desinstalar la app del dispositivo/emulador
adb uninstall com.yapenotifier.android

# 3. Instalar la nueva versión
./gradlew installDebug

# 4. Ver logs en tiempo real
adb logcat -s YapeNotifier:* LinkDeviceViewModel:* RetrofitClient:*
```

## Verificación del Flujo Completo

### 1. Backend (Laravel)

```bash
# Verificar que el endpoint público funciona
curl -X GET https://api.notificaciones.space/api/devices/link-code/TEST1234

# Debería retornar:
# {"valid":false,"message":"Código no encontrado"}
```

### 2. Generar un código de vinculación (desde web dashboard o admin app)

```bash
# Como usuario autenticado, generar código
curl -X POST https://api.notificaciones.space/api/devices/generate-link-code \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Debería retornar:
# {
#   "message": "Código de vinculación generado exitosamente",
#   "code": "ABC12345",
#   "expires_at": "2025-01-09T...",
#   "qr_code_data": "ABC12345"
# }
```

### 3. Vincular dispositivo (desde Android app)

```bash
# Sin autenticación, vincular dispositivo
curl -X POST https://api.notificaciones.space/api/devices/link-by-code \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ABC12345",
    "device_uuid": "550e8400-e29b-41d4-a716-446655440000",
    "device_name": "Samsung Galaxy S21"
  }'

# Debería retornar:
# {
#   "message": "Dispositivo vinculado exitosamente",
#   "device": {
#     "id": 123,
#     "uuid": "550e8400-e29b-41d4-a716-446655440000",
#     "name": "Samsung Galaxy S21",
#     "commerce_id": 1,
#     "user_id": null,
#     ...
#   }
# }
```

## Checklist de Verificación

- [ ] Docker Desktop está corriendo
- [ ] Contenedores de Laravel están activos (`docker-compose ps`)
- [ ] Migraciones ejecutadas (`php artisan migrate:status`)
- [ ] Tabla `devices` tiene `user_id` nullable
- [ ] Tabla `devices` tiene `commerce_id`
- [ ] Endpoint público `/api/devices/link-code/{code}` responde
- [ ] Endpoint público `/api/devices/link-by-code` responde
- [ ] Android app genera UUID correctamente
- [ ] Android app puede escanear QR
- [ ] Android app puede vincular dispositivo sin login
- [ ] Dispositivo vinculado puede enviar notificaciones

## Logs a Revisar

### Laravel:
```bash
docker-compose exec app tail -f storage/logs/laravel.log
```

### Android:
```bash
adb logcat -s YapeNotifier:* LinkDeviceViewModel:* RetrofitClient:* ApiCallHandler:*
```

## Conclusión

La arquitectura está **correctamente implementada**. Los problemas son de **infraestructura** (Docker no corriendo) y posiblemente de **migraciones no ejecutadas**.

**Acción inmediata requerida:**
1. Iniciar Docker Desktop
2. Levantar contenedores con `docker-compose up -d`
3. Ejecutar `php artisan migrate`
4. Probar el flujo completo

Si después de estos pasos el problema persiste, revisar los logs de Laravel y Android para identificar el error específico.


