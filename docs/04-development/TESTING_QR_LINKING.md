# Guía de Pruebas: Sistema de Vinculación QR

> **Referencias relacionadas:**
> - [DEVICE_LINKING_GUIDE.md](../05-features/DEVICE_LINKING_GUIDE.md) - Guía profesional de vinculación
> - [DEVICE_LINKING_FLOW.md](../05-features/DEVICE_LINKING_FLOW.md) - Flujo detallado paso a paso
> - [TROUBLESHOOTING.md](../06-operations/TROUBLESHOOTING.md) - Diagnóstico y solución de problemas

## Objetivo
Probar el flujo completo de vinculación de dispositivos mediante código QR sin necesidad de autenticación de usuario.

## Prerequisitos

### Backend (Laravel)
- [ ] Docker Desktop está corriendo
- [ ] Contenedores levantados: `docker-compose ps`
- [ ] Migraciones ejecutadas: `docker-compose exec app php artisan migrate`
- [ ] Base de datos accesible

### Android
- [ ] Android Studio instalado
- [ ] Dispositivo físico o emulador configurado
- [ ] App compilada y lista para instalar

## Paso 1: Verificar Backend

### 1.1 Levantar el Backend

```powershell
# En Windows PowerShell
cd "E:\1_WORK\9 BenjaJobs\yape-notifier\apps\api"

# Iniciar contenedores
docker-compose up -d

# Verificar que estén corriendo
docker-compose ps

# Debería mostrar:
# NAME                IMAGE               STATUS
# yape-api-app        ...                 Up
# yape-api-db         ...                 Up
```

### 1.2 Ejecutar Migraciones

```powershell
# Ejecutar migraciones pendientes
docker-compose exec app php artisan migrate --force

# Verificar estado
docker-compose exec app php artisan migrate:status

# Ejecutar script de verificación
docker-compose exec app bash check-migrations.sh
```

### 1.3 Verificar Rutas Públicas

```powershell
# Listar rutas de vinculación
docker-compose exec app php artisan route:list --path=devices/link

# Debería mostrar:
# GET|HEAD  api/devices/link-code/{code} ......... (public)
# POST      api/devices/link-by-code ............. (public)
```

### 1.4 Ver Logs en Tiempo Real

```powershell
# En una terminal separada
docker-compose logs -f app
```

## Paso 2: Crear un Commerce y Usuario Admin (Web Dashboard)

### 2.1 Registrar Usuario Admin

```bash
# Opción 1: Usar la web dashboard
# Navegar a: https://dashboard.notificaciones.space/register

# Opción 2: Usar curl
curl -X POST https://api.notificaciones.space/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin Test",
    "email": "admin@test.com",
    "password": "password123",
    "password_confirmation": "password123"
  }'
```

### 2.2 Crear Commerce

```bash
curl -X POST https://api.notificaciones.space/api/commerces \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mi Negocio Test"
  }'
```

## Paso 3: Generar Código de Vinculación

### 3.1 Desde Web Dashboard

1. Login en https://dashboard.notificaciones.space
2. Ir a "Dispositivos" → "Agregar Dispositivo"
3. Click en "Generar Código QR"
4. Se genera un código de 8 caracteres (ej: `ABC12345`)
5. Se muestra un QR code con ese código

### 3.2 Desde API (curl)

```bash
curl -X POST https://api.notificaciones.space/api/devices/generate-link-code \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Respuesta esperada:
# {
#   "message": "Código de vinculación generado exitosamente",
#   "code": "ABC12345",
#   "expires_at": "2025-01-09T12:00:00.000000Z",
#   "qr_code_data": "ABC12345"
# }
```

### 3.3 Verificar Código Generado

```bash
# Verificar que el código es válido
curl -X GET https://api.notificaciones.space/api/devices/link-code/ABC12345

# Respuesta esperada:
# {
#   "valid": true,
#   "message": "Código válido",
#   "commerce": {
#     "id": 1,
#     "name": "Mi Negocio Test"
#   }
# }
```

