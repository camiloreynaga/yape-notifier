# 🗑️ Guía Profesional: Limpieza de Dispositivos en Base de Datos

> **Referencias relacionadas:**
>
> - [DEPLOY_CLEAN_DEVICES_COMMAND.md](DEPLOY_CLEAN_DEVICES_COMMAND.md) - Comando de despliegue para limpiar dispositivos
> - [DEVICE_LIFECYCLE.md](../05-features/DEVICE_LIFECYCLE.md) - Ciclo de vida de dispositivos
> - [DEVICE_UNLINKING.md](../05-features/DEVICE_UNLINKING.md) - Desvinculación de dispositivos
> - [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Diagnóstico y solución de problemas

## 📋 Análisis de Relaciones y Restricciones

### Relaciones de la Tabla `devices`

La tabla `devices` tiene las siguientes relaciones con otras tablas:

| Tabla Relacionada       | Foreign Key | Política de Eliminación | Impacto                         |
| ----------------------- | ----------- | ----------------------- | ------------------------------- |
| `notifications`         | `device_id` | `CASCADE`               | Se eliminan automáticamente     |
| `app_instances`         | `device_id` | `CASCADE`               | Se eliminan automáticamente     |
| `device_monitored_apps` | `device_id` | `CASCADE`               | Se eliminan automáticamente     |
| `device_link_codes`     | `device_id` | `SET NULL`              | Se pone en NULL automáticamente |

**Conclusión:** Al eliminar un dispositivo, PostgreSQL automáticamente:

- ✅ Elimina todas sus notificaciones
- ✅ Elimina todas sus instancias de apps
- ✅ Elimina todas sus apps monitoreadas
- ✅ Pone en NULL el `device_id` en `device_link_codes`

---

## 🎯 Opciones para Limpiar Dispositivos

### **Opción 1: Comando Artisan (RECOMENDADO) ⭐**

La forma más profesional y segura es usar el comando Artisan personalizado:

```bash
# Ver qué se eliminaría (dry-run)
php artisan devices:clean --dry-run

# Eliminar con confirmación
php artisan devices:clean

# Eliminar sin confirmación (útil para scripts)
php artisan devices:clean --force
```

**Ventajas:**

- ✅ Muestra estadísticas antes de eliminar
- ✅ Modo dry-run para verificar
- ✅ Transacciones para garantizar atomicidad
- ✅ Logging de operaciones
- ✅ Manejo de errores robusto

---

### **Opción 2: Usando Eloquent (Laravel)**

Si prefieres hacerlo desde código o Tinker:

```php
use App\Models\Device;
use Illuminate\Support\Facades\DB;

// Opción A: Eliminar todos (más seguro, respeta eventos del modelo)
DB::transaction(function () {
    $count = Device::query()->delete();
    echo "Eliminados {$count} dispositivos\n";
});

// Opción B: Usando chunk para grandes volúmenes (más eficiente)
DB::transaction(function () {
    $total = 0;
    Device::chunk(1000, function ($devices) use (&$total) {
        foreach ($devices as $device) {
            $device->delete(); // Respeta eventos del modelo
            $total++;
        }
    });
    echo "Eliminados {$total} dispositivos\n";
});
```

**Desde Tinker:**

```bash
php artisan tinker
```

```php
DB::transaction(function () {
    Device::query()->delete();
});
```

---

### **Opción 3: Usando Query Builder (Más Rápido)**

Si no necesitas eventos del modelo y quieres máxima velocidad:

```php
use Illuminate\Support\Facades\DB;

DB::transaction(function () {
    $count = DB::table('devices')->delete();
    echo "Eliminados {$count} dispositivos\n";
});
```

**Desde Tinker:**

```bash
php artisan tinker
```

```php
DB::table('devices')->delete();
```

---

### **Opción 4: SQL Directo (PostgreSQL)**

Si necesitas máxima velocidad y control directo:

```sql
-- Verificar cuántos registros se eliminarán
SELECT
    (SELECT COUNT(*) FROM devices) as devices,
    (SELECT COUNT(*) FROM notifications) as notifications,
    (SELECT COUNT(*) FROM app_instances) as app_instances,
    (SELECT COUNT(*) FROM device_monitored_apps) as monitored_apps,
    (SELECT COUNT(*) FROM device_link_codes WHERE device_id IS NOT NULL) as link_codes;

-- Eliminar todos los dispositivos (CASCADE eliminará automáticamente las relaciones)
BEGIN;

DELETE FROM devices;

-- Verificar que se eliminaron
SELECT COUNT(*) FROM devices; -- Debe ser 0

-- Si todo está bien, confirmar
COMMIT;

-- Si algo salió mal, revertir
-- ROLLBACK;
```

**Desde psql:**

```bash
psql -U tu_usuario -d tu_base_de_datos
```

```sql
DELETE FROM devices;
```

---

## 🔒 Consideraciones de Seguridad y Mejores Prácticas

### 1. **Siempre usar Transacciones**

Las transacciones garantizan que si algo falla, todo se revierte:

```php
DB::beginTransaction();
try {
    Device::query()->delete();
    DB::commit();
} catch (\Exception $e) {
    DB::rollBack();
    throw $e;
}
```

### 2. **Verificar Antes de Eliminar**

Siempre verifica qué se va a eliminar:

```php
$deviceCount = Device::count();
$notificationCount = DB::table('notifications')->count();
// ... etc
```

### 3. **Backup Antes de Operaciones Destructivas**

```bash
# Backup de PostgreSQL
pg_dump -U usuario -d base_de_datos > backup_antes_limpieza_$(date +%Y%m%d_%H%M%S).sql
```

### 4. **Logging**

Registra todas las operaciones destructivas:

```php
Log::info('Limpieza de dispositivos iniciada', [
    'device_count' => Device::count(),
    'user_id' => auth()->id(),
]);
```

---

## 📊 Script de Verificación Post-Limpieza

Después de limpiar, verifica que todo se eliminó correctamente:

```sql
-- Verificar que no quedan dispositivos
SELECT COUNT(*) as devices_restantes FROM devices;

-- Verificar que no quedan notificaciones
SELECT COUNT(*) as notifications_restantes FROM notifications;

-- Verificar que no quedan app_instances
SELECT COUNT(*) as app_instances_restantes FROM app_instances;

-- Verificar que no quedan device_monitored_apps
SELECT COUNT(*) as monitored_apps_restantes FROM device_monitored_apps;

-- Verificar device_link_codes (deben tener device_id = NULL)
SELECT
    COUNT(*) as total_link_codes,
    COUNT(device_id) as link_codes_con_device,
    COUNT(*) - COUNT(device_id) as link_codes_sin_device
FROM device_link_codes;
```

---

## 🚀 Ejemplo Completo: Script de Limpieza Profesional

```php
<?php

use App\Models\Device;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/**
 * Script profesional para limpiar todos los dispositivos
 */
function cleanAllDevices(): array
{
    $stats = [
        'devices' => 0,
        'notifications' => 0,
        'app_instances' => 0,
        'monitored_apps' => 0,
        'link_codes_updated' => 0,
    ];

    DB::beginTransaction();

    try {
        // Contar antes de eliminar
        $stats['devices'] = Device::count();
        $stats['notifications'] = DB::table('notifications')->count();
        $stats['app_instances'] = DB::table('app_instances')->count();
        $stats['monitored_apps'] = DB::table('device_monitored_apps')->count();
        $stats['link_codes_updated'] = DB::table('device_link_codes')
            ->whereNotNull('device_id')
            ->count();

        // Eliminar dispositivos (CASCADE eliminará automáticamente las relaciones)
        Device::query()->delete();

        // Verificar que device_link_codes se actualizaron correctamente
        $linkCodesWithDevice = DB::table('device_link_codes')
            ->whereNotNull('device_id')
            ->count();

        if ($linkCodesWithDevice > 0) {
            throw new \Exception("Algunos device_link_codes aún tienen device_id");
        }

        DB::commit();

        Log::info('Limpieza de dispositivos completada', $stats);

        return [
            'success' => true,
            'message' => 'Dispositivos eliminados exitosamente',
            'stats' => $stats,
        ];
    } catch (\Exception $e) {
        DB::rollBack();

        Log::error('Error al limpiar dispositivos', [
            'error' => $e->getMessage(),
            'stats' => $stats,
        ]);

        return [
            'success' => false,
            'message' => $e->getMessage(),
            'stats' => $stats,
        ];
    }
}
```

---

## ⚠️ Advertencias Importantes

1. **Operación Irreversible**: Una vez eliminados, los datos no se pueden recuperar sin backup.

2. **Impacto en Producción**: Esta operación eliminará:

   - Todos los dispositivos registrados
   - Todas las notificaciones históricas
   - Todas las instancias de apps
   - Todas las configuraciones de apps monitoreadas

3. **Usuarios y Comercios**: Los usuarios y comercios NO se eliminan, solo los dispositivos asociados.

4. **Foreign Keys**: Las restricciones CASCADE garantizan integridad referencial, pero eliminan datos relacionados.

---

## 📝 Resumen de Comandos Rápidos

```bash
# Opción más segura (recomendada)
php artisan devices:clean --dry-run    # Ver qué se eliminaría
php artisan devices:clean               # Eliminar con confirmación
php artisan devices:clean --force       # Eliminar sin confirmación

# Desde Tinker
php artisan tinker
>>> Device::query()->delete();

# SQL directo
psql -U usuario -d base_de_datos
DELETE FROM devices;
```

---

## ✅ Checklist Pre-Limpieza

- [ ] Backup de la base de datos realizado
- [ ] Verificado el entorno (no producción sin confirmación)
- [ ] Revisadas las estadísticas de registros a eliminar
- [ ] Notificado al equipo si es necesario
- [ ] Modo dry-run ejecutado y verificado
- [ ] Transacciones configuradas
- [ ] Logging habilitado

---

**Última actualización:** 2025-01-27  
**Autor:** Sistema de Documentación YapeNotifier
