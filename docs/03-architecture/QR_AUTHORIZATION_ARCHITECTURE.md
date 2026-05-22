# 🔐 Arquitectura de Autorización por QR

Documentación de la arquitectura profesional donde **el QR es la autorización suficiente** para enviar notificaciones.

---

## 📋 Resumen Ejecutivo

### Problema Original

- ❌ Dispositivo vinculado con QR pero requería login adicional
- ❌ Empleados capturadores necesitaban crear cuentas de usuario
- ❌ Flujo confuso: "¿Por qué pedir login si ya escaneé el QR?"

### Solución Implementada

- ✅ **QR es la autorización suficiente**
- ✅ Login es **opcional** (solo para trazabilidad)
- ✅ Dispositivos pueden enviar notificaciones sin cuenta de usuario
- ✅ "Modo Capturador" sin autenticación

---

## 🎯 Principio Arquitectónico

### El QR como Mecanismo de Autorización

```
Admin autenticado → Genera QR → QR contiene autorización del comercio
                                ↓
                    Empleado escanea QR → Dispositivo autorizado
                                ↓
                    Dispositivo puede enviar notificaciones ✅
                    (sin necesidad de login del empleado)
```

**Fundamento:**
- El QR es generado por un **admin autenticado**
- El QR vincula el dispositivo a un **comercio específico**
- El `commerce_id` es la **autorización**
- El `user_id` es **opcional** (solo trazabilidad)

---

## 🏗️ Arquitectura Técnica

### Modelo de Datos

```sql
CREATE TABLE devices (
    id BIGINT PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,  -- Identificador del dispositivo
    user_id BIGINT NULL,                -- OPCIONAL (trazabilidad)
    commerce_id BIGINT NULL,            -- REQUERIDO (autorización)
    name VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Reglas:**
- `uuid`: Identificador único del dispositivo físico
- `commerce_id`: **Autorización** - si existe, puede enviar notificaciones
- `user_id`: **Opcional** - para trazabilidad y auditoría

### Flujo de Autorización

```
┌─────────────────────────────────────────────────────────────┐
│ PASO 1: Admin genera QR (requiere autenticación)           │
│ POST /api/devices/generate-link-code                        │
│ Authorization: Bearer {admin_token}                         │
│ Response: { code: "ABC12345", commerce_id: 5 }             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 2: Empleado escanea QR (NO requiere autenticación)    │
│ POST /api/devices/link-by-code                              │
│ Body: { code: "ABC12345", device_uuid: "..." }             │
│ NO Authorization header                                      │
│ Response: Device vinculado con commerce_id = 5             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 3: Dispositivo envía notificaciones                    │
│ POST /api/notifications                                      │
│ Body: { device_id: "...", amount: 50, ... }                │
│ NO Authorization header                                      │
│ Backend verifica: device.commerce_id existe? ✅             │
│ Response: Notificación creada                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔒 Seguridad

### Validaciones Implementadas

#### 1. Generación de QR (Requiere Autenticación)

```php
// DeviceLinkController::generateLinkCode()
public function generateLinkCode(Request $request): JsonResponse
{
    $user = $request->user(); // Requiere autenticación
    
    if (!$user->commerce_id) {
        return response()->json(['message' => 'Usuario no pertenece a un negocio'], 400);
    }
    
    if (!$user->isAdmin()) {
        return response()->json(['message' => 'Solo admins pueden generar códigos'], 403);
    }
    
    // Genera código vinculado al commerce del admin
    $linkCode = $this->deviceLinkService->generateLinkCode($user->commerce_id);
    
    return response()->json(['code' => $linkCode->code]);
}
```

**Seguridad:**
- ✅ Solo admins autenticados pueden generar QR
- ✅ QR está vinculado al comercio del admin
- ✅ Código expira en 24 horas

#### 2. Vinculación con QR (No Requiere Autenticación)

