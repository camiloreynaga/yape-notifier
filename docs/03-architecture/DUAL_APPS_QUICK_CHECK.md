# Checklist Rápido: Validación de Apps Duales

## 🚀 Validación Rápida (5 minutos)

### 1. Verificar Constraint en BD (30 segundos)

```sql
-- Verificar que el constraint único existe
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'app_instances'
AND CONSTRAINT_NAME = 'unique_app_instance';
```

**✅ Resultado esperado:** 1 fila con `CONSTRAINT_TYPE = 'UNIQUE'`

---

### 2. Verificar Instancias Duales Existentes (1 minuto)

```sql
-- Buscar dispositivos con múltiples instancias del mismo package
SELECT 
    d.name as device_name,
    ai.package_name,
    COUNT(DISTINCT ai.android_user_id) as instance_count,
    GROUP_CONCAT(DISTINCT CONCAT('User ', ai.android_user_id, 
        IF(ai.instance_label IS NOT NULL, 
           CONCAT(' (', ai.instance_label, ')'), 
           '')) 
        SEPARATOR ', ') as instances
FROM app_instances ai
JOIN devices d ON d.id = ai.device_id
GROUP BY d.id, d.name, ai.package_name
HAVING COUNT(DISTINCT ai.android_user_id) > 1
ORDER BY instance_count DESC;
```

**✅ Resultado esperado:** 
- Si hay dispositivos con apps duales, deben aparecer aquí
- Ejemplo: `device_name: "Mi Xiaomi", package_name: "com.bcp.innovacxion.yapeapp", instance_count: 2`

---

### 3. Verificar Notificaciones Asociadas Correctamente (1 minuto)

```sql
-- Verificar que las notificaciones están asociadas a instancias
SELECT 
    ai.package_name,
    ai.android_user_id,
    ai.instance_label,
    COUNT(n.id) as notification_count,
    MIN(n.received_at) as first_notification,
    MAX(n.received_at) as last_notification
FROM app_instances ai
LEFT JOIN notifications n ON n.app_instance_id = ai.id
WHERE ai.package_name IS NOT NULL
GROUP BY ai.id, ai.package_name, ai.android_user_id, ai.instance_label
ORDER BY notification_count DESC
LIMIT 20;
```

**✅ Resultado esperado:**
- Cada instancia debe tener notificaciones asociadas
- `android_user_id` debe ser diferente para instancias del mismo package

---

### 4. Detectar Problemas Potenciales (1 minuto)

```sql
-- Notificaciones con android_user_id pero sin app_instance_id (PROBLEMA)
SELECT 
    COUNT(*) as problematic_notifications,
    COUNT(DISTINCT device_id) as affected_devices,
    COUNT(DISTINCT package_name) as affected_packages
FROM notifications
WHERE package_name IS NOT NULL
AND android_user_id IS NOT NULL
AND app_instance_id IS NULL;
```

**✅ Resultado esperado:** `problematic_notifications = 0` (si hay valores > 0, hay un problema)

---

### 5. Verificar Instancias Sin Asignar (30 segundos)

```sql
-- Instancias que necesitan asignación manual
SELECT 
    COUNT(*) as unassigned_count,
    COUNT(DISTINCT device_id) as devices_with_unassigned,
    COUNT(DISTINCT package_name) as packages_with_unassigned
FROM app_instances
WHERE instance_label IS NULL;
```

**✅ Resultado esperado:** 
- Puede haber instancias sin asignar (normal)
- Si hay muchas (>10), considerar asignación masiva desde dashboard

---

## 🔍 Validación Detallada (15 minutos)

### 6. Verificar Logs de Android

```bash
# En dispositivo Android conectado
adb logcat -d | grep "PaymentNotificationService" | grep -E "UserId|androidUserId" | tail -20
```

**✅ Resultado esperado:**
- Debe mostrar diferentes `UserId` para el mismo package
- Ejemplo: `UserId: 0` y `UserId: 999` para `com.bcp.innovacxion.yapeapp`

---

### 7. Verificar Logs de Backend

```bash
# En servidor backend
tail -100 storage/logs/laravel.log | grep -i "app.instance\|android_user_id" | tail -20
```

