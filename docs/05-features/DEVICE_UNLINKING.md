# 🔓 Desvinculación de Dispositivos

> **Referencias relacionadas:**
> - [DEVICE_LIFECYCLE.md](DEVICE_LIFECYCLE.md) - Ciclo de vida completo de dispositivos
> - [DEVICE_LINKING_GUIDE.md](DEVICE_LINKING_GUIDE.md) - Guía de vinculación de dispositivos
> - [CLEAN_DEVICES.md](../06-operations/CLEAN_DEVICES.md) - Limpieza completa de dispositivos (operación avanzada)

Guía completa para desvincular dispositivos de comercios y permitir re-vinculación a otros comercios.

---

## 📋 Tabla de Contenidos

1. [Descripción General](#descripción-general)
2. [Casos de Uso](#casos-de-uso)
3. [Endpoint API](#endpoint-api)
4. [Flujo Completo](#flujo-completo)
5. [Seguridad](#seguridad)
6. [Ejemplos](#ejemplos)

---

## 📖 Descripción General

La funcionalidad de desvinculación permite que un dispositivo se desconecte de su comercio actual y pueda vincularse a otro comercio diferente.

### Características

- ✅ Limpia `commerce_id` del dispositivo
- ✅ Preserva `device_uuid` y `user_id` (trazabilidad)
- ✅ Mantiene historial de notificaciones
- ✅ Permite re-vinculación a otro comercio
- ✅ Requiere autenticación y propiedad del dispositivo
- ✅ Registra operación en logs (auditoría)

### Qué NO hace

- ❌ No elimina el dispositivo de la base de datos
- ❌ No elimina notificaciones históricas
- ❌ No limpia el UUID del dispositivo
- ❌ No cierra la sesión del usuario

---

## 🎯 Casos de Uso

### 1. Transferir Dispositivo a Otro Comercio

**Escenario:** Un dispositivo capturador se usaba en Comercio A y ahora se necesita en Comercio B.

**Pasos:**
1. Desvincular dispositivo de Comercio A
2. Generar código QR en Comercio B
3. Escanear QR con el dispositivo
4. Dispositivo ahora captura para Comercio B

### 2. Corregir Vinculación Incorrecta

**Escenario:** El dispositivo se vinculó al comercio equivocado por error.

**Pasos:**
1. Desvincular dispositivo del comercio incorrecto
2. Vincular al comercio correcto con QR

### 3. Remover Dispositivo de Flota

**Escenario:** Un comercio ya no quiere que un dispositivo capture notificaciones.

**Pasos:**
1. Desvincular dispositivo
2. El dispositivo queda sin comercio asignado
3. No puede enviar notificaciones hasta que se vincule nuevamente

---

## 🔌 Endpoint API

### POST /api/devices/{id}/unlink

Desvincula un dispositivo de su comercio actual.

#### Autenticación

**Requerida:** Sí (Sanctum token)

#### Parámetros de Ruta

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `id` | integer | ID del dispositivo a desvincular |

#### Headers

```http
Authorization: Bearer {token}
Content-Type: application/json
```

#### Respuesta Exitosa (200)

```json
{
  "message": "Dispositivo desvinculado exitosamente. Ahora puede vincularse a otro negocio.",
  "device": {
    "id": 10,
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "user_id": 3,
    "commerce_id": null,
    "name": "Samsung Galaxy A52",
    "platform": "android",
    "is_active": true,
    "last_seen_at": "2026-01-05T16:30:00Z",
    "created_at": "2026-01-01T10:00:00Z",
    "updated_at": "2026-01-05T16:30:00Z"
  }
}
```

#### Errores

**400 - Dispositivo ya desvinculado:**
```json
{
  "message": "El dispositivo ya está desvinculado",
  "device": { ... }
}
```

**404 - Dispositivo no encontrado:**
```json
{
  "message": "No query results for model [App\\Models\\Device] {id}"
}
```

**401 - No autenticado:**
```json
{
  "message": "Unauthenticated."
}
```

**403 - No autorizado (dispositivo de otro usuario):**
```json
{
  "message": "This action is unauthorized."
}
```

---

## 🔄 Flujo Completo

### Flujo de Cambio de Comercio

```
┌─────────────────────────────────────────────────────────────┐
│ ESTADO INICIAL                                              │
│ Device: uuid=ABC, user_id=5, commerce_id=1 (Comercio A)    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 1: Desvincular                                         │
│ POST /api/devices/10/unlink                                 │
│ Authorization: Bearer {token_usuario_5}                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ RESULTADO: Dispositivo desvinculado                         │
│ Device: uuid=ABC, user_id=5, commerce_id=null              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 2: Admin de Comercio B genera QR                      │
│ POST /api/devices/generate-link-code                        │
│ Response: { code: "XYZ12345" }                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ PASO 3: Usuario escanea QR en app Android                  │
│ POST /api/devices/link-by-code                              │
│ Body: { code: "XYZ12345", device_uuid: "ABC" }             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ RESULTADO FINAL                                             │
│ Device: uuid=ABC, user_id=5, commerce_id=2 (Comercio B)    │
│ ✅ Dispositivo ahora captura para Comercio B               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔒 Seguridad

### Validaciones Implementadas

1. **Autenticación requerida:**
   - Solo usuarios autenticados pueden desvincular dispositivos
   - Token Sanctum válido obligatorio

2. **Propiedad del dispositivo:**
   - Solo el propietario del dispositivo puede desvincularlo
   - Verificado via `$request->user()->devices()->findOrFail($id)`

3. **Estado del dispositivo:**
   - Verifica que el dispositivo tenga `commerce_id` antes de desvincular
   - Retorna error si ya está desvinculado

4. **Auditoría:**
   - Todas las desvinculaciones se registran en logs
   - Incluye: device_id, uuid, old_commerce_id, user_id, timestamp

### Logs de Auditoría

```php
Log::info('Device unlinked from commerce', [
    'device_id' => 10,
    'device_uuid' => '550e8400-e29b-41d4-a716-446655440000',
    'old_commerce_id' => 1,
    'user_id' => 5,
]);
```

---

## 💡 Ejemplos

### Ejemplo 1: Desvincular desde cURL

```bash
curl -X POST https://api.example.com/api/devices/10/unlink \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc..." \
  -H "Content-Type: application/json"
```

### Ejemplo 2: Desvincular desde JavaScript

```javascript
const unlinkDevice = async (deviceId, token) => {
  const response = await fetch(`https://api.example.com/api/devices/${deviceId}/unlink`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  if (!response.ok) {
    throw new Error('Error al desvincular dispositivo');
  }
  
  return await response.json();
};

// Uso
try {
  const result = await unlinkDevice(10, userToken);
  console.log(result.message);
  console.log('Nuevo commerce_id:', result.device.commerce_id); // null
} catch (error) {
  console.error(error);
}
```

### Ejemplo 3: Flujo Completo en Android (Kotlin)

```kotlin
// 1. Desvincular dispositivo
suspend fun unlinkDevice(deviceId: Long): Result<Device> {
    return try {
        val response = apiService.unlinkDevice(deviceId)
        if (response.isSuccessful) {
            Result.success(response.body()!!.device)
        } else {
            Result.failure(Exception(response.errorBody()?.string()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// 2. Vincular a nuevo comercio con QR
suspend fun linkToNewCommerce(code: String, deviceUuid: String): Result<Device> {
    return try {
        val request = LinkDeviceRequest(code, deviceUuid)
        val response = apiService.linkDeviceByCode(request)
        if (response.isSuccessful) {
            Result.success(response.body()!!.device)
        } else {
            Result.failure(Exception(response.errorBody()?.string()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Uso en ViewModel
viewModelScope.launch {
    // Paso 1: Desvincular
    val unlinkResult = unlinkDevice(deviceId)
    if (unlinkResult.isSuccess) {
        // Paso 2: Mostrar pantalla de escaneo QR
        navigateToQRScanner()
    } else {
        showError(unlinkResult.exceptionOrNull()?.message)
    }
}
```

---

## ✅ Checklist de Implementación

### Backend (API)
- [x] Método `unlinkDevice()` en `DeviceService`
- [x] Método `unlink()` en `DeviceController`
- [x] Ruta `POST /api/devices/{id}/unlink` en `api.php`
- [x] Validación de autenticación y propiedad
- [x] Logging de auditoría
- [x] Manejo de errores

### Frontend (Android)
- [ ] Agregar método `unlinkDevice()` en `ApiService`
- [ ] Implementar UI para desvincular en `MainActivity`
- [ ] Mostrar diálogo de confirmación
- [ ] Navegar a `LinkDeviceActivity` después de desvincular
- [ ] Mostrar mensajes de éxito/error

### Frontend (Web Dashboard)
- [ ] Agregar botón "Desvincular" en lista de dispositivos
- [ ] Implementar confirmación de desvinculación
- [ ] Actualizar lista después de desvincular
- [ ] Mostrar estado del dispositivo (vinculado/desvinculado)

---

## 🔍 Verificación

### Verificar desvinculación en base de datos

```sql
-- Ver dispositivos desvinculados
SELECT id, uuid, user_id, commerce_id, name, updated_at
FROM devices
WHERE commerce_id IS NULL
ORDER BY updated_at DESC;

-- Ver historial de un dispositivo específico
SELECT id, uuid, user_id, commerce_id, name, created_at, updated_at
FROM devices
WHERE uuid = '550e8400-e29b-41d4-a716-446655440000';
```

### Verificar en logs

```bash
# Ver logs de desvinculación
docker compose exec php-fpm grep "Device unlinked from commerce" /var/www/storage/logs/laravel.log | tail -10

# Ver detalles de una desvinculación específica
docker compose exec php-fpm grep "device_id.*10" /var/www/storage/logs/laravel.log | grep "unlinked"
```

---

## 🚨 Troubleshooting

### Error: "El dispositivo ya está desvinculado"

**Causa:** El dispositivo ya tiene `commerce_id = null`.

**Solución:** No es necesario desvincular. El dispositivo ya puede vincularse a cualquier comercio.

### Error: "This action is unauthorized"

**Causa:** El usuario no es propietario del dispositivo.

**Solución:** Solo el usuario que registró el dispositivo puede desvincularlo.

### Error: "No query results for model"

**Causa:** El ID del dispositivo no existe o no pertenece al usuario.

**Solución:** Verificar que el ID sea correcto y que el dispositivo pertenezca al usuario autenticado.

---

## 📚 Referencias

- [Device Linking Guide](./DEVICE_LINKING_GUIDE.md)
- [Device Linking Architecture](../03-architecture/DEVICE_LINKING_ARCHITECTURE.md)
- [API Routes](../../apps/api/routes/api.php)
- [DeviceService](../../apps/api/app/Services/DeviceService.php)



