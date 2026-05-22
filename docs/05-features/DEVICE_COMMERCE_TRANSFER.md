# 🔄 Transferencia de Dispositivos entre Comercios

Documentación del escenario completo: Dispositivo va de Commerce A → Commerce B → regresa a Commerce A.

---

## 📋 Escenario Completo

### El Caso de Uso

```
Dispositivo UUID: "98ba8129-e372-4f32-a3ae-69bc78f7c95f"

Tiempo 0: Vinculado a Commerce A (id=5)
Tiempo 1: Desvinculado de Commerce A
Tiempo 2: Vinculado a Commerce B (id=10)
Tiempo 3: Desvinculado de Commerce B
Tiempo 4: Re-vinculado a Commerce A (id=5) ← ¿Qué pasa aquí?
```

---

## 🔄 Flujo Detallado Paso a Paso

### Paso 1: Vinculación Inicial a Commerce A

```sql
-- Admin de Commerce A genera QR
INSERT INTO device_link_codes (code, commerce_id, expires_at)
VALUES ('ABC123', 5, '2026-01-02 10:00:00');

-- Empleado escanea QR
-- Backend crea dispositivo
INSERT INTO devices (uuid, commerce_id, is_active, created_at)
VALUES ('98ba8129-...', 5, true, '2026-01-01 10:00:00');

-- Dispositivo envía notificaciones
INSERT INTO notifications (device_id, commerce_id, amount, created_at)
VALUES 
  (1, 5, 50.00, '2026-01-01 11:00:00'),
  (1, 5, 75.00, '2026-01-01 12:00:00'),
  (1, 5, 100.00, '2026-01-01 13:00:00');

-- Estado actual:
-- Device: { id: 1, uuid: "98ba8129-...", commerce_id: 5, is_active: true }
-- Notifications: 3 registros con commerce_id=5
```

### Paso 2: Desvinculación de Commerce A

```sql
-- Admin de Commerce A desvincula dispositivo
UPDATE devices 
SET commerce_id = NULL, user_id = NULL, is_active = false
WHERE id = 1;

-- Estado actual:
-- Device: { id: 1, uuid: "98ba8129-...", commerce_id: null, is_active: false }
-- Notifications: 3 registros con commerce_id=5 (PRESERVADOS)

-- Commerce A mantiene acceso a sus notificaciones históricas ✅
SELECT * FROM notifications WHERE commerce_id = 5;
-- Resultado: 3 notificaciones
```

### Paso 3: Vinculación a Commerce B

```sql
-- Admin de Commerce B genera QR
INSERT INTO device_link_codes (code, commerce_id, expires_at)
VALUES ('XYZ789', 10, '2026-01-03 10:00:00');

-- Empleado escanea QR
-- Backend busca dispositivo por UUID
SELECT * FROM devices WHERE uuid = '98ba8129-...';
-- ✅ Encontrado: { id: 1, commerce_id: null, is_active: false }

-- Backend actualiza (NO crea nuevo)
UPDATE devices 
SET commerce_id = 10, is_active = true
WHERE id = 1;

-- Dispositivo envía notificaciones en Commerce B
INSERT INTO notifications (device_id, commerce_id, amount, created_at)
VALUES 
  (1, 10, 120.00, '2026-01-02 11:00:00'),
  (1, 10, 150.00, '2026-01-02 12:00:00');

-- Estado actual:
-- Device: { id: 1, uuid: "98ba8129-...", commerce_id: 10, is_active: true }
-- Notifications: 
--   - 3 registros con commerce_id=5 (Commerce A)
--   - 2 registros con commerce_id=10 (Commerce B)
```

### Paso 4: Desvinculación de Commerce B

```sql
-- Admin de Commerce B desvincula dispositivo
UPDATE devices 
SET commerce_id = NULL, user_id = NULL, is_active = false
WHERE id = 1;

-- Estado actual:
-- Device: { id: 1, uuid: "98ba8129-...", commerce_id: null, is_active: false }
-- Notifications: 
--   - 3 registros con commerce_id=5 (Commerce A) ✅
--   - 2 registros con commerce_id=10 (Commerce B) ✅

-- Ambos comercios mantienen sus historiales ✅
```

### Paso 5: Re-vinculación a Commerce A (El Regreso)

