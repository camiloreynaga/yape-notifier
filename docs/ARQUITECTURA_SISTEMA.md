# 🏗️ Arquitectura y Flujo del Sistema

## 📋 Visión General

El sistema Yape Notifier está compuesto por **3 componentes principales** que trabajan juntos:

1. **App Android** - Captura notificaciones de pagos
2. **API Laravel** - Procesa y almacena los datos
3. **Dashboard Web** - Visualiza y administra la información

---

## 🔄 Flujo Completo del Sistema

### 1️⃣ Configuración Inicial (Primera Vez)

```
┌─────────────────┐
│  Administrador  │
│  (Dashboard)    │
└────────┬────────┘
         │
         │ 1. Crea cuenta
         ▼
┌─────────────────┐
│   API Laravel   │
│  /api/register  │
└────────┬────────┘
         │
         │ 2. Guarda usuario en BD
         ▼
┌─────────────────┐
│  PostgreSQL DB  │
│   (users table) │
└─────────────────┘

┌─────────────────┐
│  Administrador  │
│  (Dashboard)    │
└────────┬────────┘
         │
         │ 3. Registra dispositivo
         │    (ej: "Caja 1 - Yape")
         ▼
┌─────────────────┐
│   API Laravel   │
│  /api/devices   │
└────────┬────────┘
         │
         │ 4. Crea dispositivo con UUID
         ▼
┌─────────────────┐
│  PostgreSQL DB  │
│ (devices table) │
└─────────────────┘
```

**Pasos:**
1. El administrador se registra en el dashboard web (`/register`)
2. La API crea el usuario en la base de datos
3. El administrador crea un dispositivo desde el dashboard (ej: "Caja 1 - Yape")
4. La API genera un UUID único para ese dispositivo
5. El administrador copia el UUID y lo configura en la app Android

---

### 2️⃣ Configuración de la App Android

```
┌─────────────────┐
│  App Android     │
└────────┬────────┘
         │
         │ 1. Usuario inicia sesión
         │    (email + password)
         ▼
┌─────────────────┐
│   API Laravel   │
│   /api/login    │
└────────┬────────┘
         │
         │ 2. Retorna token (Sanctum)
         ▼
┌─────────────────┐
│  App Android    │
│  (Guarda token) │
└────────┬────────┘
         │
         │ 3. Configura UUID del dispositivo
         │    (copiado del dashboard)
         ▼
┌─────────────────┐
│  App Android    │
│  (Listo para    │
│   capturar)     │
└─────────────────┘
```

**Pasos:**
1. Usuario abre la app Android
2. Inicia sesión con email y contraseña
3. La API retorna un token de autenticación (Laravel Sanctum)
4. La app guarda el token localmente (DataStore encriptado)
5. Usuario configura el UUID del dispositivo (copiado del dashboard)
6. Usuario otorga permiso de acceso a notificaciones
7. La app está lista para capturar notificaciones

---

### 3️⃣ Flujo de Captura y Envío de Notificaciones (Tiempo Real)

```
┌─────────────────────────────────┐
│  App de Banco/Yape en Android   │
│  (Yape, BCP, Interbank, etc.)   │
└──────────────┬──────────────────┘
               │
               │ 1. Usuario recibe pago
               │    App genera notificación
               ▼
┌─────────────────────────────────┐
│  Sistema Android                │
│  (NotificationListenerService)   │
└──────────────┬──────────────────┘
               │
               │ 2. Intercepta notificación
               │    Detecta: package name, título, texto
               ▼
┌─────────────────────────────────┐
│  App Android                     │
│  (NotificationParser)            │
└──────────────┬──────────────────┘
               │
               │ 3. Parsea contenido:
               │    - ¿Es pago recibido?
               │    - Extrae monto (S/ 150.00)
               │    - Extrae pagador (Juan Pérez)
               │    - Extrae moneda (PEN)
               ▼
┌─────────────────────────────────┐
│  App Android                     │
│  (NotificationRepository)        │
└──────────────┬──────────────────┘
               │
               │ 4. Prepara payload:
               │    {
               │      device_id: "uuid-del-dispositivo",
               │      source_app: "com.yape",
               │      title: "Pago recibido",
               │      body: "Recibiste S/ 150.00...",
               │      amount: 150.00,
               │      currency: "PEN",
               │      payer_name: "Juan Pérez",
               │      received_at: "2025-12-08T..."
               │    }
               │
               │ 5. Envía POST con token Bearer
               ▼
┌─────────────────────────────────┐
│   API Laravel                   │
│   POST /api/notifications       │
│   (Middleware: auth:sanctum)     │
└──────────────┬──────────────────┘
               │
               │ 6. Valida token
               │    Verifica dispositivo existe
               │    Verifica dispositivo activo
               ▼
┌─────────────────────────────────┐
│  NotificationService             │
│  (Laravel)                       │
└──────────────┬──────────────────┘
               │
               │ 7. Verifica duplicados
               │    (mismo device + app + body + tiempo)
               │
               │ 8. Crea notificación en BD
               │
               │ 9. Actualiza last_seen_at del dispositivo
               ▼
┌─────────────────────────────────┐
│  PostgreSQL Database             │
│  (notifications table)           │
└──────────────┬──────────────────┘
               │
               │ 10. Retorna respuesta
               ▼
┌─────────────────────────────────┐
│  App Android                     │
│  (Recibe confirmación)           │
└─────────────────────────────────┘
```

