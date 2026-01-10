# 🔒 Guía Segura: Migrar Paquetes Globales a Commerces

Guía paso a paso para probar y ejecutar el comando de migración de paquetes globales **sin afectar la versión de producción**.

---

## 📋 Estrategias de Prueba Segura

### Opción 1: Usar Entorno de Staging (Recomendado) ⭐

Si tienes un entorno de staging configurado, úsalo primero:

```bash
# Conectarse al servidor
ssh deploy@tu-servidor

# Ir a staging
cd /var/apps/yape-notifier/infra/docker/environments/staging

# Probar el comando en staging primero
docker compose --env-file .env exec php-fpm php artisan monitor-packages:migrate-global
```

**Ventajas:**
- ✅ No afecta producción
- ✅ Puedes probar múltiples veces
- ✅ Mismo código que producción

---

### Opción 2: Backup + Prueba en Producción (Si no hay staging)

Si no tienes staging, sigue estos pasos **estrictamente**:

#### **PASO 1: Crear Backup Completo** 🔐

```bash
# Conectarse al servidor
ssh deploy@tu-servidor

# Ir a producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Crear backup de la base de datos
./backup.sh

# Verificar que el backup se creó
ls -lh backups/backup_*.sql.gz | tail -1
```

**⚠️ IMPORTANTE:** Guarda la ruta del backup para rollback si es necesario.

#### **PASO 2: Verificar Estado Actual** 📊

```bash
# Entrar al contenedor PHP
docker compose --env-file .env exec php-fpm bash

# Verificar paquetes globales existentes
php artisan tinker
```

```php
// En tinker, ejecutar:
\App\Models\MonitorPackage::whereNull('commerce_id')->count();
\App\Models\MonitorPackage::whereNull('commerce_id')->get(['id', 'package_name', 'app_name']);

// Ver commerces existentes
\App\Models\Commerce::count();
\App\Models\Commerce::get(['id', 'name']);

// Ver paquetes por commerce (ejemplo commerce_id = 1)
\App\Models\MonitorPackage::where('commerce_id', 1)->count();

// Salir de tinker
exit
```

```bash
# Salir del contenedor
exit
```

#### **PASO 3: Ejecutar Migración (Sin --delete-global)** 🚀

```bash
# Ejecutar migración SIN eliminar paquetes globales
docker compose --env-file .env exec php-fpm php artisan monitor-packages:migrate-global

# El comando mostrará:
# - Cuántos paquetes globales encontró
# - Cuántos commerces procesará
# - Progreso en tiempo real
# - Resumen final
```

**El comando usa transacciones**, así que si falla, todo se revierte automáticamente.

#### **PASO 4: Verificar Resultados** ✅

```bash
# Entrar al contenedor PHP
docker compose --env-file .env exec php-fpm bash

# Verificar que los paquetes se copiaron
php artisan tinker
```

```php
// Verificar que cada commerce tiene los paquetes
\App\Models\Commerce::all()->each(function($commerce) {
    $count = \App\Models\MonitorPackage::where('commerce_id', $commerce->id)->count();
    echo "Commerce {$commerce->name} (ID: {$commerce->id}): {$count} paquetes\n";
});

// Verificar que los paquetes globales siguen existiendo
\App\Models\MonitorPackage::whereNull('commerce_id')->count();

// Ver un ejemplo de paquete copiado
\App\Models\MonitorPackage::where('commerce_id', 1)
    ->where('package_name', 'com.yape.android')
    ->first();

exit
```

```bash
exit
```

#### **PASO 5: Probar en el Dashboard** 🖥️

1. Acceder al dashboard: `https://dashboard.notificaciones.space`
2. Ir a **Configuración** → **Apps Monitoreadas**
3. Verificar que aparecen los paquetes (deberían ser 8)
4. Verificar que puedes activar/desactivar paquetes
5. Verificar que puedes crear nuevos paquetes

#### **PASO 6: Si Todo Está Bien, Eliminar Paquetes Globales** 🗑️

**Solo después de verificar que todo funciona correctamente:**

```bash
# Ejecutar migración Y eliminar paquetes globales
docker compose --env-file .env exec php-fpm php artisan monitor-packages:migrate-global --delete-global
```

**⚠️ ADVERTENCIA:** Esta acción es **irreversible**. Asegúrate de que:
- ✅ Todos los commerces tienen sus paquetes
- ✅ El dashboard funciona correctamente
- ✅ Los dispositivos Android pueden obtener los paquetes

---

## 🔄 Rollback (Si Algo Sale Mal)

Si necesitas revertir los cambios:

### Opción A: Restaurar desde Backup

