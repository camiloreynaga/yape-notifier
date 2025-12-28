# Validación y Aseguramiento de Apps Duales

## 📋 Resumen Ejecutivo

Este documento describe cómo el sistema **asegura** el soporte de apps duales (MIUI Dual Apps, Parallel Apps, etc.) y cómo **revisar profesionalmente** que esta funcionalidad está operando correctamente.

---

## 🔒 Cómo se Asegura el Soporte de Apps Duales

### 1. **Nivel de Base de Datos: Constraint Único**

**Ubicación:** `apps/api/database/migrations/2025_01_15_000004_create_app_instances_table.php`

```php
// Constraint único que garantiza unicidad
$table->unique(['device_id', 'package_name', 'android_user_id'], 'unique_app_instance');
```

**¿Qué asegura?**

- ✅ **Integridad referencial**: Imposible tener dos instancias idénticas en la misma BD
- ✅ **Prevención de duplicados**: La BD rechaza automáticamente intentos de crear instancias duplicadas
- ✅ **Consistencia de datos**: Garantiza que cada combinación `(device_id, package_name, android_user_id)` es única

**Validación:**

```sql
-- Verificar que el constraint existe
SHOW CREATE TABLE app_instances;

-- Intentar insertar duplicado (debe fallar)
INSERT INTO app_instances (commerce_id, device_id, package_name, android_user_id)
VALUES (1, 1, 'com.bcp.innovacxion.yapeapp', 0);
INSERT INTO app_instances (commerce_id, device_id, package_name, android_user_id)
VALUES (1, 1, 'com.bcp.innovacxion.yapeapp', 0); -- ❌ Debe fallar
```

---

### 2. **Nivel de Backend: Creación Automática y Validación**

**Ubicación:** `apps/api/app/Services/NotificationService.php`

#### 2.1. Creación Automática de AppInstance

```php
// Líneas 84-103: Creación automática cuando llega una notificación
if (isset($data['package_name']) && isset($data['android_user_id']) && $commerceId) {
    try {
        $appInstance = $this->appInstanceService->findOrCreate(
            $device,
            $data['package_name'],
            $data['android_user_id']
        );
    } catch (\Exception $e) {
        Log::error('Failed to create/find app instance', [...]);
        // Continue without app instance
    }
}
```

**¿Qué asegura?**

- ✅ **Detección automática**: Cada notificación crea/busca su AppInstance automáticamente
- ✅ **Sin intervención manual**: No requiere configuración previa
- ✅ **Manejo de errores**: Logs detallados si falla la creación

#### 2.2. Deduplicación Mejorada

```php
// Líneas 199-206: Deduplicación que incluye android_user_id
if (isset($data['package_name']) && isset($data['android_user_id'])) {
    $query->where('package_name', $data['package_name'])
        ->where('android_user_id', $data['android_user_id']);
}
```

**¿Qué asegura?**

- ✅ **Deduplicación precisa**: Distingue entre instancias duales del mismo package
- ✅ **Prevención de falsos positivos**: No marca como duplicado una notificación de Yape 1 cuando viene de Yape 2

#### 2.3. Modelo AppInstance con findOrCreate

**Ubicación:** `apps/api/app/Models/AppInstance.php`

```php
public static function findOrCreate(
    int $commerceId,
    int $deviceId,
    string $packageName,
    int $androidUserId,
    ?string $instanceLabel = null
): self {
    return self::firstOrCreate(
        [
            'device_id' => $deviceId,
            'package_name' => $packageName,
            'android_user_id' => $androidUserId,
        ],
        [
            'commerce_id' => $commerceId,
            'instance_label' => $instanceLabel,
        ]
    );
}
```

**¿Qué asegura?**

- ✅ **Idempotencia**: Múltiples llamadas con los mismos parámetros retornan la misma instancia
- ✅ **Thread-safe**: Laravel maneja la concurrencia correctamente
- ✅ **Atomicidad**: Operación atómica (no puede quedar en estado inconsistente)

---

