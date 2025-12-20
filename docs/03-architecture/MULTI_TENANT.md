# Sistema Multi-Tenant (Commerce)

## Descripción

El sistema implementa multi-tenancy usando el concepto de **Commerce** (comercio). Cada comercio es una unidad aislada de datos, permitiendo que múltiples negocios usen la misma instancia del sistema sin compartir información.

## Concepto: Commerce

Un **Commerce** es un tenant que agrupa:
- Usuarios (administradores y captadores)
- Dispositivos
- Notificaciones
- App Instances
- Apps monitoreadas

## Implementación

### Modelo de Datos

**Tabla principal:** `commerces`

**Campos:**
- `id`
- `name` (nombre del comercio)
- `created_at`, `updated_at`

**Relaciones:**
- `users` (1:N)
- `devices` (1:N)
- `notifications` (1:N)
- `app_instances` (1:N)
- `monitor_packages` (1:N)

### Aislamiento de Datos

Todas las tablas relacionadas tienen `commerce_id`:

- `users.commerce_id`
- `devices.commerce_id`
- `notifications.commerce_id`
- `app_instances.commerce_id`
- `monitor_packages.commerce_id`

### Filtrado Automático

Todos los servicios filtran automáticamente por `commerce_id` del usuario autenticado:

```php
// Ejemplo en NotificationService
$notifications = Notification::where('commerce_id', $user->commerce_id)
    ->where(...)
    ->get();
```

## Roles

### Administrador (Admin)

- Gestiona el comercio
- Crea dispositivos y códigos de vinculación
- Visualiza todas las notificaciones del comercio
- Gestiona apps monitoreadas

### Captador

- Dispositivo Android que captura notificaciones
- Vinculado a un comercio mediante código QR
- Solo puede enviar notificaciones al comercio al que pertenece

## Flujo de Trabajo

1. **Registro**: Usuario se registra (sin commerce)
2. **Crear Commerce**: Admin crea commerce o se asigna automáticamente
3. **Vinculación**: Dispositivos se vinculan al commerce mediante código QR
4. **Aislamiento**: Todos los datos se filtran por `commerce_id`

## Endpoints API

### Commerce

- `POST /api/commerces` - Crear comercio
- `GET /api/commerces/me` - Obtener comercio del usuario
- `GET /api/commerces/check` - Verificar si usuario tiene comercio

### Dispositivos

- `POST /api/devices/generate-link-code` - Generar código QR (admin)
- `POST /api/devices/link-by-code` - Vincular dispositivo

## Seguridad

- Los usuarios solo pueden acceder a datos de su commerce
- Los dispositivos solo pueden enviar notificaciones a su commerce
- Validación automática en todos los endpoints

## Referencias

- **Estado de implementación**: Ver `docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Arquitectura general**: Ver `docs/03-architecture/OVERVIEW.md` (pendiente)

