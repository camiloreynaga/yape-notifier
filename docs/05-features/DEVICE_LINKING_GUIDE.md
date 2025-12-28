# Guía Profesional: Vinculación Adecuada de Dispositivos

## 📋 Resumen Ejecutivo

Esta guía explica **cómo vincular adecuadamente** los dispositivos Android al sistema, asegurando que el proceso se complete correctamente y que los dispositivos queden correctamente asociados a sus comercios.

---

## 🎯 Objetivo del Proceso de Vinculación

**Propósito:** Asociar un dispositivo Android a un `commerce_id` específico para que:
- ✅ Las notificaciones se asocien al comercio correcto
- ✅ El dispositivo aparezca en el dashboard del comercio
- ✅ Las instancias de apps se vinculen al comercio correcto
- ✅ El multi-tenancy funcione correctamente

---

## 🔄 Proceso Completo de Vinculación

### Escenario 1: Usuario Nuevo (Primera Vez) - Automático

**Cuándo usar:** Usuario que se registra por primera vez e instala la app

**Flujo:**
```
1. Usuario se registra (POST /api/register)
   ↓
2. Backend crea automáticamente un Commerce para el usuario
   ↓
3. App Android crea dispositivo (POST /api/devices)
   - Device se crea con user_id y commerce_id automáticamente
   ↓
4. ✅ Dispositivo queda vinculado automáticamente
   - NO necesita código de vinculación
   - Funciona inmediatamente
```

**Validación:**
```sql
-- Verificar que el dispositivo está vinculado
SELECT id, uuid, name, user_id, commerce_id, created_at
FROM devices
WHERE user_id = {user_id}
AND commerce_id IS NOT NULL;
```

---

### Escenario 2: Dispositivo Existente Sin Commerce - Manual

**Cuándo usar:** 
- Dispositivo que ya existe pero `commerce_id = NULL`
- Dispositivo que necesita cambiar de comercio
- Dispositivo de un usuario "captador" que se une a un comercio existente

**Flujo Paso a Paso:**

#### Paso 1: Verificar Estado del Dispositivo

**En Base de Datos:**
```sql
-- Verificar si el dispositivo existe y su estado
SELECT 
    d.id,
    d.uuid,
    d.name,
    d.user_id,
    d.commerce_id,
    u.email,
    u.commerce_id as user_commerce_id,
    CASE 
        WHEN d.commerce_id IS NULL THEN 'Sin vincular'
        WHEN d.commerce_id != u.commerce_id THEN 'Commerce diferente'
        ELSE 'Vinculado correctamente'
    END as estado
FROM devices d
JOIN users u ON u.id = d.user_id
WHERE d.uuid = '{device_uuid}';
```

**En Dashboard Web:**
- Ir a `/devices`
- Buscar el dispositivo
- Verificar columna "Comercio" (debe estar vacía o mostrar "Sin asignar")

**En App Android:**
- Si el dispositivo no está vinculado, la app mostrará automáticamente `LinkDeviceActivity`
- Si no aparece, verificar logs: `adb logcat | grep "LinkDevice"`

---

#### Paso 2: Generar Código de Vinculación

**Desde Dashboard Web (Recomendado):**

1. **Navegar a la página:**
   - Ir a `/devices/add` o `/devices` → Botón "Agregar Dispositivo"

2. **Generar código:**
   - Hacer clic en "Generar Código de Vinculación"
   - El sistema genera un código de 8 caracteres (ej: `ABC12345`)
   - El código expira en **24 horas**

3. **Obtener código:**
   - **Opción A:** Escanear QR mostrado en pantalla
   - **Opción B:** Copiar código manualmente

**Desde API (Para integraciones):**

```http
POST /api/devices/generate-link-code
Authorization: Bearer {admin_token}
Content-Type: application/json

Response:
{
  "message": "Código de vinculación generado exitosamente",
  "code": "ABC12345",
  "expires_at": "2025-01-28T10:30:00Z",
  "link_code": {
    "id": 1,
    "commerce_id": 5,
    "code": "ABC12345",
    "expires_at": "2025-01-28T10:30:00Z",
    "used_at": null,
    "device_id": null
  }
}
```

