# 🔐 Cómo Funciona el Sistema con PIN

> **Explicación completa del flujo desde que el empleado abre la app hasta que el admin ve la notificación**

---

## 📱 Flujo Completo: Paso a Paso

### 🎬 Escenario de Ejemplo

**Personajes:**
- **Juan** → Administrador del comercio "Bodega San Miguel"
- **María** → Empleada captadora (nueva)
- **Cliente** → Persona que paga con Yape

---

## FASE 1: Preparación (Dashboard)

### Paso 1: Juan crea cuenta para María

```
Juan (en dashboard web):
1. Va a "Empleados" → "Agregar Empleado"
2. Escribe: Nombre = "María Gonzales"
3. Click en "Generar PIN"
4. Sistema genera: PIN = "4729"
5. Click en "Crear Empleado"

✅ Sistema crea en BD:
┌─────────────────────────────────┐
│ users                           │
├─────────────────────────────────┤
│ id: 15                          │
│ name: "María Gonzales"          │
│ pin: "4729"                     │
│ role: "capturer"                │
│ commerce_id: 1 (Bodega San Mig.)│
│ is_active: true                 │
└─────────────────────────────────┘
```

**Juan le dice a María:**
> "María, tu PIN es **4729**. Descarga la app y úsalo para entrar."

---

## FASE 2: Primer Uso de la App (Android)

### Paso 2: María descarga e instala la app

```
María:
1. Descarga "Yape Notifier" desde Google Play
2. Abre la app por primera vez
3. Ve pantalla: "Ingresa tu PIN"
```

**Pantalla de PIN:**
```
┌─────────────────────────────────┐
│                                 │
│         [LOGO]                  │
│                                 │
│      Ingresa tu PIN             │
│                                 │
│      ○  ○  ○  ○                 │
│                                 │
│      [1] [2] [3]                │
│      [4] [5] [6]                │
│      [7] [8] [9]                │
│      [ ] [0] [⌫]                │
│                                 │
│  ¿Olvidaste tu PIN?             │
│  Contacta a tu administrador    │
└─────────────────────────────────┘
```

### Paso 3: María ingresa su PIN

```
María escribe: 4 → 7 → 2 → 9

App Android:
1. Envía request a backend:
   POST https://api.notificaciones.space/api/auth/login-pin
   Body: { "pin": "4729" }

2. Backend busca en BD:
   SELECT * FROM users WHERE pin = '4729' AND is_active = true

3. Backend encuentra a María:
   ✅ Usuario válido
   ✅ Tiene commerce_id = 1
   ✅ Está activa

4. Backend genera token JWT:
   token = "eyJ0eXAiOiJKV1QiLCJhbGc..."

5. Backend responde:
   {
     "message": "Login exitoso",
     "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
     "user": {
       "id": 15,
       "name": "María Gonzales",
       "role": "capturer",
       "commerce_id": 1
     }
   }

6. App guarda en SharedPreferences:
   ✅ auth_token = "eyJ0eXAiOiJKV1QiLCJhbGc..."
   ✅ user_id = "15"
   ✅ user_name = "María Gonzales"
   ✅ commerce_id = "1"
```

**María ve:**
```
┌─────────────────────────────────┐
│  ✅ ¡Bienvenida María Gonzales! │
└─────────────────────────────────┘
```

---

## FASE 3: Vinculación de Dispositivo

### Paso 4: Juan genera código QR

```
Juan (en dashboard):
1. Va a "Dispositivos" → "Generar Código"
2. Sistema genera código: "ABC123"
3. Muestra QR en pantalla

✅ Sistema crea en BD:
┌─────────────────────────────────┐
│ device_link_codes               │
├─────────────────────────────────┤
│ code: "ABC123"                  │
│ commerce_id: 1                  │
│ expires_at: 2026-01-09 22:00:00 │
│ is_used: false                  │
└─────────────────────────────────┘
```

### Paso 5: María escanea el QR

