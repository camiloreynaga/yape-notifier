# Changelog

Registro de cambios, mejoras y correcciones implementadas en el proyecto.

---

## 2025-01-21

### ✅ Mejoras Implementadas

#### 1. Validación de Notificaciones (Fase 2 - API)

**Estado:** ✅ Completamente implementada

**Archivos creados:**
- `app/Services/PaymentNotificationValidator.php` - Validador completo con reglas de exclusión/inclusión
- `tests/Unit/PaymentNotificationValidatorTest.php` - 20+ casos de prueba, cobertura > 80%

**Archivos modificados:**
- `app/Services/NotificationService.php` - Integrado validador, marca notificaciones inválidas como `inconsistent`

**Características:**
- ✅ Validación de palabras clave de exclusión (publicidad, promociones, recordatorios)
- ✅ Validación de patrones regex de exclusión e inclusión
- ✅ Validación de montos válidos (0.01 - 1,000,000)
- ✅ Logging detallado con razón del rechazo
- ✅ Notificaciones inválidas marcadas como `status='inconsistent'` (no se rechazan completamente)

**Ejemplos de validación:**
- **Rechazadas:** "¿Ya te depositaron? 💰💰 👀👀 Ingresa al app...", "Hasta $150 dscto. 💸 Solo hoy..."
- **Aceptadas:** "JOHN DOE te envió un pago por S/ 50...", "MARIA GARCIA te ha plineado S/ 25.50"

**Referencias:**
- Ver `docs/05-features/NOTIFICATION_FILTERING.md` para documentación completa

---

#### 2. Mejoras en MonitorPackage

**Estado:** ✅ Completamente implementada

**Archivos modificados:**
- `app/Services/MonitorPackageService.php` - Filtrado por `commerce_id` en todos los métodos
- `app/Http/Controllers/MonitorPackageController.php` - Validaciones de commerce en todos los endpoints

**Mejoras:**
- ✅ Filtrado automático por `commerce_id` en todos los endpoints
- ✅ Validación de pertenencia al commerce antes de operaciones
- ✅ Asignación automática de `commerce_id` al crear
- ✅ Mensajes de error claros cuando no pertenece al commerce

**Impacto:** Garantiza multi-tenancy completo en gestión de apps monitoreadas

---

#### 3. Validación de Commerce Mejorada

**Estado:** ✅ Completamente implementada

**Archivos creados:**
- `app/Http/Middleware/RequiresCommerce.php` - Middleware para validar que el usuario tenga commerce

**Archivos modificados:**
- `app/Http/Controllers/NotificationController.php` - Validación temprana de commerce en `store()`

**Mejoras:**
- ✅ Validación temprana de commerce en operaciones críticas
- ✅ Mensajes de error claros y útiles (403 en lugar de 500)
- ✅ Middleware reutilizable para otras rutas
- ✅ Logging de intentos sin commerce

**Impacto:** Evita errores 500 y mejora experiencia de usuario con mensajes claros

---

### 📊 Estadísticas

**Código creado:**
- 1 nuevo servicio (`PaymentNotificationValidator`)
- 1 nuevo middleware (`RequiresCommerce`)
- 1 nuevo test suite (`PaymentNotificationValidatorTest`)
- **Total:** ~500 líneas de código nuevo

**Código modificado:**
- `NotificationService.php` - Integración de validador
- `MonitorPackageService.php` - Filtrado por commerce
- `MonitorPackageController.php` - Validaciones de commerce
- `NotificationController.php` - Validación de commerce
- **Total:** ~150 líneas modificadas

**Tests:**
- 20+ casos de prueba en `PaymentNotificationValidatorTest`
- Cobertura > 80%
- Todos los tests pasando ✅

---

### 🔍 Próximos Pasos Recomendados

1. **Fase 1 (Android):** Implementar filtrado en cliente Android
2. **Métricas:** Endpoint para estadísticas de rechazos por tipo
3. **Configuración remota:** Mover palabras clave a base de datos

---

## 2025-01-21 (Anterior)

### ✅ Bug Crítico Corregido

#### androidUserId - Resuelto

**Ubicación:** `apps/android-client/.../PaymentNotificationListenerService.kt:73`

**Problema:** Usaba `sbn.user?.hashCode()` que no es un identificador único confiable

**Solución:** Cambiado a `sbn.userId` (equivalente a `getIdentifier()` pero público)

**Estado:** ✅ Resuelto

**Referencias:**
- Ver `docs/03-architecture/ANDROID_USER_ID.md` para análisis técnico completo
- Ver `docs/07-reference/KNOWN_ISSUES.md` para detalles

---

## Formato de Entradas

Cada entrada debe incluir:
- **Fecha** del cambio
- **Tipo** (Mejora/Bug fix/Feature/Refactor)
- **Descripción** clara
- **Archivos** afectados
- **Impacto** en el sistema
- **Referencias** a documentación relacionada

---

**Última actualización:** 2025-01-21

