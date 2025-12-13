# 🚀 Guía Completa de Deployment - Yape Notifier

Guía profesional y consolidada para desplegar Yape Notifier en producción usando Digital Ocean Droplets con subdominios.

---

## 📋 Tabla de Contenidos

1. [Arquitectura del Sistema](#arquitectura-del-sistema)
2. [Prerrequisitos](#prerrequisitos)
3. [Opción 1: Digital Ocean Droplet con Subdominios (Recomendado)](#opción-1-digital-ocean-droplet-con-subdominios-recomendado)
4. [Opción 2: Digital Ocean App Platform (Alternativa)](#opción-2-digital-ocean-app-platform-alternativa)
5. [Configuración de Variables de Entorno](#configuración-de-variables-de-entorno)
6. [Solución de Problemas](#solución-de-problemas)
7. [Mantenimiento y Actualizaciones](#mantenimiento-y-actualizaciones)
8. [Backup y Recuperación](#backup-y-recuperación)

---

## 🏗️ Arquitectura del Sistema

```
Internet
   │
   ▼
[ Caddy :80, :443 ]
   │ (HTTPS automático con Let's Encrypt)
   ├─► api.notificaciones.space → [ Nginx API :80 ] → [ PHP-FPM :9000 ] → [ Laravel API ]
   └─► dashboard.notificaciones.space → [ Dashboard :80 ] → [ React App ]
   │
   ▼
[ PostgreSQL :5432 ]
   (interno, no expuesto)
```

### Componentes

- **Caddy**: Reverse proxy con HTTPS automático (Let's Encrypt)
- **Nginx API**: Servidor web para Laravel (PHP-FPM)
- **PHP-FPM**: Aplicación Laravel 11
- **Dashboard**: Frontend React servido por Nginx
- **PostgreSQL**: Base de datos (no expuesta públicamente)

---

## ✅ Prerrequisitos

Antes de comenzar, asegúrate de tener:

- ✅ Cuenta en Digital Ocean
- ✅ Dominio configurado: `notificaciones.space`
- ✅ Acceso SSH al Droplet
- ✅ Repositorio en GitHub/GitLab (o acceso al código)
- ✅ Conocimientos básicos de Linux y Docker

---

## 🐳 Opción 1: Digital Ocean Droplet con Subdominios (Recomendado)

Esta es la opción recomendada para producción, ya que ofrece:

- ✅ Control total sobre la infraestructura
- ✅ HTTPS automático con Caddy
- ✅ Subdominios profesionales (`api.notificaciones.space`, `dashboard.notificaciones.space`)
- ✅ Mejor rendimiento y escalabilidad
- ✅ Costo optimizado

### Paso 1: Crear el Droplet

1. **Inicia sesión** en [Digital Ocean Dashboard](https://cloud.digitalocean.com/)

2. **Crea un nuevo Droplet:**

   - Haz clic en **"Create"** → **"Droplets"**
   - **Imagen**: Ubuntu 22.04 LTS (o la más reciente)
   - **Plan**:
     - Mínimo: **Basic - $12/mes** (2GB RAM, 1 vCPU)
     - Recomendado: **Basic - $24/mes** (4GB RAM, 2 vCPU) para mejor rendimiento
   - **Región**: Elige la más cercana a tus usuarios
   - **Autenticación**:
     - ✅ **SSH keys** (recomendado) - Agrega tu clave pública SSH
     - O contraseña (menos seguro)
   - **Hostname**: `yape-notifier-prod` (o el que prefieras)
   - **Tags**: Opcional (ej: `production`, `yape-notifier`)

3. **Haz clic en "Create Droplet"**

4. **Anota la IP del Droplet** - La necesitarás para configurar DNS

### Paso 2: Configurar DNS (Subdominios)

Antes de continuar, configura los subdominios en tu proveedor de DNS.

#### 2.1. Obtener la IP del Droplet

En Digital Ocean Dashboard, ve a tu Droplet y copia la **IP pública** (ej: `157.230.45.123`)

#### 2.2. Configurar Registros DNS

Ve a tu proveedor de DNS (donde compraste el dominio) y agrega los siguientes registros:

**Opción A: Registros A (Recomendado)**

```
Tipo: A
Nombre: api
Valor: 157.230.45.123  (IP de tu Droplet)
TTL: 3600 (o el mínimo)

Tipo: A
Nombre: dashboard
Valor: 157.230.45.123  (IP de tu Droplet)
TTL: 3600 (o el mínimo)
```

**Opción B: Registro A Wildcard (Alternativa)**

Si prefieres usar un solo registro para todos los subdominios:

```
Tipo: A
Nombre: *
Valor: 157.230.45.123  (IP de tu Droplet)
TTL: 3600
```

#### 2.3. Verificar Propagación DNS

Espera 5-15 minutos y verifica que los DNS se hayan propagado:

```bash
# En tu máquina local
nslookup api.notificaciones.space
nslookup dashboard.notificaciones.space

# Deberías ver la IP de tu Droplet
```

**Nota**: La propagación DNS puede tardar hasta 24 horas, pero generalmente es mucho más rápido.

### Paso 3: Configuración Inicial del Servidor

#### 3.1. Conectarse al Droplet

```bash
# Reemplaza con tu IP y usuario
ssh root@TU_IP_DROPLET

# O si configuraste un usuario:
ssh usuario@TU_IP_DROPLET
```

#### 3.2. Actualizar el Sistema

```bash
# Actualizar paquetes
apt update && apt upgrade -y

# Instalar herramientas básicas
apt install -y curl wget git nano ufw
```

#### 3.3. Configurar Firewall

```bash
# Permitir SSH
ufw allow 22/tcp

# Permitir HTTP y HTTPS (para Caddy)
ufw allow 80/tcp
ufw allow 443/tcp

# Activar firewall
ufw --force enable

# Verificar estado
ufw status
```

### Paso 4: Instalar Docker y Docker Compose

#### 4.1. Instalar Docker

```bash
# Descargar script de instalación
curl -fsSL https://get.docker.com -o get-docker.sh

# Ejecutar instalación
sh get-docker.sh

# Agregar usuario actual al grupo docker (si no eres root)
# usermod -aG docker $USER

# Verificar instalación
docker --version
```

#### 4.2. Instalar Docker Compose Plugin

```bash
# Instalar Docker Compose Plugin
apt install docker-compose-plugin -y

# Verificar instalación
docker compose version
```

#### 4.3. Verificar que Docker Funciona

```bash
# Probar Docker
docker run hello-world

# Si ves "Hello from Docker!", todo está bien
```

**⚠️ Si obtienes error "Cannot connect to the Docker daemon":**

Si estás en el grupo `docker` pero aún no puedes conectarte:

```bash
# Opción 1: Reiniciar sesión SSH (recomendado)
exit
# Luego vuelve a conectarte
ssh deploy@TU_SERVIDOR

# Opción 2: Activar el grupo sin cerrar sesión
newgrp docker

# Opción 3: Verificar que el servicio Docker esté corriendo
sudo systemctl status docker
sudo systemctl start docker  # Si no está corriendo
```

### Paso 5: Clonar el Repositorio

#### 5.1. Crear Directorio para la Aplicación

```bash
# Crear directorio
mkdir -p /var/apps
cd /var/apps
```

#### 5.2. Clonar el Repositorio

```bash
# Si es un repositorio público
git clone https://github.com/TU_USUARIO/yape-notifier.git

# Si es privado, necesitarás configurar SSH keys o usar HTTPS con token
# git clone git@github.com:TU_USUARIO/yape-notifier.git

# Entrar al directorio
cd yape-notifier
```

#### 5.3. Verificar Estructura

```bash
# Verificar que existe la estructura
ls -la infra/docker/

# Deberías ver:
# - docker-compose.yml
# - caddy/
# - nginx/
# - etc.
```

### Paso 6: Configurar Variables de Entorno

**⚠️ IMPORTANTE**: Este paso es **OBLIGATORIO** antes de construir las imágenes Docker.

#### 6.1. Entendiendo las Variables de Entorno en Docker Compose

**Cómo funciona Docker Compose con variables de entorno:**

1. **Interpolación de variables** (`${VARIABLE}`): Docker Compose resuelve estas variables **antes** de crear los contenedores. Busca las variables en:

   - Variables del shell actual
   - Archivo `.env` en el mismo directorio (carga automática)
   - Variables del sistema
   - Archivo especificado con `--env-file` (mejor práctica)

2. **Variables en contenedores** (`env_file`): Estas se cargan **dentro** del contenedor después de la creación.

**Problema común**: Si usas `${DB_PASSWORD}` en `docker-compose.yml`, Docker Compose necesita resolverla **antes** de crear el contenedor. Si solo está en `.env.production` (usado por `env_file`), no la encontrará para la interpolación.

**Solución profesional**: Usar `--env-file .env.production` explícitamente en todos los comandos de Docker Compose. Esto asegura que las variables estén disponibles tanto para interpolación como para los contenedores.

#### 6.2. Crear Archivo .env.production

```bash
# Ir al directorio de Docker
cd /var/apps/yape-notifier/infra/docker

# Verificar que existe la plantilla
ls -la .env.production.example

# Crear archivo .env.production desde la plantilla
cp .env.production.example .env.production

# Editar el archivo con tus valores reales
nano .env.production
```

**⚠️ Si no creas este archivo o DB_PASSWORD está vacío, obtendrás el error: "The DB_PASSWORD variable is not set" o "Database is uninitialized and superuser password is not specified"**

#### 6.3. Configurar Variables de Entorno

El archivo `.env.production.example` contiene todas las variables necesarias con valores de ejemplo. Después de copiarlo a `.env.production`, ajusta los siguientes valores:

**Variables que DEBES configurar:**

```env
# ============================================
# Base de Datos PostgreSQL
# ============================================
DB_CONNECTION=pgsql
DB_HOST=db
DB_PORT=5432
DB_DATABASE=yape_notifier
DB_USERNAME=postgres
DB_PASSWORD=TU_CONTRASEÑA_SEGURA_AQUI

# ============================================
# Aplicación Laravel
# ============================================
APP_NAME="Yape Notifier API"
APP_ENV=production
APP_DEBUG=false
APP_KEY=base64:TU_CLAVE_AQUI  # Se generará después
APP_URL=https://api.notificaciones.space
APP_TIMEZONE=UTC

# ============================================
# Sesiones y Cache
# ============================================
SESSION_DRIVER=database
SESSION_LIFETIME=120
CACHE_DRIVER=file
QUEUE_CONNECTION=database

# ============================================
# Logs
# ============================================
LOG_CHANNEL=stderr
LOG_LEVEL=error

# ============================================
# Dashboard
# ============================================
DASHBOARD_API_URL=https://api.notificaciones.space

# ============================================
# CORS (si es necesario)
# ============================================
CORS_ALLOWED_ORIGINS=https://dashboard.notificaciones.space
```

**Variables importantes a configurar:**

1. **`DB_PASSWORD`**: Cambia `TU_CONTRASEÑA_SEGURA_AQUI` por una contraseña fuerte para PostgreSQL
2. **`APP_KEY`**: Se generará automáticamente después del primer despliegue (puedes dejarlo vacío inicialmente)
3. **`APP_URL`**: Ya está configurado para `https://api.notificaciones.space` (verificar si es correcto)
4. **`DASHBOARD_API_URL`**: Ya está configurado para `https://api.notificaciones.space` (verificar si es correcto)

**Nota**: El archivo `.env.production.example` contiene todas las variables con valores por defecto. Solo necesitas ajustar las mencionadas arriba.

#### 6.3. Guardar y Salir

```bash
# En nano: Ctrl+O (guardar), Enter, Ctrl+X (salir)
```

### Paso 7: Configurar Caddy para Subdominios

Caddy manejará automáticamente HTTPS con Let's Encrypt para tus subdominios.

#### 7.1. Editar Caddyfile

```bash
# Editar Caddyfile
nano /var/apps/yape-notifier/infra/docker/caddy/Caddyfile
```

#### 7.2. Configurar Subdominios

Asegúrate de que el Caddyfile tenga esta configuración:

```caddy
# API - api.notificaciones.space
api.notificaciones.space {
    # Reverse proxy a Nginx API
    reverse_proxy nginx-api:80 {
        header_up X-Real-IP {remote_host}
        header_up X-Forwarded-For {remote_host}
        header_up X-Forwarded-Proto {scheme}
        header_up Host {host}

        health_uri /up
        health_interval 30s
        health_timeout 5s
    }

    # Logging
    log {
        output file /var/log/caddy/api.log
        format json
    }

    # Compression
    encode gzip zstd
}

# Dashboard - dashboard.notificaciones.space
dashboard.notificaciones.space {
    # Reverse proxy a Dashboard
    reverse_proxy dashboard:80 {
        header_up X-Real-IP {remote_host}
        header_up X-Forwarded-For {remote_host}
        header_up X-Forwarded-Proto {scheme}
        header_up Host {host}

        health_uri /health
        health_interval 30s
        health_timeout 5s
    }

    # Logging
    log {
        output file /var/log/caddy/dashboard.log
        format json
    }

    # Compression
    encode gzip zstd
}
```

**Nota**: El dominio `notificaciones.space` ya está configurado. Si usas otro dominio, actualiza estos valores.

#### 7.3. Guardar y Salir

```bash
# Guardar: Ctrl+O, Enter, Ctrl+X
```

### Paso 8: Desplegar la Aplicación

#### 8.1. Construir las Imágenes Docker

```bash
# Asegúrate de estar en el directorio correcto
cd /var/apps/yape-notifier/infra/docker

# Construir imágenes (esto puede tardar varios minutos)
docker compose build
```

#### 8.2. Iniciar los Servicios

```bash
# Iniciar todos los servicios
docker compose up -d

# Verificar que los contenedores estén corriendo
docker compose ps
```

Deberías ver algo como:

```
NAME                        STATUS
yape-notifier-php-fpm       Up
yape-notifier-nginx-api     Up
yape-notifier-dashboard     Up
yape-notifier-caddy         Up
yape-notifier-db            Up
```

#### 8.3. Ver Logs (Opcional)

```bash
# Ver logs de todos los servicios
docker compose logs -f

# O ver logs de un servicio específico
docker compose logs -f caddy
docker compose logs -f php-fpm
```

### Paso 9: Verificar el Despliegue

#### 9.1. Verificar Contenedores

```bash
# Ver estado de contenedores
docker compose ps

# Todos deberían estar "Up" y "healthy"
```

#### 9.2. Generar APP_KEY de Laravel

```bash
# Generar APP_KEY
docker compose exec php-fpm php artisan key:generate --show

# Copiar la clave generada (ej: base64:xxxxx...)
# Editar .env.production y actualizar APP_KEY
nano .env.production

# Reiniciar PHP-FPM
docker compose restart php-fpm
```

#### 9.3. Ejecutar Migraciones

```bash
# Ejecutar migraciones de base de datos
docker compose exec php-fpm php artisan migrate --force
```

#### 9.4. Configurar Permisos

```bash
# Configurar permisos de storage
docker compose exec php-fpm chown -R www-data:www-data /var/www/storage
docker compose exec php-fpm chown -R www-data:www-data /var/www/bootstrap/cache
docker compose exec php-fpm chmod -R 775 /var/www/storage
docker compose exec php-fpm chmod -R 775 /var/www/bootstrap/cache
```

#### 9.5. Optimizar Laravel

```bash
# Cachear configuración
docker compose exec php-fpm php artisan config:cache
docker compose exec php-fpm php artisan route:cache
docker compose exec php-fpm php artisan view:cache
```

#### 9.6. Verificar Health Checks

```bash
# Verificar API (desde el servidor)
curl http://localhost/up

# Deberías ver: OK

# Verificar Dashboard (desde el servidor)
curl http://localhost/health

# Deberías ver una respuesta de salud
```

#### 9.7. Verificar desde el Navegador

Espera unos minutos para que Caddy obtenga los certificados SSL automáticamente, luego:

1. **Abre en tu navegador:**

   - API: `https://api.notificaciones.space/up`
   - Dashboard: `https://dashboard.notificaciones.space`

2. **Verifica que:**
   - ✅ El certificado SSL esté activo (candado verde)
   - ✅ La API responda correctamente
   - ✅ El Dashboard cargue sin errores

---

## ☁️ Opción 2: Digital Ocean App Platform (Alternativa)

Digital Ocean App Platform es un servicio PaaS que facilita el despliegue automático desde GitHub, ideal si prefieres menos configuración manual.

### Ventajas y Desventajas

**Ventajas:**

- ✅ Despliegue automático desde GitHub
- ✅ Escalado automático
- ✅ SSL/HTTPS incluido
- ✅ Menos configuración manual

**Desventajas:**

- ❌ Menos control sobre la infraestructura
- ❌ Más costoso
- ❌ URLs largas por defecto (a menos que uses dominio personalizado)

### Pasos Rápidos

1. **Crear Base de Datos PostgreSQL** en Digital Ocean Dashboard → Databases
2. **Crear App API** en App Platform:
   - Source Directory: `apps/api`
   - Build Command: `composer install --no-dev --optimize-autoloader --no-interaction`
   - Run Command: `php artisan migrate --force && php artisan config:cache && php artisan route:cache && php artisan serve --host=0.0.0.0 --port=$PORT`
3. **Crear App Dashboard** en App Platform:
   - Source Directory: `apps/web-dashboard`
   - Build Command: `npm ci && npm run build`
   - Run Command: `npx serve -s dist -l $PORT`
4. **Configurar Variables de Entorno** (ver sección correspondiente)
5. **Configurar Dominio Personalizado** (opcional)

> 📖 **Nota**: Para una guía detallada de App Platform, consulta la documentación oficial de Digital Ocean.

---

## ⚙️ Configuración de Variables de Entorno

### Archivos de Plantilla Disponibles

El proyecto incluye archivos de ejemplo con todas las variables necesarias:

- **`infra/docker/.env.production.example`** - Plantilla para producción
- **`infra/docker/.env.staging.example`** - Plantilla para staging

### Crear Archivos .env desde Plantillas

```bash
# Producción
cd infra/docker
cp .env.production.example .env.production
nano .env.production  # Configurar valores reales

# Staging
cp .env.staging.example .env.staging
nano .env.staging  # Configurar valores reales
```

### Variables Requeridas para Producción

El archivo `.env.production.example` contiene todas las variables. Las más importantes a configurar son:

```env
# Base de Datos (OBLIGATORIO)
DB_PASSWORD=TU_CONTRASEÑA_SEGURA_AQUI  # ⚠️ Cambiar esto

# Aplicación Laravel
APP_KEY=                              # Se genera automáticamente
APP_URL=https://api.notificaciones.space  # Ya configurado

# Dashboard
DASHBOARD_API_URL=https://api.notificaciones.space  # Ya configurado

# CORS
CORS_ALLOWED_ORIGINS=https://dashboard.notificaciones.space  # Ya configurado
```

**Todas las demás variables** ya están configuradas con valores por defecto en el archivo `.env.production.example`.

### Generar APP_KEY

```bash
# Desde el contenedor
docker compose exec php-fpm php artisan key:generate --show

# Copia la clave generada y actualiza .env.production
```

---

## 🐛 Solución de Problemas

### Error: "The DB_PASSWORD variable is not set"

**Causa**: El archivo `.env.production` no existe o no tiene la variable `DB_PASSWORD` configurada.

**Solución**:

```bash
# 1. Ir al directorio de Docker
cd /var/apps/yape-notifier/infra/docker

# 2. Verificar si existe .env.production
ls -la .env.production

# 3. Si no existe, crearlo desde la plantilla
cp .env.production.example .env.production

# 4. Editar y configurar DB_PASSWORD (OBLIGATORIO)
nano .env.production
# Busca la línea: DB_PASSWORD=TU_CONTRASEÑA_SEGURA_AQUI
# Cámbiala por: DB_PASSWORD=tu_contraseña_real_aqui

# 5. Verificar que se guardó correctamente
grep DB_PASSWORD .env.production

# 6. Ahora intentar de nuevo
docker compose up -d
```

**⚠️ IMPORTANTE**: `DB_PASSWORD` es **OBLIGATORIO** y debe tener un valor. No puede estar vacío.

### Error: "Certificate not obtained" (Caddy)

**Causa**: DNS no propagado o subdominios incorrectos

**Solución**:

```bash
# Verificar DNS
nslookup api.notificaciones.space
nslookup dashboard.notificaciones.space

# Ver logs de Caddy
docker compose logs caddy

# Verificar que los subdominios en Caddyfile coincidan con DNS
```

### Error: "502 Bad Gateway"

**Causa**: Nginx o PHP-FPM no están corriendo

**Solución**:

```bash
# Verificar contenedores
docker compose ps

# Reiniciar servicios
docker compose restart nginx-api php-fpm

# Ver logs
docker compose logs nginx-api
docker compose logs php-fpm
```

### Error: "Container yape-notifier-db is unhealthy" o "dependency db failed to start"

**Causa**: La base de datos PostgreSQL no puede iniciar correctamente, generalmente por:

1. `DB_PASSWORD` no configurado o vacío en `.env.production`
2. Variables de entorno incorrectas
3. Volumen de datos corrupto
4. Healthcheck fallando

**Solución paso a paso**:

```bash
# 1. Verificar que .env.production existe y tiene DB_PASSWORD
cd /var/apps/yape-notifier/infra/docker
cat .env.production | grep DB_PASSWORD

# Si no existe o está vacío, crearlo/editar:
cp .env.production.example .env.production
nano .env.production
# Asegúrate de que DB_PASSWORD tenga un valor (ej: DB_PASSWORD=tu_contraseña_segura_123)

# 2. Ver logs del contenedor de base de datos
docker compose logs db

# 3. Si hay errores de permisos o datos corruptos, eliminar el volumen (CUIDADO: esto borra los datos)
docker compose down -v
# Luego volver a levantar
docker compose up -d db

# 4. Esperar a que la base de datos esté healthy (puede tardar 30-60 segundos)
docker compose ps db
# Debe mostrar "healthy" en el estado

# 5. Si sigue fallando, iniciar solo la base de datos primero
docker compose up -d db
# Esperar 30 segundos
docker compose logs -f db
# Presiona Ctrl+C cuando veas "database system is ready to accept connections"

# 6. Luego iniciar el resto de servicios
docker compose up -d
```

**Diagnóstico avanzado**:

```bash
# Ver el estado detallado del healthcheck
docker inspect yape-notifier-db | grep -A 10 Health

# Probar conexión manual a PostgreSQL
docker compose exec db psql -U postgres -d yape_notifier -c "SELECT version();"

# Verificar variables de entorno del contenedor
docker compose exec db env | grep POSTGRES
```

### Error: "Database connection failed"

**Causa**: Variables de entorno incorrectas o base de datos no iniciada

**Solución**:

```bash
# Verificar que la base de datos esté corriendo
docker compose ps db

# Verificar variables de entorno
docker compose exec php-fpm env | grep DB_

# Probar conexión
docker compose exec php-fpm php artisan tinker
# Luego en tinker: DB::connection()->getPdo();
```

### Error: "Permission denied" en storage

**Solución**:

```bash
docker compose exec php-fpm chown -R www-data:www-data /var/www/storage
docker compose exec php-fpm chmod -R 775 /var/www/storage
```

### Dashboard no se conecta a la API

**Causa**: `VITE_API_BASE_URL` incorrecta o CORS mal configurado

**Solución**:

```bash
# Verificar variable en .env.production
grep DASHBOARD_API_URL .env.production

# Reconstruir dashboard con la URL correcta
docker compose build dashboard
docker compose up -d dashboard

# Verificar CORS en Laravel
docker compose exec php-fpm php artisan config:clear
docker compose exec php-fpm php artisan config:cache
```

### Ver Logs Detallados

```bash
# Todos los logs
docker compose logs -f

# Logs específicos
docker compose logs -f caddy
docker compose logs -f php-fpm
docker compose logs -f nginx-api
docker compose logs -f dashboard
docker compose logs -f db
```

---

## 🔄 Mantenimiento y Actualizaciones

### Actualizar Código

```bash
# Ir al directorio del proyecto
cd /var/apps/yape-notifier

# Actualizar código
git pull origin main

# Reconstruir y reiniciar
cd infra/docker
docker compose build
docker compose up -d

# Ejecutar migraciones si hay nuevas
docker compose exec php-fpm php artisan migrate --force

# Limpiar cache
docker compose exec php-fpm php artisan config:cache
docker compose exec php-fpm php artisan route:cache
```

### Reiniciar Servicios

```bash
# Reiniciar todos los servicios
docker compose restart

# Reiniciar un servicio específico
docker compose restart php-fpm
docker compose restart caddy
```

### Ver Uso de Recursos

```bash
# Ver uso de recursos de contenedores
docker stats

# Ver espacio en disco
df -h

# Ver volúmenes Docker
docker volume ls
```

### Script de Actualización Automática

Crea un script `/var/apps/yape-notifier/update.sh`:

```bash
#!/bin/bash
set -e

cd /var/apps/yape-notifier
git pull origin main
cd infra/docker

docker compose build
docker compose up -d
docker compose exec php-fpm php artisan migrate --force
docker compose exec php-fpm php artisan config:cache
docker compose exec php-fpm php artisan route:cache

echo "✅ Actualización completada"
```

Hazlo ejecutable:

```bash
chmod +x /var/apps/yape-notifier/update.sh
```

---

## 💾 Backup y Recuperación

### Backup de Base de Datos

```bash
# Crear backup
docker compose exec db pg_dump -U postgres yape_notifier > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup con compresión
docker compose exec db pg_dump -U postgres yape_notifier | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### Restaurar Backup

```bash
# Restaurar backup sin comprimir
docker compose exec -T db psql -U postgres yape_notifier < backup_20240101_120000.sql

# Restaurar backup comprimido
gunzip < backup_20240101_120000.sql.gz | docker compose exec -T db psql -U postgres yape_notifier
```

### Backup Automático (Cron)

Crea un script `/var/apps/yape-notifier/backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/var/backups/yape-notifier"
mkdir -p $BACKUP_DIR
cd /var/apps/yape-notifier/infra/docker

docker compose exec -T db pg_dump -U postgres yape_notifier | gzip > $BACKUP_DIR/backup_$(date +%Y%m%d_%H%M%S).sql.gz

# Eliminar backups más antiguos de 30 días
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +30 -delete
```

Agrega a crontab:

```bash
# Backup diario a las 2 AM
0 2 * * * /var/apps/yape-notifier/backup.sh
```

---

## ✅ Checklist de Despliegue

### Pre-Deployment

- [ ] Droplet creado y accesible
- [ ] DNS configurado y propagado
- [ ] Docker y Docker Compose instalados
- [ ] Repositorio clonado
- [ ] `.env.production` configurado correctamente
- [ ] `Caddyfile` configurado con subdominios correctos

### Deployment

- [ ] Contenedores corriendo y saludables
- [ ] `APP_KEY` generado y configurado
- [ ] Migraciones ejecutadas
- [ ] Permisos de storage configurados
- [ ] Certificados SSL obtenidos por Caddy
- [ ] API accesible en `https://api.notificaciones.space`
- [ ] Dashboard accesible en `https://dashboard.notificaciones.space`
- [ ] Dashboard se conecta correctamente a la API
- [ ] Logs sin errores críticos

### Post-Deployment

- [ ] Backup de base de datos configurado
- [ ] Monitoreo configurado (opcional)
- [ ] Documentación actualizada

---

## 📚 Recursos Adicionales

- [Documentación de Digital Ocean](https://docs.digitalocean.com/)
- [Documentación de Docker](https://docs.docker.com/)
- [Documentación de Caddy](https://caddyserver.com/docs/)
- [Documentación de Laravel](https://laravel.com/docs)
- [Documentación de PostgreSQL](https://www.postgresql.org/docs/)

---

## 🆘 Soporte

Si encuentras problemas:

1. Revisa los logs: `docker compose logs -f`
2. Verifica la configuración DNS
3. Verifica que los puertos estén abiertos en el firewall
4. Consulta la sección de [Solución de Problemas](#solución-de-problemas)

---

**¡Felicitaciones! Tu aplicación debería estar funcionando en producción con HTTPS automático.** 🎉
