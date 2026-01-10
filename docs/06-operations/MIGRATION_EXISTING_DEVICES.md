# 🔄 Migración de Dispositivos Existentes

## ⏰ Línea de Tiempo de la Arquitectura

### Evolución del Schema de `devices`

```
📅 2024-01-01: Creación inicial
┌─────────────────────────────────────────────────────────────┐
│ CREATE TABLE devices (                                      │
│   id BIGINT,                                                │
│   user_id BIGINT NOT NULL,  ← OBLIGATORIO                  │
│   uuid UUID UNIQUE,                                         │
│   name VARCHAR,                                             │
│   platform VARCHAR DEFAULT 'android',                       │
│   is_active BOOLEAN DEFAULT true,                           │
│   last_seen_at TIMESTAMP                                    │
│ )                                                           │
└─────────────────────────────────────────────────────────────┘

📅 2025-01-15: Se añade commerce_id
┌─────────────────────────────────────────────────────────────┐
│ ALTER TABLE devices ADD COLUMN:                            │
│   commerce_id BIGINT NULLABLE,  ← NUEVO, nullable          │
│   alias VARCHAR NULLABLE                                    │
└─────────────────────────────────────────────────────────────┘

📅 2025-01-20: Se añaden campos de salud
┌─────────────────────────────────────────────────────────────┐
│ ALTER TABLE devices ADD COLUMNS:                           │
│   battery_level INT NULLABLE,                              │
│   battery_optimization_disabled BOOLEAN NULLABLE,          │
│   notification_permission_enabled BOOLEAN NULLABLE,        │
│   last_heartbeat TIMESTAMP NULLABLE                        │
└─────────────────────────────────────────────────────────────┘

📅 2025-12-28: user_id se hace nullable (ARQUITECTURA ACTUAL)
┌─────────────────────────────────────────────────────────────┐
│ ALTER TABLE devices MODIFY COLUMN:                         │
│   user_id BIGINT NULLABLE  ← AHORA OPCIONAL                │
│                                                             │
│ Razón: Soportar "Modo Capturador Anónimo"                 │
│ - Dispositivos pueden vincularse sin autenticación         │
│ - QR es la autorización primaria                           │
│ - user_id es opcional (solo para trazabilidad)            │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Estado Actual: ¿Funciona o No?

### ✅ LA ARQUITECTURA YA ESTÁ FUNCIONANDO

**Respuesta corta:** Sí, la arquitectura completa de transferencia entre comercios **YA ESTÁ FUNCIONANDO** desde que se ejecutaron las migraciones.

**Fecha de activación:** 
- `commerce_id` disponible desde: **2025-01-15**
- `user_id` nullable desde: **2025-12-28**
- Lógica de vinculación/desvinculación: **Implementada en el código actual**

### 📊 Verificación del Estado

```sql
-- Verificar que las migraciones se ejecutaron
SELECT * FROM migrations 
WHERE migration LIKE '%devices%'
ORDER BY batch, id;

-- Resultado esperado:
-- ✅ 2024_01_01_000002_create_devices_table
-- ✅ 2025_01_15_000003_add_commerce_to_devices_table
-- ✅ 2025_01_20_000002_add_health_fields_to_devices_table
-- ✅ 2025_12_28_000001_make_user_id_nullable_in_devices_table
```

---

## 🔍 Análisis de Dispositivos Existentes

### Escenario 1: Dispositivos creados ANTES de 2025-01-15

**Problema:** No tienen `commerce_id` (era NULL por defecto)

```sql
-- Identificar dispositivos sin commerce_id
SELECT 
    id,
    uuid,
    user_id,
    commerce_id,  -- NULL
    name,
    created_at
FROM devices
WHERE commerce_id IS NULL;
```

**Estado:**
```
Device: { 
  id: 1, 
  uuid: "abc-123", 
  user_id: 5,           ← Tiene usuario
  commerce_id: null,    ← NO tiene comercio ❌
  created_at: "2024-12-01"
}
```

**¿Qué pasa con estos dispositivos?**

1. **Al enviar notificaciones:**
   ```php
   // NotificationController::store()
   
   if (!$device->commerce_id) {
       return response()->json([
           'message' => 'Dispositivo no vinculado. Por favor, escanea el código QR.',
           'error' => 'device_not_linked',
       ], 403);
   }
   ```
   
   ❌ **RECHAZADO** - No pueden enviar notificaciones

2. **Solución automática:**
   ```php
   // DeviceService::findDeviceByUuid()
   
   // Si el usuario tiene commerce_id, se auto-sincroniza
   if (!$device->commerce_id && $user->commerce_id) {
       $device->update(['commerce_id' => $user->commerce_id]);
   }
   ```
   
   ✅ **AUTO-REPARADO** - Al hacer login, se sincroniza automáticamente

### Escenario 2: Dispositivos creados DESPUÉS de 2025-01-15

**Estado:** Deberían tener `commerce_id` si se vincularon correctamente

```sql
-- Verificar dispositivos recientes
SELECT 
    id,
    uuid,
    user_id,
    commerce_id,
    created_at
