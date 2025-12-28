# Vinculación de Dispositivos

**Estado:** ✅ IMPLEMENTADO

**Descripción:** Sistema completo de registro y vinculación de dispositivos Android a comercios mediante códigos QR o códigos manuales.

---

## 📋 Resumen Ejecutivo

El sistema implementa un **enfoque flexible y profesional** para la vinculación de dispositivos:

1. **Registro Inicial de Dispositivo** (POST `/api/devices`): Crea el dispositivo en la base de datos (opcional, puede crearse automáticamente)
2. **Vinculación a Comercio** (POST `/api/devices/link-by-code`): Asocia el dispositivo a un `commerce_id` usando un código QR/número

**IMPORTANTE:** 
- El sistema implementa un **patrón find-or-create**: si el dispositivo no existe, se crea automáticamente durante la vinculación
- La autenticación es **opcional**: el código QR es el mecanismo de autorización principal
- Un dispositivo puede existir en la base de datos pero **NO estar vinculado a ningún comercio** (`commerce_id = null`). En este caso, la app Android mostrará la pantalla de vinculación.

**Para más detalles sobre la arquitectura, ver:** `docs/05-features/DEVICE_LINKING_ARCHITECTURE.md`

---

## 🔄 Flujo Completo del Proceso

### Escenario 1: Usuario Nuevo (Primera Vez)

```
1. Usuario instala la app Android
   ↓
2. Usuario se registra (POST /api/register)
   - Se crea el usuario
   - Se crea automáticamente un Commerce para el usuario
   - Usuario recibe token de autenticación
   ↓
3. App Android crea dispositivo (POST /api/devices)
   - Se crea Device con user_id y commerce_id (del usuario)
   - Device queda vinculado automáticamente
   ↓
4. App funciona normalmente (no necesita vinculación)
```

### Escenario 2: Dispositivo Existente Sin Commerce (Tu Caso)

```
1. Dispositivo ya existe en la base de datos
   - Device tiene user_id pero commerce_id = null
   - O Device tiene commerce_id diferente al que necesita
   ↓
2. App Android detecta que no tiene commerce_id válido
   - Muestra pantalla LinkDeviceActivity
   - Pide escanear QR o ingresar código
   ↓
3. Admin genera código de vinculación (Dashboard Web o Admin App)
   - POST /api/devices/generate-link-code
   - Se crea DeviceLinkCode con código de 8 caracteres
   - Código expira en 24 horas
   ↓
4. Usuario escanea QR o ingresa código en app Android
   - GET /api/devices/link-code/{code} (validación)
   - POST /api/devices/link-by-code (vinculación)
   ↓
5. Dispositivo queda vinculado al commerce
   - Device.commerce_id se actualiza
   - DeviceLinkCode se marca como usado
   ↓
6. App funciona normalmente
```

---

## 📱 Proceso Detallado: Lado Android

### 1. Detección de Necesidad de Vinculación

**Ubicación:** `LoginActivity.kt` o `MainActivity.kt`

La app verifica si el dispositivo necesita vinculación:

```kotlin
// Pseudocódigo
if (user.commerce_id == null || device.commerce_id == null) {
    // Navegar a LinkDeviceActivity
    startActivity(Intent(this, LinkDeviceActivity::class.java))
}
```

### 2. Pantalla de Vinculación (LinkDeviceActivity)

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/ui/LinkDeviceActivity.kt`

**Funcionalidades:**

1. **Escaneo de QR:**
   - Botón "Escanear QR"
   - Solicita permiso de cámara
   - Usa librería `barcodescanner` para escanear
   - El QR contiene el código de 8 caracteres (ej: "ABC12345")

2. **Ingreso Manual:**
   - Campo de texto para ingresar código
   - Validación automática cuando se ingresan 8 caracteres
   - Formato: 8 caracteres alfanuméricos (mayúsculas)

3. **Validación en Tiempo Real:**
   - Al escanear o ingresar código, llama a `GET /api/devices/link-code/{code}`
   - Muestra información del comercio si el código es válido
   - Muestra error si el código es inválido, expirado o ya usado

4. **Confirmación:**
   - Diálogo de confirmación mostrando nombre del comercio
   - Botón "Vincular" para confirmar

5. **Vinculación:**
   - Llama a `POST /api/devices/link-by-code` con:
     - `code`: Código escaneado/ingresado
     - `device_uuid`: UUID del dispositivo (guardado en PreferencesManager)

### 3. ViewModel (LinkDeviceViewModel)

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/LinkDeviceViewModel.kt`

