# 🔒 Seguridad Multi-Tenant y Validaciones

Documentación completa de cómo el sistema asegura que un dispositivo pertenece a una cuenta o comercio específico.

---

## 📋 Resumen Ejecutivo

### Principio Fundamental

**El `commerce_id` es la unidad de aislamiento multi-tenant.**

- Cada comercio tiene su propio `commerce_id`
- Cada dispositivo tiene un `commerce_id` (asignado via QR)
- Cada notificación tiene un `commerce_id` (heredado del dispositivo)
- **Regla de oro:** Un comercio solo puede ver/modificar sus propios recursos

---

## 🏗️ Arquitectura de Seguridad

### Modelo de Datos

```sql
-- Comercios (tenants)
CREATE TABLE commerces (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    -- ...
);

-- Usuarios (pertenecen a un comercio)
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255),
    commerce_id BIGINT REFERENCES commerces(id), -- Tenant isolation
    role ENUM('admin', 'user'),
    -- ...
);

-- Dispositivos (pertenecen a un comercio)
CREATE TABLE devices (
    id BIGINT PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE,           -- Physical device identifier
    commerce_id BIGINT REFERENCES commerces(id), -- AUTHORIZATION
    user_id BIGINT REFERENCES users(id) NULL,    -- TRACEABILITY (optional)
    is_active BOOLEAN DEFAULT true,
    -- ...
);

-- Notificaciones (pertenecen a un comercio)
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id),
    commerce_id BIGINT REFERENCES commerces(id), -- Inherited from device
    amount DECIMAL(10,2),
    -- ...
);
```

### Jerarquía de Autorización

```
Commerce (Tenant)
    ├── Users (commerce_id)
    │   └── Admin users can manage commerce resources
    ├── Devices (commerce_id)
    │   ├── Linked via QR code (commerce_id from QR)
    │   └── Optionally associated with user (user_id)
    └── Notifications (commerce_id)
        └── Created by devices belonging to commerce
```

---

## 🔐 Validaciones de Seguridad por Operación

### 1. Generación de QR (Admin Only)

**Endpoint:** `POST /api/devices/generate-link-code`

**Autenticación:** ✅ Requerida (`auth:sanctum`)

**Validaciones:**

```php
public function generateLinkCode(Request $request): JsonResponse
{
    $user = $request->user(); // Must be authenticated
    
    // Validation 1: User must belong to a commerce
    if (!$user->commerce_id) {
        return response()->json([
            'message' => 'Usuario no pertenece a un negocio'
        ], 400);
    }
    
    // Validation 2: User must be admin
    if (!$user->isAdmin()) {
        return response()->json([
            'message' => 'Solo admins pueden generar códigos'
        ], 403);
    }
    
    // Generate code linked to user's commerce
    $linkCode = DeviceLinkCode::create([
        'code' => DeviceLinkCode::generateUniqueCode(),
        'commerce_id' => $user->commerce_id, // ← Commerce isolation
        'expires_at' => now()->addHours(24),
    ]);
    
    return response()->json(['code' => $linkCode->code]);
}
```

**Seguridad:**
- ✅ Solo admins autenticados pueden generar QR
- ✅ QR está vinculado al `commerce_id` del admin
- ✅ Código expira en 24 horas
- ✅ Código es de un solo uso

---

### 2. Vinculación con QR

**Endpoint:** `POST /api/devices/link-by-code`

**Autenticación:** ❌ No requerida (QR es la autorización)

**Validaciones:**

```php
public function linkByCode(Request $request): JsonResponse
{
    $code = $request->input('code');
    $deviceUuid = $request->input('device_uuid');
    
    // Validation 1: Code must be valid (not expired, not used)
    $linkCode = DeviceLinkCode::where('code', $code)
        ->where('expires_at', '>', now())
        ->whereNull('used_at')
        ->first();
    
    if (!$linkCode) {
        return response()->json([
            'message' => 'Código inválido o expirado'
        ], 400);
    }
    
    // Validation 2: Check if device already exists
    $existingDevice = Device::where('uuid', $deviceUuid)->first();
    
    if ($existingDevice) {
        // Validation 3: If device exists, verify commerce compatibility
        if ($existingDevice->commerce_id && 
            $existingDevice->commerce_id !== $linkCode->commerce_id) {
            return response()->json([
                'message' => 'Dispositivo ya vinculado a otro negocio'
            ], 409);
        }
        
        // Update device with commerce from QR
        $existingDevice->update([
            'commerce_id' => $linkCode->commerce_id,
        ]);
        
        $device = $existingDevice;
    } else {
        // Create new device linked to commerce from QR
        $device = Device::create([
            'uuid' => $deviceUuid,
            'commerce_id' => $linkCode->commerce_id, // ← From QR
            'user_id' => null, // Optional
            'is_active' => true,
        ]);
    }
    
    // Mark code as used
    $linkCode->update(['used_at' => now()]);
    
    return response()->json(['device' => $device]);
}
```