### 3. **Nivel de Android: Captura Correcta de android_user_id**

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt`

```kotlin
// Líneas 68-78: Captura correcta de identificadores duales
@Suppress("DEPRECATION")
val androidUserId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    sbn.userId  // ✅ CORRECTO: Público, funcional, confiable
} else {
    null
}
val androidUid = sbn.uid // Optional UID
val postedAt = sbn.postTime
```

**¿Qué asegura?**

- ✅ **Identificador único**: `sbn.userId` es el identificador oficial de Android para perfiles duales
- ✅ **Estabilidad**: El valor no cambia entre reinicios de la app
- ✅ **Compatibilidad**: Funciona desde API 24 (Android 7.0) hasta la actualidad

**⚠️ IMPORTANTE:**

- ❌ **NO usar** `sbn.user?.hashCode()` - No es confiable
- ❌ **NO usar** `sbn.user?.getIdentifier()` - Puede ser API oculta
- ✅ **USAR** `sbn.userId` - Solución correcta y documentada

---

### 4. **Nivel de Tests: Validación Automatizada**

**Ubicación:** `apps/api/tests/Unit/NotificationServiceDualAppsTest.php`

#### Test 1: Creación con identificadores duales

```php
public function test_create_notification_with_dual_app_identifiers(): void
{
    // Verifica que se crea correctamente con android_user_id
    $this->assertEquals(10, $notification->android_user_id);
    $this->assertNotNull($notification->app_instance_id);
}
```

#### Test 2: Diferentes android_user_id crean diferentes instancias

```php
public function test_different_android_user_ids_create_different_instances(): void
{
    // Yape 1 (android_user_id = 10)
    $notification1 = $this->service->createNotification($data1, $device);

    // Yape 2 (android_user_id = 11)
    $notification2 = $this->service->createNotification($data2, $device);

    // Deben tener diferentes app_instance_id
    $this->assertNotEquals($notification1->app_instance_id, $notification2->app_instance_id);
}
```

**¿Qué asegura?**

- ✅ **Validación automatizada**: Tests que se ejecutan en CI/CD
- ✅ **Regresión**: Detecta si se rompe la funcionalidad en el futuro
- ✅ **Documentación viva**: Los tests documentan el comportamiento esperado

---

## 🔍 Cómo Revisar Profesionalmente

### Checklist de Validación

#### ✅ 1. Verificación en Base de Datos

```sql
-- 1. Verificar que existen múltiples instancias del mismo package
SELECT
    device_id,
    package_name,
    android_user_id,
    instance_label,
    COUNT(*) as notification_count
FROM app_instances ai
LEFT JOIN notifications n ON n.app_instance_id = ai.id
WHERE package_name = 'com.bcp.innovacxion.yapeapp'
GROUP BY ai.id, device_id, package_name, android_user_id, instance_label
ORDER BY device_id, android_user_id;

-- Resultado esperado: Múltiples filas con diferentes android_user_id
-- Ejemplo:
-- device_id | package_name                          | android_user_id | instance_label | notification_count
-- 1         | com.bcp.innovacxion.yapeapp         | 0              | Yape Principal | 150
-- 1         | com.bcp.innovacxion.yapeapp         | 999            | Yape Secundario| 75
```

```sql
-- 2. Verificar constraint único
SELECT
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'app_instances'
AND CONSTRAINT_NAME = 'unique_app_instance';

-- Resultado esperado: UNIQUE constraint existe
```

```sql
-- 3. Verificar que no hay duplicados (debe retornar 0 filas)
SELECT
    device_id,
    package_name,
    android_user_id,
    COUNT(*) as count
FROM app_instances
GROUP BY device_id, package_name, android_user_id
HAVING COUNT(*) > 1;

-- Resultado esperado: 0 filas
```

---

#### ✅ 2. Verificación en Logs de Android

**Ubicación:** Logcat con filtro `PaymentNotificationService`

```bash
# Buscar capturas de notificaciones con diferentes android_user_id
adb logcat | grep "PaymentNotificationService" | grep "UserId"