```php
// DeviceLinkController::linkByCode()
public function linkByCode(Request $request): JsonResponse
{
    $user = $request->user(); // Nullable - autenticación opcional
    $code = $request->input('code');
    $deviceUuid = $request->input('device_uuid');
    
    // Validar código
    $linkCode = DeviceLinkCode::where('code', $code)->valid()->first();
    if (!$linkCode) {
        return response()->json(['message' => 'Código inválido'], 400);
    }
    
    // Vincular dispositivo al comercio del código
    $device = Device::updateOrCreate(
        ['uuid' => $deviceUuid],
        [
            'commerce_id' => $linkCode->commerce_id,
            'user_id' => $user?->id, // Opcional
        ]
    );
    
    return response()->json(['device' => $device]);
}
```

**Seguridad:**
- ✅ Código debe ser válido (no expirado, no usado)
- ✅ Dispositivo se vincula al comercio del código
- ✅ `user_id` es opcional (trazabilidad)

#### 3. Envío de Notificaciones (No Requiere Autenticación)

```php
// NotificationController::store()
public function store(Request $request): JsonResponse
{
    $user = $request->user(); // Nullable - autenticación opcional
    $deviceUuid = $request->input('device_id');
    
    // Buscar dispositivo por UUID
    $device = Device::where('uuid', $deviceUuid)->where('is_active', true)->first();
    
    if (!$device) {
        return response()->json(['message' => 'Dispositivo no encontrado'], 404);
    }
    
    // VALIDACIÓN CRÍTICA: Dispositivo debe tener commerce_id (autorización)
    if (!$device->commerce_id) {
        return response()->json([
            'message' => 'Dispositivo no vinculado. Escanea código QR.',
            'error' => 'device_not_linked'
        ], 403);
    }
    
    // Crear notificación
    $notification = Notification::create([
        'device_id' => $device->id,
        'commerce_id' => $device->commerce_id, // Del dispositivo
        // ... otros campos
    ]);
    
    return response()->json(['notification' => $notification], 201);
}
```

**Seguridad:**
- ✅ Dispositivo debe existir y estar activo
- ✅ Dispositivo debe tener `commerce_id` (autorización)
- ✅ Notificación se asocia al comercio del dispositivo
- ✅ Multi-tenant: cada comercio solo ve sus notificaciones

---

## 🎭 Modos de Operación

### Modo 1: Capturador Anónimo (Sin Login)

**Caso de uso:** Empleado capturador sin cuenta de usuario

```
1. Admin genera QR en dashboard
2. Empleado escanea QR en Android
   → Dispositivo vinculado (commerce_id asignado)
   → user_id = null
3. Empleado puede enviar notificaciones ✅
4. MainActivity muestra: "✅ Modo Capturador (sin usuario)"
```

**Ventajas:**
- ✅ No requiere crear cuentas para empleados
- ✅ Rápido onboarding
- ✅ Menos gestión de usuarios

**Limitaciones:**
- ❌ No hay trazabilidad de quién envía notificaciones
- ❌ No puede acceder a dashboard web

### Modo 2: Capturador Autenticado (Con Login)

**Caso de uso:** Empleado con cuenta de usuario

```
1. Empleado hace LOGIN en Android
2. Empleado escanea QR
   → Dispositivo vinculado (commerce_id asignado)
   → user_id asignado
3. Empleado puede enviar notificaciones ✅
4. MainActivity muestra: "✅ Usuario: email@example.com"
```

**Ventajas:**
- ✅ Trazabilidad completa
- ✅ Puede acceder a dashboard web
- ✅ Auditoría de acciones

**Limitaciones:**
- ⚠️ Requiere gestión de cuentas de usuario

### Modo 3: Admin (Con Login)

**Caso de uso:** Dueño del negocio

```
1. Admin hace LOGIN en dashboard web o Android
2. Admin genera QR
3. Admin puede:
   - Ver todas las notificaciones
   - Gestionar dispositivos
   - Generar más códigos QR
```

---

## 📱 Implementación en Android

### Worker de Notificaciones