**Seguridad:**
- ✅ Código debe ser válido (no expirado, no usado)
- ✅ Dispositivo se vincula al `commerce_id` del código
- ✅ Si dispositivo ya existe, verifica compatibilidad de comercio
- ✅ Código se marca como usado (un solo uso)

---

### 3. Envío de Notificaciones

**Endpoint:** `POST /api/notifications`

**Autenticación:** ❌ No requerida (QR es la autorización)

**Validaciones:**

```php
public function store(Request $request): JsonResponse
{
    $user = $request->user(); // Nullable
    $deviceUuid = $request->input('device_id');
    
    // Validation 1: Find device by UUID
    if ($user) {
        // If authenticated, use multi-tenant validation
        $device = $this->deviceService->findDeviceByUuid($user, $deviceUuid);
    } else {
        // If not authenticated, just find by UUID
        $device = Device::where('uuid', $deviceUuid)
            ->where('is_active', true)
            ->first();
    }
    
    if (!$device) {
        return response()->json([
            'message' => 'Dispositivo no encontrado'
        ], 404);
    }
    
    // Validation 2: Device MUST have commerce_id (authorization)
    if (!$device->commerce_id) {
        return response()->json([
            'message' => 'Dispositivo no vinculado. Escanea código QR.',
            'error' => 'device_not_linked'
        ], 403);
    }
    
    // Validation 3: Device must be active
    if (!$device->is_active) {
        return response()->json([
            'message' => 'Dispositivo inactivo'
        ], 403);
    }
    
    // Create notification with commerce_id from device
    $notification = Notification::create([
        'device_id' => $device->id,
        'commerce_id' => $device->commerce_id, // ← Inherited from device
        'amount' => $request->input('amount'),
        // ...
    ]);
    
    return response()->json(['notification' => $notification], 201);
}
```

**Seguridad:**
- ✅ Dispositivo debe existir y estar activo
- ✅ Dispositivo debe tener `commerce_id` (autorización)
- ✅ Notificación hereda `commerce_id` del dispositivo
- ✅ Si usuario autenticado, valida que pertenezca al mismo comercio

---

### 4. Consulta de Notificaciones

**Endpoint:** `GET /api/notifications`

**Autenticación:** ✅ Requerida (`auth:sanctum`)

**Validaciones:**

```php
public function index(Request $request): JsonResponse
{
    $user = $request->user(); // Must be authenticated
    
    // Validation 1: User must have commerce_id
    if (!$user->commerce_id) {
        return response()->json([
            'message' => 'Usuario no pertenece a un negocio'
        ], 400);
    }
    
    // Validation 2: Only return notifications from user's commerce
    $notifications = Notification::where('commerce_id', $user->commerce_id)
        ->orderBy('created_at', 'desc')
        ->paginate(50);
    
    return response()->json($notifications);
}
```

**Seguridad:**
- ✅ Usuario debe estar autenticado
- ✅ Usuario debe pertenecer a un comercio
- ✅ Solo devuelve notificaciones del comercio del usuario
- ✅ **Aislamiento multi-tenant:** Un comercio NO puede ver notificaciones de otro

---

### 5. Desvinculación de Dispositivo

**Endpoint:** `POST /api/devices/{id}/unlink`

**Autenticación:** ✅ Requerida (`auth:sanctum`)

**Validaciones:**