FROM devices
WHERE created_at >= '2025-01-15'
ORDER BY created_at DESC;
```

**Posibles estados:**

```
✅ CORRECTO:
Device: { 
  commerce_id: 5,    ← Vinculado correctamente
  user_id: 10,       ← Usuario asociado
  is_active: true
}

⚠️ PARCIAL:
Device: { 
  commerce_id: 5,    ← Vinculado correctamente
  user_id: null,     ← Sin usuario (modo capturador)
  is_active: true
}

❌ INCORRECTO:
Device: { 
  commerce_id: null, ← NO vinculado
  user_id: 10,       ← Tiene usuario pero no comercio
  is_active: true
}
```

---

## 🛠️ ¿Se Deben Borrar y Re-vincular?

### ❌ NO, NO ES NECESARIO BORRAR

**Razón:** El sistema tiene **auto-reparación** y **migración en caliente**.

### ✅ Estrategia de Migración (Sin Downtime)

#### Opción 1: Auto-reparación Pasiva (Recomendada)

**Cómo funciona:**
1. Los dispositivos antiguos sin `commerce_id` siguen en la BD
2. Cuando el usuario hace login, el sistema detecta `commerce_id = null`
3. Si el usuario tiene `commerce_id`, se auto-sincroniza
4. El dispositivo queda funcional automáticamente

**Código que lo hace:**
```php
// DeviceService::findDeviceByUuid()

if (!$device->commerce_id && $user->commerce_id) {
    $device->update(['commerce_id' => $user->commerce_id]);
    
    Log::info('Device commerce_id auto-synced from user', [
        'device_id' => $device->id,
        'commerce_id' => $user->commerce_id,
    ]);
}
```

**Ventajas:**
- ✅ Sin downtime
- ✅ Sin intervención manual
- ✅ Se repara al primer login
- ✅ No se pierden datos históricos

**Desventajas:**
- ⚠️ Dispositivos sin login reciente quedan sin reparar
- ⚠️ Si el usuario no tiene `commerce_id`, no se repara

---

#### Opción 2: Migración Proactiva (Script de Datos)

**Cuándo usarla:**
- Si hay muchos dispositivos sin `commerce_id`
- Si quieres garantizar que todos estén correctos
- Si tienes dispositivos huérfanos (sin usuario o usuario sin comercio)

**Script de migración:**

```php
<?php
// database/migrations/2026_01_05_000001_migrate_devices_commerce_id.php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

return new class extends Migration
{
    /**
     * Run the migrations.
     * 
     * Migrates devices without commerce_id by syncing from their user's commerce_id.
     * This is a data migration, not a schema migration.
     */
    public function up(): void
    {
        Log::info('Starting device commerce_id migration');
        
        // Find devices without commerce_id but with user_id
        $devicesWithoutCommerce = DB::table('devices')
            ->whereNull('commerce_id')
            ->whereNotNull('user_id')
            ->get();
        
        $migratedCount = 0;
        $skippedCount = 0;
        
        foreach ($devicesWithoutCommerce as $device) {
            // Get user's commerce_id
            $user = DB::table('users')->find($device->user_id);
            
            if ($user && $user->commerce_id) {
                // Update device with user's commerce_id
                DB::table('devices')
                    ->where('id', $device->id)
                    ->update([
                        'commerce_id' => $user->commerce_id,
                        'updated_at' => now(),
                    ]);
                
                Log::info('Device commerce_id migrated', [
                    'device_id' => $device->id,
                    'device_uuid' => $device->uuid,
                    'user_id' => $device->user_id,
                    'commerce_id' => $user->commerce_id,
                ]);
                
                $migratedCount++;
            } else {
                Log::warning('Device user has no commerce_id - skipped', [
                    'device_id' => $device->id,
                    'device_uuid' => $device->uuid,
                    'user_id' => $device->user_id,
                ]);
                
                $skippedCount++;
            }
        }
        
        Log::info('Device commerce_id migration completed', [
            'migrated' => $migratedCount,
            'skipped' => $skippedCount,
        ]);
    }

    /**
     * Reverse the migrations.
     * 
     * WARNING: This will set commerce_id to NULL for migrated devices.
     * Only use if you need to rollback the migration.
     */
    public function down(): void
    {
        // No rollback - data migrations are typically not reversible
        Log::warning('Device commerce_id migration rollback not implemented');
    }
};
```

**Ejecutar migración:**
```bash
# En Docker
docker exec -it yape-notifier-php-fpm-prod php artisan migrate