**Estados:**

- **ValidationState:**
  - `Idle`: Sin validación
  - `Validating`: Validando código
  - `Valid(commerce)`: Código válido, muestra info del comercio
  - `Invalid(message)`: Código inválido, muestra mensaje de error

- **LinkState:**
  - `Idle`: Sin vinculación
  - `Linking`: Vinculando dispositivo
  - `Success(message)`: Vinculación exitosa
  - `Error(message)`: Error en vinculación

**Métodos:**

```kotlin
// Validar código sin vincular
fun validateCode(code: String)

// Vincular dispositivo usando código
fun linkDevice(code: String)
```

---

## 🖥️ Proceso Detallado: Lado Backend

### 1. Generación de Código de Vinculación

**Endpoint:** `POST /api/devices/generate-link-code`  
**Autenticación:** Requerida (Sanctum)  
**Rol:** Admin (opcional, puede ser configurado)

**Controlador:** `DeviceLinkController::generateLinkCode()`

**Proceso:**

1. Verifica que el usuario tenga `commerce_id`
2. Verifica que el usuario sea admin (opcional)
3. Llama a `DeviceLinkService::generateLinkCode($commerceId)`
4. Genera código único de 8 caracteres alfanuméricos (mayúsculas)
5. Crea registro en tabla `device_link_codes`:
   - `commerce_id`: ID del comercio
   - `code`: Código generado (ej: "ABC12345")
   - `expires_at`: 24 horas desde ahora
   - `used_at`: null (se marca cuando se usa)
   - `device_id`: null (se asigna cuando se vincula)

**Respuesta:**

