# Revisión Completa de Implementación vs Requisitos

## 📋 Resumen Ejecutivo

Este documento revisa el estado actual de la implementación comparado con los requisitos del prompt original. Se identifica qué está implementado, qué falta y qué necesita corrección.

---

## ✅ 1. OBJETIVO DEL SISTEMA

### Requisito
Centralizar en un panel de Administrador (móvil Android y web) las notificaciones de pagos/transferencias captadas en múltiples teléfonos Android (captadores). Soporte para apps duales (Dual Apps) donde un mismo teléfono puede tener Yape duplicado.

### Estado: ✅ **IMPLEMENTADO** (con bug crítico)

**✅ Implementado:**
- Panel web para visualizar notificaciones (`NotificationsPage.tsx`)
- App Android que captura notificaciones (`PaymentNotificationListenerService.kt`)
- Backend centralizado que recibe notificaciones (`NotificationController.php`)
- Múltiples dispositivos por usuario
- Sistema de AppInstance para distinguir instancias duales
- Dashboard web muestra instancias y permite renombrarlas

**🔴 BUG CRÍTICO:**
- **Línea 67 de `PaymentNotificationListenerService.kt`**: Usa `sbn.user?.hashCode()` en lugar de `sbn.user?.identifier`
- Esto hace que el identificador de instancia dual sea incorrecto y no funcione correctamente
- **CORRECCIÓN NECESARIA**: Cambiar a `sbn.user?.identifier` o `sbn.user.identifier`

**❌ Faltante:**
- App Android para administrador (solo existe app para captador)
- Dashboard móvil Android para admin

---

## ✅ 2. ROLES Y ESTRUCTURA MULTI-COMERCIO (MULTI-TENANT)

### Requisito
- **Comercio (Tenant)**: unidad aislada de datos
- **Administrador**: gestiona el comercio y visualiza todo (móvil + web)
- **Captador (dispositivo)**: teléfono Android que lee notificaciones

### Estado: ✅ **IMPLEMENTADO**

**✅ Implementado:**
- Modelo `Commerce` con todas las relaciones
- Tabla `commerces` en base de datos
- Campo `commerce_id` en: `users`, `devices`, `notifications`, `app_instances`, `monitor_packages`
- Campo `role` en `users` (admin, captador)
- `CommerceService` para gestión de comercios
- Endpoints API:
  - `POST /api/commerces` - Crear comercio
  - `GET /api/commerces/me` - Obtener comercio del usuario
  - `GET /api/commerces/check` - Verificar si usuario tiene comercio
- Dashboard web tiene pantalla para crear comercio (`CreateCommercePage.tsx`)
- Todos los servicios filtran por `commerce_id` (multi-tenant)

**⚠️ Parcial:**
- El registro de usuario no crea automáticamente un comercio (debe crearse después)
- No hay validación que obligue a tener comercio antes de usar el sistema

---

## ✅ 3. REQUISITO CRÍTICO: APPS DUALES (MIUI Y OTROS)

### Requisito
Cada evento de notificación debe incluir:
- `packageName` ✅
- `androidUserId` (de `StatusBarNotification.getUser()` → `UserHandle.getIdentifier()`) ⚠️
- `androidUid` (opcional) ✅
- `deviceId` ✅

Crear concepto de **AppInstance** = `(deviceId + packageName + androidUserId)`

### Estado: ✅ **IMPLEMENTADO** (con bug crítico)

**✅ Backend Implementado:**
- Tabla `app_instances` con todos los campos necesarios
- Modelo `AppInstance` con método `findOrCreate`
- Campos en `notifications`: `package_name`, `android_user_id`, `android_uid`, `app_instance_id`
- `AppInstanceService` para gestión de instancias
- Endpoints API:
  - `GET /api/app-instances` - Listar instancias del comercio
  - `GET /api/devices/{id}/app-instances` - Instancias de un dispositivo
  - `PATCH /api/app-instances/{id}/label` - Actualizar nombre de instancia
- `NotificationService` crea/busca AppInstance automáticamente
- Deduplicación mejorada usando `package_name + android_user_id + posted_at + body`

**✅ Android Implementado:**
- `CapturedNotification` tiene campos: `androidUserId`, `androidUid`, `postedAt`
- `NotificationData` incluye todos los campos dual
- `SendNotificationWorker` envía todos los campos al backend
- Migración de Room DB (v1 → v2) para nuevos campos

