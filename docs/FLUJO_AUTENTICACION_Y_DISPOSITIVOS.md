# Flujo de Autenticación e Identificación de Dispositivos

Este documento explica cómo funciona el sistema de autenticación y cómo la app Android identifica con qué cliente/dispositivo está trabajando.

## 🔐 1. ¿Por qué se necesita el inicio de sesión?

### Razones de Seguridad y Funcionalidad

El inicio de sesión es **NECESARIO** por las siguientes razones:

#### 1.1. Autenticación de Usuario (Laravel Sanctum)
- **Todas las rutas de notificaciones están protegidas** con el middleware `auth:sanctum`
- El token de autenticación identifica **qué usuario** está enviando las notificaciones
- Sin autenticación, la API rechazaría todas las peticiones con error 401 (Unauthorized)

**Rutas protegidas:**
```php
// routes/api.php
Route::middleware('auth:sanctum')->group(function () {
    Route::post('/notifications', [NotificationController::class, 'store']); // ← Requiere token
    Route::get('/notifications', [NotificationController::class, 'index']);
    Route::apiResource('devices', DeviceController::class);
    // ... más rutas
});
```

#### 1.2. Asociación de Notificaciones con Usuario
- Cada notificación se guarda con un `user_id` en la base de datos
- Esto permite que múltiples usuarios tengan sus propios dispositivos y notificaciones
- El sistema puede filtrar y mostrar solo las notificaciones del usuario autenticado

**Ejemplo en NotificationService:**
```php
// app/Services/NotificationService.php
$notification = Notification::create([
    'user_id' => $device->user_id, // ← Se obtiene del dispositivo, que pertenece al usuario
    'device_id' => $device->id,
    // ...
]);
```

#### 1.3. Registro Automático de Dispositivo
- Al hacer login, la app **automáticamente registra el dispositivo** en el backend
- Esto crea la relación entre el usuario y el dispositivo físico
- El dispositivo queda asociado al usuario que inició sesión

**Flujo en LoginViewModel:**
```kotlin
// apps/android-client/.../LoginViewModel.kt
fun login(email: String, password: String) {
    // 1. Login y obtener token
    val response = apiService.login(request)
    preferencesManager.saveAuthToken(authResponse.token)
    
    // 2. Registrar dispositivo automáticamente
    registerDevice() // ← Crea el dispositivo en el backend
}
```

---

## 📱 2. ¿Cómo sabe la app Android con qué cliente/dispositivo está trabajando?

### Sistema de Identificación por UUID/ID

La app Android identifica el dispositivo usando un **sistema de dos niveles**:

#### 2.1. Identificación del Dispositivo

**Paso 1: Generación/Obtención del UUID**
- Al iniciar sesión, la app genera o recupera un **UUID único** del dispositivo
- Este UUID se guarda localmente en `PreferencesManager` (DataStore encriptado)
- Si no existe, se genera uno nuevo: `UUID.randomUUID().toString()`

**Paso 2: Registro en el Backend**
- Al hacer login, la app envía el UUID al backend para crear/actualizar el dispositivo
- El backend crea un registro en la tabla `devices` con:
  - `user_id`: ID del usuario autenticado
  - `uuid`: UUID único del dispositivo
  - `name`: Nombre del dispositivo (ej: "Samsung Galaxy S21")
  - `platform`: "android"
  - `is_active`: true/false

**Código en LoginViewModel:**
```kotlin
private suspend fun registerDevice() {
    val deviceUuid = preferencesManager.deviceUuid.first()
        ?: kotlinx.coroutines.runBlocking {
            val uuid = java.util.UUID.randomUUID().toString()
            preferencesManager.saveDeviceUuid(uuid)
            uuid
        }

    val deviceName = android.os.Build.MODEL ?: "Android Device"
    val createDeviceRequest = CreateDeviceRequest(
        uuid = deviceUuid,
        name = deviceName,
        platform = "android"
    )

    val deviceResponse = apiService.createDevice(createDeviceRequest)
    if (deviceResponse.isSuccessful) {
        val device = deviceResponse.body()?.get("device") as? Device
        device?.id?.let { deviceId ->
            preferencesManager.saveDeviceId(deviceId.toString()) // ← Guarda el ID del backend
        }
    }
}
```

