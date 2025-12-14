# Monitor Packages - Documentación

## Descripción

Sistema de gestión de paquetes monitoreados (monitor packages) que permite configurar qué aplicaciones Android deben ser monitoreadas para recibir notificaciones de pago.

## Estructura

### Base de Datos

**Tabla:** `monitor_packages`

- `id`: ID único
- `package_name`: Nombre del paquete Android (único, formato: `com.example.app`)
- `app_name`: Nombre descriptivo de la aplicación (opcional)
- `description`: Descripción de la aplicación (opcional)
- `is_active`: Estado activo/inactivo (boolean)
- `priority`: Prioridad para ordenamiento (integer, 0-100)
- `created_at`, `updated_at`: Timestamps

### Modelo

**Archivo:** `app/Models/MonitorPackage.php`

- Scopes: `active()`, `ordered()`
- Casts: `is_active` (boolean), `priority` (integer)

### Servicio

**Archivo:** `app/Services/MonitorPackageService.php`

Métodos principales:
- `getActivePackagesArray()`: Obtiene array simple de packages activos (para clientes)
- `getAllPackages()`: Obtiene todos los packages (para admin)
- `createPackage()`: Crea un nuevo package
- `updatePackage()`: Actualiza un package
- `deletePackage()`: Elimina un package
- `togglePackageStatus()`: Activa/desactiva un package
- `bulkCreatePackages()`: Crea múltiples packages a la vez

## Endpoints API

### Público (sin autenticación)

#### GET `/api/settings/monitored-packages`

Obtiene la lista de packages activos para que los clientes Android los consuman.

**Respuesta:**
```json
{
  "packages": [
    "com.yapenotifier.android",
    "pe.com.interbank.mobilebanking",
    "com.bcp.bancadigital",
    "com.bbva.bbvacontinental",
    "com.scotiabank.mobile",
    "com.yape.android",
    "com.bcp.innovacxion.yapeapp",
    "com.plin.android"
  ]
}
```

### Protegidos (requieren autenticación)

#### GET `/api/monitor-packages`

Lista todos los monitor packages.

**Query Parameters:**
- `active_only` (boolean, opcional): Filtrar solo activos

**Respuesta:**
```json
{
  "packages": [
    {
      "id": 1,
      "package_name": "com.yape.android",
      "app_name": "Yape",
      "description": "Aplicación oficial de Yape",
      "is_active": true,
      "priority": 95,
      "created_at": "2025-12-15T10:00:00.000000Z",
      "updated_at": "2025-12-15T10:00:00.000000Z"
    }
  ]
}
```

#### POST `/api/monitor-packages`

Crea un nuevo monitor package.

**Body:**
```json
{
  "package_name": "com.example.app",
  "app_name": "Example App",
  "description": "Descripción opcional",
  "is_active": true,
  "priority": 50
}
```

**Validaciones:**
- `package_name`: Requerido, único, formato Android válido (regex: `/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/`)
- `app_name`: Opcional, máximo 255 caracteres
- `description`: Opcional, máximo 1000 caracteres
- `is_active`: Opcional, boolean (default: true)
- `priority`: Opcional, integer 0-100 (default: 0)

#### GET `/api/monitor-packages/{id}`

Obtiene un monitor package específico.

#### PUT/PATCH `/api/monitor-packages/{id}`

Actualiza un monitor package.

**Body:** (mismos campos que POST, todos opcionales)

#### DELETE `/api/monitor-packages/{id}`

Elimina un monitor package.

#### POST `/api/monitor-packages/{id}/toggle-status`

Activa o desactiva un monitor package.

**Body:**
```json
{
  "is_active": true
}
```

Si no se envía `is_active`, alterna el estado actual.

#### POST `/api/monitor-packages/bulk-create`

Crea múltiples packages a la vez.

**Body:**
```json
{
  "packages": [
    "com.example.app1",
    "com.example.app2",
    "com.example.app3"
  ]
}
```

**Respuesta:**
```json
{
  "message": "Packages created successfully",
  "created_count": 3,
  "packages": [...]
}
```

**Nota:** Los packages que ya existen se omiten automáticamente.

## Migración y Seeder

### Ejecutar migración

```bash
php artisan migrate
```

### Ejecutar seeder (poblar datos iniciales)

```bash
php artisan db:seed --class=MonitorPackageSeeder
```

El seeder incluye los siguientes packages por defecto:
- com.yapenotifier.android
- pe.com.interbank.mobilebanking
- com.bcp.bancadigital
- com.bbva.bbvacontinental
- com.scotiabank.mobile
- com.yape.android
- com.bcp.innovacxion.yapeapp
- com.plin.android

## Uso en el Cliente Android

El cliente Android ya está configurado para consumir el endpoint público:

```kotlin
// En ApiService.kt
@GET("api/settings/monitored-packages")
suspend fun getMonitoredPackages(): Response<MonitoredPackagesResponse>

// El cliente llama automáticamente a este endpoint
// y actualiza la lista de packages monitoreados
```

## Ejemplos de Uso

### Crear un package

```bash
curl -X POST http://localhost:8000/api/monitor-packages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "package_name": "com.example.app",
    "app_name": "Example App",
    "is_active": true
  }'
```

### Obtener packages activos (público)

```bash
curl http://localhost:8000/api/settings/monitored-packages
```

### Actualizar un package

```bash
curl -X PUT http://localhost:8000/api/monitor-packages/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "app_name": "Updated App Name",
    "priority": 100
  }'
```

### Desactivar un package

```bash
curl -X POST http://localhost:8000/api/monitor-packages/1/toggle-status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "is_active": false
  }'
```

## Notas de Implementación

1. **Validación de Package Names**: Se valida que el formato sea un package name de Android válido usando regex.

2. **Unicidad**: El `package_name` debe ser único en la base de datos.

3. **Endpoint Público**: El endpoint `/api/settings/monitored-packages` es público para que los clientes Android puedan obtener la lista sin autenticación.

4. **Ordenamiento**: Los packages se ordenan por `priority` (descendente) y luego por `package_name` (ascendente).

5. **Bulk Create**: El método `bulkCreate` omite automáticamente los packages que ya existen, evitando duplicados.