**✅ Resultado esperado:**
- No debe haber errores relacionados con `app_instance`
- Debe haber logs de creación exitosa de instancias

---

### 8. Ejecutar Tests Automatizados

```bash
# Desde apps/api
php artisan test --filter=NotificationServiceDualAppsTest
php artisan test --filter=AppInstanceServiceTest
```

**✅ Resultado esperado:** Todos los tests pasan (verde)

---

### 9. Verificar en Dashboard Web

1. Navegar a `/app-instances`
2. Buscar instancias del mismo package con diferentes `android_user_id`
3. Verificar que se pueden editar y asignar nombres
4. Ir a `/notifications` y verificar que el filtro por instancia funciona

**✅ Resultado esperado:**
- Dashboard muestra instancias correctamente
- Filtro funciona
- Edición de nombres funciona

---

## 🎯 Validación en Dispositivo Real (Recomendado)

### Prueba Manual Completa

**Requisitos:**
- Dispositivo con apps duales configuradas (MIUI, Samsung, etc.)
- Dos instancias de Yape activas

**Pasos:**

1. **Enviar pago desde Yape 1:**
   - Abrir Yape 1
   - Enviar pago de prueba
   - Verificar en logs de Android: `UserId: 0` (o el ID del perfil principal)

2. **Enviar pago desde Yape 2:**
   - Abrir Yape 2 (instancia dual)
   - Enviar pago de prueba
   - Verificar en logs de Android: `UserId: 999` (o el ID del perfil dual)

3. **Verificar en Dashboard:**
   - Debe aparecer 1 nueva instancia (si es la primera vez)
   - O debe asociarse a la instancia existente
   - Las notificaciones deben aparecer en la instancia correcta

**✅ Resultado esperado:**
- ✅ Dos notificaciones en el dashboard
- ✅ Cada una asociada a su instancia correcta
- ✅ `android_user_id` diferente para cada instancia

---

## 📊 Reporte de Estado

### Query Completo de Estado

```sql
-- Reporte completo del estado de apps duales
SELECT 
    'Total de instancias' as metric,
    COUNT(*) as value
FROM app_instances
UNION ALL
SELECT 
    'Instancias asignadas',
    COUNT(*)
FROM app_instances
WHERE instance_label IS NOT NULL
UNION ALL
SELECT 
    'Instancias sin asignar',
    COUNT(*)
FROM app_instances
WHERE instance_label IS NULL
UNION ALL
SELECT 
    'Dispositivos con apps duales',
    COUNT(DISTINCT device_id)
FROM (
    SELECT device_id, package_name
    FROM app_instances
    GROUP BY device_id, package_name
    HAVING COUNT(DISTINCT android_user_id) > 1
) as dual_apps
UNION ALL
SELECT 
    'Notificaciones con instancia',
    COUNT(*)
FROM notifications
WHERE app_instance_id IS NOT NULL
UNION ALL
SELECT 
    'Notificaciones sin instancia (problema)',
    COUNT(*)
FROM notifications
WHERE package_name IS NOT NULL
AND android_user_id IS NOT NULL
AND app_instance_id IS NULL;
```

---

## ⚠️ Señales de Problema

### 🚨 Crítico

- ❌ Constraint único no existe en BD
- ❌ Notificaciones con `android_user_id` pero sin `app_instance_id`
- ❌ Errores en logs al crear AppInstance
- ❌ Tests fallando

### ⚠️ Advertencia

- ⚠️ Muchas instancias sin asignar (>10)
- ⚠️ Dispositivos con más de 5 instancias del mismo package
- ⚠️ `android_user_id` siempre es 0 (posible problema en captura)

### ✅ Normal

- ✅ Instancias sin asignar (se asignan manualmente)
- ✅ `android_user_id` = 0 para perfil principal (normal)
- ✅ `android_user_id` > 0 para perfiles duales (normal)

---

## 📝 Notas

- **Frecuencia recomendada:** Ejecutar validación rápida semanalmente
- **Frecuencia detallada:** Ejecutar validación detallada mensualmente
- **Frecuencia en dispositivo real:** Probar cuando se actualiza Android o se agregan nuevos dispositivos

---

_Última actualización: 2025-01-21_