```
María (en app):
1. Ve pantalla: "Vincula tu dispositivo"
2. Click en "Escanear QR"
3. Escanea código: "ABC123"

App Android:
1. Genera UUID del dispositivo:
   device_uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

2. Envía request a backend:
   POST https://api.notificaciones.space/api/devices/link-by-code
   Headers: {
     "Authorization": "Bearer eyJ0eXAiOiJKV1QiLCJhbGc..."
   }
   Body: {
     "code": "ABC123",
     "device_uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     "device_name": "Samsung Galaxy A54"
   }

3. Backend (DeviceLinkService):
   a) Valida token JWT → Obtiene user_id = 15 (María)
   b) Valida código "ABC123" → ✅ Válido, commerce_id = 1
   c) Busca dispositivo por UUID → No existe
   d) Crea nuevo dispositivo:

✅ Sistema crea en BD:
┌─────────────────────────────────┐
│ devices                         │
├─────────────────────────────────┤
│ id: 42                          │
│ uuid: "a1b2c3d4-e5f6-7890..."   │
│ user_id: 15 (María)             │
│ commerce_id: 1 (Bodega)         │
│ name: "Samsung Galaxy A54"      │
│ alias: "Teléfono de María Gon." │
│ platform: "android"             │
│ is_active: true                 │
│ last_seen_at: 2026-01-09 20:00  │
└─────────────────────────────────┘

   e) Marca código como usado:
      UPDATE device_link_codes 
      SET is_used = true, device_id = 42
      WHERE code = 'ABC123'

4. Backend responde:
   {
     "success": true,
     "device": { ... },
     "message": "Dispositivo vinculado exitosamente"
   }

5. App guarda en SharedPreferences:
   ✅ device_id = "42"
   ✅ device_uuid = "a1b2c3d4-e5f6-7890..."
```

**María ve:**
```
┌─────────────────────────────────┐
│  ✅ Dispositivo vinculado       │
│                                 │
│  Ahora puedes capturar          │
│  notificaciones de pago         │
└─────────────────────────────────┘
```

---

## FASE 4: Captura de Notificación (Día a Día)

### Paso 6: Cliente paga con Yape

```
Cliente:
1. Abre app Yape
2. Envía S/ 25.50 a "Bodega San Miguel"
3. Yape muestra: "✅ Enviaste S/ 25.50 a Bodega San Miguel"
```

**Sistema Android genera notificación:**
```
┌─────────────────────────────────┐
│ 🟣 Yape                         │
│ Recibiste S/ 25.50              │
│ De: Carlos Mendoza              │
│ Hace 1 segundo                  │
└─────────────────────────────────┘
```

### Paso 7: App captura la notificación

```
PaymentNotificationListenerService (en background):

1. Detecta notificación de "com.yape"
2. Extrae texto: "Recibiste S/ 25.50\nDe: Carlos Mendoza"
3. Parsea datos:
   ✅ amount = 25.50
   ✅ currency = "PEN"
   ✅ sender_name = "Carlos Mendoza"
   ✅ app_name = "Yape"

4. Guarda en Room Database (local):
   ┌─────────────────────────────────┐
   │ NotificationEntity              │
   ├─────────────────────────────────┤
   │ id: 1                           │
   │ title: "Yape"                   │
   │ text: "Recibiste S/ 25.50..."   │
   │ amount: 25.50                   │
   │ currency: "PEN"                 │
   │ sender_name: "Carlos Mendoza"   │
   │ app_name: "Yape"                │
   │ timestamp: 2026-01-09 20:15:30  │
   │ is_sent: false                  │
   └─────────────────────────────────┘

5. Programa envío con WorkManager:
   SendNotificationWorker.schedule()
```

### Paso 8: App envía notificación al backend