**🔴 BUG CRÍTICO:**
- **Línea 67 de `PaymentNotificationListenerService.kt`**: 
  ```kotlin
  val androidUserId = sbn.user?.hashCode() // ❌ INCORRECTO
  ```
  Debe ser:
  ```kotlin
  val androidUserId = sbn.user?.identifier // ✅ CORRECTO
  ```
- `hashCode()` no es el identificador único del UserHandle, por lo que las instancias duales no se distinguen correctamente

**✅ Dashboard Web:**
- Pantalla `AppInstancesPage.tsx` para gestionar instancias
- Muestra instancias asignadas y sin asignar
- Permite renombrar instancias
- Filtro por instancia en `NotificationsPage.tsx`
- Columna de instancia en tabla de notificaciones

**❌ Faltante en Android:**
- Pantalla para detectar/nombrar instancias duales automáticamente
- UI para asignar nombres a instancias desde la app Android
- Detección automática de múltiples instancias del mismo package

---

## ⚠️ 4. MVP FUNCIONAL

### 4.1 Administrador (Android + Web)

#### Login / Registro
**Estado: ✅ IMPLEMENTADO**
- Login/registro en web (`LoginPage.tsx`, `RegisterPage.tsx`)
- Login/registro en Android (`LoginActivity.kt`, `RegisterActivity.kt`)
- Autenticación con Laravel Sanctum

#### Crear Comercio
**Estado: ✅ IMPLEMENTADO**
- Pantalla web `CreateCommercePage.tsx`
- Endpoint `POST /api/commerces`
- Validación y creación automática de relaciones

#### Ver Feed Central de Notificaciones
**Estado: ✅ IMPLEMENTADO**
- `NotificationsPage.tsx` muestra feed de notificaciones
- Endpoint `GET /api/notifications` con paginación
- Cards con información completa

#### Filtros
**Estado: ✅ IMPLEMENTADO**

**✅ Implementado:**
- Filtro por dispositivo (`device_id`)
- Filtro por app (`source_app`)
- Filtro por instancia (`app_instance_id`) ✅
- Filtro por fechas (`start_date`, `end_date`)
- Filtro por estado (`status`)
- Filtro por `package_name` (implícito en instancia)

#### Vista de Dispositivos
**Estado: ✅ IMPLEMENTADO**

**✅ Implementado:**
- `DevicesPage.tsx` lista dispositivos
- Muestra `last_seen_at` (última actividad)
- Muestra estado `is_active`
- Indicador Online/Offline (método `isOnline()` en modelo `Device`)
- Salud del servicio:
  - `battery_level`
  - `battery_optimization_disabled`
  - `notification_permission_enabled`
  - `last_heartbeat`
- Endpoint `POST /api/devices/{id}/health` para actualizar salud
- Muestra información de instancias por dispositivo

#### Vincular Captadores (QR o código numérico)
**Estado: ✅ IMPLEMENTADO**

**✅ Implementado:**
- Pantalla web `AddDevicePage.tsx` para generar QR/código
- Endpoint `POST /api/devices/generate-link-code` para generar código
- Endpoint `GET /api/devices/link-code/{code}` para validar código (público)
- Endpoint `POST /api/devices/link-by-code` para vincular dispositivo
- Pantalla Android `LinkDeviceActivity.kt` para escanear QR/ingresar código
- Escaneo de QR con permisos de cámara
- Validación de código antes de vincular
- Confirmación de vinculación

#### Configuración: Catálogo de Apps
**Estado: ✅ IMPLEMENTADO**
- Modelo `MonitorPackage` (catálogo de apps)
- Endpoint `GET /api/settings/monitored-packages` (público, usado por Android)
- Endpoint `GET /api/monitor-packages` (admin)
- Endpoint `POST /api/monitor-packages` (crear)
- Endpoint `POST /api/monitor-packages/{id}/toggle-status` (activar/desactivar)
- Relación con `commerce_id` (multi-tenant)
- Dashboard web tiene gestión de apps monitoreadas (parcial)

### 4.2 Captador (Android)

#### Modo "Vincular Dispositivo"
**Estado: ✅ IMPLEMENTADO**
- Pantalla `LinkDeviceActivity.kt` para vinculación
- Escaneo de QR con `ScanContract`
- Ingreso manual de código numérico
- Validación de código antes de vincular
- Confirmación de vinculación

#### Wizard de Permisos
**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

**✅ Implementado:**
- Se detecta si falta permiso de notificaciones
- Redirección a configuración de permisos
- Verificación de estado del servicio