## Paso 4: Probar Vinculación desde Android

### 4.1 Instalar App en Dispositivo

```bash
# Navegar al proyecto Android
cd "E:\1_WORK\9 BenjaJobs\yape-notifier\apps\android-client"

# Limpiar y compilar
./gradlew clean
./gradlew assembleDebug

# Instalar en dispositivo
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4.2 Iniciar App y Ver Logs

```bash
# En una terminal separada, ver logs de Android
adb logcat -c  # Limpiar logs
adb logcat -s YapeNotifier:* LinkDeviceViewModel:* RetrofitClient:* ApiCallHandler:*
```

### 4.3 Flujo de Vinculación en la App

1. **Abrir la app** - Debería mostrar `LinkDeviceActivity`
2. **Verificar UUID generado** - Buscar en logs:
   ```
   YapeNotifierApplication: Device UUID generado y guardado: [UUID]
   ```
3. **Escanear QR o ingresar código** - Usar el código `ABC12345`
4. **Validar código** - Buscar en logs:
   ```
   LinkDeviceViewModel: Intentando vincular dispositivo. UUID: [UUID], Autenticado: false
   ```
5. **Vincular dispositivo** - Click en "Vincular"
6. **Verificar éxito** - Buscar en logs:
   ```
   LinkDeviceViewModel: Device ID saved: [ID]
   LinkDeviceViewModel: Device linked successfully: [ID], Commerce ID: [COMMERCE_ID]
   ```

### 4.4 Verificar en Backend

```bash
# Ver logs de Laravel
docker-compose logs -f app | grep "Device linked to commerce"

# Debería mostrar:
# Device linked to commerce via code: device_id=X, commerce_id=Y, code=ABC12345, was_created=true
```

### 4.5 Verificar en Base de Datos

```bash
# Conectar a la base de datos
docker-compose exec app php artisan tinker

# En tinker:
$device = \App\Models\Device::latest()->first();
echo "Device ID: " . $device->id . PHP_EOL;
echo "UUID: " . $device->uuid . PHP_EOL;
echo "Commerce ID: " . $device->commerce_id . PHP_EOL;
echo "User ID: " . ($device->user_id ?? 'NULL') . PHP_EOL;
echo "Name: " . $device->name . PHP_EOL;
echo "Is Active: " . ($device->is_active ? 'YES' : 'NO') . PHP_EOL;

# Verificar que:
# - commerce_id NO es NULL
# - user_id ES NULL (porque no hay login)
# - is_active es true
```

## Paso 5: Probar Envío de Notificaciones

### 5.1 Desde la App Android

1. **Navegar a MainActivity** - Después de vincular
2. **Capturar una notificación de Yape/Plin**
3. **Verificar en logs**:
   ```
   SendNotificationWorker: Sending notification to API
   SendNotificationWorker: Notification sent successfully
   ```

### 5.2 Verificar en Backend

```bash
# Ver logs de Laravel
docker-compose logs -f app | grep "Notification created"

# Debería mostrar:
# Notification created successfully: notification_id=X, device_id=Y, commerce_id=Z, authenticated=false
```

### 5.3 Verificar en Base de Datos

```bash
# En tinker:
$notification = \App\Models\Notification::latest()->first();
echo "Notification ID: " . $notification->id . PHP_EOL;
echo "Device ID: " . $notification->device_id . PHP_EOL;
echo "Commerce ID: " . $notification->commerce_id . PHP_EOL;
echo "User ID: " . ($notification->user_id ?? 'NULL') . PHP_EOL;
echo "Amount: " . $notification->amount . PHP_EOL;
echo "Source App: " . $notification->source_app . PHP_EOL;

# Verificar que:
# - commerce_id NO es NULL
# - user_id ES NULL (modo capturer)
# - device_id corresponde al dispositivo vinculado
```

## Paso 6: Casos de Error a Probar

### 6.1 Código Inválido

```bash
# Intentar validar código inexistente
curl -X GET https://api.notificaciones.space/api/devices/link-code/INVALID1