```
SendNotificationWorker (en background):

1. Lee notificación de Room Database
2. Obtiene datos guardados:
   - auth_token = "eyJ0eXAiOiJKV1QiLCJhbGc..."
   - device_id = "42"
   - device_uuid = "a1b2c3d4-e5f6-7890..."

3. Envía request a backend:
   POST https://api.notificaciones.space/api/notifications
   Headers: {
     "Authorization": "Bearer eyJ0eXAiOiJKV1QiLCJhbGc..."
   }
   Body: {
     "title": "Yape",
     "text": "Recibiste S/ 25.50\nDe: Carlos Mendoza",
     "package_name": "com.yape",
     "posted_at": "2026-01-09T20:15:30Z",
     "device_uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     "android_user_id": 0,
     "amount": 25.50,
     "currency": "PEN",
     "sender_name": "Carlos Mendoza",
     "app_name": "Yape"
   }

4. Backend (NotificationService):
   a) Valida token JWT → Obtiene user_id = 15 (María)
   b) Busca dispositivo por UUID → device_id = 42
   c) Obtiene commerce_id del dispositivo → commerce_id = 1
   d) Verifica duplicados → No existe
   e) Crea notificación:

✅ Sistema crea en BD:
┌─────────────────────────────────┐
│ notifications                   │
├─────────────────────────────────┤
│ id: 789                         │
│ user_id: 15 (María) ✅          │
│ device_id: 42                   │
│ commerce_id: 1 (Bodega) ✅      │
│ title: "Yape"                   │
│ text: "Recibiste S/ 25.50..."   │
│ package_name: "com.yape"        │
│ posted_at: 2026-01-09 20:15:30  │
│ amount: 25.50                   │
│ currency: "PEN"                 │
│ sender_name: "Carlos Mendoza"   │
│ app_name: "Yape"                │
│ is_validated: false             │
│ created_at: 2026-01-09 20:15:31 │
└─────────────────────────────────┘

   f) Broadcast por WebSocket (Laravel Reverb):
      Event: NotificationReceived
      Channel: commerce.1

5. Backend responde:
   {
     "success": true,
     "notification": { ... },
     "message": "Notificación registrada exitosamente"
   }

6. App actualiza Room Database:
   UPDATE NotificationEntity 
   SET is_sent = true 
   WHERE id = 1
```

---

## FASE 5: Visualización en Dashboard

### Paso 9: Juan ve la notificación en tiempo real

```
Juan (en dashboard web):

1. Está viendo la página "Notificaciones"
2. WebSocket recibe evento:
   Event: NotificationReceived
   Data: {
     "id": 789,
     "amount": 25.50,
     "sender_name": "Carlos Mendoza",
     "app_name": "Yape",
     "user_name": "María Gonzales",  ✅ TRAZABILIDAD
     "device_name": "Samsung Galaxy A54",
     "created_at": "2026-01-09 20:15:31"
   }

3. Dashboard actualiza tabla en tiempo real:
```

**Pantalla de Juan:**
```
┌─────────────────────────────────────────────────────────────────┐
│ 📊 Notificaciones de Pago                                       │
├─────────────────────────────────────────────────────────────────┤
│ ID  │ Monto   │ De              │ App  │ Captador │ Dispositivo │
├─────┼─────────┼─────────────────┼──────┼──────────┼─────────────┤
│ 789 │ S/ 25.50│ Carlos Mendoza  │ Yape │ María G. │ Samsung A54 │ ← NUEVO
│ 788 │ S/ 15.00│ Ana Torres      │ Plin │ Pedro L. │ Xiaomi 11   │
│ 787 │ S/ 50.00│ Luis Ramirez    │ Yape │ María G. │ Samsung A54 │
└─────┴─────────┴─────────────────┴──────┴──────────┴─────────────┘

✅ Juan puede ver:
- Quién capturó: "María Gonzales"
- Desde qué dispositivo: "Samsung Galaxy A54"
- Cuándo: "Hace 1 segundo"
```

---

## 🔍 Trazabilidad Completa

### ¿Qué información tenemos?

```sql
-- Consulta para ver trazabilidad completa
SELECT 
    n.id,
    n.amount,
    n.sender_name,
    n.app_name,
    n.created_at,
    u.name AS captador,           -- ✅ Quién capturó
    d.name AS dispositivo,        -- ✅ Desde dónde
    d.uuid AS device_uuid,        -- ✅ Identificador único
    c.name AS comercio            -- ✅ Para qué comercio
FROM notifications n
JOIN users u ON n.user_id = u.id
JOIN devices d ON n.device_id = d.id
JOIN commerces c ON n.commerce_id = c.id
WHERE n.id = 789;

Resultado:
┌─────┬────────┬────────────────┬──────┬─────────────────────┬─────────────────┬──────────────────┬────────────────────┬──────────────────┐
│ id  │ amount │ sender_name    │ app  │ created_at          │ captador        │ dispositivo      │ device_uuid        │ comercio         │
├─────┼────────┼────────────────┼──────┼─────────────────────┼─────────────────┼──────────────────┼────────────────────┼──────────────────┤
│ 789 │ 25.50  │ Carlos Mendoza │ Yape │ 2026-01-09 20:15:31 │ María Gonzales  │ Samsung Galaxy..│ a1b2c3d4-e5f6...  │ Bodega San Miguel│
└─────┴────────┴────────────────┴──────┴─────────────────────┴─────────────────┴──────────────────┴────────────────────┴──────────────────┘
```

