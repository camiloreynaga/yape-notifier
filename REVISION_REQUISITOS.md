# Revisión de Requisitos vs Implementación Actual

## 📋 Resumen Ejecutivo

Este documento compara los requisitos especificados con la implementación actual del proyecto. Se identifican las funcionalidades implementadas, las faltantes y las que requieren ajustes.

---

## ✅ 1. OBJETIVO DEL SISTEMA

### Requisito

Centralizar en un panel de Administrador (móvil Android y web) las notificaciones de pagos/transferencias captadas en múltiples teléfonos Android (captadores). Soporte para apps duales (Dual Apps) donde un mismo teléfono puede tener Yape duplicado.

### Estado: ⚠️ **PARCIALMENTE IMPLEMENTADO**

**✅ Implementado:**

- Panel web para visualizar notificaciones
- App Android que captura notificaciones
- Backend centralizado que recibe notificaciones
- Múltiples dispositivos por usuario

**❌ Faltante:**

- **CRÍTICO**: No se captura `androidUserId` para distinguir apps duales
- **CRÍTICO**: No existe el concepto de "AppInstance" en el modelo de datos
- No hay diferenciación entre instancias duales (Yape 1 / Yape 2)
- El admin no puede nombrar instancias (ej. "Yape 1 (Rocío)")

---

## ✅ 2. ROLES Y ESTRUCTURA MULTI-COMERCIO (MULTI-TENANT)

### Requisito

- **Comercio (Tenant)**: unidad aislada de datos
- **Administrador**: gestiona el comercio y visualiza todo (móvil + web)
- **Captador (dispositivo)**: teléfono Android que lee notificaciones

### Estado: ❌ **NO IMPLEMENTADO**

**❌ Faltante:**

- **CRÍTICO**: No existe el modelo `Commerce` (Tenant)
- **CRÍTICO**: No hay multi-tenancy - todos los usuarios comparten el mismo espacio
- No existe la relación `User -> Commerce`
- No existe la relación `Device -> Commerce`
- No existe la relación `Notification -> Commerce`
- El sistema actual es single-tenant (cada usuario tiene sus propios datos, pero no hay aislamiento por comercio)

**✅ Implementado:**

- Usuarios con autenticación
- Dispositivos asociados a usuarios
- Notificaciones asociadas a usuarios

---

## ❌ 3. REQUISITO CRÍTICO: APPS DUALES (MIUI Y OTROS)

### Requisito

Cada evento de notificación debe incluir:

- `packageName`
- `androidUserId` (de `StatusBarNotification.getUser()` → `UserHandle.getIdentifier()`)
- `androidUid` (opcional)
- `deviceId`

Crear concepto de **AppInstance** = `(deviceId + packageName + androidUserId)`

### Estado: ❌ **NO IMPLEMENTADO**

**❌ Faltante:**

- **CRÍTICO**: No se captura `androidUserId` en `PaymentNotificationListenerService.kt`
- **CRÍTICO**: No existe tabla/modelo `app_instances` en la base de datos
- **CRÍTICO**: No existe campo `android_user_id` en tabla `notifications`
- **CRÍTICO**: No existe campo `app_instance_id` en tabla `notifications`
- No hay detección de instancias duales en Android
- No hay pantalla para mapear/nombrar instancias duales
- El admin no puede ver/renombrar instancias

**Código actual (Android):**

```kotlin
// PaymentNotificationListenerService.kt línea 54-65
override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName  // ✅ Se captura
    // ❌ NO se captura: sbn.getUser()?.identifier
    // ❌ NO se captura: sbn.getUid()
}
```

**Modelo de datos actual:**

- Tabla `notifications` NO tiene: `android_user_id`, `app_instance_id`, `package_name`
- Solo tiene: `source_app` (string genérico como "yape", "plin")

---

## ⚠️ 4. MVP FUNCIONAL

### 4.1 Administrador (Android + Web)

#### Login / Registro

**Estado: ✅ IMPLEMENTADO**

- ✅ Login/registro en web (`LoginPage.tsx`, `RegisterPage.tsx`)
- ✅ Login/registro en Android (`LoginActivity.kt`, `RegisterActivity.kt`)
- ✅ Autenticación con Laravel Sanctum

#### Crear Comercio

**Estado: ❌ NO IMPLEMENTADO**

- ❌ No existe pantalla/endpoint para crear comercio
- ❌ No existe modelo `Commerce`

#### Ver Feed Central de Notificaciones

