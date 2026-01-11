# 🚀 Pasos para Desplegar Comando de Limpieza de Dispositivos

> **Referencias relacionadas:**
> - [CLEAN_DEVICES.md](CLEAN_DEVICES.md) - Guía profesional de limpieza de dispositivos
> - [UPDATE_CHECKLIST.md](UPDATE_CHECKLIST.md) - Checklist de actualización del servidor
> - [DEPLOYMENT_COMMANDS.md](../02-deployment/DEPLOYMENT_COMMANDS.md) - Comandos rápidos de despliegue
> - [GUIA_ACTUALIZACION.md](../02-deployment/GUIA_ACTUALIZACION.md) - Guía de actualización

Guía paso a paso para enviar el nuevo comando `devices:clean` y la documentación al servidor de producción.

---

## 📋 Resumen de Cambios a Desplegar

**Archivos nuevos:**
- ✅ `apps/api/app/Console/Commands/CleanDevicesCommand.php` - Comando Artisan
- ✅ `docs/06-operations/CLEAN_DEVICES.md` - Documentación completa

**No requiere:**
- ❌ Migraciones de base de datos
- ❌ Cambios en configuración
- ❌ Cambios en dependencias

---

## 🔄 PASO 1: Commit y Push en Local

### 1.1 Verificar cambios

```bash
# Desde la raíz del proyecto
git status
```

**Deberías ver:**
```
modified:   apps/api/app/Console/Commands/CleanDevicesCommand.php (nuevo)
modified:   docs/06-operations/CLEAN_DEVICES.md (nuevo)
```

### 1.2 Agregar cambios

```bash
git add apps/api/app/Console/Commands/CleanDevicesCommand.php
git add docs/06-operations/CLEAN_DEVICES.md
```

### 1.3 Hacer commit

```bash
git commit -m "feat: add professional device cleanup command

- Add CleanDevicesCommand with dry-run and safety features
- Add comprehensive CLEAN_DEVICES.md documentation
- Include transaction safety and logging
- Support for cascade deletion verification"
```

### 1.4 Push al repositorio

```bash
# Verificar rama actual
git branch

# Push (ajusta la rama según tu configuración)
git push origin tenant-version
# O: git push origin main
# O: git push origin production
```

**Verificar que se subió:**
```bash
git log --oneline -1
# Deberías ver tu commit
```

---

## 🖥️ PASO 2: Conectarse al Servidor

### 2.1 SSH al servidor

```bash
# Conectar vía SSH (ajusta según tu configuración)
ssh deploy@tu-servidor
# O: ssh root@tu-servidor
# O: ssh usuario@tu-ip-droplet
```

### 2.2 Verificar ubicación del proyecto

```bash
# Navegar al directorio del proyecto
cd /var/apps/yape-notifier
# O la ruta que uses: cd /home/deploy/yape-notifier

# Verificar que estás en el directorio correcto
pwd
ls -la
# Deberías ver: apps/, docs/, infra/, etc.
```

---

## 📥 PASO 3: Actualizar Código en el Servidor

### 3.1 Verificar rama actual

```bash
cd /var/apps/yape-notifier

# Ver rama actual
git branch

# Ver estado
git status
```

### 3.2 Hacer backup (opcional pero recomendado)

```bash
# Backup del código actual (por si acaso)
tar -czf backup_code_$(date +%Y%m%d_%H%M%S).tar.gz apps/api/app/Console/Commands/
```

### 3.3 Actualizar desde repositorio

```bash
# Guardar cambios locales si los hay (si aplica)
git stash

# Actualizar código
git pull origin tenant-version
# O: git pull origin main
# O: git pull origin production

# Si hiciste stash, restaurar cambios (si aplica)
# git stash pop
```

### 3.4 Verificar que los archivos se descargaron

```bash
# Verificar que el comando existe
ls -la apps/api/app/Console/Commands/CleanDevicesCommand.php

# Verificar que la documentación existe
ls -la docs/06-operations/CLEAN_DEVICES.md

# Ver contenido del comando (primeras líneas)
head -20 apps/api/app/Console/Commands/CleanDevicesCommand.php
```

**Deberías ver:**
```php
<?php

namespace App\Console\Commands;

use App\Models\Device;
...
```

---

## 🐳 PASO 4: Reconstruir Contenedor PHP-FPM

