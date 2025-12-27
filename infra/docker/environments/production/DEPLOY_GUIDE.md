# 🚀 Guía Completa de Despliegue en Producción

Guía paso a paso profesional para desplegar Yape Notifier en producción, considerando todas las mejoras y soluciones implementadas.

## 📋 Prerequisitos

- Acceso SSH al servidor
- Usuario con permisos para Docker
- DNS configurado: `api.notificaciones.space`, `dashboard.notificaciones.space`
- Puertos 80 y 443 disponibles
- Git configurado en el servidor

---

## 🔧 Paso 1: Preparación del Entorno

### 1.1. Conectarse al servidor

```bash
ssh deploy@Server-notifier
```

### 1.2. Navegar al directorio del proyecto

```bash
cd /var/apps/yape-notifier
```

### 1.3. Verificar rama y actualizar código

```bash
# Verificar rama actual
git branch

# Actualizar código desde el repositorio
git pull origin tenant-version
# O si estás en main/master:
# git pull origin main
```

### 1.4. Limpiar artefactos de BuildKit (si existen)

```bash
cd infra/docker/environments/production

# Hacer ejecutables los scripts
chmod +x clean-buildkit-artifacts.sh fix-migrations.sh diagnose-health.sh fix-healthchecks.sh

# Limpiar artefactos de BuildKit
./clean-buildkit-artifacts.sh
```

### 1.5. Verificar archivo .env

```bash
# Verificar que .env existe y tiene las variables necesarias
ls -la .env

# Verificar variables críticas
grep -E "DB_PASSWORD|APP_KEY|REVERB_APP_KEY" .env
```

**Variables requeridas en `.env`:**
- `DB_PASSWORD` - Contraseña de PostgreSQL
- `APP_KEY` - Key de Laravel (se genera automáticamente si no existe)
- `REVERB_APP_KEY` - Key de Reverb (si usas WebSockets)
- `REVERB_APP_SECRET` - Secret de Reverb
- `BROADCAST_CONNECTION` - `reverb` o `null`
- `SANCTUM_STATEFUL_DOMAINS` - Dominios para autenticación
- `SESSION_DOMAIN` - Dominio para sesiones

---

## 🔄 Paso 2: Despliegue (Primera Vez o Reconstrucción Completa)

### Opción A: Usar script de deploy (Recomendado)

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Despliegue normal (con cache)
./deploy.sh

# O sin cache (rebuild completo, más lento pero más seguro)
./deploy.sh --no-cache
```

### Opción B: Despliegue manual paso a paso

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# 1. Detener contenedores existentes
docker compose --env-file .env down --remove-orphans

# 2. Reconstruir imágenes (con BuildKit)
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build --no-cache

# 3. Iniciar servicios
docker compose --env-file .env up -d

# 4. Esperar a que PostgreSQL esté listo
echo "Esperando a PostgreSQL..."
sleep 15

# 5. Verificar que PostgreSQL está listo
docker compose --env-file .env exec -T db pg_isready -U postgres || echo "⚠️ PostgreSQL aún no está listo"
```

---

## 🔄 Paso 3: Actualización (Código ya actualizado)

### Opción A: Usar script de update (Recomendado - con backup)

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# El script hace backup automático antes de actualizar
./update.sh
```

### Opción B: Actualización manual

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# 1. Backup manual de base de datos (RECOMENDADO)
mkdir -p backups
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > backups/backup_$(date +%Y%m%d_%H%M%S).sql.gz

# 2. Reconstruir imágenes
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build --no-cache

# 3. Reiniciar servicios
docker compose --env-file .env restart

# 4. Limpiar caches de Laravel
docker compose --env-file .env exec -T php-fpm php artisan config:clear
docker compose --env-file .env exec -T php-fpm php artisan route:clear
docker compose --env-file .env exec -T php-fpm sh -c "rm -f /var/www/bootstrap/cache/packages.php /var/www/bootstrap/cache/services.php /var/www/bootstrap/cache/config.php"

# 5. Regenerar package discovery
docker compose --env-file .env exec -T php-fpm php artisan package:discover --ansi

# 6. Regenerar caches
docker compose --env-file .env exec -T php-fpm php artisan config:cache
docker compose --env-file .env exec -T php-fpm php artisan route:cache
```

---

## 🔧 Paso 4: Resolver Problemas Comunes

### 4.1. Migraciones desincronizadas

Si ves errores como "Duplicate table" o "relation already exists":

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Opción A: Script automático
./fix-migrations.sh