---

## 🔐 Seguridad y Validaciones

### ¿Qué pasa si...?

#### 1. María intenta usar el PIN de otro empleado

```
María escribe PIN: "5678" (PIN de Pedro)

Backend:
1. Busca user con pin = "5678"
2. Encuentra a Pedro (user_id = 16)
3. Genera token para Pedro
4. María queda autenticada como Pedro

⚠️ PROBLEMA: María puede hacerse pasar por Pedro

✅ SOLUCIÓN: Vincular PIN a dispositivo específico
   - Opción A: PIN + IMEI
   - Opción B: PIN + Biometría
   - Opción C: Resetear PIN al cambiar dispositivo
```

**Recomendación:** Agregar validación de dispositivo único por usuario.

#### 2. María pierde su teléfono

```
Juan (en dashboard):
1. Va a "Dispositivos"
2. Busca "Samsung Galaxy A54 (María)"
3. Click en "Desactivar"

Backend:
UPDATE devices 
SET is_active = false 
WHERE id = 42;

Resultado:
- App de María deja de poder enviar notificaciones
- Backend rechaza requests con device_id = 42
- María necesita volver a vincular con nuevo código
```

#### 3. Alguien intenta adivinar PINs

```
Atacante intenta:
- PIN: "0000" → ❌ Inválido
- PIN: "1234" → ❌ Inválido
- PIN: "5555" → ❌ Inválido
...

✅ PROTECCIÓN: Rate limiting en backend
- Máximo 5 intentos por IP cada 15 minutos
- Bloqueo temporal después de 10 intentos fallidos
```

---

## 📊 Comparación: Antes vs Después

### ANTES (Sin PIN)

```
Notificación capturada:
┌─────────────────────────────────┐
│ notifications                   │
├─────────────────────────────────┤
│ id: 789                         │
│ user_id: NULL ❌                │
│ device_id: 42                   │
│ commerce_id: 1                  │
│ amount: 25.50                   │
└─────────────────────────────────┘

Preguntas sin respuesta:
❌ ¿Quién capturó esta notificación?
❌ ¿María o Pedro?
❌ ¿Cómo auditar?
❌ ¿Cómo detectar fraude?
```

### DESPUÉS (Con PIN)

```
Notificación capturada:
┌─────────────────────────────────┐
│ notifications                   │
├─────────────────────────────────┤
│ id: 789                         │
│ user_id: 15 (María) ✅          │
│ device_id: 42                   │
│ commerce_id: 1                  │
│ amount: 25.50                   │
└─────────────────────────────────┘

Preguntas respondidas:
✅ ¿Quién capturó? → María Gonzales
✅ ¿Desde dónde? → Samsung Galaxy A54
✅ ¿Cuándo? → 2026-01-09 20:15:31
✅ ¿Para qué comercio? → Bodega San Miguel
✅ ¿Auditoría? → Completa
✅ ¿Detectar fraude? → Posible
```

---

## 🎯 Resumen del Flujo

```
1. PREPARACIÓN (Dashboard)
   Juan crea empleado → Sistema genera PIN

2. AUTENTICACIÓN (Android)
   María ingresa PIN → Backend valida → Genera token JWT

3. VINCULACIÓN (Android + Backend)
   María escanea QR → Backend vincula dispositivo a María

4. CAPTURA (Android Background)
   Cliente paga → App captura notificación → Guarda local

5. ENVÍO (Android Background)
   WorkManager envía a backend con token JWT

6. REGISTRO (Backend)
   Backend guarda con user_id = María

7. VISUALIZACIÓN (Dashboard)
   Juan ve notificación con nombre de María en tiempo real
```

---

## 🔑 Puntos Clave

1. **PIN = Identidad**
   - Cada empleado tiene un PIN único
   - PIN se valida en backend
   - Backend retorna token JWT

2. **Token = Autorización**
   - Token se envía en cada request
   - Backend extrae user_id del token
   - user_id se guarda en cada notificación

3. **Trazabilidad = Accountability**
   - Cada notificación tiene user_id
   - Cada dispositivo tiene user_id
   - Auditoría completa garantizada

4. **Seguridad = Confianza**
   - PIN único por empleado
   - Token expirable (24h)
   - Dispositivos desactivables

---

**¿Quedó claro el flujo completo?** 🎯