**Validación del código generado:**
```sql
-- Verificar que el código se creó correctamente
SELECT 
    id,
    commerce_id,
    code,
    expires_at,
    used_at,
    device_id,
    TIMESTAMPDIFF(HOUR, NOW(), expires_at) as horas_restantes
FROM device_link_codes
WHERE code = 'ABC12345'
AND commerce_id = {commerce_id};
```

---

#### Paso 3: Vincular Dispositivo desde App Android

**Proceso en la App:**

1. **Abrir app Android:**
   - Si el dispositivo no está vinculado, la app mostrará automáticamente `LinkDeviceActivity`
   - Si no aparece, ir a Configuración → Vincular Dispositivo

2. **Escanear QR o ingresar código:**
   - **Opción A:** Hacer clic en "Escanear QR" y apuntar a la pantalla del dashboard
   - **Opción B:** Ingresar código manualmente (8 caracteres, mayúsculas)

3. **Validación automática:**
   - La app valida el código en tiempo real (`GET /api/devices/link-code/{code}`)
   - Muestra información del comercio si el código es válido
   - Muestra error si el código es inválido, expirado o ya usado

4. **Confirmar vinculación:**
   - Diálogo muestra: "¿Deseas vincular este dispositivo al comercio: {nombre}?"
   - Hacer clic en "Vincular"

5. **Proceso de vinculación:**
   - La app envía `POST /api/devices/link-by-code` con:
     - `code`: Código escaneado/ingresado
     - `device_uuid`: UUID del dispositivo (guardado en PreferencesManager)

6. **Resultado:**
   - ✅ Si exitoso: App muestra "Dispositivo vinculado exitosamente" y navega a `MainActivity`
   - ❌ Si falla: App muestra mensaje de error específico

---

#### Paso 4: Verificar Vinculación Exitosa

**En Base de Datos:**
```sql
-- Verificar que el dispositivo quedó vinculado
SELECT 
    d.id,
    d.uuid,
    d.name,
    d.commerce_id,
    c.name as commerce_name,
    d.updated_at as vinculado_en
FROM devices d
LEFT JOIN commerces c ON c.id = d.commerce_id
WHERE d.uuid = '{device_uuid}';

-- Verificar que el código se marcó como usado
SELECT 
    id,
    code,
    commerce_id,
    device_id,
    used_at,
    expires_at
FROM device_link_codes
WHERE code = 'ABC12345';
```

**En Dashboard Web:**
- Ir a `/devices`
- El dispositivo debe aparecer con el nombre del comercio en la columna "Comercio"
- El estado debe ser "Activo"

**En App Android:**
- La app debe navegar automáticamente a `MainActivity`
- No debe mostrar más la pantalla de vinculación
- Las notificaciones deben funcionar normalmente

**En Logs del Backend:**
```bash
# Buscar logs de vinculación exitosa
grep "Device linked to commerce" storage/logs/laravel.log | tail -5

# Debe mostrar:
# Device linked to commerce via code: device_id=X, commerce_id=Y, code=ABC12345
```

---

## ✅ Checklist de Vinculación Correcta

### Pre-requisitos

- [ ] Usuario tiene cuenta creada y está autenticado
- [ ] Usuario tiene `commerce_id` asignado (o se creará uno)
- [ ] Dispositivo existe en la base de datos (creado con `POST /api/devices`)
- [ ] Dispositivo tiene `user_id` correcto (pertenece al usuario autenticado)

### Generación de Código

- [ ] Admin genera código desde Dashboard Web o API
- [ ] Código tiene formato correcto (8 caracteres alfanuméricos)
- [ ] Código tiene `commerce_id` asignado
- [ ] Código expira en 24 horas (`expires_at` configurado)
- [ ] Código no ha sido usado antes (`used_at = NULL`)

### Vinculación