```kotlin
// SendNotificationWorker.kt
override suspend fun doWork(): Result {
    // Verificar vinculación (commerce_id) - REQUERIDO
    val commerceId = preferencesManager.commerceId.first()
    if (commerceId.isNullOrBlank()) {
        Log.w(TAG, "Dispositivo no vinculado")
        return Result.failure()
    }
    
    // Autenticación (user_id) - OPCIONAL
    val authToken = preferencesManager.authToken.first()
    val isAuthenticated = !authToken.isNullOrBlank()
    
    Log.d(TAG, "Commerce: $commerceId, Authenticated: $isAuthenticated")
    
    // Enviar notificaciones (funciona con o sin autenticación)
    val result = repository.sendNotification(notificationData)
    
    return if (result.isSuccess) Result.success() else Result.retry()
}
```

### MainActivity

```kotlin
// MainActivity.kt
private fun loadUserInfo() {
    val commerceId = preferencesManager.commerceId.first()
    val authToken = preferencesManager.authToken.first()
    
    when {
        commerceId.isNullOrBlank() -> {
            // Crítico: no vinculado
            binding.tvUserInfo.text = "⚠️ Dispositivo no vinculado - Escanea QR"
            binding.tvUserInfo.setTextColor(Color.RED)
        }
        authToken.isNullOrBlank() -> {
            // Modo capturador sin usuario
            binding.tvUserInfo.text = "✅ Modo Capturador (sin usuario)"
            binding.tvUserInfo.setTextColor(Color.ORANGE)
        }
        else -> {
            // Modo completo con usuario
            binding.tvUserInfo.text = "✅ Usuario: $email"
            binding.tvUserInfo.setTextColor(Color.GREEN)
        }
    }
}
```

---

## ✅ Ventajas de esta Arquitectura

### 1. Simplicidad de Onboarding

```
ANTES:
Admin → Crea cuenta para empleado → Empleado hace login → Escanea QR → Funciona

AHORA:
Admin → Genera QR → Empleado escanea QR → Funciona ✅
```

### 2. Flexibilidad

- ✅ Soporta modo anónimo (sin usuarios)
- ✅ Soporta modo autenticado (con usuarios)
- ✅ Migración gradual (empezar anónimo, luego agregar usuarios)

### 3. Seguridad Multi-Tenant

- ✅ QR generado por admin autenticado
- ✅ Dispositivo vinculado a comercio específico
- ✅ Notificaciones aisladas por comercio
- ✅ No hay cross-contamination entre comercios

### 4. Escalabilidad

- ✅ Menos cuentas de usuario = menos gestión
- ✅ Menos autenticaciones = menos carga en servidor
- ✅ Más simple = menos bugs

---

## 🔄 Migración desde Arquitectura Anterior

### Cambios en Backend

1. **NotificationController:**
   - ✅ Autenticación opcional (`$user = $request->user()`)
   - ✅ Validación por `commerce_id` en lugar de `user_id`
   - ✅ Ruta movida fuera de `auth:sanctum` middleware

2. **DeviceLinkService:**
   - ✅ Permite vincular sin autenticación
   - ✅ `user_id` es nullable

3. **Routes:**
   - ✅ `POST /notifications` ahora es público

### Cambios en Android

1. **SendNotificationWorker:**
   - ✅ Verifica `commerce_id` en lugar de `authToken`
   - ✅ Funciona sin autenticación

2. **MainActivity:**
   - ✅ Muestra 3 estados: no vinculado, capturador, autenticado
   - ✅ No pide login si ya está vinculado

### Compatibilidad

- ✅ **Backward compatible:** Dispositivos con `user_id` siguen funcionando
- ✅ **Forward compatible:** Nuevos dispositivos pueden ser anónimos
- ✅ **Migración gradual:** No requiere cambios en dispositivos existentes

---

## 📚 Referencias

- [Device Linking Guide](../05-features/DEVICE_LINKING_GUIDE.md)
- [Device Unlinking](../05-features/DEVICE_UNLINKING.md)
- [NotificationController](../../apps/api/app/Http/Controllers/NotificationController.php)
- [SendNotificationWorker](../../apps/android-client/app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt)