```sql
-- Admin de Commerce A genera NUEVO QR
INSERT INTO device_link_codes (code, commerce_id, expires_at)
VALUES ('DEF456', 5, '2026-01-04 10:00:00');

-- Empleado escanea QR
-- Backend busca dispositivo por UUID
SELECT * FROM devices WHERE uuid = '98ba8129-...';
-- ✅ Encontrado: { id: 1, commerce_id: null, is_active: false }

-- Validación: ¿Device tiene commerce_id diferente?
-- device.commerce_id = null
-- linkCode.commerce_id = 5
-- ✅ PERMITIDO (device no tiene commerce_id activo)

-- Backend actualiza (MISMO registro que al inicio)
UPDATE devices 
SET commerce_id = 5, is_active = true
WHERE id = 1;

-- Dispositivo envía nuevas notificaciones en Commerce A
INSERT INTO notifications (device_id, commerce_id, amount, created_at)
VALUES 
  (1, 5, 200.00, '2026-01-03 11:00:00'),
  (1, 5, 250.00, '2026-01-03 12:00:00');

-- Estado final:
-- Device: { id: 1, uuid: "98ba8129-...", commerce_id: 5, is_active: true }
-- Notifications: 
--   - 5 registros con commerce_id=5 (3 antiguos + 2 nuevos) ✅
--   - 2 registros con commerce_id=10 (Commerce B) ✅
```

---

## 📊 Vista de Datos Final

### Tabla `devices`

```sql
SELECT * FROM devices WHERE uuid = '98ba8129-...';

-- Resultado:
-- id | uuid          | commerce_id | is_active | created_at          | updated_at
-- 1  | 98ba8129-...  | 5           | true      | 2026-01-01 10:00:00 | 2026-01-03 10:00:00

-- ✅ MISMO ID desde el inicio (id=1)
-- ✅ MISMO UUID (98ba8129-...)
-- ✅ commerce_id actual: 5 (de vuelta en Commerce A)
-- ✅ created_at preservado (fecha original)
-- ✅ updated_at refleja última actualización
```

### Tabla `notifications`

```sql
SELECT 
    id,
    device_id,
    commerce_id,
    amount,
    created_at
FROM notifications 
WHERE device_id = 1
ORDER BY created_at;

-- Resultado:
-- id | device_id | commerce_id | amount  | created_at
-- 1  | 1         | 5           | 50.00   | 2026-01-01 11:00:00  ← Commerce A (periodo 1)
-- 2  | 1         | 5           | 75.00   | 2026-01-01 12:00:00  ← Commerce A (periodo 1)
-- 3  | 1         | 5           | 100.00  | 2026-01-01 13:00:00  ← Commerce A (periodo 1)
-- 4  | 1         | 10          | 120.00  | 2026-01-02 11:00:00  ← Commerce B
-- 5  | 1         | 10          | 150.00  | 2026-01-02 12:00:00  ← Commerce B
-- 6  | 1         | 5           | 200.00  | 2026-01-03 11:00:00  ← Commerce A (periodo 2)
-- 7  | 1         | 5           | 250.00  | 2026-01-03 12:00:00  ← Commerce A (periodo 2)

-- ✅ Historial COMPLETO preservado
-- ✅ Se puede ver el "viaje" del dispositivo entre comercios
-- ✅ Cada notificación mantiene su commerce_id original
```

---

## 🔍 Consultas por Comercio

### Commerce A (id=5) - Vista de sus notificaciones

```sql
SELECT * FROM notifications 
WHERE commerce_id = 5
ORDER BY created_at;

-- Resultado:
-- id | amount  | created_at
-- 1  | 50.00   | 2026-01-01 11:00:00  ← Periodo 1 (antes de irse)
-- 2  | 75.00   | 2026-01-01 12:00:00  ← Periodo 1
-- 3  | 100.00  | 2026-01-01 13:00:00  ← Periodo 1
-- 6  | 200.00  | 2026-01-03 11:00:00  ← Periodo 2 (después de regresar)
-- 7  | 250.00  | 2026-01-03 12:00:00  ← Periodo 2

-- ✅ Commerce A ve TODAS sus notificaciones (ambos periodos)
-- ✅ Total: 5 notificaciones
-- ✅ NO ve las notificaciones de Commerce B
```

### Commerce B (id=10) - Vista de sus notificaciones

```sql
SELECT * FROM notifications 
WHERE commerce_id = 10
ORDER BY created_at;

-- Resultado:
-- id | amount  | created_at
-- 4  | 120.00  | 2026-01-02 11:00:00
-- 5  | 150.00  | 2026-01-02 12:00:00

-- ✅ Commerce B ve solo SUS notificaciones
-- ✅ Total: 2 notificaciones
-- ✅ NO ve las notificaciones de Commerce A
```

---

## 🔐 Validaciones de Seguridad

### Validación 1: No se puede vincular si está activo en otro comercio

```php
// DeviceLinkService::linkDevice()

$device = Device::where('uuid', $deviceUuid)->first();

if ($device->commerce_id && $device->commerce_id !== $linkCode->commerce_id) {
    // ❌ RECHAZADO: Dispositivo activo en otro comercio
    return [
        'success' => false,
        'message' => 'El dispositivo ya pertenece a otro negocio'
    ];
}
```