- [ ] Usuario escanea QR o ingresa código en app Android
- [ ] App valida código (`GET /api/devices/link-code/{code}`)
- [ ] Validación muestra información correcta del comercio
- [ ] Usuario confirma vinculación
- [ ] App envía `POST /api/devices/link-by-code` con código y `device_uuid`
- [ ] Backend actualiza `Device.commerce_id`
- [ ] Backend marca código como usado (`used_at = now()`)
- [ ] Backend asigna `DeviceLinkCode.device_id`

### Verificación Post-Vinculación

- [ ] `Device.commerce_id` no es NULL
- [ ] `Device.commerce_id` coincide con el del código
- [ ] `DeviceLinkCode.used_at` no es NULL
- [ ] `DeviceLinkCode.device_id` está asignado
- [ ] App Android no muestra más pantalla de vinculación
- [ ] Dashboard Web muestra dispositivo vinculado
- [ ] Notificaciones se asocian correctamente al comercio

---

## 🔍 Validaciones y Verificaciones

### Validación 1: Estado del Dispositivo

```sql
-- Verificar estado completo del dispositivo
SELECT 
    d.id,
    d.uuid,
    d.name,
    d.user_id,
    d.commerce_id,
    u.email,
    u.commerce_id as user_commerce_id,
    CASE 
        WHEN d.commerce_id IS NULL THEN '❌ Sin vincular'
        WHEN d.commerce_id != u.commerce_id THEN '⚠️ Commerce diferente al usuario'
        WHEN d.commerce_id = u.commerce_id THEN '✅ Vinculado correctamente'
    END as estado,
    d.created_at,
    d.updated_at
FROM devices d
JOIN users u ON u.id = d.user_id
WHERE d.uuid = '{device_uuid}';
```

**Resultado esperado:**
- `estado = '✅ Vinculado correctamente'`
- `commerce_id` no es NULL
- `commerce_id` coincide con `user_commerce_id` (o es intencional si es captador)

---

### Validación 2: Código de Vinculación

```sql
-- Verificar estado del código
SELECT 
    dlc.id,
    dlc.code,
    dlc.commerce_id,
    c.name as commerce_name,
    dlc.device_id,
    d.name as device_name,
    dlc.expires_at,
    dlc.used_at,
    CASE 
        WHEN dlc.used_at IS NOT NULL THEN '✅ Usado'
        WHEN dlc.expires_at < NOW() THEN '❌ Expirado'
        WHEN dlc.expires_at >= NOW() THEN '✅ Válido'
    END as estado,
    TIMESTAMPDIFF(HOUR, NOW(), dlc.expires_at) as horas_restantes
FROM device_link_codes dlc
LEFT JOIN commerces c ON c.id = dlc.commerce_id
LEFT JOIN devices d ON d.id = dlc.device_id
WHERE dlc.code = 'ABC12345';
```

**Resultado esperado (antes de usar):**
- `estado = '✅ Válido'`
- `used_at` es NULL
- `horas_restantes > 0`

**Resultado esperado (después de usar):**
- `estado = '✅ Usado'`
- `used_at` no es NULL
- `device_id` está asignado

---

### Validación 3: Integridad de Datos

```sql
-- Verificar que no hay dispositivos huérfanos
SELECT COUNT(*) as dispositivos_sin_commerce
FROM devices
WHERE commerce_id IS NULL
AND created_at < DATE_SUB(NOW(), INTERVAL 1 DAY);

-- Verificar que no hay códigos huérfanos
SELECT COUNT(*) as codigos_sin_usar_expirados
FROM device_link_codes
WHERE used_at IS NULL
AND expires_at < NOW()
AND expires_at < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- Verificar que todos los dispositivos tienen user_id
SELECT COUNT(*) as dispositivos_sin_usuario
FROM devices
WHERE user_id IS NULL;
```

**Resultado esperado:**
- `dispositivos_sin_commerce = 0` (o solo dispositivos recién creados)
- `codigos_sin_usar_expirados = 0` (o se limpian automáticamente)
- `dispositivos_sin_usuario = 0`

---

## 🚨 Problemas Comunes y Soluciones

### Problema 1: "Dispositivo no encontrado"

**Síntomas:**
- App muestra: "Dispositivo no encontrado"
- Backend retorna error 404