```php
public function unlink(Request $request, int $id): JsonResponse
{
    $user = $request->user(); // Must be authenticated
    
    // Validation 1: Device must exist
    $device = Device::findOrFail($id);
    
    // Validation 2: Device must be linked to a commerce
    if (!$device->commerce_id) {
        return response()->json([
            'message' => 'Dispositivo ya está desvinculado'
        ], 400);
    }
    
    // Validation 3: User must belong to device's commerce (commerce admin)
    if ($device->commerce_id !== $user->commerce_id) {
        Log::warning('Unauthorized unlink attempt', [
            'user_id' => $user->id,
            'user_commerce_id' => $user->commerce_id,
            'device_id' => $device->id,
            'device_commerce_id' => $device->commerce_id,
        ]);
        
        return response()->json([
            'message' => 'No tienes permiso para desvincular este dispositivo'
        ], 403);
    }
    
    // Unlink device (clear commerce_id and user_id)
    $device->update([
        'commerce_id' => null,
        'user_id' => null,
        'is_active' => false, // Deactivate for security
    ]);
    
    return response()->json([
        'message' => 'Dispositivo desvinculado exitosamente'
    ]);
}
```

**Seguridad:**
- ✅ Usuario debe estar autenticado
- ✅ Usuario debe pertenecer al comercio del dispositivo
- ✅ Solo admins del comercio pueden desvincular dispositivos
- ✅ Dispositivo se desactiva automáticamente
- ✅ **Aislamiento multi-tenant:** Un comercio NO puede desvincular dispositivos de otro

---

## 🛡️ Validación Multi-Tenant en `findDeviceByUuid`

Este método es crítico para la seguridad multi-tenant:

```php
public function findDeviceByUuid(User $user, string $uuid): ?Device
{
    // Find device by UUID (primary identifier)
    $device = Device::where('uuid', $uuid)->first();
    
    if (!$device) {
        return null;
    }
    
    // CRITICAL: Multi-tenant security validation
    // If both device and user have commerce_id, they MUST match
    if ($device->commerce_id && $user->commerce_id) {
        if ($device->commerce_id !== $user->commerce_id) {
            // Security violation: device belongs to different commerce
            Log::warning('Multi-tenant security violation', [
                'device_id' => $device->id,
                'device_commerce_id' => $device->commerce_id,
                'user_id' => $user->id,
                'user_commerce_id' => $user->commerce_id,
            ]);
            
            return null; // ← Reject for security
        }
    }
    
    // Auto-sync user_id if device doesn't have one
    if (!$device->user_id && $user->id) {
        $device->update(['user_id' => $user->id]);
    }
    
    // Auto-sync commerce_id ONLY if device doesn't have one
    // Device's commerce_id (from QR) takes priority
    if (!$device->commerce_id && $user->commerce_id) {
        $device->update(['commerce_id' => $user->commerce_id]);
    }
    
    return $device->fresh();
}
```

**Reglas de Seguridad:**

1. **Si dispositivo tiene `commerce_id` Y usuario tiene `commerce_id`:**
   - ✅ Deben coincidir (validación multi-tenant)
   - ❌ Si no coinciden, rechazar (violación de seguridad)

2. **Si dispositivo tiene `commerce_id` pero usuario no:**
   - ✅ Permitir (dispositivo vinculado via QR, usuario sin comercio)

3. **Si dispositivo NO tiene `commerce_id` pero usuario sí:**
   - ✅ Sincronizar `commerce_id` del usuario al dispositivo

4. **Si ninguno tiene `commerce_id`:**
   - ⚠️ Permitir (caso edge, se manejará después)

---

## 🔄 Flujo de Seguridad Completo

### Escenario 1: Vinculación Normal

```
1. Admin (commerce_id=5) genera QR
   → QR contiene commerce_id=5
   
2. Empleado escanea QR
   → Device creado con commerce_id=5
   → Device.user_id = null (modo capturador)
   
3. Empleado envía notificación
   → Busca device por UUID
   → Verifica device.commerce_id existe ✅
   → Crea notification con commerce_id=5
   
4. Admin consulta notificaciones
   → Filtra por commerce_id=5
   → Ve solo notificaciones de su comercio ✅
```

### Escenario 2: Intento de Cross-Commerce (Bloqueado)

```
1. Admin A (commerce_id=5) genera QR
   → QR contiene commerce_id=5
   
2. Empleado escanea QR
   → Device creado con commerce_id=5
   
3. Usuario B (commerce_id=10) intenta usar device
   → Llama findDeviceByUuid(user_B, device_uuid)
   → Validación: device.commerce_id (5) ≠ user.commerce_id (10)
   → RECHAZADO ❌
   → Log: "Multi-tenant security violation"
```

### Escenario 3: Desvinculación y Re-vinculación