# Local
php artisan migrate
```

**Verificar resultados:**
```sql
-- Contar dispositivos migrados
SELECT 
    COUNT(*) as total_devices,
    COUNT(commerce_id) as devices_with_commerce,
    COUNT(*) - COUNT(commerce_id) as devices_without_commerce
FROM devices;

-- Resultado esperado:
-- total_devices: 10
-- devices_with_commerce: 10  ← Todos tienen commerce_id ✅
-- devices_without_commerce: 0
```

---

#### Opción 3: Migración Manual (Para Casos Específicos)

**Cuándo usarla:**
- Pocos dispositivos (< 10)
- Quieres control total
- Necesitas validar cada caso

**Pasos:**

1. **Identificar dispositivos sin commerce_id:**
   ```sql
   SELECT 
       d.id,
       d.uuid,
       d.user_id,
       u.email as user_email,
       u.commerce_id as user_commerce_id,
       d.commerce_id as device_commerce_id,
       d.created_at
   FROM devices d
   LEFT JOIN users u ON d.user_id = u.id
   WHERE d.commerce_id IS NULL
   ORDER BY d.created_at DESC;
   ```

2. **Para cada dispositivo, decidir:**
   
   **Caso A: Usuario tiene commerce_id**
   ```sql
   -- Sincronizar commerce_id del usuario
   UPDATE devices 
   SET commerce_id = (SELECT commerce_id FROM users WHERE id = devices.user_id)
   WHERE id = 1 AND commerce_id IS NULL;
   ```
   
   **Caso B: Usuario NO tiene commerce_id**
   ```sql
   -- Opción 1: Asignar a un comercio específico
   UPDATE devices SET commerce_id = 5 WHERE id = 1;
   
   -- Opción 2: Desvincular (marcar como inactivo)
   UPDATE devices 
   SET is_active = false, commerce_id = NULL 
   WHERE id = 1;
   
   -- Opción 3: Eliminar si es dispositivo de prueba
   DELETE FROM devices WHERE id = 1;
   ```

3. **Verificar:**
   ```sql
   SELECT * FROM devices WHERE commerce_id IS NULL;
   -- Resultado esperado: 0 rows
   ```

---

## 📊 Diagnóstico Rápido

### Script de Diagnóstico

```sql
-- ============================================
-- DIAGNÓSTICO DE DISPOSITIVOS
-- ============================================

-- 1. Resumen general
SELECT 
    'Total Devices' as metric,
    COUNT(*) as count
FROM devices
UNION ALL
SELECT 
    'With commerce_id',
    COUNT(*) 
FROM devices 
WHERE commerce_id IS NOT NULL
UNION ALL
SELECT 
    'Without commerce_id',
    COUNT(*) 
FROM devices 
WHERE commerce_id IS NULL
UNION ALL
SELECT 
    'Active devices',
    COUNT(*) 
FROM devices 
WHERE is_active = true
UNION ALL
SELECT 
    'Inactive devices',
    COUNT(*) 
FROM devices 
WHERE is_active = false;

-- 2. Dispositivos problemáticos
SELECT 
    'PROBLEMA' as status,
    d.id,
    d.uuid,
    d.user_id,
    u.email,
    u.commerce_id as user_commerce,
    d.commerce_id as device_commerce,
    d.is_active,
    d.created_at
FROM devices d
LEFT JOIN users u ON d.user_id = u.id
WHERE d.commerce_id IS NULL
ORDER BY d.created_at DESC;

-- 3. Dispositivos correctos
SELECT 
    'OK' as status,
    d.id,
    d.uuid,
    d.commerce_id,
    c.name as commerce_name,
    d.is_active,
    d.created_at
FROM devices d
LEFT JOIN commerces c ON d.commerce_id = c.id
WHERE d.commerce_id IS NOT NULL
ORDER BY d.created_at DESC
LIMIT 10;