# Respuesta esperada:
# {
#   "valid": false,
#   "message": "Código no encontrado"
# }
```

### 6.2 Código Expirado

```bash
# Esperar 24 horas después de generar el código
# O modificar en BD: UPDATE device_link_codes SET expires_at = NOW() - INTERVAL 1 HOUR WHERE code = 'ABC12345';

curl -X GET https://api.notificaciones.space/api/devices/link-code/ABC12345

# Respuesta esperada:
# {
#   "valid": false,
#   "message": "Código expirado"
# }
```

### 6.3 Código Ya Usado

```bash
# Intentar vincular el mismo código dos veces
curl -X POST https://api.notificaciones.space/api/devices/link-by-code \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ABC12345",
    "device_uuid": "550e8400-e29b-41d4-a716-446655440001",
    "device_name": "Device 2"
  }'

# Respuesta esperada:
# {
#   "message": "Código ya utilizado"
# }
```

### 6.4 Dispositivo Sin Commerce Intenta Enviar Notificación

```bash
# Crear dispositivo sin commerce_id
# En tinker:
$device = \App\Models\Device::create([
    'uuid' => '550e8400-e29b-41d4-a716-446655440002',
    'name' => 'Unlinked Device',
    'platform' => 'android',
    'is_active' => true
]);

# Intentar enviar notificación
curl -X POST https://api.notificaciones.space/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "550e8400-e29b-41d4-a716-446655440002",
    "title": "Test",
    "text": "Test notification",
    "source_app": "Yape",
    "package_name": "com.yape.app"
  }'

# Respuesta esperada:
# {
#   "message": "Dispositivo no vinculado. Por favor, escanea el código QR...",
#   "error": "device_not_linked"
# }
```

## Checklist Final

### Backend
- [ ] Docker corriendo
- [ ] Migraciones ejecutadas
- [ ] Tabla `devices` tiene `user_id` nullable
- [ ] Tabla `devices` tiene `commerce_id`
- [ ] Tabla `device_link_codes` existe
- [ ] Rutas públicas configuradas
- [ ] Logs muestran "Device linked to commerce"

### Android
- [ ] App instalada
- [ ] UUID generado en Application.onCreate()
- [ ] QR scanner funciona
- [ ] Código se valida correctamente
- [ ] Dispositivo se vincula sin login
- [ ] device_id y commerce_id se guardan localmente
- [ ] Notificaciones se envían correctamente

### Base de Datos
- [ ] Dispositivo creado con commerce_id
- [ ] Dispositivo creado con user_id = NULL
- [ ] Código marcado como usado (used_at)
- [ ] Notificaciones creadas con commerce_id
- [ ] Notificaciones creadas con user_id = NULL

## Troubleshooting

### Problema: "Device UUID no encontrado"
**Solución**: Verificar que `YapeNotifierApplication.onCreate()` se ejecuta correctamente.

### Problema: "Código no válido"
**Solución**: Verificar que el código fue generado correctamente y no ha expirado.

### Problema: "Dispositivo no vinculado"
**Solución**: Verificar que el dispositivo tiene `commerce_id` en la BD.

### Problema: "Error de conexión"
**Solución**: Verificar que la URL de la API es correcta en `build.gradle.kts`.

### Problema: "Docker no está corriendo"
**Solución**: Iniciar Docker Desktop manualmente.

## Conclusión

Si todos los pasos se completan exitosamente, el sistema de vinculación QR está funcionando correctamente y los dispositivos pueden:

1. ✅ Vincularse a un commerce sin autenticación de usuario
2. ✅ Enviar notificaciones sin autenticación de usuario
3. ✅ Operar en "modo capturer" (sin cuenta de usuario)

El QR code es el mecanismo de autorización principal, no la autenticación de usuario.


