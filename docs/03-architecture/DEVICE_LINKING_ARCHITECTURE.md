# 🏗️ Arquitectura de Vinculación de Dispositivos

## 📋 Resumen Ejecutivo

Este documento describe la **arquitectura profesional** implementada para la vinculación de dispositivos, explicando las decisiones de diseño y los principios aplicados.

**Versión:** 2.0 - Enfoque Flexible  
**Fecha:** 2025-12-28  
**Autor:** Senior Architect Review

---

## 🎯 Principios de Diseño

### 1. Separación de Responsabilidades

**Registro de Dispositivo ≠ Vinculación a Comercio**

- **Registro**: Asociar dispositivo a un usuario (opcional, para trazabilidad)
- **Vinculación**: Asociar dispositivo a un comercio (requerido, para funcionalidad)

Estas son operaciones **independientes** que pueden ocurrir en cualquier orden.

### 2. Código QR como Mecanismo de Autorización

**El código QR es el mecanismo de autorización principal**, no la autenticación del usuario.

- ✅ El código QR tiene expiración (24 horas)
- ✅ El código QR tiene uso único
- ✅ El código QR está asociado a un comercio específico
- ✅ No requiere autenticación previa del usuario

### 3. Find-or-Create Pattern

**El backend implementa un patrón find-or-create** para flexibilidad:

```php
// Pseudocódigo
if (device exists by UUID) {
    update device with commerce_id
} else {
    create device with commerce_id
}
```

**Ventajas:**
- ✅ Reduce fricción en el onboarding
- ✅ Permite vinculación sin login previo
- ✅ Evita errores de "dispositivo no encontrado"
- ✅ Mantiene consistencia de datos

### 4. Autenticación Opcional

**La autenticación es opcional** pero recomendada para trazabilidad:

- **Sin autenticación**: Dispositivo vinculado directamente al comercio
- **Con autenticación**: Dispositivo vinculado al comercio + asociado al usuario

**Casos de uso:**
- **Modo Capturer**: Empleado vincula su teléfono sin cuenta → Sin autenticación
- **Modo Admin**: Administrador vincula dispositivo personal → Con autenticación

---

## 🔄 Flujo Arquitectónico

### Flujo Completo

```
┌─────────────────────────────────────────────────────────┐
│                    USUARIO                              │
│  (Opcional - para trazabilidad)                        │
└─────────────────────────────────────────────────────────┘
           │
           │ (opcional)
           │
┌─────────────────────────────────────────────────────────┐
│                    DISPOSITIVO                           │
│  (Identificado por UUID único por instalación)          │
│  - UUID: Generado una vez al instalar la app            │
│  - Persistido localmente en PreferencesManager         │
└─────────────────────────────────────────────────────────┘
           │                    │
           │                    │
    ┌──────▼──────┐      ┌──────▼──────┐
    │   USUARIO   │      │  COMERCIO   │
    │ (opcional)  │      │ (requerido) │
    │             │      │             │
    │ user_id     │      │ commerce_id │
    │ (trazabilidad)    │ (funcionalidad)
    └─────────────┘      └─────────────┘
```

### Flujo de Vinculación

```
1. Admin genera código QR
   ↓
2. Código QR contiene:
   - code: 8 caracteres alfanuméricos
   - commerce_id: ID del comercio
   - expires_at: 24 horas
   ↓
3. Usuario escanea código QR
   ↓
4. App valida código (GET /api/devices/link-code/{code})
   ↓
5. App envía vinculación (POST /api/devices/link-by-code)
   - code: Código escaneado
   - device_uuid: UUID del dispositivo
   - device_name: Nombre del dispositivo (opcional)
   ↓
6. Backend procesa:
   a. Valida código (expiración, uso único)
   b. Busca dispositivo por UUID
   c. Si no existe → Crea dispositivo con commerce_id
   d. Si existe → Actualiza commerce_id
   e. Si usuario autenticado → Asocia user_id
   ↓
7. Backend marca código como usado
   ↓
8. ✅ Dispositivo vinculado exitosamente
```

---

## 🔐 Seguridad y Autorización

### Mecanismos de Seguridad

1. **Código QR con Expiración**
   - Código válido por 24 horas
   - Se invalida automáticamente después de expirar

2. **Uso Único**
   - Cada código solo puede usarse una vez
   - Previene reutilización de códigos

3. **Asociación a Comercio**
   - Cada código está asociado a un comercio específico
   - Previene vinculación a comercios incorrectos

4. **Validación de UUID**
   - UUID debe tener formato válido
   - UUID es único por instalación de app

### Niveles de Autorización

| Nivel | Requisito | Propósito |
|-------|-----------|-----------|
| **Nivel 1: Código QR** | Código válido | Autorizar vinculación básica |
| **Nivel 2: Autenticación** | Token de usuario | Trazabilidad y asociación a usuario |
| **Nivel 3: Admin** | Rol de administrador | Generar códigos de vinculación |

---

## 📊 Modelo de Datos

### Relaciones

```
Device
├── user_id (nullable) → User
├── commerce_id (required) → Commerce
└── uuid (unique, required)

DeviceLinkCode
├── commerce_id (required) → Commerce
├── device_id (nullable) → Device (after linking)
├── code (unique, required)
├── expires_at (required)
└── used_at (nullable)
```

