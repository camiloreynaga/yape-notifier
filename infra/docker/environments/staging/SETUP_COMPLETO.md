# 🚀 Guía Completa de Configuración de Staging

Guía paso a paso para configurar completamente el entorno de staging.

---

## 📋 Checklist Pre-Configuración

Antes de empezar, asegúrate de tener:

- [ ] ✅ Acceso SSH al servidor
- [ ] ✅ Permisos de sudo (si es necesario)
- [ ] ✅ Docker y Docker Compose instalados
- [ ] ✅ Código del proyecto clonado en el servidor
- [ ] ✅ Acceso al panel DNS (si quieres usar subdominios)

---

## 🔧 Paso 1: Configuración Inicial

### 1.1. Conectarse al Servidor

```bash
ssh deploy@tu-servidor
# O: ssh root@tu-servidor
```

### 1.2. Ir al Directorio de Staging

```bash
cd /var/apps/yape-notifier/infra/docker/environments/staging
```

### 1.3. Ejecutar Setup Inicial

```bash
# Hacer ejecutable (solo primera vez)
chmod +x setup.sh

# Ejecutar setup
./setup.sh
```

Esto creará el archivo `.env` desde `.env.example`.

---

## ⚙️ Paso 2: Configurar Variables de Entorno

### 2.1. Editar Archivo .env

```bash
nano .env
```

### 2.2. Configurar Variables Obligatorias

**⚠️ IMPORTANTE:** Cambia estos valores:

```env
# ⚠️ OBLIGATORIO: Contraseña diferente a producción
DB_PASSWORD=tu_contraseña_staging_segura_aqui

# Opción 1: Con subdominios (si tienes DNS configurado)
APP_URL=http://staging-api.notificaciones.space
DASHBOARD_API_URL=http://staging-api.notificaciones.space

# Opción 2: Por puerto (si no tienes DNS)
APP_URL=http://localhost:8080
DASHBOARD_API_URL=http://localhost:8080
```

### 2.3. Verificar Configuración

```bash
# Verificar que DB_PASSWORD está configurado
grep "^DB_PASSWORD=" .env

# No debe estar vacío ni tener "TU_CONTRASEÑA"
```

---

## 🌐 Paso 3: Configurar DNS (Opcional pero Recomendado)

### 3.1. Si Quieres Usar Subdominios

Sigue la guía en `CONFIGURACION_DNS.md`:

```bash
# Leer guía
cat CONFIGURACION_DNS.md
```

### 3.2. Actualizar Caddyfile

Si configuraste DNS, edita el Caddyfile:

```bash
nano Caddyfile
```

Descomenta los bloques de subdominios y comenta el bloque `:80`.

---

## 🐳 Paso 4: Desplegar Staging

### 4.1. Ejecutar Deployment

```bash
# Hacer ejecutable (solo primera vez)
chmod +x deploy.sh

# Desplegar
./deploy.sh
```

El script:
1. ✅ Valida `composer.lock` (compatibilidad PHP 8.2)
2. ✅ Construye imágenes Docker
3. ✅ Inicia contenedores
4. ✅ Ejecuta migraciones
5. ✅ Limpia caches

### 4.2. Verificar Deployment

```bash
# Ver estado de contenedores
docker compose --env-file .env ps

# Ver logs
docker compose --env-file .env logs -f

# Probar API
curl http://localhost:8080/api/up
# O si usas subdominios:
curl http://staging-api.notificaciones.space/up
```

---

## ✅ Paso 5: Verificación Final

### 5.1. Verificar Servicios

```bash
# Ver todos los contenedores
docker compose --env-file .env ps

# Deben estar todos "Up" y saludables
```

### 5.2. Verificar API

```bash
# Health check
curl http://localhost:8080/api/up
# Debe responder: {"status":"ok"}

# O con subdominio:
curl http://staging-api.notificaciones.space/up
```

### 5.3. Verificar Dashboard

Abre en navegador:
- `http://TU_IP:8080` (puerto directo)
- `http://staging-dashboard.notificaciones.space` (subdominio)

### 5.4. Verificar Base de Datos

