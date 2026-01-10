# 🔄 Guía de Actualización - API y Dashboard

> **Referencias relacionadas:**
>
> - [ACTUALIZACION_PARCIAL.md](ACTUALIZACION_PARCIAL.md) - Actualización parcial (solo Dashboard o solo API)
> - [DEPLOYMENT.md](DEPLOYMENT.md) - Guía completa de despliegue
> - [DEPLOYMENT_COMMANDS.md](DEPLOYMENT_COMMANDS.md) - Comandos rápidos de despliegue
> - [UPDATE_CHECKLIST.md](../06-operations/UPDATE_CHECKLIST.md) - Checklist de actualización

Guía profesional para actualizar la API (Laravel) y el Dashboard (React) en producción.

---

## 📋 Resumen Ejecutivo

**Respuesta corta**: **SÍ, debes ejecutar el script `update.sh`**. No es suficiente solo reconstruir Docker manualmente.

### ¿Por qué usar el script y no solo `docker compose build`?

El script `update.sh` hace mucho más que solo reconstruir imágenes:

1. ✅ **Backup automático** de la base de datos antes de actualizar
2. ✅ **Validación de `composer.lock`** (compatibilidad con PHP 8.2 LTS)
3. ✅ **Limpieza de caches** de Laravel (config, route, view, cache)
4. ✅ **Ejecución de migraciones** con manejo inteligente de errores
5. ✅ **Regeneración de package discovery** (solo dependencias de producción)
6. ✅ **Verificación de healthchecks** de todos los servicios
7. ✅ **Generación de script de rollback** automático
8. ✅ **Optimización de caches** para producción después de migraciones

Si solo haces `docker compose build`, te perderás estos pasos críticos y podrías tener problemas.

---

## 🎯 Proceso de Actualización (Paso a Paso)

### Escenario: Actualizar API o Dashboard después de hacer cambios en el código

#### **PASO 1: Actualizar código en el servidor**

```bash
# Conectarse al servidor
ssh deploy@tu-servidor
# O: ssh root@tu-servidor

# Ir al directorio del proyecto
cd /var/apps/yape-notifier

# Actualizar código desde el repositorio
git pull origin tenant-version
# O la rama que uses: git pull origin main
```

#### **PASO 2: Ir al directorio de producción**

```bash
cd infra/docker/environments/production
```

#### **PASO 3: Ejecutar script de actualización**

```bash
# Hacer el script ejecutable (solo primera vez)
chmod +x update.sh

# Ejecutar actualización (con backup automático)
./update.sh
```

El script `update.sh` automáticamente:

1. **Crea backup** de la base de datos (comprimido en `./backups/`)
2. **Genera script de rollback** automático
3. **Valida** que el código esté actualizado (te pregunta confirmación)
4. **Valida `composer.lock`** (compatibilidad con PHP 8.2 LTS)
5. **Reconstruye imágenes Docker** (con BuildKit para cache optimizado)
6. **Limpia caches** de Laravel
7. **Regenera package discovery** (solo dependencias de producción)
8. **Ejecuta migraciones** (con manejo inteligente de errores)
9. **Reinicia servicios** (docker compose up -d)
10. **Verifica healthchecks** de todos los servicios
11. **Optimiza caches** para producción

#### **PASO 4: Verificar que todo funciona**

```bash
# Ver estado de servicios
docker compose --env-file .env ps

# Verificar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate:status

# Probar API
curl -f https://api.notificaciones.space/up

# Ver logs si hay problemas
docker compose --env-file .env logs -f
```

---

## 🔄 Comparación: `deploy.sh` vs `update.sh`

### Cuándo usar `deploy.sh` (Despliegue inicial o reconstrucción completa)

```bash
./deploy.sh
# O sin cache: ./deploy.sh --no-cache
```

**Usa `deploy.sh` cuando:**

- ✅ Es el **primer despliegue** en el servidor
- ✅ Necesitas **reconstrucción completa** sin cache
- ✅ Cambiaste **configuración de Docker** (Dockerfile, docker-compose.yml)
- ✅ Cambiaste **variables de entorno críticas** (DB_PASSWORD, APP_KEY)
- ✅ Quieres **validar todo desde cero**

**Lo que hace `deploy.sh`:**

1. Valida configuración (.env, DB_PASSWORD, APP_KEY)
2. Valida `composer.lock` (PHP 8.2 LTS)
3. **Detiene contenedores** (docker compose down)
4. **Construye imágenes** (con BuildKit)
5. **Inicia servicios** (docker compose up -d)
6. Espera a PostgreSQL (wait loop activo)
7. Genera APP_KEY si no existe (solo primera vez)
8. Configura permisos
9. Limpia caches
10. Ejecuta migraciones
11. Optimiza Laravel
12. Verifica healthchecks

### Cuándo usar `update.sh` (Actualización de código existente)

```bash
./update.sh
```

