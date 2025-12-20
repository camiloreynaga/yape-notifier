# Estado de Implementación

> Última actualización: 2025-01-21

Este documento describe el estado actual de implementación de todas las funcionalidades del proyecto.

---

## 📋 Resumen Ejecutivo

El proyecto tiene una **base sólida y funcional** con:
- ✅ Multi-tenancy implementado
- ✅ Apps duales implementado (con bug crítico a corregir)
- ✅ Sistema de vinculación QR funcionando
- ✅ Dashboard web completo
- ✅ Backend robusto con todos los endpoints

**El único bloqueador crítico es el bug en `androidUserId`** que debe corregirse para que las apps duales funcionen correctamente.

El resto de funcionalidades faltantes son mejoras de UX y funcionalidades adicionales que no bloquean el funcionamiento básico del sistema.

---

## Por Componente

### Backend (Laravel API)

| Feature | Estado | Observaciones |
|---------|--------|---------------|
| Multi-tenant con Commerce | ✅ Implementado | Tabla, modelo, migración, relaciones completas |
| AppInstance para apps duales | ✅ Implementado | Tabla completa con todos los campos |
| Sistema de vinculación QR/código | ✅ Implementado | Endpoints completos |
| Gestión de apps monitoreadas | ✅ Implementado | Modelo `MonitorPackage` con `commerce_id` |
| Salud de dispositivos | ✅ Implementado | Campos y endpoints implementados |
| Deduplicación mejorada | ✅ Implementado | Usa `package_name + android_user_id + posted_at + body` |
| Todos los endpoints necesarios | ✅ Implementado | API completa |

---

### Android App

| Feature | Estado | Observaciones |
|---------|--------|---------------|
| Captura de notificaciones | ✅ Implementado | `PaymentNotificationListenerService.kt` |
| Envío al backend | ✅ Implementado | `SendNotificationWorker` |
| Vinculación por QR/código | ✅ Implementado | `LinkDeviceActivity.kt` |
| Almacenamiento local | ✅ Implementado | Room Database |
| Bug en androidUserId | 🔴 Crítico | Usa `hashCode()` en lugar de `identifier` |
| UI para gestionar instancias | ❌ Faltante | Ver roadmap |
| UI para seleccionar apps | ❌ Faltante | Ver roadmap |
| Wizard completo de permisos | ⚠️ Parcial | Falta guías de batería y OEM |

---

### Dashboard Web

| Feature | Estado | Observaciones |
|---------|--------|---------------|
| Autenticación | ✅ Implementado | Login/registro completo |
| Feed de notificaciones con filtros | ✅ Implementado | `NotificationsPage.tsx` |
| Gestión de dispositivos | ✅ Implementado | `DevicesPage.tsx` |
| Gestión de instancias | ✅ Implementado | `AppInstancesPage.tsx` |
| Crear comercio | ✅ Implementado | `CreateCommercePage.tsx` |
| Generar códigos de vinculación | ✅ Implementado | `AddDevicePage.tsx` |
| Estadísticas y KPIs | ✅ Implementado | Dashboard completo |
| Configuración de apps monitoreadas | ⚠️ Parcial | Solo API, falta UI completa |
| Dashboard con tabs | ❌ Faltante | Actualmente páginas separadas |

---

## Por Feature Principal

### 1. Objetivo del Sistema

**Estado:** ✅ **IMPLEMENTADO** (con bug crítico)

**Implementado:**
- Panel web para visualizar notificaciones
- App Android que captura notificaciones
- Backend centralizado que recibe notificaciones
- Múltiples dispositivos por usuario
- Sistema de AppInstance para distinguir instancias duales
- Dashboard web muestra instancias y permite renombrarlas

**Bug crítico:**
- `androidUserId` usa `hashCode()` en lugar de `identifier` (ver `KNOWN_ISSUES.md`)

**Faltante:**
- App Android para administrador
- Dashboard móvil Android para admin

---

### 2. Roles y Estructura Multi-Comercio (Multi-Tenant)

**Estado:** ✅ **IMPLEMENTADO**

**Implementado:**
- Modelo `Commerce` con todas las relaciones
- Tabla `commerces` en base de datos
- Campo `commerce_id` en: `users`, `devices`, `notifications`, `app_instances`, `monitor_packages`
- Campo `role` en `users` (admin, captador)
- `CommerceService` para gestión de comercios
- Endpoints API completos
- Dashboard web tiene pantalla para crear comercio
- Todos los servicios filtran por `commerce_id` (multi-tenant)

**Parcial:**
- El registro de usuario no crea automáticamente un comercio (debe crearse después)
- No hay validación que obligue a tener comercio antes de usar el sistema

---

### 3. Apps Duales (MIUI y otros)

**Estado:** ✅ **IMPLEMENTADO** (con bug crítico)

**Backend Implementado:**
- Tabla `app_instances` con todos los campos necesarios
- Modelo `AppInstance` con método `findOrCreate`
- Campos en `notifications`: `package_name`, `android_user_id`, `android_uid`, `app_instance_id`
- `AppInstanceService` para gestión de instancias
- Endpoints API completos
- `NotificationService` crea/busca AppInstance automáticamente
- Deduplicación mejorada