**Estado: ✅ IMPLEMENTADO**

- ✅ `NotificationsPage.tsx` muestra feed de notificaciones
- ✅ Endpoint `GET /api/notifications` con paginación

#### Filtros

**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

**✅ Implementado:**

- Filtro por dispositivo (`device_id`)
- Filtro por app (`source_app`) - pero usa strings genéricos, no `packageName`
- Filtro por fechas (`start_date`, `end_date`)
- Filtro por estado (`status`)

**❌ Faltante:**

- **CRÍTICO**: Filtro por instancia (Yape 1 / Yape 2) - no existe porque no hay AppInstance
- Filtro por `packageName` específico (solo hay `source_app` genérico)

#### Vista de Dispositivos

**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

**✅ Implementado:**

- `DevicesPage.tsx` lista dispositivos
- Muestra `last_seen_at` (última actividad)
- Muestra estado `is_active`

**❌ Faltante:**

- Indicador Online/Offline (solo `last_seen_at`, no hay lógica de "online")
- Salud del servicio (permisos, batería) - no se captura/envía desde Android
- No se muestra información de instancias por dispositivo

#### Vincular Captadores (QR o código numérico)

**Estado: ❌ NO IMPLEMENTADO**

- ❌ No existe pantalla "Agregar dispositivo" con QR/código
- ❌ No existe endpoint para generar código de vinculación
- ❌ No existe flujo de escaneo QR en Android
- ❌ El registro de dispositivo actual es automático al hacer login (no hay vinculación manual)

#### Configuración: Catálogo de Apps

**Estado: ✅ IMPLEMENTADO**

- ✅ Existe `MonitorPackage` (modelo y tabla)
- ✅ Endpoint `GET /api/settings/monitored-packages` (público)
- ✅ Endpoint `GET /api/monitor-packages` (admin)
- ⚠️ No está integrado en el dashboard web (solo en API)

### 4.2 Captador (Android)

#### Modo "Vincular Dispositivo"

**Estado: ❌ NO IMPLEMENTADO**

- ❌ No existe pantalla de vinculación
- ❌ No hay escaneo de QR
- ❌ No hay ingreso de código numérico
- El dispositivo se registra automáticamente al hacer login

#### Wizard de Permisos

**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

- ✅ Se detecta si falta permiso de notificaciones
- ⚠️ No hay wizard guiado paso a paso
- ❌ No hay guía para desactivar optimización de batería
- ❌ No hay detección de OEM (MIUI, OPPO, etc.) con guías específicas (aunque existe `OemDetector.kt`, no se usa en UI)

#### Selector de Apps a Monitorear

**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

- ✅ La app consulta `GET /api/settings/monitored-packages` para obtener lista
- ✅ Se filtra por `packageName` en el servicio
- ❌ No hay UI para que el usuario seleccione qué apps monitorear
- ❌ No hay configuración por dispositivo (solo global)

#### Detección/Gestión de Instancias Duales

**Estado: ❌ NO IMPLEMENTADO**

- ❌ No se detectan instancias duales
- ❌ No hay pantalla "Instancias detectadas"
- ❌ No se permite asignar nombres (ej. "Yape 1 → Rocío")
- ❌ No se envía `androidUserId` al backend

#### Estado "Capturando OK"

**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

- ✅ Existe `ServiceStatusManager` que actualiza estado
- ✅ Se muestra en alguna UI (probablemente en `MainActivity`)
- ❌ No se muestra "último evento enviado" de forma clara
- ❌ No hay indicador visual claro de "Capturando OK"

---

## ❌ 5. MODELO DE DATOS

### Requisito vs Implementación

| Entidad Requerida      | Estado       | Observaciones                                                                                               |
| ---------------------- | ------------ | ----------------------------------------------------------------------------------------------------------- |
| **Commerce**           | ❌ NO EXISTE | Falta tabla, modelo, migración                                                                              |
| **User**               | ✅ EXISTE    | Pero falta relación con `commerce_id`                                                                       |
| **Device**             | ✅ EXISTE    | Pero falta relación con `commerce_id`                                                                       |
| **MonitoredApp**       | ⚠️ PARCIAL   | Existe `MonitorPackage` pero sin `commerceId`                                                               |
| **DeviceMonitoredApp** | ❌ NO EXISTE | No hay relación dispositivo-app                                                                             |
| **AppInstance**        | ❌ NO EXISTE | **CRÍTICO**: Falta tabla completa                                                                           |
| **NotificationEvent**  | ⚠️ PARCIAL   | Existe `Notification` pero falta: `packageName`, `androidUserId`, `appInstanceId`, `commerceId`, `postedAt` |