### Estados del Dispositivo

| Estado | user_id | commerce_id | Descripción |
|--------|---------|-------------|-------------|
| **Sin vincular** | null | null | Dispositivo recién creado, no vinculado |
| **Vinculado directo** | null | ✅ | Vinculado sin autenticación (modo capturer) |
| **Vinculado con usuario** | ✅ | ✅ | Vinculado con autenticación (trazabilidad) |

---

## 🎨 Decisiones de Arquitectura

### ¿Por qué Find-or-Create?

**Problema anterior:**
- Dispositivo debía existir antes de vincularlo
- Requería login previo
- Fricción innecesaria para empleados

**Solución implementada:**
- Backend crea dispositivo automáticamente si no existe
- No requiere login previo
- Reduce fricción en onboarding

**Trade-offs:**
- ✅ Ventaja: UX mejorada, menos fricción
- ✅ Ventaja: Flexibilidad para diferentes casos de uso
- ⚠️ Consideración: Algunos dispositivos pueden no tener user_id (OK para modo capturer)

### ¿Por qué Autenticación Opcional?

**Problema anterior:**
- Todos los dispositivos requerían autenticación
- Empleados necesitaban crear cuentas solo para vincular

**Solución implementada:**
- Autenticación es opcional
- Código QR es el mecanismo de autorización principal
- Si hay autenticación, se asocia para trazabilidad

**Trade-offs:**
- ✅ Ventaja: UX mejorada para modo capturer
- ✅ Ventaja: Flexibilidad para diferentes flujos
- ⚠️ Consideración: Algunos dispositivos pueden no tener user_id (aceptable)

---

## 🔍 Validaciones y Reglas de Negocio

### Validaciones del Backend

1. **Código de Vinculación**
   - ✅ Debe existir en la base de datos
   - ✅ No debe estar expirado (24 horas)
   - ✅ No debe estar usado previamente
   - ✅ Debe pertenecer al comercio correcto

2. **UUID del Dispositivo**
   - ✅ Debe tener formato válido (UUID v4)
   - ✅ Debe ser único por instalación

3. **Dispositivo Existente**
   - ✅ Si existe y tiene commerce_id diferente → Error
   - ✅ Si existe y commerce_id es null → Actualizar
   - ✅ Si no existe → Crear automáticamente

4. **Usuario Autenticado (Opcional)**
   - ✅ Si está autenticado → Asociar user_id
   - ✅ Si no está autenticado → user_id = null (OK)

---

## 📝 Logs y Trazabilidad

### Eventos Registrados

1. **Generación de Código**
   ```php
   Log::info('Device link code generated', [
       'code' => $code,
       'commerce_id' => $commerceId,
       'expires_at' => $expiresAt,
   ]);
   ```

2. **Validación de Código**
   ```php
   Log::info('Device link code validated', [
       'code' => $code,
       'commerce_id' => $linkCode->commerce_id,
   ]);
   ```

3. **Creación Automática de Dispositivo**
   ```php
   Log::info('Device created automatically during link', [
       'device_id' => $device->id,
       'device_uuid' => $deviceUuid,
       'commerce_id' => $linkCode->commerce_id,
       'user_id' => $user?->id,
   ]);
   ```

4. **Vinculación Exitosa**
   ```php
   Log::info('Device linked to commerce via code', [
       'device_id' => $device->id,
       'device_uuid' => $deviceUuid,
       'commerce_id' => $linkCode->commerce_id,
       'code' => $code,
       'user_id' => $user?->id,
       'was_created' => !$device->wasRecentlyCreated,
   ]);
   ```

---

## ✅ Checklist de Implementación

### Backend

- [x] DeviceLinkService implementa find-or-create pattern
- [x] Autenticación es opcional en linkByCode endpoint
- [x] Validaciones de código QR (expiración, uso único)
- [x] Validación de formato UUID
- [x] Logs completos para trazabilidad
- [x] Manejo de errores robusto

### Frontend

- [x] LinkDeviceViewModel simplificado (sin lógica de registro manual)
- [x] Uso de ApiCallHandler para manejo de errores type-safe
- [x] Envío de device_name opcional
- [x] Manejo de estados de vinculación (Idle, Linking, Success, Error)
- [x] Logs para debugging

### Documentación

- [x] `docs/05-features/DEVICE_LINKING.md` - Documentación técnica general
- [x] `docs/05-features/DEVICE_LINKING_GUIDE.md` - Guía práctica paso a paso
- [x] `docs/03-architecture/DEVICE_LINKING_ARCHITECTURE.md` - Arquitectura y decisiones de diseño (este documento)

---

## 🚀 Próximos Pasos (Opcional)

### Mejoras Futuras

1. **Rate Limiting**
   - Limitar intentos de vinculación por dispositivo
   - Prevenir abuso de códigos

2. **Notificaciones**
   - Notificar al admin cuando un dispositivo se vincula
   - Notificar al usuario si está autenticado

3. **Auditoría**
   - Historial de vinculaciones
   - Tracking de cambios de commerce_id

4. **Validaciones Adicionales**
   - Verificar que el dispositivo no está vinculado a otro comercio activo
   - Validar que el comercio existe y está activo

---

**Última actualización:** 2025-12-28  
**Versión:** 2.0  
**Estado:** ✅ Implementado y Documentado