**Causas posibles:**
1. Dispositivo no existe en la base de datos
2. UUID del dispositivo no coincide
3. Dispositivo pertenece a otro usuario

**Solución:**
```sql
-- 1. Verificar si el dispositivo existe
SELECT * FROM devices WHERE uuid = '{device_uuid}';

-- 2. Si no existe, crear dispositivo primero
-- Desde la app Android, el dispositivo se crea automáticamente al registrarse
-- O manualmente: POST /api/devices

-- 3. Verificar que el user_id coincide
SELECT d.*, u.email 
FROM devices d
JOIN users u ON u.id = d.user_id
WHERE d.uuid = '{device_uuid}';
```

---

### Problema 2: "Código no encontrado" o "Código inválido"

**Síntomas:**
- App muestra: "Código no encontrado"
- Validación falla

**Causas posibles:**
1. Código mal escrito (mayúsculas/minúsculas)
2. Código expirado (24 horas)
3. Código ya usado

**Solución:**
```sql
-- 1. Verificar estado del código
SELECT * FROM device_link_codes WHERE code = 'ABC12345';

-- 2. Si está expirado, generar nuevo código
-- Desde Dashboard: /devices/add → Generar nuevo código

-- 3. Si está usado, verificar dispositivo vinculado
SELECT d.* FROM devices d
JOIN device_link_codes dlc ON dlc.device_id = d.id
WHERE dlc.code = 'ABC12345';
```

---

### Problema 3: "El dispositivo ya pertenece a otro negocio"

**Síntomas:**
- Backend rechaza vinculación
- Mensaje: "El dispositivo ya pertenece a otro negocio"

**Causa:**
- Dispositivo tiene `commerce_id` diferente al del código

**Solución:**
```sql
-- 1. Verificar commerce_id actual
SELECT id, uuid, name, commerce_id 
FROM devices 
WHERE uuid = '{device_uuid}';

-- 2. Si es intencional cambiar de comercio:
-- Opción A: Eliminar dispositivo del comercio anterior (solo admin)
-- Opción B: Crear nuevo dispositivo con nuevo UUID

-- 3. Si NO es intencional:
-- Verificar que el código pertenece al comercio correcto
SELECT dlc.*, c.name as commerce_name
FROM device_link_codes dlc
JOIN commerces c ON c.id = dlc.commerce_id
WHERE dlc.code = 'ABC12345';
```

---

### Problema 4: "App sigue mostrando pantalla de vinculación"

**Síntomas:**
- Dispositivo está vinculado en BD pero app sigue pidiendo código
- App no navega a MainActivity

**Causas posibles:**
1. Cache de la app no actualizado
2. DeviceId local no sincronizado
3. App no verifica correctamente el estado

**Solución:**
```kotlin
// En Android, verificar que el dispositivo se actualizó correctamente
// LinkDeviceActivity debe verificar en backend al iniciar:

// 1. Verificar por deviceId local
val deviceId = preferencesManager.deviceId.first()
if (deviceId != null) {
    val currentDevice = apiService.getDevice(deviceId)
    if (currentDevice?.commerce_id != null) {
        // Dispositivo ya vinculado, navegar a MainActivity
        navigateToMain()
    }
}

// 2. Verificar por UUID
val deviceUuid = preferencesManager.deviceUuid.first()
val devices = apiService.getDevices()
val linkedDevice = devices.find { it.uuid == deviceUuid && it.commerce_id != null }
if (linkedDevice != null) {
    // Dispositivo vinculado, actualizar deviceId local y navegar
    preferencesManager.saveDeviceId(linkedDevice.id)
    navigateToMain()
}
```

**Solución rápida:**
- Cerrar completamente la app
- Abrir de nuevo
- La app debe verificar estado en backend al iniciar

---

## 📊 Mejores Prácticas

### 1. **Generar Códigos con Tiempo Limitado**

✅ **Correcto:**
- Generar código justo antes de vincular
- Código expira en 24 horas
- Limpiar códigos expirados automáticamente