**Pasos detallados:**

1. **Captura**: El `NotificationListenerService` de Android intercepta la notificación del banco/Yape
2. **Filtrado**: Verifica si es de una app de pago configurada (Yape, BCP, etc.)
3. **Parseo**: El `NotificationParser` extrae información relevante:
   - Monto: "S/ 150.00" → `150.00`
   - Pagador: "Juan Pérez" → `"Juan Pérez"`
   - Moneda: Detecta "S/" → `"PEN"`
4. **Preparación**: Construye el objeto `NotificationData` con todos los datos
5. **Envío**: Hace POST a `/api/notifications` con:
   - Header: `Authorization: Bearer {token}`
   - Body: JSON con los datos de la notificación
6. **Validación API**: 
   - Verifica token válido
   - Busca dispositivo por UUID
   - Verifica que el dispositivo esté activo
7. **Procesamiento**:
   - Verifica duplicados (mismo device + app + body en ventana de 5 segundos)
   - Crea registro en BD
   - Actualiza `last_seen_at` del dispositivo
8. **Respuesta**: Retorna confirmación a la app Android

---

### 4️⃣ Visualización en Dashboard Web

```
┌─────────────────────────────────┐
│  Administrador                  │
│  (Abre Dashboard Web)           │
└──────────────┬──────────────────┘
               │
               │ 1. Inicia sesión
               │    (email + password)
               ▼
┌─────────────────────────────────┐
│   API Laravel                   │
│   POST /api/login               │
└──────────────┬──────────────────┘
               │
               │ 2. Retorna token
               ▼
┌─────────────────────────────────┐
│  Dashboard Web                  │
│  (Guarda token en localStorage) │
└──────────────┬──────────────────┘
               │
               │ 3. Carga dashboard
               │    GET /api/notifications/statistics
               │    GET /api/notifications
               │    GET /api/devices
               ▼
┌─────────────────────────────────┐
│   API Laravel                   │
│   (Consulta BD)                 │
└──────────────┬──────────────────┘
               │
               │ 4. Retorna datos
               ▼
┌─────────────────────────────────┐
│  Dashboard Web                  │
│  (Muestra gráficos, tablas)      │
└─────────────────────────────────┘
```

**Funcionalidades del Dashboard:**

1. **Dashboard Principal** (`/dashboard`):
   - Estadísticas generales (total monto, cantidad de notificaciones)
   - Gráficos por día, por aplicación, por estado
   - Resumen por dispositivo

2. **Notificaciones** (`/notifications`):
   - Lista paginada de todas las notificaciones
   - Filtros: dispositivo, aplicación, fecha, estado
   - Cambio de estado (pendiente/validado/inconsistente)
   - Exportación a CSV

3. **Dispositivos** (`/devices`):
   - Lista de dispositivos registrados
   - Crear/editar/eliminar dispositivos
   - Activar/desactivar dispositivos
   - Ver UUID y última actividad

---

## 🔐 Autenticación y Seguridad

### Flujo de Autenticación

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│  App Android │         │  Dashboard   │         │  API Laravel │
│              │         │     Web      │         │              │
└──────┬───────┘         └──────┬───────┘         └──────┬───────┘
       │                        │                        │
       │ POST /api/login        │ POST /api/login        │
       ├────────────────────────┼────────────────────────┤
       │                        │                        │
       │                        │                        │
       │  Response:             │  Response:             │
       │  {                     │  {                     │
       │    token: "abc123...", │    token: "xyz789...", │
       │    user: {...}         │    user: {...}         │
       │  }                     │  }                     │
       │                        │                        │
       │  Guarda token          │  Guarda token          │
       │  (DataStore)           │  (localStorage)        │
       │                        │                        │
       │  Usa en headers:      │  Usa en headers:       │
       │  Authorization:        │  Authorization:        │
       │  Bearer abc123...      │  Bearer xyz789...      │
       └────────────────────────┴────────────────────────┘