```bash
# Entrar al contenedor PHP
docker compose --env-file .env exec php-fpm bash

# Verificar conexión a BD
php artisan tinker
```

```php
// En tinker
\DB::connection()->getPdo();
// Debe mostrar: PDO connection object

// Verificar que la BD es staging
\Config::get('database.connections.pgsql.database');
// Debe mostrar: yape_notifier_staging

exit
```

```bash
exit
```

---

## 🔍 Paso 6: Verificar Recursos

### 6.1. Monitorear Recursos

```bash
# Hacer ejecutable (solo primera vez)
chmod +x check-resources.sh

# Verificar recursos
./check-resources.sh
```

Esto mostrará:
- Uso de CPU y RAM de staging vs producción
- Volúmenes Docker
- Redes Docker

### 6.2. Verificar Límites

```bash
# Ver límites de recursos de staging
docker inspect yape-notifier-php-fpm-staging | grep -A 10 "Resources"
```

Debe mostrar límites configurados (1 CPU, 512MB RAM).

---

## 🎯 Uso Diario

### Deploy a Staging

```bash
cd /var/apps/yape-notifier/infra/docker/environments/staging

# Actualizar código
git pull origin staging  # o la rama que uses

# Desplegar
./deploy.sh
```

### Ver Logs

```bash
# Todos los servicios
docker compose --env-file .env logs -f

# Solo PHP-FPM
docker compose --env-file .env logs -f php-fpm

# Solo base de datos
docker compose --env-file .env logs -f db
```

### Reiniciar Servicios

```bash
# Reiniciar todo
docker compose --env-file .env restart

# Reiniciar servicio específico
docker compose --env-file .env restart php-fpm
```

### Detener Staging

```bash
# Detener sin eliminar datos
docker compose --env-file .env stop

# Detener y eliminar contenedores (mantiene volúmenes)
docker compose --env-file .env down

# Detener y eliminar TODO (incluyendo volúmenes) ⚠️ CUIDADO
docker compose --env-file .env down -v
```

---

## 🐛 Troubleshooting

### Error: "DB_PASSWORD not set"

**Solución:**
```bash
# Verificar que .env existe y tiene DB_PASSWORD
grep "^DB_PASSWORD=" .env

# Si está vacío, editarlo
nano .env
# Configurar: DB_PASSWORD=tu_contraseña_segura
```

### Error: "composer.lock está desactualizado"

**Solución:**
```bash
# Actualizar composer.lock (desde tu máquina local con PHP 8.2)
cd apps/api
composer update --no-interaction
git add composer.lock
git commit -m "chore: update composer.lock"
git push
```

### Contenedores no inician

**Solución:**
```bash
# Ver logs detallados
docker compose --env-file .env logs

# Verificar recursos del servidor
docker stats

# Verificar que los puertos no están en uso
netstat -tulpn | grep 8080
```

### Dashboard no carga

**Solución:**
```bash
# Verificar que dashboard está corriendo
docker compose --env-file .env ps dashboard

# Ver logs del dashboard
docker compose --env-file .env logs dashboard

# Verificar que Caddy está enrutando correctamente
docker compose --env-file .env logs caddy
```

---

## 📚 Referencias

- **Configuración DNS:** Ver `CONFIGURACION_DNS.md`
- **Estrategia de Staging:** Ver `../../../../docs/02-deployment/STAGING_ENVIRONMENT_STRATEGY.md`
- **Docker Compose:** Ver `../../../../docs/02-deployment/DOCKER.md`

---

## ✅ Checklist Final

Después de completar todos los pasos, verifica:

- [ ] ✅ `.env` configurado con `DB_PASSWORD`
- [ ] ✅ Contenedores corriendo (`docker compose ps`)
- [ ] ✅ API responde (`curl http://localhost:8080/api/up`)
- [ ] ✅ Dashboard accesible (navegador)
- [ ] ✅ Base de datos conectada (tinker)
- [ ] ✅ Recursos limitados (check-resources.sh)
- [ ] ✅ DNS configurado (si usas subdominios)
- [ ] ✅ Logs sin errores críticos

---

**¡Staging está listo para usar!** 🎉

**Última actualización:** 2025-01-15