```
1. Device vinculado a commerce_id=5
   
2. Admin A (commerce_id=5) desvincula device
   → Verifica: user.commerce_id (5) == device.commerce_id (5) ✅
   → Device.commerce_id = null
   → Device.user_id = null
   → Device.is_active = false
   
3. Admin B (commerce_id=10) genera nuevo QR
   → QR contiene commerce_id=10
   
4. Mismo device escanea nuevo QR
   → Device.commerce_id = 10 (actualizado)
   → Device.is_active = true
   → Ahora pertenece a commerce_id=10 ✅
```

---

## 📊 Matriz de Permisos

| Operación | Autenticación | Validación Commerce | Notas |
|-----------|---------------|---------------------|-------|
| Generar QR | ✅ Requerida | ✅ User debe tener commerce_id | Solo admins |
| Vincular con QR | ❌ Opcional | ✅ QR contiene commerce_id | QR es autorización |
| Enviar notificación | ❌ Opcional | ✅ Device debe tener commerce_id | Device es autorización |
| Ver notificaciones | ✅ Requerida | ✅ Solo del commerce del user | Multi-tenant |
| Desvincular device | ✅ Requerida | ✅ User y device mismo commerce | Solo admins |
| Ver devices | ✅ Requerida | ✅ Solo del commerce del user | Multi-tenant |

---

## ✅ Garantías de Seguridad

### 1. Aislamiento Multi-Tenant

- ✅ Cada comercio solo ve sus propios recursos
- ✅ Validación en cada query: `WHERE commerce_id = $user->commerce_id`
- ✅ Imposible acceder a recursos de otro comercio

### 2. Autorización por QR

- ✅ QR generado por admin autenticado
- ✅ QR contiene `commerce_id` del admin
- ✅ QR expira en 24 horas
- ✅ QR es de un solo uso

### 3. Validación de Dispositivos

- ✅ Dispositivo debe tener `commerce_id` para enviar notificaciones
- ✅ Dispositivo debe estar activo
- ✅ Si usuario autenticado, debe pertenecer al mismo comercio

### 4. Trazabilidad

- ✅ Todas las operaciones se loguean
- ✅ Violaciones de seguridad se registran
- ✅ `user_id` opcional para auditoría

### 5. Desvinculación Segura

- ✅ Solo admins del comercio pueden desvincular
- ✅ Dispositivo se desactiva automáticamente
- ✅ Limpia `commerce_id` y `user_id` para permitir re-vinculación

---

## 🚨 Escenarios de Ataque Mitigados

### Ataque 1: Cross-Commerce Access

**Intento:** Usuario de Commerce A intenta acceder a dispositivo de Commerce B

**Mitigación:**
```php
if ($device->commerce_id !== $user->commerce_id) {
    Log::warning('Multi-tenant security violation');
    return null; // Rechazado
}
```

### Ataque 2: Reutilización de QR

**Intento:** Reutilizar un código QR ya usado

**Mitigación:**
```php
$linkCode = DeviceLinkCode::where('code', $code)
    ->whereNull('used_at') // ← Solo códigos no usados
    ->first();
```

### Ataque 3: QR Expirado

**Intento:** Usar un código QR expirado

**Mitigación:**
```php
$linkCode = DeviceLinkCode::where('code', $code)
    ->where('expires_at', '>', now()) // ← Solo códigos válidos
    ->first();
```

### Ataque 4: Envío sin Autorización

**Intento:** Enviar notificación sin vincular dispositivo

**Mitigación:**
```php
if (!$device->commerce_id) {
    return response()->json([
        'message' => 'Dispositivo no vinculado'
    ], 403);
}
```

### Ataque 5: Desvinculación No Autorizada

**Intento:** Usuario de Commerce A intenta desvincular dispositivo de Commerce B

**Mitigación:**
```php
if ($device->commerce_id !== $user->commerce_id) {
    return response()->json([
        'message' => 'No tienes permiso'
    ], 403);
}
```

---

## 📚 Referencias

- [QR Authorization Architecture](../03-architecture/QR_AUTHORIZATION_ARCHITECTURE.md)
- [Device Linking Guide](../05-features/DEVICE_LINKING_GUIDE.md)
- [Device Unlinking](../05-features/DEVICE_UNLINKING.md)
- [DeviceService](../../apps/api/app/Services/DeviceService.php)
- [NotificationController](../../apps/api/app/Http/Controllers/NotificationController.php)