**❌ Faltante:**
- Wizard guiado paso a paso completo
- Guía específica para desactivar optimización de batería
- Detección de OEM (MIUI, OPPO, etc.) con guías específicas (aunque existe `OemDetector.kt`, no se usa en UI)
- Instrucciones visuales para cada paso

#### Selector de Apps a Monitorear
**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

**✅ Implementado:**
- La app consulta `GET /api/settings/monitored-packages` para obtener lista
- Se filtra por `packageName` en el servicio
- Modelo `DeviceMonitoredApp` para configuración por dispositivo
- Endpoints API:
  - `GET /api/devices/{id}/monitored-apps`
  - `POST /api/devices/{id}/monitored-apps`

**❌ Faltante:**
- UI en Android para que el usuario seleccione qué apps monitorear
- Pantalla de configuración de apps por dispositivo

#### Detección/Gestión de Instancias Duales
**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

**✅ Implementado:**
- Se captura `androidUserId` (aunque con bug, ver sección 3)
- Se envía al backend
- Backend crea AppInstance automáticamente
- Dashboard web permite renombrar instancias

**❌ Faltante:**
- Pantalla Android "Instancias detectadas" para mostrar instancias encontradas
- UI para asignar nombres desde Android (ej. "Yape 1 → Rocío")
- Detección automática y notificación cuando se detectan múltiples instancias

#### Estado "Capturando OK"
**Estado: ✅ IMPLEMENTADO**

**✅ Implementado:**
- `ServiceStatusManager` actualiza estado del servicio
- Se muestra estado en UI
- Logging detallado de eventos
- Indicador de último evento procesado

**⚠️ Mejorable:**
- Mostrar "último evento enviado" de forma más clara
- Indicador visual más prominente de "Capturando OK"

---

## ✅ 5. MODELO DE DATOS

### Estado: ✅ **IMPLEMENTADO COMPLETAMENTE**

| Entidad Requerida      | Estado    | Observaciones                                    |
| ---------------------- | --------- | ------------------------------------------------ |
| **Commerce**           | ✅ EXISTE | Tabla, modelo, migración, relaciones completas    |
| **User**               | ✅ EXISTE | Con `commerce_id` y `role`                       |
| **Device**             | ✅ EXISTE | Con `commerce_id`, `alias`, campos de salud      |
| **MonitoredApp**       | ✅ EXISTE | `MonitorPackage` con `commerce_id`               |
| **DeviceMonitoredApp** | ✅ EXISTE | Relación dispositivo-app implementada            |
| **AppInstance**        | ✅ EXISTE | Tabla completa con todos los campos             |
| **NotificationEvent** | ✅ EXISTE | `Notification` con todos los campos requeridos   |

### Campos en Tablas

#### Tabla `users`
- ✅ `commerce_id` (FK a `commerces`)
- ✅ `role` (admin, captador)

#### Tabla `devices`
- ✅ `commerce_id` (FK a `commerces`)
- ✅ `alias` (nombre descriptivo del dispositivo)
- ✅ `battery_level`
- ✅ `battery_optimization_disabled`
- ✅ `notification_permission_enabled`
- ✅ `last_heartbeat`

#### Tabla `notifications`
- ✅ `commerce_id` (FK a `commerces`)
- ✅ `package_name` (string, ej. "com.bcp.innovacxion.yapeapp")
- ✅ `android_user_id` (integer, identificador de perfil dual)
- ✅ `android_uid` (integer, opcional)
- ✅ `app_instance_id` (FK a `app_instances`)
- ✅ `posted_at` (timestamp, hora original de la notificación)
- ✅ `received_at` (timestamp, hora backend)

#### Tabla `app_instances`
- ✅ `id`
- ✅ `commerce_id` (FK)
- ✅ `device_id` (FK)
- ✅ `package_name`
- ✅ `android_user_id`
- ✅ `instance_label` (ej. "Yape 1 (Rocío)")
- ✅ `created_at`, `updated_at`
- ✅ Constraint único: `(device_id, package_name, android_user_id)`

#### Tabla `device_monitored_apps`
- ✅ `device_id` (FK)
- ✅ `package_name`
- ✅ `enabled` (boolean)

---

## ✅ 6. FLUJOS DE UX

### 6.1 Admin: Alta del Comercio
**Estado: ✅ IMPLEMENTADO**
- Flujo: Registro → Login → Crear Comercio (si no tiene)
- Pantalla `CreateCommercePage.tsx`
- Validación y creación automática

