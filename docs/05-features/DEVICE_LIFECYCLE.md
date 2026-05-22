# 🔄 Ciclo de Vida de Dispositivos

> **Referencias relacionadas:**
> - [DEVICE_UNLINKING.md](DEVICE_UNLINKING.md) - Desvinculación de dispositivos
> - [CLEAN_DEVICES.md](../06-operations/CLEAN_DEVICES.md) - Limpieza completa de dispositivos (operación avanzada)
> - [DEVICE_LINKING_GUIDE.md](DEVICE_LINKING_GUIDE.md) - Guía de vinculación de dispositivos

Documentación completa del ciclo de vida de un dispositivo, desde su vinculación hasta su desvinculación y re-vinculación.

---

## 📋 Resumen Ejecutivo

### Principios Fundamentales

1. **Los dispositivos NUNCA se eliminan de la base de datos**
   - Se preserva el historial completo
   - Las notificaciones mantienen su relación con el dispositivo
   - Permite auditoría y trazabilidad

2. **Desvinculación = "Reset" no "Delete"**
   - Se limpian `commerce_id` y `user_id`
   - Se desactiva el dispositivo (`is_active = false`)
   - Se preserva el `uuid` y el registro

3. **Re-vinculación = "Actualización" no "Creación"**
   - Se busca el dispositivo por `uuid`
   - Si existe, se actualiza con el nuevo `commerce_id`
   - Si no existe, se crea nuevo

---

## 🔄 Estados del Dispositivo

```
┌─────────────────────────────────────────────────────────────┐
│ Estado 1: NO EXISTE                                         │
│ - Dispositivo físico nunca vinculado                        │
│ - No hay registro en BD                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    [Escanea QR por primera vez]
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Estado 2: VINCULADO (Activo)                               │
│ - uuid: "98ba8129-..."                                      │
│ - commerce_id: 5                                            │
│ - user_id: null (modo capturador) o 1 (modo autenticado)   │
│ - is_active: true                                           │
│ - Puede enviar notificaciones ✅                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    [Admin desvincula dispositivo]
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Estado 3: DESVINCULADO (Inactivo)                          │
│ - uuid: "98ba8129-..." (preservado)                         │
│ - commerce_id: null                                         │
│ - user_id: null                                             │
│ - is_active: false                                          │
│ - NO puede enviar notificaciones ❌                         │
│ - Historial de notificaciones preservado ✅                │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    [Escanea nuevo QR]
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Estado 4: RE-VINCULADO (Activo)                            │
│ - uuid: "98ba8129-..." (mismo dispositivo físico)           │
│ - commerce_id: 10 (nuevo comercio)                          │
│ - user_id: null o nuevo usuario                             │
│ - is_active: true                                           │
│ - Puede enviar notificaciones ✅                            │
│ - Historial anterior preservado (commerce_id=5) ✅         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Preservación de Datos

### Tabla `devices`

```sql
-- Ejemplo de ciclo de vida completo

-- Estado 1: Vinculación inicial
INSERT INTO devices (uuid, commerce_id, user_id, is_active, created_at)
VALUES ('98ba8129-...', 5, NULL, true, '2026-01-01 10:00:00');

-- Estado 2: Desvinculación
UPDATE devices 
SET commerce_id = NULL, user_id = NULL, is_active = false
WHERE uuid = '98ba8129-...';
-- ❌ NO se hace DELETE
-- ✅ El registro se preserva

-- Estado 3: Re-vinculación
UPDATE devices 
SET commerce_id = 10, is_active = true
WHERE uuid = '98ba8129-...';
-- ✅ Se actualiza el mismo registro
-- ✅ El ID del dispositivo NO cambia
```

### Tabla `notifications`

```sql
-- Las notificaciones mantienen su relación con el dispositivo
-- Incluso después de desvinculación

SELECT * FROM notifications WHERE device_id = 123;