**Usa `update.sh` cuando:**

- ✅ Ya tienes el sistema **desplegado y funcionando**
- ✅ Solo actualizaste **código de la aplicación** (PHP, React)
- ✅ Hiciste **git pull** y quieres aplicar los cambios
- ✅ Necesitas **backup automático** antes de actualizar
- ✅ Quieres **rollback fácil** si algo falla

**Lo que hace `update.sh`:**

1. **Crea backup** de la base de datos (comprimido)
2. **Genera script de rollback** automático
3. Valida que el código esté actualizado (pregunta confirmación)
4. Valida `composer.lock` (PHP 8.2 LTS)
5. **Reconstruye imágenes** (con BuildKit, sin cache)
6. Limpia caches de Laravel
7. Regenera package discovery
8. Ejecuta migraciones (con manejo inteligente)
9. **Reinicia servicios** (docker compose up -d)
10. Verifica healthchecks
11. Optimiza caches

**Diferencia clave**: `update.sh` hace **backup automático** y genera **rollback script**, mientras que `deploy.sh` no (porque asume despliegue inicial).

---

## ⚠️ ¿Qué pasa si solo reconstruyo Docker manualmente?

Si ejecutas solo:

```bash
docker compose --env-file .env build
docker compose --env-file .env up -d
```

**Problemas que tendrás:**

1. ❌ **No hay backup** → Si algo falla, no puedes hacer rollback fácil
2. ❌ **Caches no se limpian** → Puedes tener código antiguo en cache
3. ❌ **Migraciones no se ejecutan** → Base de datos desactualizada
4. ❌ **Package discovery no se regenera** → Dependencias de desarrollo pueden quedar en cache
5. ❌ **No se valida `composer.lock`** → Puedes tener incompatibilidades
6. ❌ **No se optimizan caches** → Performance subóptima en producción

**Resultado**: Tu aplicación puede funcionar parcialmente o tener bugs sutiles.

---

## 🚨 Rollback (Si algo sale mal)

El script `update.sh` genera automáticamente un script de rollback:

```bash
# Ubicación del rollback
./backups/rollback_YYYYMMDD_HHMMSS.sh

# Ejecutar rollback
./backups/rollback_YYYYMMDD_HHMMSS.sh
```

El script de rollback:

1. Detiene servicios
2. Restaura backup de la base de datos
3. Reinicia servicios

---

## 📝 Checklist de Actualización

Antes de ejecutar `update.sh`:

- [ ] Código actualizado en el servidor (`git pull`)
- [ ] `composer.lock` actualizado y commiteado (si cambiaste dependencias)
- [ ] Migraciones creadas y commiteadas (si hay cambios en BD)
- [ ] Variables de entorno verificadas (`.env` no necesita cambios normalmente)
- [ ] Backup manual adicional (opcional, pero recomendado para cambios grandes)

Después de ejecutar `update.sh`:

- [ ] Todos los servicios están "healthy" (`docker compose ps`)
- [ ] API responde (`curl https://api.notificaciones.space/up`)
- [ ] Dashboard carga correctamente (abrir en navegador)
- [ ] Migraciones ejecutadas (`php artisan migrate:status`)
- [ ] No hay errores en logs (`docker compose logs`)

---

## 🔧 Solución de Problemas Comunes

### Error: "composer.lock está desactualizado"

```bash
# En tu máquina local o en el servidor
cd apps/api

# Actualizar composer.lock con PHP 8.2
docker run --rm -v $(pwd):/app -w /app php:8.2-cli sh -c \
  'curl -sS https://getcomposer.org/installer | php && php composer.phar update --no-interaction'

# Commitear y pushear
git add composer.lock
git commit -m "fix: update composer.lock for PHP 8.2 LTS"
git push

# Luego ejecutar update.sh de nuevo
```

### Error: "Migraciones fallan con 'Duplicate table'"

```bash
# El script update.sh intenta sincronizar automáticamente
# Si falla, ejecuta manualmente:
cd infra/docker/environments/production
./fix-migrations.sh
```

### Error: "Servicios unhealthy después de actualizar"

```bash
# Diagnosticar problemas de healthcheck
cd infra/docker/environments/production
./diagnose-health.sh

# Ver logs específicos
docker compose --env-file .env logs php-fpm
docker compose --env-file .env logs nginx-api
docker compose --env-file .env logs dashboard
```

---

## 📚 Referencias

- **Script de actualización**: `infra/docker/environments/production/update.sh`
- **Script de despliegue**: `infra/docker/environments/production/deploy.sh`
- **Guía completa de deployment**: `docs/02-deployment/DEPLOYMENT.md`
- **Comandos rápidos**: `COMANDOS_DEPLOY_PRODUCCION.txt`

---

## 🎨 Actualización Solo del Dashboard (Optimizada)

Si **solo modificaste el código del dashboard** (React/Vite) y no hay cambios en:

- ❌ Backend (Laravel/PHP)
- ❌ Base de datos (migraciones)
- ❌ Configuración de Docker
- ❌ Variables de entorno críticas

Entonces puedes usar un proceso **más rápido y ligero** que no requiere backup ni migraciones.

### Proceso Optimizado para Solo Dashboard

#### Opción A: Usar Script Automatizado (Recomendado)

```bash
# 1. Actualizar código en el servidor
ssh deploy@tu-servidor
cd /var/apps/yape-notifier
git pull origin tenant-version

# 2. Ir al directorio de producción
cd infra/docker/environments/production

# 3. Ejecutar script optimizado para dashboard
chmod +x update-dashboard.sh
./update-dashboard.sh
```

El script `update-dashboard.sh` automáticamente:

- ✅ Verifica que el código esté actualizado
- ✅ Reconstruye la imagen del dashboard (con BuildKit)
- ✅ Reinicia el contenedor del dashboard
- ✅ Verifica healthcheck y respuesta del dashboard

#### Opción B: Proceso Manual

```bash
# 1. Actualizar código en el servidor
ssh deploy@tu-servidor
cd /var/apps/yape-notifier
git pull origin tenant-version

# 2. Ir al directorio de producción
cd infra/docker/environments/production

# 3. Reconstruir SOLO el servicio dashboard (con BuildKit para cache)
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose --env-file .env build dashboard

# 4. Reiniciar SOLO el contenedor dashboard
docker compose --env-file .env up -d dashboard

# 5. Verificar que el dashboard funciona
curl -f https://dashboard.notificaciones.space/health
# O abrir en navegador: https://dashboard.notificaciones.space
```

### ¿Por qué es más rápido?

- ✅ **No hace backup** (no hay cambios en BD)
- ✅ **No ejecuta migraciones** (no hay cambios en BD)
- ✅ **No limpia caches de Laravel** (no hay cambios en backend)
- ✅ **No valida composer.lock** (no hay cambios en PHP)
- ✅ **Solo reconstruye el dashboard** (React/Vite)
- ✅ **Solo reinicia el contenedor dashboard**

### Tiempo estimado

- **Proceso completo (`update.sh`)**: 5-10 minutos (backup, validaciones, migraciones, etc.)
- **Solo dashboard**: 2-3 minutos (solo build de React + reinicio)

### Cuándo usar cada proceso

| Escenario                                     | Proceso Recomendado                            |
| --------------------------------------------- | ---------------------------------------------- |
| Solo cambios en `apps/web-dashboard/` (React) | **Solo dashboard** (proceso optimizado arriba) |
| Cambios en `apps/api/` (Laravel)              | `./update.sh` (proceso completo)               |
| Cambios en ambos (API + Dashboard)            | `./update.sh` (proceso completo)               |
| Cambios en migraciones/BD                     | `./update.sh` (proceso completo)               |
| Cambios en Dockerfiles o docker-compose.yml   | `./deploy.sh` (reconstrucción completa)        |

### Verificación Post-Actualización (Solo Dashboard)

```bash
# 1. Verificar que el contenedor está corriendo
docker compose --env-file .env ps dashboard

# 2. Verificar healthcheck
docker compose --env-file .env ps dashboard | grep healthy

# 3. Verificar que responde
curl -f https://dashboard.notificaciones.space/health

# 4. Ver logs si hay problemas
docker compose --env-file .env logs dashboard --tail=50

# 5. Abrir en navegador y verificar visualmente
# https://dashboard.notificaciones.space
```

### Si algo falla (Rollback del Dashboard)

Si el nuevo dashboard tiene problemas, puedes hacer rollback rápido:

```bash
# 1. Ver imágenes disponibles
docker images | grep dashboard

# 2. Si tienes una imagen anterior, puedes recrear el contenedor con esa imagen
# O simplemente hacer git checkout a un commit anterior y reconstruir

# 3. Volver a un commit anterior
cd /var/apps/yape-notifier
git log --oneline -10  # Ver últimos commits
git checkout <commit-anterior>  # Volver a commit anterior

# 4. Reconstruir dashboard
cd infra/docker/environments/production
docker compose --env-file .env build dashboard
docker compose --env-file .env up -d dashboard
```

---

## ✅ Resumen Final

### Para actualizar **Solo Dashboard** (React):

```bash
git pull
cd infra/docker/environments/production
docker compose --env-file .env build dashboard
docker compose --env-file .env up -d dashboard
```

### Para actualizar **API o ambos** (API + Dashboard):

```bash
git pull
cd infra/docker/environments/production
./update.sh  ← **Este es el paso crítico**
```

**NO hagas solo `docker compose build`** sin el script cuando hay cambios en la API - te perderás pasos críticos como backup, limpieza de caches, migraciones, etc.

El script `update.sh` está diseñado para ser **seguro, automatizado y con rollback fácil**. Úsalo siempre para actualizaciones que involucren el backend.