#### 2.2. Envío de Notificaciones

Cuando la app detecta una notificación de pago, envía los datos junto con el identificador del dispositivo:

**Código en NotificationRepository:**
```kotlin
// apps/android-client/.../NotificationRepository.kt
suspend fun sendNotification(notificationData: NotificationData): Boolean {
    // 1. Obtener device ID (preferido) o UUID (fallback)
    val deviceId = preferencesManager.deviceId.first()
        ?: preferencesManager.deviceUuid.first()
        ?: run {
            val uuid = UUID.randomUUID().toString()
            runBlocking { preferencesManager.saveDeviceUuid(uuid) }
            uuid
        }

    // 2. Obtener token de autenticación
    val token = preferencesManager.authToken.first()
    if (token == null) {
        return false // ← Sin token, no puede enviar
    }

    // 3. Enviar notificación con device_id
    val notificationWithDevice = notificationData.copy(deviceId = deviceId.toString())
    val response = apiService.createNotification(notificationWithDevice)
    // ...
}
```

#### 2.3. Validación en el Backend

El backend valida que:
1. El usuario esté autenticado (token válido)
2. El dispositivo exista y pertenezca al usuario autenticado
3. El dispositivo esté activo

**Código en NotificationController:**
```php
// apps/api/app/Http/Controllers/NotificationController.php
public function store(CreateNotificationRequest $request): JsonResponse
{
    $user = $request->user(); // ← Usuario autenticado por token
    $deviceUuid = $request->input('device_id');

    // Buscar dispositivo por UUID y usuario
    $device = $this->deviceService->findDeviceByUuid($user, $deviceUuid);

    if (!$device) {
        return response()->json(['message' => 'Device not found'], 404);
    }

    if (!$device->is_active) {
        return response()->json(['message' => 'Device is not active'], 403);
    }

    // Crear notificación asociada al dispositivo y usuario
    $notification = $this->notificationService->createNotification(
        $request->validated(),
        $device
    );
    // ...
}
```

### Resumen del Flujo de Identificación

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Usuario inicia sesión en la app Android                  │
│    → Obtiene token de autenticación                         │
│    → Genera/recupera UUID del dispositivo                   │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. App registra dispositivo en backend                       │
│    POST /api/devices                                         │
│    Headers: Authorization: Bearer {token}                    │
│    Body: { uuid, name, platform }                           │
│    → Backend crea registro en tabla 'devices'               │
│    → Asocia dispositivo al user_id del token                │
│    → Retorna device.id                                      │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. App guarda device.id localmente                          │
│    → preferencesManager.saveDeviceId(deviceId)              │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. App detecta notificación de pago                         │
│    → Extrae datos (monto, pagador, etc.)                    │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. App envía notificación al backend                         │
│    POST /api/notifications                                   │
│    Headers: Authorization: Bearer {token}                   │
│    Body: {                                                  │
│      device_id: {uuid o id},  ← Identifica el dispositivo  │
│      source_app: "yape",                                     │
│      amount: 150.00,                                        │
│      ...                                                    │
│    }                                                        │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Backend valida y procesa                                  │
│    → Verifica token → Obtiene usuario                       │
│    → Busca dispositivo por UUID + user_id                   │
│    → Verifica que dispositivo esté activo                   │
│    → Crea notificación con user_id y device_id              │
└─────────────────────────────────────────────────────────────┘
```

### Múltiples Dispositivos

**Escenario:** Un usuario tiene 3 dispositivos Android configurados

1. **Cada dispositivo tiene su propio UUID único**
   - Dispositivo 1: UUID `550e8400-e29b-41d4-a716-446655440000`
   - Dispositivo 2: UUID `660e8400-e29b-41d4-a716-446655440001`
   - Dispositivo 3: UUID `770e8400-e29b-41d4-a716-446655440002`

2. **Todos están asociados al mismo usuario**
   - En la tabla `devices`: todos tienen el mismo `user_id`
   - Pero cada uno tiene un `uuid` diferente

3. **Cuando un dispositivo envía una notificación:**
   - Incluye su `device_id` (UUID) en la petición
   - El backend identifica qué dispositivo específico la envió
   - La notificación se guarda con ese `device_id`
   - El dashboard puede filtrar por dispositivo

4. **Ventajas:**
   - Puedes ver qué dispositivo recibió cada pago
   - Puedes activar/desactivar dispositivos individualmente
   - Puedes ver estadísticas por dispositivo

---

## 🌐 3. ¿A qué URL o API está apuntando en modo desarrollo?

### Configuración Actual

**Archivo:** `apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/RetrofitClient.kt`

```kotlin
object RetrofitClient {
    // TODO: Cambiar por la URL real de tu API
    // Para desarrollo local con emulador: "http://10.0.2.2:8000/"
    // Para dispositivo físico: "http://TU_IP_LOCAL:8000/"
    // Para producción: "https://tu-api.railway.app/"
    private const val BASE_URL = "http://10.0.2.2:8000/"  // ← Configuración actual
    // ...
}
```

### Explicación de las URLs

#### 3.1. `http://10.0.2.2:8000/` (Emulador Android)
- **10.0.2.2** es una IP especial que el emulador de Android Studio usa para referirse al `localhost` de la máquina host
- Equivale a `http://localhost:8000` o `http://127.0.0.1:8000` en tu computadora
- **Solo funciona cuando usas el emulador de Android Studio**

