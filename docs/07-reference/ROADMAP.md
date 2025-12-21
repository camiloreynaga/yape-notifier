# Roadmap

> Última actualización: 2025-01-21

Este documento lista las funcionalidades pendientes y mejoras planificadas, organizadas por prioridad.

---

## 🔴 Crítico (Bloquea funcionalidad core)

_No hay pendientes críticos actualmente._

---

## 🟡 Importante (Funcionalidad parcial)

### 1. Pantalla Android para gestionar instancias duales

**Descripción:**
- Detectar instancias automáticamente
- Permitir asignar nombres a instancias desde la app Android
- Mostrar lista de instancias encontradas
- UI para mapear instancias (ej. "Yape 1 → Rocío")

**Componente:** Android App

**Estimación:** 5 días

**Estado:** Pendiente

**Referencias:**
- Ver `docs/03-architecture/DUAL_APPS.md` para detalles técnicos

---

### 2. Wizard completo de permisos en Android

**Descripción:**
- Guía paso a paso completa
- Instrucciones específicas para desactivar optimización de batería
- Detección de OEM (MIUI, OPPO, etc.) con guías específicas
- Instrucciones visuales para cada paso

**Componente:** Android App

**Estimación:** 3 días

**Estado:** Pendiente

**Notas:** Ya existe detección básica de permisos, falta wizard completo

---

### 3. Selector de apps en Android

**Descripción:**
- UI para que el usuario seleccione qué apps monitorear
- Configuración por dispositivo
- Pantalla de configuración de apps por dispositivo

**Componente:** Android App

**Estimación:** 3 días

**Estado:** Pendiente

**Notas:** La funcionalidad backend ya existe, falta UI

---

### 4. App Android para administrador

**Descripción:**
- Dashboard móvil para administradores
- Gestión de dispositivos desde móvil
- Visualización de notificaciones
- Gestión de instancias desde móvil

**Componente:** Android App (nueva app o modo admin)

**Estimación:** 10 días

**Estado:** Pendiente

**Notas:** Actualmente solo existe app para captadores

---

## 🟢 Mejoras (Nice to have)

### 1. Dashboard web con tabs

**Descripción:** Reorganizar dashboard web con tabs en lugar de páginas separadas

**Componente:** Web Dashboard

**Estimación:** 2 días

**Estado:** Pendiente

---

### 2. UI completa para configuración de apps monitoreadas

**Descripción:** Completar UI en dashboard web para gestionar apps monitoreadas

**Componente:** Web Dashboard

**Estimación:** 2 días

**Estado:** Pendiente

**Notas:** La funcionalidad backend existe, falta UI completa

---

### 3. Mejoras en indicadores de estado

**Descripción:**
- Mostrar "último evento enviado" de forma más clara
- Indicador visual más prominente de "Capturando OK"
- Indicador online/offline más preciso

**Componente:** Android App, Web Dashboard

**Estimación:** 2 días

**Estado:** Pendiente

---

### 4. Exportación mejorada

**Descripción:** Mejorar funcionalidad de exportación (Excel, más formatos)

**Componente:** Web Dashboard

**Estimación:** 2 días

**Estado:** Pendiente

---

## ✅ Completado

### Bugs Corregidos

1. **Corregir bug androidUserId** ✅ (2025-01-21)
   - Cambiado de `hashCode()` a `sbn.userId`
   - Ubicación: `PaymentNotificationListenerService.kt:73`
   - Ver `docs/03-architecture/ANDROID_USER_ID.md` para análisis técnico
   - Ver `docs/07-reference/KNOWN_ISSUES.md` para detalles

### Features Completadas

1. **Validación de Notificaciones (Fase 2 - API)** ✅ (2025-01-21)
   - `PaymentNotificationValidator` implementado
   - Filtrado de publicidad/promociones en servidor
   - Tests unitarios con cobertura > 80%
   - Ver `docs/05-features/NOTIFICATION_FILTERING.md` para detalles
   - Ver `docs/07-reference/CHANGELOG.md` para changelog completo

2. **Mejoras en MonitorPackage** ✅ (2025-01-21)
   - Filtrado automático por `commerce_id`
   - Validación de pertenencia al commerce
   - Asignación automática de `commerce_id`
   - Ver `docs/07-reference/CHANGELOG.md` para detalles

3. **Validación de Commerce Mejorada** ✅ (2025-01-21)
   - Middleware `RequiresCommerce` creado
   - Validación temprana en operaciones críticas
   - Mensajes de error mejorados (403 en lugar de 500)
   - Ver `docs/07-reference/CHANGELOG.md` para detalles

---

## Cómo agregar nuevos items al roadmap

1. Agregar a la sección correspondiente según prioridad
2. Incluir:
   - Descripción clara
   - Componente afectado
   - Estimación (días)
   - Estado (Pendiente/En progreso/Completado)
   - Referencias a documentación relacionada

---

## Referencias

- **Bugs conocidos**: Ver `docs/07-reference/KNOWN_ISSUES.md`
- **Estado de implementación**: Ver `docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Arquitectura**: Ver `docs/03-architecture/`