### 4.1 Ir al directorio de producción

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
```

### 4.2 Verificar estado actual

```bash
# Ver contenedores corriendo
docker compose --env-file .env ps

# Deberías ver algo como:
# NAME                              STATUS
# yape-notifier-php-fpm-prod        Up
# yape-notifier-db-prod             Up (healthy)
# yape-notifier-nginx-api-prod      Up
# ...
```

### 4.3 Reconstruir contenedor PHP-FPM

**Opción A: Usar script de actualización (Recomendado)**

```bash
# Si tienes script update.sh
chmod +x update.sh
./update.sh
```

**Opción B: Reconstrucción manual**

```bash
# Reconstruir solo el contenedor PHP-FPM (más rápido)
docker compose --env-file .env build php-fpm

# Reiniciar el contenedor
docker compose --env-file .env up -d php-fpm

# Verificar que se reinició correctamente
docker compose --env-file .env ps php-fpm
```

**Opción C: Reconstrucción completa (si hay problemas)**

```bash
# Reconstruir sin cache (más lento pero más seguro)
docker compose --env-file .env build --no-cache php-fpm

# Reiniciar
docker compose --env-file .env up -d php-fpm
```

### 4.4 Verificar logs

```bash
# Ver logs del contenedor PHP-FPM
docker compose --env-file .env logs php-fpm --tail=50

# Buscar errores
docker compose --env-file .env logs php-fpm | grep -i error
```

---

## ✅ PASO 5: Verificar que el Comando Funciona

### 5.1 Verificar que el comando está registrado

```bash
# Listar comandos disponibles
docker compose --env-file .env exec php-fpm php artisan list | grep devices

# Deberías ver:
# devices:clean    Elimina todos los dispositivos de la base de datos de forma segura
```

### 5.2 Probar modo dry-run (sin eliminar nada)

```bash
# Probar el comando en modo dry-run
docker compose --env-file .env exec php-fpm php artisan devices:clean --dry-run
```

**Salida esperada:**
```
🔍 Analizando base de datos...

┌─────────────────────┬──────────┐
│ Tabla               │ Registros│
├─────────────────────┼──────────┤
│ devices             │ 15       │
│ notifications       │ 234      │
│ app_instances       │ 8        │
│ device_monitored... │ 12       │
│ device_link_codes...│ 3        │
└─────────────────────┴──────────┘

🔍 MODO DRY-RUN: No se eliminarán registros.
Se eliminarían 15 dispositivos y sus registros relacionados.
```

### 5.3 Verificar ayuda del comando

```bash
# Ver ayuda del comando
docker compose --env-file .env exec php-fpm php artisan devices:clean --help
```

**Deberías ver:**
```
Description:
  Elimina todos los dispositivos de la base de datos de forma segura

Usage:
  devices:clean [options]

Options:
  --force            Force deletion without confirmation
  --dry-run          Show what would be deleted without actually deleting
  -h, --help         Display help for the command
  -q, --quiet        Do not output any message
  -V, --version      Display this application version
  -v|vv|vvv, --verbose
                     Increase the verbosity of messages
```

---

## 🧪 PASO 6: Pruebas Adicionales (Opcional)

### 6.1 Verificar que el comando está en el namespace correcto

```bash
# Verificar autoload
docker compose --env-file .env exec php-fpm composer dump-autoload

# Verificar que no hay errores de sintaxis
docker compose --env-file .env exec php-fpm php -l apps/api/app/Console/Commands/CleanDevicesCommand.php
```

**Salida esperada:**
```
No syntax errors detected in apps/api/app/Console/Commands/CleanDevicesCommand.php
```

### 6.2 Verificar logs de Laravel

```bash
# Ver logs de Laravel (si hay algún error)
docker compose --env-file .env exec php-fpm tail -50 storage/logs/laravel.log
```

---

## 📊 PASO 7: Verificación Final

### 7.1 Verificar estado de servicios

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Ver estado de todos los servicios
docker compose --env-file .env ps

# Todos deben estar "Up" o "Up (healthy)"
```

### 7.2 Verificar que la API sigue funcionando

```bash
# Probar endpoint de health
curl -f https://api.notificaciones.space/up

# Deberías ver:
# {"status":"ok"}
```

### 7.3 Verificar documentación

```bash
# Verificar que la documentación está disponible
cat docs/06-operations/CLEAN_DEVICES.md | head -30
```