#### 3.2. `http://TU_IP_LOCAL:8000/` (Dispositivo Físico)
- Necesitas usar la **IP local de tu computadora** en la red WiFi
- Ejemplo: `http://192.168.1.100:8000/`
- **Requisitos:**
  - El teléfono y la computadora deben estar en la **misma red WiFi**
  - El backend debe estar corriendo con `--host=0.0.0.0` para aceptar conexiones externas

#### 3.3. `https://tu-api.railway.app/` (Producción)
- URL del backend desplegado en producción (Railway, Heroku, etc.)

### Cómo Encontrar tu IP Local

**Windows:**
```bash
ipconfig
# Busca "Dirección IPv4" en la sección de tu adaptador WiFi/Ethernet
# Ejemplo: 192.168.1.100
```

**Mac/Linux:**
```bash
ifconfig
# O
ip addr
# Busca tu IP en la red local (generalmente empieza con 192.168.x.x o 10.x.x.x)
```

### Configuración para Desarrollo

**Opción 1: Usando Emulador (Recomendado para desarrollo inicial)**
```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"
```

**Backend debe correr:**
```bash
cd yape-notifier/apps/api
php artisan serve --host=127.0.0.1 --port=8000
# O simplemente:
php artisan serve
```

**Opción 2: Usando Dispositivo Físico**
```kotlin
private const val BASE_URL = "http://192.168.1.100:8000/"  // ← Cambiar por tu IP
```

**Backend debe correr:**
```bash
cd yape-notifier/apps/api
php artisan serve --host=0.0.0.0 --port=8000
# El --host=0.0.0.0 permite conexiones desde otros dispositivos
```

---

## 🧪 4. ¿Cómo debería funcionar para probar?

### Flujo Completo de Pruebas

#### Paso 1: Configurar el Backend

```bash
# 1. Ir al directorio del API
cd yape-notifier/apps/api

# 2. Instalar dependencias (si no lo has hecho)
composer install

# 3. Configurar base de datos
cp .env.example .env
php artisan key:generate
php artisan migrate

# 4. Iniciar servidor
# Para emulador:
php artisan serve --host=127.0.0.1 --port=8000

# Para dispositivo físico:
php artisan serve --host=0.0.0.0 --port=8000
```

#### Paso 2: Configurar la App Android

**2.1. Configurar URL según tu caso:**

**Si usas emulador:**
```kotlin
// RetrofitClient.kt
private const val BASE_URL = "http://10.0.2.2:8000/"
```

**Si usas dispositivo físico:**
```kotlin
// RetrofitClient.kt
private const val BASE_URL = "http://192.168.1.XXX:8000/"  // ← Tu IP local
```

