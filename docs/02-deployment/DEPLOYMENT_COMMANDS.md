# Comandos de Despliegue en Producción

> **Referencias relacionadas:**
> - [DEPLOYMENT.md](DEPLOYMENT.md) - Guía completa de despliegue
> - [GUIA_PRODUCCION_PASO_A_PASO.md](GUIA_PRODUCCION_PASO_A_PASO.md) - Guía detallada paso a paso
> - [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) - Checklist de despliegue

Guía rápida de comandos para desplegar actualizaciones en producción.

---

## Pasos para Desplegar en Producción

### PASO 1: En tu máquina local - Hacer commit y push

```bash
git add .
git commit -m "feat: add employee management with PIN generation

- Add UserController with CRUD endpoints
- Add EmployeesPage in dashboard
- Auto-generate PIN when creating employees
- Fix migration detection in deploy/update scripts (detect DONE)
- Add regenerate PIN functionality"

git push origin tenant-version
# O la rama que uses: git push origin main
```

### PASO 2: Conectarse al servidor

```bash
ssh deploy@tu-servidor
# O: ssh root@tu-servidor
```

### PASO 3: Actualizar código en el servidor

```bash
cd /var/apps/yape-notifier
git pull origin tenant-version
# O: git pull origin main
```

### PASO 4: Ir al directorio de producción

```bash
cd infra/docker/environments/production
```

### PASO 5: Ejecutar actualización (con backup automático)

```bash
chmod +x update.sh
./update.sh
```

**El script:**
- Hace backup automático de la BD
- Reconstruye imágenes Docker
- Ejecuta migraciones (ahora detecta DONE correctamente)
- Reinicia servicios
- Verifica healthchecks

### PASO 6: Verificar que todo funciona

```bash
# Ver estado de servicios
docker compose --env-file .env ps

# Verificar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate:status

# Probar API
curl -f https://api.notificaciones.space/up
```

### PASO 7: Verificar nueva funcionalidad

1. Abrir dashboard: https://dashboard.notificaciones.space
2. Ir a la pestaña "Empleados"
3. Crear un empleado y verificar que se genera el PIN

---

## Si algo falla - Rollback

El script `update.sh` genera un script de rollback automático:

**Ubicación:** `./backups/rollback_YYYYMMDD_HHMMSS.sh`

**Ejecutar:**
```bash
./backups/rollback_YYYYMMDD_HHMMSS.sh
```

---

**Última actualización:** 2025-01-21