```json
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

### 2. Validación de Código (Público)

**Endpoint:** `GET /api/devices/link-code/{code}`  
**Autenticación:** NO requerida (público)

**Controlador:** `DeviceLinkController::validateLinkCode()`

**Proceso:**

1. Busca `DeviceLinkCode` por código (case-insensitive)
2. Verifica que el código exista
3. Verifica que no esté usado (`used_at == null`)
4. Verifica que no esté expirado (`expires_at > now()`)
5. Si es válido, devuelve información del comercio

**Respuesta (Válido):**

```json
{
  "valid": true,
  "commerce_id": 5,
  "commerce": {
    "id": 5,
    "name": "Mi Negocio",
    "created_at": "2025-01-20T10:00:00Z"
  },
  "message": "Código válido"
}
```

**Respuesta (Inválido):**

```json
{
  "valid": false,
  "commerce_id": null,
  "message": "Código no encontrado" // o "Código ya utilizado" o "Código expirado"
}
```

### 3. Vinculación de Dispositivo

**Endpoint:** `POST /api/devices/link-by-code`  
**Autenticación:** Requerida (Sanctum)

**Request Body:**

```json
{
  "code": "ABC12345",
  "device_uuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Controlador:** `DeviceLinkController::linkByCode()`

**Proceso (DeviceLinkService::linkDevice()):**

1. **Validar código:**
   - Llama a `validateCode($code)`
   - Verifica que sea válido, no usado, no expirado

2. **Validar UUID:**
   - Verifica formato UUID válido (regex)

3. **Buscar dispositivo:**
   - Busca `Device` por `uuid` y `user_id`
   - El dispositivo DEBE existir y pertenecer al usuario autenticado

4. **Verificar conflicto:**
   - Si el dispositivo ya tiene `commerce_id` diferente, rechaza
   - Mensaje: "El dispositivo ya pertenece a otro negocio"

5. **Actualizar dispositivo:**
   - Actualiza `Device.commerce_id = linkCode.commerce_id`
   - Guarda cambios

6. **Marcar código como usado:**
   - Actualiza `DeviceLinkCode.used_at = now()`
   - Actualiza `DeviceLinkCode.device_id = device.id`

7. **Logging:**
   - Registra la vinculación exitosa

**Respuesta (Éxito):**

```json
{
  "message": "Dispositivo vinculado exitosamente",
  "device": {
    "id": 10,
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Mi Dispositivo",
    "user_id": 3,
    "commerce_id": 5,
    "is_active": true,
    "created_at": "2025-01-20T10:00:00Z",
    "updated_at": "2025-01-27T15:30:00Z"
  }
}
```

**Respuesta (Error):**

```json
{
  "message": "Dispositivo no encontrado" // o "Código inválido" o "El dispositivo ya pertenece a otro negocio"
}
```

---

## 🔍 Modelos de Base de Datos

### Tabla: `devices`

```sql
CREATE TABLE devices (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,           -- Usuario propietario
    commerce_id BIGINT NULL,           -- Comercio al que pertenece (NULL = sin vincular)
    uuid VARCHAR(36) UNIQUE NOT NULL,  -- UUID único del dispositivo
    name VARCHAR(255) NOT NULL,        -- Nombre del dispositivo
    alias VARCHAR(255) NULL,            -- Alias personalizado
    platform VARCHAR(50) DEFAULT 'android',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Estados posibles:**
- `commerce_id = NULL`: Dispositivo sin vincular (necesita código)
- `commerce_id = X`: Dispositivo vinculado al comercio X

### Tabla: `device_link_codes`

```sql
CREATE TABLE device_link_codes (
    id BIGINT PRIMARY KEY,
    commerce_id BIGINT NOT NULL,      -- Comercio que genera el código
    code VARCHAR(8) UNIQUE NOT NULL,  -- Código de 8 caracteres (ej: "ABC12345")
    device_id BIGINT NULL,            -- Dispositivo vinculado (NULL hasta que se use)
    expires_at TIMESTAMP NOT NULL,    -- Expira en 24 horas
    used_at TIMESTAMP NULL,           -- NULL = no usado, TIMESTAMP = usado
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Estados del código:**
- `used_at = NULL` y `expires_at > now()`: **Válido** (puede usarse)
- `used_at != NULL`: **Usado** (no puede usarse de nuevo)
- `used_at = NULL` y `expires_at <= now()`: **Expirado** (no puede usarse)

---

## 🔐 Seguridad y Validaciones

### Validaciones del Código

1. **Formato:**
   - 8 caracteres alfanuméricos
   - Case-insensitive (se normaliza a mayúsculas)

2. **Unicidad:**
   - Cada código es único en la base de datos
   - Se genera con `Str::random(8)` y verifica unicidad

3. **Expiración:**
   - Código expira en 24 horas
   - No puede usarse después de expirar

4. **Uso único:**
   - Cada código solo puede usarse una vez
   - Se marca con `used_at` cuando se usa

### Validaciones del Dispositivo

1. **UUID:**
   - Formato UUID válido (regex: `/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i`)
   - Debe existir en la base de datos
   - Debe pertenecer al usuario autenticado

2. **Comercio:**
   - Si el dispositivo ya tiene `commerce_id` diferente, rechaza vinculación
   - Previene que un dispositivo pertenezca a múltiples comercios

3. **Autenticación:**
   - El usuario debe estar autenticado para vincular
   - Solo puede vincular sus propios dispositivos

---

## 📊 Flujo de Datos Completo

### Diagrama de Secuencia

```
[Admin/Dashboard]          [Backend API]              [App Android]
      |                            |                         |
      |-- POST /generate-link-code |                         |
      |                            |-- Crea DeviceLinkCode   |
      |<-- code: "ABC12345" -------|                         |
      |                            |                         |
      |-- Muestra QR con código ---|                         |
      |                            |                         |
      |                            |<-- GET /link-code/ABC12345
      |                            |-- Valida código         |
      |                            |-- Devuelve commerce info|
      |                            |------------------------>|
      |                            |                         |
      |                            |<-- POST /link-by-code   |
      |                            |   {code, device_uuid}   |
      |                            |-- Busca Device          |
      |                            |-- Actualiza commerce_id  |
      |                            |-- Marca código usado    |
      |                            |------------------------>|
      |                            |                         |
```

---

## 🐛 Problemas Comunes y Soluciones

### Problema 1: "Dispositivo aparece en lista pero pide código"

**Causa:**
- El dispositivo existe en la base de datos pero `commerce_id = NULL`
- O el `commerce_id` del dispositivo no coincide con el del usuario

**Solución:**
1. Verificar en base de datos: `SELECT * FROM devices WHERE uuid = '...'`
2. Si `commerce_id IS NULL`, el dispositivo necesita vinculación
3. Generar código desde Dashboard Web o Admin App
4. Escanear/ingresar código en la app Android

### Problema 2: "Código no encontrado"

**Causas posibles:**
- Código mal escrito (verificar mayúsculas/minúsculas)
- Código expirado (24 horas)
- Código ya usado

**Solución:**
- Generar nuevo código desde Dashboard
- Verificar que el código no haya expirado
- Verificar que el código no haya sido usado antes

### Problema 3: "Dispositivo no encontrado"

**Causa:**
- El dispositivo no existe en la base de datos
- O el UUID no coincide con el del usuario autenticado

**Solución:**
1. Verificar que el dispositivo se haya creado correctamente
2. Verificar que el `device_uuid` en la app coincida con el de la BD
3. Si no existe, crear dispositivo primero: `POST /api/devices`

### Problema 4: "El dispositivo ya pertenece a otro negocio"

**Causa:**
- El dispositivo ya tiene `commerce_id` diferente

**Solución:**
- Si es intencional, primero eliminar el dispositivo del comercio anterior
- O crear un nuevo dispositivo con nuevo UUID

---

## 📝 Endpoints API Resumen

### Generar Código de Vinculación

```http
POST /api/devices/generate-link-code
Authorization: Bearer {token}
Content-Type: application/json

Response:
{
  "message": "Código de vinculación generado exitosamente",
  "code": "ABC12345",
  "expires_at": "2025-01-28T10:30:00Z",
  "link_code": { ... }
}
```

### Validar Código (Público)

```http
GET /api/devices/link-code/{code}

Response (Válido):
{
  "valid": true,
  "commerce_id": 5,
  "commerce": { "id": 5, "name": "Mi Negocio" },
  "message": "Código válido"
}

Response (Inválido):
{
  "valid": false,
  "commerce_id": null,
  "message": "Código no encontrado" // o "Código ya utilizado" o "Código expirado"
}
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

Response:
{
  "message": "Dispositivo vinculado exitosamente",
  "device": { ... }
}
```

### Listar Códigos Activos

```http
GET /api/devices/link-codes
Authorization: Bearer {token}

Response:
{
  "link_codes": [
    {
      "id": 1,
      "code": "ABC12345",
      "commerce_id": 5,
      "expires_at": "2025-01-28T10:30:00Z",
      "used_at": null,
      "device_id": null
    }
  ]
}
```

---

## 🔄 Flujo de Actualización de la App

### ¿Por qué ahora pide código si antes no?

**Razón:** La app se actualizó y ahora verifica correctamente el `commerce_id` del dispositivo.

**Antes (Versión antigua):**
- La app no verificaba `commerce_id`
- Funcionaba aunque el dispositivo no estuviera vinculado
- Las notificaciones se creaban pero sin `commerce_id` válido

**Ahora (Versión nueva):**
- La app verifica `commerce_id` al iniciar
- Si `commerce_id` es NULL, muestra pantalla de vinculación
- Esto asegura que todas las notificaciones tengan `commerce_id` correcto

**Solución:**
1. Generar código desde Dashboard Web (como admin)
2. Escanear/ingresar código en la app Android
3. El dispositivo quedará vinculado y funcionará normalmente

---

## ✅ Checklist de Vinculación

Para vincular un dispositivo correctamente:

- [ ] Usuario tiene cuenta y está autenticado
- [ ] Dispositivo existe en la base de datos (creado con POST /api/devices)
- [ ] Admin genera código de vinculación (POST /api/devices/generate-link-code)
- [ ] Código no ha expirado (24 horas)
- [ ] Código no ha sido usado antes
- [ ] Usuario escanea QR o ingresa código en app Android
- [ ] App valida código (GET /api/devices/link-code/{code})
- [ ] App vincula dispositivo (POST /api/devices/link-by-code)
- [ ] Dispositivo queda con `commerce_id` asignado
- [ ] App funciona normalmente

---

## 📚 Referencias

- **Backend Service:** `apps/api/app/Services/DeviceLinkService.php`
- **Backend Controller:** `apps/api/app/Http/Controllers/DeviceLinkController.php`
- **Android Activity:** `apps/android-client/app/src/main/java/com/yapenotifier/android/ui/LinkDeviceActivity.kt`
- **Android ViewModel:** `apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/LinkDeviceViewModel.kt`
- **Modelo:** `apps/api/app/Models/DeviceLinkCode.php`
- **Migración:** `apps/api/database/migrations/2025_01_20_000001_create_device_link_codes_table.php`
- **Guía práctica:** Ver `docs/05-features/DEVICE_LINKING_GUIDE.md` para guía paso a paso detallada

---

**Última actualización:** 2025-01-27