**Escenario bloqueado:**
```
Device: { commerce_id: 10, is_active: true }  ← Activo en Commerce B
QR: { commerce_id: 5 }                        ← Intenta vincular a Commerce A

❌ RECHAZADO
💡 Solución: Commerce B debe desvincular primero
```

### Validación 2: Se puede vincular si está desvinculado

```php
$device = Device::where('uuid', $deviceUuid)->first();

if ($device->commerce_id === null) {
    // ✅ PERMITIDO: Dispositivo desvinculado
    $device->update([
        'commerce_id' => $linkCode->commerce_id,
        'is_active' => true,
    ]);
}
```

**Escenario permitido:**
```
Device: { commerce_id: null, is_active: false }  ← Desvinculado
QR: { commerce_id: 5 }                           ← Vincula a Commerce A

✅ PERMITIDO
✅ Device se actualiza con commerce_id = 5
```

---

## 📈 Análisis de Datos

### Detectar dispositivos que han cambiado de comercio

```sql
-- Contar notificaciones por dispositivo y comercio
SELECT 
    device_id,
    commerce_id,
    COUNT(*) as notification_count,
    MIN(created_at) as first_notification,
    MAX(created_at) as last_notification
FROM notifications
GROUP BY device_id, commerce_id
HAVING device_id = 1
ORDER BY first_notification;

-- Resultado:
-- device_id | commerce_id | count | first_notification  | last_notification
-- 1         | 5           | 3     | 2026-01-01 11:00:00 | 2026-01-01 13:00:00
-- 1         | 10          | 2     | 2026-01-02 11:00:00 | 2026-01-02 12:00:00
-- 1         | 5           | 2     | 2026-01-03 11:00:00 | 2026-01-03 12:00:00

-- ✅ Se puede ver claramente el "viaje" del dispositivo
-- ✅ Commerce A → Commerce B → Commerce A
```

### Detectar periodos de inactividad

```sql
-- Buscar gaps entre notificaciones
WITH notification_gaps AS (
    SELECT 
        device_id,
        commerce_id,
        created_at,
        LAG(created_at) OVER (PARTITION BY device_id ORDER BY created_at) as prev_notification,
        EXTRACT(EPOCH FROM (created_at - LAG(created_at) OVER (PARTITION BY device_id ORDER BY created_at))) / 3600 as hours_gap
    FROM notifications
    WHERE device_id = 1
)
SELECT * FROM notification_gaps
WHERE hours_gap > 24 OR prev_notification IS NULL;

-- Resultado muestra gaps grandes (desvinculaciones)
```

---

## 🎯 Casos de Uso Reales

### Caso 1: Dispositivo de Empleado Temporal

```
Escenario: Empleado temporal trabaja en múltiples negocios

1. Enero: Trabaja en Restaurant A
   - Device vinculado a commerce_id=5
   - Captura 100 notificaciones

2. Febrero: Termina contrato, se desvincula
   - Device: commerce_id=null

3. Marzo: Trabaja en Restaurant B
   - Device vinculado a commerce_id=10
   - Captura 50 notificaciones

4. Abril: Termina contrato, se desvincula
   - Device: commerce_id=null

5. Mayo: Regresa a Restaurant A
   - Device vinculado a commerce_id=5 (mismo que antes)
   - Captura nuevas notificaciones

Resultado:
- Restaurant A: 100 notificaciones antiguas + nuevas ✅
- Restaurant B: 50 notificaciones ✅
- Historial completo preservado ✅
- Aislamiento perfecto entre comercios ✅
```

### Caso 2: Dispositivo de Prueba/Demo

```
Escenario: Dispositivo usado para demos con múltiples clientes

1. Demo con Cliente A
   - Device vinculado a commerce_id=5
   - Genera datos de prueba

2. Termina demo, se desvincula
   - Device: commerce_id=null

3. Demo con Cliente B
   - Device vinculado a commerce_id=10
   - Genera datos de prueba

4. Cliente A decide contratar, regresa el dispositivo
   - Device vinculado a commerce_id=5
   - Empieza producción

Resultado:
- Cliente A ve solo sus datos (demo + producción) ✅
- Cliente B ve solo sus datos de demo ✅
- No hay contaminación cruzada ✅
```

### Caso 3: Dispositivo Compartido entre Sucursales

```
Escenario: Empresa con múltiples sucursales (cada una un commerce)

1. Dispositivo en Sucursal Norte (commerce_id=5)
   - Captura notificaciones

2. Dispositivo se transfiere a Sucursal Sur (commerce_id=10)
   - Se desvincula de Norte
   - Se vincula a Sur
   - Captura notificaciones

3. Dispositivo regresa a Sucursal Norte (commerce_id=5)
   - Se desvincula de Sur
   - Se vincula a Norte
   - Continúa capturando

Resultado:
- Cada sucursal mantiene su historial completo ✅
- Auditoría clara de transferencias ✅
- Contabilidad separada por sucursal ✅
```