❌ **Incorrecto:**
- Generar códigos con mucha anticipación
- Códigos que nunca expiran
- Códigos que quedan huérfanos

---

### 2. **Validar Antes de Vincular**

✅ **Correcto:**
```kotlin
// Validar código antes de vincular
fun validateCode(code: String) {
    val validation = apiService.validateLinkCode(code)
    if (validation.valid) {
        // Mostrar información del comercio
        showCommerceInfo(validation.commerce)
        // Permitir confirmación
        enableLinkButton()
    } else {
        showError(validation.message)
    }
}
```

❌ **Incorrecto:**
- Vincular sin validar
- No mostrar información del comercio
- No permitir confirmación del usuario

---

### 3. **Manejo de Errores**

✅ **Correcto:**
```kotlin
try {
    val response = apiService.linkDeviceByCode(request)
    if (response.isSuccessful) {
        // Verificar que el dispositivo quedó vinculado
        val device = response.body()?.device
        if (device?.commerce_id != null) {
            // Éxito
            navigateToMain()
        } else {
            // Error: dispositivo no vinculado
            showError("Error al vincular dispositivo")
        }
    } else {
        // Error del servidor
        val errorMessage = response.errorBody()?.string()
        showError(errorMessage ?: "Error desconocido")
    }
} catch (e: Exception) {
    // Error de red o excepción
    showError("Error de conexión: ${e.message}")
}
```

❌ **Incorrecto:**
- No manejar errores
- Asumir éxito sin verificar
- No mostrar mensajes de error al usuario

---

### 4. **Logging y Auditoría**

✅ **Correcto:**
```php
// Backend: Log detallado
Log::info('Device linked to commerce via code', [
    'device_id' => $device->id,
    'device_uuid' => $deviceUuid,
    'commerce_id' => $linkCode->commerce_id,
    'code' => $code,
    'user_id' => $user->id,
    'ip_address' => $request->ip(),
]);
```

❌ **Incorrecto:**
- No registrar eventos de vinculación
- No incluir información suficiente en logs
- No auditar cambios de `commerce_id`

---

## 🔐 Seguridad

### Validaciones de Seguridad

1. **Autenticación:**
   - ✅ Usuario debe estar autenticado para vincular
   - ✅ Solo puede vincular sus propios dispositivos
   - ✅ UUID debe pertenecer al usuario autenticado

2. **Código:**
   - ✅ Código expira en 24 horas
   - ✅ Código solo se puede usar una vez
   - ✅ Código es case-insensitive pero se normaliza

3. **Dispositivo:**
   - ✅ UUID debe tener formato válido
   - ✅ Dispositivo debe existir en BD
   - ✅ Dispositivo debe pertenecer al usuario

4. **Comercio:**
   - ✅ Si dispositivo ya tiene `commerce_id` diferente, rechaza
   - ✅ Previene que dispositivo pertenezca a múltiples comercios

---

## 📝 Resumen de Endpoints

### Generar Código
```http
POST /api/devices/generate-link-code
Authorization: Bearer {token}
```

### Validar Código (Público)
```http
GET /api/devices/link-code/{code}
```

### Vincular Dispositivo
```http
POST /api/devices/link-by-code
Authorization: Bearer {token}
Content-Type: application/json

{
  "code": "ABC12345",
  "device_uuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## ✅ Conclusión

Para vincular adecuadamente un dispositivo:

1. ✅ **Verificar estado** del dispositivo en BD
2. ✅ **Generar código** desde Dashboard o API
3. ✅ **Escanear/ingresar código** en app Android
4. ✅ **Validar código** antes de vincular
5. ✅ **Confirmar vinculación** con información del comercio
6. ✅ **Verificar resultado** en BD, Dashboard y App
7. ✅ **Manejar errores** apropiadamente
8. ✅ **Registrar eventos** en logs

**Resultado esperado:**
- `Device.commerce_id` asignado correctamente
- `DeviceLinkCode.used_at` marcado
- App Android funciona normalmente
- Dashboard muestra dispositivo vinculado
- Notificaciones se asocian al comercio correcto

---

_Última actualización: 2025-01-21_