```

**Laravel Sanctum:**
- Cada dispositivo/app genera su propio token
- Los tokens se validan en cada petición
- Los tokens se pueden revocar (logout)
- Cada usuario solo ve sus propios datos

---

## 📊 Flujo de Datos Completo

### Ejemplo Real: Pago Recibido por Yape

```
1. Cliente paga S/ 150.00 por Yape
   ↓
2. App Yape genera notificación:
   "Recibiste S/ 150.00 de Juan Pérez"
   ↓
3. App Android intercepta notificación
   ↓
4. Parser extrae:
   - source_app: "com.yape"
   - amount: 150.00
   - currency: "PEN"
   - payer_name: "Juan Pérez"
   - received_at: "2025-12-08T23:30:00Z"
   ↓
5. App Android envía a API:
   POST http://api.com/api/notifications
   Headers: Authorization: Bearer {token}
   Body: {
     device_id: "550e8400-e29b-41d4-a716-446655440000",
     source_app: "com.yape",
     title: "Pago recibido",
     body: "Recibiste S/ 150.00 de Juan Pérez",
     amount: 150.00,
     currency: "PEN",
     payer_name: "Juan Pérez",
     received_at: "2025-12-08T23:30:00Z"
   }
   ↓
6. API Laravel:
   - Valida token → Usuario autenticado
   - Busca dispositivo por UUID
   - Verifica dispositivo activo
   - Verifica duplicados (no hay)
   - Crea registro en BD
   - Actualiza last_seen_at del dispositivo
   ↓
7. Respuesta a App Android:
   {
     message: "Notification created successfully",
     notification: { id: 123, ... }
   }
   ↓
8. Administrador abre Dashboard:
   - Ve nueva notificación en tiempo real
   - Puede filtrar, validar, exportar
```

---

## 🗄️ Estructura de Base de Datos

### Tablas Principales

```
users
├── id
├── name
├── email
├── password (hashed)
└── timestamps

devices
├── id
├── user_id (FK → users)
├── uuid (único, usado por app Android)
├── name ("Caja 1 - Yape")
├── platform ("android")
├── is_active (true/false)
├── last_seen_at (última notificación recibida)
└── timestamps

notifications
├── id
├── user_id (FK → users)
├── device_id (FK → devices)
├── source_app ("com.yape", "com.bcp.bancamovil", etc.)
├── title
├── body (texto completo)
├── amount (150.00)
├── currency ("PEN")
├── payer_name ("Juan Pérez")
├── received_at (timestamp del pago)
├── raw_json (datos adicionales)
├── status ("pending", "validated", "inconsistente")
├── is_duplicate (true/false)
└── timestamps
```

---

## 🔄 Sincronización y Estados

### Estados del Dispositivo

- **Activo** (`is_active = true`): Acepta notificaciones
- **Inactivo** (`is_active = false`): Rechaza notificaciones (403)
- **Última actividad** (`last_seen_at`): Se actualiza cada vez que recibe una notificación

### Estados de Notificación

- **Pendiente** (`pending`): Recién recibida, sin revisar
- **Validada** (`validated`): Confirmada como correcta
- **Inconsistente** (`inconsistente`): Hay algún problema

### Detección de Duplicados

El sistema detecta duplicados cuando:
- Mismo `device_id`
- Misma `source_app`
- Mismo `body` (texto completo)
- Dentro de una ventana de 5 segundos

---

## 🚀 Flujo de Despliegue

### Desarrollo Local

```
App Android (Emulador)
    ↓
http://10.0.2.2:8000/api  (API local)
    ↓
Dashboard Web
    ↓
http://localhost:3001  (Vite dev server)
```

### Producción

```
App Android (Dispositivos físicos)
    ↓
https://api.tudominio.com/api  (Railway/DigitalOcean)
    ↓
Dashboard Web
    ↓
