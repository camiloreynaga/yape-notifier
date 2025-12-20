# Prompt: Yape Notifier API - Backend Laravel

## Contexto del Sistema

Eres el desarrollador del backend Laravel 11 para el sistema **Yape Notifier**, una plataforma multi-tenant que centraliza notificaciones de pagos capturadas desde múltiples dispositivos Android. El sistema soporta apps duales (MIUI Dual Apps) y permite que múltiples comercios usen la misma instancia sin compartir datos.

## Estado Actual de Implementación

### ✅ Implementado Completamente

1. **Multi-tenancy con Commerce**
   - Modelo `Commerce` con todas las relaciones
   - Campo `commerce_id` en: `users`, `devices`, `notifications`, `app_instances`, `monitor_packages`
   - Todos los servicios filtran automáticamente por `commerce_id`
   - Endpoints: `POST /api/commerces`, `GET /api/commerces/me`, `GET /api/commerces/check`

2. **AppInstance para Apps Duales**
   - Tabla `app_instances` con constraint único: `(device_id, package_name, android_user_id)`
   - Modelo `AppInstance` con método `findOrCreate`
   - Campos en `notifications`: `package_name`, `android_user_id`, `android_uid`, `app_instance_id`
   - `NotificationService` crea/busca AppInstance automáticamente
   - Endpoints: `GET /api/app-instances`, `GET /api/devices/{id}/app-instances`, `PATCH /api/app-instances/{id}/label`

3. **Sistema de Vinculación QR/Código**
   - `DeviceLinkService` para generar y validar códigos
   - Endpoints: `POST /api/devices/generate-link-code`, `GET /api/devices/link-code/{code}`, `POST /api/devices/link-by-code`

4. **Gestión de Apps Monitoreadas**
   - Modelo `MonitorPackage` con `commerce_id`
   - Endpoint público: `GET /api/settings/monitored-packages`
   - Endpoints admin: CRUD completo de monitor packages

5. **Salud de Dispositivos**
   - Campos en `devices`: `battery_level`, `battery_optimization_disabled`, `notification_permission_enabled`, `last_heartbeat`
   - Endpoint: `POST /api/devices/{id}/health`

6. **Deduplicación Mejorada**
   - Usa: `package_name + android_user_id + posted_at + body`
   - Verifica duplicados dentro de ±5 segundos

## ⚠️ Pendiente de Implementar

### 1. Filtrado Inteligente de Notificaciones (Prioridad: Media)

**Contexto:** Actualmente el sistema recibe TODAS las notificaciones, incluyendo publicidad y promociones. Necesitamos filtrar solo pagos reales.

**Requisitos:**

1. **Crear `PaymentNotificationValidator`**
   - Ubicación: `app/Services/PaymentNotificationValidator.php`
   - Validar que la notificación sea realmente un pago (no publicidad)
   - Implementar lista de palabras clave de exclusión (ver `docs/05-features/NOTIFICATION_FILTERING.md`)
   - Implementar patrones regex de exclusión e inclusión
   - Retornar `true` si es válida, `false` si debe rechazarse
   - Incluir razón del rechazo para logging

2. **Actualizar `NotificationService`**
   - Ubicación: `app/Services/NotificationService.php`
   - Método `createNotification`: Llamar a `PaymentNotificationValidator::isValid()` antes de crear
   - Si no es válida: Log warning y lanzar `InvalidNotificationException`
   - Opcional: Guardar en tabla de notificaciones rechazadas para auditoría

3. **Crear Excepción (Opcional)**
   - Ubicación: `app/Exceptions/InvalidNotificationException.php`
   - Extender `Exception` o `ValidationException`

4. **Tests Unitarios**
   - Ubicación: `tests/Unit/PaymentNotificationValidatorTest.php`
   - Casos: pagos reales (pasan), publicidad (rechazadas), recordatorios (rechazadas), promociones (rechazadas)

**Palabras clave de exclusión:**
- Publicidad: "descuento", "dscto", "oferta", "promoción", "solo hoy", "exclusivo", "campaña"
- Recordatorios: "recuerda", "recordatorio", "no dejes", "venza", "revisa", "ingresa", "ya te depositaron"
- Cambio de moneda: "vender dólares", "comprar dólares", "cambio"
- Consumos: "realizaste un consumo", "consumo con tu tarjeta", "movimiento"