### Campos Faltantes en Tablas Existentes

#### Tabla `users`

- ❌ `commerce_id` (FK a `commerces`)
- ❌ `role` (admin, captador, etc.)

#### Tabla `devices`

- ❌ `commerce_id` (FK a `commerces`)
- ❌ `alias` (nombre descriptivo del dispositivo)

#### Tabla `notifications`

- ❌ `commerce_id` (FK a `commerces`)
- ❌ `package_name` (string, ej. "com.bcp.innovacxion.yapeapp")
- ❌ `android_user_id` (integer, identificador de perfil dual)
- ❌ `android_uid` (integer, opcional)
- ❌ `app_instance_id` (FK a `app_instances`)
- ❌ `posted_at` (timestamp, hora original de la notificación)
- ⚠️ `received_at` existe pero debería ser "hora backend"
- ⚠️ `source_app` existe pero es genérico (debería ser `package_name`)

### Tablas Faltantes

#### `commerces`

```sql
- id
- name
- owner_user_id (FK a users)
- created_at
- updated_at
```

#### `app_instances`

```sql
- id
- commerce_id (FK)
- device_id (FK)
- package_name
- android_user_id
- instance_label (ej. "Yape 1 (Rocío)")
- created_at
- updated_at
```

#### `device_monitored_apps`

```sql
- device_id (FK)
- package_name
- enabled (boolean)
```

---

## ❌ 6. FLUJOS DE UX

### 6.1 Admin: Alta del Comercio

**Estado: ❌ NO IMPLEMENTADO**

- No existe flujo de registro → crear comercio

### 6.2 Admin: Vincular Captador

**Estado: ❌ NO IMPLEMENTADO**

- No existe pantalla "Agregar dispositivo"
- No existe generación de QR/código
- No existe pantalla "Esperando vinculación..."

### 6.3 Captador: Vinculación y Permisos

**Estado: ❌ NO IMPLEMENTADO**

- No existe pantalla "Vincular como captador"
- No existe escaneo QR
- No existe wizard de permisos completo

### 6.4 Captador: Detección de Instancias (Dual Apps)

**Estado: ❌ NO IMPLEMENTADO**

- No existe pantalla "Instancias detectadas"
- No existe UI para asignar nombres a instancias

### 6.5 Admin: Operación Diaria

**Estado: ⚠️ PARCIALMENTE IMPLEMENTADO**

- ✅ Feed con cards de notificaciones
- ✅ Muestra App, Dispositivo, Hora, Monto
- ❌ No muestra Instancia (no existe)
- ✅ Filtros por App, Dispositivo, Fecha
- ❌ No hay filtro por Instancia

---

## ⚠️ 7. PANTALLAS CONCRETAS

### Admin (Android + Web)

| Pantalla                         | Web | Android | Estado                               |
| -------------------------------- | --- | ------- | ------------------------------------ |
| Login                            | ✅  | ✅      | Implementado                         |
| Registro                         | ✅  | ✅      | Implementado                         |
| Crear comercio                   | ❌  | ❌      | **FALTA**                            |
| Dashboard (tabs)                 | ⚠️  | ❌      | Web tiene páginas separadas, no tabs |
| Notificaciones (feed + filtros)  | ✅  | ❌      | Solo web                             |
| Detalle de notificación          | ❌  | ❌      | **FALTA**                            |
| Dispositivos (lista + salud)     | ⚠️  | ❌      | Web tiene lista básica, falta salud  |
| Agregar dispositivo (QR/código)  | ❌  | ❌      | **FALTA**                            |
| Configuración: Apps monitoreadas | ❌  | ❌      | **FALTA** (solo API)                 |

### Captador (Android)

| Pantalla                                   | Estado                                     |
| ------------------------------------------ | ------------------------------------------ |
| Vincular dispositivo (QR/código)           | ❌ **FALTA**                               |
| Wizard permisos (notificaciones + batería) | ⚠️ Parcial (solo notificaciones)           |
| Apps a monitorear (checklist)              | ❌ **FALTA**                               |
| Instancias duales (mapeo/alias)            | ❌ **FALTA**                               |
| Estado (capturando / errores)              | ⚠️ Parcial (existe `ServiceStatusManager`) |

---

## ❌ 8. REGLAS OPERATIVAS

