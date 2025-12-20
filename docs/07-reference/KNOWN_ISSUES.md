# Bugs Conocidos

> Última actualización: 2025-01-21

Este documento lista todos los bugs conocidos del proyecto, organizados por prioridad.

---

## 🔴 Críticos (Bloquean funcionalidad core)

### Bug: androidUserId usa hashCode() en lugar de identifier

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt:67`

**Código actual (incorrecto):**
```kotlin
val androidUserId = sbn.user?.hashCode() // ❌ INCORRECTO
```

**Código correcto:**
```kotlin
val androidUserId = sbn.user?.identifier // ✅ CORRECTO
```

**Impacto:**
- `hashCode()` no es el identificador único del UserHandle
- Las instancias duales no se distinguen correctamente
- AppInstance se crea con identificador incorrecto
- El sistema de apps duales no funciona correctamente

**Solución:**
1. Cambiar línea 67 de `PaymentNotificationListenerService.kt`
2. Verificar que `identifier` esté disponible en la versión de Android SDK usada
3. Probar con dispositivos MIUI reales

**Estado:** Pendiente de corrección

**Referencias:**
- Ver `docs/03-architecture/DUAL_APPS.md` para más detalles sobre apps duales
- Ver `docs/07-reference/ROADMAP.md` para priorización

---

## 🟡 Importantes (Afectan funcionalidad parcial)

_No hay bugs importantes reportados actualmente._

---

## 🟢 Menores (No bloquean funcionalidad)

_No hay bugs menores reportados actualmente._

---

## ✅ Resueltos

_Lista de bugs que han sido corregidos:_

_No hay bugs resueltos documentados aún._

---

## Cómo reportar un nuevo bug

1. Verificar que el bug no esté ya listado aquí
2. Agregar el bug a la sección correspondiente según su prioridad
3. Incluir:
   - Título descriptivo
   - Ubicación exacta (archivo y línea)
   - Impacto
   - Pasos para reproducir (si aplica)
   - Solución propuesta (si se conoce)
   - Estado (Pendiente/En progreso/Resuelto)

---

## Referencias

- **Estado de implementación**: Ver `docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Roadmap**: Ver `docs/07-reference/ROADMAP.md`
- **Arquitectura**: Ver `docs/03-architecture/`

