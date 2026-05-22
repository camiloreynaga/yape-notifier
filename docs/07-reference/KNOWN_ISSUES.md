# Bugs Conocidos

> Última actualización: 2025-01-21

Este documento lista todos los bugs conocidos del proyecto, organizados por prioridad.

---

## 🔴 Críticos (Bloquean funcionalidad core)

_No hay bugs críticos activos actualmente._

---

## ✅ Resueltos

### Bug: androidUserId - Resuelto ✅

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt:73`

**Problema original:**
- Código inicial usaba `sbn.user?.hashCode()` que es incorrecto
- `hashCode()` no es un identificador único confiable
- Las apps duales no se distinguían correctamente

**Solución implementada:**
```kotlin
@Suppress("DEPRECATION")
val androidUserId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    sbn.userId  // ✅ CORRECTO: Usa userId directamente
} else {
    null
}
```

**Estado:** ✅ **RESUELTO** (2025-01-21)

**Notas técnicas:**
- `sbn.userId` es equivalente a `sbn.user?.getIdentifier()` pero es público y accesible
- `getIdentifier()` puede ser API oculta en algunas versiones del SDK
- `userId` está deprecated desde API 29 pero sigue funcionando correctamente
- La solución actual es la mejor opción disponible

**Referencias:**
- Ver `docs/03-architecture/DUAL_APPS.md` para detalles técnicos completos
- Ver `docs/03-architecture/ANDROID_USER_ID.md` para análisis técnico detallado

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