### Requisito: Soporte para múltiples instancias de la misma app

**Estado: ❌ NO IMPLEMENTADO**

- No se captura `androidUserId`
- No se crean `AppInstance`
- No se puede distinguir entre Yape 1 y Yape 2

### Requisito: Admin puede renombrar instancias

**Estado: ❌ NO IMPLEMENTADO**

- No existe concepto de instancia
- No hay UI para renombrar

### Requisito: Nueva instancia queda "Sin asignar"

**Estado: ❌ NO IMPLEMENTADO**

- No existe lógica de detección de nuevas instancias

### Requisito: Feed muestra App + Instancia + Dispositivo

**Estado: ❌ NO IMPLEMENTADO**

- Feed actual muestra: App + Dispositivo
- Falta: Instancia

---

## ❌ 9. NOTA TÉCNICA (Android)

### Requisito

En `NotificationListenerService`, capturar:

- `sbn.getPackageName()` ✅
- `sbn.getUser()` → `UserHandle.getIdentifier()` ❌
- `sbn.getUid()` (opcional) ❌
- `sbn.getNotification().extras` → title/text ✅

### Estado: ⚠️ **PARCIALMENTE IMPLEMENTADO**

**Código actual (`PaymentNotificationListenerService.kt`):**

```kotlin
override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName  // ✅
    // ❌ FALTA: val androidUserId = sbn.user?.identifier
    // ❌ FALTA: val androidUid = sbn.uid

    val notification = sbn.notification ?: return
    val title = notification.extras?.getString("android.title") ?: ""  // ✅
    val text = notification.extras?.getCharSequence("android.text")?.toString() ?: ""  // ✅
}
```

---

## 📊 RESUMEN POR PRIORIDAD

### 🔴 CRÍTICO (Bloquea funcionalidad core)

1. **Apps Duales**: No se captura `androidUserId` → no se pueden distinguir instancias
2. **AppInstance**: No existe modelo/tabla → no se puede mapear "Yape 1 (Rocío)"
3. **Multi-tenant**: No existe `Commerce` → no hay aislamiento de datos
4. **packageName**: No se guarda en BD, solo `source_app` genérico

### 🟡 IMPORTANTE (Funcionalidad parcial)

1. **Vinculación QR**: No existe flujo de vinculación manual
2. **Wizard permisos**: Incompleto (falta batería, guías OEM)
3. **Selector apps**: No hay UI para seleccionar apps por dispositivo
4. **Salud dispositivo**: No se captura/envía información de batería/permisos
5. **Dashboard Android**: No existe app Android para admin

### 🟢 MEJORAS (Nice to have)

1. Detalle de notificación
2. Configuración de apps en dashboard web
3. Indicador online/offline más preciso
4. Exportación mejorada

---

## 🎯 RECOMENDACIONES

### Fase 1: Apps Duales (CRÍTICO)

1. Modificar `PaymentNotificationListenerService.kt` para capturar `androidUserId`
2. Crear migración para tabla `app_instances`
3. Crear modelo `AppInstance`
4. Agregar campos `android_user_id`, `app_instance_id`, `package_name` a `notifications`
5. Modificar endpoint `POST /api/notifications` para recibir estos campos
6. Crear pantalla Android para detectar/nombrar instancias
7. Agregar filtro por instancia en dashboard web

### Fase 2: Multi-tenant

1. Crear migración para tabla `commerces`
2. Agregar `commerce_id` a `users`, `devices`, `notifications`
3. Modificar queries para filtrar por `commerce_id`
4. Crear pantalla "Crear comercio" en registro

### Fase 3: Vinculación y UX

1. Implementar generación de QR/código para vinculación
2. Crear wizard de permisos completo
3. Agregar selector de apps en Android
4. Mejorar dashboard con tabs y mejor organización

---

## 📝 NOTAS FINALES

El proyecto tiene una base sólida con:

- ✅ Autenticación funcionando
- ✅ Captura de notificaciones básica
- ✅ Dashboard web funcional
- ✅ API REST bien estructurada

Sin embargo, **faltan los requisitos críticos** para el caso de uso de apps duales y multi-tenant. El sistema actual funciona para un usuario con múltiples dispositivos, pero no para:

- Múltiples comercios (tenants)
- Distinguir entre instancias duales de la misma app
- Vincular dispositivos de forma controlada (QR)

La implementación de apps duales es **absolutamente crítica** porque es el requisito principal del sistema según el prompt.