-- Resultado:
-- id | device_id | commerce_id | amount | created_at
-- 1  | 123       | 5           | 50.00  | 2026-01-01 11:00:00  ← Comercio anterior
-- 2  | 123       | 5           | 75.00  | 2026-01-02 12:00:00  ← Comercio anterior
-- [dispositivo desvinculado y re-vinculado a commerce_id=10]
-- 3  | 123       | 10          | 100.00 | 2026-01-05 14:00:00  ← Nuevo comercio

-- ✅ Historial completo preservado
-- ✅ Cada notificación mantiene su commerce_id original
-- ✅ Auditoría completa del dispositivo
```

---

## 🔐 Lógica de Re-vinculación

### Código en `DeviceLinkService::linkDevice()`

```php
public function linkDevice(string $code, string $deviceUuid, ?User $user, ?string $deviceName): array
{
    // 1. Validar código QR
    $linkCode = DeviceLinkCode::where('code', $code)
        ->where('expires_at', '>', now())
        ->whereNull('used_at')
        ->first();
    
    if (!$linkCode) {
        return ['success' => false, 'message' => 'Código inválido'];
    }
    
    // 2. Buscar dispositivo por UUID
    $device = Device::where('uuid', $deviceUuid)->first();
    
    if (!$device) {
        // CASO A: Dispositivo NO existe → CREAR
        $device = Device::create([
            'uuid' => $deviceUuid,
            'commerce_id' => $linkCode->commerce_id,
            'user_id' => $user?->id,
            'is_active' => true,
        ]);
        
        Log::info('Device created', ['device_id' => $device->id]);
    } else {
        // CASO B: Dispositivo existe → ACTUALIZAR
        
        // Validación: Si device tiene commerce_id diferente, rechazar
        if ($device->commerce_id && $device->commerce_id !== $linkCode->commerce_id) {
            return [
                'success' => false,
                'message' => 'El dispositivo ya pertenece a otro negocio'
            ];
        }
        
        // Actualizar con nuevo commerce_id
        $device->update([
            'commerce_id' => $linkCode->commerce_id,
            'user_id' => $user?->id ?? $device->user_id, // Preservar user_id si no hay nuevo
            'is_active' => true, // ← Reactivar dispositivo
        ]);
        
        Log::info('Device re-linked', [
            'device_id' => $device->id,
            'new_commerce_id' => $linkCode->commerce_id
        ]);
    }
    
    // 3. Marcar código como usado
    $linkCode->markAsUsed();
    
    return ['success' => true, 'device' => $device];
}
```

### Flujo de Re-vinculación

```
┌─────────────────────────────────────────────────────────────┐
│ PASO 1: Dispositivo desvinculado                            │
│ Device: { uuid: "ABC", commerce_id: null, is_active: false }│
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 2: Admin de Commerce B genera QR                       │
│ QR: { code: "XYZ123", commerce_id: 10 }                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 3: Dispositivo escanea QR                              │
│ POST /api/devices/link-by-code                              │
│ Body: { code: "XYZ123", device_uuid: "ABC" }               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 4: Backend busca device por UUID                       │
│ Device::where('uuid', 'ABC')->first()                       │
│ ✅ Encontrado (aunque desvinculado)                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 5: Backend actualiza device                            │
│ $device->update([                                           │
│   'commerce_id' => 10,  // ← Nuevo comercio                │
│   'is_active' => true,  // ← Reactivado                    │
│ ])                                                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ RESULTADO: Dispositivo re-vinculado                         │
│ Device: { uuid: "ABC", commerce_id: 10, is_active: true }  │
│ - Mismo ID de dispositivo                                   │
│ - Historial de notificaciones preservado                    │
│ - Puede enviar notificaciones al nuevo comercio ✅          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Historial de Notificaciones

### ¿Qué pasa con las notificaciones antiguas?

**Respuesta: Se preservan TODAS las notificaciones.**