```bash
# Identificar el backup más reciente
ls -lh backups/backup_*.sql.gz | tail -1

# Restaurar backup (ejemplo con fecha)
BACKUP_FILE="backups/backup_20250115_143022.sql.gz"

# Restaurar
gunzip < "$BACKUP_FILE" | docker compose --env-file .env exec -T db psql -U postgres yape_notifier

# Verificar restauración
docker compose --env-file .env exec php-fpm php artisan tinker
```

```php
// Verificar que los paquetes globales volvieron
\App\Models\MonitorPackage::whereNull('commerce_id')->count();

exit
```

### Opción B: Eliminar Paquetes Copiados Manualmente

```bash
docker compose --env-file .env exec php-fpm php artisan tinker
```

```php
// Eliminar todos los paquetes con commerce_id (mantener solo globales)
\App\Models\MonitorPackage::whereNotNull('commerce_id')->delete();

// Verificar
\App\Models\MonitorPackage::whereNull('commerce_id')->count();

exit
```

---

## 🧪 Prueba en Entorno de Testing (Docker Test)

Si quieres probar completamente sin tocar producción:

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Levantar entorno de testing
docker compose -f docker-compose.test.yml --env-file .env up -d db-test

# Esperar a que la BD esté lista
sleep 10

# Ejecutar migraciones en BD de test
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan migrate:fresh

# Ejecutar seeder para crear datos de prueba
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan db:seed --class=MonitorPackageSeeder

# Crear algunos commerces de prueba
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan tinker
```

```php
// Crear commerces de prueba
\App\Models\Commerce::create(['name' => 'Test Commerce 1', 'owner_user_id' => 1]);
\App\Models\Commerce::create(['name' => 'Test Commerce 2', 'owner_user_id' => 1]);

exit
```

```bash
# Probar el comando de migración
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan monitor-packages:migrate-global

# Verificar resultados
docker compose -f docker-compose.test.yml --env-file .env run --rm api-test php artisan tinker
```

```php
// Verificar resultados
\App\Models\MonitorPackage::whereNotNull('commerce_id')->count();

exit
```

```bash
# Limpiar entorno de testing
docker compose -f docker-compose.test.yml --env-file .env down -v
```

---

## 📝 Checklist Pre-Migración

Antes de ejecutar en producción, verifica:

- [ ] ✅ Backup de base de datos creado y verificado
- [ ] ✅ Ruta del backup guardada para rollback
- [ ] ✅ Estado actual documentado (paquetes globales, commerces)
- [ ] ✅ Comando probado en staging/testing
- [ ] ✅ Ventana de mantenimiento programada (si es necesario)
- [ ] ✅ Acceso al dashboard verificado
- [ ] ✅ Plan de rollback preparado

---

## 🎯 Comandos Rápidos de Referencia

```bash
# Backup
cd /var/apps/yape-notifier/infra/docker/environments/production
./backup.sh

# Ver estado actual
docker compose --env-file .env exec php-fpm php artisan tinker
# Luego ejecutar queries de verificación

# Migrar (sin eliminar globales)
docker compose --env-file .env exec php-fpm php artisan monitor-packages:migrate-global

# Migrar y eliminar globales (solo después de verificar)
docker compose --env-file .env exec php-fpm php artisan monitor-packages:migrate-global --delete-global

# Verificar resultados
docker compose --env-file .env exec php-fpm php artisan tinker
# Luego ejecutar queries de verificación
```

---

## ⚠️ Advertencias Importantes

1. **No ejecutes `--delete-global` inmediatamente**: Primero verifica que todo funciona
2. **El comando usa transacciones**: Si falla, se revierte automáticamente
3. **Backup es crítico**: Siempre haz backup antes de modificar producción
4. **Prueba primero en staging**: Si tienes staging, úsalo siempre primero
5. **Verifica el dashboard**: Después de migrar, verifica que el dashboard funciona

---

## 📞 Soporte

Si encuentras problemas:

1. **Revisa los logs**: `docker compose --env-file .env logs php-fpm`
2. **Verifica el estado**: Usa `php artisan tinker` para inspeccionar la BD
3. **Restaura desde backup**: Si es necesario, usa el backup creado
4. **Consulta la documentación**: Ver `docs/05-features/MONITOR_PACKAGES.md`

---

## ✅ Post-Migración

Después de migrar exitosamente:

1. ✅ Verificar que todos los commerces tienen paquetes
2. ✅ Probar el dashboard (ver, editar, crear paquetes)
3. ✅ Verificar que los dispositivos Android pueden obtener paquetes
4. ✅ Documentar la migración (fecha, resultados)
5. ✅ Eliminar paquetes globales (solo si todo está bien)

---

**Última actualización:** 2025-01-15