# Resultado esperado:
# PaymentNotificationService: Payment notification saved locally. Package: com.bcp.innovacxion.yapeapp, UserId: 0, ...
# PaymentNotificationService: Payment notification saved locally. Package: com.bcp.innovacxion.yapeapp, UserId: 999, ...
```

**Validación:**

- ✅ Verificar que se capturan diferentes `UserId` para el mismo package
- ✅ Verificar que el valor no cambia entre reinicios de la app
- ✅ Verificar que se envía correctamente al backend

---

#### ✅ 3. Verificación en Backend (Logs)

**Ubicación:** `storage/logs/laravel.log`

```bash
# Buscar creación de AppInstances
grep "app instance" storage/logs/laravel.log | tail -20

# Buscar errores relacionados
grep -i "android_user_id\|app_instance" storage/logs/laravel.log | grep -i error
```

**Validación:**

- ✅ No debe haber errores al crear AppInstances
- ✅ Debe haber logs de creación para diferentes `android_user_id`

---

#### ✅ 4. Prueba en Dispositivo Real

**Requisitos:**

- Dispositivo con soporte de apps duales (MIUI, Samsung, etc.)
- Dos instancias de Yape configuradas (Yape 1 y Yape 2)

**Pasos:**

1. Configurar dos instancias de Yape en el dispositivo
2. Enviar un pago desde Yape 1
3. Enviar un pago desde Yape 2
4. Verificar en el Dashboard que aparecen dos instancias diferentes
5. Verificar que las notificaciones se asignan correctamente a cada instancia

**Resultado esperado:**

- ✅ Dashboard muestra 2 instancias de `com.bcp.innovacxion.yapeapp`
- ✅ Cada instancia tiene un `android_user_id` diferente (ej: 0 y 999)
- ✅ Las notificaciones se filtran correctamente por instancia

---

#### ✅ 5. Ejecutar Tests Automatizados

```bash
# Desde el directorio apps/api
php artisan test --filter=NotificationServiceDualAppsTest
php artisan test --filter=AppInstanceServiceTest

# Resultado esperado: Todos los tests pasan
```

---

#### ✅ 6. Verificación en Dashboard Web

**Pantalla:** `AppInstancesPage.tsx`

**Validaciones:**

1. ✅ Navegar a "Instancias de Apps" en el dashboard
2. ✅ Verificar que se muestran múltiples instancias del mismo package
3. ✅ Verificar que cada instancia muestra su `android_user_id`
4. ✅ Probar asignar nombres a las instancias (ej: "Yape Principal", "Yape Secundario")
5. ✅ Verificar que el filtro por instancia funciona en la página de notificaciones

---

## 🎯 Manera Profesional de Trabajar esta Parte

### 1. **Arquitectura en Capas con Validación**

```
┌─────────────────────────────────────────┐
│  Android: Captura android_user_id      │  ← Validación: Logs + Tests unitarios
│  (PaymentNotificationListenerService)   │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  Backend: Creación automática           │  ← Validación: Tests + Logs
│  (NotificationService)                  │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  Base de Datos: Constraint único       │  ← Validación: Migraciones + Queries
│  (unique_app_instance)                 │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  Dashboard: Gestión manual              │  ← Validación: UI/UX + Tests E2E
│  (AppInstancesPage)                     │
└─────────────────────────────────────────┘
```

### 2. **Principios Aplicados**

#### ✅ **Defensa en Profundidad (Defense in Depth)**

- Múltiples capas de validación (Android → Backend → BD)
- Si una capa falla, las otras previenen el error

#### ✅ **Fail-Safe Defaults**

- Si `android_user_id` es `null`, no se crea AppInstance (pero la notificación se guarda)
- El sistema funciona incluso si falta el identificador dual

#### ✅ **Principle of Least Surprise**

- Comportamiento predecible: misma combinación = misma instancia
- `findOrCreate` es idempotente

#### ✅ **Observabilidad**

- Logs detallados en cada capa
- Métricas disponibles en el dashboard

---

### 3. **Monitoreo y Alertas Recomendados**

#### Métricas a Monitorear:

```sql
-- 1. Instancias sin asignar (deben ser asignadas manualmente)
SELECT COUNT(*) as unassigned_instances
FROM app_instances
WHERE instance_label IS NULL;