```sql
-- Dispositivo 123 vinculado a Commerce 5
-- Envía notificaciones...

INSERT INTO notifications (device_id, commerce_id, amount)
VALUES (123, 5, 50.00);

INSERT INTO notifications (device_id, commerce_id, amount)
VALUES (123, 5, 75.00);

-- Admin de Commerce 5 desvincula dispositivo
UPDATE devices SET commerce_id = NULL WHERE id = 123;

-- ❌ NO se eliminan las notificaciones
-- ✅ Las notificaciones se mantienen con commerce_id = 5

-- Dispositivo 123 se re-vincula a Commerce 10
UPDATE devices SET commerce_id = 10 WHERE id = 123;

-- Nuevas notificaciones se crean con commerce_id = 10
INSERT INTO notifications (device_id, commerce_id, amount)
VALUES (123, 10, 100.00);

-- Consulta de notificaciones por comercio:
SELECT * FROM notifications WHERE commerce_id = 5;
-- ✅ Devuelve las 2 notificaciones antiguas

SELECT * FROM notifications WHERE commerce_id = 10;
-- ✅ Devuelve solo la notificación nueva

-- Consulta de notificaciones por dispositivo:
SELECT * FROM notifications WHERE device_id = 123;
-- ✅ Devuelve TODAS las 3 notificaciones (historial completo)
```

### Aislamiento Multi-Tenant

```
Commerce 5:
  - Ve notificaciones con commerce_id = 5
  - NO ve notificaciones con commerce_id = 10
  - Aunque sean del mismo dispositivo físico

Commerce 10:
  - Ve notificaciones con commerce_id = 10
  - NO ve notificaciones con commerce_id = 5
  - Aunque sean del mismo dispositivo físico

✅ Aislamiento perfecto entre comercios
✅ Cada comercio solo ve sus propias notificaciones
```

---

## 🔒 Seguridad en Re-vinculación

### Validación 1: Dispositivo ya vinculado a otro comercio

```php
// Si dispositivo está ACTIVAMENTE vinculado a otro comercio
if ($device->commerce_id && $device->commerce_id !== $linkCode->commerce_id) {
    return [
        'success' => false,
        'message' => 'El dispositivo ya pertenece a otro negocio'
    ];
}
```

**Escenario bloqueado:**
```
Device: { uuid: "ABC", commerce_id: 5, is_active: true }
QR: { commerce_id: 10 }

❌ RECHAZADO: Dispositivo activo en otro comercio
💡 Solución: Admin de Commerce 5 debe desvincular primero
```

### Validación 2: Solo admin del comercio puede desvincular

```php
public function unlink(Request $request, int $id): JsonResponse
{
    $user = $request->user();
    $device = Device::findOrFail($id);
    
    // Validar que user pertenece al comercio del dispositivo
    if ($device->commerce_id !== $user->commerce_id) {
        return response()->json([
            'message' => 'No tienes permiso para desvincular este dispositivo'
        ], 403);
    }
    
    // OK - desvincular
    $device->update([
        'commerce_id' => null,
        'user_id' => null,
        'is_active' => false,
    ]);
}
```

**Escenario bloqueado:**
```
User A (commerce_id: 5) intenta desvincular Device (commerce_id: 10)

❌ RECHAZADO: User no pertenece al comercio del dispositivo
✅ Solo admins de Commerce 10 pueden desvincular
```

---

## 📈 Casos de Uso Reales

### Caso 1: Empleado cambia de negocio

```
1. Empleado trabaja en Negocio A
   - Device vinculado a commerce_id = 5
   - Envía 100 notificaciones

2. Empleado renuncia y se va a Negocio B
   - Admin de Negocio A desvincula dispositivo
   - Device: { commerce_id: null, is_active: false }

3. Empleado empieza en Negocio B
   - Admin de Negocio B genera QR
   - Empleado escanea QR
   - Device: { commerce_id: 10, is_active: true }

4. Resultado:
   - Negocio A mantiene historial de 100 notificaciones ✅
   - Negocio B empieza con historial limpio ✅
   - Dispositivo funciona normalmente en Negocio B ✅
```

### Caso 2: Dispositivo robado/perdido