# Opción B: Manual (si el script falla)
docker compose --env-file .env exec php-fpm php artisan tinker

# Dentro de tinker:
DB::table('migrations')->insert(['migration' => '2025_01_15_000006_create_monitor_packages_table', 'batch' => DB::table('migrations')->max('batch') + 1]);
DB::table('migrations')->insert(['migration' => '2025_01_15_000007_create_device_monitored_apps_table', 'batch' => DB::table('migrations')->max('batch') + 1]);
DB::table('migrations')->insert(['migration' => '2025_01_15_000008_add_commerce_to_monitor_packages_table', 'batch' => DB::table('migrations')->max('batch') + 1]);
exit
```

### 4.2. Servicios unhealthy

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Diagnosticar problemas
./diagnose-health.sh

# Intentar reparar automáticamente
./fix-healthchecks.sh

# Si persisten, verificar manualmente:

# Caddy
docker compose --env-file .env exec caddy wget --quiet --tries=1 --spider --timeout=5 http://localhost:2019/config/ && echo "✅ Caddy OK" || echo "❌ Caddy no responde"

# Dashboard
docker compose --env-file .env exec dashboard wget --quiet --tries=1 --spider --timeout=5 http://localhost/health && echo "✅ Dashboard OK" || echo "❌ Dashboard no responde"
```

### 4.3. Error 419 (CSRF Token Mismatch)

Ya está resuelto en el código (removido `EnsureFrontendRequestsAreStateful`), pero si persiste:

```bash
# Limpiar todos los caches
docker compose --env-file .env exec -T php-fpm php artisan config:clear
docker compose --env-file .env exec -T php-fpm php artisan route:clear
docker compose --env-file .env exec -T php-fpm php artisan cache:clear

# Regenerar caches
docker compose --env-file .env exec -T php-fpm php artisan config:cache
docker compose --env-file .env exec -T php-fpm php artisan route:cache

# Reiniciar PHP-FPM
docker compose --env-file .env restart php-fpm
```

### 4.4. Error 502 Bad Gateway

```bash
# Verificar que Nginx puede comunicarse con PHP-FPM
docker compose --env-file .env exec nginx-api ping -c 2 php-fpm

# Verificar logs de Nginx
docker compose --env-file .env logs nginx-api --tail=50 | grep -i error

# Reiniciar servicios
docker compose --env-file .env restart nginx-api php-fpm
```

---

## ✅ Paso 5: Verificación Post-Despliegue

### 5.1. Verificar estado de servicios

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Ver estado de todos los servicios
docker compose --env-file .env ps

# Verificar healthchecks
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Status}}\t{{.Health}}'
```

**Estado esperado:**
- ✅ `db`: healthy
- ✅ `php-fpm`: healthy
- ✅ `nginx-api`: healthy
- ✅ `reverb`: healthy (si está configurado)
- ⚠️ `caddy`: puede estar unhealthy pero funcionar (verificar manualmente)
- ⚠️ `dashboard`: puede estar unhealthy pero funcionar (verificar manualmente)

### 5.2. Verificar API

```bash
# Health check
curl -f https://api.notificaciones.space/up && echo "✅ API responde" || echo "❌ API no responde"

# Probar login
curl -X POST https://api.notificaciones.space/api/login \
  -H "Content-Type: application/json" \
  -H "Origin: https://dashboard.notificaciones.space" \
  -d '{"email":"test@example.com","password":"test123"}' \
  -w "\nHTTP Status: %{http_code}\n"

# Debería devolver 200 (éxito) o 401 (credenciales incorrectas)
# NO debería devolver 419 (CSRF) ni 502 (Bad Gateway)
```

### 5.3. Verificar Dashboard

```bash
# Verificar que el dashboard responde
curl -f https://dashboard.notificaciones.space && echo "✅ Dashboard responde" || echo "❌ Dashboard no responde"
```

### 5.4. Verificar Reverb (si está configurado)

```bash
# Verificar que Reverb está corriendo
docker compose --env-file .env logs reverb --tail=20

# Deberías ver: "INFO  Starting server on 0.0.0.0:8080"
```

### 5.5. Verificar migraciones

```bash
# Verificar estado de migraciones
docker compose --env-file .env exec -T php-fpm php artisan migrate:status

# No debería haber migraciones "Pending" que fallen con "Duplicate table"
```

---

## 🔍 Paso 6: Monitoreo y Logs

### 6.1. Ver logs en tiempo real

```bash
# Todos los servicios
docker compose --env-file .env logs -f