-- 2. Dispositivos con múltiples instancias del mismo package
SELECT
    device_id,
    package_name,
    COUNT(*) as instance_count
FROM app_instances
GROUP BY device_id, package_name
HAVING COUNT(*) > 1;

-- 3. Notificaciones sin app_instance_id (posible problema)
SELECT COUNT(*) as notifications_without_instance
FROM notifications
WHERE package_name IS NOT NULL
AND android_user_id IS NOT NULL
AND app_instance_id IS NULL;
```

#### Alertas Recomendadas:

1. **Alta prioridad:**

   - Notificaciones con `android_user_id` pero sin `app_instance_id` (indica fallo en creación)
   - Errores al crear AppInstance en logs

2. **Media prioridad:**
   - Muchas instancias sin asignar (más de 10)
   - Dispositivos con más de 5 instancias del mismo package (posible configuración incorrecta)

---

### 4. **Workflow de Gestión**

#### Flujo Normal (Automático):

```
1. Android captura notificación → Captura android_user_id
2. Android envía al backend → Incluye package_name + android_user_id
3. Backend crea/busca AppInstance → Automático
4. Notificación se asocia a AppInstance → Automático
```

#### Flujo de Asignación Manual (Dashboard):

```
1. Admin ve instancias "Sin asignar" en dashboard
2. Admin hace clic en "Editar" en una instancia
3. Admin asigna nombre (ej: "Yape Principal")
4. Backend actualiza instance_label
5. Dashboard muestra instancia como "Asignada"
```

---

### 5. **Documentación y Mantenimiento**

#### Documentación Existente:

- ✅ `docs/03-architecture/DUAL_APPS.md` - Arquitectura general
- ✅ `docs/03-architecture/ANDROID_USER_ID.md` - Análisis técnico de android_user_id
- ✅ Este documento - Validación y revisión

#### Mantenimiento Recomendado:

1. **Revisión mensual:**

   - Ejecutar queries de validación
   - Revisar logs de errores
   - Verificar que no hay instancias huérfanas

2. **Revisión trimestral:**

   - Actualizar tests si cambia el comportamiento
   - Revisar documentación
   - Validar en dispositivos nuevos con apps duales

3. **Cuando se detecta un problema:**
   - Revisar logs de Android (captura)
   - Revisar logs de Backend (creación)
   - Verificar constraint en BD
   - Ejecutar tests automatizados

---

## 📊 Dashboard de Validación

### Queries Útiles para el Dashboard

```sql
-- Vista: Resumen de instancias por dispositivo
CREATE OR REPLACE VIEW v_app_instances_summary AS
SELECT
    d.id as device_id,
    d.name as device_name,
    ai.package_name,
    COUNT(DISTINCT ai.android_user_id) as instance_count,
    COUNT(DISTINCT CASE WHEN ai.instance_label IS NOT NULL THEN ai.id END) as assigned_count,
    COUNT(DISTINCT CASE WHEN ai.instance_label IS NULL THEN ai.id END) as unassigned_count,
    COUNT(n.id) as total_notifications
FROM devices d
LEFT JOIN app_instances ai ON ai.device_id = d.id
LEFT JOIN notifications n ON n.app_instance_id = ai.id
GROUP BY d.id, d.name, ai.package_name;
```

---

## ✅ Conclusión

El sistema **asegura** el soporte de apps duales mediante:

1. ✅ **Constraint único en BD** - Previene duplicados a nivel de base de datos
2. ✅ **Creación automática** - Detecta y crea instancias sin intervención manual
3. ✅ **Captura correcta** - Usa `sbn.userId` que es el identificador oficial de Android
4. ✅ **Tests automatizados** - Valida el comportamiento en CI/CD
5. ✅ **Deduplicación mejorada** - Distingue entre instancias duales

**Para revisar profesionalmente:**

- Ejecutar queries de validación en BD
- Revisar logs de Android y Backend
- Probar en dispositivo real con apps duales
- Ejecutar tests automatizados
- Verificar en el Dashboard web

**Mantenimiento:**

- Monitoreo mensual de métricas
- Revisión trimestral de documentación
- Alertas para problemas críticos

---

_Última actualización: 2025-01-21_