### 6.2 Admin: Vincular Captador
**Estado: ✅ IMPLEMENTADO**
- Pantalla `AddDevicePage.tsx` para generar QR/código
- Generación de código de 8 caracteres
- Pantalla "Esperando vinculación..." con QR y código
- Expiración de códigos (configurable)

### 6.3 Captador: Vinculación y Permisos
**Estado: ✅ IMPLEMENTADO**
- Pantalla `LinkDeviceActivity.kt` "Vincular como captador"
- Escaneo QR o ingreso de código
- Validación de código antes de vincular
- Activación de permiso de notificaciones (redirección a configuración)
- ⚠️ Falta wizard completo de permisos con guías

### 6.4 Captador: Detección de Instancias (Dual Apps)
**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**
- Backend detecta y crea instancias automáticamente
- Dashboard web permite nombrar instancias
- ❌ No hay pantalla Android "Instancias detectadas"
- ❌ No hay UI para asignar nombres desde Android

### 6.5 Admin: Operación Diaria
**Estado: ✅ IMPLEMENTADO**
- Feed con cards de notificaciones
- Muestra: App + Instancia + Dispositivo + Hora + Monto ✅
- Filtros: App, Dispositivo, Instancia, Fecha ✅
- Vista de dispositivos con salud
- Gestión de instancias

---

## ⚠️ 7. PANTALLAS CONCRETAS

### Admin (Android + Web)

| Pantalla                         | Web | Android | Estado                               |
| -------------------------------- | --- | ------- | ------------------------------------ |
| Login                            | ✅  | ✅      | Implementado                         |
| Registro                         | ✅  | ✅      | Implementado                         |
| Crear comercio                   | ✅  | ❌      | Solo web                             |
| Dashboard (tabs)                 | ⚠️  | ❌      | Web tiene páginas separadas, no tabs |
| Notificaciones (feed + filtros)  | ✅  | ❌      | Solo web                             |
| Detalle de notificación          | ✅  | ❌      | Solo web (`NotificationDetailPage`)   |
| Dispositivos (lista + salud)     | ✅  | ❌      | Solo web                             |
| Agregar dispositivo (QR/código)  | ✅  | ❌      | Solo web                             |
| Configuración: Apps monitoreadas | ⚠️  | ❌      | Parcial (solo API, falta UI completa)|
| Gestión de instancias            | ✅  | ❌      | Solo web (`AppInstancesPage`)         |

### Captador (Android)

| Pantalla                                   | Estado                                     |
| ------------------------------------------ | ------------------------------------------ |
| Vincular dispositivo (QR/código)           | ✅ **IMPLEMENTADO** (`LinkDeviceActivity`) |
| Wizard permisos (notificaciones + batería) | ⚠️ Parcial (solo notificaciones)           |
| Apps a monitorear (checklist)              | ❌ **FALTA**                               |
| Instancias duales (mapeo/alias)            | ❌ **FALTA**                               |
| Estado (capturando / errores)              | ✅ Implementado (`ServiceStatusManager`)   |

---

## ✅ 8. REGLAS OPERATIVAS

### Requisito: Soporte para múltiples instancias de la misma app
**Estado: ✅ IMPLEMENTADO**
- Se captura `androidUserId` (aunque con bug, ver sección 3)
- Se crean `AppInstance` automáticamente
- Se puede distinguir entre Yape 1 y Yape 2

### Requisito: Admin puede renombrar instancias
**Estado: ✅ IMPLEMENTADO**
- Dashboard web permite renombrar instancias
- Endpoint `PATCH /api/app-instances/{id}/label`
- ❌ No hay UI Android para renombrar

### Requisito: Nueva instancia queda "Sin asignar"
**Estado: ✅ IMPLEMENTADO**
- Instancias sin `instance_label` aparecen como "Sin asignar"
- Dashboard web las muestra separadas
- Permite asignar nombre después

### Requisito: Feed muestra App + Instancia + Dispositivo
**Estado: ✅ IMPLEMENTADO**
- Feed muestra: App + Instancia + Dispositivo + Hora + Monto
- Columna de instancia en tabla
- Filtro por instancia disponible

---

## 🔴 9. NOTA TÉCNICA (Android) - BUG CRÍTICO

### Requisito
En `NotificationListenerService`, capturar:
- `sbn.getPackageName()` ✅
- `sbn.getUser()` → `UserHandle.getIdentifier()` ❌ **BUG**
- `sbn.getUid()` (opcional) ✅
- `sbn.getNotification().extras` → title/text ✅