-- 4. Análisis de notificaciones
SELECT 
    d.id as device_id,
    d.uuid,
    d.commerce_id as device_commerce,
    COUNT(n.id) as notification_count,
    COUNT(DISTINCT n.commerce_id) as unique_commerces_in_notifications
FROM devices d
LEFT JOIN notifications n ON d.id = n.device_id
GROUP BY d.id, d.uuid, d.commerce_id
HAVING d.commerce_id IS NULL AND notification_count > 0
ORDER BY notification_count DESC;
```

**Ejecutar diagnóstico:**
```bash
# En Docker
docker exec -it yape-notifier-postgres-prod psql -U yapenotifier -d yapenotifier_prod -f /path/to/diagnostic.sql

# O desde PHP
php artisan tinker
>>> DB::select("SELECT COUNT(*) FROM devices WHERE commerce_id IS NULL");
```

---

## 🎯 Recomendación Final

### Para tu Caso Específico:

#### Si estás en **MVP/Desarrollo** (pocos usuarios):
```
✅ Opción 1: Auto-reparación Pasiva

Acción: NINGUNA
- Deja que el sistema se auto-repare al login
- Monitorea los logs para ver la sincronización
- Si hay dispositivos huérfanos, usa migración manual
```

#### Si estás en **Producción** (muchos usuarios):
```
✅ Opción 2: Migración Proactiva

Acción: Ejecutar script de migración
1. Crear migración de datos (script arriba)
2. Ejecutar en horario de bajo tráfico
3. Verificar con diagnóstico
4. Monitorear logs por 24-48 horas
```

#### Si tienes **Dispositivos de Prueba**:
```
✅ Opción 3: Limpieza Manual

Acción: Eliminar dispositivos de prueba
1. Identificar dispositivos sin commerce_id
2. Verificar que no tengan notificaciones importantes
3. Eliminar o desvincular
4. Crear nuevos dispositivos con QR
```

---

## ⚠️ Casos Especiales

### Caso 1: Dispositivo con notificaciones pero sin commerce_id

```sql
-- Identificar
SELECT 
    d.id,
    d.uuid,
    d.commerce_id,
    COUNT(n.id) as notification_count
FROM devices d
INNER JOIN notifications n ON d.id = n.device_id
WHERE d.commerce_id IS NULL
GROUP BY d.id;
```

**Solución:**
```sql
-- Inferir commerce_id de las notificaciones
UPDATE devices d
SET commerce_id = (
    SELECT n.commerce_id 
    FROM notifications n 
    WHERE n.device_id = d.id 
    GROUP BY n.commerce_id 
    ORDER BY COUNT(*) DESC 
    LIMIT 1
)
WHERE d.commerce_id IS NULL
AND EXISTS (SELECT 1 FROM notifications WHERE device_id = d.id);
```

### Caso 2: Usuario sin commerce_id

```sql
-- Identificar usuarios sin comercio
SELECT 
    u.id,
    u.email,
    u.commerce_id,
    COUNT(d.id) as device_count
FROM users u
LEFT JOIN devices d ON u.id = d.user_id
WHERE u.commerce_id IS NULL
GROUP BY u.id;
```

**Solución:**
```sql
-- Asignar a un comercio por defecto o crear uno
INSERT INTO commerces (name, created_at, updated_at)
VALUES ('Comercio Principal', NOW(), NOW())
RETURNING id;

-- Asignar usuarios sin comercio
UPDATE users 
SET commerce_id = 1  -- ID del comercio creado
WHERE commerce_id IS NULL;
```

---

## ✅ Checklist de Migración

```
□ Ejecutar diagnóstico SQL
□ Verificar migraciones aplicadas
□ Identificar dispositivos sin commerce_id
□ Decidir estrategia (pasiva/proactiva/manual)
□ Hacer backup de la BD
□ Ejecutar migración (si aplica)
□ Verificar resultados con diagnóstico
□ Monitorear logs por 24-48 horas
□ Validar que notificaciones se envíen correctamente
□ Documentar casos especiales (si los hay)
```

---

## 📚 Referencias

- [Device Lifecycle](DEVICE_LIFECYCLE.md)
- [Device Commerce Transfer](DEVICE_COMMERCE_TRANSFER.md)
- [QR Authorization Architecture](../03-architecture/QR_AUTHORIZATION_ARCHITECTURE.md)
- [Multi-Tenant Security](../04-security/MULTI_TENANT_SECURITY.md)