```
1. Dispositivo vinculado a Negocio A
   - Device: { commerce_id: 5, is_active: true }

2. Dispositivo se pierde/roba
   - Admin desvincula dispositivo inmediatamente
   - Device: { commerce_id: null, is_active: false }

3. Dispositivo NO puede enviar notificaciones
   - Backend valida: device.commerce_id existe? ❌
   - Rechaza todas las notificaciones ✅

4. Si se recupera el dispositivo:
   - Admin genera nuevo QR
   - Escanea QR
   - Device: { commerce_id: 5, is_active: true }
   - Funciona normalmente ✅
```

### Caso 3: Dispositivo de prueba compartido

```
1. Dispositivo de prueba usado por Commerce A
   - Device: { commerce_id: 5, is_active: true }
   - Envía notificaciones de prueba

2. Termina periodo de prueba
   - Admin A desvincula dispositivo
   - Device: { commerce_id: null, is_active: false }

3. Dispositivo se usa para prueba en Commerce B
   - Admin B genera QR
   - Escanea QR
   - Device: { commerce_id: 10, is_active: true }

4. Resultado:
   - Mismo dispositivo físico
   - Historial de Commerce A preservado ✅
   - Historial de Commerce B separado ✅
   - Aislamiento multi-tenant perfecto ✅
```

---

## 🔍 Consultas de Auditoría

### Ver historial completo de un dispositivo

```sql
-- Dispositivo
SELECT * FROM devices WHERE uuid = '98ba8129-...';

-- Todas las notificaciones del dispositivo (todos los comercios)
SELECT 
    n.id,
    n.commerce_id,
    c.name as commerce_name,
    n.amount,
    n.created_at
FROM notifications n
LEFT JOIN commerces c ON n.commerce_id = c.id
WHERE n.device_id = 123
ORDER BY n.created_at DESC;

-- Resultado:
-- id | commerce_id | commerce_name | amount | created_at
-- 5  | 10          | Negocio B     | 100.00 | 2026-01-05 14:00:00
-- 4  | 10          | Negocio B     | 80.00  | 2026-01-04 13:00:00
-- 3  | 5           | Negocio A     | 75.00  | 2026-01-02 12:00:00
-- 2  | 5           | Negocio A     | 50.00  | 2026-01-01 11:00:00
-- 1  | 5           | Negocio A     | 25.00  | 2026-01-01 10:00:00

-- ✅ Historial completo visible
-- ✅ Se puede ver el cambio de comercio
```

### Ver dispositivos desvinculados

```sql
SELECT 
    id,
    uuid,
    name,
    is_active,
    updated_at as unlinked_at
FROM devices
WHERE commerce_id IS NULL
AND is_active = false
ORDER BY updated_at DESC;

-- Resultado: Lista de dispositivos desvinculados
-- Útil para auditoría y re-vinculación
```

---

## ✅ Resumen de Garantías

### 1. Preservación de Datos

- ✅ Dispositivos NUNCA se eliminan
- ✅ Notificaciones NUNCA se eliminan
- ✅ Historial completo preservado
- ✅ Auditoría completa disponible

### 2. Aislamiento Multi-Tenant

- ✅ Cada comercio solo ve sus notificaciones
- ✅ Notificaciones mantienen su `commerce_id` original
- ✅ Re-vinculación no afecta historial anterior

### 3. Seguridad

- ✅ Solo admins del comercio pueden desvincular
- ✅ Dispositivo desvinculado no puede enviar notificaciones
- ✅ Re-vinculación valida conflictos de comercio

### 4. Flexibilidad

- ✅ Dispositivos pueden cambiar de comercio
- ✅ Dispositivos pueden re-vincularse múltiples veces
- ✅ Historial completo siempre disponible

---

## 📚 Referencias

- [QR Authorization Architecture](../03-architecture/QR_AUTHORIZATION_ARCHITECTURE.md)
- [Multi-Tenant Security](../04-security/MULTI_TENANT_SECURITY.md)
- [Device Unlinking](DEVICE_UNLINKING.md)
- [DeviceLinkService](../../apps/api/app/Services/DeviceLinkService.php)
- [DeviceService](../../apps/api/app/Services/DeviceService.php)