**Patrones de inclusión (solo pagos reales):**
- `.*te envió un pago por (S/|\$).*`
- `.*te ha plineado (S/|\$).*`
- `.*te (envió|transferió) (un pago|dinero) (por|de) (S/|\$).*`
- `.*recibiste (un pago|dinero) (de|por) (S/|\$).*`

**Criterios de aceptación:**
- ✅ Rechaza notificaciones de publicidad/promociones
- ✅ Rechaza notificaciones de recordatorios
- ✅ Acepta solo notificaciones de pagos reales
- ✅ Logging detallado de notificaciones rechazadas
- ✅ Tests unitarios con cobertura > 80%
- ✅ Performance: validación < 10ms

## Arquitectura y Convenciones

### Estructura de Servicios

- **Services**: Lógica de negocio (no acceso directo a DB)
- **Models**: Eloquent con relaciones y scopes
- **Controllers**: Solo orquestación, delegar a Services
- **Requests**: Validación de entrada

### Multi-tenant

- **Siempre filtrar por `commerce_id`** del usuario autenticado
- Usar `$user->commerce_id` o `$device->commerce_id`
- Nunca exponer datos de otros comercios

### Apps Duales

- **AppInstance** se crea automáticamente si hay `package_name` y `android_user_id`
- Usar `AppInstanceService::findOrCreate()` en `NotificationService`
- El `android_user_id` viene del cliente (actualmente tiene bug, pero backend debe manejarlo correctamente)

### Deduplicación

- Verificar antes de crear: `package_name + android_user_id + posted_at + body`
- Ventana de tiempo: ±5 segundos
- Marcar como `is_duplicate = true` si es duplicado

## Endpoints Existentes (Referencia)

### Autenticación
- `POST /api/register` - Registrar usuario
- `POST /api/login` - Iniciar sesión
- `POST /api/logout` - Cerrar sesión
- `GET /api/me` - Usuario autenticado

### Commerce
- `POST /api/commerces` - Crear comercio
- `GET /api/commerces/me` - Obtener comercio del usuario
- `GET /api/commerces/check` - Verificar si tiene comercio

### Dispositivos
- `GET /api/devices` - Listar dispositivos
- `POST /api/devices` - Crear dispositivo
- `POST /api/devices/generate-link-code` - Generar código QR
- `POST /api/devices/link-by-code` - Vincular dispositivo
- `POST /api/devices/{id}/health` - Actualizar salud

### Notificaciones
- `POST /api/notifications` - Crear notificación
- `GET /api/notifications` - Listar (filtros: device_id, source_app, app_instance_id, start_date, end_date, status)
- `GET /api/notifications/{id}` - Obtener notificación
- `PATCH /api/notifications/{id}/status` - Actualizar estado
- `GET /api/notifications/statistics` - Estadísticas

### App Instances
- `GET /api/app-instances` - Listar instancias del comercio
- `GET /api/devices/{deviceId}/app-instances` - Instancias de un dispositivo
- `PATCH /api/app-instances/{id}/label` - Actualizar nombre

## Testing

- Usar Pest PHP (ya configurado)
- Tests unitarios para Services
- Tests de integración para Controllers
- Cobertura objetivo: > 80%

## Documentación de Referencia

- **Filtrado de notificaciones**: `docs/05-features/NOTIFICATION_FILTERING.md`
- **Estado de implementación**: `docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Bugs conocidos**: `docs/07-reference/KNOWN_ISSUES.md`
- **Roadmap**: `docs/07-reference/ROADMAP.md`
- **Arquitectura apps duales**: `docs/03-architecture/DUAL_APPS.md`
- **Arquitectura multi-tenant**: `docs/03-architecture/MULTI_TENANT.md`

## Tareas Prioritarias

1. **Implementar filtrado de notificaciones** (ver sección Pendiente)
2. **Mejorar logging** de notificaciones rechazadas
3. **Agregar métricas** de notificaciones filtradas (opcional)
4. **Optimizar queries** de notificaciones con índices apropiados

## Notas Importantes

- El sistema ya está funcional y en producción
- Los cambios deben ser backward compatible
- Siempre mantener el filtrado multi-tenant
- Las notificaciones rechazadas deben loguearse para auditoría
- Considerar performance: el filtrado debe ser rápido (< 10ms)