# Servicio específico
docker compose --env-file .env logs -f php-fpm
docker compose --env-file .env logs -f nginx-api
docker compose --env-file .env logs -f caddy
```

### 6.2. Ver logs de errores

```bash
# Logs de Laravel
docker compose --env-file .env exec php-fpm tail -f /var/www/storage/logs/laravel.log

# Logs de Nginx
docker compose --env-file .env exec nginx-api tail -f /var/log/nginx/api-error.log

# Logs de Caddy
docker compose --env-file .env logs caddy --tail=50 | grep -i error
```

---

## 🎯 Resumen de Comandos Rápidos

### Despliegue completo (primera vez)
```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
git pull origin tenant-version
./clean-buildkit-artifacts.sh
./deploy.sh --no-cache
```

### Actualización (código ya actualizado)
```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
git pull origin tenant-version
./update.sh
```

### Resolver problemas
```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Migraciones desincronizadas
./fix-migrations.sh

# Servicios unhealthy
./diagnose-health.sh
./fix-healthchecks.sh

# Limpiar artefactos
./clean-buildkit-artifacts.sh
```

### Verificar estado
```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Estado de servicios
docker compose --env-file .env ps

# Healthchecks
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Status}}\t{{.Health}}'

# Migraciones
docker compose --env-file .env exec php-fpm php artisan migrate:status

# API
curl -f https://api.notificaciones.space/up
```

---

## ⚠️ Checklist Pre-Despliegue

Antes de desplegar, verifica:

- [ ] Código actualizado en el servidor (`git pull`)
- [ ] `.env` configurado con todas las variables necesarias
- [ ] `DB_PASSWORD` configurado y seguro
- [ ] `APP_KEY` configurado (o se generará automáticamente)
- [ ] Variables de Reverb configuradas (si usas WebSockets)
- [ ] `SANCTUM_STATEFUL_DOMAINS` y `SESSION_DOMAIN` configurados
- [ ] Backup de base de datos (si es actualización)
- [ ] DNS configurado correctamente
- [ ] Puertos 80 y 443 disponibles

---

## 🆘 Troubleshooting Rápido

| Problema | Comando de Diagnóstico | Solución |
|----------|------------------------|----------|
| Migraciones fallan | `docker compose --env-file .env exec php-fpm php artisan migrate:status` | `./fix-migrations.sh` |
| Servicios unhealthy | `./diagnose-health.sh` | `./fix-healthchecks.sh` |
| Error 419 CSRF | `docker compose --env-file .env logs php-fpm \| grep -i csrf` | Limpiar caches y regenerar |
| Error 502 Bad Gateway | `docker compose --env-file .env logs nginx-api` | Reiniciar nginx-api y php-fpm |
| API no responde | `curl -f https://api.notificaciones.space/up` | Verificar logs y healthchecks |
| Artefactos en Git | `git status` | `./clean-buildkit-artifacts.sh` |

---

## 📚 Documentación Adicional

- **Reverb Setup**: Ver `REVERB_SETUP.md`
- **Docker Infrastructure**: Ver `../../../../docs/02-deployment/DOCKER.md`
- **Deployment Guide**: Ver `../../../../docs/02-deployment/DEPLOYMENT.md`

---

## ✅ Verificación Final

Después del despliegue, ejecuta esta verificación completa:

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

echo "=== Estado de Servicios ==="
docker compose --env-file .env ps

echo ""
echo "=== Healthchecks ==="
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Health}}'

echo ""
echo "=== Migraciones ==="
docker compose --env-file .env exec -T php-fpm php artisan migrate:status | grep -E "Pending|Ran" | tail -5

echo ""
echo "=== API Health Check ==="
curl -f -s https://api.notificaciones.space/up && echo "✅ API OK" || echo "❌ API Error"

echo ""
echo "=== Test Login ==="
curl -X POST https://api.notificaciones.space/api/login \
  -H "Content-Type: application/json" \
  -H "Origin: https://dashboard.notificaciones.space" \
  -d '{"email":"test@example.com","password":"test123"}' \
  -s -o /dev/null -w "HTTP Status: %{http_code}\n"
```

**Resultado esperado:**
- ✅ Todos los servicios corriendo
- ✅ Servicios críticos (db, php-fpm, nginx-api) healthy
- ✅ No hay migraciones pendientes con errores
- ✅ API responde con 200
- ✅ Login devuelve 200 o 401 (NO 419 ni 502)

---

¡Despliegue completado! 🎉

