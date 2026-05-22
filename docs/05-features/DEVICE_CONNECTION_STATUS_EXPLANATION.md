# 🔌 Explicación: Estado de Conexión del Dispositivo

## ❓ ¿Por qué el dispositivo aparece como "Desconectado"?

### 📋 Resumen

El dispositivo aparece como **"Desconectado"** cuando su `last_heartbeat` (último latido) es **mayor a 5 minutos** desde la última vez que reportó su estado de salud al backend.

---

## 🔍 Cómo Funciona el Sistema

### 1. **DeviceHealthWorker** (App Android)

El `DeviceHealthWorker` es un **Worker de WorkManager** que se ejecuta periódicamente para enviar información de salud del dispositivo al backend:

- **Frecuencia**: Cada 15 minutos (configurable)
- **Datos enviados**:
  - Nivel de batería
  - Estado de optimización de batería
  - Estado de permisos de notificaciones
  - **`last_heartbeat`** (se actualiza automáticamente en el backend)

### 2. **Backend: Cálculo de Estado Online/Offline**

El backend determina si un dispositivo está "online" usando el método `isOnline()` en el modelo `Device`:

```php
public function isOnline(): bool
{
    if (!$this->last_heartbeat) {
        return false;
    }

    // Dispositivo está online si last_heartbeat es menor a 5 minutos
    return $this->last_heartbeat->diffInMinutes(now()) < 5;
}
```

**Regla**: Si `last_heartbeat` es **menor a 5 minutos** → **Online**  
**Regla**: Si `last_heartbeat` es **mayor a 5 minutos** o **null** → **Offline**

---

## 🚨 Causas Comunes de "Desconectado"

### 1. **DeviceHealthWorker No Está Programado**

**Síntoma**: El dispositivo nunca envía health checks.

**Causas posibles**:

- El worker no se programó después de vincular el dispositivo
- El `deviceId` no está guardado en `PreferencesManager`
- WorkManager no está funcionando correctamente

**Solución**:

```kotlin
// Verificar que se llama después de vincular
DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(context)
DeviceHealthWorkerHelper.sendImmediateHealthCheck(context)
```

### 2. **DeviceHealthWorker Está Fallando**

**Síntoma**: El worker se ejecuta pero falla al enviar datos.

**Causas posibles**:

- Error de red (sin conexión a internet)
- Error de autenticación (aunque el endpoint es público)
- Error en el backend (HTTP 500)
- `deviceId` incorrecto o no existe en el backend

**Solución**:

- Revisar logs del `DeviceHealthWorker`
- Verificar que el `deviceId` existe en el backend
- Verificar conectividad a internet

### 3. **El Dispositivo No Tiene `deviceId`**

**Síntoma**: El worker no puede enviar datos porque no tiene `deviceId`.

**Causa**: El dispositivo no se vinculó correctamente o el `deviceId` no se guardó.

**Solución**:

- Verificar que el dispositivo está vinculado
- Verificar que `PreferencesManager.deviceId` tiene un valor
- Re-vincular el dispositivo si es necesario

### 4. **El Worker Se Ejecuta Pero El Backend No Actualiza `last_heartbeat`**

**Síntoma**: El worker se ejecuta exitosamente pero `last_heartbeat` no se actualiza.

**Causa**: Error en el backend al procesar el health check.

**Solución**:

- Revisar logs del backend
- Verificar que el endpoint `/api/devices/{id}/health` funciona correctamente
- Verificar que el `DeviceService.updateHealth()` actualiza `last_heartbeat`

---

## ✅ Verificación y Diagnóstico

### 1. **Verificar en la App Android**

```kotlin
// Verificar que deviceId existe
val deviceId = preferencesManager.deviceId.first()
if (deviceId.isNullOrBlank()) {
    // Problema: No hay deviceId
}

// Verificar que el worker está programado
val workManager = WorkManager.getInstance(context)
val workInfos = workManager.getWorkInfosForUniqueWork(DeviceHealthWorker.WORK_NAME).get()
// Verificar que hay un work programado
```

### 2. **Verificar en el Backend**

