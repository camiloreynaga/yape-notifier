# Sistema de Apps Duales

## Descripción

El sistema soporta apps duales (MIUI Dual Apps, Parallel Apps, etc.) donde un mismo dispositivo puede tener múltiples instancias de la misma app de pago (ej: Yape 1 y Yape 2).

## Concepto: AppInstance

Una **AppInstance** identifica de forma única una instancia de una app en un dispositivo:

```
AppInstance = (device_id + package_name + android_user_id)
```

### Campos Clave

- **`device_id`**: UUID del dispositivo Android
- **`package_name`**: Package de la app (ej: `com.bcp.innovacxion.yapeapp`)
- **`android_user_id`**: Identificador del perfil dual (0 = perfil principal, >0 = perfil dual)

## Implementación

### Backend

**Modelo:** `App\Models\AppInstance`

**Tabla:** `app_instances`

**Campos:**
- `id`
- `commerce_id` (FK)
- `device_id` (FK)
- `package_name`
- `android_user_id`
- `instance_label` (ej: "Yape 1 (Rocío)")
- `created_at`, `updated_at`

**Constraint único:** `(device_id, package_name, android_user_id)`

**Endpoints API:**
- `GET /api/app-instances` - Listar instancias del comercio
- `GET /api/devices/{id}/app-instances` - Instancias de un dispositivo
- `PATCH /api/app-instances/{id}/label` - Actualizar nombre de instancia

### Android

**Captura de datos:**
- `packageName`: `sbn.packageName`
- `androidUserId`: `sbn.userId` ✅ **CORRECTO**: Usa `userId` directamente (equivalente a `getIdentifier()` pero público)
- `androidUid`: `sbn.uid` (opcional)

**Implementación actual:**
```kotlin
@Suppress("DEPRECATION")
val androidUserId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    sbn.userId  // ✅ CORRECTO: Público, funcional, confiable
} else {
    null
}
```

**⚠️ IMPORTANTE - Opciones NO recomendadas:**
- ❌ `sbn.user?.hashCode()` - NO es un identificador único confiable
- ❌ `sbn.user?.getIdentifier()` - Puede ser API oculta (error de compilación)
- ✅ `sbn.userId` - **Usar esta opción** (público, funcional, equivalente a `getIdentifier()`)

**Ver análisis técnico completo:** `docs/03-architecture/ANDROID_USER_ID.md`

**Almacenamiento local:**
- `CapturedNotification` incluye todos los campos dual
- Room Database con migración v1 → v2

**Envío al backend:**
- `SendNotificationWorker` envía todos los campos dual
- Backend crea/busca AppInstance automáticamente

### Dashboard Web

- Pantalla `AppInstancesPage.tsx` para gestionar instancias
- Muestra instancias asignadas y sin asignar
- Permite renombrar instancias
- Filtro por instancia en `NotificationsPage.tsx`

## Flujo de Trabajo

1. **Captura**: Android captura notificación con `package_name` y `android_user_id`
2. **Envío**: Android envía notificación al backend con todos los campos dual
3. **Creación automática**: Backend busca/crea AppInstance si no existe
4. **Asignación**: Admin puede renombrar instancia desde dashboard web
5. **Filtrado**: Notificaciones se pueden filtrar por instancia

## Deduplicación

Las notificaciones se consideran duplicadas si tienen:
- Mismo `device_id`
- Mismo `package_name`
- Mismo `android_user_id`
- Mismo `body`
- `posted_at` dentro de ±5 segundos

## ✅ Estado Actual

**Implementación:** ✅ **CORRECTA**

El código actual usa `sbn.userId` que es la solución correcta y funcional. Este valor es equivalente a `getIdentifier()` pero es público y accesible sin requerir APIs ocultas.

**Historial:**
- ❌ Versión inicial usaba `hashCode()` (incorrecto)
- ✅ Corregido a `sbn.userId` (correcto y funcional)

Ver `docs/03-architecture/ANDROID_USER_ID.md` para análisis técnico detallado de todas las opciones.

## Referencias

- **Análisis técnico androidUserId**: Ver `docs/03-architecture/ANDROID_USER_ID.md`
- **Bugs conocidos**: Ver `docs/07-reference/KNOWN_ISSUES.md`
- **Estado de implementación**: Ver `docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Roadmap**: Ver `docs/07-reference/ROADMAP.md`