**2.2. Verificar que el teléfono y la PC estén en la misma red WiFi**

#### Paso 3: Probar el Flujo Completo

**3.1. Instalar y abrir la app en el dispositivo/emulador**

**3.2. Registrar un usuario:**
- Toca "Registrarse" en la pantalla de login
- Completa el formulario:
  - Nombre: "Usuario Prueba"
  - Email: "test@example.com"
  - Contraseña: "password123"
  - Confirmar contraseña: "password123"
- Toca "Registrarse"
- **La app automáticamente:**
  - Obtiene el token de autenticación
  - Genera un UUID para el dispositivo
  - Registra el dispositivo en el backend
  - Guarda el token y device_id localmente

**3.3. Verificar en el backend que el dispositivo se creó:**
```bash
# En otra terminal, acceder a tinker
php artisan tinker

# Ver dispositivos
\App\Models\Device::all();

# Ver usuarios
\App\Models\User::all();
```

**3.4. Activar el servicio de notificaciones:**
- En la app, toca "Activar Notificaciones"
- Ve a Configuración del sistema → Accesibilidad → Servicios instalados
- Activa "Yape Notifier"
- Regresa a la app

**3.5. Probar recepción de notificación:**
- Solicita un pago de prueba desde Yape/Plin
- Cuando llegue la notificación:
  - La app la detectará automáticamente
  - La procesará y extraerá los datos
  - La enviará al backend con el `device_id` y `token`

**3.6. Verificar en el backend:**
```bash
php artisan tinker

# Ver notificaciones recibidas
\App\Models\Notification::latest()->take(5)->get();

# Ver notificaciones de un dispositivo específico
$device = \App\Models\Device::first();
$device->notifications;
```

### Checklist de Verificación

- [ ] Backend corriendo y accesible
- [ ] URL configurada correctamente en `RetrofitClient.kt`
- [ ] Usuario registrado/iniciado sesión
- [ ] Token de autenticación guardado en la app
- [ ] Dispositivo registrado en el backend (verificar con tinker)
- [ ] Servicio de notificaciones activado
- [ ] Notificación de pago recibida y procesada
- [ ] Notificación guardada en la base de datos

### Debugging

**Ver logs de la app Android:**
```bash
# En Android Studio, abre Logcat
# Filtra por: "PaymentNotificationService" o "NotificationRepository"
```

**Ver logs del backend:**
```bash
# Los logs de Laravel están en storage/logs/laravel.log
tail -f storage/logs/laravel.log
```

**Probar la API manualmente:**
```bash
# 1. Registrar usuario
curl -X POST http://localhost:8000/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123",
    "password_confirmation": "password123"
  }'

# 2. Login (guarda el token de la respuesta)
curl -X POST http://localhost:8000/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# 3. Ver dispositivos (usa el token del paso 2)
curl -X GET http://localhost:8000/api/devices \
  -H "Authorization: Bearer TU_TOKEN_AQUI"

# 4. Ver notificaciones
curl -X GET http://localhost:8000/api/notifications \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

---

## 📋 Resumen

### ¿Es necesario el login?
**SÍ**, porque:
- Las rutas de API están protegidas con autenticación
- Cada notificación debe asociarse a un usuario
- El dispositivo se registra automáticamente al hacer login

### ¿Cómo identifica el dispositivo?
- Cada dispositivo tiene un **UUID único** generado localmente
- Al hacer login, el dispositivo se registra en el backend con ese UUID
- El backend asocia el dispositivo al usuario autenticado
- Al enviar notificaciones, la app incluye el `device_id` (UUID)
- El backend valida que el dispositivo pertenezca al usuario del token

### ¿A qué URL apunta en desarrollo?
- **Emulador:** `http://10.0.2.2:8000/`
- **Dispositivo físico:** `http://TU_IP_LOCAL:8000/` (ej: `http://192.168.1.100:8000/`)

### ¿Cómo probar?
1. Configurar backend y URL en la app
2. Registrar/iniciar sesión (esto registra el dispositivo automáticamente)
3. Activar servicio de notificaciones
4. Recibir un pago de prueba
5. Verificar en la base de datos que se guardó correctamente