```sql
-- Verificar last_heartbeat del dispositivo
SELECT id, name, uuid, last_heartbeat, last_seen_at,
       TIMESTAMPDIFF(MINUTE, last_heartbeat, NOW()) as minutes_since_heartbeat
FROM devices
WHERE uuid = 'TU_UUID_AQUI';

-- Si last_heartbeat es NULL o mayor a 5 minutos, el dispositivo aparecerá como desconectado
```

### 3. **Verificar Logs**

**Android (Logcat)**:

```
DeviceHealthWorker: Starting device health worker...
DeviceHealthWorker: Sending device health data: batteryLevel=85, ...
DeviceHealthWorker: Device health data sent successfully
```

**Backend (Laravel Logs)**:

```
Device health updated: device_id=23, battery_level=85, ...
```

---

## 🔧 Soluciones

### Solución 1: Re-programar el Worker

Si el worker no está programado, programarlo manualmente:

```kotlin
// En la app Android
DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(context)
DeviceHealthWorkerHelper.sendImmediateHealthCheck(context)
```

### Solución 2: Verificar Conectividad

Asegurarse de que:

- El dispositivo tiene conexión a internet
- El backend está accesible
- No hay firewall bloqueando las peticiones

### Solución 3: Re-vincular el Dispositivo

Si el `deviceId` no existe o es incorrecto:

1. Eliminar el dispositivo del backend (opcional)
2. Re-vincular el dispositivo desde la app
3. Verificar que el `deviceId` se guarda correctamente
4. Programar el worker nuevamente

### Solución 4: Verificar Backend

Asegurarse de que:

- El endpoint `/api/devices/{id}/health` está funcionando
- El método `DeviceService.updateHealth()` actualiza `last_heartbeat`
- No hay errores en los logs del backend

---

## 📊 Flujo Completo

```
1. App Android: DeviceHealthWorker se ejecuta cada 15 minutos
   ↓
2. Worker recopila datos de salud (batería, permisos, etc.)
   ↓
3. Worker envía POST /api/devices/{id}/health al backend
   ↓
4. Backend: DeviceService.updateHealth() actualiza:
   - battery_level
   - battery_optimization_disabled
   - notification_permission_enabled
   - last_heartbeat = now() ← CRÍTICO
   ↓
5. Backend: Device.isOnline() verifica:
   - Si last_heartbeat < 5 minutos → Online ✅
   - Si last_heartbeat >= 5 minutos o NULL → Offline ❌
   ↓
6. Dashboard/App Admin muestra estado según isOnline()
```

---

## 🎯 Mejores Prácticas

1. **Siempre programar el worker después de vincular**:

   ```kotlin
   DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(context)
   DeviceHealthWorkerHelper.sendImmediateHealthCheck(context)
   ```

2. **Verificar que el worker se ejecuta**:

   - Revisar logs periódicamente
   - Monitorear `last_heartbeat` en el backend

3. **Manejar errores gracefully**:

   - El worker debe retry en caso de fallo
   - Mostrar advertencias si el dispositivo está desconectado por mucho tiempo

4. **Optimizar frecuencia**:
   - 15 minutos es un buen balance entre batería y precisión
   - Puede ajustarse según necesidades

---

## 📝 Notas Importantes

- **`last_heartbeat`** es diferente de **`last_seen_at`**:

  - `last_heartbeat`: Última vez que el dispositivo reportó su salud (actualizado por DeviceHealthWorker)
  - `last_seen_at`: Última vez que el dispositivo se vio (actualizado en varias operaciones)

- **El estado "Online/Offline" se basa SOLO en `last_heartbeat`**, no en `last_seen_at`.

- **Un dispositivo puede estar "Desconectado" pero seguir recibiendo notificaciones** si el servicio de notificaciones está funcionando pero el DeviceHealthWorker no.

---

**Última actualización**: 2025-12-28  
**Versión**: 1.0

**Referencias relacionadas:**

- `docs/05-features/DEVICE_LINKING.md` - Sistema de vinculación de dispositivos
- `docs/05-features/DEVICE_LINKING_GUIDE.md` - Guía práctica de vinculación