---

## 🎯 PASO 8: Uso del Comando en Producción

### 8.1 Ver qué se eliminaría (dry-run)

```bash
docker compose --env-file .env exec php-fpm php artisan devices:clean --dry-run
```

### 8.2 Eliminar con confirmación (cuando sea necesario)

```bash
# El comando pedirá confirmación interactiva
docker compose --env-file .env exec php-fpm php artisan devices:clean
```

**Seguir las instrucciones:**
```
⚠️  ADVERTENCIA: Esta operación eliminará:
   - 15 dispositivos
   - 234 notificaciones (CASCADE)
   - 8 instancias de apps (CASCADE)
   - 12 apps monitoreadas (CASCADE)
   - Los device_id en device_link_codes se pondrán en NULL

¿Estás seguro de que deseas continuar? (yes/no) [no]:
```

### 8.3 Eliminar sin confirmación (para scripts)

```bash
# Usar --force para evitar confirmación
docker compose --env-file .env exec php-fpm php artisan devices:clean --force
```

---

## 🚨 Troubleshooting

### Problema 1: Comando no aparece en `php artisan list`

**Solución:**
```bash
# Limpiar cache de Laravel
docker compose --env-file .env exec php-fpm php artisan config:clear
docker compose --env-file .env exec php-fpm php artisan cache:clear

# Regenerar autoload
docker compose --env-file .env exec php-fpm composer dump-autoload

# Verificar nuevamente
docker compose --env-file .env exec php-fpm php artisan list | grep devices
```

### Problema 2: Error "Class CleanDevicesCommand not found"

**Solución:**
```bash
# Regenerar autoload de Composer
docker compose --env-file .env exec php-fpm composer dump-autoload

# Verificar que el archivo existe
docker compose --env-file .env exec php-fpm ls -la app/Console/Commands/CleanDevicesCommand.php
```

### Problema 3: Error de permisos

**Solución:**
```bash
# Verificar permisos del archivo
docker compose --env-file .env exec php-fpm ls -la app/Console/Commands/CleanDevicesCommand.php

# Si es necesario, ajustar permisos (desde el servidor, no desde Docker)
chmod 644 apps/api/app/Console/Commands/CleanDevicesCommand.php
```

### Problema 4: Contenedor no se reconstruye

**Solución:**
```bash
# Forzar reconstrucción sin cache
docker compose --env-file .env build --no-cache php-fpm

# Eliminar contenedor y volver a crearlo
docker compose --env-file .env down php-fpm
docker compose --env-file .env up -d php-fpm
```

---

## 📋 Checklist de Despliegue

### Pre-despliegue
- [ ] Commit realizado en local
- [ ] Push al repositorio exitoso
- [ ] Backup del código actual (opcional)

### Despliegue
- [ ] Conectado al servidor vía SSH
- [ ] Código actualizado con `git pull`
- [ ] Archivos nuevos verificados
- [ ] Contenedor PHP-FPM reconstruido
- [ ] Contenedor reiniciado correctamente

### Verificación
- [ ] Comando aparece en `php artisan list`
- [ ] Modo dry-run funciona correctamente
- [ ] Ayuda del comando se muestra correctamente
- [ ] No hay errores en logs
- [ ] API sigue funcionando

### Post-despliegue
- [ ] Documentación verificada
- [ ] Comando probado en modo dry-run
- [ ] Listo para uso en producción

---

## 🎉 ¡Despliegue Completado!

El comando `devices:clean` ahora está disponible en producción y listo para usar.

**Recordatorios importantes:**
- ⚠️ Siempre usar `--dry-run` primero para verificar
- ⚠️ Hacer backup de la BD antes de eliminar dispositivos
- ⚠️ El comando elimina datos de forma permanente
- ✅ El comando usa transacciones para garantizar atomicidad
- ✅ El comando registra todas las operaciones en logs

---

## 📚 Referencias

- [CLEAN_DEVICES.md](CLEAN_DEVICES.md) - Documentación completa del comando
- [DEPLOYMENT_COMMANDS.md](../02-deployment/DEPLOYMENT_COMMANDS.md) - Guía general de deployment
- [DEPLOYMENT.md](../02-deployment/DEPLOYMENT.md) - Guía completa de deployment

---

**Última actualización:** 2025-01-27