**Android Implementado:**
- `CapturedNotification` tiene campos: `androidUserId`, `androidUid`, `postedAt`
- `NotificationData` incluye todos los campos dual
- `SendNotificationWorker` envía todos los campos al backend
- Migración de Room DB (v1 → v2) para nuevos campos

**Bug crítico:**
- `androidUserId` usa `hashCode()` en lugar de `identifier` (ver `KNOWN_ISSUES.md`)

**Dashboard Web:**
- Pantalla `AppInstancesPage.tsx` para gestionar instancias
- Muestra instancias asignadas y sin asignar
- Permite renombrar instancias
- Filtro por instancia en `NotificationsPage.tsx`
- Columna de instancia en tabla de notificaciones

**Faltante en Android:**
- Pantalla para detectar/nombrar instancias duales automáticamente
- UI para asignar nombres desde Android
- Detección automática de múltiples instancias del mismo package

---

### 4. MVP Funcional

#### 4.1 Administrador (Android + Web)

| Funcionalidad | Estado | Observaciones |
|---------------|--------|---------------|
| Login / Registro | ✅ Implementado | Web y Android |
| Crear Comercio | ✅ Implementado | Solo web |
| Ver Feed Central de Notificaciones | ✅ Implementado | Web completo |
| Filtros | ✅ Implementado | Todos los filtros necesarios |
| Vista de Dispositivos | ✅ Implementado | Con salud y estado |
| Vincular Captadores (QR/código) | ✅ Implementado | Web y Android |
| Configuración: Catálogo de Apps | ✅ Implementado | Backend completo, UI parcial |

#### 4.2 Captador (Android)

| Funcionalidad | Estado | Observaciones |
|---------------|--------|---------------|
| Modo "Vincular Dispositivo" | ✅ Implementado | `LinkDeviceActivity.kt` |
| Wizard de Permisos | ⚠️ Parcial | Falta guías de batería y OEM |
| Selector de Apps a Monitorear | ⚠️ Parcial | Backend existe, falta UI |
| Detección/Gestión de Instancias Duales | ⚠️ Parcial | Backend existe, falta UI Android |
| Estado "Capturando OK" | ✅ Implementado | `ServiceStatusManager` |

---

## Modelo de Datos

**Estado:** ✅ **IMPLEMENTADO COMPLETAMENTE**

| Entidad | Estado | Observaciones |
|---------|--------|---------------|
| Commerce | ✅ EXISTE | Tabla, modelo, migración, relaciones completas |
| User | ✅ EXISTE | Con `commerce_id` y `role` |
| Device | ✅ EXISTE | Con `commerce_id`, `alias`, campos de salud |
| MonitoredApp | ✅ EXISTE | `MonitorPackage` con `commerce_id` |
| DeviceMonitoredApp | ✅ EXISTE | Relación dispositivo-app implementada |
| AppInstance | ✅ EXISTE | Tabla completa con todos los campos |
| NotificationEvent | ✅ EXISTE | `Notification` con todos los campos requeridos |

---

## Flujos de UX

| Flujo | Estado | Observaciones |
|-------|--------|---------------|
| Admin: Alta del Comercio | ✅ Implementado | Flujo completo |
| Admin: Vincular Captador | ✅ Implementado | QR/código funcionando |
| Captador: Vinculación y Permisos | ✅ Implementado | Falta wizard completo |
| Captador: Detección de Instancias | ⚠️ Parcial | Backend existe, falta UI |
| Admin: Operación Diaria | ✅ Implementado | Feed completo con filtros |

---

## Pantallas Concretas

### Admin (Android + Web)

| Pantalla | Web | Android | Estado |
|----------|-----|--------|--------|
| Login | ✅ | ✅ | Implementado |
| Registro | ✅ | ✅ | Implementado |
| Crear comercio | ✅ | ❌ | Solo web |
| Dashboard (tabs) | ⚠️ | ❌ | Web tiene páginas separadas |
| Notificaciones (feed + filtros) | ✅ | ❌ | Solo web |
| Detalle de notificación | ✅ | ❌ | Solo web |
| Dispositivos (lista + salud) | ✅ | ❌ | Solo web |
| Agregar dispositivo (QR/código) | ✅ | ❌ | Solo web |
| Configuración: Apps monitoreadas | ⚠️ | ❌ | Parcial (solo API) |
| Gestión de instancias | ✅ | ❌ | Solo web |

### Captador (Android)

| Pantalla | Estado |
|----------|--------|
| Vincular dispositivo (QR/código) | ✅ Implementado |
| Wizard permisos (notificaciones + batería) | ⚠️ Parcial |
| Apps a monitorear (checklist) | ❌ Falta |
| Instancias duales (mapeo/alias) | ❌ Falta |
| Estado (capturando / errores) | ✅ Implementado |

---

## Referencias

- **Bugs conocidos**: Ver `docs/07-reference/KNOWN_ISSUES.md`
- **Roadmap**: Ver `docs/07-reference/ROADMAP.md`
- **Arquitectura**: Ver `docs/03-architecture/`