https://dashboard.tudominio.com  (Nginx + build estático)
```

---

## 📱 Configuración de la App Android

### Pasos para Configurar un Dispositivo

1. **En el Dashboard Web:**
   - Crear dispositivo: "Caja 1 - Yape"
   - Copiar el UUID generado (ej: `550e8400-e29b-41d4-a716-446655440000`)

2. **En la App Android:**
   - Iniciar sesión con email y contraseña
   - Pegar el UUID del dispositivo
   - Otorgar permiso de acceso a notificaciones
   - La app está lista

3. **Verificación:**
   - El dashboard muestra el dispositivo como "activo"
   - `last_seen_at` se actualiza cuando llega una notificación

---

## 🔍 Troubleshooting Común

### Problema: App Android no envía notificaciones

**Verificar:**
1. Token válido (no expirado)
2. UUID del dispositivo correcto
3. Dispositivo activo en dashboard
4. Permiso de notificaciones otorgado
5. URL de API correcta en la app

### Problema: Dashboard no muestra datos

**Verificar:**
1. Token válido en localStorage
2. Usuario tiene notificaciones asociadas
3. Filtros no están ocultando datos
4. API respondiendo correctamente

### Problema: Duplicados

**Causa:** Misma notificación enviada múltiples veces
**Solución:** El sistema detecta y marca como duplicado automáticamente

---

## 📝 Resumen del Flujo

1. **Setup**: Admin crea cuenta y dispositivos en dashboard
2. **Config**: App Android se autentica y configura UUID
3. **Captura**: App Android intercepta notificaciones de pagos
4. **Parseo**: App Android extrae datos relevantes
5. **Envío**: App Android envía a API con autenticación
6. **Procesamiento**: API valida, verifica duplicados, guarda en BD
7. **Visualización**: Dashboard muestra datos en tiempo real
8. **Administración**: Admin filtra, valida, exporta datos

---

## 🔗 Endpoints Clave

### Autenticación
- `POST /api/register` - Crear cuenta
- `POST /api/login` - Iniciar sesión
- `POST /api/logout` - Cerrar sesión
- `GET /api/me` - Obtener usuario actual

### Dispositivos
- `GET /api/devices` - Listar dispositivos
- `POST /api/devices` - Crear dispositivo
- `PUT /api/devices/{id}` - Actualizar dispositivo
- `DELETE /api/devices/{id}` - Eliminar dispositivo
- `POST /api/devices/{id}/toggle-status` - Activar/desactivar

### Notificaciones
- `POST /api/notifications` - Crear notificación (desde Android)
- `GET /api/notifications` - Listar notificaciones (con filtros)
- `GET /api/notifications/statistics` - Estadísticas
- `GET /api/notifications/{id}` - Ver notificación específica
- `PATCH /api/notifications/{id}/status` - Cambiar estado

---

---

## 🔄 Mapeo de Aplicaciones

La app Android mapea los package names a identificadores simples:

| Package Name (Android) | Source App (API) |
|------------------------|------------------|
| `com.yape.android` | `yape` |
| `com.plin.android` | `plin` |
| `com.bcp.bancadigital` | `bcp` |
| `com.interbank.mobilebanking` | `interbank` |
| `com.bbva.bbvacontinental` | `bbva` |
| `com.scotiabank.mobile` | `scotiabank` |

El parser en Android detecta el package name y lo convierte al identificador simple antes de enviarlo a la API.

---

## 📱 Ejemplo de Payload Completo

### Request desde App Android

```json
POST /api/notifications
Headers:
  Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc...
  Content-Type: application/json

Body:
{
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "source_app": "yape",
  "title": "Pago recibido",
  "body": "Recibiste S/ 150.00 de Juan Pérez",
  "amount": 150.00,
  "currency": "PEN",
  "payer_name": "Juan Pérez",
  "received_at": "2025-12-08T23:30:00.000Z",
  "raw_json": {
    "package_name": "com.yape.android",
    "title": "Pago recibido",
    "body": "Recibiste S/ 150.00 de Juan Pérez"
  },
  "status": "pending"
}
```

### Response de la API

```json
Status: 201 Created