### Estado: ⚠️ **IMPLEMENTADO CON BUG CRÍTICO**

**Código actual (`PaymentNotificationListenerService.kt` línea 67):**
```kotlin
val androidUserId = sbn.user?.hashCode() // ❌ INCORRECTO
```

**Debe ser:**
```kotlin
val androidUserId = sbn.user?.identifier // ✅ CORRECTO
```

**Impacto:**
- `hashCode()` no es el identificador único del UserHandle
- Las instancias duales no se distinguen correctamente
- AppInstance se crea con identificador incorrecto
- No funciona correctamente el sistema de apps duales

**Corrección necesaria:**
1. Cambiar línea 67 de `PaymentNotificationListenerService.kt`
2. Verificar que `identifier` esté disponible en la versión de Android SDK usada
3. Probar con dispositivos MIUI reales

---

## 📊 RESUMEN POR PRIORIDAD

### 🔴 CRÍTICO (Bloquea funcionalidad core)

1. **BUG: androidUserId usa hashCode()** → Corregir a `identifier` ⚠️
   - **Ubicación**: `PaymentNotificationListenerService.kt` línea 67
   - **Impacto**: Apps duales no funcionan correctamente
   - **Estado**: Implementado pero con bug

### 🟡 IMPORTANTE (Funcionalidad parcial)

1. **Wizard permisos completo** → Agregar guías de batería y OEM
2. **Selector apps en Android** → UI para seleccionar apps por dispositivo
3. **Gestión instancias en Android** → Pantalla para nombrar instancias desde Android
4. **App Android para admin** → Dashboard móvil para administradores

### 🟢 MEJORAS (Nice to have)

1. Dashboard con tabs en lugar de páginas separadas
2. Configuración de apps en dashboard web (UI completa)
3. Indicador online/offline más preciso
4. Exportación mejorada

---

## ✅ IMPLEMENTACIONES COMPLETADAS

### Backend (Laravel API)
- ✅ Multi-tenant con Commerce
- ✅ AppInstance para apps duales
- ✅ Sistema de vinculación QR/código
- ✅ Gestión de apps monitoreadas
- ✅ Salud de dispositivos
- ✅ Deduplicación mejorada
- ✅ Todos los endpoints necesarios

### Android App
- ✅ Captura de notificaciones
- ✅ Envío al backend
- ✅ Vinculación por QR/código
- ✅ Almacenamiento local
- ⚠️ Bug en androidUserId (hashCode vs identifier)
- ❌ UI para gestionar instancias
- ❌ UI para seleccionar apps

### Dashboard Web
- ✅ Autenticación
- ✅ Feed de notificaciones con filtros
- ✅ Gestión de dispositivos
- ✅ Gestión de instancias
- ✅ Crear comercio
- ✅ Generar códigos de vinculación
- ✅ Estadísticas y KPIs

---

## 🎯 PENDIENTES DE IMPLEMENTACIÓN

### Crítico
1. **Corregir bug androidUserId**: Cambiar `hashCode()` por `identifier`

### Importante
1. **Pantalla Android para gestionar instancias duales**
   - Detectar instancias automáticamente
   - Permitir asignar nombres
   - Mostrar lista de instancias detectadas

2. **Wizard completo de permisos en Android**
   - Guía paso a paso
   - Instrucciones para desactivar optimización de batería
   - Detección de OEM con guías específicas

3. **Selector de apps en Android**
   - UI para seleccionar qué apps monitorear
   - Configuración por dispositivo

4. **App Android para administrador**
   - Dashboard móvil
   - Gestión de dispositivos
   - Visualización de notificaciones

### Mejoras
1. Dashboard web con tabs
2. UI completa para configuración de apps monitoreadas
3. Mejoras en indicadores de estado

---

## 📝 NOTAS FINALES

El proyecto tiene una **base sólida y funcional** con:
- ✅ Multi-tenancy implementado
- ✅ Apps duales implementado (con bug a corregir)
- ✅ Sistema de vinculación QR funcionando
- ✅ Dashboard web completo
- ✅ Backend robusto con todos los endpoints

**El único bloqueador crítico es el bug en `androidUserId`** que debe corregirse para que las apps duales funcionen correctamente.

El resto de funcionalidades faltantes son mejoras de UX y funcionalidades adicionales que no bloquean el funcionamiento básico del sistema.

