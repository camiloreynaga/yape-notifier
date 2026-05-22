# 🚀 Guía de Producción - Paso a Paso

> **Guía profesional para desplegar y actualizar Yape Notifier en producción**
> 
> **Audiencia:** DevOps / Administradores de sistemas
> 
> **Tiempo estimado:** 30-60 minutos (despliegue inicial) / 15-30 minutos (actualización)

---

## 📋 Índice

1. [Prerequisitos](#prerequisitos)
2. [Despliegue Inicial (Primera Vez)](#despliegue-inicial-primera-vez)
3. [Actualización de Sistema Existente](#actualización-de-sistema-existente)
4. [Verificación Post-Despliegue](#verificación-post-despliegue)
5. [Troubleshooting](#troubleshooting)
6. [Comandos Rápidos de Referencia](#comandos-rápidos-de-referencia)

---

## 🔧 Prerequisitos

### 1.1. Acceso al Servidor

```bash
# Conectarse al servidor de producción
ssh deploy@tu-servidor
# O
ssh root@tu-servidor
```

### 1.2. Verificar Instalación de Docker

```bash
# Verificar Docker
docker --version
docker compose version

# Si no está instalado:
# Ubuntu/Debian:
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

### 1.3. Verificar Estructura del Proyecto

```bash
# Navegar al directorio del proyecto
cd /var/apps/yape-notifier
# O la ruta donde tengas el proyecto

# Verificar estructura
ls -la infra/docker/environments/production/
```

**Estructura esperada:**
```
infra/docker/environments/production/
├── .env                    # Variables de entorno (OBLIGATORIO)
├── docker-compose.yml      # Configuración de servicios
├── Caddyfile              # Configuración de reverse proxy
├── deploy.sh              # Script de despliegue inicial
├── update.sh              # Script de actualización
├── setup.sh               # Script de configuración inicial
└── [otros scripts de utilidad]
```

### 1.4. Verificar Archivo .env

```bash
cd infra/docker/environments/production

# Verificar que .env existe
ls -la .env

# Verificar variables críticas
grep -E "DB_PASSWORD|APP_KEY|APP_URL" .env
```

**Variables obligatorias en `.env`:**
- `DB_PASSWORD` - Contraseña de PostgreSQL (DEBE estar configurada)
- `DB_DATABASE` - Nombre de la base de datos (default: `yape_notifier`)
- `DB_USERNAME` - Usuario de PostgreSQL (default: `postgres`)
- `DB_HOST=db` - Host de la base de datos (debe ser `db` para Docker)
- `APP_URL` - URL de la API (ej: `https://api.notificaciones.space`)
- `APP_KEY` - Key de Laravel (se genera automáticamente si no existe)

**Variables opcionales pero recomendadas:**
- `REVERB_APP_KEY` - Key de Reverb (si usas WebSockets)
- `REVERB_APP_SECRET` - Secret de Reverb
- `SANCTUM_STATEFUL_DOMAINS` - Dominios para autenticación
- `SESSION_DOMAIN` - Dominio para sesiones

---

## 🆕 Despliegue Inicial (Primera Vez)

### Paso 1: Preparar el Entorno

```bash
# 1. Navegar al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# 2. Si no existe .env, usar setup.sh
if [ ! -f ".env" ]; then
    chmod +x setup.sh
    ./setup.sh
    # Luego editar .env y configurar DB_PASSWORD
    nano .env  # o vim .env
fi
```

### Paso 2: Actualizar Código desde Repositorio

```bash
# Desde el directorio raíz del proyecto
cd /var/apps/yape-notifier

# Verificar rama actual
git branch

# Actualizar código
git pull origin main
# O la rama que uses: git pull origin tenant-version
```

### Paso 3: Verificar Compatibilidad PHP 8.2

```bash
# El script deploy.sh valida automáticamente, pero puedes verificar manualmente:
cd apps/api

# Verificar composer.json
grep '"php"' composer.json

# Debe mostrar: "php": ">=8.2 <8.3"
```

### Paso 4: Ejecutar Despliegue

```bash
# Volver al directorio de producción
cd /var/apps/yape-notifier/infra/docker/environments/production

# Hacer ejecutables los scripts (si no lo están)
chmod +x deploy.sh update.sh setup.sh

# Opción A: Despliegue con cache (más rápido)
./deploy.sh

# Opción B: Despliegue sin cache (más seguro, más lento)
./deploy.sh --no-cache
```

**¿Qué hace `deploy.sh`?**
1. ✅ Valida que Docker y Docker Compose estén instalados
2. ✅ Verifica que `.env` existe y tiene `DB_PASSWORD` configurado
3. ✅ Valida compatibilidad de `composer.lock` con PHP 8.2 LTS
4. ✅ Detiene contenedores existentes
5. ✅ Construye imágenes Docker (con BuildKit)
6. ✅ Inicia contenedores
7. ✅ Espera a que PostgreSQL esté listo
8. ✅ Genera `APP_KEY` si no existe (solo primera vez)
9. ✅ Configura permisos de directorios
10. ✅ Limpia caches de Laravel
11. ✅ Ejecuta migraciones de base de datos
12. ✅ Optimiza Laravel (config:cache, route:cache)
13. ✅ Verifica healthchecks de servicios
14. ✅ Verifica que la API responde

**Tiempo estimado:** 10-20 minutos

### Paso 5: Verificar Despliegue

```bash
# Ver estado de servicios
docker compose --env-file .env ps

# Verificar healthchecks
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Status}}\t{{.Health}}'

# Verificar API
curl -f https://api.notificaciones.space/up
# Debe responder: {"status":"ok"}
```

**Estado esperado:**
- ✅ `db` (PostgreSQL): healthy
- ✅ `php-fpm`: healthy
- ✅ `nginx-api`: healthy
- ✅ `reverb`: healthy (si está configurado)
- ⚠️ `caddy`: puede estar unhealthy pero funcionar (verificar manualmente)
- ⚠️ `dashboard`: puede estar unhealthy pero funcionar (verificar manualmente)

---

## 🔄 Actualización de Sistema Existente

> **IMPORTANTE:** Este es el flujo más común. Se usa cuando ya tienes el sistema corriendo y necesitas actualizar código, dependencias o migraciones.

### Paso 1: Conectarse al Servidor

```bash
ssh deploy@tu-servidor
cd /var/apps/yape-notifier/infra/docker/environments/production
```

### Paso 2: Actualizar Código

```bash
# Desde el directorio raíz
cd /var/apps/yape-notifier

# Verificar cambios pendientes
git status

# Si hay cambios locales que quieres guardar:
git stash

# Actualizar desde repositorio
git pull origin main
# O: git pull origin tenant-version
```

### Paso 3: Ejecutar Actualización con Backup Automático

```bash
# Volver al directorio de producción
cd infra/docker/environments/production

# Ejecutar script de actualización (hace backup automático)
chmod +x update.sh
./update.sh
```

**¿Qué hace `update.sh`?**
1. ✅ **Crea backup automático** de la base de datos (comprimido)
2. ✅ **Genera script de rollback** automático
3. ✅ Verifica estado actual del sistema
4. ✅ Valida compatibilidad PHP 8.2 y `composer.lock`
5. ✅ Reconstruye imágenes Docker (sin cache)
6. ✅ Limpia caches de Laravel
7. ✅ Regenera package discovery
8. ✅ Ejecuta migraciones (con manejo de errores)
9. ✅ Reinicia servicios
10. ✅ Verifica healthchecks
11. ✅ Optimiza caches para producción

**Tiempo estimado:** 15-30 minutos

### Paso 4: Verificar Actualización

```bash
# Ver estado de servicios
docker compose --env-file .env ps

# Verificar migraciones
docker compose --env-file .env exec -T php-fpm php artisan migrate:status

# Verificar API
curl -f https://api.notificaciones.space/up

# Ver logs recientes
docker compose --env-file .env logs php-fpm --tail=50
```

### Paso 5: (Opcional) Rollback si es Necesario

Si algo salió mal, el script `update.sh` genera un script de rollback:

```bash
# El script te mostrará la ruta del rollback, ejemplo:
./backups/rollback_20250115_143022.sh

# Ejecutar rollback
./backups/rollback_20250115_143022.sh
```

---

## ✅ Verificación Post-Despliegue

### 1. Verificar Estado de Servicios

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Estado general
docker compose --env-file .env ps

# Healthchecks detallados
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Health}}\t{{.Status}}'
```

### 2. Verificar API

```bash
# Health check endpoint
curl -f https://api.notificaciones.space/up
# Esperado: {"status":"ok"}

# Probar login (debe devolver 200 o 401, NO 419 ni 502)
curl -X POST https://api.notificaciones.space/api/login \
  -H "Content-Type: application/json" \
  -H "Origin: https://dashboard.notificaciones.space" \
  -d '{"email":"test@example.com","password":"test123"}' \
  -w "\nHTTP Status: %{http_code}\n"
```

### 3. Verificar Dashboard

```bash
# Verificar que el dashboard responde
curl -f https://dashboard.notificaciones.space
```

### 4. Verificar Migraciones

```bash
# Ver estado de migraciones
docker compose --env-file .env exec -T php-fpm php artisan migrate:status

# No debe haber migraciones "Pending" con errores
```

### 5. Verificar Logs

```bash
# Logs de todos los servicios
docker compose --env-file .env logs --tail=100

# Logs de un servicio específico
docker compose --env-file .env logs php-fpm --tail=50
docker compose --env-file .env logs nginx-api --tail=50
docker compose --env-file .env logs caddy --tail=50

# Logs de Laravel
docker compose --env-file .env exec php-fpm tail -f /var/www/storage/logs/laravel.log
```

### 6. Verificar Base de Datos

```bash
# Conectar a PostgreSQL
docker compose --env-file .env exec -it db psql -U postgres -d yape_notifier

# Verificar tablas
\dt

# Verificar migraciones ejecutadas
SELECT * FROM migrations ORDER BY batch DESC, id DESC LIMIT 10;

# Salir
\q
```

---

## 🔧 Troubleshooting

### Problema: Migraciones Desincronizadas

**Síntoma:** Error "Duplicate table" o "relation already exists"

**Solución:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Opción A: Script automático
chmod +x fix-migrations.sh
./fix-migrations.sh

# Opción B: Manual
docker compose --env-file .env exec php-fpm php artisan migrate:status
# Identificar migración problemática y marcarla como ejecutada
```

### Problema: Servicios Unhealthy

**Síntoma:** Servicios muestran estado "unhealthy" en `docker compose ps`

**Solución:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Diagnosticar
chmod +x diagnose-health.sh
./diagnose-health.sh

# Intentar reparar automáticamente
chmod +x fix-healthchecks.sh
./fix-healthchecks.sh

# Ver logs del servicio problemático
docker compose --env-file .env logs [nombre-servicio] --tail=100
```

### Problema: Error 302 Redirect (HTML en lugar de JSON)

**Síntoma:** La API devuelve un redirect HTML en lugar de JSON

**Solución:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Opción A: Script automático
chmod +x fix-302-redirect.sh
./fix-302-redirect.sh

# Opción B: Manual
# 1. Verificar APP_URL en .env
grep APP_URL .env
# Debe ser: APP_URL=https://api.notificaciones.space

# 2. Si no es HTTPS, actualizar
sed -i 's|APP_URL=.*|APP_URL=https://api.notificaciones.space|' .env

# 3. Reiniciar servicios
docker compose --env-file .env restart nginx-api php-fpm

# 4. Limpiar y regenerar caches
docker compose --env-file .env exec -T php-fpm php artisan config:clear
docker compose --env-file .env exec -T php-fpm php artisan route:clear
docker compose --env-file .env exec -T php-fpm php artisan config:cache
docker compose --env-file .env exec -T php-fpm php artisan route:cache
```

### Problema: Error 419 CSRF Token Mismatch

**Síntoma:** La API devuelve 419 en requests POST

**Solución:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

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

### Problema: Error 502 Bad Gateway

**Síntoma:** Nginx devuelve 502

**Solución:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Verificar que PHP-FPM está corriendo
docker compose --env-file .env ps php-fpm

# Verificar logs de Nginx
docker compose --env-file .env logs nginx-api --tail=50 | grep -i error

# Verificar logs de PHP-FPM
docker compose --env-file .env logs php-fpm --tail=50

# Reiniciar servicios
docker compose --env-file .env restart nginx-api php-fpm
```

### Problema: Artefactos de BuildKit en Git

**Síntoma:** `git status` muestra archivos de BuildKit

**Solución:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Limpiar artefactos
chmod +x clean-buildkit-artifacts.sh
./clean-buildkit-artifacts.sh
```

### Problema: API No Responde

**Síntoma:** `curl https://api.notificaciones.space/up` falla

**Solución:**

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# 1. Verificar que los servicios están corriendo
docker compose --env-file .env ps

# 2. Verificar logs
docker compose --env-file .env logs --tail=100

# 3. Verificar DNS
nslookup api.notificaciones.space

# 4. Verificar Caddy
docker compose --env-file .env logs caddy --tail=50

# 5. Verificar que Caddy puede alcanzar nginx-api
docker compose --env-file .env exec caddy wget --quiet --tries=1 --spider --timeout=5 http://nginx-api:80/up
```

---

## 📝 Comandos Rápidos de Referencia

### Despliegue Inicial

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
git pull origin main
./deploy.sh --no-cache
```

### Actualización

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production
git pull origin main
./update.sh
```

### Ver Estado

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Estado de servicios
docker compose --env-file .env ps

# Healthchecks
docker compose --env-file .env ps --format 'table {{.Name}}\t{{.Health}}'

# Migraciones
docker compose --env-file .env exec php-fpm php artisan migrate:status
```

### Ver Logs

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Todos los servicios
docker compose --env-file .env logs -f

# Servicio específico
docker compose --env-file .env logs -f php-fpm
docker compose --env-file .env logs -f nginx-api
docker compose --env-file .env logs -f caddy
```

### Reiniciar Servicios

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Todos los servicios
docker compose --env-file .env restart

# Servicio específico
docker compose --env-file .env restart php-fpm
docker compose --env-file .env restart nginx-api
```

### Backup Manual

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Crear backup
mkdir -p backups
docker compose --env-file .env exec -T db pg_dump -U postgres yape_notifier | gzip > backups/backup_$(date +%Y%m%d_%H%M%S).sql.gz

# O usar el script
chmod +x backup.sh
./backup.sh
```

### Restaurar Backup

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Restaurar desde backup
gunzip < backups/backup_20250115_143022.sql.gz | docker compose --env-file .env exec -T db psql -U postgres yape_notifier
```

### Limpiar Todo y Reconstruir

```bash
cd /var/apps/yape-notifier/infra/docker/environments/production

# Detener y eliminar contenedores
docker compose --env-file .env down

# Eliminar volúmenes (¡CUIDADO! Esto borra la base de datos)
# docker compose --env-file .env down -v

# Reconstruir desde cero
./deploy.sh --no-cache
```

---

## 🎯 Checklist Pre-Despliegue

Antes de desplegar, verifica:

- [ ] Código actualizado en el servidor (`git pull`)
- [ ] `.env` configurado con todas las variables necesarias
- [ ] `DB_PASSWORD` configurado y seguro
- [ ] `APP_KEY` configurado (o se generará automáticamente)
- [ ] Variables de Reverb configuradas (si usas WebSockets)
- [ ] `SANCTUM_STATEFUL_DOMAINS` y `SESSION_DOMAIN` configurados
- [ ] Backup de base de datos (si es actualización)
- [ ] DNS configurado correctamente (`api.notificaciones.space`, `dashboard.notificaciones.space`)
- [ ] Puertos 80 y 443 disponibles
- [ ] `composer.lock` actualizado y compatible con PHP 8.2

---

## 📚 Documentación Relacionada

- [DEPLOY_GUIDE_PRODUCTION.md](DEPLOY_GUIDE_PRODUCTION.md) - Guía detallada con más ejemplos
- [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) - Checklist específico para nuevas features
- [DOCKER.md](DOCKER.md) - Documentación de la infraestructura Docker
- [DIGITAL_OCEAN_DEPLOYMENT.md](DIGITAL_OCEAN_DEPLOYMENT.md) - Guía específica para DigitalOcean

---

## 🆘 Soporte

Si encuentras problemas:

1. **Revisa los logs:** `docker compose --env-file .env logs -f`
2. **Verifica healthchecks:** `docker compose --env-file .env ps`
3. **Ejecuta scripts de diagnóstico:** `./diagnose-health.sh`
4. **Consulta la documentación:** `docs/02-deployment/`

---

**¡Despliegue exitoso!** 🎉