{
  "message": "Notification created successfully",
  "notification": {
    "id": 123,
    "user_id": 2,
    "device_id": 5,
    "source_app": "yape",
    "title": "Pago recibido",
    "body": "Recibiste S/ 150.00 de Juan Pérez",
    "amount": "150.00",
    "currency": "PEN",
    "payer_name": "Juan Pérez",
    "received_at": "2025-12-08T23:30:00.000000Z",
    "status": "pending",
    "is_duplicate": false,
    "created_at": "2025-12-08T23:30:01.000000Z",
    "updated_at": "2025-12-08T23:30:01.000000Z"
  }
}
```

---

## 🔐 Seguridad y Autenticación

### Flujo de Tokens

1. **Login/Register**: Usuario obtiene token
2. **Almacenamiento**:
   - App Android: DataStore encriptado
   - Dashboard Web: localStorage
3. **Uso**: Token se envía en header `Authorization: Bearer {token}`
4. **Validación**: Laravel Sanctum valida token en cada request
5. **Expiración**: Tokens no expiran por defecto (se pueden revocar manualmente)

### Autorización

- Cada usuario solo ve sus propios datos
- Los dispositivos están asociados a un usuario específico
- Las notificaciones están asociadas a un dispositivo y usuario
- El middleware `auth:sanctum` protege todas las rutas privadas

---

## 🎯 Casos de Uso Completos

### Caso 1: Negocio con Múltiples Cajas

**Escenario**: Un negocio tiene 3 cajas, cada una con un celular Android

1. **Setup**:
   - Admin crea cuenta en dashboard
   - Crea 3 dispositivos: "Caja 1", "Caja 2", "Caja 3"
   - Obtiene 3 UUIDs diferentes

2. **Configuración**:
   - Cada celular instala la app Android
   - Cada uno se autentica con la misma cuenta
   - Cada uno configura su UUID correspondiente

3. **Operación**:
   - Cliente paga en Caja 1 → Notificación llega al celular de Caja 1
   - App Android envía notificación con UUID de Caja 1
   - Dashboard muestra: "Caja 1 recibió S/ 150.00"
   - Admin puede filtrar por caja para ver ingresos por ubicación

### Caso 2: Validación de Pagos

**Escenario**: Admin quiere validar que los pagos recibidos coincidan con las órdenes

1. **Recepción**: Notificaciones llegan automáticamente
2. **Revisión**: Admin abre dashboard y ve todas las notificaciones
3. **Validación**: 
   - Compara monto recibido vs monto esperado
   - Marca como "validated" si coincide
   - Marca como "inconsistente" si hay diferencia
4. **Reporte**: Exporta a CSV para contabilidad

### Caso 3: Monitoreo en Tiempo Real

**Escenario**: Admin quiere ver ingresos del día en tiempo real

1. **Dashboard**: Abre página de dashboard
2. **Estadísticas**: Ve totales del día, gráficos por hora
3. **Filtros**: Filtra por dispositivo, aplicación, rango de fechas
4. **Alertas**: Identifica duplicados o inconsistencias

---

## 🛠️ Tecnologías Utilizadas

### App Android
- **Kotlin** - Lenguaje principal
- **MVVM** - Arquitectura
- **NotificationListenerService** - Captura notificaciones
- **Retrofit** - Cliente HTTP
- **Coroutines** - Operaciones asíncronas
- **DataStore** - Almacenamiento local encriptado

### API Laravel
- **PHP 8.2+** - Lenguaje
- **Laravel 11** - Framework
- **Laravel Sanctum** - Autenticación
- **PostgreSQL** - Base de datos
- **Service Pattern** - Arquitectura limpia

### Dashboard Web
- **React 18** - Framework frontend
- **TypeScript** - Tipado estático
- **Vite** - Build tool
- **Tailwind CSS** - Estilos
- **Recharts** - Gráficos
- **Axios** - Cliente HTTP

---

## 📊 Flujo de Datos Resumido

```
┌─────────────┐
│   Cliente   │ Paga S/ 150.00
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  App Yape   │ Genera notificación
└──────┬──────┘
       │
       ▼
┌─────────────────────────┐
│ App Android             │
│ - Intercepta            │
│ - Parsea                │
│ - Extrae datos           │
└──────┬──────────────────┘
       │
       │ POST /api/notifications
       │ (con token)
       ▼
┌─────────────────────────┐
│ API Laravel             │
│ - Valida token          │
│ - Verifica dispositivo  │
│ - Detecta duplicados    │
│ - Guarda en BD          │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│ PostgreSQL              │
│ (notifications table)   │
└──────┬──────────────────┘
       │
       │ GET /api/notifications
       │ (con token)
       ▼
┌─────────────────────────┐
│ Dashboard Web           │
│ - Muestra en tiempo real│
│ - Gráficos y estadísticas│
│ - Filtros y exportación │
└─────────────────────────┘
```

---

Este es el flujo completo del sistema. ¿Hay alguna parte específica que quieras que profundice más?