---

## ⚠️ Consideraciones Importantes

### 1. Notificaciones NO se "mueven"

```
❌ INCORRECTO: Las notificaciones antiguas NO cambian de commerce_id

Cuando device regresa a Commerce A:
- Las notificaciones antiguas mantienen commerce_id=5 ✅
- Las notificaciones de Commerce B mantienen commerce_id=10 ✅
- Las nuevas notificaciones se crean con commerce_id=5 ✅

✅ CORRECTO: Cada notificación mantiene su commerce_id original
```

### 2. Historial es inmutable

```
✅ Las notificaciones son registros históricos
✅ NUNCA se modifican después de crearse
✅ NUNCA se eliminan (ni siquiera al desvincular)
✅ commerce_id es parte del registro histórico
```

### 3. Device es el "contenedor" actual

```
Device representa el estado ACTUAL del dispositivo físico:
- commerce_id: ¿A qué comercio pertenece AHORA?
- is_active: ¿Está activo AHORA?
- updated_at: ¿Cuándo fue la última actualización?

Las notificaciones representan el HISTORIAL:
- Cada notificación es un evento en el tiempo
- commerce_id en notification: ¿A qué comercio pertenecía el device CUANDO se creó?
```

---

## 📊 Diagrama de Estados

```
┌─────────────────────────────────────────────────────────────┐
│ ESTADO A: Vinculado a Commerce A (id=5)                    │
│ Device: { commerce_id: 5, is_active: true }                │
│ Notifications: 3 con commerce_id=5                         │
└─────────────────────────────────────────────────────────────┘
                            ↓ [Desvincular]
┌─────────────────────────────────────────────────────────────┐
│ ESTADO B: Desvinculado                                      │
│ Device: { commerce_id: null, is_active: false }            │
│ Notifications: 3 con commerce_id=5 (preservadas)           │
└─────────────────────────────────────────────────────────────┘
                            ↓ [Vincular a Commerce B]
┌─────────────────────────────────────────────────────────────┐
│ ESTADO C: Vinculado a Commerce B (id=10)                   │
│ Device: { commerce_id: 10, is_active: true }               │
│ Notifications: 3 con commerce_id=5 + 2 con commerce_id=10  │
└─────────────────────────────────────────────────────────────┘
                            ↓ [Desvincular]
┌─────────────────────────────────────────────────────────────┐
│ ESTADO D: Desvinculado                                      │
│ Device: { commerce_id: null, is_active: false }            │
│ Notifications: 3 con commerce_id=5 + 2 con commerce_id=10  │
└─────────────────────────────────────────────────────────────┘
                            ↓ [Vincular a Commerce A]
┌─────────────────────────────────────────────────────────────┐
│ ESTADO E: Re-vinculado a Commerce A (id=5)                 │
│ Device: { commerce_id: 5, is_active: true }                │
│ Notifications: 5 con commerce_id=5 + 2 con commerce_id=10  │
│                (3 antiguas + 2 nuevas)                      │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Garantías del Sistema

### 1. Integridad de Datos

- ✅ NUNCA se eliminan dispositivos
- ✅ NUNCA se eliminan notificaciones
- ✅ Historial completo siempre disponible
- ✅ Auditoría completa de transferencias

### 2. Aislamiento Multi-Tenant

- ✅ Cada comercio solo ve sus notificaciones
- ✅ No hay contaminación cruzada
- ✅ Notificaciones mantienen su commerce_id original
- ✅ Re-vinculación no afecta historiales anteriores

### 3. Flexibilidad

- ✅ Dispositivos pueden cambiar de comercio múltiples veces
- ✅ Dispositivos pueden regresar a comercios anteriores
- ✅ Proceso de desvinculación/re-vinculación es simple
- ✅ No hay límite de transferencias

### 4. Seguridad

- ✅ Solo admin del comercio actual puede desvincular
- ✅ No se puede vincular si está activo en otro comercio
- ✅ Todas las operaciones se loguean
- ✅ Validaciones multi-tenant en cada operación

---

## 📚 Referencias

- [Device Lifecycle](DEVICE_LIFECYCLE.md)
- [Multi-Tenant Security](../04-security/MULTI_TENANT_SECURITY.md)
- [QR Authorization Architecture](../03-architecture/QR_AUTHORIZATION_ARCHITECTURE.md)
- [DeviceLinkService](../../apps/api/app/Services/DeviceLinkService.php)
- [DeviceService](../../apps/api/app/Services/DeviceService.php)



